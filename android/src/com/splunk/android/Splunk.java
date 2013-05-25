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

package com.splunk.android;

import android.content.Context;
import android.util.Log;

import com.splunk.utils.MessageHandler;

/**
 * @author Nicholas Key
 * @copyright Copyright 2013 Splunk, Inc.
 * @license Apache License 2.0
 */
public class Splunk {

    private static final String TAG = "SplunkLogger";

    /**
     * TODO: 
     * 1. there could be a lot of app related info from Context
     * 2. persist stacktrace somewhere if no network is found, flush them
     *    to Splunk once network is available
     */
    public static synchronized void connect(
            String splunkUrl, String username, String password,
            Context applicationContext) {

        Log.i(TAG, ">>>>>>>> Received <<<<<<<<");
        Log.i(TAG, username + ":" + password);

        Thread.setDefaultUncaughtExceptionHandler(
                new MessageHandler(splunkUrl, username, password,
                        applicationContext));
    }

    public static synchronized void TCPconnect(
            String splunkUrl, int portNumber, Context applicationContext) {
        Log.i(TAG, ">>>>>>>> Received <<<<<<<<");
        Log.i(TAG, splunkUrl + ":" + portNumber);

        Thread.setDefaultUncaughtExceptionHandler(
                new MessageHandler(splunkUrl, portNumber,
                        applicationContext));
    }
}
