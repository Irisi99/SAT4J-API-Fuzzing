package org.sat4j.fuzzer;

import java.util.Random;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

public class TraceFactory {

    static Random masterRandomGenerator;
    static Random slaveRandomGenerator;
    // For every call of APIGenerator it creates MAX_ITERATIONS traces 
    // Should probably make it run for a ceratin time or until it finds X errors
    static int MAX_ITERATIONS = 500;
    static int MAXVAR;
    static int CLAUSE_LENGTH = 3;    
    static boolean UNIFORM;    
    static boolean ASSUMPTIONS;
    static int NUMBER_OF_CLAUSES;
    static double coeficient;
    static ISolver solver;
    static int index; 
    static ArrayList<Integer> usedLiterals = new ArrayList<Integer>();
    static ArrayList<String> SOLVERS = new ArrayList<String>();

    // sanity check - verbose - print all variables you are choosing
    public static void run(long seed, boolean isTraceSeed, boolean verbose) {

        initializeOptions();

        // If we want to generate a specific Trace we do not need the master random generator
        if(!isTraceSeed){
            //  The Random class uses a 48-bit seed
            masterRandomGenerator = new Random(seed);
        }

        int iteration = 0;
        int increments = 0;
        int totalIncrements = 5;
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
            } else {
                // Or get it from the arguments (of the comandline from TraceRunner)
                slaveSeed = seed;
            }

            slaveRandomGenerator = new Random(slaveSeed); 
            
            MAXVAR = 0;
            NUMBER_OF_CLAUSES = 0;
            // Higher coeficient (more clauses) means more UNSAT instances
            coeficient = 3;

            // Uniform Clause length or not -> ranges from 1 to max length
            UNIFORM = slaveRandomGenerator.nextBoolean();
            if(UNIFORM && verbose){
                System.out.println("c CLAUSE_LENGTH: " + CLAUSE_LENGTH);
            }

            // HEX ID for Trace
            trace = new Trace(Long.toHexString(slaveSeed));
            usedLiterals.clear();
            index = 1;

            try {
                // Initialize the Solver with randomized Options
                solver = initializeSolver(trace, verbose);
            } catch (Exception e) {
                if(!isTraceSeed){
                    trace.toFile();
                    System.out.print(" --- Inside Exception from initializeSolver()");
                    System.out.println(" --- " + e.getMessage());
                }
                if(verbose){
                    e.printStackTrace(System.out);
                }
                continue;
            }

            // Flip assumptions - randomly generate assumptions
            ASSUMPTIONS = slaveRandomGenerator.nextBoolean();

            // Incremental -> add clauses - solve - repeat (increase number of variables and clauses)
            increments = 0;

            // Increments range from 1 to 5
            totalIncrements = slaveRandomGenerator.nextInt(5) + 1;

            while(increments < totalIncrements){
                increments ++;

                // Add 20 - 200 to the Number of Variables on each increment
                int OLDMAXVAR = MAXVAR;
                MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR;

                // Add Coeficient * newVariables new Clauses each increment
                NUMBER_OF_CLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR));

                if(verbose){
                    System.out.println("c MAXVAR: " + MAXVAR);                    
                    System.out.println("c NUMBER_OF_CLAUSES: " + NUMBER_OF_CLAUSES);
                }

                try{
                    addClauses(trace);
                } catch (final Exception e) {
                    if(!isTraceSeed){
                        trace.toFile();
                        System.out.print(" --- Inside Exception from addClause()");
                        System.out.println(" --- " + e.getMessage());
                    }
                    if(verbose){
                        e.printStackTrace(System.out);
                    }
                    break;
                }

                try {
                    startTime = System.currentTimeMillis();

                    // If Assumptions flag is true then solve with assumptions
                    if(ASSUMPTIONS){
                        int[] assumption;

                        // Power law for the number of literals we are assuming
                        // Start with assuming 1/10 of variables and then increase by 1/10 with probability 1/6 
                        int size = MAXVAR/10;
                        while(slaveRandomGenerator.nextDouble() < 1.0/6.0){
                            size += MAXVAR/10;
                        }

                        assumption = new int[size];

                        for (int i=0 ; i < size; i++) {
                            assumption[i] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                            // Need to check if that literal is assumed before, if we are assuming 0
                            // Or if that literal is not used in a clause anywhere in the trace
                            while(isAlreadyPresent(assumption, i) || assumption[i] == 0 || !usedLiterals.contains(assumption[i])){
                                assumption[i] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                            }
                        }
                        if(verbose){
                            System.out.println("c ASSUMPTIONS: " + toString(assumption));
                        }

                        trace.addToTrace(index + " assuming " + toString(assumption));
                        index++;

                        // Call solver and pass the generated assumptions
                        isSAT = solver.isSatisfiable(new VecInt(assumption));

                    // If Assumptions flag is false then simply try to solve the formula
                    } else {

                        trace.addToTrace(index + " solve");
                        index++;

                        // Call the solver
                        isSAT = solver.isSatisfiable(); 
                    }
                    
                    endTime = System.currentTimeMillis();

                    // If it is SAT then we continue with next iteration
                    if (isSAT) {
                        // If this was the last iteration update statistics and continue to next trace
                        if(increments == totalIncrements){
                            SATinstances++;
                            if(verbose){
                                System.out.println("c SATISFIABLE!");
                                System.out.println("c SOLUTION: "+toString(solver.model()));
                            }
                        }
                    // If it is UNSAT no need to continue with the other increments, update statistics and continue to next trace
                    } else{
                        UNSATinstances ++;
                        if(verbose){
                            System.out.println("c UNSATISFIABLE!");
                            if(ASSUMPTIONS){
                                // Ask for explanation why it is UNSAT - array of failed assumptions
                                System.out.println("c EXPLANATION: " + solver.unsatExplanation());
                            }
                        }
                        break;
                    }

                } catch (final Exception e) {
                    if(!isTraceSeed){
                        trace.toFile();
                        System.out.print(" --- Inside Exception from isSatisfiable()");
                        System.out.println(" --- " + e.getMessage());
                    }
                    if(verbose){
                        e.printStackTrace(System.out);
                    }
                    break;
                }
            }

            // Get statistics from the Solver for the trace and updated the local ones
            stats = solver.getStat();
            LearnedClauses += (long) stats.get("learnedclauses");            
            NrConflicts += (long) stats.get("conflicts");
            SolverRunTime += endTime - startTime;

            if(isTraceSeed){
                break;
            }
        }

        // How many SAT? How long does it take to run the solver? Number of conflicts/learned clauses?
        System.out.println("c Statistics for "+ iteration +" iterations : ");
        System.out.println("c SAT Instances : " + SATinstances);
        System.out.println("c UNSAT Instances : " + UNSATinstances);        
        System.out.println("c Average Leanred Clauses : " + LearnedClauses/iteration);        
        System.out.println("c Average Nr Conflicts : " + NrConflicts/iteration);
        System.out.println("c Average Solver Run Time : " + SolverRunTime/iteration + " milli sec");
    }

    private static void addClauses(Trace trace) throws ContradictionException{

        for (int i = 0; i < NUMBER_OF_CLAUSES; i++) {

            int[] clause;

            // Start with default clause length 3
            CLAUSE_LENGTH = 3;

            // Check if Clauses are uniform or if we need to generate a length
            if(!UNIFORM){

                // Flip a coin if we need to make it shorter / longer and keep repeating
                Double percentage = slaveRandomGenerator.nextDouble();
                
                if(percentage < 0.01){
                    // 1% unit clause
                    CLAUSE_LENGTH = 1;
                } else if(percentage < 0.1){
                    // 10% binary clause
                    CLAUSE_LENGTH = 2;
                } else {
                    // ~ 17% longer clauses
                    while (percentage < 1.0/6.0) {
                        CLAUSE_LENGTH += 1;
                        percentage = slaveRandomGenerator.nextDouble();
                    }
                }
            }
            clause = new int[CLAUSE_LENGTH];

            for (int j = 0; j < CLAUSE_LENGTH; j++) {
                // Generate a literal that is a valid variable but could be positive or negative
                clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);

                // Check that this literal is not used before in the clause and that it is not 0
                while (clause[j] == 0 || isAlreadyPresent(clause, j)) {
                    clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR + 1)) - (MAXVAR + 1);
                }

                // We need to know which literals we can assume if we have assumptions on so we keep track of all literals
                if(ASSUMPTIONS && !usedLiterals.contains(clause[j])){
                    usedLiterals.add(clause[j]);
                }
            }

            try {
                trace.addToTrace(index + " addClause " + toString(clause));
                index++;

                solver.addClause(new VecInt(clause));

            } catch (ContradictionException e) {
                // We do not create empty clauses but if all literals in our clause are already
                // assigned a value through unit propagation then they are not considered and it throws an error
                // Almost only happens when we have unit or binary clauses
                if(e.getMessage().contains("Creating Empty clause ?")){
                    // If it does happen we generate the clause again
                    NUMBER_OF_CLAUSES += 1;
                } else {
                    throw e;
                }
            }
        }

    }

    public static void initializeOptions(){

        // All the Pre-Defined Solver Configurations for Minisat
        SOLVERS.add("DFS");
        SOLVERS.add("LEARNING");
        SOLVERS.add("ORDERS");
        SOLVERS.add("PHASE");
        SOLVERS.add("RESTARTS");
        SOLVERS.add("SIMP");        
        SOLVERS.add("PARAMS");
        SOLVERS.add("CLEANING");
    }

    public static ISolver initializeSolver(Trace trace, boolean verbose) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{

        trace.addToTrace(index + " init");
        index++;

        // Initialize deafult solver for Minisat
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        ICDCL<?> asolver = (ICDCL<?>) factory.defaultSolver();

        // Use no Options
        if(slaveRandomGenerator.nextBoolean()){
            // Use all Options
            if(slaveRandomGenerator.nextBoolean()){
                
                // Choose one of the Solvers predifined
                String solverName = SOLVERS.get(slaveRandomGenerator.nextInt(SOLVERS.size()));
                trace.addToTrace(index + " using solver " + solverName);
                index++;
                asolver = (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);

                // Set Random Walk probability in %
                double proba = slaveRandomGenerator.nextDouble();
                trace.addToTrace(index + " Random Walk " + proba);
                index++;
                IOrder order = asolver.getOrder();
                order = new RandomWalkDecorator((VarOrderHeap) order, proba);
                asolver.setOrder(order);
                
                // Use DBS (Dependency Based) Simplification
                trace.addToTrace(index + " DBS simplification allowed");
                index++;
                asolver.setDBSimplificationAllowed(true);
               
                if(verbose){
                    System.out.println("SOLVER: "+solverName);
                }

            // Use some Options
            } else {
                
                //Flip coin to choose one of the Solvers predifined or use default solver
                if (slaveRandomGenerator.nextBoolean()) {
                    String solverName = SOLVERS.get(slaveRandomGenerator.nextInt(SOLVERS.size()));
                    trace.addToTrace(index + " using solver " + solverName);
                    index++;
                    asolver = (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
                    
                    if(verbose){
                        System.out.println("SOLVER: "+solverName);
                    }
                }

                // Flip coin to set Random Walk probability or use default from solver
                if(slaveRandomGenerator.nextBoolean()){
                    double proba = slaveRandomGenerator.nextDouble();
                    trace.addToTrace(index + " Random Walk " + proba);
                    index++;
                    IOrder order = asolver.getOrder();
                    order = new RandomWalkDecorator((VarOrderHeap) order, proba);
                    asolver.setOrder(order);
                }

                // Flip coin to use DBS simplification or not
                if (slaveRandomGenerator.nextBoolean()) {
                    trace.addToTrace(index + " DBS simplification");
                    index++;
                    asolver.setDBSimplificationAllowed(true);
                }
            }
        }

        if(verbose){
            System.out.println(asolver.toString());
        }
        return asolver;
    }

    // Make sure all literals in the clause / assumption are different from each other
    public static Boolean isAlreadyPresent(int[] clause, int index){
        for(int i = 0; i < index; i++){
            if(clause[i] == clause[index])
                return true;
        }
        return false;
    }

    // Custom toString method for array for easier parsing when rurning trace file
    public static String toString( int[] clause){
        String stringClause = "";
        for(int i=0; i < clause.length; i++){
            stringClause += clause[i]+" ";
        }
        return stringClause;
    }

}