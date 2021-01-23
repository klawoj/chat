
name := "chat"

version := "0.1"

val scalaV = "2.13.3"
val akkaV = "2.5.27"
val scalazV = "7.2.27"
val scalaCheckV = "1.14.1"
val scalaTestV = "3.1.0"
val mockitoV = "1.10.19"
val logBackV = "1.2.3"
val scalaTestMockitoV = "1.0.0-M2"
val scalaCollectionCompatV = "2.1.3"
val akkaHttpV = "10.1.11"
val scalaTestCheckV = "3.1.0.0"

lazy val chat = project
  .in(file("."))
  .settings(
    scalaVersion := scalaV,
    name := "chat",
    organization := "pl.klawoj",
    libraryDependencies ++= {
      Seq(

        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe.akka" %% "akka-stream" % akkaV,
        "com.typesafe.akka" %% "akka-stream-contrib" % "0.10",

        "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % Test,

        "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-caching" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
        "com.typesafe.akka" %% "akka-slf4j" % akkaV,
        "ch.qos.logback" % "logback-classic" % logBackV,

        "de.heikoseeberger" %% "akka-http-circe" % "1.30.0" excludeAll
          ExclusionRule(organization = "com.typesafe.akka"),
        "de.heikoseeberger" %% "akka-http-jsoniter-scala" % "1.31.0" excludeAll
          ExclusionRule(organization = "com.typesafe.akka"),
        "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0" excludeAll
          ExclusionRule(organization = "com.typesafe.akka"),

        "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.1.2" % Compile,
        "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.1.2" % Provided,


        "com.typesafe.akka" %% "akka-cluster" % akkaV,
        "com.typesafe.akka" %% "akka-cluster-sharding" % akkaV,
        "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
        "com.typesafe.akka" %% "akka-cluster-metrics" % akkaV,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.5",
        "com.typesafe.akka" %% "akka-discovery" % akkaV,


        "org.scalacheck" %% "scalacheck" % scalaCheckV % Test,
        "org.scalatest" %% "scalatest" % scalaTestV % Test,
        "org.scalatestplus" %% "scalatestplus-mockito" % scalaTestMockitoV % Test,
        "org.scalatestplus" %% "scalacheck-1-14" % scalaTestCheckV % Test,
        "org.mockito" % "mockito-core" % mockitoV % Test,
        "com.softwaremill.diffx" %% "diffx-scalatest" % "0.3.16" % Test,
        "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3",
        "org.scala-lang" % "scala-reflect" % scalaV,
        "org.scalaz" %% "scalaz-core" % scalazV,


        "com.typesafe.akka" %% "akka-slf4j" % akkaV,
        "ch.qos.logback" % "logback-classic" % logBackV
      )
    })