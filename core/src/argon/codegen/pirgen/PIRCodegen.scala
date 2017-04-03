package argon.codegen.pirgen

import scala.language.postfixOps
import argon.codegen.Codegen
import argon.codegen.FileDependencies

trait PIRCodegen extends Codegen with FileDependencies { // FileDependencies extends Codegen already
  import IR._
  override val name = "PIR Codegen"
  override val lang: String = "pir"
  override val ext: String = "scala"

  override protected def emitBlock(b: Block[_]): Unit = {
    visitBlock(b)
    emit(src"// results in ${b.result}")
  }

  final protected def emitController(b: Block[_]): Unit = {
    visitBlock(b)
    emit(src"// results in ${b.result}")
  }

  override def quote(s: Exp[_]): String = s match {
    case c: Const[_] => quoteConst(c)
    case b: Bound[_] => s"b${b.id}"
    case lhs: Sym[_] => s"x${lhs.id}"
  }

  final protected def emitGlobal(x: String): Unit = { 
    withStream(getStream("GlobalWires")) {
      emit(x) 
    }
  }

  final protected def emitModule(lhs: String, x: String, args: String*): Unit = {
    // dependencies ::= AlwaysDep(s"""${sys.env("SPATIAL_HOME")}/src/spatial/codegen/pirgen/resources/template-level/templates/$x.scala""")

    emit(src"""val $lhs = Module(new ${x}(${args.mkString}))""")
  } 

}