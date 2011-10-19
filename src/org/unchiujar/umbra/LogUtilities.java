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

package org.unchiujar.umbra;

import android.location.Location;

public class LogUtilities {

    /**
     * Logging utility method used for formatting a list of numbers in the
     * [x,y,z] format.
     * 
     * @param numbers the numbers to be formated
     * @return a formatted string
     */
    public static String numberLogList(double... numbers) {
        String formatted = "[";
        for (double d : numbers) {
            formatted += d + ",";
        }
        return formatted + "\b]";
    }

    public static String locationLogList(Location... locations) {
        String formatted = "[";
        for (Location location : locations) {
            formatted += location.getLatitude() + " " + location.getLongitude() + "][";
        }

        return formatted + "\b";
    }
}
