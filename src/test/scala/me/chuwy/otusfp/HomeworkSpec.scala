package me.chuwy.otusfp


import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import io.circe.Json
import io.circe.syntax.KeyOps
import me.chuwy.otusfp.homework.Main._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._
import org.specs2.mutable.Specification

class HomeworkSpec extends Specification with CatsEffect {

  import cats.effect.unsafe.IORuntime

  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  val httpApp: IO[HttpRoutes[IO]] = CounterService.apply.map(routes)

  "service" should {

    "counter" should {
      "increment on each request" in {
        val request: Request[IO] =
          Request(method = Method.GET, uri = uri"/counter")

        val expected = Json.obj(
          "counter" := 3
        )

        val actualResp: IO[Json] = for {
          app <- httpApp
          client <- IO.pure(Client.fromHttpApp(app.orNotFound))
          _ <- client.expect[Json](request)
          _ <- client.expect[Json](request)
          response <- client.expect[Json](request)
        } yield response

        actualResp.map(_ must beEqualTo(expected))
      }
    }



    "slow route" should {

      "return right json" in {

        val request: Request[IO] = Request(method = Method.GET, uri = uri"/slow/10/50/1")

        val expected: String = List.fill(10)("7").mkString

        val actualResp: IO[String] = for {
          app <- httpApp
          client <- IO.pure(Client.fromHttpApp(app.orNotFound))
          response <- client.expect[String](request)
        } yield response

        actualResp.map(_ must beEqualTo(expected))

      }
    }
  }
}
