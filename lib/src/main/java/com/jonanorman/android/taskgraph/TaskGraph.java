package com.jonanorman.android.taskgraph;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TaskGraph {

    Set<TaskGraphListener> listenerSet;
    Set<Task> taskSet;
    Task firstTask;
    Task lastTask;

    public TaskGraph() {
        this.listenerSet = new HashSet<>();
        this.taskSet = new HashSet<>();
    }


    public TaskGraph setFirstTask(Task task) {
        firstTask = task;
        return this;
    }

    public TaskGraph setLastTask(Task task) {
        lastTask = task;
        return this;
    }

    public TaskGraph addTask(Task task) {
        taskSet.add(task);
        return this;
    }

    public TaskGraph removeTask(Task task) {
        taskSet.remove(task);
        return this;
    }

    public TaskGraph clearTask() {
        taskSet.clear();
        return this;
    }

    public TaskGraph addTaskGraphListener(TaskGraphListener listener) {
        listenerSet.add(listener);
        return this;
    }

    public TaskGraph removeTaskGraphListener(TaskGraphListener listener) {
        listenerSet.remove(listener);
        return this;
    }

    public TaskGraph clearTaskGraphListener() {
        listenerSet.clear();
        return this;
    }

    public void execute() {
        TaskGraphExecutor.getDefault().execute(this);
    }

    public void execute(TaskGraphExecutor executor) {
        executor.execute(this);
    }

    public interface TaskGraphListener {

        void onTaskGraphStart(TaskGraph taskGraph);

        void onTaskGraphEnd(TaskGraph taskGraph,long time, TimeUnit timeUnit);

        void onTaskGraphCancel(TaskGraph taskGraph, TaskCancelException cancelException);
    }
}
