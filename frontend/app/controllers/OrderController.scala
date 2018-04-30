package controllers

import java.util.UUID

import javax.inject.Inject
import be.yannickdeturck.lagomshopscala.item.api.ItemService
import be.yannickdeturck.lagomshopscala.order.api.{Order, OrderService}
import org.slf4j.LoggerFactory
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, number, optional, text}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * @author Yannick De Turck
  */
class OrderController @Inject()(cc: ControllerComponents, orderService: OrderService,
                                itemService: ItemService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  private val logger = LoggerFactory.getLogger(classOf[OrderController])

  def index = Action.async { implicit rh =>
    orderService.getOrders.invoke
      .map(getOrderResp => Ok(views.html.order.index(getOrderResp.orders)))
  }

  def createOrderForm = Action.async { implicit rh =>
    itemService.getItems.invoke()
      .map(getItemsResp => Ok(views.html.order.create(OrderForm.empty, getItemsResp.items)))
  }

  def createOrder = Action.async { implicit rh =>
    OrderForm.bind(rh).fold(
      errorForm => itemService.getItems.invoke()
        .map(getItemsResp => Ok(views.html.order.create(errorForm, getItemsResp.items))),
      orderForm => {
        val order = Order.create(orderForm.itemId, orderForm.amount, orderForm.customer)
        orderService.createOrder.invoke(order).map { order =>
          Redirect(routes.OrderController.getOrder(order.safeId))
        }
      }
    )
  }

  def getOrder(id: UUID) = Action.async { implicit rh =>
    orderService.getOrder(id).invoke()
      .map(order => Ok(views.html.order.detail(order)))
  }
}

case class OrderForm(id: Option[UUID] = None,
                     itemId: UUID = null,
                     amount: Int = 1,
                     customer: String = "")

object OrderForm {
  private val form = Form(
    mapping(
      "id" -> optional(
        text.verifying("order.error.invalid.id", id => Try(UUID.fromString(id)).isSuccess)
          .transform[UUID](UUID.fromString, _.toString)
      ),
      "itemId" -> text.verifying("item.error.invalid.id", id => Try(UUID.fromString(id)).isSuccess)
        .transform[UUID](UUID.fromString, _.toString),
      "amount" -> number(0, 1000),
      "customer" -> nonEmptyText
    )(OrderForm.apply)(OrderForm.unapply)
  )

  def fill(orderForm: OrderForm): Form[OrderForm] = form.fill(orderForm)

  def bind(implicit request: Request[AnyContent]): Form[OrderForm] = {
    form.bindFromRequest()
  }

  def empty: Form[OrderForm] = form
}