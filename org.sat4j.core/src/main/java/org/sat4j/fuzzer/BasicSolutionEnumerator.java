package org.sat4j.fuzzer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.TimeoutException;
import org.sat4j.core.VecInt;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.Reader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

public class BasicSolutionEnumerator {

    public static void main(String[] args) {

        // Initialize a new solver
        ISolver solver = SolverFactory.newDefault();
        // Set a 10 minute timeout
        solver.setTimeout(600);
        // Initialize a new Reader that will parse the cnf file
        Reader reader = new DimacsReader(solver);

        int i = 0;
        try {
            // CNF filename is given on the command line
            reader.parseInstance(args[0]);
            Boolean is_sat = false;
            do{
                // Check if cnf is SAT
                is_sat = solver.isSatisfiable();

                // If it is UNSAT end computation
                if (!is_sat)
                    break;

                i++;
                // Get the proof of SAT from the solver
                int[] lits = Arrays.copyOf(solver.modelWithInternalVariables(), solver.modelWithInternalVariables().length);
                System.out.println("c Solution " + i + " : " + TraceFactory.toString(lits) + "\n");

                // Swap the sign of the literals and add them as a new clause in the solver in order to find other solutions
                for (int j = 0; j < lits.length; j++) {
                    lits[j] = lits[j] * -1;
                }
                solver.addClause(new VecInt(lits));

            } while (is_sat);

            System.out.println("No other solution");

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (ParseFormatException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ContradictionException e) {
            System.out.println(e.getMessage());
        } catch (TimeoutException e) {
            System.out.println(e.getMessage());
        }
    }

    public static int countSolutions(ISolver solver) throws TimeoutException, ContradictionException{
        int i = 0;
        Boolean is_sat = true;
        while (is_sat) {
            is_sat = solver.isSatisfiable();
            if (!is_sat)
                break;
            i++;
            // Get the proof of SAT from the solver
            int[] lits = Arrays.copyOf(solver.modelWithInternalVariables(), solver.modelWithInternalVariables().length);
            for (int j = 0; j < lits.length; j++) {
                lits[j] = lits[j] * -1;
            }
            solver.addClause(new VecInt(lits));
        }
        return i;
    }
}