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
 *        Yen-Liang, Shen - Simplified Chinese and Traditional Chinese translations
 ******************************************************************************/

package org.unchiujar.umbra2.backend;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra2.activities.FogOfExplore;
import org.unchiujar.umbra2.location.ApproximateLocation;
import org.unchiujar.umbra2.location.LocationOrder;
import org.unchiujar.umbra2.services.LocationService;

import java.util.*;

public class VisitedAreaCache implements ExploredProvider {
    /**
     * The interval between database updates.
     */
    private static final long UPDATE_INTERVAL = 10 * 1000;
    /**
     * The maximum number of entries in the cache's TreeSet. UNUSED
     */
    private static final int MAX_CACHE_SIZE = 2000;
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitedAreaCache.class);

    private int mPreviousSize = 0;

    private Timer mUpdateTimer;

    /**
     * Flag signaling that unsaved data has been added to cache.
     */
    private boolean mDirty = false;

    /**
     * Actual Locations mCached.
     */
    private TreeSet<ApproximateLocation> mLocations = new TreeSet<ApproximateLocation>(
            new LocationOrder());

    /**
     * TreeSet used to keep new locations between database updates.
     */
    private SortedSet<ApproximateLocation> mNewLocations = Collections.synchronizedSortedSet(new TreeSet<ApproximateLocation>(
            new LocationOrder()));

    private Context mContext;
    private boolean mCached = false;

    private Intent mLocationServiceIntent = new Intent(FogOfExplore.SERVICE_INTENT_NAME);
    private LocationRecorder recorder;

    public VisitedAreaCache(Context context) {
        super();
        this.mContext = context;
        recorder = new LocationRecorder(context);
        // create database update task to be run
        // at UPDATE_TIME intervals
        TimerTask mUpdateDb = new TimerTask() {

            @Override
            public void run() {
                if (mDirty) {
                    LOGGER.debug("Updating database with  {} new locations ", mNewLocations.size());
                    // TODO lame list creation
                    ArrayList<ApproximateLocation> addedLocations = new ArrayList<ApproximateLocation>();
                    for (ApproximateLocation location : mNewLocations) {
                        recorder.insert(location);
                    }
//                    recorder.insert(addedLocations);

                    LOGGER.debug("Database update completed.");
                    // reset mDirty cache flag
                    mDirty = false;
                    // clear the TreeSet containing mLocations
                    mNewLocations.clear();
                } else {
                    LOGGER.debug("No new location added, no update needed.");
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
            // add the location to the database update tree set
            mNewLocations.add(location);
            LOGGER.debug("Unsaved mLocations: {}", mNewLocations.size());
            mDirty = true;
            mPreviousSize = mLocations.size();
        }
        return mPreviousSize;
    }

    @Override
    public synchronized void insert(List<ApproximateLocation> locations) {
        for (ApproximateLocation location : locations) {
            insert(location);
        }
    }

    @Override
    public List<ApproximateLocation> selectAll() {
        cacheDatabaseInMemory();
        return new ArrayList<ApproximateLocation>(mLocations);
    }

    private void cacheDatabaseInMemory() {
        if (!mCached) {
            LOGGER.debug("Loading all visited points form database...");
            // TODO find a better method
            // cache the entire database
            mLocations.addAll(recorder.selectAll());
            mCached = true;
            LOGGER.debug("Loaded {} points", mLocations.size());

        }
    }

    @Override
    public List<ApproximateLocation> selectVisited(
            ApproximateLocation upperLeft, ApproximateLocation lowerRight) {
        cacheDatabaseInMemory();
        ArrayList<ApproximateLocation> visited = new ArrayList<ApproximateLocation>(
                mLocations.subSet(upperLeft, lowerRight));
        LOGGER.debug("Returning {}  cached results", visited.size());
        return visited;
    }

    /**
     * Messenger for communicating with service.
     */
    Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    private boolean mIsBound;

    /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9000:
                    if (msg.obj != null) {
                        Location location = (Location) msg.obj;
                        LOGGER.debug("Location received", location);

                        if (location.getAccuracy() < LocationOrder.METERS_RADIUS * 5) {
                            // record to database
                            long size = insert(new ApproximateLocation(location));
                            LOGGER.debug("New tree size is {}", size);

                        }

                    } else {
                        LOGGER.debug("@@@@ Null object received");
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
            LOGGER.debug("Client Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            sendMessage(LocationService.MSG_REGISTER_CLIENT);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            LOGGER.debug("Disconnected from location service");
        }
    };

    private void doBindService() {
        mContext.bindService(mLocationServiceIntent, mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        LOGGER.debug("Binding to location service");
    }

    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                sendMessage(LocationService.MSG_UNREGISTER_CLIENT);
            }

            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
            LOGGER.debug("Unbinding cache from location service.");
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
