package com.jonanorman.android.taskgraph;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskGraphModule {

    private static final String TAG = "TASK_GRAPH_LOG";

    private static final Object CONTEXT_SYNC = new Object();
    private static final Object MAIN_PROCESS_SYNC = new Object();
    private static final Object PROCESS_NAME_SYNC = new Object();
    private static final Object PACKAGE_NAME_SYNC = new Object();
    private static final Object LOG_FUNCTION_SYNC = new Object();
    private static final Object ENABLE_TRACE_SYNC = new Object();
    private static final Object LOG_GRAPH_VIZ_SYNC = new Object();

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ConcurrentLinkedDeque<Activity> ACTIVITY_DEQUE = new ConcurrentLinkedDeque<Activity>();
    private static final List<TopActivityListener> TOP_ACTIVITY_LISTENER_LIST = new CopyOnWriteArrayList<>();


    private static final Application.ActivityLifecycleCallbacks TOP_ACTIVITY_LIFECYCLE_CALLBACKS = new Application.ActivityLifecycleCallbacks() {
        private Activity topActivity;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            ACTIVITY_DEQUE.add(activity);
            Activity currentActivity = getTopActivity();
            if (currentActivity != topActivity) {
                topActivity = currentActivity;
                for (TopActivityListener topActivityListener : TOP_ACTIVITY_LISTENER_LIST) {
                    topActivityListener.onTopActivityShow(topActivity);
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            ACTIVITY_DEQUE.remove(activity);
            Activity currentActivity = getTopActivity();
            if (currentActivity != topActivity) {
                topActivity = currentActivity;
                for (TopActivityListener topActivityListener : TOP_ACTIVITY_LISTENER_LIST) {
                    topActivityListener.onTopActivityShow(topActivity);
                }
            }
        }
    };


    private static String PROCESS_NAME;
    private static Boolean MAIN_PROCESS;
    private static String PACKAGE_NAME;
    private static Application APP_CONTEXT;
    private static boolean APP_INIT = false;
    private static boolean ENABLE_TRACE = true;
    private static boolean LOG_GRAPH_VIZ = true;


    private static final LogFunction DEFAULT_LOG_FUNCTION = new LogFunction(TAG) {

        @Override
        void verbose(String message) {
            Log.v(getTag(), message);
        }

        @Override
        void debug(String message) {
            Log.d(getTag(), message);
        }

        @Override
        void info(String message) {
            Log.i(getTag(), message);
        }

        @Override
        void warn(String message) {
            Log.w(getTag(), message);
        }

        @Override
        void error(String message) {
            Log.e(getTag(), message);
        }
    };


    private static LogFunction LOG_FUNCTION = DEFAULT_LOG_FUNCTION;


    public static boolean isMainProcess() {
        synchronized (MAIN_PROCESS_SYNC) {
            if (MAIN_PROCESS != null) {
                return MAIN_PROCESS;
            }
            MAIN_PROCESS = Objects.equals(getProcessName(), getPackageName());
            return MAIN_PROCESS;
        }
    }

    public static String getProcessName() {
        synchronized (PROCESS_NAME_SYNC) {
            if (PROCESS_NAME != null) {
                return PROCESS_NAME;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                PROCESS_NAME = Application.getProcessName();
            if (PROCESS_NAME == null) PROCESS_NAME = getProcessNameByProc();
            if (PROCESS_NAME == null) PROCESS_NAME = getProcessNameByActivityThread();
            if (PROCESS_NAME == null) PROCESS_NAME = getProcessNameByActivityManager();
            return PROCESS_NAME;
        }
    }

    public static Application getApplication() {
        synchronized (CONTEXT_SYNC) {
            if (APP_CONTEXT != null) {
                return APP_CONTEXT;
            }
            if (APP_CONTEXT == null) {
                APP_CONTEXT = getContextByActivityThread();
            }
            if (APP_CONTEXT == null) {
                throw new NullPointerException("please first initApplicationContext");
            }
        }
        return APP_CONTEXT;
    }

    public static void initApplication(Application application) {
        if (application == null && APP_CONTEXT != null) {
            return;
        }
        synchronized (CONTEXT_SYNC) {
            if (APP_CONTEXT == null) {
                APP_CONTEXT = application;
                APP_CONTEXT.registerActivityLifecycleCallbacks(TOP_ACTIVITY_LIFECYCLE_CALLBACKS);
                APP_INIT = true;
            }
        }
    }

    public static Activity getTopActivity() {
        synchronized (CONTEXT_SYNC) {
            if (!APP_INIT) {
                throw new NullPointerException("please first initApplicationContext");
            }
        }
        return ACTIVITY_DEQUE.getLast();
    }

    public static void runInMainThread(Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            MAIN_HANDLER.post(runnable);
        }
    }

    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }


    public static void setLogFunction(LogFunction logFunction) {
        synchronized (LOG_FUNCTION_SYNC) {
            LOG_FUNCTION = logFunction;
        }
    }

    public static void setEnableTrace(boolean enableTrace) {
        synchronized (ENABLE_TRACE_SYNC) {
            ENABLE_TRACE = enableTrace;
        }
    }

    public static boolean isEnableTrace() {
        synchronized (ENABLE_TRACE_SYNC) {
            return ENABLE_TRACE;
        }
    }

    public static String getPackageName() {
        synchronized (PACKAGE_NAME_SYNC) {
            if (PACKAGE_NAME != null) {
                return PACKAGE_NAME;
            }
            PACKAGE_NAME = getApplication().getPackageName();
            return PACKAGE_NAME;
        }
    }

    public static void setLogGraphViz(boolean logGraphViz) {
        synchronized (LOG_GRAPH_VIZ_SYNC) {
            LOG_GRAPH_VIZ = logGraphViz;
        }

    }

    public static boolean isLogGraphViz() {
        synchronized (LOG_GRAPH_VIZ_SYNC) {
            return LOG_GRAPH_VIZ;
        }
    }

    public static void addTopActivityListener(TopActivityListener topActivityListener) {
        if (!TOP_ACTIVITY_LISTENER_LIST.contains(topActivityListener)) {
            TOP_ACTIVITY_LISTENER_LIST.add(topActivityListener);
        }
    }

    public static void removeTopActivityListener(TopActivityListener topActivityListener) {
        if (TOP_ACTIVITY_LISTENER_LIST.contains(topActivityListener)) {
            TOP_ACTIVITY_LISTENER_LIST.remove(topActivityListener);
        }
    }

    public static void getRecentActivity(RecentActivityListener recentActivityListener) {
        AtomicBoolean status = new AtomicBoolean();
        TaskGraphModule.addTopActivityListener(new TaskGraphModule.TopActivityListener() {
            @Override
            public void onTopActivityShow(Activity activity) {
                if (!status.getAndSet(true)) {
                    recentActivityListener.onRecentActivity(activity);
                }
            }
        });

        TaskGraphModule.runInMainThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = TaskGraphModule.getTopActivity();
                if (activity != null) {
                    if (!status.getAndSet(true)) {
                        recentActivityListener.onRecentActivity(activity);
                    }
                }
            }
        });
    }

    private static String getProcessNameByProc() {
        BufferedReader bufferedReader = null;
        try {
            File file = new File("/proc/" + Process.myPid() + "/" + "cmdline");
            bufferedReader = new BufferedReader(new FileReader(file));
            String processName = bufferedReader.readLine().trim();
            return processName;
        } catch (Exception e) {
            logThrowable(e);
            return null;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public static void logVerbose(String message) {
        LOG_FUNCTION.verbose(message);
    }

    public static void logDebug(String message) {
        LOG_FUNCTION.debug(message);
    }

    public static void logInfo(String message) {
        LOG_FUNCTION.info(message);
    }

    public static void logWarn(String message) {
        LOG_FUNCTION.warn(message);
    }

    public static void logError(String message) {
        LOG_FUNCTION.error(message);
    }

    public static void logThrowable(Throwable throwable) {
        if (throwable == null) return;
        logWarn(throwable.getMessage() + "\n" + Log.getStackTraceString(throwable));
    }


    private static String getProcessNameByActivityManager() {
        try {
            ActivityManager am = (ActivityManager) getApplication().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
            if (runningApps == null) {
                return null;
            }
            for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
                if (procInfo.pid == Process.myPid()) {
                    return procInfo.processName;
                }
            }
            return null;
        } catch (Exception e) {
            logThrowable(e);
            return null;
        }
    }

    private static String getProcessNameByActivityThread() {
        try {
            Object activityThread = getActivityThreadInActivityThreadStaticField();
            if (activityThread == null)
                activityThread = getActivityThreadInActivityThreadStaticMethod();
            if (activityThread == null) activityThread = getActivityThreadInLoadedApkField();
            Method method = activityThread.getClass().getMethod("currentProcessName");
            method.setAccessible(true);
            String value = (String) method.invoke(activityThread);
            return value;
        } catch (Exception e) {
            logThrowable(e);
        }
        return null;
    }


    private static Application getContextByActivityThread() {
        try {
            Object activityThread = getActivityThreadInActivityThreadStaticField();
            if (activityThread == null)
                activityThread = getActivityThreadInActivityThreadStaticMethod();
            if (activityThread == null) activityThread = getActivityThreadInLoadedApkField();
            Field declaredField = activityThread.getClass().getDeclaredField("mInitialApplication");
            declaredField.setAccessible(true);
            Object object = declaredField.get(activityThread);
            if (object instanceof Application) {
                return ((Application) object);
            }
        } catch (Throwable e) {
            logThrowable(e);
        }
        return null;
    }


    private static Object getActivityThreadInActivityThreadStaticField() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Field activityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            activityThreadField.setAccessible(true);
            return activityThreadField.get(null);
        } catch (Exception e) {
            logThrowable(e);
            return null;
        }
    }

    private static Object getActivityThreadInActivityThreadStaticMethod() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            return activityThreadClass.getMethod("currentActivityThread").invoke(null);
        } catch (Exception e) {
            logThrowable(e);
            return null;
        }
    }

    private static Object getActivityThreadInLoadedApkField() {
        try {
            Field loadedApkField = Application.class.getDeclaredField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(getApplication());
            Field activityThreadField = loadedApk.getClass().getDeclaredField("mActivityThread");
            activityThreadField.setAccessible(true);
            return activityThreadField.get(loadedApk);
        } catch (Exception e) {
            logThrowable(e);
            return null;
        }
    }


    public static abstract class LogFunction {

        final String tag;

        public LogFunction(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        abstract void verbose(String message);

        abstract void debug(String message);

        abstract void info(String message);

        abstract void warn(String message);

        abstract void error(String message);
    }

    public interface TopActivityListener {
        void onTopActivityShow(Activity activity);
    }

    public interface RecentActivityListener {
        void onRecentActivity(Activity activity);
    }

}
