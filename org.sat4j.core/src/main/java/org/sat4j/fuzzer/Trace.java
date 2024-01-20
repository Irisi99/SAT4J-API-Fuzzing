package org.sat4j.fuzzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Trace {

    private String ID;
    private List<String> traceCalls;

    public Trace() {
        this("0");
    }

    public Trace(String Id) {
        ID = Id;
        traceCalls = new ArrayList<String>();
    }

    public void setId(String Id) {
        ID = Id;
    }

    public String getId() {
        return ID;
    }

    public void addToTrace(String call) {
        traceCalls.add(call);
    }

    public void removeFromTrace(String call) {
        traceCalls.remove(call);
    }

    public void toFile() {
        String path = "./traces/" + ID + ".txt";

        File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            FileWriter myWriter = new FileWriter(path);
            for (int i = 0; i < traceCalls.size(); i++)
                myWriter.write(traceCalls.get(i) + "\n");
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        System.out.print("Created trace file " + ID + ".txt");
    }

}
