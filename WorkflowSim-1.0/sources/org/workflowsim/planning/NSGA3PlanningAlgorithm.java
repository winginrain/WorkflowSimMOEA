package org.workflowsim.planning;

import org.cloudbus.cloudsim.core.CloudSim;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.workflow.WorkflowSimProblem;
import org.workflowsim.Task;

public class NSGA3PlanningAlgorithm extends BasePlanningAlgorithm{

    @Override
    public void run() throws Exception {
        int taskNum = getTaskList().size();
        int [] allocation = new int[taskNum + 1];
        WorkflowSimProblem workflowSimProblem = new WorkflowSimProblem(getTaskList(),
                getVmList(), 2);
        NondominatedPopulation result = new Executor()
                .withProblem(workflowSimProblem)
                .withAlgorithm("NSGAIII")
                .withMaxEvaluations(10000)
                .run();
        Solution solution = result.get(0);
        workflowSimProblem.evaluate(solution);
        for (Solution s : result){
            if(s.getObjective(0) < solution.getObjective(0)){
                solution = s;
            }
        }
        for(int i = 0; i < taskNum; i++){
            Task task = getTaskList().get(i);
            int vmIndex = EncodingUtils.getSubset(solution.getVariable(task.getCloudletId() - 1))[0];
            allocation[task.getCloudletId()] = vmIndex;
            task.setVmId(vmIndex);
        }
        System.out.println("NSGAIII planner done");
    }
}
