package me.chuwy.otusfp.homework

import cats.effect.{ExitCode, IO, IOApp, Ref}
import fs2.{Chunk, Stream}
import me.chuwy.otusfp.homework.Model.Counter
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.{HttpRoutes, Response}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.FiniteDuration

object Main extends IOApp {


  case class CounterService(ref: Ref[IO, Int]){
    def increment: IO[Counter] = ref.updateAndGet(_ + 1).map(Counter.apply)
  }

  object CounterService{
    def apply: IO[CounterService] = Ref[IO].of(0).map(x => CounterService(x))
  }

  def slowStream(chunk: Int, total: Int, time: Int): Stream[IO, String] = {
    Stream.emit(7)
      .repeatN(total)
      .chunkN(chunk)
      .metered[IO](FiniteDuration(time,TimeUnit.SECONDS))
      .evalMapChunk{
        chunk:Chunk[Int]=>
          IO.pure(chunk.toList.mkString)
      }

  }


  def routes(counter: CounterService): HttpRoutes[IO] = HttpRoutes.of[IO]{
    case GET -> Root / "counter" => Ok(counter.increment)

    case GET -> Root / "slow" / IntVar(chunk) / IntVar(total) / IntVar(time) =>

      val response: IO[Response[IO]] = for {
        chunkSize <- IO.fromOption(Some(chunk).filter(_ > 0))(new Exception("wrong chunk"))
        totalSize <- IO.fromOption(Some(total).filter(_ > 0))(new Exception("wrong total"))
        delay <- IO.fromOption(Some(time).filter(_ > 0))(new Exception("wrong time"))
        result <- Ok(slowStream(totalSize, chunkSize, delay))
      } yield result

      response.attempt.flatMap {
        case Left(error) => BadRequest(error.getMessage)
        case Right(value) => IO.pure(value)
      }
  }

  def server(counter: CounterService): BlazeServerBuilder[IO] =
    BlazeServerBuilder[IO](global)
      .bindHttp(port = 8080, host = "localhost")
      .withHttpApp(routes(counter).orNotFound)

  def run(args: List[String]): IO[ExitCode] =
    CounterService.apply.flatMap{ counter =>
      server(counter)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }

}
