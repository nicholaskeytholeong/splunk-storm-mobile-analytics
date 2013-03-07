#Steps to reference this logging library into Android app
1. Create an Splunk Storm account
2. Make a reference to splunkstormmobileanalytics.jar
3. Connect to SplunkStorm in the main activity of the app
```java
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplunkStorm.connect("STORM_PROJECT_KEY", "STORM_ACCESS_TOKEN", getApplicationContext());
```
4. Add access internet permission in the AndroidManifest.xml

    
    <uses-permission android:name="android.permission.INTERNET"/>
  	

5. You are set
6. Go to the Storm dashboard to perform crash analytics!
7. Thank you for trying this logging library!
