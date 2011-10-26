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

package org.unchiujar.umbra.backend;

import org.unchiujar.umbra.location.ApproximateLocation;
import org.unchiujar.umbra.location.LocationOrder;
import org.unchiujar.umbra.services.LocationService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public class VisitedAreaCache implements LocationProvider {
    private static final String TAG = VisitedAreaCache.class.getName();
    /** The interval between database updates. */
    private static final long UPDATE_INTERVAL = 30 * 1000;
    /** The maximum number of entries in the cache's TreeSet. UNUSED */
    private static final int MAX_CACHE_SIZE = 2000;

    private int mPreviousSize = 0;

    private TimerTask mUpdateDb;
    private Timer mUpdateTimer;

    /** Flag signaling that unsaved data has been added to cache. */
    private boolean mDirty = false;

    /** Actual Locations mCached. */
    private TreeSet<ApproximateLocation> mLocations = new TreeSet<ApproximateLocation>(
            new LocationOrder());

    /** TreeSet used to keep new locations between database updates. */
    private TreeSet<ApproximateLocation> mNewLocations = new TreeSet<ApproximateLocation>(
            new LocationOrder());

    /** Upper left bound of mCached rectangle area. */
    private ApproximateLocation mUpperLeftBoundCached;

    /** Lower right bound of mCached rectangle area. */
    private ApproximateLocation mLowerRightBoundCached;
    private Context mContext;
    private boolean mCached = false;

    private static VisitedAreaCache mInstance;

    private Intent mLocationServiceIntent = new Intent("org.com.unchiujar.LocationService");

    private VisitedAreaCache(Context context) {
        super();
        this.mContext = context;
        // create database update task to be run
        // at UPDATE_TIME intervals
        mUpdateDb = new TimerTask() {

            @Override
            public void run() {
                if (mDirty) {
                    Log.d(TAG, "Updating database with " + mNewLocations.size()
                            + " new mLocations...");
                    LocationRecorder recorder = LocationRecorder
                            .getInstance(VisitedAreaCache.this.mContext);

                    // TODO lame list creation
                    ArrayList<ApproximateLocation> addedLocations = new ArrayList<ApproximateLocation>();
                    for (ApproximateLocation location : mNewLocations) {
                        addedLocations.add(location);
                    }
                    recorder.insert(addedLocations);

                    Log.d(TAG, "Database update completed.");
                    // reset mDirty cache flag
                    mDirty = false;
                    // clear the TreeSet containing mLocations
                    mNewLocations.clear();
                } else {
                    Log.d(TAG, "No new location added, no update needed.");
                }
            }

        };
        // create and schedule database updates
        mUpdateTimer = new Timer();
        mUpdateTimer.schedule(mUpdateDb, UPDATE_INTERVAL, UPDATE_INTERVAL);

        doBindService();
    }

    public void destroy() {
        mUpdateTimer.cancel();
        doUnbindService();
    }

    public static VisitedAreaCache getInstance(Context context) {
        return (mInstance == null) ? mInstance = new VisitedAreaCache(context) : mInstance;
    }

    @Override
    public void deleteAll() {
        mLocations.clear();

    }

    @Override
    public synchronized long insert(ApproximateLocation location) {
        mLocations.add(location);
        // set mDirty cache flag if an actual location was
        // inserted in the tree, checks by tree size
        if (mLocations.size() != mPreviousSize) {
            // add the location to the database update treeset
            mNewLocations.add(location);
            Log.d(TAG, "Unsaved mLocations: " + mNewLocations.size());
            mDirty = true;
            mPreviousSize = mLocations.size();
        }
        return mPreviousSize;
    }

    @Override
    public List<ApproximateLocation> selectAll() {
        return new ArrayList<ApproximateLocation>(mLocations);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.unchiujar.umbra.LocationProvider#selectVisited(org.unchiujar.umbra
     * .ApproximateLocation , org.unchiujar.umbra.ApproximateLocation)
     */
    @Override
    public List<ApproximateLocation> selectVisited(ApproximateLocation upperLeft,
            ApproximateLocation lowerRight) {
        if (!mCached) {
            Log.d(TAG, "Loading all visited points form database...");
            // TODO find a better method
            // cache the entire database
            LocationRecorder recorder = LocationRecorder.getInstance(mContext);
            mLocations.addAll(recorder.selectAll());
            mCached = true;
            Log.d(TAG, "Loaded " + mLocations.size() + " points.");

        }
        ArrayList<ApproximateLocation> visited = new ArrayList<ApproximateLocation>(
                mLocations.subSet(upperLeft,
                        lowerRight));
        Log.d(TAG, "Returning  " + visited.size() + "  mCached results");
        return visited;
    }

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    private boolean mIsBound;

    /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationService.MSG_SET_VALUE:
                    Log.d(TAG, "Received from service: " + msg.arg1);
                    break;
                case 9000:
                    if (msg.obj != null) {
                        Location location = (Location) msg.obj;
                        Log.d(TAG, location.toString());

                        if (location.getAccuracy() < LocationOrder.METERS_RADIUS * 2) {
                            // record to database
                            long size = insert(new ApproximateLocation(location));
                            Log.d(TAG, "New tree size is :" + size);

                        }

                    } else {
                        Log.d(TAG, "@@@@ Null object received");
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
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            Log.d(TAG, "Client Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, LocationService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null, LocationService.MSG_SET_VALUE, this.hashCode(), 0);
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.d(TAG, "Disconnected from location service");
        }
    };

    private void doBindService() {
        mContext.bindService(mLocationServiceIntent, mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "Binding to location service");
    }

    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, LocationService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
            Log.d(TAG, "Unbinding from location service.");
        }
    }

}
