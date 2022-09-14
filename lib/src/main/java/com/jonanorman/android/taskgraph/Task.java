package com.jonanorman.android.taskgraph;

import android.text.TextUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Task implements Runnable, Cloneable {

    private Runnable taskRunnable;
    private Set<TaskCallback> callbackSet;
    private boolean mainThread;
    private boolean onlyMainProcess;
    private String name;
    private Set<Object> dependsOnSet;


    Task(Builder builder) {
        this.name = builder.name;
        this.taskRunnable = builder.taskRunnable;
        this.mainThread = builder.mainThread;
        this.onlyMainProcess = builder.onlyMainProcess;
        this.callbackSet = new HashSet<>();
        this.callbackSet.addAll(builder.callbackSet);
        this.dependsOnSet = new HashSet<>();
        for (Object dependsOn : builder.dependsOnSet) {
            dependsOnSet.add(dependsOn);
        }
    }

    public final void run() {
        try {
            for (TaskCallback taskCallback : callbackSet) {
                taskCallback.doFirst(this);
            }
            taskRunnable.run();
        } finally {
            for (TaskCallback taskCallback : callbackSet) {
                taskCallback.doLast(this);
            }
        }

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(taskRunnable, task.taskRunnable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskRunnable);
    }

    @Override
    public String toString() {
        return TextUtils.isEmpty(name) ? taskRunnable.getClass().getName() : name;
    }


    @Override
    protected Object clone() throws CloneNotSupportedException {
        Task clone = (Task) super.clone();
        clone.taskRunnable = taskRunnable;
        clone.dependsOnSet = new HashSet<>();
        for (Object depend : dependsOnSet) {
            clone.dependsOnSet.add(depend);
        }
        clone.name = name;
        clone.mainThread = mainThread;
        clone.onlyMainProcess = onlyMainProcess;
        clone.callbackSet = new HashSet<>();
        for (TaskCallback taskCallback : callbackSet) {
            clone.callbackSet.add(taskCallback);
        }
        return clone;
    }

    void addTaskCallback(TaskCallback taskcallback) {
        callbackSet.add(taskcallback);
    }


    public boolean isOnlyMainProcess() {
        return onlyMainProcess;
    }

    public boolean isMainThread() {
        return mainThread;
    }

    public String getName() {
        return name;
    }

    public Runnable getTaskRunnable() {
        return taskRunnable;
    }

    public Set<Object> getDependsOnSet() {
        return dependsOnSet;
    }

    public interface TaskCallback {

        void doFirst(Task task);

        void doLast(Task task);
    }

    public static class Builder implements Cloneable {
        private boolean mainThread;
        private boolean onlyMainProcess;
        private Runnable taskRunnable;
        private Set<TaskCallback> callbackSet;
        private Set<Object> dependsOnSet;
        private String name;

        public Builder() {
            this(null, null, false, true);
        }

        public Builder(String name) {
            this(name, null, false, true);
        }

        public Builder(Runnable runnable) {
            this(runnable, false);
        }

        public Builder(Runnable runnable, boolean mainThread) {
            this(null, runnable, mainThread, true);
        }

        public Builder(String name,Runnable runnable) {
            this(null, runnable, false, true);
        }


        public Builder(String name, Runnable runnable, boolean mainThread, boolean onlyMainProcess) {
            this.name = name;
            this.taskRunnable = runnable;
            this.mainThread = mainThread;
            this.onlyMainProcess = onlyMainProcess;
            callbackSet = new HashSet<>();
            dependsOnSet = new HashSet<>();
        }

        public boolean isMainThread() {
            return mainThread;
        }

        public Builder setMainThread(boolean mainThread) {
            this.mainThread = mainThread;
            return this;
        }

        public boolean isOnlyMainProcess() {
            return onlyMainProcess;
        }

        public Builder setOnlyMainProcess(boolean onlyMainProcess) {
            this.onlyMainProcess = onlyMainProcess;
            return this;
        }

        public void setRunnable(Runnable taskRunnable) {
            this.taskRunnable = taskRunnable;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addTaskCallback(TaskCallback taskcallback) {
            callbackSet.add(taskcallback);
            return this;
        }

        public Builder removeTaskCallback(TaskCallback taskcallback) {
            callbackSet.add(taskcallback);
            return this;
        }

        public Builder clearTaskCallback() {
            callbackSet.clear();
            return this;
        }

        public Builder dependsOn(String name) {
            dependsOnSet.add(name);
            return this;
        }

        public Builder dependsOn(Builder builder) {
            dependsOnSet.add(builder.taskRunnable);
            return this;
        }

        public Builder dependsOn(Runnable taskRunnable) {
            dependsOnSet.add(taskRunnable);
            return this;
        }


        public Builder dependsOn(String... name) {
            dependsOnSet.add(name);
            return this;
        }

        public Builder dependsOn(Builder... builder) {
            for (Builder b : builder) {
                dependsOnSet.add(b.taskRunnable);
            }
            return this;
        }

        public Builder dependsOn(Runnable... taskRunnable) {
            dependsOnSet.add(taskRunnable);
            return this;
        }


        public Builder clearDependsOn() {
            dependsOnSet.clear();
            return this;
        }


        @Override
        public Builder clone() throws CloneNotSupportedException {
            Builder clone = (Builder) super.clone();
            clone.taskRunnable = taskRunnable;
            clone.dependsOnSet = new HashSet<>();
            for (Object object : dependsOnSet) {
                clone.dependsOnSet.add(object);
            }
            clone.mainThread = mainThread;
            clone.onlyMainProcess = onlyMainProcess;
            clone.callbackSet = new HashSet<>();
            for (TaskCallback taskCallback : callbackSet) {
                clone.callbackSet.add(taskCallback);
            }
            return clone;
        }

        Task build() {
            if (taskRunnable == null) {
                throw new NullPointerException("taskRunnable is null");
            }
            return new Task(this);
        }
    }

}
