package com.jonanorman.android.taskgraph;

import android.os.Trace;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class TaskController implements Task.TaskInterceptorChain, Runnable {

    final Task task;
    final Set<Task.TaskListener> listenerSet;
    final Queue<Task.TaskInterceptor> taskInterceptorQueue;
    final boolean mainThread;
    final boolean onlyMainProcess;
    final int priority;
    final String name;
    final Set<Object> dependsOnSet;
    private final TaskGraphController graphController;
    private final Object sync;
    private boolean canceled;
    private long runStartTime;
    private long runCostTime;
    private long interceptStartTime;
    private boolean interceptLogEnable;
    private TaskControllerListener controllerListener;
    private volatile Task.TaskInterceptor currentInterceptor;
    private boolean proceed;
    private boolean runOver;

    TaskController(Task task, TaskGraphController graphController) {
        this.task = task;
        this.graphController = graphController;
        this.sync = new Object();
        this.name = task.name;
        this.priority = task.priority;
        this.mainThread = task.mainThread;
        this.onlyMainProcess = task.onlyMainProcess;
        this.listenerSet = new HashSet<>();
        this.listenerSet.addAll(task.listenerSet);
        this.dependsOnSet = new HashSet<>();
        this.dependsOnSet.addAll(task.dependsSet);
        this.taskInterceptorQueue = new LinkedList<>();
        this.taskInterceptorQueue.addAll(task.taskInterceptorSet);
        this.taskInterceptorQueue.add(new RealRunTaskInterceptor());
    }

    public final void run() {
        interceptStartTime = System.currentTimeMillis();
        interceptLogEnable = taskInterceptorQueue.size() > 1;
        if (mainThread) {
            nextMainThreadIntercept();
        } else {
            nextAsyncThreadIntercept();
        }
    }

    private void nextMainThreadIntercept() {
        runNextIntercept();
    }


    private void nextAsyncThreadIntercept() {
        if (!runNextIntercept())
            return;
        while (true) {
            synchronized (sync) {
                if (isFinish()) {
                    return;
                }
                if (proceed) {
                    proceed = false;
                    nextAsyncThreadIntercept();
                    break;
                } else {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        canceled = true;
                        if (controllerListener != null) {
                            controllerListener.onTaskControllerCancel(new TaskCancelException(
                                    name + " canceled" + ", because thread interrupted",
                                    task, true));
                        }

                    }
                }
            }
        }
    }

    private boolean runNextIntercept() {
        Task.TaskInterceptor interceptor = taskInterceptorQueue.poll();
        synchronized (sync) {
            if (isFinish()) {
                return false;
            }
            if (interceptor == null) {
                runOver = true;
                return false;
            }
        }
        if (currentInterceptor != null &&!(currentInterceptor instanceof RealRunTaskInterceptor)) {
            TaskGraphModule.logVerbose(currentInterceptor + " interrupt proceed");
        }
        if (!(interceptor instanceof RealRunTaskInterceptor)) {
            TaskGraphModule.logVerbose(interceptor + " interrupting");
        }

        currentInterceptor = interceptor;
        interceptor.onIntercept(this);
        return true;
    }


    private boolean isFinish() {
        synchronized (sync) {
            if (runOver || canceled || graphController.isFinished()) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public void cancel() {
        synchronized (sync) {
            if (isFinish()) {
                return;
            }
            canceled = true;
            sync.notifyAll();
        }
        if (controllerListener != null) {
            controllerListener.onTaskControllerCancel(new TaskCancelException(
                    name + " canceled" + ", because " + currentInterceptor + " cancel",
                    task, false));
        }
    }

    public void proceed() {
        synchronized (sync) {
            if (isFinish() || proceed) {
                return;
            }
            proceed = true;
            sync.notifyAll();
        }
        if (mainThread) {
            TaskGraphModule.runInMainThread(new Runnable() {
                @Override
                public void run() {
                    nextMainThreadIntercept();
                }
            });
        }
    }

    class RealRunTaskInterceptor implements Task.TaskInterceptor {

        @Override
        public void onIntercept(Task.TaskInterceptorChain interceptorChain) {
            if (interceptLogEnable) {
                TaskGraphModule.logDebug(task.getName() + " intercept cost time " + (System.currentTimeMillis() - interceptStartTime));
            }
            logStart();
            if (controllerListener != null) {
                controllerListener.onTaskControllerFist(TaskController.this);
            }
            for (Task.TaskListener taskCallback : listenerSet) {
                taskCallback.doFirst(task);
            }
            task.run();
            for (Task.TaskListener taskCallback : listenerSet) {
                taskCallback.doLast(task, runCostTime, TimeUnit.MILLISECONDS);
            }
            logEnd();
            if (controllerListener != null) {
                controllerListener.onTaskControllerLast(TaskController.this);
            }
            synchronized (sync) {
                runOver = true;
            }
            interceptorChain.proceed();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskController)) return false;
        TaskController that = (TaskController) o;
        return task != that.task;
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, "TaskController");
    }

    private void logStart() {
        runStartTime = System.currentTimeMillis();
        if (TaskGraphModule.isEnableTrace()) {
            Trace.beginSection(name);
        }
        TaskGraphModule.logVerbose("task:" + name + " start");
    }

    private void logEnd() {
        if (TaskGraphModule.isEnableTrace()) {
            Trace.endSection();
        }
        runCostTime = System.currentTimeMillis() - runStartTime;
        TaskGraphModule.logDebug("task:" + name + " end " + runCostTime + "ms");
    }

    void setControllerListener(TaskControllerListener controllerListener) {
        this.controllerListener = controllerListener;
    }

    interface TaskControllerListener {

        void onTaskControllerFist(TaskController taskController);

        void onTaskControllerLast(TaskController taskController);

        void onTaskControllerCancel(TaskCancelException taskCancelException);

    }

}
