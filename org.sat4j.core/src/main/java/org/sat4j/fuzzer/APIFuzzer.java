package org.sat4j.fuzzer;

import java.math.BigInteger;
import java.security.SecureRandom;

public class APIFuzzer {

    public static void main(String[] args) {

        SecureRandom rand = new SecureRandom();
        long masterSeed = 0;
        int nrTraces = 100;
        boolean isTraceSeed = false;
        boolean verbose = false;
        boolean skipProofCheck = false;

        // Give seed for SecureRandom and number of Traces to be generated in comandline 
        if(args!= null && args.length > 0){
            for(int i=0; i < args.length; i+=2){
                if(args[i].trim().equals("-s")){
                    try {  
                        masterSeed = Long.parseLong(args[i+1]);
                    } catch(NumberFormatException e){  
                        masterSeed = Long.parseUnsignedLong(args[i+1], 16);
                    } 
                } else if(args[i].trim().equals("-n")){
                    nrTraces = Integer.parseInt(args[i+1]);
                } else if(args[i].trim().equals("-v")){
                    verbose = true;
                    i--;
                } else if(args[i].trim().equals("-p")){
                    skipProofCheck = true;
                    i--;
                } else {
                    System.out.println("The only acceptable parameters are :");
                    System.out.println("    -s seed (long)");
                    System.out.println("    -n nrOfTraces (int)");
                    System.out.println("    -v to run it in verbose mode");
                    System.out.println("    -p to skip IDRUP Proof check");
                    System.exit(0);
                }
            }
        }

        // Generate Master Seed 64 bit if not provided
        if(masterSeed == 0){
            byte[] seedBytes = rand.generateSeed(64);
            masterSeed = new BigInteger(seedBytes).longValue();
        }

        // System.out.println("Master Seed: "+Long.toHexString(masterSeed));

        // Call TraceFactory and pass Master Seed
        TraceFactory.run(masterSeed, nrTraces, skipProofCheck, isTraceSeed, verbose);
    }

}
