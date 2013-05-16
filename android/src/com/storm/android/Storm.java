package com.storm.android;

import android.content.Context;

import com.splunk.utils.SplunkMessageHandler;

/**
 * @author Nicholas Key (Splunk)
 */
public class Storm {

    public static synchronized void connect(String projectId,
            String accessToken, Context applicationContext) {

        Thread.setDefaultUncaughtExceptionHandler(new SplunkMessageHandler(
                projectId, accessToken, applicationContext));
    }

    public static synchronized void TCPconnect(String splunkUrl,
            int portNumber, Context applicationContext) {

        Thread.setDefaultUncaughtExceptionHandler(new SplunkMessageHandler(
                splunkUrl, portNumber, applicationContext));
    }
}
