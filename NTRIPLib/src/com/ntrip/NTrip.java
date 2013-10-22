/**
 *----------------------------------------------------------------------------
 * NTrip library
 *------------------------------------------------------------------------------
 * INESC Inovação (INOV)
 * Av. Duque d'ávila no. 23
 * Lisboa
 * Portugal
 *------------------------------------------------------------------------------
 * Copyright (C) 2012 INESC Inovacao
 * All rights reserved.
 *
 * THE AUTHORING COMPANY DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT
 * SHALL THE AUTHORING COMPANY BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUEN-
 * TIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS AC-
 * TION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
 * SOFTWARE.
 * 
 * @author Fábio Barata @ INOV
 * @description Interface for ntrip service developed by creators. Based on ntrip example application
 *
 */

package com.ntrip;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;

abstract public class NTrip {
	DecimalFormat df = new DecimalFormat();
	
	private Activity activity;
	
	Messenger mService = null;
	boolean mIsBound;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	public String NetworkProtocol = "ntripv1";
	public String MACAddress = "";			// BLUETOOTH DEVICE MAC ADDRESS
	public String SERVERIP = "";			// Server IP
	public String SERVERPORT = "";			// Server Port
	public String USERNAME = "";			// Username
	public String PASSWORD = "";			// Password
	public String MOUNTPOINT = "";
	public boolean SendGGAToServer = false;
	
	/**
	 * Update status interruption
	 * @param fixtype current fix type. Values can be Invalid, GPS, DGPS, PPS, RTK, FloatRTK, Estimated, Manual, Simulation, WAAS or Unknown
	 */
	abstract public void UpdateStatus(String fixtype,String info1,String info2);
	
	/**
	 * Message logging from service
	 * @param msg message to log
	 */
	abstract public void UpdateLogAppend(String msg);
	
	/**
	 * Called when a new position fix arrives
	 * @param time time of the fix
	 * @param lat  latitude of the fix (degrees)
	 * @param lon  longitude of the fix (degrees)
	 */
	abstract public void UpdatePosition(double time,double lat,double lon);
	
	/**
	 * Called when application is connected to service
	 */
	abstract public void onServiceConnected();
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NTRIPService.MSG_UPDATE_STATUS:
				Bundle b1 = msg.getData();
				String fixtype = b1.getString("fixtype");
				String info1 = b1.getString("info1");
				String info2 = b1.getString("info2");
				LogMessage("Fix: "+fixtype);
				LogMessage("INFO1: "+info1);
				LogMessage("INFO2: "+info2);
				UpdateStatus(fixtype,info1,info2);
				break;
			case NTRIPService.MSG_UPDATE_BYTESIN:
				int bytesin = msg.arg1;
				LogMessage("total bytes: "+bytesin+" "+(bytesin%4096));
				break;
			case NTRIPService.MSG_SHOW_PROGRESSBAR:
				LogMessage("Show progressbar "+msg.arg1);
				break;
			case NTRIPService.MSG_UPDATE_LOG_APPEND:
				Bundle b2 = msg.getData();
				String append = b2.getString("logappend");
				LogMessage(append);
				UpdateLogAppend(append);
				break;
			case NTRIPService.MSG_UPDATE_LOG_FULL:
				Bundle b3 = msg.getData();
				LogMessage(b3.getString("logfull"));
				break;
			case NTRIPService.MSG_THREAD_SUICIDE:
				//Log.i("Activity", "Service informed Activity of Suicide.");
				doUnbindService();
				activity.stopService(new Intent(activity, NTRIPService.class));
				LogMessage("Service Stopped");
				break;
			case NTRIPService.MSG_UPDATE_POSITION:
				Bundle b4 = msg.getData();
				double time = b4.getDouble("time",-1.0);
				double lat = b4.getDouble("lat",0.0);
				double lon = b4.getDouble("lon",0.0);
				LogMessage("Updated position: Time "+time+" Lat "+lat+" Lon "+lon);
				UpdatePosition(time,lat,lon);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				//Register client with service
				Message msg = Message.obtain(null, NTRIPService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);

				//Request a status update.
				msg = Message.obtain(null, NTRIPService.MSG_UPDATE_STATUS, 0, 0);
				mService.send(msg);

				//Request full log from service.
				msg = Message.obtain(null, NTRIPService.MSG_UPDATE_LOG_FULL, 0, 0);
				mService.send(msg);
				
				SetSettings();
				
				NTrip.this.onServiceConnected();
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do anything with it
			}
		}
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been unexpectedly disconnected - process crashed.
			mService = null;
		}
	};
	
	
	public NTrip(Activity activity) {
		this.activity = activity;
		CheckIfServiceIsRunning();
		
		onResume();
	}
	
	public void onResume() {
		if (mIsBound) { // Request a status update.
			if (mService != null) {
				try {
					//Request service reload preferences, in case those changed
					Message msg = Message.obtain(null, NTRIPService.MSG_RELOAD_PREFERENCES, 0, 0);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {}
			}
		}
	}
	
	private void CheckIfServiceIsRunning() {
		//If the service is running when the activity starts, we want to automatically bind to it.
		if (NTRIPService.isRunning())
			doBindService();
	}
	
	public void Connect() {
		SetSettings();
		LogMessage("Starting Service");
		if (!NTRIPService.isRunning())
			activity.startService(new Intent(activity, NTRIPService.class));
		doBindService();
	}
	
	public void Disconnect() {
		doUnbindService();
		if (NTRIPService.isRunning())
			activity.stopService(new Intent(activity, NTRIPService.class));
		LogMessage("Service Stopped");
	}
	
	private void LogMessage(String m) {
		//android.util.Log.d("Debug","LogMessage: "+m);
	}
	
	private void SetSettings() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		Editor editor = preferences.edit();
		editor.putString("networkprotocol", NetworkProtocol);
		editor.putString("bluetooth_mac",MACAddress);
		editor.putString("ntripcasterip",SERVERIP);
		editor.putString("ntripcasterport", SERVERPORT);
		editor.putString("ntripusername",USERNAME);
		editor.putString("ntrippassword",PASSWORD);
		editor.putString("ntripstream",MOUNTPOINT);
		editor.putBoolean("ntripsendggatocaster", SendGGAToServer);
		editor.commit();
	}
	
	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		activity.bindService(new Intent(activity, NTRIPService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		if (mService != null) {
			try {
				//Request status update
				Message msg = Message.obtain(null, NTRIPService.MSG_UPDATE_STATUS, 0, 0);
				msg.replyTo = mMessenger;
				mService.send(msg);

				//Request full log from service.
				msg = Message.obtain(null, NTRIPService.MSG_UPDATE_LOG_FULL, 0, 0);
				mService.send(msg);
			} catch (RemoteException e) {}
		}
	}
	void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it, then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null, NTRIPService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has crashed.
				}
			}
			// Detach our existing connection.
			activity.unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	protected void finalize() {
		try {
			doUnbindService();
		} catch (Throwable t) {
			//Log.e("MainActivity", "Failed to unbind from the service", t);
		}
	}
	
	public boolean isBound() {
		return mIsBound;
	}
}