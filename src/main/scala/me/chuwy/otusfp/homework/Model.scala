package me.chuwy.otusfp.homework

import cats.effect._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object Model {
  case class Counter(counter: Int)

  implicit val counterEncoder: Encoder[Counter] = deriveEncoder[Counter]
  implicit val counterDecoder: Decoder[Counter] = deriveDecoder[Counter]
  implicit def counterEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, Counter] = jsonOf[F, Counter]
  implicit def counterEntityEncoder[F[_]: Concurrent]: EntityEncoder[F, Counter] = jsonEncoderOf[F, Counter]

}
