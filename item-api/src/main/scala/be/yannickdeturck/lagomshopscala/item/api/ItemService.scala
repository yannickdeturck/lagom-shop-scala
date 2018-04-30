package be.yannickdeturck.lagomshopscala.item.api

import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

/**
  * @author Yannick De Turck
  */
trait ItemService extends Service {

  // Check services running: http://localhost:9008/services
  def createItem: ServiceCall[Item, Item]

  def getItem(id: UUID): ServiceCall[NotUsed, Item]

  def getItems: ServiceCall[NotUsed, GetItemsResponse]

  def itemEvents: Topic[ItemEvent]

  def itemStream: ServiceCall[NotUsed, Source[Item, NotUsed]]

  override final def descriptor: Descriptor = {
    import Service._

    named("item").withCalls(
      pathCall("/api/item", createItem),
      pathCall("/api/item", getItems _),
      pathCall("/api/item/stream", itemStream _),
      pathCall("/api/item/:id", getItem _)
    ).withTopics(
      topic("item-ItemEvent", this.itemEvents)
    ).withAutoAcl(true)
  }
}

case class GetItemsResponse(items: Seq[Item])

object GetItemsResponse {
  implicit val format: Format[GetItemsResponse] = Json.format
}