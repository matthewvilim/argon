package argon.lang.direct
package typeclasses

import forge._
import org.virtualized.virtualize

trait OrderExp {

  implicit class OrderInfixOps[T:Order](lhs: T) {
    @api def > (rhs: T): MBoolean = ord[T].lessThan(rhs, lhs)
    @api def >=(rhs: T): MBoolean = ord[T].lessThanOrEqual(rhs, lhs)
    @api def < (rhs: T): MBoolean = ord[T].lessThan(lhs, rhs)
    @api def <=(rhs: T): MBoolean = ord[T].lessThanOrEqual(lhs, rhs)
    @api def ===(rhs: T): MBoolean = ord[T].equal(lhs, rhs)
    @api def =!=(rhs: T): MBoolean = !(lhs === rhs)
  }

  def ord[T:Order] = implicitly[Order[T]]
}