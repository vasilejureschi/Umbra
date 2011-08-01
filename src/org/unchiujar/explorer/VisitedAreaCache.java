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
    private AproximateLocation upperLeftBound;

    /** Lower right bound of cached rectangle area. */
    private AproximateLocation lowerRightBound;
    private Context context;

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
        // inserted in the tree
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

    // TODO crappy code,
    @Override
    public List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation lowerRight) {
        // get from database if the checked bounds are bigger than the cached bounds
//
//        // initialize
//        if (upperLeftBound == null) {
//            upperLeftBound = upperLeft;
//            lowerRightBound = lowerRight;
//            LocationRecorder recorder = LocationRecorder.getInstance(context);
//            locations.addAll(recorder.selectVisited(upperLeft,lowerRight));
//
//        }
//
//        List<AproximateLocation> rectangles = LocationUtilities.complementArea(upperLeftBound, lowerRightBound,
//                upperLeft, lowerRight);
//
//        upperLeftBound = upperLeft;
//        lowerRightBound = lowerRight;
//        // TODO NPE here, see why
//        Log.d(TAG, ""+rectangles.size());
//
//        Log.d(TAG, ""+rectangles);
//        
//        if (rectangles != null && rectangles.size() == 4) {
//            LocationRecorder recorder = LocationRecorder.getInstance(context);
//            locations.addAll(recorder.selectVisited(rectangles.get(0), rectangles.get(1)));
//            locations.addAll(recorder.selectVisited(rectangles.get(2), rectangles.get(3)));
//        }
        Log.d(TAG, "Getting visited points between " + upperLeft + lowerRight);
      LocationRecorder recorder = LocationRecorder.getInstance(context);

        locations.addAll(recorder.selectVisited(upperLeft, lowerRight));
        ArrayList<AproximateLocation> visited = new ArrayList<AproximateLocation>(locations.subSet(upperLeft,
                lowerRight));

        return visited;
    }

}
