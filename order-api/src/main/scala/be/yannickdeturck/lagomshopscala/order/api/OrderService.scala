package be.yannickdeturck.lagomshopscala.order.api

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

/**
  * @author Yannick De Turck
  */
trait OrderService extends Service {

  def createOrder: ServiceCall[Order, Order]

  def getOrder(id: UUID): ServiceCall[NotUsed, Order]

  def getOrders: ServiceCall[NotUsed, GetOrdersResponse]

  def orderEvents: Topic[OrderEvent]

  override final def descriptor: Descriptor = {
    import Service._

    named("order").withCalls(
      pathCall("/api/order", createOrder),
      pathCall("/api/order", getOrders _),
      pathCall("/api/order/:id", getOrder _)
    ).withTopics(
      topic("order-OrderEvent", this.orderEvents)
    ).withAutoAcl(true)
  }
}

case class GetOrdersResponse(orders: Seq[Order])

object GetOrdersResponse {
  implicit val format: Format[GetOrdersResponse] = Json.format
}