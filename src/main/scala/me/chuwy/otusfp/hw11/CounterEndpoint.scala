package me.chuwy.otusfp.hw11

import cats.effect.{IO, IOApp, Ref, Resource}
import com.comcast.ip4s.{Host, Port}
import io.circe._
import io.circe.generic.semiauto._
import me.chuwy.otusfp.hw11.CounterEndpoint.Counter
import org.http4s.Method.GET
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, Request}

object CounterEndpoint {

  case class Counter(counter: Int)

  implicit val counterEncoder: Encoder[Counter] = deriveEncoder

  private def routes(counter: Ref[IO, Int]): HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "counter" => for {
      c <- counter.updateAndGet(_ + 1)
      r <- Ok(counterEncoder(Counter(c)).toString())
    } yield r
  }

  def router(counter: Ref[IO, Int]): HttpRoutes[IO] = Router("/" -> routes(counter))

  def server(counter: Ref[IO, Int]): Resource[IO, Server] = EmberServerBuilder.default[IO]
    .withPort(Port.fromInt(8080).get)
    .withHost(Host.fromString("localhost").get)
    .withHttpApp(router(counter).orNotFound)
    .build

}

object CounterMain extends IOApp.Simple {

  def run: IO[Unit] = for {
    cnt <- IO.ref(0)
    _ <- CounterEndpoint.server(cnt).use(_ => IO.never)
  } yield ()

}

object CounterTest extends IOApp.Simple {

  implicit val counterDecoder: Decoder[Counter] = deriveDecoder

  private val request: Request[IO] = Request(method = GET, uri = uri"/counter")

  def client(counter: Ref[IO, Int]): Client[IO] = Client.fromHttpApp(CounterEndpoint.router(counter).orNotFound)

  private val counterInit = 10
  private val expected = 11

  def run: IO[Unit] = for {
    counter <- IO.ref(counterInit)
    resp <- client(counter).expect[Json](request)
    _ <- resp.as[Counter] match {
      case Left(value) => IO.println(value.getMessage())
      case Right(value) => IO.println(s"Q: counter value is $expected?\nA: ${value.counter == expected}")
    }
  } yield ()

}
