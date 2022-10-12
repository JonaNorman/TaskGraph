package com.jonanorman.android.taskgraph;

public class TaskCancelException extends RuntimeException {

    private final boolean interrupted;
    private final Task task;

    public TaskCancelException(String message, Task task, boolean interrupted) {
        super(message);
        this.task = task;
        this.interrupted = interrupted;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public Task getTask() {
        return task;
    }
}
