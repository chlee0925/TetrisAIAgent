#!/bin/sh
set -e

if [ ! -d "out" ]
then
    mkdir out
fi

cd out
rm -f *.class
javac -d . ../*.java
java PlayerSkeleton
