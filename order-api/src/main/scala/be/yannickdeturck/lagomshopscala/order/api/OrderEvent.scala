package be.yannickdeturck.lagomshopscala.order.api

import java.util.UUID

import julienrf.json.derived
import play.api.libs.json._

/**
  * @author Yannick De Turck
  */
sealed trait OrderEvent {
  val id: UUID
}

case class OrderCreated(id: UUID, itemId: UUID, amount: Int, customer: String) extends OrderEvent

object OrderCreated {
  implicit val format: Format[OrderCreated] = Json.format
}

object OrderEvent {
  implicit val format: Format[OrderEvent] =
    derived.flat.oformat((__ \ "type").format[String])
}