#!/bin/bash

nNodes=$1
ip=$2
port=$3

echo "deploying Node 1 at $ip : $port"
java -jar ../pub-sub-HyParView-assembly-0.1.jar $ip $port > ../logs/out$port.log 2 &

for run in $(seq 2 $nNodes)
do
	let myPort=$((port+run))
	echo "deploying Node $run at $ip : $myPort"
	java -jar ../pub-sub-HyParView-assembly-0.1.jar $ip $myPort $ip $port > ../logs/out$myPort.log 2 &
done