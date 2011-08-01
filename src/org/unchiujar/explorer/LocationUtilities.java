package org.unchiujar.explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.location.Location;

import com.google.android.maps.GeoPoint;

/**
 * Utility class used for transforming between various representations for locations.
 * 
 * @author vasile
 * 
 */
public class LocationUtilities {

    public static GeoPoint locationToGeoPoint(Location location) {
        return coordinatesToGeoPoint(location.getLatitude(), location.getLongitude());
    }

    public static AproximateLocation geoPointToLocation(GeoPoint geoPoint) {
        return coordinatesToLocation(geoPoint.getLatitudeE6() / 1e6, geoPoint.getLongitudeE6() / 1e6);
    }

    /**
     * @param latitude
     *            in decimal degrees
     * @param longitude
     *            in decimal degrees
     * @return a GeoPoint with the coordinates
     */
    public static GeoPoint coordinatesToGeoPoint(double latitude, double longitude) {
        return new GeoPoint(new Double(latitude * 1e6).intValue(), new Double(longitude * 1e6).intValue());
    }

    /**
     * @param latitude
     *            in decimal degrees
     * @param longitude
     *            in decimal degrees
     * @return a AproximateLocation with the coordinates
     */
    public static AproximateLocation coordinatesToLocation(double latitude, double longitude) {
        AproximateLocation location = new AproximateLocation("Translator");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    /**
     * @param latitudeE6
     *            in microdegrees
     * @param longitudeE6
     *            in microdegrees
     * @return a AproximateLocation with the coordinates
     */
    public static AproximateLocation coordinatesToLocation(int latitudeE6, int longitudeE6) {
        AproximateLocation location = new AproximateLocation("Translator");
        location.setLatitude(latitudeE6 / 1e6);
        location.setLongitude(longitudeE6 / 1e6);
        return location;

    }

    // TODO: lame code calculations following, improve
    // TODO: write tests
    // TODO: zoom case is not correctly checked, rectangle A included in B
    public static List<AproximateLocation> complementArea(Location upperLeftA, Location lowerRightA, Location upperLeftB,
            Location lowerRightB) {
        // north is +, south is -
        // west is - , east is +
        // yay for inventing words :D
        double northestLatitude = 0;
        double westestLongitude = 0;
        double southestLatitude = 0;
        double eastestLongitude = 0;
        // calculate upper left bounds
        // largest latitude
        northestLatitude = (upperLeftA.getLatitude() > upperLeftB.getLatitude()) ? upperLeftA.getLatitude()
                : upperLeftB.getLatitude();
        // smallest longitude
        westestLongitude = (upperLeftA.getLongitude() < upperLeftB.getLongitude()) ? upperLeftA
                .getLongitude() : upperLeftB.getLongitude();

        // calculate lower right bounds
        // smallest latitude
        southestLatitude = (lowerRightA.getLatitude() < lowerRightB.getLatitude()) ? lowerRightA
                .getLatitude() : lowerRightB.getLatitude();
        // largest longitude
        eastestLongitude = (lowerRightA.getLongitude() > lowerRightB.getLongitude()) ? lowerRightA
                .getLongitude() : lowerRightB.getLongitude();
                
        // if B is included in A return an empty array        
        if (northestLatitude == upperLeftA.getLatitude() && 
                westestLongitude == upperLeftA.getLongitude() &&
                southestLatitude == lowerRightA.getLatitude() &&
                eastestLongitude == lowerRightA.getLongitude()) {
            return Collections.emptyList();
        }
        AproximateLocation rect1UL = new AproximateLocation("mumu");
        AproximateLocation rect1BR = new AproximateLocation("mumu");

        AproximateLocation rect2UL = new AproximateLocation("mumu");
        AproximateLocation rect2BR = new AproximateLocation("mumu");

        // The coordinates for the dotted areas are to be calculated.
        // The same calculations apply if the rectangles do not overlap
        /**
         * configuration 1:
         * 
         * <pre>
         *   _____
         *  |..B. |..
         *  |..------
         *  |__|__| |
         *  ***| A  |
         *  ***|____|
         * 
         * </pre>
         * 
         * 
         */
        if (northestLatitude > upperLeftA.getLatitude() && westestLongitude < upperLeftA.getLongitude()) {
            rect1UL.setLatitude(northestLatitude);
            rect1UL.setLongitude(westestLongitude);

            rect1BR.setLatitude(upperLeftA.getLatitude());
            rect1BR.setLongitude(lowerRightA.getLongitude());

            rect2UL.setLatitude(upperLeftA.getLatitude());
            rect2UL.setLongitude(westestLongitude);

            rect2BR.setLatitude(lowerRightA.getLatitude());
            rect2BR.setLongitude(upperLeftA.getLongitude());
            return createList(rect1UL, rect1BR, rect2UL, rect2BR);

        }

        /**
         * configuration 2:
         * 
         * <pre>
         *         _____
         *     ...|..B. |  
         *     ------***|
         *     |  | |***|
         *     | A _|___|
         *     |____|****
         * 
         * </pre>
         * 
         * 
         */

        if (northestLatitude > upperLeftA.getLatitude() && eastestLongitude > lowerRightA.getLongitude()) {

            rect1UL.setLatitude(northestLatitude);
            rect1UL.setLongitude(upperLeftA.getLongitude());

            rect1BR.setLatitude(upperLeftA.getLatitude());
            rect1BR.setLongitude(eastestLongitude);

            rect2UL.setLatitude(upperLeftA.getLatitude());
            rect2UL.setLongitude(lowerRightA.getLongitude());

            rect2BR.setLatitude(southestLatitude);
            rect2BR.setLongitude(eastestLongitude);
            return createList(rect1UL, rect1BR, rect2UL, rect2BR);

        }

        /**
         * configuration 3:
         * 
         * <pre>
         *   _____
         *  |  A  |..
         *  |  ------
         *  |__|__|.|
         *  ***|*B .|
         *  ***|____|
         * 
         * </pre>
         * 
         * 
         */
        if (eastestLongitude > lowerRightA.getLongitude() && southestLatitude > lowerRightA.getLatitude()) {

            rect1UL.setLatitude(upperLeftA.getLatitude());
            rect1UL.setLongitude(lowerRightA.getLongitude());

            rect1BR.setLatitude(southestLatitude);
            rect1BR.setLongitude(eastestLongitude);

            rect2UL.setLatitude(lowerRightA.getLatitude());
            rect2UL.setLongitude(upperLeftA.getLongitude());

            rect2BR.setLatitude(southestLatitude);
            rect2BR.setLongitude(lowerRightA.getLongitude());
            return createList(rect1UL, rect1BR, rect2UL, rect2BR);

        }

        /**
         * configuration 4:
         * 
         * <pre>
         *         _____
         *     ...|  A  |  
         *     ------   |
         *     |..| |   |
         *     |.B _|___|
         *     |...*|***
         * 
         * </pre>
         * 
         * 
         */

        if (southestLatitude < lowerRightA.getLatitude() && westestLongitude > upperLeftA.getLongitude()) {

            rect1UL.setLatitude(upperLeftA.getLatitude());
            rect1UL.setLongitude(westestLongitude);

            rect1BR.setLatitude(southestLatitude);
            rect1BR.setLongitude(upperLeftA.getLongitude());

            rect2UL.setLatitude(lowerRightA.getLatitude());
            rect2UL.setLongitude(upperLeftA.getLongitude());

            rect2BR.setLatitude(southestLatitude);
            rect2BR.setLongitude(lowerRightA.getLongitude());
            return createList(rect1UL, rect1BR, rect2UL, rect2BR);
        }
        assert false : "Unexpected rectangle values were passed : " + upperLeftA + " " + lowerRightA + " "
                + upperLeftB + " " + lowerRightB;

        return Collections.emptyList();
    }
    
    private  static List<AproximateLocation> createList(AproximateLocation... locations){
        ArrayList<AproximateLocation> rectangles = new ArrayList<AproximateLocation>();
        for (AproximateLocation aproximateLocation : locations) {
            rectangles.add(aproximateLocation);
        }
        return rectangles;
        
    }
}
