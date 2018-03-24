#!/bin/sh
set -e

if [ ! -d "out" ]
then
    mkdir out
fi

cd out
rm -f *.class
javac -d . ../*.java

CMD="$1"
if [ "$CMD" == "--novisual" ] # No Visualization mode
then
    java NoVisualPlayerSkeleton
elif [ "$CMD" == "--buildonly" ] # Build only command
then
    : # Do nothing
else
    java PlayerSkeleton
fi
