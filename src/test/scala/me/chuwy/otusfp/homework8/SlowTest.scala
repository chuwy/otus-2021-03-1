package ru.otus.sgribkov.tankbattle.server.objects.scaladeveloper.homework8

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import org.http4s.Status.BadRequest
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Request, Response, Uri}
import org.scalatest.flatspec.AnyFlatSpec
import ru.otus.sgribkov.scaladeveloper.homework8.Restful
import scala.math.ceil


class SlowTest extends AnyFlatSpec {

  def responseSlow(request: Request[IO]): IO[Option[Response[IO]]] =
    Ref.of[IO, Long](0).flatMap { (c: Restful.Counter[IO]) =>
      val routes = Restful.router(c)
      routes.run(request).value
    }

  "check body length and execution time" should "ok" in {

    val chunk = 10
    val total = 29
    val time = 1
    val timeTotal = ceil(total.toDouble / chunk).toInt * time

    val uriSlow: Uri = Uri.unsafeFromString(s"http://localhost:8080/slow/$chunk/$total/$time")
    val requestSlow: Request[IO] = Request[IO](uri = uriSlow)

    val slowMetrics = for {
      start <- IO.realTime
      resp <- responseSlow(requestSlow).flatMap(r => IO.pure(r))
      body <- resp.get.as[String]
      end <- IO.realTime
      lnt = body.length
      time <- IO.pure((end - start).toSeconds)
    } yield (lnt, time)

    val result = slowMetrics.unsafeRunSync()
    println(result)

    assert(result._1 == total)
    assert(result._2 == timeTotal)
  }

  "check bad request" should "ok" in {

    val expectedMsg = """"chunk is not integer greater than 0, total is not integer greater than 0""""
    val requestSlow: Request[IO] = Request[IO](uri = uri"http://localhost:8080/slow/a/-1/1")

    val statusSlow = for {
      resp <- responseSlow(requestSlow).flatMap(r => IO.pure(r))
      msg <- resp.get.as[String]
      st = resp.get.status
    } yield (st, msg)

    val result = statusSlow.unsafeRunSync()

    assert(result._1 == BadRequest)
    assert(result._2 == expectedMsg)
  }

}
