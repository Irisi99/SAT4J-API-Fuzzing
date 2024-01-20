Master Project: API Fuzzing for SAT4J

Basic Enumerator of solutions is run with following comand : java -cp org.sta4j.core.jar org/sat4j/BasicSolutionEnumerator file.cnf

API Fuzzer is run with following command : java -cp org.sta4j.core.jar org/sat4j/APIFuzzer

Create 'traces' folder inside ant build folder
API Trace Runner is run with following command : java -cp org.sta4j.core.jar org/sat4j/fuzzer/TraceRunner seed

Code Coverage is generated with the following comand : mvn clean verify -Dtest=TestAPIGenerator#runAPIFuzzerForCoverage
