package be.yannickdeturck.lagomshopscala.order.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import be.yannickdeturck.lagomshopscala.order.impl._
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

/**
  * @author Yannick De Turck
  */
class OrderEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues {
  private val system = ActorSystem("OrderEntitySpec",
    JsonSerializerRegistry.actorSystemSetupFor(OrderSerializerRegistry))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private val id = UUID.randomUUID
  private val itemId = UUID.randomUUID
  private val order = Order(id, itemId, 3, "Yannick")

  private def withTestDriver[T](block: PersistentEntityTestDriver[OrderCommand, OrderEvent, Option[Order]] => T): T = {
    val driver = new PersistentEntityTestDriver(system, new OrderEntity, id.toString)
    try {
      block(driver)
    } finally {
      driver.getAllIssues shouldBe empty
    }
  }

  "order entity" should {
    "allow creating an order" in withTestDriver { driver =>
      val outcome = driver.run(CreateOrder(order))
      outcome.events should contain only OrderCreated(order)
      outcome.state should ===(Some(order))
    }

    "allow looking up an order" in withTestDriver { driver =>
      driver.run(CreateOrder(order))
      val outcome = driver.run(GetOrder)
      outcome.events shouldBe empty
      outcome.replies should contain only Some(order)
      outcome.state should ===(Some(order))
    }
  }
}