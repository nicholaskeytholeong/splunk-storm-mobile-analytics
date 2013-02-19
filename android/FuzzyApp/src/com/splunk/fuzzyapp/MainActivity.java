package com.splunk.fuzzyapp;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	private static final String TAG = "MyActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	try
                {
                    throw new Exception("button 1 crashes");
                }
                catch (Exception exception)
                {
            		Log.e(TAG, exception.getMessage());
                }
            }
        });
	}

	public void selfDestruct(View view) {
		try
        {
            throw new Exception("button is clicked");
        }
        catch (Exception exception)
        {
    		Log.e(TAG, exception.getMessage());
        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
