package com.koall.mail.service

import com.koall.mail.model.MailConfig
import org.slf4j.LoggerFactory

object MailService {
  final val logger = LoggerFactory.getLogger("mailService")

  def start(): Unit = {
    MailConfig.init()
    val seq = Consumer.prop.getProperty("topics").split(",").toSeq
    Consumer.consume(seq)
  }
}
