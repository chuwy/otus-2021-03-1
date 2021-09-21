package fifth

import cats.effect._
import cats.syntax.all._
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.blaze.server._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._


object Main extends IOApp {
  def counterService(rr: Ref[IO, Int]): HttpRoutes[IO] = {
    case class Counter(counter: Int)

    HttpRoutes.of[IO] {
      case GET -> Root / "counter" =>
        for {
          n <- rr.modify(x => (x + 1, x))
          resp <- Ok(Counter(n).asJson)
        } yield resp
    }
  }

  def slowService: HttpRoutes[IO] = {
    def stream(chunk: Int, total: Int, time: Int): Stream[IO, String] = {
      (Stream.awakeEvery[IO](time.second) zipRight Stream.emits('0' to '9')).map(_.toString).repeat.take(total).chunkN(chunk).map(v => v.toList.fold("")(_ + _) + "\n")
    }

    HttpRoutes.of[IO] {
      case GET -> Root / "slow" / chunk / total / time => {
        try {
          val a = chunk.toInt
          val b = total.toInt
          val c = time.toInt

          if (a <= 0)
            BadRequest("It happens")
          else if (b <= 0)
            BadRequest("It happens")
          else if (c <= 0)
            BadRequest("It happens")
          else
            Ok(stream(a, b, c))
        }
        catch {
          case _: Throwable => BadRequest("It happens")
        }
      }
    }
  }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      ref <- Ref[IO].of(1)
      services = counterService(ref) <+> slowService

      exit_code <- BlazeServerBuilder[IO](global)
        .bindHttp(8080, "localhost")
        .withHttpApp(services.orNotFound)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exit_code
  }
}
