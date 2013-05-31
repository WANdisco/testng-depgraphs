# testng-depgraphs

Reporter plug-in for TestNG that generates graph visualisations of test dependencies.

For each of your TestNG test suites, testng-depgraphs will generate a file that contains a dot language directed graph of all the test method and group dependencies that are specified in your code. If you have the GraphViz command line tools installed it will also generate a PNG image of the graph.

<p align="center">
  <img src="https://github.com/WANdisco/testng-depgraphs/blob/master/example.png?raw=true" alt="Portion of an example graph image"/>
</p>

Blue squares are test groups. Green, yellow and red squares are passed, skipped and failed test methods, respectively. The arrows show the direction of the dependencies.

## Usage

Simply add testng-depgraphs as a test dependency in your Maven POM file:

    <dependency>
      <groupId>com.wandisco</groupId>
      <artifactId>testng-depgraphs</artifactId>
      <version>1.0-SNAPSHOT</version>
      <scope>test</scope>
    </dependency>

Or if you are using Ant, just include the testng-depgraphs JAR in the run-time classpath for your tests.

Your project's test suite will automatically start generating dependency graphs through the magic of Java's service loader mechanism.

## Licence

This project is licenced under the [Apache Software License, Version 2.0][ASL2].

[ASL2]: http://www.apache.org/licenses/LICENSE-2.0
