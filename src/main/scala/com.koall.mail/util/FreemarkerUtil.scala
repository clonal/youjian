package com.koall.mail.util

import java.io._

import freemarker.template.{Configuration, TemplateExceptionHandler}

import scala.collection.JavaConverters._

object FreemarkerUtil {
  val cfg = new Configuration(Configuration.VERSION_2_3_22)
  cfg.setClassForTemplateLoading(this.getClass(), "/template")
  cfg.setDefaultEncoding("UTF-8")
  cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)

  def html(tmpName: String, params: Map[String, String]): StringWriter => String = { out =>
    val template = cfg.getTemplate(tmpName, "UTF-8")

    template.process(params.asJava, out)
    out.flush()
    out.close()
    out.getBuffer.toString
  }
}

case class Sample(name: String, website: String ,link: String)