package org.sat4j.fuzzer;

import java.math.BigInteger;
import java.security.SecureRandom;

public class APIFuzzer {

    public static void main(String[] args) {

        SecureRandom rand = new SecureRandom();

        // Give seed for SecureRandom in comandline 
        if(args!= null && args.length > 0){
            rand.setSeed(Long.parseLong(args[0]));
        }

        // Generate Master Seed 64 bit
        byte[] masterSeed = rand.generateSeed(64);
        long value = new BigInteger(masterSeed).longValue();

        // Call TraceFactory and pass Master Seed
        TraceFactory.run(value, false, false);
    }

}
