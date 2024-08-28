package org.sat4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.constraints.MixedDataStructureSingleWL;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.opt.MinOneDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.OptToSatAdapter;

public class BugSAT184 {

    @Test
    public void testIssueWithDBSimplificationAllowed()
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        solver.setDataStructureFactory(new MixedDataStructureSingleWL());
        solver.setDBSimplificationAllowed(true);
        solver.addClause(2, 4);
        solver.addClause(1);
        solver.addClause(4, 3);
        OptToSatAdapter minone = new OptToSatAdapter(
                new MinOneDecorator(solver));
        assertTrue(minone.isSatisfiable());
        int[] model = minone.model();
        assertNotNull(model);
        assertEquals(4, model.length);
        assertTrue(minone.isSatisfiable());
        assertEquals(4, minone.model().length);
    }

    @Test
    public void testOtherIssueWithDBSimplificationAllowed()
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        solver.setDataStructureFactory(new MixedDataStructureSingleWL());
        solver.setDBSimplificationAllowed(true);
        solver.addClause(5, 4, -7);
        solver.addClause(6, 3);
        solver.addClause(1, 7);
        solver.addClause(2, 3);
        OptToSatAdapter minone = new OptToSatAdapter(
                new MinOneDecorator(solver));
        assertTrue(minone.isSatisfiable());
        int[] model = minone.model();
        assertNotNull(model);
        assertEquals(7, model.length);
        assertTrue(minone.isSatisfiable());
        assertEquals(7, minone.model().length);
    }
}
