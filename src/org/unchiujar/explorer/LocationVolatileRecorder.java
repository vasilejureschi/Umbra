package org.unchiujar.explorer;

import java.util.ArrayList;
import java.util.List;

public class LocationVolatileRecorder implements LocationProvider {

    private List<AproximateLocation> locations = new ArrayList<AproximateLocation>();
    
    private static LocationVolatileRecorder instance;

    private LocationVolatileRecorder() {        
        super();
    }

    public static LocationVolatileRecorder getInstance() {
        return (instance == null) ? instance = new LocationVolatileRecorder() : instance;
    }
    @Override
    public void deleteAll() {
        locations.clear();

    }

    @Override
    public synchronized long insert(AproximateLocation location) {
        locations.add(location);
        return locations.size();
    }

    @Override
    public List<AproximateLocation> selectAll() {
        return locations;
    }

    @Override
    public List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation bottomRight) {
        ArrayList<AproximateLocation> visited = new ArrayList<AproximateLocation>();
        for (AproximateLocation location : locations) {
            if (location.getLatitude() >= upperLeft.getLatitude() &&
                    location.getLatitude() <= bottomRight.getLatitude() &&
                    location.getLongitude() >= upperLeft.getLongitude() &&
                    location.getLongitude() <= bottomRight.getLongitude()) {
                visited.add(location);
            }
        }
        return visited;
    }

}
