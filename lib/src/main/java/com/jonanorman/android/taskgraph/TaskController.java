package com.jonanorman.android.taskgraph;

import android.os.Trace;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class TaskController implements Task.TaskInterceptorChain {

    final Task task;
    final Set<Task.TaskListener> listenerSet;
    final Queue<Task.TaskInterceptor> taskInterceptorQueue;
    final boolean mainThread;
    final boolean onlyMainProcess;
    final String name;
    final Set<Object> dependsOnSet;
    private final TaskGraphController graphController;
    private final Object sync;
    private boolean cancel;
    private boolean interrupted;
    private long startTime;
    private long costTime;
    private TaskControllerEndListener controllerEndListener;
    private Task.TaskInterceptor currentInterceptor;
    private Task.TaskInterceptor proceedInterceptor;


    TaskController(Task task, TaskGraphController graphController) {
        this.task = task;
        this.graphController = graphController;
        this.sync = new Object();
        this.name = task.name;
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

    final void run() {
        nextIntercept();
    }


    private void nextIntercept() {
        synchronized (sync) {
            currentInterceptor = taskInterceptorQueue.poll();
            if (currentInterceptor == null) {
                return;
            }
            TaskGraphModule.logVerbose(currentInterceptor +" interrupting");
            currentInterceptor.onIntercept(this);
        }
        while (!graphController.isFinished()) {
            synchronized (sync) {
                if (cancel) {
                    throw new TaskCancelException(interrupted ?
                            name + " canceled" + ", because thread interrupted" :
                            name + " canceled" + ", because " + currentInterceptor + " cancel",
                            task, interrupted);
                } else if (currentInterceptor == proceedInterceptor) {
                    TaskGraphModule.logVerbose(currentInterceptor +" interrupt proceed");
                    proceedInterceptor = null;
                    nextIntercept();
                    break;
                } else {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        cancel = true;
                        interrupted = true;
                    }
                }
            }
        }
    }

    private String getCurrentThreadMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        if (TaskGraphModule.isMainProcess()) {
            stringBuilder.append("主进程");
        } else {
            stringBuilder.append("进程名:" + TaskGraphModule.getProcessName());
        }
        stringBuilder.append(" 线程:" + Thread.currentThread().getName() + " ");
        return stringBuilder.toString();
    }


    @Override
    public String toString() {
        return  name;
    }

    public void cancel() {
        synchronized (sync) {
            cancel = true;
            sync.notifyAll();
        }
    }

    public void proceed() {
        synchronized (sync) {
            proceedInterceptor = currentInterceptor;
            sync.notifyAll();
        }
    }

    class RealRunTaskInterceptor implements Task.TaskInterceptor {

        @Override
        public void onIntercept(Task.TaskInterceptorChain interceptorChain) {
            logStart();
            for (Task.TaskListener taskCallback : listenerSet) {
                taskCallback.doFirst(task);
            }
            task.run();
            logEnd();
            for (Task.TaskListener taskCallback : listenerSet) {
                taskCallback.doLast(task,costTime, TimeUnit.MILLISECONDS);
            }
            if (controllerEndListener != null) {
                controllerEndListener.onTaskControllerEnd(TaskController.this);
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
        startTime = System.currentTimeMillis();
        if (TaskGraphModule.isEnableTrace()) {
            Trace.beginSection(name);
        }
        TaskGraphModule.logVerbose(getCurrentThreadMessage() + "task:" + name + " start");
    }

    private void logEnd() {
        if (TaskGraphModule.isEnableTrace()) {
            Trace.endSection();
        }
        costTime = System.currentTimeMillis() - startTime;
        TaskGraphModule.logDebug(getCurrentThreadMessage() + "task:" + name + " end " + costTime + "ms");
    }

    void setControllerEndListener(TaskControllerEndListener controllerEndListener) {
        this.controllerEndListener = controllerEndListener;
    }

    interface TaskControllerEndListener {
        void onTaskControllerEnd(TaskController controller);
    }

}
