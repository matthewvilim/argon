package argon.ops

// import org.virtualized.{RefinedManifest,RecordOps,Record}
import argon.Config
import argon.core.Staging

trait StructApi extends StructExp with VoidApi

trait StructExp extends Staging with VoidExp with TextExp {

  abstract class Struct[T:StructType] { self =>
    def field[R:Staged](name: String)(implicit ctx: SrcCtx): R = wrap(field_apply[T,R](unwrap(self.asInstanceOf[T]), name))
  }
  def infix_toString[T:StructType](struct: T)(implicit ctx: SrcCtx): Text = {
    val tp = implicitly[StructType[T]]
    val fields = tp.fields.map{case (name,fieldTyp) => textify(field(struct, name)(tp, fieldTyp, ctx))(mtyp(fieldTyp),ctx) }
    lift[String,Text](tp.prefix + "(") + fields.reduceLeft{(a,b) => a + "," + b } + ")"
  }

  abstract class StructType[T] extends Staged[T] {
    override def isPrimitive = false
    def fields: Seq[(String, Staged[_])]
    def prefix: String = this.stagedClass.getSimpleName
  }

  // def record_new[T: RefinedManifest](fields: (String, _)*): T
  // def record_select[T: Manifest](record: Record, field: String): T
  def struct[T:StructType](fields: (String, Exp[_])*)(implicit ctx: SrcCtx): T = wrap(struct_new[T](fields))
  def field[T:StructType,R:Staged](struct: T, name: String)(implicit ctx: SrcCtx): R = wrap(field_apply[T,R](struct.s, name))

  /** IR Nodes **/
  abstract class StructAlloc[T:StructType] extends Op[T] {
    def elems: Seq[(String, Exp[_])]

    override def inputs   = syms(elems.map(_._2))
    override def reads    = Nil
    override def freqs    = normal(elems.map(_._2))

    override def aliases  = Nil
    override def contains = syms(elems.map(_._2))
  }

  case class SimpleStruct[S:StructType](elems: Seq[(String,Exp[_])]) extends StructAlloc[S] {
    def mirror(f:Tx) = struct_new[S](elems.map{case (idx,sym) => idx -> f(sym) })
  }

  case class FieldApply[S:StructType,T:Staged](struct: Exp[S], field: String) extends Op2[S,T] {
    def mirror(f:Tx) = field_apply[S,T](f(struct), field)

    override def extracts = syms(struct)
  }
  case class FieldUpdate[S:StructType,T:Staged](struct: Exp[S], field: String, value: Exp[T]) extends Op3[S,T,Void] {
    def mirror(f:Tx) = field_update(f(struct), field, f(value))

    override def contains = syms(value)  // TODO: Is this necessary?
  }


  /** Constructors **/
  def struct_new[S:StructType](elems: Seq[(String, Exp[_])])(implicit ctx: SrcCtx): Exp[S] = {
    stage(SimpleStruct(elems))(ctx)
  }

  // TODO: Should struct unwrapping be disabled for mutable structs?
  def field_apply[S:StructType,T:Staged](struct: Exp[S], index: String)(implicit ctx: SrcCtx): Exp[T] = struct match {
    case Op(s:StructAlloc[_]) if Config.unwrapStructs => unwrapStruct[S,T](struct, index) match {
      case Some(x) => x
      case None => stage(FieldApply[S,T](struct, index))(ctx)
    }
    case _ => stage(FieldApply[S,T](struct, index))(ctx)
  }
  def field_update[S:StructType,T:Staged](struct: Exp[S], index: String, value: Exp[T])(implicit ctx: SrcCtx): Exp[Void] = {
    stageWrite(struct)(FieldUpdate(struct, index, value))(ctx)
  }

  /** Helper functions **/
  object Struct {
    def unapply(x: Op[_]): Option[Map[String,Exp[_]]] = x match {
      case s: StructAlloc[_] => Some(s.elems.toMap)
      case _ => None
    }
  }

  def unwrapStruct[S:StructType,T:Staged](struct: Exp[S], index: String): Option[Exp[T]] = struct match {
    case Op(Struct(elems)) => elems.get(index) match {
      case Some(x) if x.tp <:< typ[T] => Some(x.asInstanceOf[Exp[T]]) // TODO: Should this be staged asInstanceOf?
      case None =>
        throw new NoFieldException(struct, index) // TODO: Should this be a user error?
    }
    case _ => None
  }

  /** Internals **/
  override def recurseAtomicLookup(s: Exp[_]): Exp[_] = s match {
    case Def(FieldApply(struct, index)) => recurseAtomicLookup(struct)
    case _ => super.recurseAtomicLookup(s)
  }

}
