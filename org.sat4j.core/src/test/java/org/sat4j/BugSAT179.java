package org.sat4j;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.orders.PureOrder;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

/**
 * Bug reported by Iris Parruca while fuzz testing Sat4j API.
 * 
 * @author leberre
 *
 */
public class BugSAT179 {

    @Test
    public void testNPEWithPureOrder()
            throws ContradictionException, TimeoutException {
        Solver<?> solver = SolverFactory.newGlucose21();
        solver.setOrder(new PureOrder(16));
        solver.addClause(-3, -2);
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void testNoNPEWithPureOrder()
            throws ContradictionException, TimeoutException {
        Solver<?> solver = SolverFactory.newGlucose21();
        solver.setOrder(new PureOrder(16));
        solver.addClause(-2, -1);
        assertTrue(solver.isSatisfiable());
    }
}
