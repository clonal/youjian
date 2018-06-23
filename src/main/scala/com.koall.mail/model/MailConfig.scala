package com.koall.mail.model

import spray.json._

import scala.io.Source

case class MailConfig(method: String, title: String, template: String)

object MailConfig {
  var mailConfigs: Map[String, Map[String, MailConfig]] = Map.empty

  def init() = {
    val files = Seq("mail_cn.json", "mail_en.json")
    files foreach { name =>
      val key = name.split("_").last.takeWhile(_ != '.')
      val mailConfig = loadConfig(name)
      mailConfigs += key -> mailConfig
    }
  }

  def loadConfig(fileName: String) = {
    val in = getClass().getResourceAsStream("/" + fileName)
    val source = Source.fromInputStream(in, "UTF-8")
    val str = source.mkString
    val json = str.parseJson
    val m = transform(json).asInstanceOf[Map[String, Any]]
    source.close
    m.map{
      case (k, v) =>
        val info = v.asInstanceOf[Map[String, Any]]
        k -> MailConfig(k, info("title").asInstanceOf[String], info("template").asInstanceOf[String])
    }
  }

  def transform(j: JsValue): Any = {
    def rec(v: JsValue): Any = (v: @unchecked) match {
      case JsObject(l) => l.foldLeft(Map[String, Any]()) {
          //          case (m, (name, _: JsUndefined)) => m
          case (m, (name, JsNull)) => m + (name -> null)
          case (m, (name, value)) => m + (name -> rec(value))
        }
      case JsArray(l) => l.map(rec).toList
      case JsString(value) => value
      case JsNumber(value) =>
        val doubleValue = value.doubleValue
        if (scala.math.abs(doubleValue - doubleValue.round) < 1e-7) {
          val longValue = doubleValue.round
          if (longValue != longValue.toInt)
            longValue
          else
            longValue.toInt
        }
        else
          doubleValue
      case JsBoolean(value) => value
      case JsNull => null
    }
    rec(j)
  }

  def getTemplate(locale: String, method: String) = {
    mailConfigs.get(locale) match {
      case Some(m) => m.get(method).map(_.template)
      case _ => None
    }
  }

  def getTitle(locale: String, method: String) = {
    mailConfigs.get(locale) match {
      case Some(m) => m.get(method).map(_.title).getOrElse("no title")
      case _ => "no title"
    }
  }
}
