package org.sat4j;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.sat4j.fuzzer.TraceFactory;

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
        // System.out.println("Number of used bits: " + (Long.SIZE - Long.numberOfLeadingZeros(value)));
        // System.out.println(value);
        
        // check the next 2 internal ones if they overlap
        TraceFactory.run(value, false, false);
    }

}
