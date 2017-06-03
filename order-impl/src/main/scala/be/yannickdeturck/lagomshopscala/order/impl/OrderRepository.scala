package be.yannickdeturck.lagomshopscala.order.impl

import java.util.UUID

import akka.Done
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Yannick De Turck
  */
private[impl] class OrderRepository(session: CassandraSession)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(classOf[OrderRepository])

  def selectAllOrders: Future[Seq[Order]] = {
    logger.info("Querying all orders...")
    session.selectAll(
      """
      SELECT * FROM item_order
    """).map(rows => rows.map(row => convertOrder(row)))
  }

  def selectOrder(id: UUID) = {
    logger.info(s"Querying order with ID $id...")
    session.selectOne("SELECT * FROM item_order WHERE id = ?", id)
  }

  private def convertOrder(orderRow: Row): Order = {
    Order(
      orderRow.getUUID("id"),
      orderRow.getUUID("itemId"),
      orderRow.getInt("amount"),
      orderRow.getString("customer"))
  }
}

private[impl] class OrderEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[OrderEvent] {
  private val logger = LoggerFactory.getLogger(classOf[OrderEventProcessor])

  private var insertOrderStatement: PreparedStatement = _

  def buildHandler: ReadSideProcessor.ReadSideHandler[OrderEvent] = {
    readSide.builder[OrderEvent]("orderEventOffset")
      .setGlobalPrepare(createTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[OrderCreated](e => {
      insertOrder(e.event.order)
    }).build
  }

  def aggregateTags: Set[AggregateEventTag[OrderEvent]] = OrderEvent.Tag.allTags

  private def createTables() = { // TODO we should also store some info about the Item entity such as title
    logger.info("Creating tables...")
    for {
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS item_order (
          id timeuuid PRIMARY KEY,
          itemId timeuuid,
          amount int,
          customer text
        )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    logger.info("Preparing statements...")
    for {
      insertOrder <- session.prepare(
        """
        INSERT INTO item_order(
          id,
          itemId,
          amount,
          customer
        ) VALUES (?, ?, ?, ?)
      """)
    } yield {
      insertOrderStatement = insertOrder
      Done
    }
  }

  private def insertOrder(order: Order) = {
    logger.info(s"Inserting $order...")
    Future.successful(List(
      insertOrderStatement.bind(order.id, order.itemId, Int.box(order.amount), order.customer)
    ))
  }
}