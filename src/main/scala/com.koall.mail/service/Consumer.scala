package com.koall.mail.service

import java.time.LocalDateTime
import java.util.Properties

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.koall.mail.model.{EMailTask, MailJob}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.duration._

object Consumer {
  val prop = getProp
  val consumer: KafkaConsumer[String, String] = init()
  final val logger = LoggerFactory.getLogger("consumer")


  def getProp: Properties = {
    val props = new Properties()
    val config = ConfigFactory.load().getConfig("consumer")
    config.entrySet().forEach{ x =>
      props.put(x.getKey, x.getValue.unwrapped().toString)
    }
    props
  }

  def getTopics: Array[String] = {
    prop.getProperty("topics").split(",")
  }

  def init(): KafkaConsumer[String, String] = {
    val props = Consumer.prop
    new KafkaConsumer[String, String](props)
  }

  def consume(seq: Seq[String]) = {
    import scala.collection.JavaConversions._
    logger.info(s"kafka consumer start at ${LocalDateTime.now}!")
    consumer.subscribe(seq)
    while (true) {
      val records = consumer.poll(100)
      for (record <- records) {
        startMailJob(MailJob(record.key, record.value))
        logger.info(s"将kafka队列消息转化为MailJob:  ${record.key} -> ${record.value} at ${LocalDateTime.now}!")
      }
    }
  }

  def startMailJob(job: MailJob): Unit = {
    logger.info("Raw message: value=" + job.json)
    val json = job.json.parseJson.asJsObject.fields map {
      case (k, v) => (k ,v.asInstanceOf[JsString].value)
    }
    implicit val timeout = Timeout(10 seconds)
    val worker = ActorSystem().actorOf(Props[Worker], "worker")
    worker ! EMailTask(job.tp, json)
  }
}
