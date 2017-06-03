package be.yannickdeturck.lagomshopscala.order.impl

import java.util.UUID

import akka.NotUsed
import akka.persistence.query.Offset
import be.yannickdeturck.lagomshopscala.item.api.ItemService
import be.yannickdeturck.lagomshopscala.order.api
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound, TransportErrorCode, TransportException}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Yannick De Turck
  */
class OrderServiceImpl(registry: PersistentEntityRegistry, orderRepository: OrderRepository, itemService: ItemService)(implicit ec: ExecutionContext) extends api.OrderService {
  private val logger = LoggerFactory.getLogger(classOf[OrderServiceImpl])

  override def createOrder = ServerServiceCall { input =>
    logger.info(s"Creating order with input $input...")
    val getItem = itemService.getItem(input.itemId).invoke()
      .recover { // TODO How to correctly catch this error?
        case e: NotFound => throw new TransportException(TransportErrorCode.BadRequest, s"Invalid item specified")
        case _ => throw new Exception("DA FAK")
      }
    getItem.flatMap { item =>
      val id = UUIDs.timeBased()
      val order = Order(id, item.safeId, input.amount, input.customer)
      val orderEntityRef = registry.refFor[OrderEntity](id.toString)
      orderEntityRef.ask(CreateOrder(order)).map { _ =>
        convertOrder(order)
      }
    }
  }

  override def getOrder(id: UUID) = ServerServiceCall { _ =>
    logger.info(s"Looking up order with ID $id...")
    val orderEntityRef = registry.refFor[OrderEntity](id.toString)
    orderEntityRef.ask(GetOrder).map {
      case Some(order) => convertOrder(order)
      case None => throw NotFound(s"Order $id not found");
    }
  }

  override def getOrders: ServiceCall[NotUsed, api.GetOrdersResponse] = ServiceCall { _ =>
    logger.info("Looking up all orders...")
    orderRepository.selectAllOrders.map(orders => api.GetOrdersResponse(orders.map(convertOrder)))
  }

  override def orderEvents: Topic[api.OrderEvent] =
    TopicProducer.taggedStreamWithOffset(OrderEvent.Tag.allTags.toList) { (tag, offset) =>
      logger.info("Creating OrderEvent Topic...")
      registry.eventStream(tag, offset)
        .filter {
          _.event match {
            case x@(_: OrderCreated) => true
            case _ => false
          }
        }.mapAsync(1)(convertEvent)
    }

  private def convertOrder(order: Order): api.Order = {
    api.Order(Some(order.id), order.itemId, order.amount, order.customer)
  }

  private def convertEvent(eventStreamElement: EventStreamElement[OrderEvent]): Future[(api.OrderEvent, Offset)] = {
    eventStreamElement match {
      case EventStreamElement(id, OrderCreated(order), offset) =>
        Future.successful {
          (api.OrderCreated(
            id = order.id,
            itemId = order.itemId,
            amount = order.amount,
            customer = order.customer
          ), offset)
        }
    }
  }
}