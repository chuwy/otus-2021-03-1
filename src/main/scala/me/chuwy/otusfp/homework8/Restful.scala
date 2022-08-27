package me.chuwy.otusfp.homework8

import cats.effect._
import com.comcast.ip4s.{Host, Port}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.{HttpRoutes, Response}
import org.http4s.circe.CirceEntityEncoder._
import fs2.Stream

import scala.concurrent.duration.DurationInt


object Restful {

  type Counter[F[_]] = Ref[F, Long]

  implicit val encoderCounterDto: Encoder[CounterDTO] = deriveEncoder[CounterDTO]

  def serviceCounter(counter: Counter[IO]): IO[Response[IO]] =
    for {
        _ <- counter.update(_ + 1)
        c <- counter.get
        resp <- Ok(CounterDTO(c))
    } yield resp

  def serviceSlow(params: SlowParams): IO[Response[IO]] = {

    val stream: Stream[IO, Long] = Stream
      .range(0, params.total)
      .as(1)
      .chunkN(params.chunk)
      .evalMapChunk(c =>
        IO.sleep(params.time.second) *>
          IO.pure(c.toArray.mkString.toLong)
      )

    Ok(stream)
  }


  def router(counter: Counter[IO]): HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root / "counter" => serviceCounter(counter)
      case GET -> Root / "slow" / chunk / total / time  => {
        val params = SlowParamsToCheck(chunk, total, time).validate
        params.fold(c => BadRequest(c.toChain.toList.mkString(", ")), serviceSlow)
      }

    }

  val server: Resource[IO, Server] = for {
    counter <- Resource.eval(Ref.of[IO, Long](0))
    s <- EmberServerBuilder
      .default[IO]
      .withPort(Port.fromInt(8080).get)
      .withHost(Host.fromString("localhost").get)
      .withHttpApp(router(counter).orNotFound)
      .build
  } yield s

}
