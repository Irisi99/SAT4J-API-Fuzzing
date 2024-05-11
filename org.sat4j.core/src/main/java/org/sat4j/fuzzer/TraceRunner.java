package org.sat4j.fuzzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.ISimplifier;
import org.sat4j.minisat.core.LearnedConstraintsEvaluationType;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.ISolver;

public class TraceRunner {

    static ArrayList<Integer> usedLiterals;

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

        usedLiterals = new ArrayList<Integer>();

        ISolver solver = Helper.initSolver("Default");
        // Second solver in case it is Enumerating Solutions
        ISolver solver2 = Helper.initSolver("Default");

        try{
            // Iterates over all the API calls
            for(int i = 0; i < apiCalls.size(); i++){

                if(apiCalls.get(i) == null)
                    continue;

                // If API call is defining the Solver used then parse the name of the solver from the trace and initialize the specified solver
                if(apiCalls.get(i).contains("Solver")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = Helper.initSolver(t[t.length-1]);
                    solver2 = Helper.initSolver(t[t.length-1]);

                // If API call is defining the Data Structure then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Data Structure Factory")){
                    String[] t = apiCalls.get(i).split(" ");
                    DataStructureFactory dsf = (DataStructureFactory) Class.forName("org.sat4j.minisat.constraints."+t[t.length-1]).getConstructor().newInstance();
                    ((ICDCL) solver).setDataStructureFactory(dsf);
                    dsf = (DataStructureFactory) Class.forName("org.sat4j.minisat.constraints."+t[t.length-1]).getConstructor().newInstance();
                    ((ICDCL) solver2).setDataStructureFactory(dsf);

                // If API call is defining the Order then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Order")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = Helper.setOrder((ICDCL) solver, t[t.length-1]);
                    solver2 = Helper.setOrder((ICDCL) solver2, t[t.length-1]);

                // If API call is defining the Phase Startegy then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Phase Selection Strategy")){
                    String[] t = apiCalls.get(i).split(" ");
                    IPhaseSelectionStrategy pss = (IPhaseSelectionStrategy) Class.forName("org.sat4j.minisat.orders."+t[t.length-1]).getConstructor().newInstance();
                    ((ICDCL) solver).getOrder().setPhaseSelectionStrategy(pss);
                    ((ICDCL) solver2).getOrder().setPhaseSelectionStrategy(pss);

                // If API call is defining the Learning Strategy then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Learning Strategy")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = Helper.setLearningStrategy((ICDCL) solver, t[t.length-1]);
                    solver2 = Helper.setLearningStrategy((ICDCL) solver2, t[t.length-1]);

                // If API call is defining the Restart Strategy then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Restart Strategy")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = Helper.setRestartStrategy((ICDCL) solver, t[t.length-1]);
                    solver2 = Helper.setRestartStrategy((ICDCL) solver2, t[t.length-1]);

                // If API call is defining the Simplification Strategy then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Simplification Type")){
                    String[] t = apiCalls.get(i).split(" ");
                    ISimplifier simplifier = (ISimplifier) Solver.class.getDeclaredField(t[t.length-1]).get(((ICDCL) solver));
                    ((ICDCL) solver).setSimplifier(simplifier);
                    simplifier = (ISimplifier) Solver.class.getDeclaredField(t[t.length-1]).get(((ICDCL) solver2));
                    ((ICDCL) solver2).setSimplifier(simplifier);

                // If API call is defining Search Parameters that will be used during solving then parse them from the trace and update the solver
                } else if(apiCalls.get(i).contains("Search Params")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = Helper.setSearchParams((ICDCL) solver, t[t.length-1]);
                    solver2 = Helper.setSearchParams((ICDCL) solver2, t[t.length-1]);

                // If API call is defining the Learned Constraints Evaluation Type then parse the name from the trace and update the solver
                } else if(apiCalls.get(i).contains("Learned Constraints Evaluation Type")){
                    String[] t = apiCalls.get(i).split(" ");
                    ((ICDCL) solver).setLearnedConstraintsDeletionStrategy(LearnedConstraintsEvaluationType.valueOf(t[t.length-1]));                    
                    ((ICDCL) solver2).setLearnedConstraintsDeletionStrategy(LearnedConstraintsEvaluationType.valueOf(t[t.length-1]));                    

                // If API call is setting Random Walk percentage
                } else if(apiCalls.get(i).contains("Random Walk")){
                    String[] t = apiCalls.get(i).split(" ");
                    Double proba = Double.parseDouble(t[t.length-1]);
                    solver = Helper.setRandomWalk((ICDCL <?>) solver, proba);
                    solver2 = Helper.setRandomWalk((ICDCL <?>) solver2, proba);

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

                    // TODO: Big Numbers ?
                    long internal = Helper.countSolutionsInt(solver);
                    // System.out.println(internal);
                    long external = Helper.countSolutionsExt(solver2);
                    // System.out.println(external);

                    int maxVariableUsed = Collections.max(usedLiterals);
                    int numberOfUnusedLiterals = maxVariableUsed - usedLiterals.size();

                    if(numberOfUnusedLiterals > 0){
                        System.out.println(numberOfUnusedLiterals);
                        long divider = Helper.combinations(numberOfUnusedLiterals, numberOfUnusedLiterals);
                        System.out.println(divider);
                        System.out.println(external);
                        external = external/divider;
                    }

                    if(internal != external) {
                        throw new Exception("Internal and External Enumerators provided different values : " + internal + " - " + external);
                    }
                }
            }

        } catch (Exception e){
            if(verbose){
                e.printStackTrace();
            }
            if(e.getMessage() != null && e.getMessage().contains("Enumerators"))
                return "Enumeration";
            // Return the class of the error that happened while running the trace
            return e.getClass().getName();
        }
        return null;
    }

    // Method to parse clause / assumptions from trace and remove any redundant information from the trace line such as index
    private static int[] getClause(String line){
        String[] t = line.split(" ");
        int[] clause = new int[t.length-2];
        for(int j=2; j < t.length; j++){
            clause[j-2] = Integer.parseInt(t[j]);

            if(!usedLiterals.contains(Math.abs(clause[j-2]))){
                usedLiterals.add(Math.abs(clause[j-2]));
            }
        }
        return clause;
    }

}
