name := "ko-mail"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

lazy val akkaVersion = "2.5.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.apache.kafka" %% "kafka" % "1.1.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-http"   % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote"        % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"       % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib"       % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "ch.lightshed" %% "courier" % "0.1.4",
  "org.freemarker" % "freemarker" % "2.3.28"
)

/**
  以下代码需要新建plugins.sbt 放入project目录下 否则 sbt assembly不生效

resolvers += "bintray-sbt-plugins" at "http://dl.bintray.com/sbt/sbt-plugin-releases"
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
  **/