package com.splunkstorm.android;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * @author Nicholas Key (Splunk)
 */
public class SplunkStorm {

    private static final String TAG = "StormLogger";

    public static synchronized void connect(
            String projectId, String accessToken, Context applicationContext) {

        /**
         * TODO: 
         * 1. there could be a lot of app related info from Context
         * 2. persist stacktrace somewhere if no network is found, flush them
         *    to Storm once network is available
         */
        Log.i(TAG, ">>>>>>>> Received <<<<<<<<");
        Log.i(TAG, accessToken + ": " + accessToken);

        new connectTask().execute(projectId, accessToken);
    }
}

class connectTask extends AsyncTask<String, Object, Object>{

    protected Object doInBackground(String... params) {
        String projectId = params[0];
        String accessToken = params[1];
        Thread.setDefaultUncaughtExceptionHandler(
                new StormMessageHandler(projectId, accessToken));
        return null;
    }
}
