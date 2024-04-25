package org.sat4j.fuzzer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.LearnedConstraintsEvaluationType;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.minisat.core.SearchParams;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;

public class TraceRunner {

    public static void main(final String[] args) {

        // Give name of Trace file or Seed in comandline 
        String argument = String.valueOf(args[0]);

        // If we pass a Seed then call TraceFactory to generate the Trace file
        if(!argument.contains(".txt")){
            long value = Long.parseUnsignedLong(argument, 16);
            TraceFactory.run(value, true, true);

        // If we pass the file of a Trace then we read the file and run all the API calls inside
        } else {
            try {
                List<String> content = Files.readAllLines(Paths.get("./traces/" + argument));
                content.remove(0);
                runTrace(content, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String runTrace(List<String> apiCalls, boolean verbose){

        ISolver solver = initSolver("Default");

        // Second solver in case it is Enumerating Solutions
        ISolver solver2 = initSolver("Default");

        try{
            // Iterates over all the API calls
            for(int i = 0; i < apiCalls.size(); i++){

                if(apiCalls.get(i) == null)
                    continue;

                // If API call is defining the Solver used then parse the name of the solver from the trace
                // and initialize the specified solver
                if(apiCalls.get(i).contains("Solver")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = initSolver(t[t.length-1]);
                    solver2 = initSolver(t[t.length-1]);

                //
                } else if(apiCalls.get(i).contains("Data Structure Factory")){
                    String[] t = apiCalls.get(i).split(" ");
                    DataStructureFactory dsf = (DataStructureFactory) Class.forName("org.sat4j.minisat.constraints."+t[t.length-1]).getConstructor().newInstance();
                    ((ICDCL) solver).setDataStructureFactory(dsf);
                    dsf = (DataStructureFactory) Class.forName("org.sat4j.minisat.constraints."+t[t.length-1]).getConstructor().newInstance();
                    ((ICDCL) solver2).setDataStructureFactory(dsf);

                //
                } else if(apiCalls.get(i).contains("Learning Strategy")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = setLearningStrategy((ICDCL) solver, t[t.length-1]);
                    solver2 = setLearningStrategy((ICDCL) solver2, t[t.length-1]);

                //
                } else if(apiCalls.get(i).contains("Order")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = setOrder((ICDCL) solver, t[t.length-1]);
                    solver2 = setOrder((ICDCL) solver2, t[t.length-1]);

                //
                } else if(apiCalls.get(i).contains("Phase Selection Strategy")){
                    String[] t = apiCalls.get(i).split(" ");
                    IPhaseSelectionStrategy pss = (IPhaseSelectionStrategy) Class.forName("org.sat4j.minisat.orders."+t[t.length-1]).getConstructor().newInstance();
                    ((ICDCL) solver).getOrder().setPhaseSelectionStrategy(pss);
                    ((ICDCL) solver2).getOrder().setPhaseSelectionStrategy(pss);

                //
                } else if(apiCalls.get(i).contains("Restart Strategy")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = setRestartStrategy((ICDCL) solver, t[t.length-1]);
                    solver2 = setRestartStrategy((ICDCL) solver2, t[t.length-1]);

                //
                } else if(apiCalls.get(i).contains("Search Params")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = setSearchParams((ICDCL) solver, t[t.length-1]);
                    solver2 = setSearchParams((ICDCL) solver2, t[t.length-1]);

                //
                } else if(apiCalls.get(i).contains("Learned Constraints Evaluation Type")){
                    String[] t = apiCalls.get(i).split(" ");
                    ((ICDCL) solver).setLearnedConstraintsDeletionStrategy(LearnedConstraintsEvaluationType.valueOf(t[t.length-1]));                    
                    ((ICDCL) solver2).setLearnedConstraintsDeletionStrategy(LearnedConstraintsEvaluationType.valueOf(t[t.length-1]));                    

                // If API call is setting Random Walk percentage
                } else if(apiCalls.get(i).contains("Random Walk")){
                    String[] t = apiCalls.get(i).split(" ");
                    Double proba = Double.parseDouble(t[t.length-1]);
                    solver = setRandomWalk((ICDCL <?>) solver, proba);
                    solver2 = setRandomWalk((ICDCL <?>) solver2, proba);

                // If API call is setting DBS simplification to true then set it true for the local solver
                }else if(apiCalls.get(i).contains("DBS simplification")){
                    solver.setDBSimplificationAllowed(true);
                    solver2.setDBSimplificationAllowed(true);

                // If API call is adding a clause then parse the clause from the trace and create a clause
                } else if(apiCalls.get(i).contains("addClause")){
                    solver.addClause(new VecInt(getClause(apiCalls.get(i)))); 
                    solver2.addClause(new VecInt(getClause(apiCalls.get(i)))); 
                      
                // If API call is solving with assumptions then parse the assumptions from the trace and try to solve
                } else if(apiCalls.get(i).contains("assuming")){
                    solver.isSatisfiable(new VecInt(getClause(apiCalls.get(i))));

                // If API call is trying to solve the formula then try to solve
                } else if(apiCalls.get(i).contains("solve")){
                    solver.isSatisfiable();

                // If API call is trying to enumerate solutions then compare internal and external enumerator results
                } else if(apiCalls.get(i).contains("enumerating")){
                    int internal = TraceFactory.countSolutionsInt(solver);
                    int external = TraceFactory.countSolutionsExt(solver2);
                    if(internal != external) {
                        throw new Exception("Internal and External Enumerators provided different values : " + internal + " - " + external);
                    }
                }
            }

        } catch (Exception e){
            if(verbose){
                e.printStackTrace();
            }
            // Return the class of the error that happened while running the trace
            return e.getClass().getName();
        }
        return null;
    }

    // Method to initialize with the specified solver
    private static ISolver initSolver(String solverName){
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        // Initialize default solver if no specific solver is passed as argument
        if(solverName == "Default"){
            return factory.defaultSolver();
        } else {
            return factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
        }
    }

    @SuppressWarnings("rawtypes")
    private static ISolver setRandomWalk(ICDCL solver, Double proba){
        IOrder order = solver.getOrder();
        if(order instanceof VarOrderHeap){
            order = new RandomWalkDecorator((VarOrderHeap) order, proba);
            solver.setOrder(order);
        }
        return solver;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static ISolver setLearningStrategy(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        Solver asolver = (Solver) solver;
        String[] t = s.split("/");
        LearningStrategy learning = (LearningStrategy) Class.forName("org.sat4j.minisat.learning."+t[0]).getConstructor().newInstance();
        
        if(t.length > 1){
            String[] property = t[1].split("=");
            BeanUtils.setProperty(learning, property[0], property[1]);
        }

        solver.setLearningStrategy(learning);
        learning.setSolver(asolver);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    private static ISolver setOrder(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        String[] t = s.split("/");
        IOrder order = (IOrder) Class.forName("org.sat4j.minisat.orders."+t[0]).getConstructor().newInstance();
        
        if(t.length > 1){
            String[] property = t[1].split("=");
            BeanUtils.setProperty(order, property[0], property[1]);
        }

        solver.setOrder(order);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    private static ISolver setRestartStrategy(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        String[] t = s.split("/");
        RestartStrategy restart = (RestartStrategy) Class.forName("org.sat4j.minisat.restarts."+t[0]).getConstructor().newInstance();
        
        if(t.length > 1){
            String[] property = t[1].split("=");
            BeanUtils.setProperty(restart, property[0], property[1]);
        }

        solver.setRestartStrategy(restart);
        return solver;
    }

    @SuppressWarnings("rawtypes")
    private static ISolver setSearchParams(ICDCL solver, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException{
        SearchParams params = (SearchParams) Class.forName("org.sat4j.minisat.core.SearchParams").getConstructor().newInstance();

        String[] t = s.split("/");
        for(int j = 0; j < t.length; j++){
            String[] property = t[j].split("=");
            BeanUtils.setProperty(params, property[0], property[1]);
        }

        solver.setSearchParams(params);
        return solver;
    }

    // Method to parse clause / assumptions from trace 
    // and remove any redundant information from the trace line such as index
    private static int[] getClause(String line){
        String[] t = line.split(" ");
        int[] clause = new int[t.length-2];
        for(int j=2; j < t.length; j++){
            clause[j-2] = Integer.parseInt(t[j]);
        }
        return clause;
    }

}
