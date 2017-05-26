package be.yannickdeturck.lagomshopscala.item.api

import java.util.UUID

import play.api.libs.json.{Format, Json}

/**
  * @author Yannick De Turck
  */
case class Item(id: Option[UUID], title: String, description: String, price: BigDecimal) {
  def safeId: UUID = id.getOrElse(UUID.randomUUID())
}

object Item {
  implicit val format: Format[Item] = Json.format

  def create(title: String, description: String, price: BigDecimal): Item = {
    Item(None, title, description, price)
  }
}