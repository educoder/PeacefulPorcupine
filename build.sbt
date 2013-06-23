name := "PeacefulPorcupine"

version := "0.1"

scalaVersion := "2.10.1"

resolvers += "Typesafe" at "http://repo.typesafe.com/typesafe/releases/"

//resolvers += "Spray" at "http://repo.spray.io"

//libraryDependencies += "org.mongodb" %% "casbah" % "2.6.0"

//libraryDependencies += "org.cometd.java" %% "cometd-java-client" % "2.5.1"

//libraryDependencies += "org.cometd.java" %% "cometd-api" % "1.1.5"

libraryDependencies += "org.cometd.java" % "cometd-java-client" % "2.5.1"

libraryDependencies += "org.eclipse.jetty.aggregate" % "jetty-client" % "7.0.1.v20091125"

libraryDependencies += "org.cometd.java" % "cometd-websocket-jetty" % "2.5.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2-M3"

//libraryDependencies += "io.spray" % "spray-can" % "1.1-M7"

//libraryDependencies += "io.spray" % "spray-client" % "1.1-M7"

//libraryDependencies += "io.spray" %%  "spray-json" % "1.2.3"

//libraryDependencies += "com.fasterxml.jackson" %% "jackson-module-scala" % "2.1.3"

libraryDependencies += "org.igniterealtime.smack" % "smack" % "3.2.1"

libraryDependencies += "org.igniterealtime.smack" % "smackx" % "3.2.1"

//libraryDependencies += "com.github.scopt" %% "scopt" % "3.0.0"

//resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"

//libraryDependencies += "org.skife.com.typesafe.config" % "typesafe-config" % "0.3.0"

unmanagedJars in Compile <++= baseDirectory map { base =>
    val libs = base / "lib"
    val dirs = (libs / "batik") +++ (libs / "libtw") +++ (libs / "kiama")
    (dirs ** "*.jar").classpath
}
