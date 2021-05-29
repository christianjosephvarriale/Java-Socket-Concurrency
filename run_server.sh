#!/bin/bash

export CLASSPATH=.:lib/*
./build.sh

echo --- Running
echo randomly chose port 10290
taskset -c 0,1 java -Xmx1g Server 10290
