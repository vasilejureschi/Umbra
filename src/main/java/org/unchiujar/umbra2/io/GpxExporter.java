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


package org.unchiujar.umbra2.io;

import android.location.Location;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Write track as GPX to a file.
 *
 * @author Sandor Dornbush
 * @author Vasile Jureschit
 */
public class GpxExporter {

    private static final NumberFormat ELEVATION_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat COORDINATE_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
    /*
     * GPX readers expect to see fractional numbers with US-style punctuation.
     * That is, they want periods for decimal points, rather than commas.
     */
        ELEVATION_FORMAT.setMaximumFractionDigits(1);
        ELEVATION_FORMAT.setGroupingUsed(false);

        COORDINATE_FORMAT.setMaximumFractionDigits(6);
        COORDINATE_FORMAT.setMaximumIntegerDigits(3);
        COORDINATE_FORMAT.setGroupingUsed(false);
    }

    private PrintWriter printWriter;

    public String getExtension() {
        return "GPX";
    }

    public void prepare(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    public void close() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter = null;
        }
    }


    public void writeHeader() {
        if (printWriter != null) {
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<gpx");
            printWriter.println("version=\"1.1\"");
            printWriter.println(
                    "creator=\"" + "Umbra" + "\"");
            printWriter.println("xmlns=\"http://www.topografix.com/GPX/1/1\"");
            printWriter.println(
                    "xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"");
            printWriter.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            printWriter.println("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1"
                    + " http://www.topografix.com/GPX/1/1/gpx.xsd"
                    + " http://www.topografix.com/GPX/Private/TopoGrafix/0/1"
                    + " http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd\">");
            printWriter.println("<metadata>");
            printWriter.println("<name>" + StringUtils.formatCData("Umbra") + "</name>");
            printWriter.println("<desc>" + StringUtils.formatCData("Umbra exported data") + "</desc>");
            printWriter.println("</metadata>");
        }
    }

    public void writeFooter() {
        if (printWriter != null) {
            printWriter.println("</gpx>");
        }
    }

    public void writeBeginWaypoints() {
        // Do nothing
    }


    public void writeEndWaypoints() {
        // Do nothing
    }


    public void writeWaypoint(Location location) {
        if (printWriter != null) {
            if (location != null) {
                printWriter.println("<wpt " + formatLocation(location) + ">");
                if (location.hasAltitude()) {
                    printWriter.println("<ele>" + ELEVATION_FORMAT.format(location.getAltitude()) + "</ele>");
                }
                printWriter.println(
                        "<time>" + StringUtils.formatDateTimeIso8601(location.getTime()) + "</time>");
                printWriter.println("</wpt>");
            }
        }
    }

    /**
     * Formats a location with latitude and longitude coordinates.
     *
     * @param location the location
     */
    private String formatLocation(Location location) {
        return "lat=\"" + COORDINATE_FORMAT.format(location.getLatitude()) + "\" lon=\""
                + COORDINATE_FORMAT.format(location.getLongitude()) + "\"";
    }


    public void writeBeginTrack(Location startLocation) {
        if (printWriter != null) {
            printWriter.println("<trk>");
            printWriter.println("<name>Umbra track</name>");
//            printWriter.println("<desc>" + StringUtils.formatCData(track.getDescription()) + "</desc>");
//            printWriter.println("<type>" + StringUtils.formatCData(track.getCategory()) + "</type>");
            printWriter.println("<extensions><topografix:color>c0c0c0</topografix:color></extensions>");
        }
    }

    public void writeEndTrack(Location endLocation) {
        if (printWriter != null) {
            printWriter.println("</trk>");
        }
    }

    public void writeOpenSegment() {
        printWriter.println("<trkseg>");
    }

    public void writeCloseSegment() {
        printWriter.println("</trkseg>");
    }


    public void writeLocation(Location location) {
        if (printWriter != null) {
            printWriter.println("<trkpt " + formatLocation(location) + ">");
            if (location.hasAltitude()) {
                printWriter.println("<ele>" + ELEVATION_FORMAT.format(location.getAltitude()) + "</ele>");
            }
            printWriter.println(
                    "<time>" + StringUtils.formatDateTimeIso8601(location.getTime()) + "</time>");
            printWriter.println("</trkpt>");
        }
    }
}
