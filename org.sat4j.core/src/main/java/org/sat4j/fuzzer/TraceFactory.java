package org.sat4j.fuzzer;

import java.util.Random;
import org.apache.commons.beanutils.BeanUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.ISimplifier;
import org.sat4j.minisat.core.LearnedConstraintsEvaluationType;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

public class TraceFactory {

    static Random masterRandomGenerator;
    static Random slaveRandomGenerator;
    static Trace trace;
    // For every call of APIFUzzer it creates MAX_ITERATIONS traces 
    // Should probably make it run for a ceratin time or until it finds X errors
    static int MAX_ITERATIONS = 100;
    static int MAXVAR;
    static boolean UNIFORM;    
    static boolean ASSUMPTIONS;
    static boolean ENUMERATING;
    static int NUMBER_OF_CLAUSES;
    static double coeficient;
    static ISolver solver;
    static ISolver solver2;
    static int index; 
    static ArrayList<Integer> usedLiterals = new ArrayList<Integer>();
    static ArrayList<String> icnf = new ArrayList<String>();
    // sanity check - verbose - print all variables you are choosing
    public static void run(long seed, boolean isTraceSeed, boolean verbose) {

        Helper.initializeOptions(verbose);

        // If we want to generate a specific Trace we do not need the master random generator
        if(!isTraceSeed){
            //  The Random class uses a 48-bit seed
            masterRandomGenerator = new Random(seed);
        }

        int iteration = 0;
        int increments = 0;
        int totalIncrements = 5;
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
            icnf.clear();

            // Generate Slave Seed
            long slaveSeed;
            if(!isTraceSeed){
                slaveSeed = masterRandomGenerator.nextLong();
            } else {
                // Or get it from the arguments (of the comandline from TraceRunner)
                slaveSeed = seed;
            }

            slaveRandomGenerator = new Random(slaveSeed); 

            // Uniform Clause length or not -> ranges from 1 to max length
            UNIFORM = slaveRandomGenerator.nextBoolean();
            if(UNIFORM && verbose){
                System.out.println("c CLAUSE_LENGTH: " + 3);
            }

            // HEX ID for Trace
            trace = new Trace(Long.toHexString(slaveSeed));
            usedLiterals.clear();
            index = 1;

            // Randomly fuzz the internal solution counter
            ENUMERATING = slaveRandomGenerator.nextBoolean();
            // Flip assumptions - randomly generate assumptions
            ASSUMPTIONS = slaveRandomGenerator.nextBoolean();

            // Simpler Formulas for enumerating or it gives timeout
            if(ENUMERATING){
                // Add 1 - 20 to the Number of Variables on each increment
                MAXVAR = slaveRandomGenerator.nextInt(20) + 1;
                coeficient = 5;
            } else {
                icnf.add("p icnf");
                // Add 20 - 200 to the Number of Variables on each increment
                MAXVAR = slaveRandomGenerator.nextInt(181) + 20;
                // Higher coeficient (more clauses) means more UNSAT instances
                coeficient = 3;
            }

            // Add Coeficient * newVariables new Clauses each increment
            NUMBER_OF_CLAUSES = (int) (coeficient * MAXVAR);
            Boolean skipMaxVar = true;

            try {

                long initSeed = slaveRandomGenerator.nextLong();
                // Initialize the Solver with randomized Options
                solver = initializeSolver(verbose, true, initSeed);
                // Set 10 min time-out per trace
                solver.setTimeout(600);
                // Initalize identical solver if we are going to compare enumerators
                if(ENUMERATING){
                    solver2 = initializeSolver(verbose, false, initSeed);
                    solver2.setTimeout(600);
                }
                    
            } catch (Exception e) {
                Helper.printException(isTraceSeed, verbose, trace, "initializeSolver()", e);
                continue;
            }

            // Incremental -> add clauses - solve - repeat (increase number of variables and clauses)
            increments = 0;

            // Increments range from 1 to 5
            totalIncrements = slaveRandomGenerator.nextInt(5) + 1;

            while(increments < totalIncrements){
                increments ++;

                if(skipMaxVar){
                    skipMaxVar = false;
                } else {
                    int OLDMAXVAR = MAXVAR;
                    if(ENUMERATING){
                        // Add 0 - 20 to the Number of Variables on each increment
                        MAXVAR = slaveRandomGenerator.nextInt(21) + OLDMAXVAR;
                    } else {
                        // Add 20 - 200 to the Number of Variables on each increment
                        MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR;
                    }
                    NUMBER_OF_CLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR));
                }

                if(verbose){
                    System.out.println("c MAXVAR: " + MAXVAR);                    
                    System.out.println("c NUMBER_OF_CLAUSES: " + NUMBER_OF_CLAUSES);
                }

                try{
                    addClauses();
                } catch (final Exception e) {
                    Helper.printException(isTraceSeed, verbose, trace, "addClause()", e);
                    break;
                }

                if(ENUMERATING){
                    try {
                        if(verbose){
                            System.out.println("Started Generating Solution Enumerations");
                        }

                        trace.addToTrace(index+" enumerating");
                        long internal = Helper.countSolutionsInt(solver);
                        long external = Helper.countSolutionsExt(solver2);
                        int maxVariableUsed = Collections.max(usedLiterals);
                        int numberOfUnusedLiterals = maxVariableUsed - usedLiterals.size();

                        if(numberOfUnusedLiterals > 0){
                            long divider = Helper.combinations(numberOfUnusedLiterals, numberOfUnusedLiterals);
                            external = external/divider;
                        }
                        if(internal != external) {
                            throw new Exception("Internal and External Enumerators provided different values : " + internal + " - " + external);
                        } else
                            break;

                    } catch (Exception e) {
                        Helper.printException(isTraceSeed, verbose, trace, "Enumeration", e);
                        break;
                    }

                } else {
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
                                while(Helper.isAlreadyPresent(assumption, i) || assumption[i] == 0 || !usedLiterals.contains(Math.abs(assumption[i]))){
                                    assumption[i] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                                }
                            }
                            if(verbose){
                                System.out.println("c ASSUMPTIONS: " + Helper.clauseToString(assumption));
                            }
                            trace.addToTrace(index + " assuming " + Helper.clauseToString(assumption));
                            index++;
                            icnf.add("q "+Helper.clauseToString(assumption)+"0");

                            // Call solver and pass the generated assumptions
                            isSAT = solver.isSatisfiable(new VecInt(assumption));

                        // If Assumptions flag is false then simply try to solve the formula
                        } else {
                            trace.addToTrace(index + " solve");
                            index++;
                            icnf.add("q 0");
                            // Call the solver
                            isSAT = solver.isSatisfiable(); 
                        }
                        
                        endTime = System.currentTimeMillis();
                        // If it is SAT then we continue with next iteration
                        if (isSAT) {

                            icnf.add("s SATISFIABLE");
                            icnf.add("m "+Helper.clauseToString(solver.model())+"0");

                            // If this was the last iteration update statistics and continue to next trace
                            if(increments == totalIncrements){
                                // Helper.createICNF(trace.getId(), icnf);

                                SATinstances++;
                                if(verbose){
                                    System.out.println("c SATISFIABLE!");
                                    System.out.println("c SOLUTION: "+ Helper.clauseToString(solver.model()));
                                }
                            }

                        // If it is UNSAT no need to continue with the other increments, update statistics and continue to next trace
                        } else {

                            icnf.add("s UNSATISFIABLE");
                            if(ASSUMPTIONS){
                                icnf.add("u "+Helper.IVecToString(solver.unsatExplanation())+"0");
                            } else {
                                icnf.add("u 0");
                            }
                            // Helper.createICNF(trace.getId(), icnf);

                            // TODO: Should I check if it is UNSAT because of Assumptions and if so continue the increments ???
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
                        Helper.printException(isTraceSeed, verbose, trace, "isSatisfiable()", e);
                        break;
                    }

                    // Get statistics from the Solver for the trace and updated the local ones
                    try {
                        stats = solver.getStat();
                        LearnedClauses += (long) stats.get("learnedclauses");            
                        NrConflicts += (long) stats.get("conflicts");
                        SolverRunTime += endTime - startTime;
                    } catch (Exception e) {
                        if(verbose){
                            System.out.println("c Error when retrieveing Statistics");
                        }
                    }
                }
            }

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

    private static void addClauses() throws ContradictionException{

        for (int i = 0; i < NUMBER_OF_CLAUSES; i++) {

            int[] clause = Helper.newClause(slaveRandomGenerator, UNIFORM);

            for (int j = 0; j < clause.length; j++) {
                // Generate a literal that is a valid variable but could be positive or negative
                clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR)) - (MAXVAR);
                // Check that this literal is not used before in the clause and that it is not 0
                while (clause[j] == 0 || Helper.isAlreadyPresent(clause, j)) {
                    clause[j] = slaveRandomGenerator.nextInt(2 * (MAXVAR + 1)) - (MAXVAR + 1);
                }
                // We need to know which literals we can assume if we have assumptions on so we keep track of all literals
                // We also need to know how many of the variables are used when enumerating
                if(!usedLiterals.contains(Math.abs(clause[j]))){
                    usedLiterals.add(Math.abs(clause[j]));
                }
            }

            try {
                trace.addToTrace(index + " addClause " + Helper.clauseToString(clause));
                index++;

                solver.addClause(new VecInt(clause));
                if(ENUMERATING){
                    solver2.addClause(new VecInt(clause));
                } else {
                    icnf.add("i "+Helper.clauseToString(clause)+"0");
                }
                    
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

    @SuppressWarnings("rawtypes")
    public static ISolver initializeSolver(boolean verbose, boolean addToTrace, Long initSeed) throws Exception {

        if(addToTrace){
            trace.addToTrace(index + " init");
            index++;
        }
        Random initRandomGenerator = new Random(initSeed);
        // Initialize deafult solver for Minisat
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        ISolver asolver = factory.defaultSolver();
        // Use all options or randomly select options to include
        Boolean useAll = initRandomGenerator.nextBoolean();
        // Use no options
        if(initRandomGenerator.nextBoolean()){
            // Configure solver or use default solver
            if (useAll || initRandomGenerator.nextBoolean()) {
                // Flip coin to use a predifined solver or configure solver randomly
                if(initRandomGenerator.nextBoolean()){
                    String solverName = Helper.SOLVERS.get(initRandomGenerator.nextInt(Helper.SOLVERS.size()));
                    if(addToTrace){
                        trace.addToTrace(index + " using solver " + solverName);
                        index++;
                    }
                    asolver = factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
                } else {
                    asolver = createSolverWithBuildingBlocks((ICDCL) asolver, initRandomGenerator, useAll, addToTrace);
                }
            }

            // Use Random walk or not
            if(useAll || initRandomGenerator.nextBoolean()){
                ICDCL temp = null;
                try {
                    temp = (ICDCL) asolver;
                } catch (Exception e) {}
                if(temp != null){
                    IOrder order = ((ICDCL) asolver).getOrder();
                    if(order instanceof VarOrderHeap){
                        Double proba = initRandomGenerator.nextDouble();
                        if(addToTrace){
                            trace.addToTrace(index + " Random Walk : "+proba);
                            index++;
                        }
                        order = new RandomWalkDecorator((VarOrderHeap) order, proba);
                        ((ICDCL) asolver).setOrder(order);
                    }
                }
            }

            // Use DBS simplification or not
            if (useAll || initRandomGenerator.nextBoolean()) {
                if(addToTrace){
                    trace.addToTrace(index + " DBS simplification");
                    index++;
                }
                asolver.setDBSimplificationAllowed(true);
            }
        }
        // if(verbose){
        //     System.out.println(asolver.toString());
        // }
        return asolver;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ISolver createSolverWithBuildingBlocks(ICDCL theSolver, Random initRandomGenerator, Boolean useAll, Boolean addToTrace) throws Exception{
        
        // What should happen when option combination is really slow ???
        // Building Blocks : DSF, LEARNING, ORDERS, PHASE, RESTARTS, SIMP, PARAMS, CLEANING

        Solver asolver = (Solver) theSolver;
        Boolean isNaturalStaticOrder = false;

        if(useAll || initRandomGenerator.nextBoolean()){
            String dsfName = Helper.DSF.get(initRandomGenerator.nextInt(Helper.DSF.size()));
            if(addToTrace){
                trace.addToTrace(index + " Data Structure Factory : "+dsfName);
                index++;
            }
            DataStructureFactory dsf = (DataStructureFactory) Class.forName("org.sat4j.minisat.constraints."+dsfName).getConstructor().newInstance();
            theSolver.setDataStructureFactory(dsf);
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            String orderName = Helper.ORDERS.get(initRandomGenerator.nextInt(Helper.ORDERS.size()));
            String log = index + " Order : "+orderName;
            Integer period = 20;
            Double varDecay = 1.0;
            if(orderName == "PureOrder"){
                // Tries to first branch on a single phase watched unassigned variable else VSIDS from MiniSAT
                period = initRandomGenerator.nextInt(101); // 0 - 100
                log += "/period="+period;
            } else if(orderName == "VarOrderHeap"){
                // VSIDS like heuristics from MiniSAT using a heap
                varDecay = initRandomGenerator.nextDouble(); // 0.0 - 1.0
                log += "/varDecay="+varDecay;
            } else if(orderName == "NaturalStaticOrder"){
                isNaturalStaticOrder = true;
            }
            if(addToTrace){
                trace.addToTrace(log);
                index++;
            }
            IOrder order = (IOrder) Class.forName("org.sat4j.minisat.orders."+orderName).getConstructor().newInstance();
            if (order != null) {
                if(orderName == "PureOrder"){
                    BeanUtils.setProperty(order, "period", period);
                } else if(orderName == "VarOrderHeap"){
                    BeanUtils.setProperty(order, "varDecay", varDecay);
                }
                theSolver.setOrder(order);
            }
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            if(isNaturalStaticOrder){
                isNaturalStaticOrder = false;
            } else {
                String pssName = Helper.PHASE.get(initRandomGenerator.nextInt(Helper.PHASE.size()));
                if(addToTrace){
                    trace.addToTrace(index + " Phase Selection Strategy : "+pssName);
                    index++;
                }
                IPhaseSelectionStrategy pss = (IPhaseSelectionStrategy) Class.forName("org.sat4j.minisat.orders."+pssName).getConstructor().newInstance();
                if (pss != null) {
                    theSolver.getOrder().setPhaseSelectionStrategy(pss);
                }
            }

        }

        if (useAll || initRandomGenerator.nextBoolean()) {
            String learningName = Helper.LEARNING.get(initRandomGenerator.nextInt(Helper.LEARNING.size()));
            String log = index + " Learning Strategy : "+learningName;
            Double percent = 0.95;
            Integer maxlength = 3;
            Integer maxpercent = 10;
            if(learningName == "ActiveLearning"){
                // Limit learning to clauses containing percent % active literals 
                percent = initRandomGenerator.nextDouble(); // 0.0 - 1.0
                log += "/percent="+percent;
            } else if(learningName == "FixedLengthLearning"){
                // Limit learning to clauses of size smaller or equal to maxlength
                maxlength = initRandomGenerator.nextInt(MAXVAR) + 1; // 1 - num of variables -> initial MAX VAR - is not updated during incremental solving
                log += "/maxlength="+maxlength;
            } else if(learningName == "PercentLengthLearning"){
                // Limit learning to clauses of size smaller or equal to maxpercent % of the number of variables
                maxpercent = initRandomGenerator.nextInt(101); // 0 - 100%
                log += "/maxpercent="+maxpercent;
            }
            if(addToTrace){
                trace.addToTrace(log);
                index++;
            }
            LearningStrategy learning = (LearningStrategy) Class.forName("org.sat4j.minisat.learning."+learningName).getConstructor().newInstance();
            if (learning != null) {
                if(learningName == "ActiveLearning"){
                    BeanUtils.setProperty(learning, "percent", percent);
                } else if(learningName == "FixedLengthLearning"){
                    BeanUtils.setProperty(learning, "maxlength", maxlength);
                } else if(learningName == "PercentLengthLearning"){
                    BeanUtils.setProperty(learning, "maxpercent", maxpercent);
                }
                theSolver.setLearningStrategy(learning);
                learning.setSolver(asolver);
            }
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            String restarterName = Helper.RESTARTS.get(initRandomGenerator.nextInt(Helper.RESTARTS.size()));
            String log = index + " Restart Strategy : "+restarterName;
            Integer period = 0;
            Integer factor = 32;
            if(restarterName == "FixedPeriodRestarts"){
                // Constant restarts strategy every period conflicts
                period = initRandomGenerator.nextInt(10001); // 0 - 10000
                log += "/period="+period;
            } else if(restarterName == "LubyRestarts"){
                // Luby style restarts strategy with factor x
                // “unit run” - hence the actual restart intervals are x, x, 2*x, x, x, 2*x, 4*x, . . .
                factor = initRandomGenerator.nextInt(1001); //0 - 1000
                log += "/factor="+factor;
            }
            if(addToTrace){
                trace.addToTrace(log);
                index++;
            }
            RestartStrategy restarter = (RestartStrategy) Class.forName("org.sat4j.minisat.restarts."+restarterName).getConstructor().newInstance();
            if (restarter != null) {
                if(restarterName == "FixedPeriodRestarts"){
                    BeanUtils.setProperty(restarter, "period", period);
                } else if(restarterName == "LubyRestarts"){
                    BeanUtils.setProperty(restarter, "factor", factor);
                }
                theSolver.setRestartStrategy(restarter);
            }
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            String simplifierName = Helper.SIMPLIFIERS.get(initRandomGenerator.nextInt(Helper.SIMPLIFIERS.size()));
            if(addToTrace){
                trace.addToTrace(index + " Simplification Type : "+simplifierName.toString());
                index++;
            }
            ISimplifier simplifier = (ISimplifier) Solver.class.getDeclaredField(simplifierName).get(theSolver);
            theSolver.setSimplifier(simplifier);
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            // this(0.95, 0.999, 1.5, 100);
            // Some parameters used during the search
            Double varDecay = 0.9 + (initRandomGenerator.nextDouble()/10); // 0.9 - 1.0
            Double claDecay = 0.5 + initRandomGenerator.nextDouble(); // 0.5 - 1.5
            Double conflictBoundIncFactor = initRandomGenerator.nextInt(3) + initRandomGenerator.nextDouble(); // 0.0 - 3.0
            Integer initConflictBound = initRandomGenerator.nextInt(1001); // 0 - 1000
            if(addToTrace){
                trace.addToTrace(index + " Search Params : varDecay="+varDecay
                                                        +"/claDecay="+claDecay
                                                        +"/conflictBoundIncFactor="+conflictBoundIncFactor
                                                        +"/initConflictBound="+initConflictBound);
                index++;
            }
            SearchParams params = (SearchParams) Class.forName("org.sat4j.minisat.core.SearchParams").getConstructor().newInstance();
            if (params != null) {
                params.setVarDecay(varDecay); 
                params.setClaDecay(claDecay);
                params.setConflictBoundIncFactor(conflictBoundIncFactor);
                params.setInitConflictBound(initConflictBound);
                theSolver.setSearchParams(params);
            }
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            LearnedConstraintsEvaluationType memory = LearnedConstraintsEvaluationType.values()[initRandomGenerator.nextInt(LearnedConstraintsEvaluationType.values().length)];
            if(addToTrace){
                trace.addToTrace(index + " Learned Constraints Evaluation Type : "+memory.toString());
                index++;
            }
            theSolver.setLearnedConstraintsDeletionStrategy(memory);
        }

        return theSolver;
    }
    
}