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

import static org.unchiujar.umbra.LocationUtilities.coordinatesToGeoPoint;
import static org.unchiujar.umbra.LocationUtilities.coordinatesToLocation;
import static org.unchiujar.umbra.LogUtilities.numberLogList;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class FogOfExplore extends MapActivity {
    private static final String TAG = FogOfExplore.class.getName();
    /** Interval between zoom checks for the zoom and pan handler. */
    public static final int ZOOM_CHECKING_DELAY = 500;
    private static final int DIALOG_START_GPS = 0;
    private static final int DIALOG_START_NET = 1;

    private Intent locationServiceIntent;
    private ExploredOverlay explored;
    private LocationChangeReceiver locationChangeReceiver;
    private MapController mapController;
    LocationProvider recorder = VisitedAreaCache.getInstance(this);
    private double currentLat;
    private double currentLong;
    private boolean visible = true;
    public double currentAccuracy;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.where_am_i:
            Log.d(TAG, "Moving to current location...");
            mapController.setCenter(coordinatesToGeoPoint(currentLat, currentLong));
            redrawOverlay();
            return true;
        case R.id.help:
            Log.d(TAG, "Showing help...");
            Intent helpIntent = new Intent(this, Help.class);
            startActivity(helpIntent);
            return true;
        case R.id.exit:
            Log.d(TAG, "Exit requested...");
            finish();
            return true;
        case R.id.settings:
            Intent settingsIntent = new Intent(this, Settings.class);
            startActivity(settingsIntent);
            return true;            
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void redrawOverlay() {
        if (!visible) {
            return;
        }
        // LocationRecorder recorder = LocationRecorder
        // .getInstance(getApplicationContext());

        final MapView mapView = (MapView) findViewById(R.id.mapview);
        int halfLatSpan = mapView.getLatitudeSpan() / 2;
        int halfLongSpan = mapView.getLongitudeSpan() / 2;
        int mapCenterLat = mapView.getMapCenter().getLatitudeE6();
        int mapCenterLong = mapView.getMapCenter().getLongitudeE6();

        AproximateLocation upperLeft = coordinatesToLocation(mapCenterLat + halfLatSpan, mapCenterLong
                - halfLongSpan);
        AproximateLocation bottomRight = coordinatesToLocation(mapCenterLat - halfLatSpan, mapCenterLong
                + halfLongSpan);
        // TODO - optimization get points for rectangle only if a zoomout
        // or a pan action occured - ie new points come into view

        Log.d(TAG,
                "Getting points for rectangle:  "
                        + numberLogList(upperLeft.getLatitude(), upperLeft.getLongitude())
                        + numberLogList(bottomRight.getLatitude(), bottomRight.getLongitude()));
        explored.setCurrent(currentLat, currentLong, currentAccuracy);
        explored.setExplored(recorder.selectVisited(upperLeft, bottomRight));

        mapView.postInvalidate();

    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_NEVER);
        mapView.setBackgroundColor(Color.RED);
        // add overlay to the list of overlays
        explored = new ExploredOverlay(this);

        List<Overlay> listOfOverlays = mapView.getOverlays();
        // listOfOverlays.clear();
        // MyLocationOverlay myLocation = new MyLocationOverlay(getApplicationContext(), mapView);
        // myLocation.enableCompass();
        // myLocation.enableMyLocation();
        // listOfOverlays.add(myLocation);
        listOfOverlays.add(explored);
        mapController = mapView.getController();
        // set city level zoom
        mapController.setZoom(17);
        redrawOverlay();
        Log.d(TAG, "onCreate completed: Activity created");
    }

    @Override
    protected void onStart() {
        visible = true;
        super.onStart();
        Log.d(TAG, "onStart completed: Activity started");
    }

    @Override
    protected void onResume() {
        IntentFilter movementFilter;
        movementFilter = new IntentFilter(LocationService.MOVEMENT_UPDATE);
        locationChangeReceiver = new LocationChangeReceiver();
        registerReceiver(locationChangeReceiver, movementFilter);
        // register zoom && pan handler
        handler.postDelayed(zoomChecker, ZOOM_CHECKING_DELAY);
        startLocationService();
        super.onResume();
        visible = true;
        Log.d(TAG, "onResume completed.");
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(zoomChecker);
        visible = false;
        super.onPause();
        Log.d(TAG, "onPause completed.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop completed.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart completed.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(zoomChecker);
        unbindService(mConnection);
        stopService(locationServiceIntent);
        unregisterReceiver(locationChangeReceiver);
        VisitedAreaCache.getInstance(this).stopDbUpdate();
        Log.d(TAG, "onDestroy completed.");
    }

    // =================END LIFECYCLE METHODS ====================

    private void startLocationService() {

        boolean isGPS = ((LocationManager) getSystemService(LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGPS) {
            showDialog(DIALOG_START_GPS);
        }
        displayConnectivityWarning();
        // bind to location service
        locationServiceIntent = new Intent(this, LocationService.class);
        bindService(locationServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Log.d(TAG, "Showing dialog with id " + id);
        switch (id) {
        case DIALOG_START_GPS:
            return createGPSDialog();
        case DIALOG_START_NET:
            // TODO internet starting dialog
            break;
        default:
        }
        return null;
    }

    private void displayConnectivityWarning(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = false;
        for (NetworkInfo info : connectivityManager.getAllNetworkInfo()) {
            if (info.getState() == NetworkInfo.State.CONNECTED
                    || info.getState() == NetworkInfo.State.CONNECTING) {
                connected = true;
                break;
            }
        }

        if (!connected) {
            Toast.makeText(getApplicationContext(), R.string.connectivity_warning, Toast.LENGTH_LONG).show();

        }
    }
    private Dialog createGPSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gps_dialog).setCancelable(false)
                .setPositiveButton(R.string.start_gps_btn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        startActivityForResult(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                    }
                }).setNegativeButton(R.string.continue_no_gps, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    public class LocationChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Location change notification received");

            Bundle bundle = intent.getExtras();
            double latitude = (Double) bundle.get(LocationService.LATITUDE);
            double longitude = (Double) bundle.get(LocationService.LONGITUDE);
            float accuracy = (Float) bundle.get(LocationService.ACCURACY);

            Log.d(TAG, "Received point" + numberLogList(latitude, longitude));
            redrawOverlay();
            currentLat = latitude;
            currentLong = longitude;
            currentAccuracy = accuracy;
            // mapController.setCenter(coordinatesToGeoPoint(latitude, longitude));
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connection to the location service established.");
        }

        public void onServiceDisconnected(ComponentName className) {
            // As our service is in the
            // same process, this should
            // never be called
            assert false;
        }
    };

    private Handler handler = new Handler();

    private Runnable zoomChecker = new Runnable() {
        private int oldZoom = 9001;
        private int oldCenterLat = -1;
        private int oldCenterLong = -1;

        public void run() {
            MapView mapView = (MapView) findViewById(R.id.mapview);

            int mapCenterLat = mapView.getMapCenter().getLatitudeE6();
            int mapCenterLong = mapView.getMapCenter().getLongitudeE6();

            if (mapView.getZoomLevel() != oldZoom || oldCenterLat != mapCenterLat
                    || oldCenterLong != mapCenterLong) {
                redrawOverlay();
                oldZoom = mapView.getZoomLevel();
                oldCenterLat = mapCenterLat;
                oldCenterLong = mapCenterLong;
            }
            handler.removeCallbacks(zoomChecker);
            handler.postDelayed(zoomChecker, ZOOM_CHECKING_DELAY);
        }
    };

}
