package be.yannickdeturck.lagomshopscala.item.impl

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
private[impl] class ItemRepository(session: CassandraSession)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(classOf[ItemRepository])

  def selectAllItems: Future[Seq[Item]] = {
    logger.info("Querying all items...")
    session.selectAll(
      """
      SELECT * FROM item
    """).map(rows => rows.map(row => convertItem(row)))
  }

  def selectItem(id: UUID) = {
    logger.info(s"Querying item with ID $id...")
    session.selectOne("SELECT * FROM item WHERE id = ?", id)
  }

  private def convertItem(itemRow: Row): Item = {
    Item(
      itemRow.getUUID("id"),
      itemRow.getString("title"),
      itemRow.getString("description"),
      itemRow.getDecimal("price"))
  }
}

private[impl] class ItemEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[ItemEvent] {
  private val logger = LoggerFactory.getLogger(classOf[ItemEventProcessor])

  private var insertItemStatement: PreparedStatement = _

  def buildHandler: ReadSideProcessor.ReadSideHandler[ItemEvent] = {
    readSide.builder[ItemEvent]("itemEventOffset")
      .setGlobalPrepare(createTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[ItemCreated](e => {
      insertItem(e.event.item)
    })
      .build
  }

  def aggregateTags: Set[AggregateEventTag[ItemEvent]] = ItemEvent.Tag.allTags

  private def createTables() = {
    logger.info("Creating tables...")
    for {
      _ <- session.executeCreateTable(
        """
        CREATE TABLE IF NOT EXISTS item (
          id timeuuid PRIMARY KEY,
          title text,
          description text,
          price decimal
        )
      """)
    } yield Done
  }

  private def prepareStatements() = {
    logger.info("Preparing statements...")
    for {
      insertItem <- session.prepare(
        """
        INSERT INTO item(
          id,
          title,
          description,
          price
        ) VALUES (?, ?, ?, ?)
      """)
    } yield {
      insertItemStatement = insertItem
      Done
    }
  }

  private def insertItem(item: Item) = {
    logger.info(s"Inserting $item...")
    Future.successful(List(
      insertItemStatement.bind(item.id, item.title, item.description, item.price.bigDecimal)
    ))
  }
}