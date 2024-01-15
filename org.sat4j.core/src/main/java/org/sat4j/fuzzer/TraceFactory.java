package org.sat4j.fuzzer;

import java.util.Random;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
public class TraceFactory {

    static Random masterRandomGenerator;
    static Random slaveRandomGenerator;
    static int MAX_ITERATIONS = 2000;
    static int MAXVAR;
    static int CLAUSE_LENGTH = 3;    
    static boolean UNIFORM;    
    static boolean ASSUMPTIONS;
    static int NUMBER_OF_CLAUSES;
    static double coeficient;
    static ISolver solver;
    static int index;
    static ArrayList<String> OPTIONS = new ArrayList<String>();    
    static ArrayList<String> SOLVERS = new ArrayList<String>();

    // sanity check - verbose - print all variables you are choosing
    static Boolean verbose = false;

    public static void run(long seed, boolean isTraceSeed) {

        initializeOptions();

        if(!isTraceSeed){
            //  The Random class uses a 48-bit seed
            masterRandomGenerator = new Random(seed);
        }

        int iteration = 0;
        int increments = 0;
        Trace trace;
        Boolean isSAT;
        // STATISTICS
        Map<String, Number> stats;
        int SATinstances = 0;
        int UNSATinstances = 0;
        long LearnedClauses = 0;        
        long NrConflicts = 0;
        long SolverRunTime = 0;
        long startTime = 0;
        long endTime = 0;

        while (iteration < MAX_ITERATIONS) {

            iteration++;

            // Generate Slave Seed
            long slaveSeed;
            if(!isTraceSeed){
                slaveSeed = masterRandomGenerator.nextLong();
                slaveRandomGenerator = new Random(slaveSeed); 
            } else {
                slaveSeed = seed;
                verbose = true;
            }

            slaveRandomGenerator = new Random(slaveSeed); 
            
            MAXVAR = 0;
            NUMBER_OF_CLAUSES = 0;
            // (3.8/4.5) * MAXVAR - about 50/50 SAT/UNSAT
            coeficient = 3.1;

            // Uniform or not - 1 to max length
            UNIFORM = slaveRandomGenerator.nextBoolean();
            if(UNIFORM && verbose){
                System.out.println("CLAUSE_LENGTH: " + CLAUSE_LENGTH);
            }
            // Higher Length of clause means more SAT (shorter solve time, less conflicts and less learned clauses)
            // Higher coeficient (more clauses) means more UNSAT

            // HEX ID for Trace
            trace = new Trace(Long.toHexString(slaveSeed));

            index = 1;
            trace.addToTrace(index + " init");
            index++;

            try {
                solver = initializeSolver();
            } catch (Exception e) {
                if(!isTraceSeed){
                    trace.toFile();
                }
                System.out.print(" --- Inisde Exception from initializeSolver()");
                System.out.println(" --- " + e.getMessage());
                continue;
            }

            // Incremental -> add clauses - solve - repeat (increase number of variables and clauses)
            // flip assumptions - randomly generate assumptions not old solutions
            ASSUMPTIONS = slaveRandomGenerator.nextBoolean();
            increments = 0;

            while(increments < 3){ // not fixed number of increments
                increments ++;

                int OLDMAXVAR = MAXVAR;
                MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR; // add 20 - 200
                NUMBER_OF_CLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR));

                if(verbose){
                    System.out.println("MAXVAR: " + MAXVAR);                    
                    System.out.println("NUMBER_OF_CLAUSES: " + NUMBER_OF_CLAUSES);
                }

                try{
                    addClauses(trace);
                } catch (final Exception e) {
                    if(!isTraceSeed){
                        trace.toFile();
                    }
                    System.out.print(" --- Inisde Exception from addClause()");
                    System.out.println(" --- " + e.getMessage());
                    break;
                }

                try {
                    startTime = System.currentTimeMillis();
                    if(ASSUMPTIONS){
                        VecInt assumption = new VecInt(0);
                        // power law for assumptions as well
                        int size = MAXVAR/10;
                        while(slaveRandomGenerator.nextDouble() < 1.0/6.0){
                            size += MAXVAR/10;
                        }

                        while (assumption.size() < size) {
                            int lit = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                            if(assumption.contains(lit) || assumption.contains(-lit) || lit == 0){
                                continue;
                            }
                            else{
                                assumption.push(lit);
                            }
                        }
                        if(verbose){
                            System.out.println("ASSUMPTIONS:" + Arrays.toString(assumption.toArray()));
                        }

                        trace.addToTrace(index + " assuming " + Arrays.toString(assumption.toArray()));
                        index++;

                        trace.addToTrace(index + " solve");
                        index++;

                        isSAT = solver.isSatisfiable(assumption);

                    } else {

                        trace.addToTrace(index + " solve");
                        index++;

                        isSAT = solver.isSatisfiable(); 
                    }
                    
                    endTime = System.currentTimeMillis();

                    if (isSAT) {
                        if(increments == 3){
                            SATinstances++;
                            if(verbose){
                                System.out.println("SATISFIABLE!");
                                System.out.println("SOLUTION: "+Arrays.toString(solver.model()));
                            }
                        }
                    } else{
                        UNSATinstances ++;
                        if(verbose){
                            System.out.println("UNSATISFIABLE!");
                            if(ASSUMPTIONS){
                                // ask for explanation why it is UNSAT - array of failed assumptions
                                System.out.println("EXPLANATION: "+solver.unsatExplanation());
                            }
                        }
                        break;
                    }

                } catch (final Exception e) {
                    if(!isTraceSeed){
                        trace.toFile();
                    }
                    System.out.print(" --- Inisde Exception from isSatisfiable()");
                    System.out.println(" --- " + e.getMessage());
                    break;
                }
            }

            stats = solver.getStat();
            // System.out.println(stats.keySet());
            LearnedClauses += (long) stats.get("learnedclauses");            
            NrConflicts += (long) stats.get("conflicts");
            SolverRunTime += endTime - startTime;

            if(verbose){
                break;
            }

            if(iteration % 1000 == 0 || verbose){
                // How many SAT? How long does it take to run the solver? Number of conflicts/learned clauses?
                if(!verbose){
                    System.out.println("Statistics for "+(iteration - 1000)+ " - " + iteration +" iterations : ");
                }
                System.out.println("SAT Instances : " + SATinstances);
                System.out.println("UNSAT Instances : " + UNSATinstances);        
                System.out.println("Average Leanred Clauses : " + LearnedClauses/iteration);        
                System.out.println("Average Nr Conflicts : " + NrConflicts/iteration);
                System.out.println("Average Solver Run Time : " + SolverRunTime/iteration + " milli sec");
            }
        }
    }

    private static void addClauses(final Trace trace) throws ContradictionException{

        for (int i = 0; i < NUMBER_OF_CLAUSES; i++) {

            final int[] clause;

            // Uniform or not
            CLAUSE_LENGTH = 3;
            if(!UNIFORM){
                // flip a coin if to make it shorter / longer and keep repeating
                Double percentage = slaveRandomGenerator.nextDouble();
                
                if(percentage < 0.01){
                    // 1% unit clause
                    CLAUSE_LENGTH = 1;
                } else if(percentage < 0.1){
                    // 10% binary clause
                    CLAUSE_LENGTH = 2;
                } else {
                    // 1/6 + longer clauses
                    while (percentage < 1.0/6.0) {
                        CLAUSE_LENGTH += 1;
                        percentage = slaveRandomGenerator.nextDouble();
                    }
                }
            }
            clause = new int[CLAUSE_LENGTH];

            for (int j = 0; j < CLAUSE_LENGTH; j++) {
                clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                while (clause[j] == 0 || isAlreadyPresent(clause, j)) {
                    clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR + 1)) - (MAXVAR + 1);
                }
            }

            try {
                solver.addClause(new VecInt(clause));            
            } catch (ContradictionException e) {
                if(e.getMessage().contains("Creating Empty clause ?")){
                    NUMBER_OF_CLAUSES += 1;
                } else {
                    throw e;
                }
            }
            
            // clauses are just for development purpopses, remove later
            trace.addToTrace(index + " addClause " + Arrays.toString(clause));
            index++;
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

    public static void initializeOptions(){

        // OPTIONS.add("kleast");
        // OPTIONS.add("optimize");
        // OPTIONS.add("randomWalk");
        OPTIONS.add("hot");
        OPTIONS.add("simplify");
        // OPTIONS.add("lower");
        // OPTIONS.add("equivalence");
        // OPTIONS.add("incomplete");
        OPTIONS.add("solver");

        SOLVERS.add("DFS");
        SOLVERS.add("LEARNING");
        SOLVERS.add("ORDERS");
        SOLVERS.add("PHASE");
        SOLVERS.add("RESTARTS");
        SOLVERS.add("SIMP");        
        SOLVERS.add("PARAMS");
        SOLVERS.add("CLEANING");
    }

    public static ISolver initializeSolver() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{

        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        ICDCL<?> asolver = (ICDCL<?>) factory.defaultSolver();

        // do nothing or
        if(slaveRandomGenerator.nextBoolean()){
            // change all from default
            if(slaveRandomGenerator.nextBoolean()){

                String solverName = SOLVERS.get(slaveRandomGenerator.nextInt(SOLVERS.size()));
                asolver = (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
                // asolver.setKeepSolverHot(true);
                asolver.setDBSimplificationAllowed(true);
                if(verbose){
                    System.out.println("SOLVER: "+solverName); 
                    // System.out.println("KEEP SOLVER HOT");
                    System.out.println("DBS SIMPLIFICATION ALLOWED");
                }

            } else {
                // change some
                if (slaveRandomGenerator.nextBoolean()) {
                    String solverName = SOLVERS.get(slaveRandomGenerator.nextInt(SOLVERS.size()));
                    asolver = (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
                    if(verbose){
                        System.out.println("SOLVER: "+solverName);
                    }
                }

                // if (slaveRandomGenerator.nextBoolean()) {
                //     asolver.setKeepSolverHot(true);
                //     if(verbose){
                //         System.out.println("KEEP SOLVER HOT");
                //     }
                // }

                if (slaveRandomGenerator.nextBoolean()) {
                    asolver.setDBSimplificationAllowed(true);
                    if(verbose){
                        System.out.println("DBS SIMPLIFICATION ALLOWED");
                    }
                }
            }
        }

        if(verbose){
            System.out.println(asolver.toString());
        }
        return asolver;
    }

}


// TO DO:
// Plant bugs? - Mutation Testing?
// Coverage? 
// Options - configure solver, not just the default one - Check Launcher.java inside 'org.sat4j.sat' folder