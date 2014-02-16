Coinffeine Server Module
========================

How to create a standalone JAR:

    sbt
    > project server
    > assembly

Then you can find the JAR at
`target/scala-2.10/coinffeine-server-standalone.jar` and use it as:

    java -jar <jar path> [-p port]

Enjoy!

