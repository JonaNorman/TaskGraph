package com.jonanorman.android.taskgraph;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskGraphExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(CPU_COUNT * 2, 6);
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
        new TaskGraphRecord(taskGraph).execute();

    }

    public void execute(Task task) {
        TaskGraph taskGraph = new TaskGraph();
        taskGraph.setMainThread(task.mainThread);
        execute(taskGraph.addTask(task));
    }


    public class TaskGraphRecord implements Runnable {

        private final TaskGraphController taskGraphController;
        private final Set<DirectedGraph.Vertex> runningTaskSet;
        private final Object sync = new Object();
        private DirectedGraph directGraph;
        private final TaskGraph taskGraph;
        private Set<DirectedGraph.Vertex> vertexSet;


        private final Comparator<DirectedGraph.Vertex> taskComparator = new Comparator<DirectedGraph.Vertex>() {
            @Override
            public int compare(DirectedGraph.Vertex o1, DirectedGraph.Vertex o2) {
                TaskController taskController1 = (TaskController) o1.getValue();
                TaskController taskController2 = (TaskController) o2.getValue();
                if (taskController1.priority > taskController2.priority) {
                    return -1;
                } else if (taskController1.priority < taskController2.priority) {
                    return 1;
                }
                String taskName1 = taskController1.name;
                String taskName2 = taskController2.name;
                int nameCompare = taskName1.compareTo(taskName2);
                if (nameCompare != 0) {
                    return nameCompare;
                }
                return taskController1.hashCode() - taskController2.hashCode();
            }
        };


        public TaskGraphRecord(TaskGraph taskGraph) {
            runningTaskSet = new HashSet<>();
            this.taskGraph = taskGraph;
            taskGraphController = new TaskGraphController(taskGraph);
        }


        public void execute() {
            if (taskGraphController.isMainThread()) {
                TaskGraphModule.runInMainThread(this);
            } else {
                threadPoolExecutor.execute(this);
            }
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
                runPendingTask();
            }
        }


        private Set<DirectedGraph.Vertex> findPendingTask() {
            Set<DirectedGraph.Vertex> pendingTaskSet = new TreeSet<>(taskComparator);
            for (DirectedGraph.Vertex<TaskController> vertex : vertexSet) {
                if (directGraph.getInDegree(vertex) == 0) {
                    if (runningTaskSet.add(vertex)) {
                        pendingTaskSet.add(vertex);
                    }
                }
            }
            return pendingTaskSet;
        }

        private void runPendingTask() {
            Set<DirectedGraph.Vertex> pendingTaskSet = findPendingTask();
            Iterator<DirectedGraph.Vertex> iterator = pendingTaskSet.iterator();
            while (iterator.hasNext()) {
                if (taskGraphController.isFinished()) {
                    return;
                }
                DirectedGraph.Vertex<TaskController> vertex = iterator.next();
                TaskController taskController = vertex.getValue();
                taskController.setControllerListener(getNextTaskControllerListener(vertex));
                if (taskController.mainThread) {
                    TaskGraphModule.runInMainThread(taskController);
                } else {
                    threadPoolExecutor.execute(taskController);
                }
            }
        }

        private TaskController.TaskControllerListener getNextTaskControllerListener(DirectedGraph.Vertex<TaskController> vertex) {
            TaskController.TaskControllerListener endListener = new TaskController.TaskControllerListener() {
                long startTime;
                long costTime;

                @Override
                public void onTaskControllerFist(TaskController taskController) {
                    startTime = System.currentTimeMillis();
                    Task task = taskController.task;
                    taskGraphController.logTaskStart(task);

                }

                @Override
                public void onTaskControllerLast(TaskController taskController) {
                    costTime = System.currentTimeMillis() - startTime;
                    Task task = taskController.task;
                    taskGraphController.logTaskLast(task, costTime, TimeUnit.MILLISECONDS);
                    nextVertex(vertex);
                }

                @Override
                public void onTaskControllerCancel(TaskCancelException taskCancelException) {
                    taskGraphController.runCancel(taskCancelException);
                }
            };
            return endListener;
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
