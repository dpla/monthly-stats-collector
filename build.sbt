ThisBuild / scalaVersion     := "2.13.7"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "dpla"
ThisBuild / organizationName := "Digital Public Library of America"

lazy val root = (project in file("."))
  .settings(
    name := "monthly-stats-collector",
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-core" % "4.0.4",
      "org.json4s" %% "json4s-jackson" % "4.0.4",
      "com.github.tototoshi" %% "scala-csv" % "1.3.10",
      "software.amazon.awssdk" % "s3" % "2.17.130"
    )
  )

