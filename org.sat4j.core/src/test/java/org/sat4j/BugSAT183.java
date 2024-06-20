package org.sat4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;
import org.sat4j.tools.SearchEnumeratorListener;
import org.sat4j.tools.SolutionFoundListener;

/**
 * Those tests clarify what happens if model enumeration happens in an empty
 * solver.
 * 
 * If solver.newVar() is not called, a single, empty model is returned, either
 * by the internal or the external method.
 * 
 * If solver.newVar(n) is called, then the number of enumerated models will be
 * 2**n with the external enumeration.
 * 
 * The internal enumeration cannot properly handle that case. It will launch an
 * IllegalStateException.
 * 
 * @author leberre
 *
 */
public class BugSAT183 {

    @Test
    public void internalEnumerationDoesWorkOnEmptySolver()
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        SolutionFoundListener sfl = new SolutionFoundListener() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void onSolutionFound(int[] solution) {
                assertEquals(0, solution.length);
            }
        };
        SearchEnumeratorListener enumerator = new SearchEnumeratorListener(sfl);
        solver.setSearchListener(enumerator);
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void externalEnumerationDoesWorkOnEmptySolver()
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        ModelIterator it = new ModelIterator(solver);
        assertTrue(it.isSatisfiable());
        assertEquals(0, it.model().length);
        assertFalse(it.isSatisfiable());
        assertEquals(1, it.numberOfModelsFoundSoFar());
    }

    @Test(expected = IllegalStateException.class)
    public void internalEnumerationDoesWorkOnEmptySolverButMaxVarId()
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        solver.newVar(2);
        SolutionFoundListener sfl = new SolutionFoundListener() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void onSolutionFound(int[] solution) {
                assertEquals(2, solution.length);
            }
        };
        SearchEnumeratorListener enumerator = new SearchEnumeratorListener(sfl);
        solver.setSearchListener(enumerator);
        solver.isSatisfiable();
    }

    @Test
    public void externalEnumerationDoesWorkOnEmptySolverButMaxVarId()
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        solver.newVar(2);
        ModelIterator it = new ModelIterator(solver);
        while (it.isSatisfiable()) {
            assertNotNull(it.model());
        }
        assertEquals(4, it.numberOfModelsFoundSoFar());
    }
}
