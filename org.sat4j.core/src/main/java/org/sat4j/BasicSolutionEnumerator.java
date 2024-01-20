package org.sat4j;

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
        ISolver solver = SolverFactory.newDefault();
        solver.setTimeout(3600); // 1 hour timeout
        Reader reader = new DimacsReader(solver);
        Boolean SAT = false;
        int i = 0;
        try {
            // CNF filename is given on the command line
            reader.parseInstance(args[0]);

            while (solver.isSatisfiable()) {
                if (!SAT) {
                    SAT = true;
                    System.out.println("SATISFIABLE!\n");
                }
                i++;
                int[] lits = Arrays.copyOf(solver.modelWithInternalVariables(), solver.modelWithInternalVariables().length);

                System.out.println("Solution " + i + " : "
                                + Arrays.toString(lits).substring(1, Arrays.toString(lits).length() - 1)
                                + "\n");

                for (int j = 0; j < lits.length; j++) {
                    lits[j] = lits[j] * -1;
                }

                solver.addClause(new VecInt(lits));

            }
            if (!SAT) {
                System.out.println("UNSATISFIABLE!");
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (ParseFormatException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ContradictionException e) {
            System.out.println("Unsatisfiable (trivial)!");
        } catch (TimeoutException e) {
            System.out.println("Timeout, sorry!");
        }
    }
}