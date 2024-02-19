package org.sat4j.fuzzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Trace {

    private String ID;
    private List<String> traceCalls;

    // Create Trace with seed 0
    public Trace() {
        this("0");
        traceCalls = new ArrayList<String>();
    }

    // Create Trace with seed from argument
    public Trace(String Id) {
        ID = Id;
        traceCalls = new ArrayList<String>();
    }

    // Set the seed of the Trace
    public void setId(String Id) {
        ID = Id;
    }

    // Return the seed of the Trace
    public String getId() {
        return ID;
    }

    // Add new API call to the Trace
    public void addToTrace(String call) {
        traceCalls.add(call);
    }

    // Remove API call from the Trace
    public void removeFromTrace(String call) {
        traceCalls.remove(call);
    }

    // Create Trace file
    public void toFile() {

        // Defines the path to the file - currently pointing to the 'trcaes' folder
        String path = "./traces/" + ID + ".txt";

        File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            FileWriter myWriter = new FileWriter(path);

            // Write all the API calls of the trace in the file seperated by a new line
            for (int i = 0; i < traceCalls.size(); i++)
                myWriter.write(traceCalls.get(i) + "\n");
            myWriter.close();
            
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        System.out.print("Created trace file " + ID + ".txt");
    }

}
