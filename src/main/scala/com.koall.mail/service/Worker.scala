package com.koall.mail.service

import java.io.{File, StringWriter}
import java.sql.Date
import java.time.LocalDateTime

import akka.actor.{Actor, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.koall.mail.model.{EMailTask, MailConfig, Record}
import com.koall.mail.util.{DBUtil, FreemarkerUtil, MailUtil}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class Worker extends Actor {
  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  final val SEND_SUCCESS = 1
  final val SEND_FAIL = 0

  final val MAX_RETRY = 10

  final val logger = LoggerFactory.getLogger("worker")

  override def receive: Receive = {
    case task: EMailTask => doWork(task)
    case _ => logger.info("receive unknown message!")
  }

  def doWork(task: EMailTask, retry: Int = 0): Unit = {
    send(task).andThen{
      case Success(_) =>
        store(task, SEND_SUCCESS).onComplete{
          case Success(_) =>
           logger.info(s"发送成功 : ${task.method} -> ${task.json} on ${LocalDateTime.now}")
           sender ! PoisonPill
          case Failure(t) =>
            logger.info(s"存储失败: ${task.method} -> ${task.json} on ${LocalDateTime.now}, Cause: ${t.getMessage}")
            sender ! PoisonPill
        }
      case Failure(t) =>
        t.getMessage match {
          case "send to nobody" | "wrong method" =>
            logger.info(s"发送失败 ${task.method} -> ${task.json} on ${LocalDateTime.now} with ${t.getMessage}")
            context.stop(self)
          case _ =>
            logger.info(s"开始重试 ${task.method} -> ${task.json} on ${LocalDateTime.now} with ${t.getMessage}")
            if (retry > MAX_RETRY) {
              logger.info(s"发送失败: ${task.method} -> ${task.json} on ${LocalDateTime.now}")
              sender ! PoisonPill
            } else {
              doWork(task, retry + 1)
            }
        }
    }
  }

  def send(mail: EMailTask): Future[_] = {
    val method = mail.method
    logger.info(s"${context.self} 开始工作 ${mail.method} -> ${mail.json} on ${LocalDateTime.now}")
    val params = mail.json
    val locale = params.getOrElse("lang", "cn")
    params.get("to") match {
      case Some(to) =>
        MailConfig.getTemplate(locale, method).map { template =>
          val html = FreemarkerUtil.html(template, params)(new StringWriter())
          MailUtil.sendHtmlMail(to, params.getOrElse("cc", ""),
            MailConfig.getTitle(locale, method), html)
        } match {
          case Some(f) =>
//            println(s"发送完毕! ")
//            logger.info(s"发送完毕! ")
            f
          case _ =>
//            println(s"发送失败1! ")
//            logger.info(s"发送失败1! ")
            logger.info("wrong method")
            Future.failed(new RuntimeException("wrong method"))
        }
      case None =>
        logger.info("send to nobody")
        Future.failed(new RuntimeException("send to nobody"))
    }
  }

  def store(task: EMailTask, success: Int) = {
    logger.info(s"${context.self} 开始存储 ${task.method} -> ${task.json} on ${LocalDateTime.now}")
    val params = task.json
     fetchId().flatMap {
       case Some(s) =>
         logger.info(s"${context.self} 开始获取唯一ID $s on ${LocalDateTime.now}")
         val record = Record(s.toLong, params.getOrElse("name", ""),
           params.getOrElse("to", ""), task.method, params.toJson(StringMapJsonFormat).compactPrint,
           success, new Date(System.currentTimeMillis()))
         DBUtil.store(record)
       case _ =>
         logger.info(s"获取唯一ID失败 on ${LocalDateTime.now}")
         Future.failed(new RuntimeException("no msg id fetched!"))
     }
  }

  def fetchId() = {
    val config = ConfigFactory.load()

    val responseFuture: Future[HttpResponse] = Http().
      singleRequest(HttpRequest(uri = config.getString("idService")))

    responseFuture.flatMap {
      case res =>
        implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
          EntityStreamingSupport.json()
        Unmarshal(res.entity).to[String].map { jsonString =>
          val j = jsonString.parseJson.convertTo[Map[String, String]](StringMapJsonFormat)
          j.get("msg")
        }
    }
  }

  implicit object StringMapJsonFormat extends RootJsonFormat[collection.immutable.Map[String, String]] {
    def write(c: collection.immutable.Map[String, String]) = {
      c.foldLeft(JsObject())((obj, entry) => JsObject(obj.fields + (entry._1 -> JsString(entry._2))))
    }
    def read(value: JsValue) = {
      value.asJsObject.fields map {
        case (k, js: JsString) => (k, js.value)
        case (k, js) => (k, js.toString)
      }
    }
  }
}

case class CompletedTask(task: EMailTask) extends Serializable
case class FailedStore(task: EMailTask, msg: String) extends Serializable
case class FailedSend(num: Int, task: EMailTask, msg: String) extends Serializable
case class FailedTask(task: EMailTask) extends Serializable