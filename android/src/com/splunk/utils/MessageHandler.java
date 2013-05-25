/**
 * Copyright 2013 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.splunk.utils;

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

import com.splunk.android.SplunkHTTPClient;
import com.splunk.android.SplunkTCPClient;
import com.storm.android.StormHTTPClient;

/**
 * @author Nicholas Key
 * @copyright Copyright 2013 Splunk, Inc.
 * @license Apache License 2.0
 */
public class MessageHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultExceptionHandler = Thread
            .getDefaultUncaughtExceptionHandler();
    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");
    private String splunkUrl = null;
    private String username = null;
    private String password = null;
    private String projectId = null;
    private String accessToken = null;
    private int portNumber = -1;
    private JSONObject stackTraceJson = new JSONObject();

    // private static final String TAG = "UNCAUGHT_EXCEPTION_HANLDER";

    public MessageHandler(String splunkUrl, String username,
            String password, Context applicationContext) {
        this.splunkUrl = splunkUrl;
        this.username = username;
        this.password = password;
    }

    public MessageHandler(String projectId, String accessToken,
            Context applicationContext) {
        this.projectId = projectId;
        this.accessToken = accessToken;
    }

    public MessageHandler(String splunkUrl, int portNumber,
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
            String strace = stackTrace.toString();
            StackTraceElement ste = ex.getStackTrace()[0];

            if (!stackTraceJson.has("sourcetype")) {
                stackTraceJson.put("SOURCETYPE", "app_message");
            }

            String origin = String
                    .format("%s.%s(%s:%s)", ste.getClassName(),
                            ste.getMethodName(), ste.getFileName(),
                            ste.getLineNumber());
            stackTraceJson.put("ORIGIN_OF_FAILURE", origin);
            stackTraceJson.put("EXCEPTION_CLASS",
                    strace.substring(0, strace.indexOf(LINE_SEPARATOR)));
            stackTraceJson.put("UNCAUGHT_EXCEPTION_STACKTRACE", strace);
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

        defaultExceptionHandler.uncaughtException(thread, ex);
    }
}
