package be.yannickdeturck.lagomshopscala.order.api

import java.util.UUID

import play.api.libs.json.{Format, Json}

/**
  * @author Yannick De Turck
  */
case class Order(id: Option[UUID], itemId: UUID, amount: Int, customer: String) {
  def safeId: UUID = id.getOrElse(UUID.randomUUID())
}

object Order {
  implicit val format: Format[Order] = Json.format

  def create(itemId: UUID, amount: Int, customer: String): Order = {
    Order(None, itemId, amount, customer)
  }
}