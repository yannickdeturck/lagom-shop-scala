package be.yannickdeturck.lagomshopscala.order.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

/**
  * @author Yannick De Turck
  */
object OrderSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Order],

    JsonSerializer[CreateOrder],
    JsonSerializer[GetOrder.type],

    JsonSerializer[OrderCreated]
  )
}