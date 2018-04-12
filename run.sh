#!/bin/sh
set -e

mkdir -p training
mkdir -p out
mkdir -p generations
cd out
rm -f *.class
javac -d . ../*.java
CMD="$1"
if [ "$CMD" == "--novisual" ] # No Visualization mode
then
    java NoVisualPlayerSkeleton "${@:2}"
elif [ "$CMD" == "--train" ] # Train
then
    cd ..
    DATE=$(date +%s)
    mkdir -p generations/$(echo $DATE)
    nohup python -u tetris_trainer.py $(echo $DATE) > training/training-$(echo $DATE).log 2>&1 &
    if [ "$2" == "-f" ]
    then
        tail -f training/training-$(echo $DATE).log
    fi
elif [ "$CMD" == "--killtrain" ]
then
    kill $(ps aux | grep tetris_trainer.py | awk 'END{print $2}' | sort | head -n 1)
elif [ "$CMD" == "--buildonly" ] # Build only command
then
    : # Do nothing
else
    java PlayerSkeleton "$@"
fi
