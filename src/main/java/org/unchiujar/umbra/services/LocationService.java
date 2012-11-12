/*******************************************************************************
 * This file is part of Umbra.
 * 
 *     Umbra is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     Umbra is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with Umbra.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *     Copyright (c) 2011 Vasile Jureschi <vasile.jureschi@gmail.com>.
 *     All rights reserved. This program and the accompanying materials
 *     are made available under the terms of the GNU Public License v3.0
 *     which accompanies this distribution, and is available at
 *     
 *    http://www.gnu.org/licenses/gpl-3.0.html
 * 
 *     Contributors:
 *        Vasile Jureschi <vasile.jureschi@gmail.com> - initial API and implementation
 ******************************************************************************/

package org.unchiujar.umbra.services;

import java.util.ArrayList;

import org.unchiujar.umbra.R;
import org.unchiujar.umbra.activities.FogOfExplore;
import org.unchiujar.umbra.location.LocationOrder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class LocationService extends Service {
	private static final int APPLICATION_ID = 1241241;
	private NotificationManager notificationManager;

	private static final String TAG = LocationService.class.getName();

	public static final String MOVEMENT_UPDATE = "org.unchiujar.umbra.MOVEMENT_UPDATE";

	public static final String LATITUDE = "org.unchiujar.umbra.LocationService.LATITUDE";

	public static final String LONGITUDE = "org.unchiujar.umbra.LocationService.LONGITUDE";
	public static final String ACCURACY = "org.unchiujar.umbra.LocationService.ACCURACY";

	/**
	 * Command to the service to register a client, receiving callbacks from the
	 * service. The Message's replyTo field must be a Messenger of the client
	 * where callbacks should be sent.
	 */
	public static final int MSG_REGISTER_CLIENT = 1;

	/**
	 * Command to the service to un`register a client, or stop receiving
	 * callbacks from the service. The Message's replyTo field must be a
	 * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	 */
	public static final int MSG_UNREGISTER_CLIENT = 2; // This is called when
														// the connection with
														// the service has been
	// established, giving us the service object we can use to
	// interact with the service. We are communicating with our
	// service through an IDL interface, so get a client-side
	// representation of that from the raw service object.

	public static final int MSG_UNREGISTER_INTERFACE = 4;
	public static final int MSG_REGISTER_INTERFACE = 5;

	public static final int MSG_WALK = 6;
	public static final int MSG_DRIVE = 7;

	public static final int MSG_LOCATION_CHANGED = 9000;

	/**
	 * Command to service to set a new value. This can be sent to the service to
	 * supply a new value, and will be sent by the service to any registered
	 * clients with the new value.
	 */
	public static final int MSG_SET_VALUE = 3;

	/**
	 * Walk update frequency for average walking speed. Average distance covered
	 * by humans while walking is 1.3 m/s. Using double update time for safety.
	 */
	private static final long WALK_UPDATE_INTERVAL = (long) (LocationOrder.METERS_RADIUS * 2 / 1.3 * 1000) / 2;

	/**
	 * Update frequency for driving at 50 km/h. Using double update time for
	 * safety.
	 */
	private static final long DRIVE_UPDATE_INTERVAL = (long) (LocationOrder.METERS_RADIUS * 2 / 13 * 1000) / 2;

	/** Fast update frequency for screen on state. */
	private static final long SCREEN_ON_UPDATE_INTERVAL = 1000;
	/** Update distance for screen on state. */
	private static final long SCREEN_ON_UPDATE_DISTANCE = 1;

	/**
	 * Initial backoff interval is double the
	 * {@link LocationService#WALK_UPDATE_INTERVAL}.
	 */
	private static final long INITIAL_BACKOFF_INTERVAL = WALK_UPDATE_INTERVAL * 2;
	/** Location search duration. */
	private static final long LOCATION_SEARCH_DURATON = 30 * 1000;
	/** The maximum duration the location listeners should be put to sleep. */
	protected static final long MAX_BACKOFF_INTERVAL = 10 * 60 * 1000;

	private boolean mWalking = true;

	/** Keeps track of all current registered clients. */
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	/** Holds last value set by a client. */
	private int mValue = 0;

	private LocationManager mLocationManager;
	private volatile boolean mOnScreen;

	private LocationListener mFine = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			Log.d(TAG, "Sending fine fast location :" + location);
			// send the location before doing any other work
			sendLocation(location);
			// only start the algorithm if the app is running in the background
			if (!mOnScreen) {
				restartBackoff();
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// NO-OP
		}

		@Override
		public void onProviderEnabled(String provider) {
			// NO-OP
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// NO-OP
		}

	};

	private void sendLocation(Location location) {
		Log.d(TAG, "Location changed: " + location);
		for (int i = mClients.size() - 1; i >= 0; i--) {

			try {
				// Send data as an Integer
				mClients.get(i).send(
						Message.obtain(null, MSG_LOCATION_CHANGED, location));

			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going
				// through the list from
				// back to front so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}

		Message message = Message.obtain();
		message.obj = location;

		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			Log.e(TAG, "Error sending location", e);
		}

	}

	// ==================== LIFECYCLE METHODS ====================

	@Override
	public void onCreate() {

		super.onCreate();
		displayRunningNotification();
		Log.d(TAG, "Location manager set up.");

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		setOffScreenState();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		mLocationManager.removeUpdates(mFine);

		notificationManager.cancel(APPLICATION_ID);
		Log.d(TAG, "Service on destroy called.");
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "Unbind called.");
		return super.onUnbind(intent);
	}

	// =================END LIFECYCLE METHODS ====================

	private void displayRunningNotification() {
		String contentTitle = getString(R.string.app_name);
		String running = getString(R.string.running);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		CharSequence tickerText = contentTitle + " " + running;

		// instantiate notification
		Notification notification = new NotificationCompat.Builder(this)
				.setTicker(tickerText).setWhen(System.currentTimeMillis())
				.setSmallIcon(R.drawable.icon).getNotification();

		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		// Define the Notification's expanded message and Intent:
		CharSequence contentText = contentTitle + " " + running;
		Intent notificationIntent = new Intent(this, FogOfExplore.class);
		// notificationIntent.setAction("android.intent.action.VIEW");
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, contentTitle, contentText,
				contentIntent);
		notificationManager.notify(APPLICATION_ID, notification);
	}

	/**
	 * Handler of incoming messages from clients`.
	 */
	private class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_WALK:
				mWalking = true;
				break;
			case MSG_DRIVE:
				mWalking = false;
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_UNREGISTER_INTERFACE:
				// remove client
				mClients.remove(msg.replyTo);
				Log.d(TAG, "Setting the service to off screen state.");
				setOffScreenState();
				break;

			case MSG_REGISTER_INTERFACE:
				Log.d(TAG, "Setting the service to on screen state.");
				// register client
				setOnScreeState();
				break;
			case MSG_REGISTER_CLIENT:
				Log.d(TAG, "Registering new client.");
				mValue = msg.arg1;
				mClients.add(msg.replyTo);
				for (int i = mClients.size() - 1; i >= 0; i--) {
					try {
						mClients.get(i).send(
								Message.obtain(null, MSG_SET_VALUE, mValue, 0));
					} catch (RemoteException e) {
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						mClients.remove(i);
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}

	}

	/**
	 * Turns off fast GPS updates when the application is not in foreground.
	 */
	private void setOffScreenState() {
		mOnScreen = false;
		// move from screen on updates to regular speed updates
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				mWalking ? WALK_UPDATE_INTERVAL : DRIVE_UPDATE_INTERVAL, 0,
				mFine);
		restartBackoff();
	}

	/**
	 * Turns on fast GPS updates when the application is in foreground and tries
	 * to display the last known location.
	 */
	private void setOnScreeState() {
		mOnScreen = true;
		stopBackoff();
		Location network = mLocationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location gps = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		// Location passive =
		// mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

		// Location toSend = (gps != null) ? gps : (network != null) ? network
		// : (passive != null) ? passive : null;
		Location toSend = (gps != null) ? gps : (network != null) ? network
				: null;

		if (toSend != null) {
			sendLocation(toSend);
		}

		// register fast location listener
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				SCREEN_ON_UPDATE_INTERVAL, SCREEN_ON_UPDATE_DISTANCE, mFine);

	}

	// *----------- Exponential backoff code ---------------
	/**
	 * Handler to post start and stop location updates runnables.
	 */
	private Handler mBackoffHandler = new Handler();

	/**
	 * The shortest duration of backoff time. The initial value is twice the
	 * frequency of {@link #WALK_UPDATE_INTERVAL} since it doesn't make sense to
	 * request locations faster when we have GPS fix problems than we there is a
	 * normal location update.
	 */
	private long mBackoffTime = INITIAL_BACKOFF_INTERVAL;

	/**
	 * Stops the location updates and posts a request to start them in after
	 * {@link #mBackoffTime} interval .
	 */
	private Runnable stopLocationRequest = new Runnable() {

		@Override
		public void run() {
			mLocationManager.removeUpdates(mFine);
			// only start if the backoff algorithm is enabled
			// necessary as the stopBackoff method may try to remove the
			// runnables while the
			// runnables are running with the efect that the algorithm doesn't
			// stop
			if (mBackoffStarted) {
				// start location requests after we have waited the backoff time
				mBackoffHandler
						.postDelayed(startLocationRequests, mBackoffTime);
			}
			// if the backoff interval has not increased to the max value
			// double the interval
			// need in order not to grow the backoff interval indefinitely
			Log.d(TAG, "Location requests stopped and will be started in "
					+ mBackoffTime + " milliseconds.");

			if (mBackoffTime < MAX_BACKOFF_INTERVAL) {
				mBackoffTime *= 2;
			}

		}
	};

	/**
	 * Starts the location updates and post a request to stop them using
	 * {@link #stopLocationRequest} after {@link #LOCATION_SEARCH_DURATON}
	 * interval.
	 */
	private Runnable startLocationRequests = new Runnable() {

		@Override
		public void run() {
			Log.d(TAG, "Location requests started and will be stopped in "
					+ LOCATION_SEARCH_DURATON + " milliseconds.");

			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER,
					mWalking ? WALK_UPDATE_INTERVAL : DRIVE_UPDATE_INTERVAL, 0,
					mFine);
			// stop location requests after we waited for the location search
			// duration
			mBackoffHandler.postDelayed(stopLocationRequest,
					LOCATION_SEARCH_DURATON);
		}
	};
	private volatile boolean mBackoffStarted;

	/**
	 * Restarts the entire backoff algorithm. Called everytime we have a
	 * location fix.
	 */
	private void restartBackoff() {
		Log.d(TAG, "Restarting backoff handler, last backoff time was "
				+ mBackoffTime);
		stopBackoff();
		mBackoffStarted = true;

		// post a request to stop the location updates but give a chance to the
		// location listeners to get a location and restart the backoff
		// algorithm again
		Log.d(TAG,
				"Initial backoff stop listener request. The updates will be stopped in "
						+ mBackoffTime * 2 + " milliseconds.");
		mBackoffHandler.postDelayed(stopLocationRequest, mBackoffTime * 2);
	}

	/**
	 * Stop the exponential backoff algorithm.
	 */
	private void stopBackoff() {
		Log.d(TAG, "Stopping backoff last backoff time was " + mBackoffTime);
		mBackoffTime = mWalking ? WALK_UPDATE_INTERVAL * 2
				: DRIVE_UPDATE_INTERVAL * 2;
		mBackoffStarted = false;
		Log.d(TAG, "Backoff time reset to  " + mBackoffTime);
		// remove any runnables from backoff handler
		mBackoffHandler.removeCallbacks(startLocationRequests);
		mBackoffHandler.removeCallbacks(stopLocationRequest);

	}

	// *----------- Exponential backoff code end ---------------

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

}
