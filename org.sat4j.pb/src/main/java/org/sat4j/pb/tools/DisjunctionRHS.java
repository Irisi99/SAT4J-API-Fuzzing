/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *******************************************************************************/
package org.sat4j.pb.tools;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.IteratorInt;

public class DisjunctionRHS<T, C extends Comparable<C>> {
    private final IVecInt literals;
    private final DependencyHelper<T, C> helper;

    private final IVec<IConstr> toName = new Vec<IConstr>();

    public DisjunctionRHS(DependencyHelper<T, C> helper, IVecInt literals) {
        this.literals = literals;
        this.helper = helper;
    }

    public ImplicationNamer<T, C> implies(T... things)
            throws ContradictionException {
        IVecInt clause = new VecInt();
        for (T t : things) {
            clause.push(this.helper.getIntValue(t));
        }
        int p;
        IConstr constr;
        for (IteratorInt it = this.literals.iterator(); it.hasNext();) {
            p = it.next();
            clause.push(p);
            constr = this.helper.solver.addClause(clause);
            if (constr == null) {
                throw new IllegalStateException(
                        "Constraints are not supposed to be null when using the helper");
            }
            this.toName.push(constr);
            clause.remove(p);
        }
        return new ImplicationNamer<T, C>(this.helper, this.toName);
    }
}
