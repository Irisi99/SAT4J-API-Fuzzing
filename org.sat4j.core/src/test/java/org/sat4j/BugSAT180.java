package org.sat4j;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.NaturalStaticOrder;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.SearchListener;
import org.sat4j.specs.TimeoutException;

public class BugSAT180 {

    private static final SearchListener<?> listener = new SearchListener<>() {
        @Override
        public void learnUnit(int p) {
            if (p == 4) {
                fail("Learn incorrect unit clause");
            }
        }
    };
    private Solver<?> solver;

    @Before
    public void setup() throws ContradictionException {
        solver = SolverFactory.newGlucose21();
        solver.setOrder(new NaturalStaticOrder());
        solver.setSearchListener(listener);
        solver.addClause(2, -4);
        solver.addClause(4, -7);
        solver.addClause(1, 6);
        solver.addClause(5, 3);
        solver.addClause(-5, -6, 7);
        solver.addClause(-3, 4);
    }

    @Test
    public void testIssueWithExpensiveSimplificationWLOnly()
            throws ContradictionException, TimeoutException {
        solver.setSimplifier(solver.expensiveSimplificationWLOnly);
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void testIssueWithNoSimplification()
            throws ContradictionException, TimeoutException {
        solver.setSimplifier(Solver.NO_SIMPLIFICATION);
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void testIssueWithExpensiveSimplification()
            throws ContradictionException, TimeoutException {
        solver.setSimplifier(solver.expensiveSimplification);
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void testIssueWithSimpleSimplification()
            throws ContradictionException, TimeoutException {
        solver.setSimplifier(solver.simpleSimplification);
        assertTrue(solver.isSatisfiable());
    }
}
