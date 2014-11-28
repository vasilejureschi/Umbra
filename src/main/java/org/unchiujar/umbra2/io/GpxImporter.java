/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.unchiujar.umbra2.io;

import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra2.location.ApproximateLocation;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Imports GPX file.
 *
 * @author Leif Hendrik Wilden
 * @author Steffen Horlacher
 * @author Rodrigo Damazio
 * @author Vasile Jureschi
 */
public class GpxImporter extends DefaultHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GpxImporter.class);
    // GPX tag names and attributes
    private static final String TAG_ALTITUDE = "ele";
    private static final String TAG_DESCRIPTION = "desc";
    private static final String TAG_NAME = "name";
    private static final String TAG_TIME = "time";
    private static final String TAG_TRACK = "trk";
    private static final String TAG_TRACK_POINT = "trkpt";

    private static final Object TAG_TRACK_SEGMENT = "trkseg";
    private static final String ATT_LAT = "lat";
    private static final String ATT_LON = "lon";

    // The SAX locator to get the current line information
    private Locator locator;

    // The current element content
    private String content;

    // True if if we're inside a track's xml element
    private boolean isInTrackElement = false;

    // The current child depth for the current track
    private int trackChildDepth = 0;

    // The current location
    private Location location;

    // The last location in the current segment
    private Location lastLocationInSegment;

    private static List<ApproximateLocation> locations = new LinkedList<ApproximateLocation>();
    private int elementsLoaded;

    /**
     * Reads GPS tracks from a GPX file and writes tracks and their coordinates to the database.
     *
     * @param inputStream the input stream for the GPX file
     */
    public static List<ApproximateLocation> importGPXFile(InputStream inputStream) throws ParserConfigurationException,
            SAXException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();
        GpxImporter gpxImporter = new GpxImporter();

        long start = System.currentTimeMillis();

        saxParser.parse(inputStream, gpxImporter);

        long end = System.currentTimeMillis();
        LOGGER.debug("Total import time: " + (end - start) + "ms");
        return locations;
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        String newContent = new String(ch, start, length);
        if (content == null) {
            content = newContent;
        } else {
            /*
             * In 99% of the cases, a single call to this method will be made for each sequence of
             * characters we're interested in, so we'll rarely be concatenating strings, thus not
             * justifying the use of a StringBuilder.
             */
            content += newContent;
        }
    }

    @Override
    public void startElement(String uri, String localName, String name,
                             Attributes attributes) throws SAXException {
        if (isInTrackElement) {
            trackChildDepth++;
            if (localName.equals(TAG_TRACK)) {
                throw new SAXException(
                        createErrorMessage("Invalid GPX. Already inside a track."));
            } else if (localName.equals(TAG_TRACK_SEGMENT)) {
                onTrackSegmentElementStart();
            } else if (localName.equals(TAG_TRACK_POINT)) {
                onTrackPointElementStart(attributes);
            }
        } else if (localName.equals(TAG_TRACK)) {
            isInTrackElement = true;
            trackChildDepth = 0;
            onTrackElementStart();
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if (!isInTrackElement) {
            content = null;
            return;
        }

        if (localName.equals(TAG_TRACK)) {
            onTrackElementEnd();
            isInTrackElement = false;
            trackChildDepth = 0;
        } else if (localName.equals(TAG_NAME)) {
            // we are only interested in the first level name element
            if (trackChildDepth == 1) {
                onNameElementEnd();
            }
        } else if (localName.equals(TAG_DESCRIPTION)) {
            // we are only interested in the first level description element
            if (trackChildDepth == 1) {
                onDescriptionElementEnd();
            }
        } else if (localName.equals(TAG_TRACK_SEGMENT)) {
            onTrackSegmentElementEnd();
        } else if (localName.equals(TAG_TRACK_POINT)) {
            onTrackPointElementEnd();
        } else if (localName.equals(TAG_ALTITUDE)) {
            onAltitudeElementEnd();
        } else if (localName.equals(TAG_TIME)) {
            onTimeElementEnd();
        }
        trackChildDepth--;

        // reset element content
        content = null;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * On track element start.
     */
    private void onTrackElementStart() {
    }

    /**
     * On track element end.
     */
    private void onTrackElementEnd() {
    }

    /**
     * On name element end.
     */
    private void onNameElementEnd() {
        // NO-OP
    }

    /**
     * On description element end.
     */
    private void onDescriptionElementEnd() {
        // NO-OP
    }

    /**
     * On track segment start.
     */
    private void onTrackSegmentElementStart() {
        location = null;
        lastLocationInSegment = null;
    }

    /**
     * On track segment element end.
     */
    private void onTrackSegmentElementEnd() {
        // Nothing needs to be done
    }

    /**
     * On track point element start.
     *
     * @param attributes the attributes
     */
    private void onTrackPointElementStart(Attributes attributes)
            throws SAXException {
        LOGGER.debug("Loaded elements " + elementsLoaded);
        elementsLoaded = 0;
        if (location != null) {
            throw new SAXException(
                    createErrorMessage("Found a track point inside another one."));
        }
        String latitude = attributes.getValue(ATT_LAT);
        String longitude = attributes.getValue(ATT_LON);

        if (latitude == null || longitude == null) {
            throw new SAXException(
                    createErrorMessage("Point with no longitude or latitude."));
        }
        double latitudeValue;
        double longitudeValue;
        try {
            latitudeValue = Double.parseDouble(latitude);
            longitudeValue = Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            throw new SAXException(
                    createErrorMessage("Unable to parse latitude/longitude: "
                            + latitude + "/" + longitude), e
            );
        }

        location = createNewLocation(latitudeValue, longitudeValue, -1L);
    }

    /**
     * On track point element end.
     */
    private void onTrackPointElementEnd() throws SAXException {
        if (!isValidLocation(location)) {
            throw new SAXException(
                    createErrorMessage("Invalid location detected: " + location));
        }

        // insert into cache
        locations.add(new ApproximateLocation(location));
        elementsLoaded++;
        lastLocationInSegment = location;
        location = null;
    }

    /**
     * On altitude element end.
     */
    private void onAltitudeElementEnd() throws SAXException {
        if (location == null || content == null) {
            return;
        }

        try {
            location.setAltitude(Double.parseDouble(content));
        } catch (NumberFormatException e) {
            throw new SAXException(
                    createErrorMessage("Unable to parse altitude: " + content),
                    e);
        }
    }

    /**
     * On time element end. Sets location time and doing additional calculations as this is the last
     * value required for the location. Also sets the start time for the trip statistics builder as
     * there is no start time in the track root element.
     */
    private void onTimeElementEnd() throws SAXException {
        if (location == null || content == null) {
            return;
        }

        // Parse the time
        long time;
        try {
            time = StringUtils.getTime(content.trim());
        } catch (IllegalArgumentException e) {
            throw new SAXException(createErrorMessage("Unable to parse time: "
                    + content), e);
        }
        location.setTime(time);

        // Calculate derived attributes from previous point
        if (lastLocationInSegment != null
                && lastLocationInSegment.getTime() != 0) {
            long timeDifference = time - lastLocationInSegment.getTime();

            // check for negative time change
            if (timeDifference <= 0) {
                LOGGER.warn("Time difference not positive.");
            } else {

                /*
                 * We don't have a speed and bearing in GPX, make something up from the last two
                 * points. GPS points tend to have some inherent imprecision, speed and bearing will
                 * likely be off, so the statistics for things like max speed will also be off.
                 */
                float speed = lastLocationInSegment.distanceTo(location)
                        * 1000.0f / timeDifference;
                location.setSpeed(speed);
            }
            location.setBearing(lastLocationInSegment.bearingTo(location));
        }
    }

    /**
     * Creates a new location
     *
     * @param latitude  location latitude
     * @param longitude location longitude
     * @param time      location time
     */
    private Location createNewLocation(double latitude, double longitude,
                                       long time) {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setAltitude(0.0f);
        loc.setTime(time);
        loc.removeAccuracy();
        loc.removeBearing();
        loc.removeSpeed();
        return loc;
    }

    /**
     * Creates an error message.
     *
     * @param message the message
     */
    private String createErrorMessage(String message) {
        return String.format(Locale.US,
                "Parsing error at line: %d column: %d. %s",
                locator.getLineNumber(), locator.getColumnNumber(), message);
    }

    // =================== utils

    /**
     * Test if a given GeoPoint is valid, i.e. within physical bounds.
     *
     * @param latLng the point to be tested
     * @return true, if it is a physical location on earth.
     */
    public static boolean isValidGeoPoint(LatLng latLng) {
        return Math.abs(latLng.latitude) < 90
                && Math.abs(latLng.longitude) <= 180;
    }

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth. Note: The
     * special separator locations (which have latitude = 100) will not qualify as valid. Neither
     * will locations with lat=0 and lng=0 as these are most likely "bad" measurements which often
     * cause trouble.
     *
     * @param location the location to test
     * @return true if the location is a valid location.
     */
    public static boolean isValidLocation(Location location) {
        return location != null && Math.abs(location.getLatitude()) <= 90
                && Math.abs(location.getLongitude()) <= 180;
    }

    public List<ApproximateLocation> getLocations() {
        return locations;
    }
}
