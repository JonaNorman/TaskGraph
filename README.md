# TaskGraph

简体中文 | [English](./README_EN.md)

![TaskGraph](https://github.com/JonaNorman/TaskGraph/blob/main/screen/logo.png?raw=true)

这是一个面向Android开发的依赖任务启动框架，致力于帮助广大Android开发者降低开发成本，它可以使用在启动优化中。

## 引用

[![Maven Central](http://maven-badges.herokuapp.com/maven-central/io.github.jonanorman.android/taskgraph/badge.svg
)](https://search.maven.org/artifact/io.github.jonanorman.android/taskgraph)

```
implementation('io.github.jonanorman.android:taskgraph:0.1.0')
```

## 使用
### 初始化

```Java
TaskGraphModule.initApplication(appliction);
```

### 构建

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


### 日志

默认的日志Tag是TASK_GRAPH_LOG，搜索TASK_GRAPH_LOG: graphviz:会输出任务的graphviz的有向图日志，复制到 [graphviz-visual-editor](http://magjac.com/graphviz-visual-editor/) 可视化查看

![img.png](screen/img.png)

### 运行跟踪

```shell
python systrace.py -o trace.html  -a packagename sched
```
packagename要替换成运行的app的包名
chrome浏览器打开chrome://tracing/,load 按钮加载trace.html
![img.png](screen/img2.png)


## 开源许可证

查看许可证 [LICENSE](./LICENSE).
