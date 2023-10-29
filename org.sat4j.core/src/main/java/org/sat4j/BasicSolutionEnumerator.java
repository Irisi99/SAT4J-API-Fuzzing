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
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

public class BasicSolutionEnumerator {
    public static void main(final String[] args) {
        final ISolver solver = SolverFactory.newDefault();
        solver.setTimeout(3600); // 1 hour timeout
        Reader reader = new DimacsReader(solver);
        final IVec<IVecInt> clauses = new Vec<>();
        IVecInt clause;
        Boolean SAT = false;
        int i = 0;
        try {
            // CNF filename is given on the command line
            IProblem problem = reader.parseInstance(args[0]);
            while (problem.isSatisfiable()) {
                if (!SAT) {
                    SAT = true;
                    System.out.println("Satisfiable !\n");
                }
                i++;
                final int[] lits = Arrays.copyOf(problem.model(),
                        problem.model().length);
                for (int j = 0; j < lits.length; j++) {
                    lits[j] = lits[j] * -1;
                }
                System.out
                        .println("Solution " + i + " : "
                                + Arrays.toString(lits).substring(1,
                                        Arrays.toString(lits).length() - 1)
                                + "\n");
                clause = new VecInt(lits);
                clauses.push(clause);
                solver.reset();
                reader = new DimacsReader(solver);
                problem = reader.parseInstance(args[0]);
                solver.addAllClauses(clauses);
            }
            if (!SAT) {
                System.out.println("Unsatisfiable !");
            }
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
        } catch (final ParseFormatException e) {
            // TODO Auto-generated catch block
        } catch (final IOException e) {
            // TODO Auto-generated catch block
        } catch (final ContradictionException e) {
            System.out.println("Unsatisfiable (trivial)!");
        } catch (final TimeoutException e) {
            System.out.println("Timeout, sorry!");
        }
    }
}