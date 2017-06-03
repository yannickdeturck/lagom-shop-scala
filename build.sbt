organization in ThisBuild := "be.yannickdeturck"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.8"

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "3.3"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test
val cassandraDriverExtras = "com.datastax.cassandra" % "cassandra-driver-extras" % "3.0.0" // Adds extra codecs

lazy val `lagom-shop-scala` = (project in file("."))
  .aggregate(itemApi, itemImpl,
    frontend)
  .settings(commonSettings: _*)

lazy val common = (project in file("common"))
  .settings(commonSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslServer % Optional,
      playJsonDerivedCodecs,
      scalaTest
    )
  )

lazy val itemApi = (project in file("item-api"))
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val itemImpl = (project in file("item-impl"))
  .dependsOn(itemApi)
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      cassandraDriverExtras,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)

lazy val orderApi = (project in file("order-api"))
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val orderImpl = (project in file("order-impl"))
  .dependsOn(orderApi, itemApi)
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      cassandraDriverExtras,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)

lazy val frontend = (project in file("frontend"))
  .dependsOn(itemApi, orderApi)
  .settings(commonSettings: _*)
  .enablePlugins(PlayScala && LagomPlay)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      macwire,
      scalaTest,
      "org.webjars" % "foundation" % "6.2.3",
      "org.webjars" % "foundation-icon-fonts" % "d596a3cfb3"
    ),
    EclipseKeys.preTasks := Seq(compile in Compile)
  )

def commonSettings: Seq[Setting[_]] = Seq(
)

lagomCassandraCleanOnStart in ThisBuild := true