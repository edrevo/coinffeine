Coinffeine
==========

Coinffeine prototype implementation.

Getting Started
---------------

To build Coinffeine you will need a Java 7 environment with a working SBT 0.13.

Continous Integration
---------------------

When building on a CI environment (e.g., Jenkins), it is recommended to define the
property `-Dconfig.resource=application-ci.conf`. This will make Akka to use an
alternative config file that introduces a dilation in the timeouts used by the test probes.
This is especially useful to avoid false errors in heavily loaded Jenkins servers.
