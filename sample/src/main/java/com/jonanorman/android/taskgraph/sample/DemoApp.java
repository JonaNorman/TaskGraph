package com.jonanorman.android.taskgraph.sample;

import android.app.Application;

import com.jonanorman.android.taskgraph.Task;
import com.jonanorman.android.taskgraph.TaskGraph;
import com.jonanorman.android.taskgraph.TaskGraphModule;


public class DemoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TaskGraphModule.initApplication(this);
        TaskGraph.Builder taskGraphBuilder = new TaskGraph.Builder();
        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("A"));

        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("B"));

        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("C").dependsOn("D").dependsOn("B"));

        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(520);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("D").dependsOn("A"));

        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("E").dependsOn("F"));

        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("F").dependsOn("C"));

        taskGraphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("G").dependsOn("C"));

        taskGraphBuilder.setFirstTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, true).setName("first"));

        taskGraphBuilder.setLastTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("last"));
        taskGraphBuilder.execute();
    }
}
