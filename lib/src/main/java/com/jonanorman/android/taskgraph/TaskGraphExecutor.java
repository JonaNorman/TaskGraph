package com.jonanorman.android.taskgraph;

import android.os.Handler;
import android.os.Looper;
import android.os.Trace;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskGraphExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(CPU_COUNT * 2, 4);
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2;
    private static final int KEEP_ALIVE_SECONDS = 30;
    private static final Object SYNC = new Object();
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger threadCount = new AtomicInteger(1);

        public Thread newThread(final Runnable r) {
            return new Thread(r, "TaskGraphThread#" + threadCount.getAndIncrement());
        }
    };
    private static volatile TaskGraphExecutor DEFAULT;


    public static TaskGraphExecutor getDefault() {
        if (DEFAULT != null) {
            return DEFAULT;
        }
        synchronized (SYNC) {
            if (DEFAULT == null) {
                DEFAULT = new TaskGraphExecutor();
            }
        }
        return DEFAULT;
    }


    private final Handler mainThreadHandler;

    private final ThreadPoolExecutor threadPoolExecutor;


    public TaskGraphExecutor() {
        mainThreadHandler = new Handler();
        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), THREAD_FACTORY);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public void setCoreThreadPoolSize(int corePoolSize) {
        threadPoolExecutor.setCorePoolSize(corePoolSize);
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        threadPoolExecutor.setMaximumPoolSize(maximumPoolSize);
    }


    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }


    public void execute(TaskGraph.Builder builder) {
        threadPoolExecutor.execute(new TaskGraphRecord(builder));
    }


    public class TaskGraphRecord implements Runnable {

        private final TaskGraph taskGraph;
        private final Set<DirectedGraph.Vertex> pendingTaskSet;
        private final Set<DirectedGraph.Vertex> runningTaskSet;
        private final Object sync = new Object();
        private DirectedGraph directGraph;
        private Set<DirectedGraph.Vertex> vertexSet;
        private boolean taskEnd;
        private long startTime;


        public TaskGraphRecord(TaskGraph.Builder builder) {
            pendingTaskSet = new HashSet<>();
            runningTaskSet = new HashSet<>();
            taskGraph = builder.builder();
        }


        public void run() {
            logStart();
            initTaskGraph();
            runStart();
            runNext();
        }

        private void logStart() {
            startTime = System.currentTimeMillis();
            TaskGraphModule.logVerbose("taskGraph start...");
        }

        private void logEnd() {
            TaskGraphModule.logDebug("taskGraph end " + (System.currentTimeMillis() - startTime) + " ms");
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


        private void initTaskGraph() {
            directGraph = taskGraph.generateDirectedGraph();
            if (TaskGraphModule.isLogGraphViz()){
                TaskGraphModule.logInfo("graphviz:\n"+directGraph.getGraphPic());
            }
            vertexSet = directGraph.getVertexSet();
            TaskGraphModule.logDebug("taskGraph has " + vertexSet.size()+" task");
            if (directGraph.hasCycle()) {
                throw new IllegalStateException("graph has cycle\n " + directGraph.getGraphPic());
            }
        }

        private void runStart() {
            taskGraph.runStart();
        }

        private void runEnd() {
            taskGraph.runEnd();
        }

        private void runNext() {
            synchronized (sync) {
                if (vertexSet.size() == 0) {
                    if (taskEnd) {
                        return;
                    }
                    taskEnd = true;
                    runEnd();
                    logEnd();
                    return;
                }
                findPendingTask();
                runPendingTask();
            }
        }


        private void findPendingTask() {
            for (DirectedGraph.Vertex<Task> vertex : vertexSet) {
                if (directGraph.getInDegree(vertex) == 0) {
                    if (runningTaskSet.add(vertex)) {
                        pendingTaskSet.add(vertex);
                    }
                }
            }
        }

        private void runPendingTask() {
            Iterator<DirectedGraph.Vertex> iterator = pendingTaskSet.iterator();
            while (iterator.hasNext()) {
                final DirectedGraph.Vertex<Task> vertex = iterator.next();
                Task task = vertex.getValue();
                task.addTaskCallback(new Task.TaskCallback() {

                    long startTime;

                    @Override
                    public void doFirst(Task task) {
                        startTime = System.currentTimeMillis();
                        if (TaskGraphModule.isEnableTrace()) {
                            Trace.beginSection(task.getName());
                        }
                        TaskGraphModule.logVerbose(getCurrentThreadMessage() + "task:" + task.getName() + " start");
                    }

                    @Override
                    public void doLast(Task task) {
                        if (TaskGraphModule.isEnableTrace()) {
                            Trace.endSection();
                        }
                        TaskGraphModule.logDebug(getCurrentThreadMessage() + "task:" + task.getName() + " end " + (System.currentTimeMillis() - startTime) + "ms");
                        nextVertex(vertex);
                    }
                });
                if (task.isMainThread()) {
                    if (Looper.getMainLooper() == Looper.myLooper()) {
                        task.run();
                    } else {
                        mainThreadHandler.post(task);
                    }
                } else {
                    threadPoolExecutor.execute(task);
                }
                iterator.remove();
            }
        }

        private void nextVertex(DirectedGraph.Vertex vertex) {
            synchronized (sync) {
                runningTaskSet.remove(vertex);
                directGraph.removeVertex(vertex);
                runNext();
            }
        }

    }


}
