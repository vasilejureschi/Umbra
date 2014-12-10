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

package org.unchiujar.umbra2.utils;

import android.location.Location;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class LocationUtilsTest {

    @Test
    public void haversineDistance() {
        Location location1 = new Location("test");
        location1.setLatitude(-73.995008d);
        location1.setLongitude(40.752842d);

        Location location2 = new Location("test");
        location2.setLatitude(-73.994905d);
        location2.setLongitude(40.752798d);

        double actual = LocationUtilities.haversineDistance(location1,
                location2);
        double expected = location1.distanceTo(location2);
        assertEquals(expected, actual, 0.2d);
    }

    @Test
    public void haversineDistanceNegative() {
        Location location1 = new Location("test");
        location1.setLatitude(-73.995008d);
        location1.setLongitude(-40.752842d);

        Location location2 = new Location("test");
        location2.setLatitude(-73.994905d);
        location2.setLongitude(-40.752798d);

        double actual = LocationUtilities.haversineDistance(location1,
                location2);
        double expected = location1.distanceTo(location2);
        assertEquals(expected, actual, 0.2d);
    }

    @Test
    public void haversineDistanceZero() {
        Location location1 = new Location("test");
        location1.setLatitude(0);
        location1.setLongitude(0);

        Location location2 = new Location("test");
        location2.setLatitude(0);
        location2.setLongitude(0);

        double actual = LocationUtilities.haversineDistance(location1,
                location2);
        double expected = location1.distanceTo(location2);
        assertEquals(expected, actual, 0.0d);
    }

}
