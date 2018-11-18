cat ../rawoutput/* > ../results/results.csv

java -cp ../pub-sub-HyParView-assembly-0.1.jar ComputeStats
