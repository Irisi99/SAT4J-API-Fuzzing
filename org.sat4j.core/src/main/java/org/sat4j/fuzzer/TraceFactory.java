package org.sat4j.fuzzer;

import java.util.Random;
import org.apache.commons.beanutils.BeanUtils;

import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Counter;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.LearnedConstraintsEvaluationType;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.minisat.core.SimplificationType;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.reader.LecteurDimacs;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.tools.ModelIterator;
import org.sat4j.tools.SearchEnumeratorListener;
import org.sat4j.tools.SolutionFoundListener;

public class TraceFactory {

    static Random masterRandomGenerator;
    static Random slaveRandomGenerator;
    static Trace trace;
    // For every call of APIFUzzer it creates MAX_ITERATIONS traces 
    // Should probably make it run for a ceratin time or until it finds X errors
    static int MAX_ITERATIONS = 10;
    static int MAXVAR;
    static int CLAUSE_LENGTH = 3;    
    static boolean UNIFORM;    
    static boolean ASSUMPTIONS;
    static boolean ENUMERATING;
    static int NUMBER_OF_CLAUSES;
    static double coeficient;
    static ISolver solver;
    static ISolver solver2;
    static int index; 
    static ArrayList<Integer> usedLiterals = new ArrayList<Integer>();
    static ArrayList<String> SOLVERS = new ArrayList<String>();
    static ArrayList<String> DSF = new ArrayList<String>();
    static ArrayList<String> LEARNING = new ArrayList<String>();
    static ArrayList<String> ORDERS = new ArrayList<String>();
    static ArrayList<String> PHASE = new ArrayList<String>();
    static ArrayList<String> RESTARTS = new ArrayList<String>();

    // sanity check - verbose - print all variables you are choosing
    public static void run(long seed, boolean isTraceSeed, boolean verbose) {

        initializeOptions(verbose);

        // If we want to generate a specific Trace we do not need the master random generator
        if(!isTraceSeed){
            //  The Random class uses a 48-bit seed
            masterRandomGenerator = new Random(seed);
        }

        // try {
        //     ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        //     ISolver asolver = factory.defaultSolver();
        //     SimplificationType simp = SimplificationType.values()[masterRandomGenerator.nextInt(SimplificationType.values().length)];
        //     ((ICDCL <?>) asolver).setSimplifier(simp);
        // } catch (Exception e) {
        //     System.out.println(e.getMessage());
        //     e.printStackTrace();
        //     return;
        // }

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

            // Uniform Clause length or not -> ranges from 1 to max length
            UNIFORM = slaveRandomGenerator.nextBoolean();
            if(UNIFORM && verbose){
                System.out.println("c CLAUSE_LENGTH: " + CLAUSE_LENGTH);
            }

            // HEX ID for Trace
            trace = new Trace(Long.toHexString(slaveSeed));
            usedLiterals.clear();
            index = 1;

            // Randomly fuzz the internal solution counter
            ENUMERATING = slaveRandomGenerator.nextBoolean();

            // Flip assumptions - randomly generate assumptions
            ASSUMPTIONS = slaveRandomGenerator.nextBoolean();

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

            // Incremental -> add clauses - solve - repeat (increase number of variables and clauses)
            increments = 0;

            // Increments range from 1 to 5
            totalIncrements = slaveRandomGenerator.nextInt(5) + 1;

            while(increments < totalIncrements){
                increments ++;

                int OLDMAXVAR = MAXVAR;

                // Simpler Formulas for enumerating or it gives timeot ???
                if(ENUMERATING){
                    // Add 0 - 20 to the Number of Variables on each increment
                    MAXVAR = slaveRandomGenerator.nextInt(21) + OLDMAXVAR;
                    coeficient = 5;
                } else {
                    // Add 20 - 200 to the Number of Variables on each increment
                    MAXVAR = slaveRandomGenerator.nextInt(181) + 20 + OLDMAXVAR;
                    // Higher coeficient (more clauses) means more UNSAT instances
                    coeficient = 3;
                }

                // Add Coeficient * newVariables new Clauses each increment
                NUMBER_OF_CLAUSES = (int) (coeficient * (MAXVAR - OLDMAXVAR));

                if(verbose){
                    System.out.println("c MAXVAR: " + MAXVAR);                    
                    System.out.println("c NUMBER_OF_CLAUSES: " + NUMBER_OF_CLAUSES);
                }

                try{
                    addClauses();
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

                if(ENUMERATING){
                    try {

                        if(verbose){
                            System.out.println("Started Generating Solution Enumerations");
                        }

                        trace.addToTrace("enumerating");

                        // solver = SolverFactory.newDefault();
                        // var reader = new LecteurDimacs(solver);
                        // reader.parseInstance("org.sat4j.core/src/test/testfiles/bug175-4.cnf");

                        // solver2 = SolverFactory.newDefault();
                        // var reader2 = new LecteurDimacs(solver2);
                        // reader2.parseInstance("org.sat4j.core/src/test/testfiles/bug175-4.cnf");

                        int internal = countSolutionsInt(solver);
                        int external = countSolutionsExt(solver2);

                        if(internal != external){
                            // Specify which is smaller/bigger for delta debugging ???
                            // Compare the found models ???
                            // Unset variables !!! - 829a0bba75dca96_dd.txt

                            throw new Exception("Internal and External Enumerators provided different values : " + internal + " - " + external);
                        } else
                            break;

                    } catch (Exception e) {
                        if(!isTraceSeed){
                            trace.toFile();
                            System.out.print(" --- Inside Exception from Enumeration");
                            System.out.println(" --- " + e.getMessage());
                        }
                        if(verbose){
                            e.printStackTrace(System.out);
                        }
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

                            // Should I check if it is UNSAT because of Assumptions and if so continue the increments ???

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
                if(ENUMERATING)
                    solver2.addClause(new VecInt(clause));

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

            String solverName = "";

            // Configure solver or use default solver
            if (useAll || initRandomGenerator.nextBoolean()) {

                // Flip coin to use a predifined solver or configure solver randomly
                if(initRandomGenerator.nextBoolean()){
                    solverName = SOLVERS.get(initRandomGenerator.nextInt(SOLVERS.size()));
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

            // Use DBS simplification or not
            if (useAll || initRandomGenerator.nextBoolean()) {
                if(addToTrace){
                    trace.addToTrace(index + " DBS simplification");
                    index++;
                }
                asolver.setDBSimplificationAllowed(true);
            }

            if(verbose && addToTrace){
                System.out.println("c SOLVER: " + solverName);
            }
        }

        if(verbose){
            System.out.println(asolver.toString());
        }
        return asolver;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ISolver createSolverWithBuildingBlocks(ICDCL theSolver, Random initRandomGenerator, Boolean useAll, Boolean addToTrace) throws Exception{
        
        // What should happen when option combination is really slow ???

        // Building Blocks : DSF, LEARNING, ORDERS, PHASE, RESTARTS, SIMP, PARAMS, CLEANING
        Solver asolver = (Solver) theSolver;

        if(useAll || initRandomGenerator.nextBoolean()){
            String dsfName = DSF.get(initRandomGenerator.nextInt(DSF.size()));
            if(addToTrace){
                trace.addToTrace(index + " Data Structure Factory : "+dsfName);
                index++;
            }
            DataStructureFactory dsf = (DataStructureFactory) Class.forName("org.sat4j.minisat.constraints."+dsfName).getConstructor().newInstance();
            theSolver.setDataStructureFactory(dsf);
        }

        // What range should I apply for parameters ???

        if (useAll || initRandomGenerator.nextBoolean()) {
            String learningName = LEARNING.get(initRandomGenerator.nextInt(LEARNING.size()));
            String log = index + " Learning Strategy : "+learningName;
            Double percent = 0.95;
            Integer maxlength = 3;
            Integer maxpercent = 10;
            if(learningName == "ActiveLearning"){
                percent = 0.9 + (initRandomGenerator.nextDouble()/10); //0.9 - 1
                log += "/percent="+percent;
            } else if(learningName == "FixedLengthLearning"){
                maxlength = initRandomGenerator.nextInt(6) + 1; //1 - num of variables
                log += "/maxlength="+maxlength;
            } else if(learningName == "PercentLengthLearning"){
                maxpercent = initRandomGenerator.nextInt(10) + 5; //0 - 100%
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
            String orderName = ORDERS.get(initRandomGenerator.nextInt(ORDERS.size()));
            String log = index + " Order : "+orderName;
            Integer period = 20;
            Double varDecay = 1.0;
            if(orderName == "PureOrder"){
                period = initRandomGenerator.nextInt(20) + 10; //10 - 30
                log += "/period="+period;
            } else if(orderName == "VarOrderHeap"){
                varDecay = 0.5 + (initRandomGenerator.nextDouble()); //0.5 - 1.5
                log += "/varDecay="+varDecay;
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
            String pssName = PHASE.get(initRandomGenerator.nextInt(PHASE.size()));
            if(addToTrace){
                trace.addToTrace(index + " Phase Selection Strategy : "+pssName);
                index++;
            }
            IPhaseSelectionStrategy pss = (IPhaseSelectionStrategy) Class.forName("org.sat4j.minisat.orders."+pssName).getConstructor().newInstance();
            if (pss != null) {
                theSolver.getOrder().setPhaseSelectionStrategy(pss);
            }
        }

        if(useAll || initRandomGenerator.nextBoolean()){
            String restarterName = RESTARTS.get(initRandomGenerator.nextInt(RESTARTS.size()));
            String log = index + " Restart Strategy : "+restarterName;
            Long period = initRandomGenerator.nextLong();
            Integer factor = 32;
            if(restarterName == "FixedPeriodRestarts"){
                log += "/period="+period;
            } else if(restarterName == "LubyRestarts"){
                factor = initRandomGenerator.nextInt(30) + 20; //20 - 50
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

        // TODO: FIX SIMPLIFICATION - error field not found
        // Setter in ISolver

        // if(useAll || initRandomGenerator.nextBoolean()){
        //     SimplificationType simp = SimplificationType.values()[initRandomGenerator.nextInt(SimplificationType.values().length)];
        //     if(addToTrace){
        //         trace.addToTrace(index + " Simplification Type : "+simp.toString());
        //         index++;
        //     }
        //     theSolver.setSimplifier(simp);
        // }

        if(useAll || initRandomGenerator.nextBoolean()){
            // this(0.95, 0.999, 1.5, 100);
            Double varDecay = 0.9 + (initRandomGenerator.nextDouble()/10); //0.9 - 1
            Double claDecay = 0.99 + (initRandomGenerator.nextDouble()/100); //0.99 - 1
            Double conflictBoundIncFactor = initRandomGenerator.nextInt(3) + initRandomGenerator.nextDouble(); //0 - 3
            Integer initConflictBound = initRandomGenerator.nextInt(40) + 80; //80 - 120
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

    // Count solutions with External Iterator
    public static int countSolutionsExt(ISolver solver) throws TimeoutException{
        var enumerator = new ModelIterator(solver);
        while (enumerator.isSatisfiable() && enumerator.model() != null) {}
        return (int) enumerator.numberOfModelsFoundSoFar();
    }

    //Count solutions with Internal Iterator
    public static int countSolutionsInt(ISolver solver) throws TimeoutException{
        Counter counter = new Counter();
        SolutionFoundListener sfl = new SolutionFoundListener() {
            @Override
            public void onSolutionFound(int[] solution) {
                counter.inc();
            }
        };
        SearchEnumeratorListener enumerator = new SearchEnumeratorListener(sfl);
        solver.setSearchListener(enumerator);
        solver.isSatisfiable();
        return  enumerator.getNumberOfSolutionFound();
    }

    
    public static void initializeOptions(boolean verbose){

        // All the Pre-Defined Solver Configurations for Minisat
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        SOLVERS.addAll(Arrays.asList(factory.solverNames()));

        // No point in keeping it as an option
        SOLVERS.remove("Default");

        // Not real solvers
        SOLVERS.remove("Statistics");
        SOLVERS.remove("DimacsOutput");

        // Available Data Structure Factories
        DSF.add("CardinalityDataStructure");
        DSF.add("CardinalityDataStructureYanMax");
        DSF.add("CardinalityDataStructureYanMin");
        DSF.add("ClausalDataStructureWL");
        DSF.add("MixedDataStructureDanielHT");
        DSF.add("MixedDataStructureDanielWL");
        DSF.add("MixedDataStructureDanielWLConciseBinary");
        DSF.add("MixedDataStructureSingleWL");

        // Available Learning Strategies
        LEARNING.add("ActiveLearning");
        LEARNING.add("ClauseOnlyLearning");
        LEARNING.add("FixedLengthLearning");
        // LEARNING.add("LimitedLearning"); - protected constructor
        LEARNING.add("MiniSATLearning");
        LEARNING.add("NoLearningButHeuristics");
        LEARNING.add("NoLearningNoHeuristics");
        LEARNING.add("PercentLengthLearning");

        // Available Order Strategies
        ORDERS.add("NaturalStaticOrder");
        // ORDERS.add("OrientedOrder"); - constructor expects file name where order is defined
        ORDERS.add("PureOrder");
        ORDERS.add("VarOrderHeap");

        // Available Phase Selection Strategies
        PHASE.add("PhaseCachingAutoEraseStrategy");
        PHASE.add("PhaseInLastLearnedClauseSelectionStrategy");
        PHASE.add("PositiveLiteralSelectionStrategy");
        PHASE.add("RandomLiteralSelectionStrategy");
        PHASE.add("RSATLastLearnedClausesPhaseSelectionStrategy");
        PHASE.add("RSATPhaseSelectionStrategy");
        PHASE.add("SolutionPhaseSelectionStrategy");
        PHASE.add("UserFixedPhaseSelectionStrategy");

        // Available Restart Startegies
        RESTARTS.add("ArminRestarts");
        RESTARTS.add("EMARestarts");
        RESTARTS.add("FixedPeriodRestarts");
        RESTARTS.add("Glucose21Restarts");
        RESTARTS.add("LubyRestarts");
        RESTARTS.add("MiniSATRestarts");
        RESTARTS.add("NoRestarts");
    }
}