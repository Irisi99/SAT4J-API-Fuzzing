package org.sat4j.fuzzer;

import java.math.BigInteger;
import java.security.SecureRandom;

public class APIFuzzer {

    public static void main(String[] args) {

        SecureRandom rand = new SecureRandom();
        int nrTraces = 100;

        // Give seed for SecureRandom and number of Traces to be generated in comandline 
        if(args!= null && args.length > 0){
            for(int i=0; i < args.length; i+=2){
                if(args[i].trim().equals("-s")){
                    rand.setSeed(Long.parseLong(args[i+1]));
                } else if(args[i].trim().equals("-n")){
                    nrTraces = Integer.parseInt(args[i+1]);
                } else {
                    System.out.println("The only acceptable parameters are :");
                    System.out.println("    -s seed (long)");
                    System.out.println("    -n nr of Traces (int)");
                    System.exit(0);
                }
            }
        }

        // Generate Master Seed 64 bit
        byte[] masterSeed = rand.generateSeed(64);
        long value = new BigInteger(masterSeed).longValue();
        // System.out.println(value);

        // Call TraceFactory and pass Master Seed
        TraceFactory.run(value, nrTraces, false, false);
    }

}
