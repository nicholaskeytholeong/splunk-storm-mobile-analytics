/**
 * 
 */
package com.splunk.android;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import android.util.Log;


/**
 * @author nkey
 *
 */
public class SplunkTCPClient {

    public synchronized void sendEvents(String splunkIPAddr, int portNumber,
            String eventInput) {

        Log.i("SplunkTCPClient.sendEvents", "============>" + eventInput);

        /**
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
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pw.close();
        }
        **/

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
            /**
            bufferedwriter.write(eventInput);
            bufferedwriter.flush();
             */
            sslsocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
        SSLSocketFactory sslsocketfactory = null;
        SSLSocket sslsocket = null;
        OutputStreamWriter outputstreamwriter = null;
        BufferedWriter bufferedwriter = null;
        try {
            sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslsocket = (SSLSocket) sslsocketfactory.createSocket(splunkIPAddr,
                    portNumber);
            outputstreamwriter = new OutputStreamWriter(
                    sslsocket.getOutputStream(), "UTF-8");
            bufferedwriter = new BufferedWriter(outputstreamwriter);
            bufferedwriter.write(eventInput);
            bufferedwriter.flush();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
         **/
    }
}
