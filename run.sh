#!/bin/sh
set -e

if [ ! -d "out" ]
then
    mkdir out
fi

cd out
rm -f *.class
javac -d . ../*.java

if [ "$1" == "--novisual" ]
then
    java NoVisualPlayerSkeleton
else
    java PlayerSkeleton
fi
