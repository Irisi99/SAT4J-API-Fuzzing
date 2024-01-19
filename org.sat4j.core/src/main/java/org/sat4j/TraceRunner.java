package org.sat4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.sat4j.core.ASolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.fuzzer.TraceFactory;
import org.sat4j.minisat.core.ICDCL;
import org.sat4j.specs.ISolver;

public class TraceRunner {

    public static void main(final String[] args) {

        // Give name of Trace File in comandline 
        String fileName = String.valueOf(args[0]);

        if(!fileName.contains("_db")){
            String seedHEX = fileName.split(".txt")[0];
            long value = Long.parseUnsignedLong(seedHEX, 16);
            TraceFactory.run(value, true, true);
        } else {
            try {
                List<String> content = Files.readAllLines(Paths.get("./traces/" + fileName));
                runTrace(content, true);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String runTrace(List<String> apiCalls, boolean verbose){

        ISolver solver = initSolver("Default");

        try{
            for(int i = 0; i < apiCalls.size(); i++){
                if(apiCalls.get(i).contains("using solver")){
                    String[] t = apiCalls.get(i).split(" ");
                    String solverName = t[t.length-1];
                    solver = initSolver(solverName);

                } else if(apiCalls.get(i).contains("DBS simplification allowed")){
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

    private static ISolver initSolver(String solverName){
        ASolverFactory<ISolver> factory = org.sat4j.minisat.SolverFactory.instance();
        if(solverName == "Default"){
            return (ICDCL<?>) factory.defaultSolver();
        } else {
            return (ICDCL<?>) factory.createSolverByName(solverName).orElseGet(factory::defaultSolver);
        }
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
