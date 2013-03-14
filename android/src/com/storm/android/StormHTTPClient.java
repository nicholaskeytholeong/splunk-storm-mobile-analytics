package com.storm.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import android.util.Base64;
import android.util.Log;

/**
 * @author Nicholas Key (Splunk)
 */
public class StormHTTPClient {

    private static final String LINE_SEPARATOR = System.getProperty(
            "line.separator");
    private static final String TAG = "StormHTTPCient";

    public synchronized void sendEvents(
            String projectId, String accessToken, String eventInput) {

        //constructs url GET params
        StringBuilder urlGetParams = new StringBuilder();
        HashMap<String, String> urlParams = new HashMap<String, String>();
        urlParams.put("index", projectId);
        urlParams.put("sourcetype", "app_message");

        ArrayList<String> listOfParams = new ArrayList<String>();
        for (String param : urlParams.keySet()) {
            listOfParams.add(param + "=" + urlParams.get(param));
        }

        Iterator<String> entries = listOfParams.iterator();
        while (entries.hasNext()){
            urlGetParams.append(entries.next());
            if (entries.hasNext()){
                urlGetParams.append("&");
            }
        }

        //makes HTTP POST request
        String stormInputUrl = "https://api.splunkstorm.com/1/inputs/http";
        String userCreds = "x:" + accessToken;

        String basicAuth = null;
        try {
            basicAuth = "Basic " + Base64.encodeToString(
                    userCreds.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        URL url = null;
        HttpsURLConnection connection = null;
        OutputStreamWriter outputstreamwriter = null;
        try {
            stormInputUrl = stormInputUrl.concat("?").concat(
                    urlGetParams.toString());

            Log.i(TAG, ">>>>>>>> HTTP POST REQUEST <<<<<<<<");
            Log.i(TAG, "stormInputUrl ==========> " + stormInputUrl);
            Log.i(TAG, "accessToken ==========> " + accessToken);
            Log.i(TAG, "encodedInput ==========> " + eventInput);

            url = new URL(stormInputUrl);
            connection = (HttpsURLConnection)url.openConnection();

            //make POST request
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty(
                    "Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            //no caching, no user interaction, no redirects
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(false);

            //POST to the API input endpoint
            outputstreamwriter = new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8");
            outputstreamwriter.write(eventInput);
            outputstreamwriter.flush();

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get response data.
        Log.i(TAG, ">>>>>>>> GETTING HTTP RESPONSE MSG ... <<<<<<<<");
        InputStream is = null;
        BufferedReader br = null;
        try {
            is = connection.getErrorStream();
            if (connection.getErrorStream() == null) {
                is = connection.getInputStream();
            }
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String responseStr = null;

            StringBuilder response = new StringBuilder(); 
            if (br != null) {
                while ((responseStr = br.readLine()) != null) {
                    response.append(responseStr);
                    response.append(LINE_SEPARATOR);
                }
            }
            br.close();

            Log.i(TAG, connection.getResponseCode() + ": " + 
                    connection.getResponseMessage());
            Log.i(TAG, "Response message: " + response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect(); 
            }
        }
        return;
    }
}
