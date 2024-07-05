package org.sat4j.fuzzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;
import org.sat4j.minisat.core.ISimplifier;
import org.sat4j.minisat.core.LearnedConstraintsEvaluationType;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.ISolverService;
import org.sat4j.specs.IVecInt;
import org.sat4j.tools.IdrupSearchListener;

public class TraceRunner {

    static ArrayList<Integer> usedLiterals;
    static Boolean ENUMERATING;
    static Boolean PROOF_CHECK_DONE;

    public static void main(final String[] args) {

        // Give name of Trace file or Seed in comandline 
        String argument = String.valueOf(args[0]);

        // If we pass a Seed then call TraceFactory to generate the Trace file
        if(!argument.contains(".txt")){
            long value = Long.parseUnsignedLong(argument, 16);
            TraceFactory.run(value, 1, true, true);

        // If we pass the file of a Trace then we read the file and run all the API calls inside
        } else {
            try {
                List<String> content = Files.readAllLines(Paths.get("./traces/" + argument));
                String seed = argument.substring(0, argument.indexOf(".txt"));
                if(!seed.contains("_dd"))
                    seed = seed.concat("_2");
                runTrace(seed, content, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String runTrace(String seed, List<String> apiCalls, boolean verbose){

        ENUMERATING = false;
        PROOF_CHECK_DONE = false;
        usedLiterals = new ArrayList<Integer>();

        ArrayList<String> icnf = new ArrayList<String>();
        icnf.add("p icnf");

        ISolver solver = Helper.initSolver("Default");
        solver.setSearchListener(new IdrupSearchListener<ISolverService>("./idrups/"+seed+".idrup"));

        // Second solver in case it is Enumerating Solutions
        ISolver solver2 = Helper.initSolver("Default");

        try{
            // Iterates over all the API calls
            for(int i = 0; i < apiCalls.size(); i++){

                if(apiCalls.get(i) == null)
                    continue;

                // If API call is defining the Solver used then parse the name of the solver from the trace and initialize the specified solver
                if(apiCalls.get(i).contains("using solver")){
                    String[] t = apiCalls.get(i).split(" ");
                    solver = Helper.initSolver(t[t.length-1]);
                    solver.setSearchListener(new IdrupSearchListener<ISolverService>("./idrups/"+seed+".idrup"));
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
                } else if(apiCalls.get(i).contains("DBS simplification")){
                    solver.setDBSimplificationAllowed(true);
                    solver2.setDBSimplificationAllowed(true);

                // if API call is passing MAX var to solver
                } else if(apiCalls.get(i).contains("newVar")){
                        String newVar = apiCalls.get(i).split(" ")[2];
                        solver.newVar(Integer.parseInt(newVar)); 
                        solver2.newVar(Integer.parseInt(newVar));
                          
                // If API call is adding a clause then parse the clause from the trace and create a clause
                } else if(apiCalls.get(i).contains("addClause")){
                    int[] clause = getClause(apiCalls.get(i));
                    solver.addClause(new VecInt(clause)); 
                    solver2.addClause(new VecInt(clause));
                    icnf.add("i "+Helper.clauseToString(clause)+"0");
                      
                // If API call is solving with assumptions then parse the assumptions from the trace and try to solve
                } else if(apiCalls.get(i).contains("assuming")){
                    int[] assumption = getClause(apiCalls.get(i));
                    Boolean result = solver.isSatisfiable(new VecInt(assumption));
                    icnf.add("q "+Helper.clauseToString(assumption)+"0");
                    if(result){
                        icnf.add("s SATISFIABLE");
                        icnf.add("m "+Helper.clauseToString(solver.model())+"0");
                    } else {
                        icnf.add("s UNSATISFIABLE");
                        IVecInt unsatCore = solver.unsatExplanation();
                        if(unsatCore != null)
                            icnf.add("u "+Helper.IVecToString(unsatCore)+"0");
                        else
                            icnf.add("u 0");
                    }

                // If API call is trying to solve the formula then try to solve
                } else if(apiCalls.get(i).contains("solve")){
                    Boolean result = solver.isSatisfiable();
                    icnf.add("q 0");
                    if(result){
                        icnf.add("s SATISFIABLE");
                        icnf.add("m "+Helper.clauseToString(solver.model())+"0");
                    } else {
                        icnf.add("s UNSATISFIABLE");
                        icnf.add("u 0");
                    }

                // If API call is trying to enumerate solutions then compare internal and external enumerator results
                } else if(apiCalls.get(i).contains("enumerating")){

                    ENUMERATING = true;

                    long internal = Helper.countSolutionsInt(solver);
                    long external = Helper.countSolutionsExt(solver2);

                    int numberOfUnusedLiterals = solver.nVars() - usedLiterals.size();

                    if(numberOfUnusedLiterals > 0){
                        long divider = Helper.combinations(numberOfUnusedLiterals, numberOfUnusedLiterals);
                        external = external/divider;
                    }

                    if(internal != external) {
                        throw new Exception("Internal and External Enumerators provided different values : " + internal + " - " + external);
                    }
                }
            }

            if(!ENUMERATING){
                PROOF_CHECK_DONE = true;

                Helper.createICNF(seed, icnf);
                Process process = Runtime.getRuntime().exec("./idrup-check icnfs/"+seed+".icnf idrups/"+seed+".idrup");

                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String error = stdError.readLine();
                // if(verbose && error != null){
                //     System.out.println(error + " " + stdError.readLine());
                // }
                if(error != null && error.contains("does not satisfy input clause"))
                    error = "does not satisfy input clause";
                else if(error != null && error.contains("lemma implication check failed"))
                    error = "lemma implication check failed";
                else if(error != null && error.contains("unsatisfiable core implication check failed"))
                    error = "unsatisfiable core implication check failed";
    
                int exitCode = process.waitFor(); 
                if(exitCode != 0){
                    throw new Exception("IDRUP Checker failed with code "+exitCode+" --- "+error);
                } else {
                    Helper.deleteProof(seed);
                }
            } else {
                Helper.deleteProof(seed);
            }

        } catch (Exception e){
            if(verbose){
                e.printStackTrace();
            }

            if(!PROOF_CHECK_DONE)
                Helper.deleteProof(seed);

            if(e.getMessage() != null && e.getMessage().contains("Enumerators")){
                return "Enumeration";
            } else if(e.getMessage() != null && e.getMessage().contains("IDRUP")){
                return e.getMessage();
            } else if(e.getMessage() != null && e.getMessage().contains("Creating Empty clause ?")){
                return "Empty Clause";
            } else {
                // Return the class of the error that happened while running the trace
                return e.getClass().getName();
            }
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
