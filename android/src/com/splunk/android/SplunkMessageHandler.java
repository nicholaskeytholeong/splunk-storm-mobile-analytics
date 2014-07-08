package com.splunk.android;

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

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.storm.android.StormHTTPClient;

/**
 * @author Nicholas Key (Splunk)
 */
public class SplunkMessageHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultExceptionHandler = Thread
            .getDefaultUncaughtExceptionHandler();
    private String splunkUrl = null;
    private String username = null;
    private String password = null;
    private String projectId = null;
    private String accessToken = null;
    private int portNumber = -1;
    private JSONObject stackTraceJson = new JSONObject();
    private static final String TAG = "UNCAUGHT_EXCEPTION_HANLDER";

    public SplunkMessageHandler(String splunkUrl, String username,
            String password, Context applicationContext) {
        this.splunkUrl = splunkUrl;
        this.username = username;
        this.password = password;
    }

    public SplunkMessageHandler(String projectId, String accessToken,
            Context applicationContext) {
        this.projectId = projectId;
        this.accessToken = accessToken;
    }

    public SplunkMessageHandler(String projectId, String accessToken,
            Context applicationContext, String splunkUrl) {
        this.projectId = projectId;
        this.accessToken = accessToken;
        this.splunkUrl = splunkUrl;
    }

    public SplunkMessageHandler(String splunkUrl, int portNumber,
            Context applicationContext) {
        this.splunkUrl = splunkUrl;
        this.portNumber = portNumber;
    }

    // defines the default behavior for any uncaught exceptions
    public void uncaughtException(Thread thread, Throwable ex) {
        final Writer stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));

        // iterate through the static fields of Build class to get phone info
        Field[] fields = Build.class.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    if ((!field.getName().equals("TIME"))
                            && !field.getName().equals("SERIAL")) {
                        stackTraceJson.put(field.getName(), field.get(null)
                                .toString());
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
            if (!stackTraceJson.has("sourcetype")) {
                stackTraceJson.put("SOURCETYPE", "app_message");
            }
            stackTraceJson.put("UNCAUGHT_EXCEPTION_STACKTRACE",
                    stackTrace.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // calls respective sendEvents method with stacktrace msg
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(new Runnable() {
            public void run() {
                if (splunkUrl != null) {
                    if (username != null && password != null) {
                        new SplunkHTTPClient().sendEvents(splunkUrl, username,
                                password, stackTraceJson.toString());
                    } else if (portNumber > -1) {
                        new SplunkTCPClient().sendEvents(splunkUrl, portNumber,
                                stackTraceJson.toString());
                    }
                } else {
                    new StormHTTPClient().sendEvents(projectId, accessToken,
                            stackTraceJson.toString());
                }
            }
        });
        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        StackTraceElement[] strace = ex.getStackTrace();
        for (StackTraceElement st : strace) {
            Log.e(TAG,
                    "phone build ==========> " + st.getClassName() + " "
                            + st.getFileName() + " " + st.getLineNumber() + " "
                            + st.getMethodName());
        }

        defaultExceptionHandler.uncaughtException(thread, ex);
    }
}
