package be.yannickdeturck.lagomshopscala.item.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

/**
  * @author Yannick De Turck
  */
class ItemEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues {
  private val system = ActorSystem("ItemEntitySpec",
    JsonSerializerRegistry.actorSystemSetupFor(ItemSerializerRegistry))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private val id = UUID.randomUUID
  private val item = Item(id, "title", "desc", BigDecimal.valueOf(25.95))

  private def withTestDriver[T](block: PersistentEntityTestDriver[ItemCommand, ItemEvent, Option[Item]] => T): T = {
    val driver = new PersistentEntityTestDriver(system, new ItemEntity, id.toString)
    try {
      block(driver)
    } finally {
      driver.getAllIssues shouldBe empty
    }
  }

  "item entity" should {
    "allow creating an item" in withTestDriver { driver =>
      val outcome = driver.run(CreateItem(item))
      outcome.events should contain only ItemCreated(item)
      outcome.state should ===(Some(item))
    }

    "allow looking up an item" in withTestDriver { driver =>
      driver.run(CreateItem(item))
      val outcome = driver.run(GetItem)
      outcome.events shouldBe empty
      outcome.replies should contain only Some(item)
      outcome.state should ===(Some(item))
    }
  }
}