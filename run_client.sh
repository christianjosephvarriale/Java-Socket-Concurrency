#!/bin/bash

export CLASSPATH=.:lib/*
./build.sh

echo --- Running
F=tiny
SAMPLE_INPUT=sample_input/$F.txt
SAMPLE_OUTPUT=sample_output/$F.out
SERVER_OUTPUT=myoutput.txt

java Client localhost 10290 $SAMPLE_INPUT $SERVER_OUTPUT

echo --- Comparing server\'s output against sample output
sort -n -o $SERVER_OUTPUT $SERVER_OUTPUT
sort -n -o $SAMPLE_OUTPUT $SAMPLE_OUTPUT

diff $SERVER_OUTPUT $SAMPLE_OUTPUT
if [ $? -eq 0 ]; then
    echo Outputs match
else
    echo Outputs DO NOT match
fi
