# Functional Pebbles for Java
[![License](https://img.shields.io/:license-MIT-blue.svg)](https://raw.githubusercontent.com/priimak/functional-pebbles/master/LICENSE)
![Build](https://github.com/priimak/functional-pebbles/workflows/Java%20CI%20with%20Gradle/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.devfortress.functional.pebbles/functional-pebbles-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22xyz.devfortress.functional.pebbles%22%20AND%20a:%22functional-pebbles-core%22)

Functional pebbles is a set of classes intended to facilitate programming functional style 
in Java. Many of the classes copy functionality from similarly named classes in Scala, 
others are static functions that mimic built-in Scala functionality. Unlike in well 
known [vavr](https://www.vavr.io/) library _Functional Pebbles_ classes behave exactly as 
they do in Scala. They are:

1. `Try` (aka Error) monad.
2. Composable `TryFunction`
3. `Tuple` pair of values holding class.
4. `Collectionz` - additional utilities for collections.

For more information see [Functional Pebbles Wiki](https://github.com/priimak/functional-pebbles/wiki).

To add as a dependency. In maven do
```sh
<dependency>
  <groupId>xyz.devfortress.functional.pebbles</groupId>
  <artifactId>functional-pebbles-core</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
and in gradle
```sh
implementation 'xyz.devfortress.functional.pebbles:functional-pebbles-core:1.0.0'
```
