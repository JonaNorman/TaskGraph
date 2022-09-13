package com.jonanorman.android.taskgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TaskGraph {

    private Set<TaskGraphCallback> callbackSet;
    private Set<Task> mainTaskSet;
    private Task firstTask;
    private Task lastTask;

    TaskGraph(Builder builder) {
        this.callbackSet = new HashSet<>();
        this.callbackSet.addAll(builder.callbackSet);
        this.mainTaskSet = new HashSet<>();
        if (builder.firstTaskBuilder != null) {
            Task task = builder.firstTaskBuilder.build();
            if (runInProcess(task)) {
                firstTask = task;
            }
        }
        if (builder.lastTaskBuilder != null) {
            Task task = builder.lastTaskBuilder.build();
            if (runInProcess(task)) {
                lastTask = task;
            }
        }
        for (Task.Builder taskBuilder : builder.taskBuilderSet) {
            Task task = taskBuilder.build();
            if (runInProcess(task)) {
                this.mainTaskSet.add(task);
            }
        }
    }

    private boolean runInProcess(Task task) {
        if (!TaskGraphModule.isMainProcess() && task.isOnlyMainProcess()) {
            return false;
        }
        return true;
    }

    void runStart() {
        for (TaskGraphCallback taskGraphCallback : callbackSet) {
            taskGraphCallback.onTaskGraphStart();
        }
    }

    void runEnd() {
        for (TaskGraphCallback taskGraphCallback : callbackSet) {
            taskGraphCallback.onTaskGraphEnd();
        }
    }

    public DirectedGraph generateDirectGraph() {
        long startTime = System.currentTimeMillis();
        DirectedGraph directedGraph = new DirectedGraph();
        Map<Task, DirectedGraph.Vertex<Task>> taskMap = new HashMap<>();
        Map<Object, List<Task>> dependsFindsMap = new HashMap<>();

        Set<Task> allSet = new HashSet<>();
        if (firstTask != null) {
            allSet.add(firstTask);
        }
        if (lastTask != null) {
            allSet.add(lastTask);
        }
        allSet.addAll(mainTaskSet);

        for (Task task : allSet) {
            DirectedGraph.Vertex<Task> vertex = new DirectedGraph.Vertex<>(task);
            taskMap.put(task, vertex);
            List<Task> taskList = dependsFindsMap.get(task.getName());
            if (taskList == null) {
                taskList = new ArrayList<>();
                dependsFindsMap.put(task.getName(), taskList);
            }
            if (!taskList.contains(task)) {
                taskList.add(task);
            }

            taskList = dependsFindsMap.get(task.getTaskRunnable());
            if (taskList == null) {
                taskList = new ArrayList<>();
                dependsFindsMap.put(task.getTaskRunnable(), taskList);
            }
            if (!taskList.contains(task)) {
                taskList.add(task);
            }
            directedGraph.addVertex(vertex);
        }

        for (Task task : allSet) {
            DirectedGraph.Vertex<Task> to = taskMap.get(task);
            Set<Object> dependsSet = task.getDependsOnSet();
            for (Object depend : dependsSet) {
                List<Task> dependTaskList = dependsFindsMap.get(depend);
                if (dependTaskList != null) {
                    for (Task dependsTask : dependTaskList) {
                        DirectedGraph.Vertex<Task> from = taskMap.get(dependsTask);
                        if (from != null) {
                            directedGraph.addEdge(new DirectedGraph.Edge(from, to));
                        }
                    }
                }
            }
        }
        if (firstTask != null) {
            Set<Task> firstToSet = new HashSet<>(allSet);
            DirectedGraph.Vertex<Task> firstVertex = taskMap.get(firstTask);
            Queue<DirectedGraph.Vertex<Task>> queue = new LinkedList<>();
            queue.offer(firstVertex);
            Set<DirectedGraph.Vertex<Task>> searchedSet = new HashSet<>();
            while (!queue.isEmpty()) {
                DirectedGraph.Vertex<Task> vertex = queue.poll();
                if (searchedSet.contains(vertex)) {
                    continue;
                }
                searchedSet.add(vertex);
                firstToSet.remove(vertex.getValue());
                Set<DirectedGraph.Edge<Task>> incomingEdgeSet = directedGraph.getIncomingEdgeSet(vertex);
                for (DirectedGraph.Edge<Task> taskEdge : incomingEdgeSet) {
                    DirectedGraph.Vertex from = taskEdge.getFrom();
                    queue.offer(from);
                }
            }
            for (Task task : firstToSet) {
                DirectedGraph.Vertex<Task> to = taskMap.get(task);
                directedGraph.addEdge(new DirectedGraph.Edge(firstVertex, to));
            }
        }

        if (lastTask != null) {
            Set<Task> lastFromSet = new HashSet<>(allSet);
            DirectedGraph.Vertex<Task> lastVertex = taskMap.get(lastTask);
            Queue<DirectedGraph.Vertex<Task>> queue = new LinkedList<>();
            queue.offer(lastVertex);
            Set<DirectedGraph.Vertex<Task>> searchedSet = new HashSet<>();
            while (!queue.isEmpty()) {
                DirectedGraph.Vertex<Task> vertex = queue.poll();
                if (searchedSet.contains(vertex)) {
                    continue;
                }
                searchedSet.add(vertex);
                lastFromSet.remove(vertex.getValue());
                Set<DirectedGraph.Edge<Task>> outgoingEdgeSet = directedGraph.getOutgoingEdgeSet(vertex);
                for (DirectedGraph.Edge<Task> taskEdge : outgoingEdgeSet) {
                    DirectedGraph.Vertex to = taskEdge.getFrom();
                    queue.offer(to);
                }
            }
            for (Task task : lastFromSet) {
                DirectedGraph.Vertex<Task> from = taskMap.get(task);
                directedGraph.addEdge(new DirectedGraph.Edge(from, lastVertex));
            }
        }
        TaskGraphModule.logDebug("generateDirectGraph take time " + (System.currentTimeMillis() - startTime) + "ms");
        return directedGraph;
    }

    public static class Builder {

        private Set<TaskGraphCallback> callbackSet;
        private Set<Task.Builder> taskBuilderSet;
        private Task.Builder firstTaskBuilder;
        private Task.Builder lastTaskBuilder;

        public Builder() {
            this.callbackSet = new HashSet<>();
            this.taskBuilderSet = new HashSet<>();
        }


        public Builder setFirstTask(Task.Builder builder) {
            firstTaskBuilder = builder;
            return this;
        }

        public Builder setLastTask(Task.Builder builder) {
            lastTaskBuilder = builder;
            return this;
        }

        public Builder addTask(Task.Builder builder) {
            taskBuilderSet.add(builder);
            return this;
        }

        public Builder removeTask(Task.Builder builder) {
            taskBuilderSet.remove(builder);
            return this;
        }

        public Builder clearTask(Task.Builder builder) {
            taskBuilderSet.clear();
            return this;
        }

        public Builder addTaskGraphCallback(TaskGraphCallback callback) {
            callbackSet.add(callback);
            return this;
        }

        public Builder removeTaskGraphCallback(TaskGraphCallback callback) {
            callbackSet.remove(callback);
            return this;
        }

        public Builder clearTaskGraphCallback(TaskGraphCallback callback) {
            callbackSet.clear();
            return this;
        }

        public void execute() {
            TaskGraphExecutor.getDefault().execute(this);
        }

        TaskGraph builder() {
            return new TaskGraph(this);
        }


        @Override
        public Object clone() throws CloneNotSupportedException {
            Builder clone = (Builder) super.clone();
            clone.callbackSet = new HashSet<>();
            for (TaskGraphCallback callback : callbackSet) {
                clone.callbackSet.add(callback);
            }
            clone.taskBuilderSet = new HashSet<>();
            for (Task.Builder builder : taskBuilderSet) {
                clone.taskBuilderSet.add(builder.clone());
            }
            if (firstTaskBuilder != null) {
                clone.firstTaskBuilder = firstTaskBuilder.clone();
            }
            if (lastTaskBuilder != null) {
                clone.lastTaskBuilder = lastTaskBuilder.clone();
            }
            return clone;
        }
    }

    public interface TaskGraphCallback {

        void onTaskGraphStart();

        void onTaskGraphEnd();
    }


}
