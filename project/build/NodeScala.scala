import sbt._

class NodeScalaConnector(info: ProjectInfo) extends DefaultProject(info){

        val scala_tools_releases = "scala-tools.releases" at "http://scala-tools.org/repo-releases"
        val scala_tools_snapshots = "scala-tools.snapshots" at "http://scala-tools.org/repo-snapshots"

        val protobuf = "com.google.protobuf" % "protobuf-java" % "2.3.0"
        val specs_2_8_0 = "org.scala-tools.testing" % "specs_2.8.0" % "1.6.5" % "test"
}