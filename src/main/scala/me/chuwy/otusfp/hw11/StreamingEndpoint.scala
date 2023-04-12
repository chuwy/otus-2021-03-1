package me.chuwy.otusfp.hw11

import cats.data.{Kleisli, OptionT}
import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s.{Host, Port}
import fs2.{Chunk, Stream}
import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.{HttpMiddleware, Router, Server}
import org.http4s.{HttpRoutes, Request}

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt

object StreamingEndpoint {

  // HTTP эндпоинт /slow/:chunk/:total/:time выдающий искусственно медленный ответ, имитируя сервер под нагрузкой
  //
  // :chunk, :total и :time - переменные, которые пользователь эндпоинта может заменить числами, например запрос по
  // адресу /show/10/1024/5 будет выдавать body кусками по 10 байт, каждые 5 секунд пока не не достигнет 1024 байт.
  //
  // Содержимое потока на усмотрение учащегося - может быть повторяющийся символ, может быть локальный файл.
  // Неверные значения переменных (строки или отрицательные числа в переменных) должны приводить к ответу Bad Request

  private def getChunkOfBytes(size: Int) = Chunk(List.fill(size)('a').mkString)

  private val routes: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / chunk / total / time =>
      val chunkSize = chunk.toInt
      val t = time.toInt

      val stream = Stream.unfoldChunk(total.toInt)(i =>
        if (i > 0) {
          if (i >= chunkSize) Some(getChunkOfBytes(chunkSize) -> (i - chunkSize))
          else Some(getChunkOfBytes(i) -> 0)
        }
        else None
      )
        .evalTap(it => IO.sleep(t.second) *> IO.pure(it))

      Ok(stream)
  }

  // validation
  private sealed trait PathValidation {
    def errorMessage: String
  }

  private case object IsValidPath extends PathValidation {
    def errorMessage: String = "Path is invalid! Path should match \"^(\\/[0-9]+){3}$\""
  }

  private def validatePath(path: String): Either[PathValidation, String] =
    Either.cond(
      path.matches("^(\\/[0-9]+){3}$"),
      path,
      IsValidPath
    )

  // middleware
  private def validationMiddleware: HttpMiddleware[IO] =
    routes =>
      Kleisli { req =>
        validatePath(req.pathInfo.toString()) match {
          case Left(err) => OptionT.liftF(BadRequest(err.errorMessage))
          case Right(_) => routes(req)
        }
      }

  // inject middleware upon the route

  def router: HttpRoutes[IO] = Router("/show" -> validationMiddleware(routes))

  val server: Resource[IO, Server] = EmberServerBuilder.default[IO]
    .withPort(Port.fromInt(8080).get)
    .withHost(Host.fromString("localhost").get)
    .withHttpApp(router.orNotFound)
    .build

}

object StreamingMain extends IOApp.Simple {

  def run: IO[Unit] = StreamingEndpoint.server.use(_ => IO.never)

}

object StreamingTest extends IOApp.Simple {

  private val request: Request[IO] = Request(method = GET, uri"/show/10/128/1")

  private val client = Client.fromHttpApp(StreamingEndpoint.router.orNotFound)

  def run: IO[Unit] = for {
    _ <- IO.println("Test with /show/10/128/1")
    start <- IO.pure(Instant.now())
    chunks <- client.stream(request)
      .flatMap(_.body.chunks)
      .compile
      .toList
    end <- IO.pure(Instant.now())
    _ <- IO.println(s"Time: ${Duration.between(start, end).toSeconds}")
    _ <- IO.println(s"Chunks received: ${chunks.size}")
    bytes = chunks.flatMap(_.toList)
    _ <- IO.println(s"Bytes received: ${bytes.size}")
  } yield ()

}
