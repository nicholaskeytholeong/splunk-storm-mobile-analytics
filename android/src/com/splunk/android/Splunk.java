package com.splunk.android;

import android.content.Context;
import android.util.Log;

/**
 * @author Nicholas Key (Splunk)
 */
public class Splunk {

    private static final String TAG = "SplunkLogger";

    public static synchronized void connect(
        String splunkUrl, String username, String password,
        Context applicationContext) {

        /**
         * TODO: 
         * 1. there could be a lot of app related info from Context
         * 2. persist stacktrace somewhere if no network is found, flush them
         *    to Splunk once network is available
         */
        Log.i(TAG, ">>>>>>>> Received <<<<<<<<");
        Log.i(TAG, username + ":" + password);

        Thread.setDefaultUncaughtExceptionHandler(
                new SplunkMessageHandler(splunkUrl, username, password,
                        applicationContext));
    }
}
