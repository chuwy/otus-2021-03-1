package fifth

import cats.effect.IO
import cats.effect._
import org.http4s.implicits._
import org.http4s.{Method, Request, Uri, _}
import org.specs2.mutable.Specification


class TestMain extends Specification {
  def execute_counter: (String, String) = {
    import cats.effect.unsafe.implicits.global

    val request: Request[IO] = Request[IO](Method.GET, Uri.uri("/counter"))
    val (counter_value1, counter_value2) = (
      for {
        ref <- Ref[IO].of(1)
        routes <- IO.pure(Main.counterService(ref))

        value1 <- routes.orNotFound.run(request).map(_.bodyText.compile.toVector.map(_.fold("")(_ + _))).flatten
        value2 <- routes.orNotFound.run(request).map(_.bodyText.compile.toVector.map(_.fold("")(_ + _))).flatten
      } yield (value1, value2)

      ).unsafeRunSync

    (counter_value1, counter_value2)
  }

  "Counter" should {
    "increase value every time" in {
      execute_counter must beEqualTo(("{\"counter\":1}", "{\"counter\":2}"))
    }
  }


  def execute(method: Method, uri: Uri): Response[IO] = {
    import cats.effect.unsafe.implicits.global
    Main.slowService.orNotFound.run(Request[IO](method, uri)).unsafeRunSync
  }

  "Slow" should {
    "accept positive numbers" in {
      execute(Method.GET, Uri.uri("/slow/1/1/1")).status.code must beEqualTo(200)
    }
    "deny negative zeros" in {
      execute(Method.GET, Uri.uri("/slow/0/1/1")).status.code must beEqualTo(400)
      execute(Method.GET, Uri.uri("/slow/1/0/1")).status.code must beEqualTo(400)
      execute(Method.GET, Uri.uri("/slow/1/1/0")).status.code must beEqualTo(400)
    }
    "deny negative numbers" in {
      execute(Method.GET, Uri.uri("/slow/-1/1/1")).status.code must beEqualTo(400)
      execute(Method.GET, Uri.uri("/slow/1/-1/1")).status.code must beEqualTo(400)
      execute(Method.GET, Uri.uri("/slow/1/1/-1")).status.code must beEqualTo(400)
    }
    "deny letters" in {
      execute(Method.GET, Uri.uri("/slow/a/1/1")).status.code must beEqualTo(400)
      execute(Method.GET, Uri.uri("/slow/1/b/1")).status.code must beEqualTo(400)
      execute(Method.GET, Uri.uri("/slow/1/1/c")).status.code must beEqualTo(400)
    }
  }
}
