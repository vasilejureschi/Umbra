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


package org.unchiujar.umbra2.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra2.R;
import org.unchiujar.umbra2.backend.ExploredProvider;
import org.unchiujar.umbra2.io.GpxImporter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Preferences extends PreferenceActivity {
    // if these values are changed also change in the necessary preferences xml
    // files
    public static final String TRANSPARENCY = "org.unchiujar.umbra.settings.transparency";
    public static final String UMBRA_PREFS = "org.unchiujar.umbra.settings";
    public static final String MEASUREMENT_SYSTEM = "org.unchiujar.umbra.settings.measurement";
    public static final String ANIMATE = "org.unchiujar.umbra.settings.animate";
    public static final String DRIVE_MODE = "org.unchiujar.umbra.settings.update_mode";
    public static final String IMPORT_DATA = "org.unchiujar.umbra.settings.import_data";
    public static final String FULLSCREEN = "org.unchiujar.umbra.settings.fullscreen";
    public static final String NOTIFICATION = "org.unchiujar.umbra.settings.notification";

    private static final int READ_REQUEST_CODE = 42;
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preference preference = findPreference(IMPORT_DATA);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                // browser.
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType("*/*.gpx");

                startActivityForResult(intent, READ_REQUEST_CODE);
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode != READ_REQUEST_CODE || resultCode != Activity.RESULT_OK) {
            return;
        }

        // The document selected by the user won't be returned in the intent.
        // Instead, a URI to that document will be contained in the return intent
        // provided to this method as a parameter.
        // Pull that URI using resultData.getData().
        Uri data = intent.getData();
        LOGGER.debug("File URI is {}", data);

        //there was no URI data so  just return
        if (data == null) {
            return;
        }

        //if we have all the data we need then load the file
        final String filePath = data.getEncodedPath();

        final ProgressDialog progress = new ProgressDialog(this);

        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setMessage(getString(R.string.importing_locations));
        progress.show();

        Runnable importer = new Runnable() {

            @Override
            public void run() {
                ExploredProvider cache = ((UmbraApplication) getApplication())
                        .getCache();

                try {
                    cache.insert(GpxImporter.importGPXFile(new FileInputStream(
                            new File(filePath))));
                } catch (ParserConfigurationException | SAXException e) {
                    LOGGER.error("Error parsing file", e);
                } catch (IOException e) {
                    LOGGER.error("Error reading file", e);
                }

                progress.dismiss();
            }
        };
        new Thread(importer).start();
    }
}
