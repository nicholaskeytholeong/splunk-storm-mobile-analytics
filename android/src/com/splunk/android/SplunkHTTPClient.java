package com.splunk.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

/**
 * @author Nicholas Key (Splunk)
 */
public class SplunkHTTPClient {

    private static final String LINE_SEPARATOR = System.getProperty(
            "line.separator");
    private static final String TAG = "SplunkHTTPCient";
    private static String authToken = "";

    // Create a trust manager that does not validate certificate chains
    private final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                }
            }
    }; 

    private ArrayList<Object> getHTTPResponse(HttpsURLConnection connection) {

        // Get response data.
        Log.i(TAG, ">>>>>>>> GETTING HTTP RESPONSE MSG ... <<<<<<<<");
        ArrayList<Object> httpResp = new ArrayList<Object>();
        InputStream is = null;
        BufferedReader br = null;
        int respCode = -1;
        String respMsg = null;
        String respSplunk = null;
        String responseStr = null;
        StringBuilder response = null;

        try {
            is = connection.getErrorStream();
            if (connection.getErrorStream() == null) {
                is = connection.getInputStream();
            }
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            response = new StringBuilder(); 
            if (br != null) {
                while ((responseStr = br.readLine()) != null) {
                    response.append(responseStr);
                    response.append(LINE_SEPARATOR);
                }
            }
            br.close();

            respCode = connection.getResponseCode();
            respMsg = connection.getResponseMessage();
            respSplunk = response.toString();

            httpResp.add(respCode);
            httpResp.add(respMsg);
            httpResp.add(respSplunk);

            Log.i(TAG, respCode + ": " + respMsg);
            Log.i(TAG, "Response message: " + respSplunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return httpResp;
    }

    private HttpsURLConnection splunkHTTPPost(String splunkUrl,
            String dataInput) {

        URL url = null;
        HttpsURLConnection connection = null;
        OutputStreamWriter outputstreamwriter = null;

        try {
            url = new URL(splunkUrl);
            connection = (HttpsURLConnection)url.openConnection();

            //make POST request
            connection.setRequestMethod("POST");
            if (!authToken.equals("")) {
                connection.setRequestProperty("Authorization",
                        "Splunk " + authToken);
            }
            connection.setDoOutput(true);

            //no caching, no user interaction, no redirects
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(false);

            //POST to the login endpoint
            outputstreamwriter = new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8");
            outputstreamwriter.write(dataInput);
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
        return connection;
    }

    private String constructGetParams(HashMap<String, String> inputDict){
        StringBuilder paramString = new StringBuilder();
        ArrayList<String> listOfParams = new ArrayList<String>();
        for (String param : inputDict.keySet()) {
            listOfParams.add(param + "=" + inputDict.get(param));
        }

        Iterator<String> entries = listOfParams.iterator();
        while (entries.hasNext()){
            paramString.append(entries.next());
            if (entries.hasNext()){
                paramString.append("&");
            }
        }
        return paramString.toString();
    }
    
    public synchronized void sendEvents(
            String splunkUrl, String username, String password,
            String eventInput) {

        /**
         * TODO: 
         * Need to implement a more secured mechanism. Disabling the trust 
         * manager defeats some parts of SSL and allows vulnerability to 
         * man-in-the-middle attacks.
         */

        SSLContext sslContext = null;
        SSLSocketFactory sslSocketFactory = null;
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(
                    new SplunkHostNameVerifier());
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        HashMap<String, String> creds = new HashMap<String, String>();
        creds.put("username", username);
        creds.put("password", password);
        String userCreds = constructGetParams(creds); 
        
        int respCode = -1;
        String respSplunk = null;

        HttpsURLConnection connection = splunkHTTPPost(
                splunkUrl.concat("/services/auth/login"), userCreds);

        ArrayList<Object> splunkResponse = getHTTPResponse(connection);
        respCode = (Integer) splunkResponse.get(0);
        respSplunk = (String) splunkResponse.get(2);

        if (respCode == 200) {
            DocumentBuilder db = null;
            InputSource inputSource = null;
            Document doc = null;
            try {
                db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(respSplunk));
                doc = db.parse(inputSource);
                authToken = doc.getElementsByTagName("sessionKey").item(0)
                        .getTextContent();
                Log.i(TAG, "auth_token: " + authToken);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        HashMap<String, String> urlParams = new HashMap<String, String>();
        //sends events to 'main' index by default
        urlParams.put("sourcetype", "app_message");
        String urlGetParams = constructGetParams(urlParams); 

        String splunkReceiversEndpoint = splunkUrl.concat(
                "/services/receivers/simple").concat("?").concat(urlGetParams);
        connection = splunkHTTPPost(splunkReceiversEndpoint, eventInput);
        getHTTPResponse(connection);
    }
}

class SplunkHostNameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}