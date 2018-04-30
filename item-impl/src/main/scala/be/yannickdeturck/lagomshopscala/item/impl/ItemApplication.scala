package be.yannickdeturck.lagomshopscala.item.impl

import be.yannickdeturck.lagomshopscala.item.api.ItemService
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{Environment, LoggerConfigurator}

import scala.concurrent.ExecutionContext

/**
  * @author Yannick De Turck
  */
trait ItemComponents extends LagomServerComponents with CassandraPersistenceComponents with PubSubComponents {
  implicit def executionContext: ExecutionContext

  def environment: Environment

  override lazy val lagomServer: LagomServer = serverFor[ItemService](wire[ItemServiceImpl])
  lazy val itemRepository = wire[ItemRepository]
  override lazy val jsonSerializerRegistry = ItemSerializerRegistry

  persistentEntityRegistry.register(wire[ItemEntity])
  readSide.register(wire[ItemEventProcessor])
}

abstract class ItemApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with ItemComponents
  with AhcWSComponents
  with LagomKafkaComponents {}

class ItemApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // Workaround for logback.xml not being detected, see https://github.com/lagom/lagom/issues/534
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    // end workaround
    new ItemApplication(context) with LagomDevModeComponents
  }

  override def load(context: LagomApplicationContext): LagomApplication =
    new ItemApplication(context) {
      override lazy val circuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)

      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def describeService = Some(readDescriptor[ItemService])
}