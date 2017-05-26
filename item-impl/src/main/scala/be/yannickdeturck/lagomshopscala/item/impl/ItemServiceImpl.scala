package be.yannickdeturck.lagomshopscala.item.impl

import java.util.UUID

import akka.NotUsed
import akka.persistence.query.Offset
import be.yannickdeturck.lagomshopscala.item.api.{GetItemsResponse, ItemService}
import be.yannickdeturck.lagomshopscala.item.api
import com.datastax.driver.core.utils.UUIDs
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Yannick De Turck
  */
class ItemServiceImpl(registry: PersistentEntityRegistry, itemRepository: ItemRepository)(implicit ec: ExecutionContext) extends ItemService {
  private val logger = LoggerFactory.getLogger(classOf[ItemServiceImpl])

  override def createItem = ServerServiceCall { input =>
    logger.info(s"Creating item with input $input...")
    val id = UUIDs.timeBased()
    val item = Item(id, input.title, input.description, input.price)
    val itemEntityRef = registry.refFor[ItemEntity](id.toString)
    itemEntityRef.ask(CreateItem(item)).map { _ =>
      convertItem(item)
    }
  }

  override def getItem(id: UUID) = ServerServiceCall { _ =>
    logger.info(s"Looking up item with ID $id...")
    val itemEntityRef = registry.refFor[ItemEntity](id.toString)
    itemEntityRef.ask(GetItem).map {
      case Some(item) => convertItem(item)
      case None => throw NotFound("Item " + id + " not found");
    }
  }

  override def getItems: ServiceCall[NotUsed, GetItemsResponse] = ServiceCall { _ =>
    logger.info("Looking up all items...")
    itemRepository.selectAllItems.map(items => GetItemsResponse(items.map(convertItem)))
  }

  override def itemEvents: Topic[api.ItemEvent] =
    TopicProducer.taggedStreamWithOffset(ItemEvent.Tag.allTags.toList) { (tag, offset) =>
      logger.info("Creating ItemEvent Topic...")
      registry.eventStream(tag, offset)
        .filter {
          _.event match {
            case x@(_: ItemCreated) => true
            case _ => false
          }
        }.mapAsync(1)(convertEvent)
    }

  private def convertItem(item: Item): api.Item = {
    api.Item(Some(item.id), item.title, item.description, item.price)
  }

  private def convertEvent(eventStreamElement: EventStreamElement[ItemEvent]): Future[(api.ItemEvent, Offset)] = {
    eventStreamElement match {
      case EventStreamElement(id, ItemCreated(item), offset) =>
        Future.successful {
          (api.ItemCreated(
            id = item.id,
            title = item.title,
            description = item.description,
            price = item.price
          ), offset)
        }
    }
  }
}