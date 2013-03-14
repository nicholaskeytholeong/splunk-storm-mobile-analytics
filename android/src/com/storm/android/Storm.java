package com.storm.android;

import android.content.Context;
import android.util.Log;

import com.splunk.android.SplunkMessageHandler;

/**
 * @author Nicholas Key (Splunk)
 */
public class Storm {

    private static final String TAG = "StormLogger";

    public static synchronized void connect(
            String projectId, String accessToken, Context applicationContext) {

        Log.i(TAG, ">>>>>>>> Received <<<<<<<<");
        Log.i(TAG, projectId + ": " + accessToken);

        Thread.setDefaultUncaughtExceptionHandler(
                new SplunkMessageHandler(projectId, accessToken,
                        applicationContext));
    }
}
