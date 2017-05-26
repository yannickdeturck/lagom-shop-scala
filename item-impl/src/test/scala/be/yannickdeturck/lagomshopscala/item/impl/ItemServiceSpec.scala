package be.yannickdeturck.lagomshopscala.item.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import be.yannickdeturck.lagomshopscala.item.api
import be.yannickdeturck.lagomshopscala.item.api.ItemService

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

/**
  * @author Yannick De Turck
  */
class ItemServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new ItemApplication(ctx) with LocalServiceLocator
  }

  val itemService: ItemService = server.serviceClient.implement[ItemService]

  override protected def afterAll(): Unit = server.stop()

  "item service" should {
    "allow creating an item" in {
      val inputItem = api.Item(None, "title", "description", BigDecimal.valueOf(10.50))
      for {
        createdItem <- itemService.createItem.invoke(inputItem)
      } yield {
        createdItem.safeId should not be null
        createdItem.title should be("title")
        createdItem.description should be("description")
        createdItem.price should be(BigDecimal.valueOf(10.50))
      }
    }

    "allow looking up a created item" in {
      val inputItem = api.Item(None, "title", "description", BigDecimal.valueOf(10.50))
      for {
        createdItem <- itemService.createItem.invoke(inputItem)
        lookupItem <- itemService.getItem(createdItem.safeId).invoke
      } yield {
        createdItem should ===(lookupItem)
      }
    }

    "allow looking up all created items" in {
      val inputItem1 = api.Item(None, "title1", "description1", BigDecimal.valueOf(7.50))
      val inputItem2 = api.Item(None, "title2", "description2", BigDecimal.valueOf(14.99))
      val inputItem3 = api.Item(None, "title3", "description3", BigDecimal.valueOf(20.00))
      (for {
        createdItem1 <- itemService.createItem.invoke(inputItem1)
        createdItem2 <- itemService.createItem.invoke(inputItem2)
        createdItem3 <- itemService.createItem.invoke(inputItem3)
      } yield {
        awaitSuccess() {
          for {
            lookupItemsResponse <- itemService.getItems.invoke
          } yield {
            lookupItemsResponse.items should contain allOf(createdItem1, createdItem2, createdItem3)
          }
        }
      }).flatMap(identity)
    }
  }

  def awaitSuccess[T](maxDuration: FiniteDuration = 10.seconds, checkEvery: FiniteDuration = 100.milliseconds)(block: => Future[T]): Future[T] = {
    val checkUntil = System.currentTimeMillis() + maxDuration.toMillis

    def doCheck(): Future[T] = {
      block.recoverWith {
        case recheck if checkUntil > System.currentTimeMillis() =>
          val timeout = Promise[T]()
          server.application.actorSystem.scheduler.scheduleOnce(checkEvery) {
            timeout.completeWith(doCheck())
          }(server.executionContext)
          timeout.future
      }
    }

    doCheck()
  }
}