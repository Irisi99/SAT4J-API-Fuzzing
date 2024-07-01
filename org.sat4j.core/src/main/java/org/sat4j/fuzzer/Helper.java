package org.sat4j.fuzzer;

import org.sat4j.specs.TimeoutException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.sat4j.core.ASolverFactory;
import org.sat4j.minisat.core.Counter;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.learning.ActiveLearning;
import org.sat4j.minisat.learning.FixedLengthLearning;
import org.sat4j.minisat.learning.PercentLengthLearning;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.minisat.orders.PureOrder;
import org.sat4j.minisat.restarts.FixedPeriodRestarts;
import org.sat4j.minisat.restarts.LubyRestarts;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.ModelIterator;
import org.sat4j.tools.SearchEnumeratorListener;
import org.sat4j.tools.SolutionFoundListener;

public class Helper {

//--------------------------------------------------- USED IN TRACE FACTORY ---------------------------------------------------------------

    static ArrayList<String> SOLVERS = new ArrayList<String>();
    // Available Data Structure Factories
    static List<String> DSF = List.of("CardinalityDataStructure","CardinalityDataStructureYanMax","CardinalityDataStructureYanMin",
        "ClausalDataStructureWL","MixedDataStructureDanielHT","MixedDataStructureDanielWL","MixedDataStructureDanielWLConciseBinary","MixedDataStructureSingleWL");
    // Available Learning Strategies
    static List<String> LEARNING = List.of("ActiveLearning","ClauseOnlyLearning","FixedLengthLearning","MiniSATLearning",
        "NoLearningButHeuristics","NoLearningNoHeuristics","PercentLengthLearning");
    // Available Order Strategies
    static List<String> ORDERS = List.of("NaturalStaticOrder","PureOrder","VarOrderHeap");
    // Available Phase Selection Strategies
    static List<String> PHASE = List.of("PhaseCachingAutoEraseStrategy","PhaseInLastLearnedClauseSelectionStrategy","PositiveLiteralSelectionStrategy","RandomLiteralSelectionStrategy",
        "RSATLastLearnedClausesPhaseSelectionStrategy","RSATPhaseSelectionStrategy","SolutionPhaseSelectionStrategy","UserFixedPhaseSelectionStrategy");
    // Available Restart Startegies
    static List<String> RESTARTS = List.of("ArminRestarts","EMARestarts","FixedPeriodRestarts","Glucose21Restarts","LubyRestarts","MiniSATRestarts","NoRestarts");
    // Available Simplifiers
    static List<String> SIMPLIFIERS = List.of("NO_SIMPLIFICATION","simpleSimplification","expensiveSimplification","expensiveSimplificationWLOnly");
    
    public static void initializeOptions(boolean verbose){

        // All the Pre-Defined Solver Configurations for Minisat
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        SOLVERS.addAll(Arrays.asList(factory.solverNames()));
        // No point in keeping it as an option
        SOLVERS.remove("Default");
        // Not real solvers
        SOLVERS.remove("Statistics");
        SOLVERS.remove("DimacsOutput");
    }

    public static int[] newClause(Random slaveRandomGenerator, Boolean UNIFORM){
            // Start with default clause length 3
            int CLAUSE_LENGTH = 3;
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
            return new int[CLAUSE_LENGTH];
    }

    // Make sure all literals in the clause / assumption are different from each other
    public static Boolean isAlreadyPresent(int[] clause, int index, int literal){
        for(int i = 0; i < index; i++){
            if(clause[i] == literal)
                return true;
        }
        return false;
    }

    // Custom toString method for array for easier parsing when rurning trace file
    public static String clauseToString(int[] clause){
        String stringClause = "";
        for(int i=0; i < clause.length; i++){
            stringClause += clause[i]+" ";
        }
        return stringClause;
    }

    public static String IVecToString(IVecInt reason){
        if(reason == null)
            return "null";
        String stringReason = "";
        String[] splitReason = String.valueOf(reason).split(",");
        for(int i=0; i < splitReason.length; i++){
            stringReason += splitReason[i]+" ";
        }
        return stringReason;
    }

    public static void printException(Boolean isTraceSeed, Boolean verbose, Trace trace, String location, Exception e){
        trace.toFile();
        if(!isTraceSeed){
            System.out.print(" --- Inside Exception from " + location + " ");
            System.out.println(" --- " + e.getMessage());
        }
        if(verbose){
            e.printStackTrace(System.out);
        }
    }
    
    public static void createICNF(String ID, ArrayList<String> icnf){
         // Defines the path to the file - currently pointing to the 'trcaes' folder
        String path = "./icnfs/" + ID + ".icnf";

        File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            FileWriter myWriter = new FileWriter(path);

            // Write all the API calls of the trace in the file seperated by a new line
            for (int i = 0; i < icnf.size(); i++)
                myWriter.write(icnf.get(i) + "\n");
            myWriter.close();
            
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

    }

    public static void deleteProof(String Id){
        File myObj = new File("icnfs/"+Id+".icnf"); 
        myObj.delete();
        myObj = new File("idrups/"+Id+".idrup");
        myObj.delete();
    }

//---------------------------------------------------- USED IN TRACE RUNNER ------------------------------------------------------------------

    // Method to initialize with the specified solver
    public static ISolver initSolver(String solverName){
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        // Initialize default solver if no specific solver is passed as argument
        ISolver solver;
        if(solverName == "Default"){
            solver =  factory.defaultSolver();
        } else {
            solver =  factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
        }
        solver.setTimeout(120);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    public static ISolver setRandomWalk(ICDCL solver, Double proba){
        IOrder order = solver.getOrder();
        if(order instanceof VarOrderHeap){
            order = new RandomWalkDecorator((VarOrderHeap) order, proba);
            solver.setOrder(order);
        }
        return solver;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ISolver setLearningStrategy(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        Solver asolver = (Solver) solver;
        String[] t = s.split("/");
        LearningStrategy learning = (LearningStrategy) Class.forName("org.sat4j.minisat.learning."+t[0]).getConstructor().newInstance();
        
        if(t.length > 1){
            String[] property = t[1].split("=");
            if(property[0].equals("maxlength")){
                ((FixedLengthLearning) learning).setMaxLength(Integer.parseInt(property[1]));
            } else if(property[0].equals("maxpercent")){
                ((PercentLengthLearning) learning).setLimit(Integer.parseInt(property[1]));
            } else if(property[0].equals("percent")){
                ((ActiveLearning) learning).setActivityPercent(Double.parseDouble(property[1]));
            }                
        }

        solver.setLearningStrategy(learning);
        learning.setSolver(asolver);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    public static ISolver setOrder(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        String[] t = s.split("/");
        IOrder order = (IOrder) Class.forName("org.sat4j.minisat.orders."+t[0]).getConstructor().newInstance();
        
        if(t.length > 1){
            String[] property = t[1].split("=");
            if(property[0].equals("period")){
                ((PureOrder) order).setPeriod(Integer.parseInt(property[1]));
            } else if(property[0].equals("varDecay")){
                ((VarOrderHeap) order).setVarDecay(Double.parseDouble(property[1]));
            }
        }

        solver.setOrder(order);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    public static ISolver setRestartStrategy(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        String[] t = s.split("/");
        RestartStrategy restart = (RestartStrategy) Class.forName("org.sat4j.minisat.restarts."+t[0]).getConstructor().newInstance();
        
        if(t.length > 1){
            String[] property = t[1].split("=");
            if(property[0].equals("period")){
                ((FixedPeriodRestarts) restart).setPeriod(Long.parseLong(property[1]));
            } else if(property[0].equals("factor")){
                ((LubyRestarts) restart).setFactor(Integer.parseInt(property[1]));
            }
        }

        solver.setRestartStrategy(restart);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    public static ISolver setSearchParams(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        SearchParams params = (SearchParams) Class.forName("org.sat4j.minisat.core.SearchParams").getConstructor().newInstance();

        String[] t = s.split("/");
        for(int j = 0; j < t.length; j++){
            String[] property = t[j].split("=");
            if(property[0].equals("varDecay")){
                params.setVarDecay(Double.parseDouble(property[1])); 
            } else if(property[0].equals("claDecay")){
                params.setClaDecay(Double.parseDouble(property[1]));
            } else if(property[0].equals("conflictBoundIncFactor")){
                params.setConflictBoundIncFactor(Double.parseDouble(property[1]));
            } else if(property[0].equals("initConflictBound")){
                params.setInitConflictBound(Integer.parseInt(property[1]));
            }
        }

        solver.setSearchParams(params);
        return solver;
    }



//---------------------------------------------------------- USED IN BOTH --------------------------------------------------------------------------

    // Count solutions with External Iterator
    public static long countSolutionsExt(ISolver solver) throws TimeoutException{
        var enumerator = new ModelIterator(solver);
        while (enumerator.isSatisfiable()) {
            int[] model = enumerator.model(); 
            //System.out.println(Helper.clauseToString(model));
            if(model == null)
                break;
        }
        return (long) enumerator.numberOfModelsFoundSoFar();
    }

    //Count solutions with Internal Iterator
    public static long countSolutionsInt(ISolver solver) throws TimeoutException{
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
        return  (long) enumerator.getNumberOfSolutionFound();
    }

    public static long combinations(int n, int r) {
        if(r == 0){
            return 1;
        } else if(r == n){
            return 1 + combinations(n, r-1);
        } else {
            return (factorial(n) / (factorial(r) * factorial(n-r))) + combinations(n, r-1);
        }
    }

    public static long factorial(int n) {
        if (n <= 2) {
            return n;
        }
        return n * factorial(n - 1);
    }
}