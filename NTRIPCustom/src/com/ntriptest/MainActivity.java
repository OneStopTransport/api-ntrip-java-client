/**
 *----------------------------------------------------------------------------
 * NTrip example
 *------------------------------------------------------------------------------
 * INESC Inovação (INOV)
 * Av. Duque d'ávila no. 23
 * Lisboa
 * Portugal
 *------------------------------------------------------------------------------
  * @author Fábio Barata @ INOV
 * @description NTrip example application (similar to original) using the NTrip library
 *
 */

package com.ntriptest;

import java.text.DecimalFormat;

import com.ntrip.NTRIPService;
import com.ntrip.NTrip;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
//import android.util.Log;

public class MainActivity extends Activity {
	Button btnService;
	TextView textLog;
	ScrollView svLog;
	private Boolean KeepScreenOn = false;
	DecimalFormat df = new DecimalFormat();
	
	private NTrip ntrip;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		btnService = (Button)findViewById(R.id.btnService);
		textLog = (TextView)findViewById(R.id.textLog);
		svLog = (ScrollView)findViewById(R.id.svLog);
		textLog.setText(SetDefaultStatusText());
		
		btnService.setOnClickListener(ListenerBtnService);
		
		restoreMe(savedInstanceState);
		
		ntrip = new NTrip(this) {
			@Override
			public void onServiceConnected() {
			}

			@Override
			public void UpdatePosition(double time, double lat, double lon) {
				LogMessage(String.format("Time: %.0f Lat: %.6fº Lon: %.6fº",time,lat,lon));
			}
			
			@Override
			public void UpdateLogAppend(String msg) {
				LogMessage("LogMessage: "+msg);
			}

			@Override
			public void UpdateStatus(String fixtype, String info1, String info2) {
				LogMessage("LogMessage: Fix "+fixtype);
			}
		};
		
		ntrip.MACAddress = "00:06:66:51:25:3D";		// BT device mac address
		ntrip.SERVERIP = "vrs.ost.pt";			// Server IP
		ntrip.SERVERPORT = "2101";					// Server port
		ntrip.USERNAME = "serverusername";					// Server username
		ntrip.PASSWORD = "serverpassword";				// Server password
		ntrip.MOUNTPOINT = "BOCH0";
		ntrip.SendGGAToServer = false;
		
		CheckIfServiceIsRunning();
	}
	
	private String SetDefaultStatusText() {
		String t = ""; 
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			return "Version: " + packageInfo.versionName + "\n" + t;
		} catch (PackageManager.NameNotFoundException e) {
			return t;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		KeepScreenOn = preferences.getBoolean("keepscreenon", false);
		
		if (KeepScreenOn) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("connectbuttontext", btnService.getText().toString());
		outState.putString("textlog", textLog.getText().toString());
	}
	
	private void restoreMe(Bundle state) {
		if (state!=null) {
			btnService.setText(state.getString("connectbuttontext"));
			textLog.setText(state.getString("textlog"));
			svLog.post(new Runnable() {
				public void run() {
					svLog.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}
	}
	
	private void CheckIfServiceIsRunning() {
		//If the service is running when the activity starts, we want to automatically bind to it.
		if (ntrip.isBound()) {
			btnService.setText("Disconnect");
		} else {
			btnService.setText("Connect");
		}
	}
	
	private OnClickListener ListenerBtnService = new OnClickListener() {
		public void onClick(View v){
			if(btnService.getText().equals("Connect")){
				LogMessage("Starting Service");
				startService(new Intent(MainActivity.this, NTRIPService.class));
				ntrip.Connect();
				
				btnService.setText("Disconnect");
			} else {
				ntrip.Disconnect();
				stopService(new Intent(MainActivity.this, NTRIPService.class));
				LogMessage("Service Stopped");
				
				btnService.setText("Connect");
			}
		}
	};
	
	private void LogMessage(String m) {
		//Check if log is too long, shorten if necessary.
		if (textLog.getText().toString().length() > 4000) {
			String templog = textLog.getText().toString();
			int tempi = templog.length();
			tempi = templog.indexOf("\n", tempi-1000);
			textLog.setText(templog.substring(tempi+1));
		}
		
		textLog.append("\n" + m);
		svLog.post(new Runnable() { 
			public void run() {
				svLog.fullScroll(ScrollView.FOCUS_DOWN); 
			}
		}); 
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (KeepScreenOn) {
			getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		
		try {
			ntrip.Disconnect();
		} catch (Throwable t) {
			//Log.e("MainActivity", "Failed to unbind from the service", t);
		}
	}
}
	