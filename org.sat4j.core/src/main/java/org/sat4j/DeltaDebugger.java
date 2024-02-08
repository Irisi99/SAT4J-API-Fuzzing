package org.sat4j;

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

    public static void main(final String[] args) throws TimeoutException, ContradictionException {

        // Give name of File in comandline 
        String fileName = String.valueOf(args[0]);

        try {
            List<String> content = Files.readAllLines(Paths.get("./traces/" + fileName));
            String seedHEX = fileName.split(".txt")[0];
            content.remove(0); //remove 'init' since we can't debug without initializing the solver

            String errorType = TraceRunner.runTrace(content, false);

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

                temp = new ArrayList<String>(content);;

                for(int i = 0; i < granularity; i++ ){

                    start = i * section;
                    end = start + section;
                    if(end > size)
                        end = size;

                    System.out.print("section size: "+section+" --- ");
                    System.out.print("start: "+start+" --- ");
                    System.out.print("end: "+end+" --- ");

                    for(int j = start; j < end; j++){
                        temp.set(j, null);
                    }
                    // temp.removeAll(content.subList(start, end));
                    // match the exception class not the error message
                    output = TraceRunner.runTrace(temp, false);

                    if(output != null && output.compareTo(errorType) == 0){
                        reduced = true;
                        System.out.println("reduced: true");
                    } else {
                        for(int j = start; j < end; j++){
                            temp.set(j, content.get(j));
                        }
                        // temp.addAll(content.subList(start, end));
                        System.out.println("reduced: false");
                    }
                }

                if(reduced){
                    fileCreated = true;
                    reduced = false;
                    temp.removeAll(Collections.singletonList(null));
                    int old_size = content.size();
                    content = new ArrayList<String>(temp);
                    createFile(content, seedHEX);
                    size = content.size();
                    if(section > 1){
                        section = (int) (size/granularity);
                    }
                    else if(old_size == size)
                        break;
                }else {
                    granularity = granularity * 2;
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

    private static void createFile(List<String> trace, String seedHEX){
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
