package argon.ops
import argon.core.Base

trait IfThenElseOps extends Base with BoolOps {this: TextOps => }
trait IfThenElseApi extends IfThenElseOps with BoolApi { this: TextApi => }

trait IfThenElseExp extends IfThenElseOps with BoolExp with OverloadHack { this: TextExp =>
  /** Virtualized Methods **/
  def __ifThenElse[A,B](cond: Bool, thenp: => A, elsep: => A)(implicit ctx: SrcCtx, l: Lift[A,B], o1: Overload1): B = {
    implicit val staged: Staged[B] = l.staged
    val unwrapThen = () => unwrap(lift(thenp) ) // directly calling unwrap(thenp) forces thenp to be evaluated here
    val unwrapElse = () => unwrap(lift(elsep) ) // wrapping it as a Function0 allows it to be delayed
    wrap(ifThenElse(cond.s, unwrapThen(), unwrapElse()))
  }

  /** IR Nodes **/
  case class IfThenElse[T:Staged](cond: Exp[Bool], thenp: Block[T], elsep: Block[T]) extends Op[T] {
    def mirror(f:Tx) = ifThenElse[T](f(cond), f(thenp), f(elsep))

    override def freqs   = normal(cond) ++ cold(thenp) ++ cold(elsep)
    override def aliases = syms(thenp.result, elsep.result)
  }

  /** Smart Constructor **/
  def ifThenElse[T:Staged](cond: Exp[Bool], thenp: => Exp[T], elsep: => Exp[T])(implicit ctx: SrcCtx): Exp[T] = cond match {
    case Const(true) if context != null => thenp // Inlining is not valid if there is no outer context
    case Const(false) if context != null => elsep
    case Op(Not(x)) => ifThenElse(x, elsep, thenp)
    case _ =>
      val thenBlk = stageBlock(thenp)
      val elseBlk = stageBlock(elsep)
      val effects = thenBlk.summary orElse elseBlk.summary
      stageEffectful(IfThenElse(cond, thenBlk, elseBlk), effects)(ctx)
  }
}
