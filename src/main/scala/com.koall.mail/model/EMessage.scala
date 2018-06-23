package com.koall.mail.model

trait EMessage extends Serializable

case class StartConsume(topics: Seq[String]) extends EMessage

case class MailJob(tp: String, json: String) extends EMessage

case class Store() extends EMessage

case class Failed() extends EMessage

case object Registration extends Serializable

case class EMailTask(method: String, json: Map[String, String]) extends Serializable