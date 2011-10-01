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
