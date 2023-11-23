package org.sat4j.fuzzer;

import java.util.Random;
import java.util.Arrays;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

public class TraceRunner {

    static Random slaveRandomGenerator;
    static int MAXVAR;
    static int LENGTHCLAUSE;
    static int NBCLAUSES;
    static double coeficient;
    static ISolver solver;
    static int index;

    public static void main(final String[] args) {
        if(args.length > 0){
            // add seed in comand line to reproduce trace
            run(Long.parseLong(args[0]));
        }
    }

    public static void run(final long seed) {

        slaveRandomGenerator = new Random(seed);

        MAXVAR = 0;
        NBCLAUSES = 0;
        coeficient = 4;
        if(slaveRandomGenerator.nextBoolean()){
            LENGTHCLAUSE = 3;
        } else {
            LENGTHCLAUSE = 3 + slaveRandomGenerator.nextInt(6);
        }

        final Trace trace = new Trace(seed);

        index = 1;
        trace.addToTrace(index + " init");
        index++;

        solver = SolverFactory.newDefault();

        int increment = 0;
        while(increment < 3){
            increment ++;

            final int OLDMAXVAR = MAXVAR;
            final int OLDNBCLAUSES = NBCLAUSES;
            MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR;
            NBCLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR)) - OLDNBCLAUSES;

            try {
                addClauses(trace);
            } catch (final Exception e) {
                System.out.println("Inisde Exception from addClause()");
                System.out.println(e.getMessage());
                trace.toFile();
                return;
            }
            
            try {
                trace.addToTrace(index + " solve");
                index++;

                final Boolean isSAT = solver.isSatisfiable();
                
                if (!isSAT) {
                    break;
                }

            } catch (final Exception e) {
                System.out.println("Inisde Exception from isSatisfiable()");
                System.out.println(e.getMessage());
                trace.toFile();
                return;
            }
        }
        trace.toFile();
    }


    private static void addClauses(final Trace trace) throws ContradictionException{

        for (int i = 0; i < NBCLAUSES; i++) {

            final int[] clause = new int[LENGTHCLAUSE];

            for (int j = 0; j < LENGTHCLAUSE; j++) {
                clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                while (clause[j] == 0 || TraceFactory.isAlreadyPresent(clause, j)) {
                    clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR + 1)) - (MAXVAR + 1);
                }
            }

            trace.addToTrace(index + " addClause " + Arrays.toString(clause));
            index++;
            solver.addClause(new VecInt(clause));
        }
    }

}