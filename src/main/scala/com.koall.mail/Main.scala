package com.koall.mail

import com.koall.mail.service.{MailService, RestService}

object Main {
  def main(args: Array[String]): Unit = {
    RestService.start()
    MailService.start()
  }
}