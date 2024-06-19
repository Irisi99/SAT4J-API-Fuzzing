package org.sat4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

public class BugSAT181 {

    @Test
    public void testIssueWithDBSimplificationAllowed()
            throws ContradictionException, TimeoutException {
        Solver<?> solver = SolverFactory.newGlucose21();
        solver.setDBSimplificationAllowed(true);
        solver.addClause(-2, 1);
        solver.addClause(-1);
        assertTrue(solver.isSatisfiable());
        solver.addClause(2);
        assertFalse(solver.isSatisfiable());
    }

    @Test
    public void testIssueWithDBSimplificationNotAllowed()
            throws ContradictionException, TimeoutException {
        Solver<?> solver = SolverFactory.newGlucose21();
        solver.setDBSimplificationAllowed(false);
        solver.addClause(-2, 1);
        solver.addClause(-1);
        assertTrue(solver.isSatisfiable());
        solver.addClause(2);
        assertFalse(solver.isSatisfiable());
    }

    @Test
    public void testIssueWithModelEnumerationWhenDBSimplificationIsAllowed()
            throws ContradictionException, TimeoutException {
        Solver<?> solver = SolverFactory.newGlucose21();
        solver.setDBSimplificationAllowed(true);
        solver.addClause(1, 2);
        solver.addClause(1, -2);
        solver.addClause(-1, 2);
        ModelIterator it = new ModelIterator(solver);
        assertTrue(it.isSatisfiable());
        int[] model = it.model();
        assertEquals(2, model.length);
        assertEquals(1, model[0]);
        assertEquals(2, model[1]);
        assertFalse(it.isSatisfiable());
    }
}
