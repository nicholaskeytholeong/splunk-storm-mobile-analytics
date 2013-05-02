#Steps to reference this logging library into Android app
- Create a Splunk Storm account
- Move splunkstormmobileanalytics.jar into the libs directory of your Android mobile app project
- Connect to SplunkStorm in the main activity of the app

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Storm.connect("STORM_PROJECT_KEY", "STORM_ACCESS_TOKEN", getApplicationContext());
```

- You may also send events through TCP

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Storm.TCPconnect("STORM_RECEIVING_URL", PORT_NUMBER, getApplicationContext());
```

- Add access internet permission between the **"manifest"** element in the AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

- If you wish to forward the data to Splunk Enterprise, do the following:

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Splunk.connect("SPLUNKD_URL", "SPLUNKD_USERNAME", "SPLUNKD_PASSWORD", getApplicationContext());
```

- You are set
- Go to the Storm dashboard to perform crash analytics!
- Thank you for trying this logging library!
