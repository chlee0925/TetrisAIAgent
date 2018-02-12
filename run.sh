#!/bin/sh
set -e

rm -f *.class
javac *.java
java PlayerSkeleton
