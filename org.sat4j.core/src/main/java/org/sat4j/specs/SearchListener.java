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
package org.sat4j.specs;

import java.io.Serializable;

import org.sat4j.annotations.Feature;

/**
 * Interface to the solver main steps. Useful for integrating search
 * visualization or debugging.
 * 
 * (that class moved from org.sat4j.minisat.core in earlier version of SAT4J).
 * 
 * @author daniel
 * @since 2.1
 */
@Feature("searchlistener")
public interface SearchListener<S extends ISolverService>
        extends UnitClauseConsumer, Serializable {

    static <T extends ISolverService> SearchListener<T> voidSearchListener() {
        return new SearchListener<>() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return "none";
            }
        };
    }

    /**
     * Provide access to the solver's controllable interface.
     * 
     * @param solverService
     *            a way to safely control the solver.
     * @since 2.3.2
     */
    default void init(S solverService) {

    }

    /**
     * decision variable
     * 
     * @param p
     */
    default void assuming(int p) {

    }

    /**
     * Unit propagation
     * 
     * @param p
     */
    default void propagating(int p) {

    }

    /**
     * Fixes the truth value of a variable before propagating it. For all calls
     * to enqueueing(p,_) there should be a call to propagating(p) unless a
     * conflict is found.
     * 
     * @param p
     *            a literal
     * @param reason
     *            it's reason
     */
    default void enqueueing(int p, IConstr reason) {

    }

    /**
     * backtrack on a decision variable
     * 
     * @param p
     */
    default void backtracking(int p) {

    }

    /**
     * adding forced variable (conflict driven assignment)
     */
    default void adding(int p) {

    }

    /**
     * learning a new clause
     * 
     * @param c
     */
    default void learn(IConstr c) {

    }

    /**
     * delete a clause
     */
    default void delete(IConstr c) {

    }

    /**
     * a conflict has been found.
     * 
     * @param confl
     *            a conflict
     * @param dlevel
     *            the decision level
     * @param trailLevel
     *            the trail level
     * 
     */
    default void conflictFound(IConstr confl, int dlevel, int trailLevel) {

    }

    /**
     * a conflict has been found while propagating values.
     * 
     * @param p
     *            the conflicting value.
     */
    default void conflictFound(int p) {

    }

    /**
     * a solution is found.
     * 
     * @param model
     *            the model found
     * @param lazyModel
     *            a way to access the model lazily
     * 
     */
    default void solutionFound(int[] model, RandomAccessModel lazyModel) {

    }

    /**
     * starts a propagation
     */
    default void beginLoop() {

    }

    /**
     * Start the search.
     * 
     */
    default void start() {

    }

    /**
     * End the search.
     * 
     * @param result
     *            the result of the search.
     */
    default void end(Lbool result) {

    }

    /**
     * The solver restarts the search.
     */
    default void restarting() {

    }

    /**
     * The solver is asked to backjump to a given decision level.
     * 
     * @param backjumpLevel
     */
    default void backjump(int backjumpLevel) {

    }

    /**
     * The solver is going to delete some learned clauses.
     */
    default void cleaning() {

    }

    /**
     * A new blocking clause is added.
     * 
     * @since 3.0
     */
    default void blockClause(IVecInt literals) {

    }

    /**
     * A new call to the SAT solver is performed.
     * 
     * @param assumptions
     *            the assumptions used in incremental SAT
     * @param global
     *            is the call part of a global process? (optimization,
     *            incremental session)
     * @since 3.0
     */
    default void checkSatisfiability(IVecInt assumptions, boolean global) {

    }

    /**
     * Add a new clause to the SAT solver.
     * 
     * @param clause
     *            a clause in Dimacs format.
     * @since 3.0
     */
    default void addClause(IVecInt clause) {

    }

    /**
     * The constraint is generated by the conflict analysis but not added to the
     * constraints.
     * 
     * There will be no unit propagation on that constraint. It may be used
     * however in conflict analysis.
     * 
     * @param c
     *            a constraint entailed by the current constraints in the
     *            solver.
     * @since 3.0
     */
    default void ignore(IConstr c) {

    }
}
