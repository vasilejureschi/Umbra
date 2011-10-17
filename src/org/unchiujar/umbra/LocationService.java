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

import java.util.ArrayList;

import android.app.IntentService;
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
import android.util.Log;

public class LocationService extends Service implements LocationListener {
    private static final int APPLICATION_ID = 1241241;
    private NotificationManager notificationManager;

    private static final String TAG = LocationService.class.getName();

    public static final String MOVEMENT_UPDATE = "org.unchiujar.umbra.MOVEMENT_UPDATE";

    public static final String LATITUDE = "org.unchiujar.umbra.LocationService.LATITUDE";

    public static final String LONGITUDE = "org.unchiujar.umbra.LocationService.LONGITUDE";
    public static final String ACCURACY = "org.unchiujar.umbra.LocationService.ACCURACY";

    private LocationManager locationManager;
    private LocationProvider locationRecorder = VisitedAreaCache.getInstance(this);


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed: " + location);
        // test if the accuracy is good enough
        if (location.getAccuracy() < LocationOrder.METERS_RADIUS * 2) {
            // record to database
            long size = locationRecorder.insert(new AproximateLocation(location));
            Log.d(TAG, "Tree size is :" + size);
        }

        
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send data as an Integer
                mClients.get(i).send(Message.obtain(null, 9000, location));


            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
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

    @Override
    public void onProviderDisabled(String arg0) {
        Log.d(TAG, "Provider disabled");

    }

    @Override
    public void onProviderEnabled(String arg0) {
        Log.d(TAG, "Provider enabled");

    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        Log.d(TAG, "Status changed");
    }

    // ==================== LIFECYCLE METHODS ====================

    
    @Override
    public void onCreate() {

        super.onCreate();
        // initialize GPS
        enableGPS();
        displayRunningNotification();
        Log.d(TAG, "Location manager set up.");
        // locationRecorder = LocationRecorder.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
//        return super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);        
        notificationManager.cancel(APPLICATION_ID);
        Log.d(TAG, "Service on destroy called.");
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbind called.");
        return super.onUnbind(intent);
    }

    // =================END LIFECYCLE METHODS ====================


    private void enableGPS() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // set up

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                (float) LocationOrder.METERS_RADIUS * 2, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000,
                (float) LocationOrder.METERS_RADIUS * 2, this);

        // set the last known location
        if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
            onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000,
                    (float) LocationOrder.METERS_RADIUS * 2, this);
        }
    }

    private void displayRunningNotification() {
        String contentTitle = getString(R.string.app_name);
        String running = getString(R.string.running);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // instantiate notification
        CharSequence tickerText = contentTitle + " " + running;
        Notification notification = new Notification(R.drawable.icon, tickerText, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        

        // Define the Notification's expanded message and Intent:
        CharSequence contentText = contentTitle + " " + running;
        Intent notificationIntent = new Intent(this, FogOfExplore.class);
        // notificationIntent.setAction("android.intent.action.VIEW");
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
        notificationManager.notify(APPLICATION_ID, notification);
    }
    
         
    
    
    
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;

    /**
     * Handler of incoming messages from clients`.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i=mClients.size()-1; i>=0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, mValue, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
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
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
