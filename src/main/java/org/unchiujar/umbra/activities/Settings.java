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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.unchiujar.umbra.R;
import org.unchiujar.umbra.backend.VisitedAreaCache;
import org.unchiujar.umbra.io.GpxImporter;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;

public class Settings extends Activity implements SeekBar.OnSeekBarChangeListener {
    public static final String UMBRA_PREFS = "org.unchiujar.umbra.settings";
    public static final String TRANSPARENCY = "org.unchiujar.umbra.settings.transparency";
    public static final String MEASUREMENT_SYSTEM = "org.unchiujar.umbra.settings.measurement";
    public static final String ANIMATE = "org.unchiujar.umbra.settings.animate";
    public static final String DRIVE_MODE = "org.unchiujar.umbra.settings.update_mode";

    private static final String TAG = Settings.class.getName();
    private SeekBar mSetTransparency;
    private CheckBox mImperial;
    private CheckBox mAnimate;
    private CheckBox mUpdate;
    private AutoCompleteTextView mTxtGpxFolder;
    private Button mBtnLoadGpx;

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
            editor.putBoolean(DRIVE_MODE, mUpdate.isChecked());
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
        mUpdate.setChecked(mSettings.getBoolean(DRIVE_MODE,
                false));
        mUpdate.setOnClickListener(mUpdateListener);
        updateCheckbox(mUpdate, R.string.updates_walk, R.string.updates_car);

        mBtnLoadGpx = (Button) findViewById(R.id.btnLoadGpx);

        mBtnLoadGpx.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // get list of gpx files from the folders
                File path = new File(mTxtGpxFolder.getText().toString());
                File[] files = path.listFiles();
                for (File file : files) {
                    if (file.toString().toLowerCase().endsWith(".gpx")) {
                        Log.d(TAG, "Found gpx file: " + file.toString());
                        try {
                            // try to load gpx data

                            GpxImporter.importGPXFile(new FileInputStream(file), new VisitedAreaCache(getApplicationContext()));
                        } catch (FileNotFoundException e) {
                            Log.d(TAG, "File not found" , e);
                        } catch (ParserConfigurationException e) {
                            Log.d(TAG, "Malformed file" , e);
                        } catch (SAXException e) {
                            Log.d(TAG, "Malformed file" , e);
                        } catch (IOException e) {
                            Log.d(TAG, "Error reading file" , e);
                        }
                        Log.d(TAG, "Imported GPX data.");
                        
                    }
                }
            }
        });

        mTxtGpxFolder = (AutoCompleteTextView) findViewById(R.id.txtSelectGpxFolder);
        mTxtGpxFolder.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // NO-OP
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // NO-OP
            }

            @Override
            public void afterTextChanged(Editable s) {
                ArrayList<String> folders = new ArrayList<String>();
                // create array of folders
                File path = new File(s.toString());
                // if it is a path create array of folders
                if (path.isDirectory()) {
                    File[] files = path.listFiles();
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folders.add(file.getAbsolutePath());
                        }
                    }
                }
                Log.v(TAG, "Folders found for autocomplete:" + folders);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(Settings.this,
                        android.R.layout.simple_dropdown_item_1line, folders);

                mTxtGpxFolder.setAdapter(adapter);
                // display the dropdown only if there is a list of folders
                // to select from
                if (folders.size() > 0) {
                    mTxtGpxFolder.showDropDown();
                }
            }
        });
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
