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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * @author Nicholas Key
 * @copyright Copyright 2013 Splunk, Inc.
 * @license Apache License 2.0
 */
public class SplunkTCPClient {

    public synchronized void sendEvents(String splunkIPAddr, int portNumber,
            String eventInput) {

        Log.i("TCPClient.sendEvents", "============>" + eventInput);

        Socket socket = null;
        PrintWriter pw = null;
        OutputStreamWriter outputstreamwriter = null;
        BufferedWriter bufferedwriter = null;
        try {
            socket = new Socket(splunkIPAddr, portNumber);
            outputstreamwriter = new OutputStreamWriter(
                    socket.getOutputStream(), "UTF-8");
            bufferedwriter = new BufferedWriter(outputstreamwriter);
            pw = new PrintWriter(bufferedwriter, true);
            pw.print(eventInput);
            bufferedwriter.flush();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pw.close();
        }

        /**
        SSLSocketFactory sslsocketfactory = null;
        SSLSocket sslsocket = null;
        PrintWriter pw = null;
        OutputStreamWriter outputstreamwriter = null;
        BufferedWriter bufferedwriter = null;
        try {
            sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslsocket = (SSLSocket) sslsocketfactory.createSocket(splunkIPAddr,
                    portNumber);
            outputstreamwriter = new OutputStreamWriter(
                    sslsocket.getOutputStream(), "UTF-8");
            bufferedwriter = new BufferedWriter(outputstreamwriter);
            pw = new PrintWriter(bufferedwriter, true);
            pw.print(eventInput);
            pw.close();
            //bufferedwriter.write(eventInput);
            //bufferedwriter.flush();
            sslsocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        **/

    }
}
