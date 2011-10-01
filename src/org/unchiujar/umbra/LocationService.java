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

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationService extends IntentService implements LocationListener {
    private static final String TAG = LocationService.class.getName();

    public static final String MOVEMENT_UPDATE = "org.unchiujar.umbra.MOVEMENT_UPDATE";

    public static final String LATITUDE = "org.unchiujar.umbra.LocationService.LATITUDE";

    public static final String LONGITUDE = "org.unchiujar.umbra.LocationService.LONGITUDE";
    public static final String ACCURACY = "org.unchiujar.umbra.LocationService.ACCURACY";

    
    private LocationManager locationManager;
    private LocationProvider locationRecorder = VisitedAreaCache.getInstance(this);

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public LocationService() {
	super("LocationService");

    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns,
     * IntentService stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
	Log.d(TAG, "On handle intent");
    }

    @Override
    public void onLocationChanged(Location location) {
	Log.d(TAG, "Location changed: " + location);
	// record to database
	long size = locationRecorder.insert(new AproximateLocation(location));
	
	
	Log.d(TAG, "Tree size is :" + size);
	// update display
	Intent intent = new Intent(MOVEMENT_UPDATE);
	intent.putExtra(LATITUDE,location.getLatitude());
	intent.putExtra(LONGITUDE,location.getLongitude());
	intent.putExtra(ACCURACY, location.getAccuracy());
	sendBroadcast(intent);

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

    @Override
    public void onCreate() {

	super.onCreate();
	// initiralize GPS
	locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	// set up
	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		5000, (float)LocationOrder.METERS_RADIUS * 2, this);
	Log.d(TAG, "Location manager set up.");
//	locationRecorder = LocationRecorder.getInstance(this);
    }

}
