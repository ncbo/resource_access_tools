#!/bin/sh

ant all
mvn package
cp target/resource-access-tools-1.0-SNAPSHOT-jar-with-dependencies.jar dist
