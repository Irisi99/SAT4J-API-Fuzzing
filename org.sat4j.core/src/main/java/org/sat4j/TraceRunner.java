package org.sat4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.fuzzer.TraceFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.orders.RandomWalkDecorator;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.specs.ISolver;

public class TraceRunner {

    public static void main(final String[] args) {

        // Give name of Trace File in comandline 
        String fileName = String.valueOf(args[0]);

        if(!fileName.contains("_dd")){
            String seedHEX = fileName.split(".txt")[0];
            long value = Long.parseUnsignedLong(seedHEX, 16);
            TraceFactory.run(value, true, true);
        } else {
            try {
                List<String> content = Files.readAllLines(Paths.get("./traces/" + fileName));
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
            for(int i = 0; i < apiCalls.size(); i++){

                if(apiCalls.get(i) == null)
                    continue;

                if(apiCalls.get(i).contains("using solver")){
                    String[] t = apiCalls.get(i).split(" ");
                    solverName = t[t.length-1];
                    solver = initSolver(solverName, proba);

                }else if(apiCalls.get(i).contains("Random Walk")){
                    proba = Double.parseDouble(apiCalls.get(i).split(" ")[3]);
                    solver = initSolver(solverName, proba);

                }else if(apiCalls.get(i).contains("DBS simplification")){
                    solver.setDBSimplificationAllowed(true);

                } else if(apiCalls.get(i).contains("addClause")){
                    solver.addClause(new VecInt(getClause(apiCalls.get(i)))); 
                      
                } else if(apiCalls.get(i).contains("assuming")){
                    solver.isSatisfiable(new VecInt(getClause(apiCalls.get(i))));

                } else if(apiCalls.get(i).contains("solve")){
                    solver.isSatisfiable();
                }
            }
        } catch (Exception e){
            if(verbose){
                e.printStackTrace();
            }
            return e.getMessage();
        }

        return null;
    }

    private static ISolver initSolver(String solverName, double proba){
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        ICDCL<?> asolver;

        if(solverName == "Default"){
            asolver = (ICDCL<?>) factory.defaultSolver();
        } else {
            asolver = (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
        }

        if(proba != 0.0){
            IOrder order = asolver.getOrder();
            order = new RandomWalkDecorator((VarOrderHeap) order, proba);
            asolver.setOrder(order);
        }

        return asolver;
    }

    private static int[] getClause(String line){
        String[] t = line.split(" ");
        int[] clause = new int[t.length-2];
        for(int j=2; j < t.length; j++){
            clause[j-2] = Integer.parseInt(t[j]);
        }
        return clause;
    }

}
