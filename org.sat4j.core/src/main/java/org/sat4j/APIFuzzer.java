package org.sat4j;

import java.security.SecureRandom;

import org.sat4j.fuzzer.TraceFactory;

public class APIFuzzer {

    public static void main(final String[] args) {

        // Generate Master Seed
        final SecureRandom rand = new SecureRandom();
        final long masterSeed = rand.nextLong();
        TraceFactory.run(masterSeed);

    }

}
