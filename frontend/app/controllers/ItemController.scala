package controllers

import java.util.UUID
import javax.inject.Inject

import be.yannickdeturck.lagomshopscala.item.api.{Item, ItemService}
import org.slf4j.LoggerFactory
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * @author Yannick De Turck
  */
class ItemController @Inject()(cc: ControllerComponents, itemService: ItemService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  private val logger = LoggerFactory.getLogger(classOf[ItemController])

  def index = Action.async { implicit rh =>
    itemService.getItems.invoke
      .map(getItemResp => Ok(views.html.item.index(getItemResp.items)))
  }

  def createItemForm = Action { implicit rh =>
    Ok(views.html.item.create(ItemForm.fill(ItemForm())))
  }

  def createItem = Action.async { implicit rh =>
    ItemForm.bind(rh).fold(
      errorForm => Future.successful(Ok(views.html.item.create(errorForm))),
      itemForm => {
        val item = Item.create(itemForm.title, itemForm.description, itemForm.price)
        itemService.createItem.invoke(item).map { item =>
          Redirect(routes.ItemController.getItem(item.safeId))
        }
      }
    )
  }

  def getItem(id: UUID) = Action.async { implicit rh =>
    itemService.getItem(id).invoke()
      .map(item => Ok(views.html.item.detail(item)))
  }
}

case class ItemForm(id: Option[UUID] = None,
                    title: String = "",
                    description: String = "",
                    price: BigDecimal = BigDecimal.valueOf(0L))

object ItemForm {
  private val form = Form(
    mapping(
      "id" -> optional(
        text.verifying("item.error.invalid.id", id => Try(UUID.fromString(id)).isSuccess)
          .transform[UUID](UUID.fromString, _.toString)
      ),
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> bigDecimal
        .verifying("item.error.invalid.price", _ > 0)
    )(ItemForm.apply)(ItemForm.unapply)
  )

  def fill(itemForm: ItemForm): Form[ItemForm] = form.fill(itemForm)

  def bind(implicit request: Request[AnyContent]): Form[ItemForm] = {
    form.bindFromRequest()
  }
}