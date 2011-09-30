package org.unchiujar.explorer;

import static org.unchiujar.explorer.LocationUtilities.coordinatesToGeoPoint;
import static org.unchiujar.explorer.LocationUtilities.coordinatesToLocation;
import static org.unchiujar.explorer.LogUtilities.numberLogList;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class FogOfExplore extends MapActivity {
    private static final String TAG = FogOfExplore.class.getName();
    /** Interval between zoom checks for the zoom and pan handler. */
    public static final int ZOOM_CHECKING_DELAY = 500;

    private Intent locationServiceIntent;
    private ExploredOverlay explored;
    private LocationChangeReceiver locationChangeReceiver;
    private MapController mapController;
    LocationProvider recorder = VisitedAreaCache.getInstance(this);
    private double currentLat;
    private double currentLong;
    private boolean visible = true;

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
            return true;
        case R.id.exit:
            Log.d(TAG, "Exit requested...");
            finish();
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

        Log.d(TAG, "Getting points for rectangle:  "
                + numberLogList(upperLeft.getLatitude(), upperLeft.getLongitude())
                + numberLogList(bottomRight.getLatitude(), bottomRight.getLongitude()));
        explored.setCurrent(currentLat, currentLong);
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
        startLocationService();
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

        displayRunningNotification();

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
        mNotificationManager.cancelAll();
        handler.removeCallbacks(zoomChecker);
        unbindService(mConnection);
        stopService(locationServiceIntent);
        unregisterReceiver(locationChangeReceiver);
        VisitedAreaCache.getInstance(this).stopDbUpdate();
        Log.d(TAG, "onDestroy completed.");
    }

    // =================END LIFECYCLE METHODS ====================

    private void startLocationService() {
        // bind to location service
        locationServiceIntent = new Intent(this, LocationService.class);
        bindService(locationServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

    }

    public class LocationChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Location change notification received");

            IntentFilter movementFilter;
            Bundle bundle = intent.getExtras();
            double latitude = (Double) bundle.get(LocationService.LATITUDE);
            double longitude = (Double) bundle.get(LocationService.LONGITUDE);

            Log.d(TAG, "Received point" + numberLogList(latitude, longitude));
            redrawOverlay();
            currentLat = latitude;
            currentLong = longitude;
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

    private void displayRunningNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // instantiate notification
        int icon = R.drawable.icon;
        CharSequence tickerText = "Explorer is running, tap to display.";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags |= Notification.FLAG_NO_CLEAR;
        // Define the Notification's expanded message and Intent:
        Context context = getApplicationContext();
        CharSequence contentTitle = "Explorer";
        CharSequence contentText = "Explorer is running, tap to display.";
        Intent notificationIntent = new Intent(this, FogOfExplore.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        mNotificationManager.notify(13234, notification);

    }
    



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
 
    private NotificationManager mNotificationManager;
}