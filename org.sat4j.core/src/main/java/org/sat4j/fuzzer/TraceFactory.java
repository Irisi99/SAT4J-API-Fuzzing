package org.sat4j.fuzzer;

import java.util.Random;
import java.util.Arrays;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

public class TraceFactory {

    static Random slaveRandomGenerator;
    static int MAXVAR;
    static int LENGTHCLAUSE;
    static int NBCLAUSES;
    static double coeficient;
    static ISolver solver;
    static int index;
    // sanity check - verbose - print all variables you are choosing
    static Boolean verbose = true;

    public static void run(final long seed) {

        //  The Random class uses a 48-bit seed - HEX number?
        final Random masterRandomGenerator = new Random(seed);

        int iterations = 0;
        // STATS
        int SATinstances = 0;
        int UNSATinstances = 0;
        long LearnedClauses = 0;        
        long NrConflicts = 0;
        long SolverRunTime = 0;
        long startTime = 0;
        long endTime = 0;

        while (iterations < 500) {

            iterations++;

            // Generate Slave Seed
            final long slaveSeed = masterRandomGenerator.nextLong();
            slaveRandomGenerator = new Random(slaveSeed); 
            // test that two seeds don't give the same sequence

            if(verbose){
                System.out.println("Number of used bits: " + (Long.SIZE - Long.numberOfLeadingZeros(slaveSeed)));
                System.out.println(slaveSeed);
            }
            
            MAXVAR = 0;
            NBCLAUSES = 0;
            // (3.8/4.5) * MAXVAR - about 50/50 SAT/UNSAT
            coeficient = 4.3;
            // flip a coin between only 3 and 3-9
            if(slaveRandomGenerator.nextBoolean()){
                LENGTHCLAUSE = 3;
            } else {
                // Change length of individual clauses as well
                // Uniform or not - 1 to max length
                // power law on length of the clause by 1/3
                // analyse how the solver behaves
                LENGTHCLAUSE = 3 + slaveRandomGenerator.nextInt(6);
                if(verbose){
                    System.out.println("LENGTHCLAUSE :" + LENGTHCLAUSE);
                }
            }

            // Higher Length of clause means more SAT (shorter solve time, less conflicts and less learned clauses)
            // Higher coeficient (more clauses) means more UNSAT

            final Trace trace = new Trace(slaveSeed);

            index = 1;
            trace.addToTrace(index + " init");
            index++;

            solver = SolverFactory.newDefault();

            // Incremental - add clauses - solve - repeat x 3 (increase number of variables and clauses)
            int increment = 0;
            while(increment < 3){
                increment ++;

                final int OLDMAXVAR = MAXVAR;
                final int OLDNBCLAUSES = NBCLAUSES;
                MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR; // add 20 - 200
                NBCLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR)) - OLDNBCLAUSES;

                if(verbose){
                    System.out.println("MAXVAR: " + MAXVAR);                    
                    System.out.println("NBCLAUSES: " + NBCLAUSES);
                }

                try{
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

                    startTime = System.currentTimeMillis();
                    final Boolean isSAT= solver.isSatisfiable();                    
                    endTime = System.currentTimeMillis();

                    if (isSAT) {
                        // final int[] assumption = Arrays.copyOf(solver.modelWithInternalVariables(), solver.modelWithInternalVariables().length);
                        if(increment == 3){
                            SATinstances++;
                            if(verbose){
                                System.out.println("SATISFIABLE!");
                                // System.out.println("Solution : " + Arrays.toString(assumption).substring(1, Arrays.toString(assumption).length() - 1) + "\n");
                            }
                        }
                    } else{
                        UNSATinstances ++;
                        if(verbose){
                            System.out.println("UNSATISFIABLE!");
                        }
                        break;
                    }

                } catch (final Exception e) {
                    System.out.println("Inisde Exception from isSatisfiable()");
                    System.out.println(e.getMessage());
                    trace.toFile();
                    return;
                }
            }

            final Map<String, Number> stats = solver.getStat();
            // System.out.println(stats.keySet());
            LearnedClauses += (long) stats.get("learnedclauses");            
            NrConflicts += (long) stats.get("conflicts");
            SolverRunTime += endTime - startTime;

            if(verbose){
                trace.toFile();
                break;
            }
        }

        // How many SAT? How long does it take to run the solver? Number of conflicts/learned clauses?
        System.out.println("SAT Instances : " + SATinstances);
        System.out.println("UNSAT Instances : " + UNSATinstances);        
        System.out.println("Average Leanred Clauses : " + LearnedClauses/iterations);        
        System.out.println("Average Nr Conflicts : " + NrConflicts/iterations);
        System.out.println("Average Solver Run Time : " + SolverRunTime/iterations + " milli sec");

    }

    private static void addClauses(final Trace trace) throws ContradictionException{

        for (int i = 0; i < NBCLAUSES; i++) {

            final int[] clause = new int[LENGTHCLAUSE];
            
            for (int j = 0; j < LENGTHCLAUSE; j++) {
                clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                while (clause[j] == 0 || isAlreadyPresent(clause, j)) {
                    clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR + 1)) - (MAXVAR + 1);
                }
            }

            trace.addToTrace(index + " addClause " + Arrays.toString(clause));
            // clauses are just for development purpopses, remove later
            index++;
            solver.addClause(new VecInt(clause));
        }

    }

    // Make sure all lits in the clause are all different from each other
    public static Boolean isAlreadyPresent(final int[] clause, final int index){
        for(int i = 0; i < index; i++){
            if(clause[i] == clause[index])
                return true;
        }
        return false;
    }

}


// TO DO:
// Plant bugs? - Mutation Testing?
// Coverage?
// Need to add Options later - configure solver, not just the default one - Check Launcher.java inside 'org.sat4j.sat' folder