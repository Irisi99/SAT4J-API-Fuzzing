Master Project: API Fuzzing for SAT4J

Code Coverage Report is generated with the following comand : mvn clean verify -Dtest=TestAPIGenerator#runAPIFuzzerForCoverage
To open overall coverage report go to : org.sat4j.core/target/site/jococo/index.html
There are also individual reports for each folder and file inside


To build the project use : ant sat

In order to run following comands you need to go to the created folder (dist/CUSTOM)

Basic Enumerator of solutions is run with following comand : java -cp org.sta4j.core.jar org/sat4j/fuzzer/BasicSolutionEnumerator file.cnf

Create 'traces' folder inside ant build folder for the following commands

API Fuzzer is run with following command : java -cp org.sta4j.core.jar org/sat4j/fuzzer/APIFuzzer

API Trace Runner is run with following command : java -cp org.sta4j.core.jar org/sat4j/fuzzer/TraceRunner argument
If the argument is a seed then it will run the TraceFactory and generate a trace file for it
If the argument is a file then it will run the trace on the file line by line

Delta Debugger is run with the following comand : java -cp org.sta4j.core.jar org/sat4j/fuzzer/DeltaDebugger argument
Where the argument is the name of the trace file
