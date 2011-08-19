package org.unchiujar.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.content.Context;
import android.util.Log;

public class VisitedAreaCache implements LocationProvider {
    private static final String TAG = VisitedAreaCache.class.getName();
    /** The interval between database updates. */
    private static final long UPDATE_INTERVAL = 30 * 1000;
    /** The maximum number of entries in the cache's TreeSet. UNUSED */
    private static final int MAX_CACHE_SIZE = 2000;

    private int previousSize = 0;

    private TimerTask updateDb;
    private Timer updateTimer;

    /** Flag signaling that unsaved data has been added to cache. */
    private boolean dirty = false;

    /** Actual Locations cached. */
    private TreeSet<AproximateLocation> locations = new TreeSet<AproximateLocation>(new LocationOrder());

    /** TreeSet used to keep new locations between database updates. */
    private TreeSet<AproximateLocation> newLocations = new TreeSet<AproximateLocation>(new LocationOrder());

    /** Upper left bound of cached rectangle area. */
    private AproximateLocation upperLeftBoundCached;

    /** Lower right bound of cached rectangle area. */
    private AproximateLocation lowerRightBoundCached;
    private Context context;
    private boolean cached = false;

    private static VisitedAreaCache instance;

    private VisitedAreaCache(Context context) {
        super();
        this.context = context;
        // create database update task to be run
        // at UPDATE_TIME intervals
        updateDb = new TimerTask() {

            @Override
            public void run() {
                if (dirty) {
                    Log.d(TAG, "Updating database with " + newLocations.size() + " new locations...");
                    LocationRecorder recorder = LocationRecorder.getInstance(VisitedAreaCache.this.context);

                    // TODO lame list creation
                    ArrayList<AproximateLocation> addedLocations = new ArrayList<AproximateLocation>();
                    for (AproximateLocation location : newLocations) {
                        addedLocations.add(location);
                    }
                    recorder.insert(addedLocations);

                    Log.d(TAG, "Database update completed.");
                    // reset dirty cache flag
                    dirty = false;
                    // clear the TreeSet containing locations
                    newLocations.clear();
                } else {
                    Log.d(TAG, "No new location added, no update needed.");
                }
            }

        };
        // create and schedule database updates
        updateTimer = new Timer();
        updateTimer.schedule(updateDb, UPDATE_INTERVAL, UPDATE_INTERVAL);

    }

    public void stopDbUpdate() {
        updateTimer.cancel();
    }

    public static VisitedAreaCache getInstance(Context context) {
        return (instance == null) ? instance = new VisitedAreaCache(context) : instance;
    }

    @Override
    public void deleteAll() {
        locations.clear();

    }

    @Override
    public synchronized long insert(AproximateLocation location) {
        locations.add(location);
        // set dirty cache flag if an actual location was
        // inserted in the tree, checks by tree size
        if (locations.size() != previousSize) {
            // add the location to the database update treeset
            newLocations.add(location);
            dirty = true;
            previousSize = locations.size();
        }
        return previousSize;
    }

    @Override
    public List<AproximateLocation> selectAll() {
        return new ArrayList<AproximateLocation>(locations);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.unchiujar.explorer.LocationProvider#selectVisited(org.unchiujar.explorer.AproximateLocation
     * , org.unchiujar.explorer.AproximateLocation)
     */
    @Override
    public List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation lowerRight) {
        if (!cached) {
            // TODO find a better method
            // cache the entire database
            LocationRecorder recorder = LocationRecorder.getInstance(context);
            locations.addAll(recorder.selectAll());
            cached = true;

        }
        ArrayList<AproximateLocation> visited = new ArrayList<AproximateLocation>(locations.subSet(upperLeft,
                lowerRight));
        Log.d(TAG, "Returning  " + visited.size() + "  cached results");
        return visited;
    }

}
