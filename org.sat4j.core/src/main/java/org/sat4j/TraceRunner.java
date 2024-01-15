package org.sat4j;

import org.sat4j.fuzzer.TraceFactory;

public class TraceRunner {

    public static void main(final String[] args) {

        // Give seed of Trace in comandline 
        String seedHEX = String.valueOf(args[0]);
        long value = Long.parseUnsignedLong(seedHEX, 16);
        TraceFactory.run(value, true);
    }

}
