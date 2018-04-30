# Lagom Shop Scala
This is a Lagom sample project using the Scala API (still work in progress).

The project contains the following services:
* *Item service* that serves as an API for creating and looking up items

The project also contains a frontend written in Play for working with items and orders.

## Setup
In order to run this project you need to have at least JDK 8 installed and sbt 0.13.13.
For more information about installing the prerequisites, consult the [Lagom documentation](https://www.lagomframework.com/documentation/1.3.x/scala/Installation.html).

Navigate to the root of the project and run `$ sbt`.

Start up the project by executing `$ runAll`.

Service are listed on [http://localhost:9008/services](http://localhost:9008/services).

Frontend is available on [http://localhost:9000](http://localhost:9000).

## Importing the project in an IDE
Import the project as an sbt project in your IDE.
For more information, consult the official Lagom documentation on importing the project in [IntelliJ](https://www.lagomframework.com/documentation/1.3.x/scala/IntellijSbt.html) and [Eclipse](https://www.lagomframework.com/documentation/1.3.x/scala/EclipseSbt.html).