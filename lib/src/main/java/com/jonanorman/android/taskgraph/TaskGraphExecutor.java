package com.jonanorman.android.taskgraph;

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


    private final ThreadPoolExecutor threadPoolExecutor;


    public TaskGraphExecutor() {
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


    public void execute(TaskGraph taskGraph) {
        threadPoolExecutor.execute(new TaskGraphRecord(taskGraph));
    }


    public class TaskGraphRecord implements Runnable {

        private final TaskGraphController taskGraphController;
        private final Set<DirectedGraph.Vertex> pendingTaskSet;
        private final Set<DirectedGraph.Vertex> runningTaskSet;
        private final Object sync = new Object();
        private DirectedGraph directGraph;
        private Set<DirectedGraph.Vertex> vertexSet;


        public TaskGraphRecord(TaskGraph taskGraph) {
            pendingTaskSet = new HashSet<>();
            runningTaskSet = new HashSet<>();
            taskGraphController = new TaskGraphController(taskGraph);
        }


        public void run() {
            initTaskGraph();
            runStart();
            runNext();
        }


        private void initTaskGraph() {
            directGraph = taskGraphController.getDirectedGraph();
            vertexSet = directGraph.getVertexSet();
            if (directGraph.hasCycle()) {
                throw new IllegalStateException("graph has cycle\n " + directGraph.getGraphPic());
            }
        }

        private void runStart() {
            taskGraphController.runStart();
        }

        private void runEnd() {
            taskGraphController.runEnd();
        }

        private void runNext() {
            synchronized (sync) {
                if (vertexSet.size() == 0) {
                    runEnd();
                    return;
                }
                findPendingTask();
                runPendingTask();
            }
        }


        private void findPendingTask() {
            for (DirectedGraph.Vertex<TaskController> vertex : vertexSet) {
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
                if (taskGraphController.isFinished()) {
                    return;
                }
                DirectedGraph.Vertex<TaskController> vertex = iterator.next();
                TaskController taskController = vertex.getValue();
                taskController.setControllerEndListener(getNextTaskControllerListener(vertex));
                Runnable taskRunnable = getTaskRunnable(taskController);
                if (taskController.mainThread) {
                    TaskGraphModule.runInMainThread(taskRunnable);
                } else {
                    threadPoolExecutor.execute(taskRunnable);
                }
                iterator.remove();
            }
        }

        private TaskController.TaskControllerEndListener getNextTaskControllerListener(DirectedGraph.Vertex<TaskController> vertex) {
            TaskController.TaskControllerEndListener endListener = new TaskController.TaskControllerEndListener() {
                @Override
                public void onTaskControllerEnd(TaskController controller) {
                    nextVertex(vertex);
                }
            };
            return endListener;
        }

        private Runnable getTaskRunnable(TaskController taskController) {
            Runnable taskRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        taskController.run();
                    } catch (TaskCancelException e) {
                        TaskGraphModule.logWarn(e.getMessage());
                        taskGraphController.runCancel(e);
                    }
                }
            };
            return taskRunnable;
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
