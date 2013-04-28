/**
 * 
 */
package com.storm.android;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;


/**
 * @author nkey
 *
 */
public class StormTCPClient {

    private static final String TAG = "StormTCPClient";

    public synchronized void sendEvents(String splunkIPAddr, int portNumber,
            String eventInput) {

        Log.i(TAG, eventInput);

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

    }
}
