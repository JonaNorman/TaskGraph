package com.jonanorman.android.taskgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class TaskGraphController {

    private final Set<TaskGraph.TaskGraphListener> graphListenerSet;
    private final Set<Task.TaskListener> taskListenerSet;
    private final Set<TaskController> mainTaskControllerSet;
    private final TaskGraph taskGraph;
    private TaskController firstTaskController;
    private TaskController lastTaskController;

    private boolean mainThread;
    private boolean started;
    private boolean ended;
    private boolean canceled;
    private Object statusSync = new Object();
    private DirectedGraph directedGraph;
    private long startTime;
    private long costTime;

    TaskGraphController(TaskGraph taskGraph) {
        this.taskGraph = taskGraph;
        this.mainThread = taskGraph.mainThread;
        this.graphListenerSet = new HashSet<>(taskGraph.graphListenerSet);
        this.taskListenerSet = new HashSet<>(taskGraph.taskListenerSet);
        this.mainTaskControllerSet = new HashSet<>();
        if (taskGraph.firstTask != null) {
            TaskController taskController = new TaskController(taskGraph.firstTask, this);
            if (runInProcess(taskController)) {
                firstTaskController = taskController;
            }
        }
        if (taskGraph.lastTask != null) {
            TaskController taskController = new TaskController(taskGraph.lastTask, this);
            if (runInProcess(taskController)) {
                lastTaskController = taskController;
            }
        }
        for (Task task : taskGraph.taskSet) {
            TaskController taskController = new TaskController(task, this);
            boolean run = runInProcess(taskController);
            if (run) {
                this.mainTaskControllerSet.add(taskController);
            }
        }
    }

    private boolean runInProcess(TaskController taskController) {
        if (!TaskGraphModule.isMainProcess() && taskController.onlyMainProcess) {
            return false;
        }
        return true;
    }

    void runStart() {
        synchronized (statusSync) {
            if (isStarted() || isFinished()) {
                return;
            }
            started = true;
        }
        logStart();
        for (TaskGraph.TaskGraphListener taskGraphCallback : graphListenerSet) {
            taskGraphCallback.onTaskGraphStart(taskGraph);
        }
    }

    void runEnd() {
        synchronized (statusSync) {
            if (isFinished()) {
                return;
            }
            ended = true;
        }
        logEnd();
        if (TaskGraphModule.isMainThread()) {
            TaskGraphExecutor.getDefault().getThreadPoolExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    callTaskGraphEndListener();
                }
            });
        } else {
            callTaskGraphEndListener();
        }

    }

    private void callTaskGraphEndListener() {
        for (TaskGraph.TaskGraphListener taskGraphCallback : graphListenerSet) {
            taskGraphCallback.onTaskGraphEnd(taskGraph, costTime, TimeUnit.MILLISECONDS);
        }
    }

    void runCancel(TaskCancelException cancelException) {
        synchronized (statusSync) {
            if (isFinished()) {
                return;
            }
            canceled = true;
        }
        TaskGraphModule.logWarn(taskGraph.name+ cancelException.getMessage());
        if (TaskGraphModule.isMainThread()) {
            TaskGraphExecutor.getDefault().getThreadPoolExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    callTaskGraphCancelListener(cancelException);
                }
            });
        } else {
            callTaskGraphCancelListener(cancelException);
        }
    }

    private void callTaskGraphCancelListener(TaskCancelException cancelException) {
        for (TaskGraph.TaskGraphListener taskGraphCallback : graphListenerSet) {
            taskGraphCallback.onTaskGraphCancel(taskGraph, cancelException);
        }
    }


    void logTaskStart(Task task) {
        for (Task.TaskListener taskListener : taskListenerSet) {
            taskListener.doFirst(task);
        }
    }

    void logTaskLast(Task task, long time, TimeUnit timeUnit) {
        for (Task.TaskListener taskListener : taskListenerSet) {
            taskListener.doLast(task, time, timeUnit);
        }
    }

    private void logStart() {
        startTime = System.currentTimeMillis();
        TaskGraphModule.logVerbose(taskGraph.name + " start...");
    }

    private void logEnd() {
        costTime = System.currentTimeMillis() - startTime;
        TaskGraphModule.logDebug(taskGraph.name+" end " + costTime + " ms");
    }

    public boolean isStarted() {
        synchronized (statusSync) {
            return started;
        }
    }

    public boolean isEnded() {
        synchronized (statusSync) {
            return ended;
        }
    }

    public boolean isCanceled() {
        synchronized (statusSync) {
            return ended;
        }
    }

    public boolean isFinished() {
        synchronized (statusSync) {
            return ended || canceled;
        }
    }

    public DirectedGraph getDirectedGraph() {
        if (directedGraph != null) {
            return directedGraph;
        }
        long startTime = System.currentTimeMillis();
        directedGraph = new DirectedGraph();
        Map<TaskController, DirectedGraph.Vertex<TaskController>> taskControllerVertexMap = new HashMap<>();
        Map<Object, List<TaskController>> dependsFindsMap = new HashMap<>();
        Set<TaskController> allSet = new HashSet<>();
        if (firstTaskController != null) {
            allSet.add(firstTaskController);
        }
        if (lastTaskController != null) {
            allSet.add(lastTaskController);
        }
        allSet.addAll(mainTaskControllerSet);

        for (TaskController task : allSet) {
            DirectedGraph.Vertex<TaskController> vertex = new DirectedGraph.Vertex<>(task);
            taskControllerVertexMap.put(task, vertex);
            List<TaskController> taskList = dependsFindsMap.get(task.name);
            if (taskList == null) {
                taskList = new ArrayList<>();
                dependsFindsMap.put(task.name, taskList);
            }
            if (!taskList.contains(task)) {
                taskList.add(task);
            }

            taskList = dependsFindsMap.get(task);
            if (taskList == null) {
                taskList = new ArrayList<>();
                dependsFindsMap.put(task, taskList);
            }
            if (!taskList.contains(task)) {
                taskList.add(task);
            }
            directedGraph.addVertex(vertex);
        }

        for (TaskController task : allSet) {
            DirectedGraph.Vertex<TaskController> to = taskControllerVertexMap.get(task);
            Set<Object> dependsSet = task.dependsOnSet;
            for (Object depend : dependsSet) {
                List<TaskController> dependTaskList = dependsFindsMap.get(depend);
                if (dependTaskList != null) {
                    for (TaskController dependsTask : dependTaskList) {
                        DirectedGraph.Vertex<TaskController> from = taskControllerVertexMap.get(dependsTask);
                        if (from != null) {
                            directedGraph.addEdge(new DirectedGraph.Edge(from, to));
                        }
                    }
                }
            }
        }
        if (firstTaskController != null) {
            Set<TaskController> firstToSet = new HashSet<>(allSet);
            DirectedGraph.Vertex<TaskController> firstVertex = taskControllerVertexMap.get(firstTaskController);
            Queue<DirectedGraph.Vertex<TaskController>> queue = new LinkedList<>();
            queue.offer(firstVertex);
            Set<DirectedGraph.Vertex<TaskController>> searchedSet = new HashSet<>();
            while (!queue.isEmpty()) {
                DirectedGraph.Vertex<TaskController> vertex = queue.poll();
                if (searchedSet.contains(vertex)) {
                    continue;
                }
                searchedSet.add(vertex);
                firstToSet.remove(vertex.getValue());
                Set<DirectedGraph.Edge<TaskController>> incomingEdgeSet = directedGraph.getIncomingEdgeSet(vertex);
                for (DirectedGraph.Edge<TaskController> taskEdge : incomingEdgeSet) {
                    DirectedGraph.Vertex from = taskEdge.getFrom();
                    queue.offer(from);
                }
            }
            for (TaskController task : firstToSet) {
                DirectedGraph.Vertex<TaskController> to = taskControllerVertexMap.get(task);
                directedGraph.addEdge(new DirectedGraph.Edge(firstVertex, to));
            }
        }

        if (lastTaskController != null) {
            Set<TaskController> lastFromSet = new HashSet<>(allSet);
            DirectedGraph.Vertex<TaskController> lastVertex = taskControllerVertexMap.get(lastTaskController);
            Queue<DirectedGraph.Vertex<TaskController>> queue = new LinkedList<>();
            queue.offer(lastVertex);
            Set<DirectedGraph.Vertex<TaskController>> searchedSet = new HashSet<>();
            while (!queue.isEmpty()) {
                DirectedGraph.Vertex<TaskController> vertex = queue.poll();
                if (searchedSet.contains(vertex)) {
                    continue;
                }
                searchedSet.add(vertex);
                lastFromSet.remove(vertex.getValue());
                Set<DirectedGraph.Edge<TaskController>> outgoingEdgeSet = directedGraph.getOutgoingEdgeSet(vertex);
                for (DirectedGraph.Edge<TaskController> taskEdge : outgoingEdgeSet) {
                    DirectedGraph.Vertex to = taskEdge.getFrom();
                    queue.offer(to);
                }
            }
            for (TaskController task : lastFromSet) {
                DirectedGraph.Vertex<TaskController> from = taskControllerVertexMap.get(task);
                directedGraph.addEdge(new DirectedGraph.Edge(from, lastVertex));
            }
        }
        if (TaskGraphModule.isLogGraphViz()) {
            TaskGraphModule.logInfo(taskGraph.name+" graphviz:\n" + directedGraph.getGraphPic());
        }
        TaskGraphModule.logDebug(taskGraph.name+" calculate directedGraph " + (System.currentTimeMillis() - startTime) + "ms");
        return directedGraph;
    }

    public boolean isMainThread() {
        return mainThread;
    }
}
