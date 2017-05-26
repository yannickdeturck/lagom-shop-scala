package be.yannickdeturck.lagomshopscala.common.util

import java.util.UUID

import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.util.Try

/**
  * @author Yannick De Turck
  */
object JsonFormats {
  def singletonReads[O](singleton: O): Reads[O] = {
    (__ \ "value").read[String].collect(
      ValidationError(s"Expected a JSON object with a single field with key 'value' and value '${singleton.getClass.getSimpleName}'")
    ) {
      case s if s == singleton.getClass.getSimpleName => singleton
    }
  }

  def singletonWrites[O]: Writes[O] = Writes { singleton =>
    Json.obj("value" -> singleton.getClass.getSimpleName)
  }

  def singletonFormat[O](singleton: O): Format[O] = {
    Format(singletonReads(singleton), singletonWrites)
  }

  implicit val uuidReads: Reads[UUID] = implicitly[Reads[String]]
    .collect(ValidationError("Invalid UUID"))(Function.unlift { str =>
      Try(UUID.fromString(str)).toOption
    })

  implicit val uuidWrites: Writes[UUID] = Writes { uuid =>
    JsString(uuid.toString)
  }
}