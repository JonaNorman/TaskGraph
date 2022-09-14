# TaskGraph

English | [简体中文](./README.md)

![TaskGraph](./screen/logo.png)

This is a dependency task startup framework for Android development, dedicated to helping Android developers reduce development costs, and it can be used in startup optimization.

## Import

[![Maven Central](http://maven-badges.herokuapp.com/maven-central/io.github.jonanorman.android/taskgraph/badge.svg
)](https://search.maven.org/artifact/io.github.jonanorman.android/taskgraph)

```
implementation('io.github.jonanorman.android:taskgraph:0.1.0')
```

## Use
### Init

```Java
TaskGraphModule.initApplication(appliction);
```

### Execute

```Java
TaskGraph.Builder graphBuilder = new TaskGraph.Builder();
graphBuilder.addTask(new Task.Builder("A",new Runnable() {
    @Override
    public void run() {
    }
}));
graphBuilder.addTask(new Task.Builder("B",new Runnable() {
    @Override
    public void run() {
    }
}).dependsOn("A").setMainThread(true));
TaskGraphExecutor.getDefault().execute(graphBuilder);
```


### Log

The default log tag is TASK_GRAPH_LOG, search for TASK_GRAPH_LOG: graphviz: will output the directed graph log of the task's graphviz, copy it to [graphviz-visual-editor](http://magjac.com/graphviz-visual-editor/) Visual view
![img.png](screen/img.png)

### trace

```shell
python systrace.py -o trace.html  -a packagename sched
```
packagename should be replaced with the package name of the running app
Chrome browser open chrome://tracing/, load button to load trace.html
![img.png](screen/img2.png)


## DOC
### Task.Builder
- **setOnlyMainProcess**

  Whether to run only in the main process, the default is true
- **setRunnable**

  The runnables of tasks in each TaskGraph cannot be the same, and the same will be overwritten
- **setName** 

  Set the task name, which can be passed or not. If not passed, the class of runnable is used as the name in the log.
- **addTaskCallback**

  set task callback
- **removeTaskCallback**

  remove task callback
- **clearTaskCallback**

  clear task callback
- **dependsOn**

  Dependent tasks, assuming that A needs to be executed after B is executed, then A.dependsOn(B), which can pass String, Builder, runnable and their arrays
- **clearDependsOn** 

   Clear dependencies

### TaskGraph.Builder

- **addTask** 

    Add a task, pass in the Task.Builder object
- **removeTask** 

    remove task
- **setFirstTask**

    Set the first task, which can be passed or not
- **setLastTask** 

  Set the last task, which can be passed or not
- **clearTask**

  clear task
- **addTaskGraphCallback** 

  Add task graph callback
- **removeTaskGraphCallback** 

  remove task graph callback
- **clearTaskGraphCallback**

  Clear task graph callback
- **execute** 

  Execute, use TaskGraphExecutor.getDefault() to execute, or new TaskGraphExecutor() to execute
### TaskGraphExecutor

- **getDefault** 

  default executor
- **setCoreThreadPoolSize** 

  Set the number of thread pool cores
- **setMaximumPoolSize** 

  Set the maximum number of thread pools
- **getThreadPoolExecutor** 

  get thread pool
- **execute**

  Pass in the TaskGraph.Builder object to execute

### TaskGraphModule

- **isMainProcess** 

  Is it the main process
- **getProcessName** 

  get process name
- **getApplication** 

  Get Application
- **initApplication** 

  Initialization, called in Application's onCreate
- **setLogFunction** 

  Initialization, called in Application's onCreate
- **setEnableTrace** 

  Whether to enable Systrace tracing, enabled by default
- **getPackageName** 

  get package name
- **setLogGraphViz** 

  Whether to enable the output of GraphViz directed graph logs, enabled by default


## License

[LICENSE](./LICENSE).
