package argon.core

import argon.traversal.Traversal

import scala.collection.mutable

trait Scopes extends Effects { self: Staging =>
  type CompilerPass = Traversal{val IR: Scopes.this.type }

  // --- State
  //private[argon] var eX: Option[CompilerPass] = None

  /** Class representing the result of a staged scope. */
  sealed abstract class Scope[T:Staged] {
    def tp: Staged[_] = typ[T]
    def result: Sym[T]            // Symbolic result of the scope
    def summary: Effects          // Effects summary for the entire scope
    def effectful: List[Sym[_]]   // List of all symbols with effectful nodes in this scope
  }

  case class Block[T:Staged](result: Sym[T], summary: Effects, effectful: List[Sym[_]]) extends Scope[T]
  case class Lambda[T:Staged](block: Block[T], inputs: Seq[Sym[_]]) extends Scope[T] {
    def result: Sym[T] = block.result
    def summary: Effects = block.summary
    def effectful: List[Sym[_]] = block.effectful
  }

  /**
    * Computes an *external* summary for a seq of nodes
    * (Ignores reads/writes on data allocated within the scope)
    */
  def summarizeScope(context: List[Sym[_]]): Effects = {
    var effects = Pure
    val allocs = new mutable.HashSet[Sym[_]]
    def clean(xs: Set[Sym[_]]) = xs diff allocs
    for (s@Effectful(u2, _) <- context) {
      if (u2.isMutable) allocs += s
      effects = effects andThen u2.copy(reads = clean(u2.reads), writes = clean(u2.writes))
    }
    effects
  }

  /**
    * Stage the effects of an isolated block.
    * No assumptions about the current context remain valid.
    */
  def stageBlock[T:Staged](block: => Sym[T]): Block[T] = {
    val saveContext = context
    context = Nil

    val result = block
    val deps = context
    context = saveContext

    val effects = summarizeScope(deps)
    Block[T](result, effects, deps)
  }
  /**
    * Stage the effects of a block that is executed 'here' (if it is executed at all).
    * All assumptions about the current context carry over unchanged.
    */
  def stageBlockInline[T:Staged](block: => Sym[T]): Block[T] = {
    val saveContext = context
    if (saveContext eq null) context = Nil

    val result = block
    val nAdded = context.length - saveContext.length

    if ((saveContext ne null) && context.drop(nAdded) != saveContext)
      throw IllegalStageHereException(saveContext, context)

    val deps = if (saveContext eq null) context else context.take(nAdded) // Most recent effects

    val effects = summarizeScope(deps)
    context = saveContext

    Block[T](result, effects, deps)
  }

  def stageLambda[T:Staged](inputs: Sym[_]*)(block: => Sym[T]): Lambda[T] = {
    Lambda(stageBlock{ block }, inputs)
  }

  /** Compiler debugging **/
  override def readable(x: Any) = x match {
    case b: Block[_] => c"Block(${b.result})"
    case b: Lambda[_] => c"""Lambda(${b.inputs.mkString("(",",",")")} => ${b.result})"""
    case _ => super.readable(x)
  }
}