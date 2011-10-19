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

import java.util.List;

interface LocationProvider {
    long insert(ApproximateLocation location);

    void deleteAll();

    List<ApproximateLocation> selectAll();

    /**
     * Returns a list of visited points in the specified area. The coordinate
     * system used is the latitude longitude decimal system.
     * 
     * @param upperLeft the upper left coordinates using the latitude/longitude
     *            system
     * @param bottomRight the bottom right coordinates using the
     *            latitude/longitude system
     * @return a List of visited locations
     */
    List<ApproximateLocation> selectVisited(ApproximateLocation upperLeft,
            ApproximateLocation bottomRight);
}
