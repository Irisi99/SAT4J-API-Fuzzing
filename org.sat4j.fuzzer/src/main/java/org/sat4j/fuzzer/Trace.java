package org.sat4j.fuzzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Trace {

    private long ID;
    private final List<String> traceCalls;

    public Trace() {
        this((long) 0);
    }

    public Trace(final long Id) {
        ID = Id;
        traceCalls = new ArrayList<String>();
    }

    public void setId(final long Id) {
        ID = Id;
    }

    public long getId() {
        return ID;
    }

    public void addToTrace(final String call) {
        traceCalls.add(call);
    }

    public void removeFromTrace(final String call) {
        traceCalls.remove(call);
    }

    public void toFile() {
        final String path = "./traces/" + ID + ".txt";

        final File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            final FileWriter myWriter = new FileWriter(path);
            for (int i = 0; i < traceCalls.size(); i++)
                myWriter.write(traceCalls.get(i) + "\n");
            myWriter.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        System.out.println("Created trace file " + ID + ".txt");
    }

}
