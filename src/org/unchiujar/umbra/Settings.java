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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

public class Settings extends Activity implements SeekBar.OnSeekBarChangeListener {
    public static final String UMBRA_PREFS = "org.unchiujar.umbra.settings";
    public static final String TRANSPARENCY = "org.unchiujar.umbra.settings.transparency";
    
    private static final String TAG = Settings.class.getName();
    private SeekBar setTransparency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setTransparency = (SeekBar) findViewById(R.id.transparency_seek);
        setTransparency.setOnSeekBarChangeListener(this);
        setTransparency.setProgress(getSharedPreferences(UMBRA_PREFS, 0).getInt(TRANSPARENCY, 120));        
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // change image transparency
        Log.d(TAG, "Transparency set to " + progress);
        ImageView view = (ImageView) findViewById(R.id.transparency_image);
        view.setAlpha(Math.abs(progress - 255));
        //save settings
        SharedPreferences settings = getSharedPreferences(UMBRA_PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(TRANSPARENCY, progress);
        editor.commit();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

}
