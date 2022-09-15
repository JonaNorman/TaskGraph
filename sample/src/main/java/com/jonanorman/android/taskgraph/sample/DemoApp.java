package com.jonanorman.android.taskgraph.sample;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.jonanorman.android.taskgraph.Task;
import com.jonanorman.android.taskgraph.TaskCancelException;
import com.jonanorman.android.taskgraph.TaskGraph;
import com.jonanorman.android.taskgraph.TaskGraphExecutor;
import com.jonanorman.android.taskgraph.TaskGraphModule;

import java.util.concurrent.TimeUnit;


public class DemoApp extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        TaskGraphModule.initApplication(this);
        TaskGraph taskGraph = new TaskGraph();
        taskGraph.addTask(new Task("A", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));

        taskGraph.addTask(new Task("B", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));

        taskGraph.addTask(new Task("C", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).dependsOn("D", "B"));

        taskGraph.addTask(new Task("D", new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(520);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).dependsOn("A").addTaskInterceptor(new Task.TaskInterceptor() {

            @Override
            public void onIntercept(Task.TaskInterceptorChain interceptorChain) {
                TaskGraphModule.getRecentActivity(new TaskGraphModule.RecentActivityListener() {
                    @Override
                    public void onRecentActivity(Activity activity) {
                        showDialog("D Task", activity, interceptorChain);
                    }
                });
            }
        }));

        taskGraph.addTask(new Task("E", new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).dependsOn("F"));

        taskGraph.addTask(new Task("F", new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).dependsOn("C").setMainThread(true));

        taskGraph.addTask(new Task("G", new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).dependsOn("C"));

        taskGraph.setFirstTask(new Task("first", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).addTaskInterceptor(new Task.TaskInterceptor() {

            @Override
            public void onIntercept(Task.TaskInterceptorChain interceptorChain) {
                TaskGraphModule.getRecentActivity(new TaskGraphModule.RecentActivityListener() {
                    @Override
                    public void onRecentActivity(Activity activity) {
                        showDialog("first Task", activity, interceptorChain);
                    }
                });
            }
        }));
        taskGraph.setLastTask(new Task("last", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
        taskGraph.addTaskGraphListener(new TaskGraph.TaskGraphListener() {
            @Override
            public void onTaskGraphStart(TaskGraph taskGraph) {

            }

            @Override
            public void onTaskGraphEnd(TaskGraph taskGraph, long time, TimeUnit timeUnit) {
                TaskGraphModule.runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TaskGraphModule.getApplication(), "finished all task, cost time "+timeUnit.toMillis(time)+" ms", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onTaskGraphCancel(TaskGraph taskGraph, TaskCancelException cancelException) {
                TaskGraphModule.runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TaskGraphModule.getApplication(), "taskGraph canceled by " + cancelException.getTask().getName() + " task", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        taskGraph.execute();
    }


    private void showDialog(String title, Activity activity, Task.TaskInterceptorChain interceptorChain) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interceptorChain.proceed();
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interceptorChain.cancel();
            }
        });
        builder.setTitle(title);
        builder.setMessage("Please click ok  to continue");
        builder.setCancelable(false);
        builder.show();
    }
}
