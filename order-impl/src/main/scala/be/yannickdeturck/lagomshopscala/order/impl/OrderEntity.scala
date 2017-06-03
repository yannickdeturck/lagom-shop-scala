package be.yannickdeturck.lagomshopscala.order.impl

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag, PersistentEntity}
import play.api.libs.json.{Format, Json}
import be.yannickdeturck.lagomshopscala.common.util.JsonFormats._

/**
  * @author Yannick De Turck
  */
class OrderEntity extends PersistentEntity {
  override type Command = OrderCommand
  override type Event = OrderEvent
  override type State = Option[Order]

  override def initialState: Option[Order] = None

  override def behavior: Behavior = {
    case None => notCreated
    case Some(order) => created(order)
  }

  private val getOrderCommand = Actions().onReadOnlyCommand[GetOrder.type, Option[Order]] {
    case (GetOrder, ctx, state) => ctx.reply(state)
  }

  private val notCreated = {
    Actions().onCommand[CreateOrder, Done] {
      case (CreateOrder(order), ctx, state) =>
        ctx.thenPersist(OrderCreated(order))(_ => ctx.reply(Done))
    }.onEvent {
      case (OrderCreated(order), state) => Some(order)
    }.orElse(getOrderCommand)
  }

  private def created(order: Order) = {
    Actions().orElse(getOrderCommand)
  }
}

case class Order(id: UUID, itemId: UUID, amount: Int, customer: String)

object Order {
  implicit val format: Format[Order] = Json.format
}

sealed trait OrderCommand

case object GetOrder extends OrderCommand with ReplyType[Option[Order]] {
  implicit val format: Format[GetOrder.type] = singletonFormat(GetOrder)
}

case class CreateOrder(order: Order) extends OrderCommand with ReplyType[Done]

object CreateOrder {
  implicit val format: Format[CreateOrder] = Json.format
}

sealed trait OrderEvent extends AggregateEvent[OrderEvent] {
  override def aggregateTag = OrderEvent.Tag
}

object OrderEvent {
  val NumShards = 4
  val Tag: AggregateEventShards[OrderEvent] = AggregateEventTag.sharded[OrderEvent](NumShards)
}

case class OrderCreated(order: Order) extends OrderEvent

object OrderCreated {
  implicit val format: Format[OrderCreated] = Json.format
}