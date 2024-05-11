package org.sat4j.fuzzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class DeltaDebugger {

    // try to remove literals from clauses as well
    // try to separate removing options and clauses 
    // try to shuffle and rename literals -> check if bug is still there or recover
    // try until no reduction is possible

    public static void main(final String[] args) throws TimeoutException, ContradictionException {

        // Get File name from comandline 
        String fileName = String.valueOf(args[0]);

        try {
            // Read all the API calls from the trace
            List<String> content = Files.readAllLines(Paths.get("./traces/" + fileName));
            String seedHEX = fileName.split(".txt")[0];
            
            // Remove 'init' since we can't run the calls without initializing the solver
            // we do this step every time and add it back to the trace in the end
            content.remove(0);

            // Run the full trace to get the error type
            String errorType = TraceRunner.runTrace(content, false);

            // Delta Debugging parameters
            int granularity = 2;
            int size = content.size();
            int section = (int) (size/granularity);
            int start = 0;
            int end = size;
            List<String> temp;
            String output;
            boolean reduced = false;
            boolean fileCreated = false;

            while(section >= 1){

                // Copy API calls into a temporary List
                temp = new ArrayList<String>(content);

                int i = 0;
                while (i * section < size){
                    start = i * section;
                    i++;
                    end = start + section;
                    if(end > size)
                        end = size;

                    System.out.print("size : "+size+" --- ");
                    System.out.print("section size: "+section+" --- ");
                    System.out.print("start: "+start+" --- ");
                    System.out.print("end: "+end+" --- ");

                    // Remove a section of the API calls in the temporary list
                    for(int j = start; j < end; j++){
                        temp.set(j, null);
                    }

                    // Store the error of the API calls if there is any
                    output = TraceRunner.runTrace(temp, false);

                    // Compare the errors and only reduce the trace if the error is the same
                    if(output != null && output.compareTo(errorType) == 0){
                        reduced = true;
                        System.out.println("reduced: true");

                    // If the errors are different then restore the section of API calls that were removed
                    } else {
                        for(int j = start; j < end; j++){
                            temp.set(j, content.get(j));
                        }
                        System.out.println("reduced: false");
                    }
                }

                if(reduced){
                    fileCreated = true;
                    reduced = false;
                    temp.removeAll(Collections.singletonList(null));
                    int old_size = content.size();

                    // Update the main list of API calls
                    content = new ArrayList<String>(temp);

                    // Create the new file with the reduced trace
                    createFile(content, seedHEX);

                    // Update the size parameter and calculate the new section size if section size is not down to 1
                    size = content.size();
                    if(section > 1){
                        if((int) (size/granularity) == 0)
                            section = 1;
                        else
                            section = (int) (size/granularity);
                    }
                    // If section size is down to 1 but we are still reducing then go over the API calls until we can't remove any
                    else if(old_size == size)
                        break;

                // If trace was not reduced then increase granularity and calculate the new section size
                }else {
                    granularity = granularity * 2;
                    if( section > 1 && (int) (size/granularity) == 0)
                        section = 1;
                    else
                        section = (int) (size/granularity);
                }
            }

            if(fileCreated){
                System.out.print("Created trace file " + seedHEX + "_dd.txt");
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    // Mehtod to create file with remining API calls from the trace after Delta Debugging
    private static void createFile(List<String> trace, String seedHEX){

        // Method is similar to that of Trace.toFile except the fact that 
        // the file name now contains a postfix of '_dd' to distinguish it from the original trace file
        String path = "./traces/" + seedHEX + "_dd.txt";

        final File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            final FileWriter myWriter = new FileWriter(path);
            myWriter.write("1 init\n");
            for (int j = 0; j < trace.size(); j++){
                myWriter.write(trace.get(j) + "\n");
            }
            myWriter.close();
        } catch (final IOException e) {
            e.printStackTrace(System.out);
        }
    }

}
