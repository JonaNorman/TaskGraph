package com.jonanorman.android.taskgraph;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Task implements Cloneable, Runnable {
    private static AtomicLong TASK_INIT_NUMBER = new AtomicLong();

    final Set<TaskListener> listenerSet;
    final Set<TaskInterceptor> taskInterceptorSet;
    final Set<Object> dependsSet;
    boolean mainThread;
    boolean onlyMainProcess;
    String name;
    Runnable runnable;

    public Task() {
        this((String) null);
    }

    public Task(String name) {
        this(name, null);
    }

    public Task(Runnable runnable) {
        this(null, runnable);
    }

    public Task(String name, Runnable runnable) {
        this(name, runnable, false, true);
    }

    public Task(String name, Runnable runnable, boolean mainThread, boolean onlyMainProcess) {
        if (name == null) {
            name = "Task-" + TASK_INIT_NUMBER.incrementAndGet();
        }
        this.runnable = runnable;
        this.name = name;
        this.mainThread = mainThread;
        this.onlyMainProcess = onlyMainProcess;
        this.listenerSet = new HashSet<>();
        this.dependsSet = new HashSet<>();
        this.taskInterceptorSet = new LinkedHashSet<>();
    }

    public Task(Task task) {
        this((Runnable) null, task);
    }

    public Task(Runnable runnable, Task task) {
        this.dependsSet = new HashSet<>();
        this.dependsSet.addAll(task.dependsSet);
        this.mainThread = task.mainThread;
        this.onlyMainProcess = task.onlyMainProcess;
        this.runnable = runnable;
        this.listenerSet = new HashSet<>();
        this.listenerSet.addAll(task.listenerSet);
        this.taskInterceptorSet = new HashSet<>();
        this.taskInterceptorSet.addAll(task.taskInterceptorSet);
    }


    public boolean isMainThread() {
        return mainThread;
    }

    public Task setMainThread(boolean mainThread) {
        this.mainThread = mainThread;
        return this;
    }

    public boolean isOnlyMainProcess() {
        return onlyMainProcess;
    }

    public Task setOnlyMainProcess(boolean onlyMainProcess) {
        this.onlyMainProcess = onlyMainProcess;
        return this;
    }

    public Task setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public Task addTaskListener(TaskListener taskListener) {
        listenerSet.add(taskListener);
        return this;
    }

    public Task removeTaskListener(TaskListener taskListener) {
        listenerSet.add(taskListener);
        return this;
    }

    public Task clearTaskListener() {
        listenerSet.clear();
        return this;
    }

    public Task dependsOn(String name) {
        dependsSet.add(name);
        return this;
    }

    public Task dependsOn(Task task) {
        dependsSet.add(task);
        return this;
    }


    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (!(that instanceof Task)) return false;
        Task task = (Task) that;
        return task != that;
    }


    public Task dependsOn(String... names) {
        for (String name : names) {
            dependsSet.add(name);
        }
        return this;
    }

    public Task dependsOn(Task... tasks) {
        for (Task task : tasks) {
            dependsSet.add(task);
        }
        return this;
    }


    public Task clearDepends() {
        dependsSet.clear();
        return this;
    }

    public Task addTaskInterceptor(TaskInterceptor taskInterceptor) {
        taskInterceptorSet.add(taskInterceptor);
        return this;
    }

    public Task removeTaskInterceptor(TaskInterceptor taskInterceptor) {
        taskInterceptorSet.add(taskInterceptor);
        return this;
    }

    public Task clearTaskInterceptor() {
        taskInterceptorSet.clear();
        return this;
    }

    @Override
    public void run() {
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override
    public String toString() {
        return "Task " + name + " [mainThread: " + mainThread + "onlyMainProcess: " + onlyMainProcess + "]";
    }

    public interface TaskListener {

        void doFirst(Task task);

        void doLast(Task task, long time, TimeUnit timeUnit);
    }

    public interface TaskInterceptor {

        void onIntercept(TaskInterceptorChain interceptorChain);
    }

    public interface TaskInterceptorChain {
        void cancel();

        void proceed();
    }
}
