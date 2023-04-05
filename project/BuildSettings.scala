// SBT
import sbt._
import Keys._

// sbt-assembly
import sbtassembly._
import sbtassembly.AssemblyKeys._


/**
 * To enable any of these you need to explicitly add Settings value to build.sbt
 */
object BuildSettings {

  // Makes package (build) metadata available withing source code
  lazy val scalifySettings = Seq(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "settings.scala"
    IO.write(file, """package me.chuwy.otusfp.generated
                      |object ProjectMetadata {
                      |  val version = "%s"
                      |  val name = "%s"
                      |  val organization = "%s"
                      |  val scalaVersion = "%s"
                      |}
                      |""".stripMargin.format(version.value, name.value, organization.value, scalaVersion.value))
      Seq(file)
    }.taskValue
  )

  lazy val buildSettings = Seq[Setting[_]](
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full)
  )

  // sbt-assembly settings
  lazy val assemblySettings = Seq(
    assemblyJarName in assembly := { moduleName.value + "-" + version.value + ".jar" }
  )

  lazy val helpersSettings = Seq[Setting[_]](
    initialCommands := "import me.chuwy.otusfp._"
  )
}
