package controllers

import javax.inject.Inject

import akka.Done
import akka.stream.scaladsl.Flow
import be.yannickdeturck.lagomshopscala.item.api.{ItemEvent, ItemService}
import be.yannickdeturck.lagomshopscala.order.api.{OrderEvent, OrderService}
import org.slf4j.LoggerFactory
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Yannick De Turck
  */
class DashboardController @Inject()(val messagesApi: MessagesApi, itemService: ItemService,
                                    orderService: OrderService)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  private val logger = LoggerFactory.getLogger(classOf[DashboardController])

  // quick 'n dirty cache, in a real scenario you would query a local data store
  var eventsCache: ListBuffer[String] = collection.mutable.ListBuffer.empty[String]

  itemService.itemEvents.subscribe.withGroupId("dashboard-item-events")
    .atLeastOnce(Flow[ItemEvent].map(event => event.id.toString).collect { case x => x }
      .mapAsync(1)(id => {
        logger.info(s"Received item event $id")
        eventsCache += s"Inserted item $id\n"
        Future(Done.getInstance)
      }))

  orderService.orderEvents.subscribe.withGroupId("dashboard-order-events")
    .atLeastOnce(Flow[OrderEvent].map(event => event.id.toString).collect { case x => x }
      .mapAsync(1)(id => {
        logger.info(s"Received order event $id")
        eventsCache += s"Inserted order $id\n"
        Future(Done.getInstance)
      }))

  def events = Action { implicit rh =>
    Ok(views.html.dashboard.events(eventsCache.toList))
  }
}
