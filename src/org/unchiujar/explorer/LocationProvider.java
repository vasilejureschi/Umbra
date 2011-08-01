package org.unchiujar.explorer;

import java.util.List;

interface LocationProvider {
    long insert(AproximateLocation location);
    void deleteAll();
    List<AproximateLocation> selectAll();
    List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation bottomRight);
}
