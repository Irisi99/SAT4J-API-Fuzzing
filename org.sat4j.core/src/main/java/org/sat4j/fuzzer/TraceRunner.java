package org.sat4j.fuzzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
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

    public static String runTrace(List<String> apiCalls, boolean verbose){

        double proba = 0.0;
        String solverName = "Default";
        ISolver solver = initSolver(solverName, proba);

        try{
            // Iterates over all the API calls
            for(int i = 0; i < apiCalls.size(); i++){

                if(apiCalls.get(i) == null)
                    continue;

                // If API call is defining the Solver used then parse the name of the solver from the trace
                // and initialize the specified solver
                if(apiCalls.get(i).contains("using solver")){
                    String[] t = apiCalls.get(i).split(" ");
                    solverName = t[t.length-1];
                    solver = initSolver(solverName, proba);

                // If API call is setting the Random Walk probaility then parse the % from the trace and pass it to the solver
                }else if(apiCalls.get(i).contains("Random Walk")){
                    proba = Double.parseDouble(apiCalls.get(i).split(" ")[3]);
                    solver = initSolver(solverName, proba);

                // If API call is setting DBS simplification to true then set it true for the local solver
                }else if(apiCalls.get(i).contains("DBS simplification")){
                    solver.setDBSimplificationAllowed(true);

                // If API call is adding a clause then parse the clause from the trace and create a clause
                } else if(apiCalls.get(i).contains("addClause")){
                    solver.addClause(new VecInt(getClause(apiCalls.get(i)))); 
                      
                // If API call is solving with assumptions then parse the assumptions from the trace and try to solve
                } else if(apiCalls.get(i).contains("assuming")){
                    solver.isSatisfiable(new VecInt(getClause(apiCalls.get(i))));

                // If API call is trying to solve the formula then try to solve
                } else if(apiCalls.get(i).contains("solve")){
                    solver.isSatisfiable();
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

    // Method to initialize solver with the options specified
    private static ISolver initSolver(String solverName, double proba){

        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        ICDCL<?> asolver;

        // Initialize default solver if no specific solver is passed as argument
        if(solverName == "Default"){
            asolver = (ICDCL<?>) factory.defaultSolver();
        } else {
            asolver = (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
        }

        // If Random Walk probability is passed as argument then pass it to the solver
        if(proba != 0.0){
            IOrder order = asolver.getOrder();
            order = new RandomWalkDecorator((VarOrderHeap) order, proba);
            asolver.setOrder(order);
        }

        return asolver;
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
