#Steps to reference this logging library into Android app
- Create a Splunk Storm account
- Make a reference to splunkstormmobileanalytics.jar to your Android mobile app project
- Connect to SplunkStorm in the main activity of the app

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SplunkStorm.connect("STORM_PROJECT_KEY", "STORM_ACCESS_TOKEN", getApplicationContext());
```

- Add access internet permission between the **"manifest"** element in the AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

- You are set
- Go to the Storm dashboard to perform crash analytics!
- Thank you for trying this logging library!
