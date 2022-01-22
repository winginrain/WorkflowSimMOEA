package org.moeaframework.core.operator;

import org.moeaframework.core.Initialization;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.workflowsim.WorkflowPlanner;

public class HEFTInitialization implements Initialization {
    /**
     * The problem.
     */
    protected final Problem problem;

    /**
     * The initial population size.
     */
    protected final int populationSize;

    /**
     * Constructs a random initialization operator.
     *
     * @param problem the problem
     * @param populationSize the initial population size
     */
    public HEFTInitialization(Problem problem, int populationSize) {
        super();
        this.problem = problem;
        this.populationSize = populationSize;
    }

    @Override
    public Solution[] initialize() {
        Solution[] initialPopulation = new Solution[populationSize];

        for (int i = 0; i < populationSize; i++) {
            Solution solution = problem.newSolution();

            for (int j = 0; j < solution.getNumberOfVariables(); j++) {
                solution.getVariable(j).randomize();
            }

            initialPopulation[i] = solution;
        }
        return initialPopulation;
    }
}
