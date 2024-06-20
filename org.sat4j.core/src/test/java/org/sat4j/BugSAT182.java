package org.sat4j;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.minisat.constraints.CardinalityDataStructure;
import org.sat4j.minisat.constraints.CardinalityDataStructureYanMax;
import org.sat4j.minisat.constraints.CardinalityDataStructureYanMin;
import org.sat4j.minisat.constraints.ClausalDataStructureWL;
import org.sat4j.minisat.constraints.MixedDataStructureDanielHT;
import org.sat4j.minisat.constraints.MixedDataStructureSingleWL;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.Solver;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.SearchEnumeratorListener;
import org.sat4j.tools.SolutionFoundListener;

public class BugSAT182 {

    private void problematicCase(DataStructureFactory dsf)
            throws ContradictionException, TimeoutException {
        Solver<DataStructureFactory> solver = SolverFactory.newGlucose21();
        solver.setDataStructureFactory(dsf);
        solver.addClause(-1, 2);
        SolutionFoundListener sfl = new SolutionFoundListener() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void onSolutionFound(int[] solution) {
                assertNotEquals(0, solution.length);
            }
        };
        SearchEnumeratorListener enumerator = new SearchEnumeratorListener(sfl);
        solver.setSearchListener(enumerator);
        assertTrue(solver.isSatisfiable());
    }

    @Test
    public void firstIssue() throws ContradictionException, TimeoutException {
        problematicCase(new MixedDataStructureDanielHT());
    }

    @Test
    public void secondIssue() throws ContradictionException, TimeoutException {
        problematicCase(new CardinalityDataStructureYanMin());
    }

    @Test
    public void thirdIssue() throws ContradictionException, TimeoutException {
        problematicCase(new CardinalityDataStructureYanMax());
    }

    @Test
    public void fourthIssue() throws ContradictionException, TimeoutException {
        problematicCase(new MixedDataStructureSingleWL());
    }

    @Test
    public void fifthIssue() throws ContradictionException, TimeoutException {
        problematicCase(new ClausalDataStructureWL());
    }

    @Test
    public void sixthIssue() throws ContradictionException, TimeoutException {
        problematicCase(new CardinalityDataStructure());
    }

}
