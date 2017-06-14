package be.yannickdeturck.lagomshopscala.item.impl

import akka.stream.scaladsl.Sink
import akka.stream.testkit.scaladsl.TestSink
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LocalServiceLocator}
import com.lightbend.lagom.scaladsl.testkit.{ServiceTest, TestTopicComponents}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import be.yannickdeturck.lagomshopscala.item.api
import be.yannickdeturck.lagomshopscala.item.api.ItemService
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

/**
  * @author Yannick De Turck
  */
class ItemServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with ItemComponents with LocalServiceLocator with AhcWSComponents with TestTopicComponents
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

    "publish item events on the Kafka topic" in {
      implicit val system = server.actorSystem
      implicit val mat = server.materializer

      for {
        createdItem <- itemService.createItem.invoke(api.Item(None, "title", "description", BigDecimal.valueOf(10L)))
        events <- itemService.itemEvents.subscribe.atMostOnceSource
          .filter(_.id == createdItem.safeId)
          .take(1)
          .runWith(Sink.seq)
      } yield {
        events.size shouldBe 1
        events.head shouldBe an[api.ItemCreated]
        val event = events.head.asInstanceOf[api.ItemCreated]
        event.title should be("title")
        event.description should be("description")
        event.price should be(BigDecimal.valueOf(10L))
      }
    }

    "publish newly created items on the PubSub topic" in {
      itemService.itemStream.invoke.map { source =>
        implicit val system = server.actorSystem
        implicit val mat = server.materializer

        val item1 = api.Item(None, "title", "description", BigDecimal.valueOf(10.50))
        val item2 = api.Item(None, "title2", "description2", BigDecimal.valueOf(12.50))
        val item3 = api.Item(None, "title3", "description3", BigDecimal.valueOf(15.50))
        val probe = source.runWith(TestSink.probe)
        probe.request(3)

        itemService.createItem.invoke(item1)
        itemService.createItem.invoke(item2)
        itemService.createItem.invoke(item3)

        probe.expectNextUnordered(item1, item2, item3)
        probe.cancel()
        succeed
      }
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