package org.sat4j.fuzzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class DeltaDebugger {

    private static List<String> content;
    private static String seedHEX;
    private static String errorType;
    private static boolean fileCreated = false;
    private static boolean tryAgain = false;

    public static void main(final String[] args) throws TimeoutException, ContradictionException {

        // Get File name from comandline 
        String fileName = String.valueOf(args[0]);

        try {
            // Read all the API calls from the trace
            content = Files.readAllLines(Paths.get("./traces/" + fileName));
            seedHEX = fileName.split(".txt")[0];
            
            // Remove 'init' since we can't run the calls without initializing the solver
            // we do this step every time and add it back to the trace in the end
            content.remove(0);

            // Run the full trace to get the error type
            errorType = TraceRunner.runTrace(seedHEX+"_dd", content, false);
            if(errorType == null){
                System.out.println("--------------- Trace does not throw error so there is nothing to Delta Debug ------------------");
                return;
            }

            do{
                tryAgain = false;
                removeLines();
                removeVariables(); //TODO
                removeLiterals();
                renameLiterals();
            } while(tryAgain);
            shuffle(); //TODO

            String output = TraceRunner.runTrace(seedHEX+"_dd", content, false);
            if(output != null && output.equals(errorType)){
                if(fileCreated){
                    System.out.println("Created trace file " + seedHEX + "_dd.txt");
                }
            } else {
                System.out.println("Error during Delta Debugging");
                Helper.deleteProof(seedHEX+"_dd");
                if(fileCreated){
                    File myObj = new File("./traces/"+seedHEX+"_dd.txt"); 
                    myObj.delete();
                }
            }

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    // try to remove options and clauses
    private static void removeLines(){

        // Delta Debugging parameters
        List<String> temp;
        String output;
        boolean reduced = false;
        boolean createNewFile = false;

        do{

            reduced = false;

            // Delta Debugging parameters
            int granularity = 2;
            int size = content.size();
            int section = (int) (size/granularity);
            int start = 0;
            int end = size;
            boolean reducedInternal = false;

            while(section >= 1){

                reducedInternal = false;
    
                // Copy API calls into a temporary List
                temp = new ArrayList<String>(content);
    
                int i = 0;
                while (i * section < size){
                    start = i * section;
                    i++;
                    end = start + section;
                    if(end > size)
                        end = size;
    
                    System.out.print("section size: "+section+" --- ");
                    System.out.print("start: "+start+" --- ");
                    System.out.print("end: "+end+" --- ");
    
                    // Remove a section of the API calls in the temporary list
                    for(int j = start; j < end; j++){
                        temp.set(j, null);
                    }
    
                    // Store the error of the API calls if there is any
                    output = TraceRunner.runTrace(seedHEX+"_dd", temp, false);
    
                    // Compare the errors and only reduce the trace if the error is the same
                    if(output != null && output.equals(errorType)){
                        tryAgain = true;
                        reduced = true;
                        reducedInternal = true;
                        createNewFile = true;
                        System.out.println("reduced: true");
    
                    // If the errors are different then restore the section of API calls that were removed
                    } else {
                        for(int j = start; j < end; j++){
                            temp.set(j, content.get(j));
                        }
                        System.out.println("reduced: false");
                    }
                }
    
                if(reducedInternal){
    
                    fileCreated = true;
                    temp.removeAll(Collections.singletonList(null));
    
                    // Update the main list of API calls
                    content = new ArrayList<String>(temp);
    
                    // Update the size parameter and calculate the new section size if section size is not down to 1
                    size = content.size();
                    if(section > 1){
                        if((int) (size/granularity) == 0)
                            section = 1;
                        else
                            section = (int) (size/granularity);
                    }
                    else
                        break;
    
                // If trace was not reduced then increase granularity and calculate the new section size
                }else {
                    granularity = granularity * 2;
                    if( section > 1 && (int) (size/granularity) == 0)
                        section = 1;
                    else
                        section = (int) (size/granularity);
                }
            }

        }while(reduced);

        if(createNewFile){
            // Create the new file with the reduced trace
            createFile(content, seedHEX);
        }
    }

    // try to remove literals from clauses
    private static void removeLiterals(){

        Map<Integer, List<Integer>> clauses = new HashMap<Integer, List<Integer>>();
        String output;
        int reduced;
        boolean createNewFile = false;

        for(int i=0; i < content.size(); i++){
            if(content.get(i).contains("addClause") || content.get(i).contains("assuming")){
                String[] line = content.get(i).split(" ");
                List<Integer> clause = getClause(line);
                clauses.put(Integer.valueOf(line[0]), clause);
            }
        }

        do{
            reduced = 0;
            for(int i=0; i < content.size(); i++){
                if(content.get(i).contains("addClause") || content.get(i).contains("assuming")){

                    String[] line = content.get(i).split(" ");
                    int index = Integer.valueOf(line[0]);
                    List<Integer> clause = clauses.get(index);

                    for(int j=0; j < clause.size(); j++){
                        int lit = clause.get(j);
                        clause.remove(j);
                        content.set(i, buildNewAPICall(index, line[1], clause));

                        System.out.print("Removing Literal "+lit+" from API Call with index "+index+" --- ");

                        output = TraceRunner.runTrace(seedHEX+"_dd", content, false);
                        if(output != null && output.equals(errorType)){
                            j--;
                            tryAgain = true;
                            reduced++;
                            createNewFile = true;
                            System.out.println("removed: true");
                        } else {
                            clause.add(j, lit);
                            content.set(i, buildNewAPICall(index, line[1], clause));
                            System.out.println("removed: false");
                        }
                    }
                }
            }
        } while(reduced > 5);

        if(createNewFile){
            // Create the new file with the reduced trace
            createFile(content, seedHEX);
        }
    }

    // try to rename literals
    private static void renameLiterals(){

        ArrayList<Integer> literals = new ArrayList<Integer>();
        ArrayList<Integer> literalsToTry = new ArrayList<Integer>();
        String output;
        boolean createNewFile = false;

        for(int i=0; i < content.size(); i++){
            if(content.get(i).contains("addClause") || content.get(i).contains("assuming")){
                String[] line = content.get(i).split(" ");
                List<Integer> clause = getClause(line);
                for(int j=0; j < clause.size(); j++){
                    if(!literals.contains(Math.abs(clause.get(j))))
                        literals.add(Math.abs(clause.get(j)));
                }
            }
        }

        if(literals.isEmpty())
            return;
        literalsToTry = new ArrayList<Integer>(literals);

        Integer minLiteral = 1;
        while(literals.contains(minLiteral)){
            literals.remove(minLiteral);
            literalsToTry.remove(minLiteral);
            minLiteral++;
        }

        if(literalsToTry.isEmpty())
            return;
        Integer maxLiteral = Collections.min(literalsToTry);

        // Copy API calls into a temporary List
        List<String> temp = new ArrayList<String>(content);

        while(maxLiteral > minLiteral){

            for(int i=0; i < temp.size(); i++){
                if((temp.get(i).contains("addClause") || content.get(i).contains("assuming")) 
                        && temp.get(i).contains(String.valueOf(maxLiteral))){
                    String[] line = temp.get(i).split(" ");
                    int index = Integer.valueOf(line[0]);
                    List<Integer> clause = getClause(line);
                    for(int j=0; j < clause.size(); j++){
                        if(clause.get(j).equals(maxLiteral)){
                            clause.set(j, minLiteral);
                        }
                        if(clause.get(j).equals(-maxLiteral)){
                            clause.set(j, -minLiteral);
                        }
                    }
                    temp.set(i, buildNewAPICall(index, line[1], clause));
                }
            }

            System.out.print("Renaming Literal "+maxLiteral+" with "+minLiteral+" --- ");

            output = TraceRunner.runTrace(seedHEX+"_dd", temp, false);
            if(output != null && output.equals(errorType)){
                tryAgain = true;
                createNewFile = true;
                content = new ArrayList<String>(temp);
                System.out.println("renamed: true");
                literals.remove(maxLiteral);
                minLiteral++;
                while(literals.contains(minLiteral)){
                    minLiteral++;
                }
            } else {
                temp = new ArrayList<String>(content);
                System.out.println("renamed: false");
            }

            literalsToTry.remove(maxLiteral);
            if(literalsToTry.isEmpty())
                break;
            maxLiteral = Collections.min(literalsToTry);
        }

        if(createNewFile){
            // Create the new file with the reduced trace
            createFile(content, seedHEX);
        }
    }

    // try to remove variables from the formula
    private static void removeVariables(){}

    // try to shuffle clauses - only clauses within one increment ???
    private static void shuffle(){}

    private static List<Integer> getClause(String[] line){
        List<Integer> clause = new ArrayList<Integer>();
        for(int j=2; j < line.length; j++){
            clause.add(Integer.parseInt(line[j]));
        }
        return clause;
    }

    private static String buildNewAPICall(Integer index, String action, List<Integer> newClause){
        String result = index + " " + action + " ";
        for(int i=0; i < newClause.size(); i++){
            result = result + newClause.get(i) + " ";
        }
        return result;
    }

    // Mehtod to create file with remaining API calls from the trace after Delta Debugging
    private static void createFile(List<String> trace, String seedHEX){

        // Method is similar to that of Trace.toFile except the fact that 
        // the file name now contains a postfix of '_dd' to distinguish it from the original trace file
        String path = "./traces/" + seedHEX + "_dd.txt";

        final File traceFile = new File(path);
        try {
            traceFile.createNewFile();
            final FileWriter myWriter = new FileWriter(path);
            myWriter.write("1 init\n");
            for (int j = 0; j < trace.size(); j++){
                myWriter.write(trace.get(j) + "\n");
            }
            myWriter.close();
        } catch (final IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
