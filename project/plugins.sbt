resolvers in ThisBuild ++= Seq(
  "sonatype-releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
  Resolver.sonatypeRepo("public"),
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.3.3")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.1.2")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.5.1")
