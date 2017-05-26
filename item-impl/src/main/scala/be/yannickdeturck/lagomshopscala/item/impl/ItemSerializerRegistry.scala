package be.yannickdeturck.lagomshopscala.item.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

/**
  * @author Yannick De Turck
  */
object ItemSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Item],

    JsonSerializer[CreateItem],
    JsonSerializer[GetItem.type],

    JsonSerializer[ItemCreated]
  )
}