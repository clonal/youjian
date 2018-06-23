package com.koall.mail.service

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

object RestService {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  final val log = LoggerFactory.getLogger("webserver")

  def start() {
    val conf = ConfigFactory.load()
//    val conf = ConfigFactory.load("webserver.properties")
    val port = conf.getInt("restPort")
    //TODO shutdown message
    val services: Flow[HttpRequest, HttpResponse, Any] = path("sendMail" / """\w+""".r) { key =>
      post {
        entity(as[String]) { json =>
          Producer.sendMsg(new ProducerRecord[String, String](Producer.getTopic(),
            key, json))
          complete(HttpEntity(ContentTypes.`application/json`, """{"code": 1, "msg": "success"}"""))
        }
      }
    }

    Http().bindAndHandle(services, "0.0.0.0", port.toInt)
    log.info(s"Server start on port $port at ${LocalDateTime.now}")
  }
}