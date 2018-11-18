#!/bin/bash

rm ../rawoutput/*
rm ../results/*
rm ../logs/*

nNodes=$1
ip=$2
port=$3

echo "deploying Node 0 at $ip : $port"
java -cp ../pub-sub-HyParView-assembly-0.1.jar Remote 0 $ip $port > ../logs/out$port.log 2 &

for run in $(seq 1 $((nNodes-1)))
do
	let myPort=$((port+run))
	echo "deploying Node $run at $ip : $myPort"
	java -cp ../pub-sub-HyParView-assembly-0.1.jar Remote $run $ip $myPort $ip $((myPort-1)) > ../logs/out$myPort.log 2 &
done

