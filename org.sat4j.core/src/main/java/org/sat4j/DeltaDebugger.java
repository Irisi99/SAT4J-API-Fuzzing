package org.sat4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class DeltaDebugger {

    public static void main(final String[] args) throws TimeoutException, ContradictionException {

        // Give seed of Trace in comandline 
        String seedHEX = String.valueOf(args[0]);

        try {
            List<String> content = Files.readAllLines(Paths.get("./traces/" + seedHEX + ".txt"));
            content.remove(0); //remove 'init' since we can't debug without initializing the solver

            String errorMessage = TraceRunner.runTrace(content, false);

            int granularity = 2;
            int size = content.size();
            int start = 0;
            int end = size;
            List<String> c;
            String output;
            boolean fileCreated = false;

            while(granularity < size){

                for(int i = 0; i < granularity; i++ ){

                    start = i * (int) (size/granularity);
                    end = start + (int) (size/granularity);

                    if(end > size || i == (granularity-1))
                        end = size;

                    c = content.subList(start, end);
                    output = TraceRunner.runTrace(c, false);

                    if(output != null && output.compareTo(errorMessage) == 0){
                        createFile(c, seedHEX);
                        fileCreated = true;
                    }
                }
                
                granularity = granularity * 2;
            }

            if(fileCreated){
                System.out.print("Created trace file " + seedHEX + "_db.txt");
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    private static void createFile(List<String> trace, String seedHEX){
        String path = "./traces/" + seedHEX + "_db.txt";
        final File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            final FileWriter myWriter = new FileWriter(path);
            for (int j = 0; j < trace.size(); j++)
                myWriter.write(trace.get(j) + "\n");
            myWriter.close();
        } catch (final IOException e) {
            e.printStackTrace(System.out);
        }
    }

}
