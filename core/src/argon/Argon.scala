package argon

import argon.core._
import argon.ops._
import argon.typeclasses._


trait ArgonExp extends ArrayExp with ArrayExtExp
  with BoolExp with ArithExp
  with CastExp with FixPtExp with FltPtExp
  with HashMapExp with IfThenElseExp with StructExp
  with TextExp with TupleExp with VoidExp
  with BitsExp with CustomBitWidths with OrderExp
  with NumExp with Reporting
  with FunctionExp with Staging

trait ArgonApi extends ArrayApi with ArrayExtApi
  with BoolApi with ArithApi
  with CastApi with FixPtApi with FltPtApi
  with HashMapApi with IfThenElseApi with StructApi
  with TextApi with TupleApi with VoidApi
  with BitsApi with OrderApi
  with NumApi with LowPriorityNumImplicits
  with FunctionApi with ArgonExp