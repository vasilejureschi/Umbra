package org.unchiujar.explorer;

import java.util.List;

interface LocationProvider {
    long insert(AproximateLocation location);
    void deleteAll();
    List<AproximateLocation> selectAll();
    /**
     * Returns a list of visited points in the specified area. The coordinate system used
     * is the latitude longitude decimal system. 
     * @param upperLeft the upper left coordinates using the latitude/longitude system 
     * @param bottomRight the bottom right coordinates using the latitude/longitude system
     * @return a List of visited locations
     */
    List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation bottomRight);
}
