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

package org.unchiujar.umbra.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import org.unchiujar.umbra.R;
import org.unchiujar.umbra.backend.ExploredProvider;
import org.unchiujar.umbra.io.GpxImporter;
import org.unchiujar.umbra.location.ApproximateLocation;
import org.unchiujar.umbra.overlays.ExploredOverlay;
import org.unchiujar.umbra.services.LocationService;
import org.unchiujar.umbra.utils.LocationUtilities;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.unchiujar.umbra.utils.LocationUtilities.coordinatesToGeoPoint;
import static org.unchiujar.umbra.utils.LocationUtilities.coordinatesToLocation;
import static org.unchiujar.umbra.utils.LogUtilities.numberLogList;

/**
 * Main activity for Umbra application.
 *
 * @author Vasile Jureschi
 * @see LocationService
 */
public class FogOfExplore extends MapActivity {
    /**
     * Logger tag.
     */
    private static final String TAG = FogOfExplore.class.getName();
    /**
     * Initial map zoom.
     */
    private static final int INITIAL_ZOOM = 17;
    /**
     * Interval between zoom checks for the zoom and pan handler.
     */
    public static final int ZOOM_CHECKING_DELAY = 500;
    /**
     * Constant used for saving the accuracy value between screen rotations.
     */
    private static final String BUNDLE_ACCURACY = "org.unchiujar.umbra.accuracy";
    /**
     * Constant used for saving the latitude value between screen rotations.
     */
    private static final String BUNDLE_LATITUDE = "org.unchiujar.umbra.latitude";
    /**
     * Constant used for saving the longitude value between screen rotations.
     */
    private static final String BUNDLE_LONGITUDE = "org.unchiujar.umbra.longitude";
    /**
     * Constant used for saving the zoom level between screen rotations.
     */
    private static final String BUNDLE_ZOOM = "org.unchiujar.umbra.zoom";

    /**
     * Intent named used for starting the location service
     *
     * @see LocationService
     */
    private static final String SERVICE_INTENT_NAME = "org.com.unchiujar.LocationService";

    /**
     * Dialog displayed while loading the explored points at application start.
     */
    private ProgressDialog mloadProgress;

    /**
     * Location service intent.
     *
     * @see LocationService
     */
    private Intent mLocationServiceIntent;

    /**
     * Map overlay displaying the explored area. Updated on location changed.
     */
    private ExploredOverlay mExplored;

    /**
     * Map controller used for centering the map to the current location.
     */
    private MapController mMapController;
    /**
     * Source for obtaining explored area information.
     */
    private ExploredProvider mRecorder;
    /**
     * Current device latitude. Updated on every location change.
     */
    private double mCurrentLat;
    /**
     * Current device longitude. Updated on every location change.
     */
    private double mCurrentLong;
    /**
     * Current location accuracy . Updated on every location change.
     */
    private double mCurrentAccuracy;

    /**
     * Flag signaling if the application is visible. Used to stop overlay updates if the map is
     * currently not visible.
     */
    private boolean mVisible = true;
    /**
     * Flag signaling if the user is walking or driving. It is passed to the location service in
     * order to change location update frequency.
     *
     * @see LocationService
     */
    private boolean mDrive;

    /**
     * Handler used to update the overlay if a pan or zoom action occurs.
     */
    private Handler mZoomPanHandler = new Handler();

    /**
     * Messenger for communicating with service.
     */
    private Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    private boolean mIsBound;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private SharedPreferences mSettings;

    /**
     * Runnable to be executed by the pan and zoom handler.
     */
    private Runnable mZoomChecker = new Runnable() {
        private int oldZoom = -1;
        private int oldCenterLat = -1;
        private int oldCenterLong = -1;

        @Override
        public void run() {
            MapView mapView = (MapView) findViewById(R.id.mapview);

            int mapCenterLat = mapView.getMapCenter().getLatitudeE6();
            int mapCenterLong = mapView.getMapCenter().getLongitudeE6();
            // check if the zoom or pan has changed and update accordingly
            if (mapView.getZoomLevel() != oldZoom
                    || oldCenterLat != mapCenterLat
                    || oldCenterLong != mapCenterLong) {
                redrawOverlay();
                oldZoom = mapView.getZoomLevel();
                oldCenterLat = mapCenterLat;
                oldCenterLong = mapCenterLong;
            }
            // start a new cycle of checks
            mZoomPanHandler.removeCallbacks(mZoomChecker);
            mZoomPanHandler.postDelayed(mZoomChecker, ZOOM_CHECKING_DELAY);
        }
    };

    /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationService.MSG_LOCATION_CHANGED:
                    if (msg.obj != null) {
                        Log.d(TAG, ((Location) msg.obj).toString());

                        mCurrentLat = ((Location) msg.obj).getLatitude();
                        mCurrentLong = ((Location) msg.obj).getLongitude();
                        mCurrentAccuracy = ((Location) msg.obj).getAccuracy();
                        redrawOverlay();

                    } else {
                        Log.d(TAG, "Null object received");
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            Log.d(TAG, "Location service attached.");
            // register client
            sendMessage(LocationService.MSG_REGISTER_CLIENT);
            // register interface
            sendMessage(LocationService.MSG_REGISTER_INTERFACE);

            // send walk or drive mode
            sendMessage(mDrive ? LocationService.MSG_DRIVE
                    : LocationService.MSG_WALK);
        }

        /*
         * (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content
         * .ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service has been
            // unexpectedly disconnected / process crashed.
            mService = null;
            Log.d(TAG, "Disconnected from location service");
        }
    };

    /**
     * Drive or walk preference listener. A listener is necessary for this option as the location
     * service needs to be notified of the change in order to change location update frequency. The
     * preference is sent when the activity comes into view and rebinds to the location service.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "Settings changed :" + sharedPreferences + " " + key);
            mDrive = mSettings.getBoolean(Preferences.DRIVE_MODE, false);
        }
    };

    // ==================== LIFECYCLE METHODS ====================

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        mloadProgress = ProgressDialog.show(this, "",
                "Loading. Please wait...", true);
        mSettings.registerOnSharedPreferenceChangeListener(mPrefListener);
        setContentView(R.layout.main);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_NEVER);
        mapView.setBackgroundColor(Color.RED);
        // add overlay to the list of overlays
        mExplored = new ExploredOverlay(this);

        List<Overlay> listOfOverlays = mapView.getOverlays();
        // listOfOverlays.clear();
        // MyLocationOverlay myLocation = new
        // MyLocationOverlay(getApplicationContext(), mapView);
        // myLocation.enableCompass();
        // myLocation.enableMyLocation();
        // listOfOverlays.add(myLocation);
        listOfOverlays.add(mExplored);
        mMapController = mapView.getController();
        // set city level zoom
        mMapController.setZoom(INITIAL_ZOOM);
        Log.d(TAG, "onCreate completed: Activity created");
        mLocationServiceIntent = new Intent(SERVICE_INTENT_NAME);
        startService(mLocationServiceIntent);
        mRecorder = ((UmbraApplication) getApplication()).getCache();
        loadFileFromIntent();
        // check we still have access to GPS info
        checkConnectivity();

    }

    /**
     * Loads a gpx data from a file path send through an intent.
     */
    private void loadFileFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {

            Uri data = intent.getData();

            if (data != null) {

                final String filePath = data.getEncodedPath();

                final ProgressDialog progress = new ProgressDialog(this);

                progress.setCancelable(false);
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setMessage(getString(R.string.importing_locations));
                progress.show();

                Runnable importer = new Runnable() {

                    @Override
                    public void run() {
                        ExploredProvider cache = ((UmbraApplication) getApplication())
                                .getCache();

                        try {
                            GpxImporter.importGPXFile(new FileInputStream(
                                    new File(filePath)), cache);
                        } catch (ParserConfigurationException e) {
                            Log.e(TAG, "Error parsing file", e);
                        } catch (SAXException e) {
                            Log.e(TAG, "Error parsing file", e);
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading file", e);
                        }

                        progress.dismiss();
                    }
                };
                new Thread(importer).start();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // restore accuracy and coordinates from saved state
        mCurrentAccuracy = savedInstanceState.getDouble(BUNDLE_ACCURACY);
        mCurrentLat = savedInstanceState.getDouble(BUNDLE_LATITUDE);
        mCurrentLong = savedInstanceState.getDouble(BUNDLE_LONGITUDE);
        mMapController.setZoom(savedInstanceState.getInt(BUNDLE_ZOOM));
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // save accuracy and coordinates
        outState.putDouble(BUNDLE_ACCURACY, mCurrentAccuracy);
        outState.putDouble(BUNDLE_LATITUDE, mCurrentLat);
        outState.putDouble(BUNDLE_LONGITUDE, mCurrentLong);
        MapView mapView = (MapView) findViewById(R.id.mapview);
        outState.putInt(BUNDLE_ZOOM, mapView.getZoomLevel());
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart completed: Activity started");
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        // register zoom && pan mZoomPanHandler
        mZoomPanHandler.postDelayed(mZoomChecker, ZOOM_CHECKING_DELAY);
        // set the visibility flag to start overlay updates
        mVisible = true;
        redrawOverlay();
        mloadProgress.cancel();
        Log.d(TAG, "onResume completed.");
        // bind to location service
        doBindService();

    }

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        mZoomPanHandler.removeCallbacks(mZoomChecker);
        mVisible = false;
        // unbind from service as the activity does
        // not display location info (is hidden or stopped)
        doUnbindService();
        Log.d(TAG, "onPause completed.");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop completed.");
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onRestart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart completed.");
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mZoomPanHandler.removeCallbacks(mZoomChecker);
        Log.d(TAG, "onDestroy completed.");
    }

    //    // ================= END LIFECYCLE METHODS ====================

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.where_am_i).setIcon(
                android.R.drawable.ic_menu_mylocation);
        menu.findItem(R.id.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.findItem(R.id.help).setIcon(android.R.drawable.ic_menu_help);
        menu.findItem(R.id.exit).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);

        return result;
    }


    /*
* (non-Javadoc)
* @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.where_am_i:
                Log.d(TAG, "Moving to current location...");
                mMapController.setCenter(coordinatesToGeoPoint(mCurrentLat,
                        mCurrentLong));
                redrawOverlay();
                return true;
            case R.id.help:
                Log.d(TAG, "Showing help...");
                Intent helpIntent = new Intent(this, Help.class);
                startActivity(helpIntent);
                return true;
            case R.id.exit:
                Log.d(TAG, "Exit requested...");
                doUnbindService();
                // cleanup
                stopService(mLocationServiceIntent);
                mRecorder.destroy();
                finish();
                return true;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, Preferences.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the current location and calls an overlay redraw.
     */
    private void redrawOverlay() {
        // FIXME hack
        // don't do anything if the activity is not visible
        if (!mVisible) {
            return;
        }

        // get the coordinates of the visible area
        final MapView mapView = (MapView) findViewById(R.id.mapview);
        final int halfLatSpan = mapView.getLatitudeSpan() / 2;
        final int halfLongSpan = mapView.getLongitudeSpan() / 2;
        final int mapCenterLat = mapView.getMapCenter().getLatitudeE6();
        final int mapCenterLong = mapView.getMapCenter().getLongitudeE6();

        final ApproximateLocation upperLeft = coordinatesToLocation(
                mapCenterLat + halfLatSpan, mapCenterLong - halfLongSpan);
        final ApproximateLocation bottomRight = coordinatesToLocation(
                mapCenterLat - halfLatSpan, mapCenterLong + halfLongSpan);
        // TODO - optimization get points for rectangle only if a zoomout
        // or a pan action occured - ie new points come into view

        Log.d(TAG,
                "Getting points for rectangle:  "
                        + numberLogList(upperLeft.getLatitude(),
                        upperLeft.getLongitude())
                        + numberLogList(bottomRight.getLatitude(),
                        bottomRight.getLongitude())
        );
        // update the current location for the overlay
        mExplored.setCurrent(mCurrentLat, mCurrentLong, mCurrentAccuracy);
        // update the overlay with the currently visible explored area
        mExplored.setExplored(mRecorder.selectVisited(upperLeft, bottomRight));
        // animate the map to the user position if the options to do so is
        // selected
        if (mSettings.getBoolean(Preferences.ANIMATE, false)) {
            mMapController.animateTo(LocationUtilities.coordinatesToGeoPoint(
                    mCurrentLat, mCurrentLong));
        }
        // call overlay redraw
        mapView.postInvalidate();

    }

    /**
     * Checks GPS and network connectivity. Displays a dialog asking the user to start the GPS if
     * not started and also displays a toast warning it no network connectivity is available.
     */
    private void checkConnectivity() {

        boolean isGPS = ((LocationManager) getSystemService(LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGPS) {
            createGPSDialog().show();
        }
        displayConnectivityWarning();
    }

    /**
     * Displays a toast warning if no network is available.
     */
    private void displayConnectivityWarning() {
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
            Toast.makeText(getApplicationContext(),
                    R.string.connectivity_warning, Toast.LENGTH_LONG).show();

        }
    }

    /**
     * Creates the GPS dialog displayed if the GPS is not started.
     *
     * @return the GPS Dialog
     */
    private Dialog createGPSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gps_dialog).setCancelable(false);

        final AlertDialog alert = builder.create();

        alert.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.start_gps_btn),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        alert.dismiss();
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }
        );

        alert.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.continue_no_gps),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        alert.dismiss();
                    }
                }
        );
        return alert;
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.maps.MapActivity#isRouteDisplayed()
     */
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * Binds to the location service. Called when the activity becomes visible.
     */
    private void doBindService() {
        bindService(mLocationServiceIntent, mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "Binding to location service");
    }

    /**
     * Unbinds from the location service. Called when the activity is stopped or closed.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // test if we have a valid service registration
            if (mService != null) {
                sendMessage(LocationService.MSG_UNREGISTER_INTERFACE);
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d(TAG, "Unbinding map from location service.");
        }
    }

    private void sendMessage(int message) {
        // TODO check message
        try {
            Message msg = Message.obtain(null, message);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            // NO-OP
            // Nothing special to do if the service
            // has crashed.
        }

    }

}
