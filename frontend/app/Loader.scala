import be.yannickdeturck.lagomshopscala.item.api.ItemService
import be.yannickdeturck.lagomshopscala.order.api.OrderService
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo, ServiceLocator}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.softwaremill.macwire.wire
import controllers.{AssetsComponents, DashboardController, ItemController, OrderController}
import play.api.ApplicationLoader.Context
import play.api.i18n.I18nComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, Mode}
import play.filters.HttpFiltersComponents
import router.Routes

import scala.collection.immutable
import scala.concurrent.ExecutionContext

/**
  * @author Yannick De Turck
  */
abstract class Frontend(context: Context) extends BuiltInComponentsFromContext(context)
  with I18nComponents
  with AhcWSComponents
  with LagomKafkaClientComponents
  with LagomServiceClientComponents
  with LagomConfigComponent
  with HttpFiltersComponents
  with AssetsComponents {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "frontend",
    Map(
      "frontend" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  override lazy val router: Routes = {
    val prefix = "/"
    wire[Routes]
  }

  lazy val itemService: ItemService = serviceClient.implement[ItemService]
  lazy val itemController: ItemController = wire[ItemController]
  lazy val orderService: OrderService = serviceClient.implement[OrderService]
  lazy val orderController: OrderController = wire[OrderController]
  lazy val dashboardController: DashboardController = wire[DashboardController]
}

class FrontendLoader extends ApplicationLoader {
  override def load(context: Context): Application = context.environment.mode match {
    case Mode.Dev =>
      (new Frontend(context) with LagomDevModeComponents).application
    case _ =>
      new Frontend(context) {
        override lazy val circuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)

        override def serviceLocator: ServiceLocator = NoServiceLocator
      }.application
  }
}