package com.jonanorman.android.taskgraph;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TaskGraph {
    private static AtomicLong TASK_INIT_NUMBER = new AtomicLong();
    Set<TaskGraphListener> graphListenerSet;
    Set<Task.TaskListener> taskListenerSet;
    Set<Task> taskSet;
    Task firstTask;
    Task lastTask;
    String name;
    boolean mainThread;

    public TaskGraph() {
        this(null);
    }


    public TaskGraph(String name) {
        if (name == null) {
            name = getClass().getSimpleName() + "-" + TASK_INIT_NUMBER.incrementAndGet();
        }
        this.name = name;
        this.graphListenerSet = new HashSet<>();
        this.taskListenerSet = new HashSet<>();
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
        graphListenerSet.add(listener);
        return this;
    }

    public TaskGraph removeTaskGraphListener(TaskGraphListener listener) {
        graphListenerSet.remove(listener);
        return this;
    }

    public TaskGraph clearTaskGraphListener() {
        graphListenerSet.clear();
        return this;
    }

    public TaskGraph addTaskListener(Task.TaskListener listener) {
        taskListenerSet.add(listener);
        return this;
    }

    public TaskGraph removeTaskListener(Task.TaskListener listener) {
        taskListenerSet.remove(listener);
        return this;
    }

    public TaskGraph clearTaskListener() {
        taskListenerSet.clear();
        return this;
    }

    public String getName() {
        return name;
    }

    public void execute() {
        TaskGraphExecutor.getDefault().execute(this);
    }

    public TaskGraph setMainThread(boolean mainThread) {
        this.mainThread = mainThread;
        return this;
    }

    public boolean isMainThread() {
        return mainThread;
    }

    public void execute(TaskGraphExecutor executor) {
        executor.execute(this);
    }

    public interface TaskGraphListener {

        void onTaskGraphStart(TaskGraph taskGraph);

        void onTaskGraphEnd(TaskGraph taskGraph, long time, TimeUnit timeUnit);

        void onTaskGraphCancel(TaskGraph taskGraph, TaskCancelException cancelException);
    }
}
