#!/bin/sh

# This script will install the external libs to a local Maven repository

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JARS=$DIR/*.jar
for f in $JARS
do
  filename=$(basename "$f")
  name="${filename%.*}"
  if mvn install:install-file -Dfile=$f \
                         -DgroupId=org.ncbo \
                         -DartifactId=$name \
                         -Dversion=1.0 \
                         -Dpackaging=jar \
                         -DgeneratePom=true ; then
    echo "$name installed"
  else
    echo "$name install failed"
    exit
  fi
done

# Uncomment the following to generate dependency statements for the POM file
# for f in $JARS
# do
#   filename=$(basename "$f")
#   name="${filename%.*}"
#   echo "<dependency>
#     <groupId>org.ncbo</groupId>
#     <artifactId>$name</artifactId>
#     <version>1.0</version>
# </dependency>"
# done