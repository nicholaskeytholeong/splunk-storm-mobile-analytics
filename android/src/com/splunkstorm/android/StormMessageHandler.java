package com.splunkstorm.android;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Log;

/**
 * @author Nicholas Key (Splunk)
 */
public class StormMessageHandler implements UncaughtExceptionHandler{

    private UncaughtExceptionHandler defaultExceptionHandler;
    private String projectId;
    private String accessToken;
    private JSONObject stackTraceJson = new JSONObject();
    private static final String TAG = "STORM_UNCAUGHT_EXCEPTION_HANLDER"; 

    //constructor for StormMessageHandler
    public StormMessageHandler(String projectId, String accessToken) {
        this.projectId = projectId;
        this.accessToken = accessToken;
        this.defaultExceptionHandler = 
                Thread.getDefaultUncaughtExceptionHandler();
    }

    //defines the default behavior for any uncaught exceptions
    public void uncaughtException(Thread thread, Throwable ex) {
        final Writer stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));

        //iterate through the static fields of Build class to get phone info
        Field[] fields = Build.class.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    if ((!field.getName().equals("TIME")) && 
                            !field.getName().equals("SERIAL")) {
                        stackTraceJson.put(field.getName(),
                                field.get(null).toString());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            stackTraceJson.put("UNCAUGHT_EXCEPTION_STACKTRACE",
                    stackTrace.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //calls StormHTTPClient.sendEvents with stacktrace msg
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(new Runnable() {
            public void run() {
                new StormHTTPClient().sendEvents(projectId, accessToken,
                        stackTraceJson.toString());
            }
        });
        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.e(TAG, ">>>>>>>> STACK TRACE IN JSON <<<<<<<<");
        Log.e(TAG, "exception msg ==========> " + stackTraceJson.toString());

        defaultExceptionHandler.uncaughtException(thread, ex);
    }
}
