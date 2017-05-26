package be.yannickdeturck.lagomshopscala.item.impl

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag, PersistentEntity}
import play.api.libs.json.{Format, Json}
import be.yannickdeturck.lagomshopscala.common.util.JsonFormats._

/**
  * @author Yannick De Turck
  */
class ItemEntity extends PersistentEntity {
  override type Command = ItemCommand
  override type Event = ItemEvent
  override type State = Option[Item]

  override def initialState: Option[Item] = None

  override def behavior: Behavior = {
    case None => notCreated
    case Some(item) => created(item)
  }

  private val getItemCommand = Actions().onReadOnlyCommand[GetItem.type, Option[Item]] {
    case (GetItem, ctx, state) => ctx.reply(state)
  }

  private val notCreated = {
    Actions().onCommand[CreateItem, Done] {
      case (CreateItem(item), ctx, state) =>
        ctx.thenPersist(ItemCreated(item))(_ => ctx.reply(Done))
    }.onEvent {
      case (ItemCreated(item), state) => Some(item)
    }.orElse(getItemCommand)
  }

  private def created(item: Item) = {
    Actions().orElse(getItemCommand)
  }
}

case class Item(id: UUID, title: String, description: String, price: BigDecimal)

object Item {
  implicit val format: Format[Item] = Json.format
}

sealed trait ItemCommand

case object GetItem extends ItemCommand with ReplyType[Option[Item]] {
  implicit val format: Format[GetItem.type] = singletonFormat(GetItem)
}

case class CreateItem(item: Item) extends ItemCommand with ReplyType[Done]

object CreateItem {
  implicit val format: Format[CreateItem] = Json.format
}

sealed trait ItemEvent extends AggregateEvent[ItemEvent] {
  override def aggregateTag = ItemEvent.Tag
}

object ItemEvent {
  val NumShards = 4
  val Tag: AggregateEventShards[ItemEvent] = AggregateEventTag.sharded[ItemEvent](NumShards)
}

case class ItemCreated(item: Item) extends ItemEvent

object ItemCreated {
  implicit val format: Format[ItemCreated] = Json.format
}