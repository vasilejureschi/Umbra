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

package org.unchiujar.umbra;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

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
     * Command to the service to unregister a client, ot stop receiving
     * callbacks from the service. The Message's replyTo field must be a
     * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;


    public static final int MSG_UNREGISTER_INTERFACE = 4;
    public static final int MSG_REGISTER_INTERFACE = 5;

    
    /**
     * Command to service to set a new value. This can be sent to the service to
     * supply a new value, and will be sent by the service to any registered
     * clients with the new value.
     */
    public static final int MSG_SET_VALUE = 3;
    protected static final long LOCATION_UPDATE_INTERVAL = 15 * 1000;

    /** Keeps track of all current registered clients. */
    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    private int mValue = 0;

    /**
     * Flag signaling we have a fix and the location taken through the GPS is
     * updated at a slow rate.
     */
    private boolean mGPSSlowMode;
    /**
     * Flag signaling we have a GPS fix. If we don't have a GPS fix (is false)
     * and the GPS slow mode is active than we have lost the GPS signal and need
     * to get the location through the network until a GPS fix is reestablished.
     */
    private boolean mHasGPSFix;
    private LocationManager mLocationManager;
    private long mLastLocationMillis;
    private Location mLastLocation;

    private LocationListener mCoarse = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Sending mCoarse location :" + location);
            sendLocation(location);
            // make the updates slower as we already have a fix
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    2 * 60 * 1000,
                    500,
                    mCoarse);

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

    private LocationListener mFine = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Sending fine fast location :" + location);
            // send the location before doing any other work
            sendLocation(location);
            // unregister the mCoarse listener as we have a better fix
            mLocationManager.removeUpdates(mCoarse);
            mLocationManager.removeUpdates(mFine);
            // we already have a fix so
            // set a slower time and longer distance for location updates
            // in order to conserve battery
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    (float) LocationOrder.METERS_RADIUS * 2, mGPSSlow);
            mGPSSlowMode = true;
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    };

    private LocationListener mGPSSlow = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if (location == null) {
                return;
            }
            mLastLocationMillis = SystemClock.elapsedRealtime();

            Log.d(TAG, "Sending regular  location :" + location);
            // send the location before doing any other work
            sendLocation(location);
            mLastLocation = location;
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    };

    private GpsStatus.Listener mGPSListener = new GpsStatus.Listener() {

        @Override
        public void onGpsStatusChanged(int event) {
            Log.v(TAG, "Checking sattelite state " + event);
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    if (mLastLocation != null) {
                        mHasGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 5 * 1000;
                    }
                    if (!mHasGPSFix && mGPSSlowMode) {
                        Log.d(TAG, "Retrying fast location fix as GPS fix is lost.");
                        doFastLocationFix();
                    }
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    mHasGPSFix = true;
                    break;
            }
        }
    };

    private void sendLocation(Location location) {
        Log.d(TAG, "Location changed: " + location);
        for (int i = mClients.size() - 1; i >= 0; i--) {

            try {
                // Send data as an Integer
                mClients.get(i).send(Message.obtain(null, 9000, location));

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    public void onCreate() {

        super.onCreate();
        displayRunningNotification();
        Log.d(TAG, "Location manager set up.");
        doFastLocationFix();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mLocationManager.removeUpdates(mCoarse);
        mLocationManager.removeUpdates(mFine);
        mLocationManager.removeUpdates(mGPSSlow);

        notificationManager.cancel(APPLICATION_ID);
        Log.d(TAG, "Service on destroy called.");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind called.");
        return super.onUnbind(intent);
    }

    // =================END LIFECYCLE METHODS ====================

    private void sendLocationOnRegistration() {

        Location network = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location gps = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location toSend = (gps != null) ? gps : (network != null) ? network : null;
        if (toSend != null) {
            sendLocation(toSend);
        }
    }

    private void doFastLocationFix() {
        mGPSSlowMode = false;
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.addGpsStatusListener(mGPSListener);
        // register a mCoarse listener for fast location update
        mLocationManager
                .requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 500, mCoarse);
        // register a mFine listener with fast location update for a fast fix
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mFine);
    }

    private void displayRunningNotification() {
        String contentTitle = getString(R.string.app_name);
        String running = getString(R.string.running);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // instantiate notification
        CharSequence tickerText = contentTitle + " " + running;
        Notification notification = new Notification(R.drawable.icon, tickerText,
                System.currentTimeMillis());
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        // Define the Notification's expanded message and Intent:
        CharSequence contentText = contentTitle + " " + running;
        Intent notificationIntent = new Intent(this, FogOfExplore.class);
        // notificationIntent.setAction("android.intent.action.VIEW");
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
        notificationManager.notify(APPLICATION_ID, notification);
    }

    /**
     * Handler of incoming messages from clients`.
     */
    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    sendLocationOnRegistration();
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_UNREGISTER_INTERFACE:
                    // remove client
                    mClients.remove(msg.replyTo);
                    Log.d(TAG, "Setting the service to off screen state.");
                    moveToOffScreenState();
                    break;

                case MSG_REGISTER_INTERFACE:
                    Log.d(TAG, "Setting the service to on screen state.");
                    //register client
                    mClients.add(msg.replyTo);
                    sendLocationOnRegistration();
                    //get a fast location to display to the user
                    doFastLocationFix();
                    break;
                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i = mClients.size() - 1; i >= 0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null, MSG_SET_VALUE, mValue, 0));
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

        private void moveToOffScreenState() {
            // stop the gps status listener as it is
            // used to always have an update to date location to
            // be displayed to the user
            mLocationManager.removeGpsStatusListener(mGPSListener);
            // stop network and fast GPS updates
            // as they are only useful for informing the user
            mLocationManager.removeUpdates(mCoarse);
            mLocationManager.removeUpdates(mFine);
            // make sure the slow gps update is on
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    (float) LocationOrder.METERS_RADIUS * 2, mGPSSlow);
            mGPSSlowMode = true;
        }
    }

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
