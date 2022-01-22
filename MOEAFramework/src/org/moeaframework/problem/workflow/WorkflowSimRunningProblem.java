package org.moeaframework.problem.workflow;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.Subset;
import org.moeaframework.problem.AbstractProblem;
import org.workflowsim.*;
import org.workflowsim.clustering.BasicClustering;

import java.util.*;
import java.util.stream.Collectors;

public class WorkflowSimRunningProblem extends AbstractProblem {

    /**
     * the task list.
     */
    private List<Job> taskList;

    private Map<Integer, Integer> indexMap;

    private List<Job> targetJob;

    /**
     * the vm list.
     */
    private List<? extends Vm> vmList;

    /**
     * the datacenter list
     */
    private WorkflowDatacenter datacenter;

    private WorkflowPlanner workflowPlanner;

    private BasicClustering basicClustering;

    List<Job> jobRunning;

    public WorkflowSimRunningProblem(int numberOfVariables, int numberOfObjectives) {
        super(numberOfVariables, numberOfObjectives);
    }

    public WorkflowSimRunningProblem(int numberOfVariables, int numberOfObjectives, int numberOfConstraints, int vms) {
        super(numberOfVariables, numberOfObjectives, numberOfConstraints);
    }

    public WorkflowSimRunningProblem(List<Job> jobList, List<? extends Vm> vmList, int numberOfObjectives) {
        this(jobList, new ArrayList<>(), vmList, numberOfObjectives);
    }

    public WorkflowSimRunningProblem(List<Job> jobList, List<Job> allocatedJobList,  List<? extends Vm> vmList, int numberOfObjectives) {
        super(jobList.size(), numberOfObjectives, 0);

        this.vmList = vmList;

        // data center
        this.datacenter = (WorkflowDatacenter) CloudSim.getEntity("Datacenter_0");

        this.workflowPlanner = (WorkflowPlanner) CloudSim.getEntity("planner_0");

        this.basicClustering = new BasicClustering();

        WorkflowEngine wfEngine = workflowPlanner.getWorkflowEngine();

        // 获取分配关系,假定在任务开始前
        this.jobRunning = new ArrayList<>();

        this.taskList = new ArrayList<>();

        this.indexMap = new HashMap<>();

        this.targetJob = jobList;

        List<Job> execJob = wfEngine.getJobsSubmittedList().stream().map(cloudlet -> (Job) cloudlet).filter(cloudlet -> cloudlet.getStatus() == Cloudlet.INEXEC ||
                cloudlet.getStatus() == Cloudlet.CREATED || cloudlet.getStatus() == Cloudlet.QUEUED).collect(Collectors.toList());

        addTask(allocatedJobList);
        addTask(jobList);
        addTask(execJob);
        addTask(wfEngine.getJobsList());

        this.jobRunning.addAll(execJob);
        this.jobRunning.addAll(allocatedJobList);
        for(Cloudlet cloudlet: wfEngine.getJobsList()){
            Job job = (Job) cloudlet;
            boolean flag = true;
            for (Object p: job.getParentList()) {
                if (taskList.contains((Job) p)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                this.jobRunning.add(job);
            }

        }

    }

    private void addTask(List<Job> taskList) {
        for(Job job: taskList){
            this.indexMap.put(job.getCloudletId(), this.taskList.size());
            this.taskList.add(job);
        }
    }

    @Override
    public void evaluate(Solution solution) {
        List<? extends Vm> vmList = this.vmList;
//        List<CondorVM> vmList = new ArrayList<>();
//        for(CondorVM vm: this.vmList){
//            vmList.add(new CondorVM(vm));
//        }

        Queue<Job> taskStack = new LinkedList<>();
        taskStack.addAll(jobRunning);

    taskStack.addAll(targetJob);

        boolean[] taskStates = new boolean[this.taskList.size()];
        TaskInfo[] taskInfos = new TaskInfo[this.taskList.size()];
        for(Job task: jobRunning){
            int taskIndex = this.indexMap.get(task.getCloudletId());
            taskInfos[taskIndex] = new TaskInfo(task.getCloudletId(), task.getExecStartTime());
        }
        for(Job task: targetJob){
            int taskIndex = this.indexMap.get(task.getCloudletId());
            taskInfos[taskIndex] = new TaskInfo(task.getCloudletId(), task.getExecStartTime());
        }


        for(Vm vm: vmList){
            this.datacenter.resetDelayTime((CondorVM) vm);
        }
        while(!taskStack.isEmpty()){
            Job job = taskStack.poll();
            int taskIndex = this.indexMap.get(job.getCloudletId());
            TaskInfo taskInfo = taskInfos[taskIndex];
            // 计算任务执行时间以及Objects
            int vmId = job.getVmId();
            if(targetJob.contains(job)){
                int index = targetJob.indexOf(job);
                vmId = EncodingUtils.getSubset(solution.getVariable(index))[0];
            }
            double estimatedFinishedTime = this.datacenter.getEstimatedFinishTime(job, (CondorVM) this.vmList.get(vmId), taskInfo.getStartTime());
            taskInfo.setVm((CondorVM)vmList.get(vmId));
            taskInfo.setSpendTime(estimatedFinishedTime);

            // 处理任务之间的关系
            List<Task> childList = job.getChildList();
            taskStates[taskIndex] = true;
            for(Task c: childList){
                Job child = (Job) c;
                boolean tag = true;
                double startTime = taskInfo.getFinishTime();
                for(Object p: child.getParentList()){
                    Job pTask = (Job) p;
                    if(!this.indexMap.containsKey(pTask.getCloudletId())) continue;
                    int pTaskIndex = this.indexMap.get(pTask.getCloudletId());
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
                    int childIndex = this.indexMap.get(child.getCloudletId());
                    taskInfos[childIndex] = new TaskInfo(child.getCloudletId(), startTime);
                    taskStack.offer(child);
                }
            }
        }
        double[] objects = new double[numberOfObjectives];
        double totalTime = 0;
        double sumTime = 0;
        double cost = 0;
//        String output = "";
        for(TaskInfo t: taskInfos){
            if(t == null) continue;
            if (totalTime < t.getFinishTime()){
                totalTime = t.getFinishTime();
            }
            sumTime += t.getSpendTime();
            CondorVM vm = t.getVm();
            cost += vm.getCost() * t.getSpendTime();
//            output += String.format("%d\t%d\t%f\t%f\t%f\t%f\n", t.getTaskId(), t.getVm().getId(), t.getStartTime(),
//                    t.getSpendTime(), t.getFinishTime(), t.getCost());
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

    public List<Job> getTaskList() {
        return taskList;
    }

    public List<? extends Vm> getVmList() {
        return vmList;
    }
}
