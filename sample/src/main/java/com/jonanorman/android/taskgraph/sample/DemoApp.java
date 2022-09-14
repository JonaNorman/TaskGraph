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
        TaskGraph.Builder graphBuilder = new TaskGraph.Builder();
        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("A"));

        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("B"));

        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("C").dependsOn("D").dependsOn("B"));

        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(520);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("D").dependsOn("A"));

        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("E").dependsOn("F"));

        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("F").dependsOn("C"));

        graphBuilder.addTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("G").dependsOn("C"));

        graphBuilder.setFirstTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, true).setName("first"));

        graphBuilder.setLastTask(new Task.Builder(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).setName("last"));
        graphBuilder.execute();
    }
}
