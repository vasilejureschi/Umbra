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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;

public class Settings extends Activity implements SeekBar.OnSeekBarChangeListener {
    public static final String UMBRA_PREFS = "org.unchiujar.umbra.settings";
    public static final String TRANSPARENCY = "org.unchiujar.umbra.settings.transparency";
    public static final String MEASUREMENT_SYSTEM = "org.unchiujar.umbra.settings.measurement";
    public static final String ANIMATE = "org.unchiujar.umbra.settings.animate";
    public static final String UPDATE_MODE = "org.unchiujar.umbra.settings.update_mode";

    private static final String TAG = Settings.class.getName();
    private SeekBar mSetTransparency;
    private CheckBox mImperial;
    private CheckBox mAnimate;
    private CheckBox mUpdate;

    private SharedPreferences mSettings;

    private final CheckBox.OnClickListener mImperialListener = new CheckBox.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Checkbox clicked");
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putBoolean(MEASUREMENT_SYSTEM, mImperial.isChecked());
            editor.commit();
            updateCheckbox(mImperial, R.string.measurement_metric, R.string.measurement_imperial);
        }
    };

    private final CheckBox.OnClickListener mAnimateListener = new CheckBox.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Checkbox clicked");
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putBoolean(ANIMATE, mAnimate.isChecked());
            editor.commit();
            updateCheckbox(mAnimate, R.string.animate_none, R.string.animate_move);
        }

    };

    private final CheckBox.OnClickListener mUpdateListener = new CheckBox.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Checkbox clicked");
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putBoolean(UPDATE_MODE, mUpdate.isChecked());
            editor.commit();
            updateCheckbox(mUpdate, R.string.updates_walk, R.string.updates_car);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(UMBRA_PREFS, 0);

        setContentView(R.layout.settings);
        mSetTransparency = (SeekBar) findViewById(R.id.transparency_seek);
        mSetTransparency.setOnSeekBarChangeListener(this);
        mSetTransparency.setProgress(mSettings.getInt(TRANSPARENCY, 120));

        mAnimate = (CheckBox) findViewById(R.id.check_animate);
        mAnimate.setChecked(mSettings.getBoolean(ANIMATE,
                true));
        mAnimate.setOnClickListener(mAnimateListener);
        updateCheckbox(mAnimate, R.string.animate_none, R.string.animate_move);

        mImperial = (CheckBox) findViewById(R.id.check_metric);
        mImperial.setChecked(mSettings.getBoolean(MEASUREMENT_SYSTEM,
                false));
        mImperial.setOnClickListener(mImperialListener);
        updateCheckbox(mImperial, R.string.measurement_metric, R.string.measurement_imperial);

        mUpdate = (CheckBox) findViewById(R.id.check_updates);
        mUpdate.setChecked(mSettings.getBoolean(UPDATE_MODE,
                false));
        mUpdate.setOnClickListener(mUpdateListener);
        updateCheckbox(mUpdate, R.string.updates_walk, R.string.updates_car);
    }

    private void updateCheckbox(CheckBox checkbox, int checkedMessage, int uncheckedMessage) {
        if (checkbox.isChecked()) {
            checkbox.setText(checkedMessage);
        } else {
            checkbox.setText(uncheckedMessage);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // change image transparency
        Log.d(TAG, "Transparency set to " + progress);
        ImageView view = (ImageView) findViewById(R.id.transparency_image);
        view.setAlpha(Math.abs(progress - 255));
        // save mSettings
        SharedPreferences.Editor editor = mSettings.edit();
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
