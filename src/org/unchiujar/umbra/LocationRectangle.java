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

// TODO: Auto-generated Javadoc
/**
 * The Class LocationRectangle.
 */
public class LocationRectangle {
    
    /** The upper left. */
    private ApproximateLocation upperLeft;
    
    /** The lower right. */
    private ApproximateLocation lowerRight;
    
    /**
     * Instantiates a new location rectangle.
     *
     * @param upperLeft the upper left
     * @param lowerRight the lower right
     */
    public LocationRectangle(ApproximateLocation upperLeft, ApproximateLocation lowerRight) {
        super();
        this.upperLeft = upperLeft;
        this.lowerRight = lowerRight;
    }
    
    /**
     * Gets the upper left.
     *
     * @return the upper left
     */
    public ApproximateLocation getUpperLeft() {
        return upperLeft;
    }
    
    /**
     * Sets the upper left.
     *
     * @param upperLeft the new upper left
     */
    public void setUpperLeft(ApproximateLocation upperLeft) {
        this.upperLeft = upperLeft;
    }
    
    /**
     * Gets the lower right.
     *
     * @return the lower right
     */
    public ApproximateLocation getLowerRight() {
        return lowerRight;
    }
    
    /**
     * Sets the lower right.
     *
     * @param lowerRight the new lower right
     */
    public void setLowerRight(ApproximateLocation lowerRight) {
        this.lowerRight = lowerRight;
    }        
}
