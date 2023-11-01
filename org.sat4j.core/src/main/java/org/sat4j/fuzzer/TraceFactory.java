package org.sat4j.fuzzer;

import java.util.Random;
import java.util.Arrays;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;

public class TraceFactory {

    public static void run(final long seed) {

        final Random masterRandomGenerator = new Random(seed);

        while (true) {

            // Generate Slave Seed
            final long slaveSeed = masterRandomGenerator.nextLong();
            final Random slaveRandomGenerator = new Random(slaveSeed);

            final int MAXVAR = slaveRandomGenerator.nextInt(220) + 20; // 20 - 200
            final int MAXLENGTHCLAUSE = slaveRandomGenerator.nextInt(MAXVAR) + 1; // No larger than max num of variables
            final int NBCLAUSES = slaveRandomGenerator.nextInt(10000) + 1; // What kind of upper bound???
            // Solver does not allow empty cnf without at least 1 clause

            System.out.println("MAXVAR: " + MAXVAR);
            System.out.println("MAXLENGTHCLAUSE: " + MAXLENGTHCLAUSE);
            System.out.println("NBCLAUSES: " + NBCLAUSES);

            final Trace trace = new Trace(slaveSeed);

            int index = 1;
            trace.addToTrace(index + " init()");
            index++;

            final ISolver solver = SolverFactory.newDefault();

            solver.newVar(MAXVAR);
            solver.setExpectedNumberOfClauses(NBCLAUSES);

            for (int i = 0; i < NBCLAUSES; i++) {

                final int clauseLength = slaveRandomGenerator.nextInt(MAXLENGTHCLAUSE) + 1;
                // Solver does not allow empty clauses throws
                // ContradictionException
                final int[] clause = new int[clauseLength];

                for (int j = 0; j < clauseLength; j++) {
                    clause[j] = slaveRandomGenerator.nextInt(MAXVAR + 1) + 1;
                    // How do I make sure that no variable is missing ???
                }

                try {
                    trace.addToTrace(index + " addClause() " + Arrays.toString(clause));
                    // clauses are just for development purpopses, remove later
                    index++;
                    solver.addClause(new VecInt(clause));
                } catch (final Exception e) {
                    System.out.println("Inisde Exception from addClause()");
                    System.out.println(e.getMessage());
                    trace.toFile();
                    return;
                }
            }

            final IProblem problem = solver;
            try {
                trace.addToTrace(index + " solve()");
                index++;
                // Need to upfate to use the run() method from Solver just like Launcher inside .sat directory
                if (problem.isSatisfiable()) { 
                    // Why do I always get SAT ??? Doesn't make sense when I have 40 smth variables and 4000 smth clauses
                    System.out.println("SATISFIABLE!");
                } else {
                    System.out.println("UNSATISFIABLE!");
                }
            } catch (final Exception e) {
                System.out.println("Inisde Exception from isSatisfiable()");
                System.out.println(e.getMessage());
                trace.toFile();
                return;
            }
            trace.toFile();
            break;
        }
    }

}


// TO DO:
// Random Generator probably needs optimizing...
// Need to add Options later - configure solver, not just default one - Check Launcher.java inside sat folder
// Add more information to the trace, not just the APIs called - I don't think the seed is enough to reproduce error ??
// Replay tool