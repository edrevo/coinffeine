Coinffeine
==========

Coinffeine P2P exchange implementation.


Getting Started
---------------

To build Coinffeine you will need:

 * Java 8 (just Java 7 if you are not interested in `coinffeine-gui` module)
 * A working [SBT 0.13](http://www.scala-sbt.org/download.html)
 * Protobuf compiler 2.5.0. You can get it with your favourite package manager
   (`protobuf` in brew, `protobuf-java` in apt-get and macports) or directly
   fron its [project page](https://code.google.com/p/protobuf/downloads/list).

To compile all the modules and run their tests:

    sbt compile test

To run the peer from the sources:

    sbt gui/run

The application can be natively packaged by the following command. Note that
you should have installed the coresponding native tools such as RPM on Red Had
or the `fakeroot` package in Ubuntu/Debian.

    sbt gui/packageJavafx


Getting coverage metrics
------------------------

To get coverage metrics just run `sbt scoverage:test` and loot at the
directory `target/scala-2.10/scoverage-report` of every module.


Continuous Integration
----------------------

When building on a CI environment (e.g., Jenkins), it is recommended to define the
property `-Dconfig.resource=application-ci.conf`. This will make Akka to use an
alternative config file that introduces a dilation in the timeouts used by the test probes.
This is especially useful to avoid false errors in heavily loaded Jenkins servers.
