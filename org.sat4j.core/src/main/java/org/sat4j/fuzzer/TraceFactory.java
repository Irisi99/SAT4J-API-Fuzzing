package org.sat4j.fuzzer;

import java.util.Random;
import java.util.Arrays;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ISolver;

public class TraceFactory {

    static Random slaveRandomGenerator;
    static int MAXVAR;
    static int LENGTHCLAUSE;
    static int NBCLAUSES;
    static double coeficient;
    static ISolver solver;
    static int index;

    public static void run(final long seed) {

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

            MAXVAR = slaveRandomGenerator.nextInt(181) + 20; // 20 - 200
            //(3.8/4.5) 4.26 * MAXVAR - about 50/50 SAT/UNSAT
            coeficient = 4;
            NBCLAUSES = (int) (coeficient * MAXVAR);
            LENGTHCLAUSE = 3; // 3 + slaveRandomGenerator.nextInt(3) - 3-sat (and above)

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
                addClauses(trace);

                final int OLDMAXVAR = MAXVAR;
                final int OLDNBCLAUSES = NBCLAUSES;
                MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR;
                NBCLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR)) - OLDNBCLAUSES;

                try {
                    trace.addToTrace(index + " solve");
                    index++;

                    startTime = System.currentTimeMillis();
                    final Boolean isSAT = solver.isSatisfiable();
                    endTime = System.currentTimeMillis();

                    // Need to update to use the run() method from Solver just like Launcher inside .sat directory
                    if ( isSAT && increment == 3) {
                        // System.out.println("SATISFIABLE!");                        
                        SATinstances++;
                        // final int[] lits = Arrays.copyOf(solver.modelWithInternalVariables(), solver.modelWithInternalVariables().length);
                        // System.out.println("Solution : " + Arrays.toString(lits).substring(1, Arrays.toString(lits).length() - 1) + "\n");
                    } else if (increment == 3){
                        // System.out.println("UNSATISFIABLE!");
                        UNSATinstances ++;
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

            // trace.toFile();
            // break;
        }

        // How many SAT? How long does it take to run the solver? Number of conflicts/learned clauses?
        System.out.println("SAT Instances : " + SATinstances);
        System.out.println("UNSAT Instances : " + UNSATinstances);        
        System.out.println("Average Leanred Clauses : " + LearnedClauses/iterations);        
        System.out.println("Average Nr Conflicts : " + NrConflicts/iterations);
        System.out.println("Average Solver Run Time : " + SolverRunTime/iterations + " milli sec");

    }

    private static void addClauses(final Trace trace){

        for (int i = 0; i < NBCLAUSES; i++) {

            // final int clauseLength = slaveRandomGenerator.nextInt(LENGTHCLAUSE) + 1;
            // Solver does not allow empty clauses throws ContradictionException
            final int[] clause = new int[LENGTHCLAUSE];

            for (int j = 0; j < LENGTHCLAUSE; j++) {
                clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                while (clause[j] == 0 || isAlreadyPresent(clause, j)) {
                    clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR + 1)) - (MAXVAR + 1);
                }
                // Make sure all lits in the clause are all different from each other
            }

            try {
                trace.addToTrace(index + " addClause " + Arrays.toString(clause));
                // clauses are just for development purpopses, remove later
                index++;
                solver.addClause(new VecInt(clause));
                // When adding contradicting clauses I get Error instead of deciding later UNSAT
            } catch (final Exception e) {
                System.out.println("Inisde Exception from addClause()");
                System.out.println(e.getMessage());
                trace.toFile();
                return;
            }
        }

    }

    private static Boolean isAlreadyPresent(final int[] clause, final int index){
        for(int i = 0; i < index; i++){
            if(clause[i] == clause[index])
                return true;
        }
        return false;
    }

}


// TO DO:
// Assumptions?
// Plant bugs? - Mutation Testing?
// Coverage?
// Random Generator probably needs optimizing...
// Need to add Options later - configure solver, not just the default one - Check Launcher.java inside 'org.sat4j.sat' folder
// Add more information to the trace, not just the APIs called - I don't think the seed is enough to reproduce error ??
// Replay tool