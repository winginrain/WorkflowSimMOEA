package org.moeaframework.problem.workflow;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.Subset;
import org.moeaframework.problem.AbstractProblem;
import org.workflowsim.*;
import org.workflowsim.clustering.BasicClustering;

import java.util.*;

public class WorkflowSimProblem extends AbstractProblem {

    /**
     * the task list.
     */
    private List<Task> taskList;


    /**
     * the vm list.
     */
    private List<CondorVM> vmList;

    /**
     * the datacenter list
     */
    private WorkflowDatacenter datacenter;

    private WorkflowPlanner workflowPlanner;

    private BasicClustering basicClustering;


    private Integer maxDepth;

    private Integer minDepth;

    Map<Integer, List<Task>> taskMap;

    public WorkflowSimProblem(int numberOfVariables, int numberOfObjectives) {
        super(numberOfVariables, numberOfObjectives);
    }

    public WorkflowSimProblem(int numberOfVariables, int numberOfObjectives, int numberOfConstraints, int vms) {
        super(numberOfVariables, numberOfObjectives, numberOfConstraints);
    }

    public WorkflowSimProblem(List<Task> taskList, List<CondorVM> vmList, int numberOfObjectives) {
        super(taskList.size(), numberOfObjectives, 0);

        this.vmList = vmList;

        // data center
        this.datacenter = (WorkflowDatacenter) CloudSim.getEntity("Datacenter_0");

        this.workflowPlanner = (WorkflowPlanner) CloudSim.getEntity("planner_0");

        this.basicClustering = new BasicClustering();

        this.taskList = taskList;

        // 获取分配关系,假定在任务开始前
        this.taskMap = new HashMap<>();
        this.maxDepth = 0;
        this.minDepth = Integer.MAX_VALUE;
        for(Task task: this.taskList){
            this.maxDepth = this.maxDepth < task.getDepth()? task.getDepth(): this.maxDepth;
            this.minDepth = this.minDepth > task.getDepth()? task.getDepth(): this.minDepth;
            if(!taskMap.containsKey(task.getDepth())){
                taskMap.put(task.getDepth(), new ArrayList<>());
            }
            taskMap.get(task.getDepth()).add(task);
        }

    }

    @Override
    public void evaluate(Solution solution) {
        List<CondorVM> vmList = this.vmList;
//        List<CondorVM> vmList = new ArrayList<>();
//        for(CondorVM vm: this.vmList){
//            vmList.add(new CondorVM(vm));
//        }


        Queue<Task> taskStack = new LinkedList<>(taskMap.get(this.minDepth));
        boolean[] taskStates = new boolean[this.taskList.size()];
        TaskInfo[] taskInfos = new TaskInfo[this.taskList.size()];
        for(Task task: taskStack){
            int taskIndex = task.getCloudletId() - 1;
            taskInfos[taskIndex] = new TaskInfo(task.getCloudletId(), 0);
        }
        for(CondorVM vm: vmList){
            this.datacenter.resetDelayTime(vm);
        }
        while(!taskStack.isEmpty()){
            Task task = taskStack.poll();
            int taskIndex = task.getCloudletId() - 1;
            TaskInfo taskInfo = taskInfos[taskIndex];
            // 计算任务执行时间以及Objects
            int vmId = EncodingUtils.getSubset(solution.getVariable(taskIndex))[0];
            double estimatedFinishedTime = this.datacenter.getEstimatedFinishTime(this.basicClustering.addTasks2Job(task), this.vmList.get(vmId), taskInfo.getStartTime());
            taskInfo.setVm(vmList.get(vmId));
            taskInfo.setSpendTime(estimatedFinishedTime);

            // 处理任务之间的关系
            List<Task> childList = task.getChildList();
            taskStates[taskIndex] = true;
            for(Task child: childList){
                boolean tag = true;
                double startTime = taskInfo.getFinishTime();
                for(Task pTask: child.getParentList()){
                    int pTaskIndex = pTask.getCloudletId() - 1;
                    TaskInfo pTaskInfo = taskInfos[pTaskIndex];
                    if(!taskStates[pTaskIndex]){
                        tag = false;
                        break;
                    }
                    if(pTaskInfo.getFinishTime() > startTime){
                        startTime = pTaskInfo.getFinishTime();
                    }
                }
                if(tag){
                    int childIndex = child.getCloudletId() - 1;
                    taskInfos[childIndex] = new TaskInfo(child.getCloudletId(), startTime);
                    taskStack.offer(child);
                }
            }
        }
        double[] objects = new double[numberOfObjectives];
        double totalTime = 0;
        double sumTime = 0;
        double cost = 0;
        String output = "";
        for(TaskInfo t: taskInfos){
            if (totalTime < t.getFinishTime()){
                totalTime = t.getFinishTime();
            }
            sumTime += t.getSpendTime();
            CondorVM vm = t.getVm();
            cost += vm.getCost() * t.getSpendTime();
            output += String.format("%d\t%d\t%f\t%f\t%f\t%f\n", t.getTaskId(), t.getVm().getId(), t.getStartTime(),
                    t.getSpendTime(), t.getFinishTime(), t.getCost());
        }
        switch (numberOfObjectives){
            case 3:
                objects[2] = cost;
            case 2:
                objects[1] = sumTime;
            case 1:
            default:
                objects[0] = totalTime;
        }

        solution.setObjectives(objects);
    }

    private class TaskInfo{
        public TaskInfo(int taskId, double startTime){
            this.taskId = taskId;
            this.startTime = startTime;
        }
        private int taskId;
        private double startTime;
        private double spendTime;
        private double finishTime;
        private CondorVM vm;
        private double cost;

        public void setSpendTime(double finishTime) {
            this.spendTime = finishTime - startTime;
            this.finishTime = finishTime ;
        }

        public double getStartTime() {
            return startTime;
        }

        public int getTaskId() {
            return taskId;
        }

        public double getFinishTime() {
            return finishTime;
        }

        public CondorVM getVm() {
            return vm;
        }

        public void setVm(CondorVM vm) {
            this.vm = vm;
        }

        public double getSpendTime() {
            return spendTime;
        }

        public double getCost() {
            return cost;
        }
    }

    @Override
    public Solution newSolution() {
        Solution solution = new Solution(numberOfVariables, numberOfObjectives);

        solution.setVariable(0, new Subset(1, vmList.size()));
        for (int i = 1; i < numberOfVariables; i++) {
            solution.setVariable(i, new Subset(1, vmList.size()));
        }

        return solution;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public List<CondorVM> getVmList() {
        return vmList;
    }
}
