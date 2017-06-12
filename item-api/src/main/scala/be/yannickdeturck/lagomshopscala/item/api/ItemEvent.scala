package be.yannickdeturck.lagomshopscala.item.api

import java.util.UUID

import julienrf.json.derived
import play.api.libs.json._

/**
  * @author Yannick De Turck
  */
sealed trait ItemEvent {
  val id: UUID
}

case class ItemCreated(id: UUID, title: String, description: String, price: BigDecimal) extends ItemEvent

object ItemCreated {
  implicit val format: Format[ItemCreated] = Json.format
}

object ItemEvent {
  implicit val format: Format[ItemEvent] = derived.flat.oformat((__ \ "type").format[String])
}