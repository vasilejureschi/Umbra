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

package org.unchiujar.umbra.activities;

import org.unchiujar.umbra.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

    private static final String TAG = Preferences.class.getName();
    //if these values are changed also change in the necessary preferences xml files
    public static final String TRANSPARENCY="org.unchiujar.umbra.settings.transparency";
    public static final String UMBRA_PREFS = "org.unchiujar.umbra.settings";
    public static final String MEASUREMENT_SYSTEM = "org.unchiujar.umbra.settings.measurement";
    public static final String ANIMATE = "org.unchiujar.umbra.settings.animate";
    public static final String DRIVE_MODE = "org.unchiujar.umbra.settings.update_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }


}
