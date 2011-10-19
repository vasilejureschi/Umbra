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
/**
 * 
 */

package org.unchiujar.umbra;

import android.location.Location;

import java.util.Comparator;

/**
 * @author vasile
 */
public class LocationOrder implements Comparator<Location> {

    /**
     * The distance in meters to which we consider something explored.
     * 
     * <pre>
     * At equator:
     * decimal places  degrees          distance
     * 0                1.0             111 km
     * 1                0.1             11.1 km
     * 2                0.01            1.11 km
     * 3                0.001           111 m
     * 4                0.0001          11.1 m
     * 5                0.00001         1.11 m
     * 6                0.000001        0.111 m
     * 7                0.0000001       1.11 cm
     * 8                0.00000001      1.11 mm
     * </pre>
     */
    public static final double DEGREES_RADIUS = 0.0001;
    public static final double METERS_RADIUS = 11.1;

    @Override
    public int compare(Location firstLocation, Location secondLocation) {

        if (firstLocation.distanceTo(secondLocation) < METERS_RADIUS) {
            return 0;
        }

        // compare by longitude
        if (firstLocation.getLongitude() > secondLocation.getLongitude() + DEGREES_RADIUS) {
            return 1;

        }

        if (firstLocation.getLongitude() < secondLocation.getLongitude() - DEGREES_RADIUS) {
            return -1;
        }
        // compare by latitude

        if (firstLocation.getLatitude() > secondLocation.getLatitude() + DEGREES_RADIUS) {
            return 1;

        }

        // if (firstLocation.getLatitude() < secondLocation.getLatitude() -
        // DEGREES_RADIUS) {
        return -1;
        // }

    }

}
