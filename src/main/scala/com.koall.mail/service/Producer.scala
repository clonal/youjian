package com.koall.mail.service

import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.slf4j.LoggerFactory

object Producer {
  val prop = getProp
  val producer = init()
  final val log = LoggerFactory.getLogger("producer")

  def getProp: Properties = {
    val props = new Properties()
    val config = ConfigFactory.load().getConfig("producer")
    config.entrySet().forEach{ x =>
      props.put(x.getKey, x.getValue.unwrapped().toString)
    }
    props
  }

  def init(): KafkaProducer[String, String] = {
    val props = prop
    new KafkaProducer[String, String](props)
  }

  def sendMsg(msg: ProducerRecord[String, String]) = {
    log.info(s"send message ${msg.value()}, ${msg.topic()}")
    producer.send(msg)
  }

  def getTopic() = prop.getProperty("topic")
}
