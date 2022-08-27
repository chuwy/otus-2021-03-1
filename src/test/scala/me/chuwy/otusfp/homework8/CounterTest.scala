package me.chuwy.otusfp.homework8

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Request, Response}
import org.scalatest.flatspec.AnyFlatSpec
import me.chuwy.otusfp.homework8.Restful

class CounterTest extends AnyFlatSpec {

  def responseCounter(request: Request[IO], currentCounter: Long): IO[Option[Response[IO]]] =
    Ref.of[IO, Long](currentCounter).flatMap { (c: Restful.Counter[IO]) =>
      val routes = Restful.router(c)
      routes.run(request).value
    }

  "check next counter value" should "ok" in {

    val expectedCounter = """{"counter":100}"""
    val requestCounter: Request[IO] = Request[IO](uri = uri"http://localhost:8080/counter")

    val bodyCounter = for {
      resp <- responseCounter(requestCounter, 99).flatMap(r => IO.pure(r))
      body <- resp.get.as[String]
    } yield body

    val result = bodyCounter.unsafeRunSync()

    assert(result === expectedCounter)
  }

}
