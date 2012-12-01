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

package org.unchiujar.umbra.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.unchiujar.umbra.R;
import org.unchiujar.umbra.activities.UmbraApplication;
import org.unchiujar.umbra.backend.ExploredProvider;
import org.unchiujar.umbra.io.GpxImporter;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RelativeLayout;

public class FolderSelectPreference extends Preference implements TextWatcher {
    private final String TAG = FolderSelectPreference.class.getName();
    private static final String DEFAULT_VALUE = "/";

    private AutoCompleteTextView mTxtGpxFolder;
    private Button mBtnLoadGpx;

    private String mFolder;
    private Context mContext;

    public FolderSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);

    }

    public FolderSelectPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        Log.d(TAG, "Init preferences.");
        mContext = context;
    }

    private void setValuesFromXml(AttributeSet attrs) {
        // mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 255);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        LayoutInflater mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        return (RelativeLayout) mInflater.inflate(R.xml.folder_import, parent,
                false);

    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        Log.d(TAG, "Binding view...");
        mTxtGpxFolder = (AutoCompleteTextView) view
                .findViewById(R.id.txtSelectGpxFolder);
        mBtnLoadGpx = (Button) view.findViewById(R.id.btnLoadGpx);

        mTxtGpxFolder.addTextChangedListener(this);
        mTxtGpxFolder.setText(mFolder);

        mBtnLoadGpx.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // get list of gpx files from the folders
                File path = new File(mTxtGpxFolder.getText().toString());
                final ArrayList<InputStream> gpxs = new ArrayList<InputStream>();
                File[] files = path.listFiles();
                for (File file : files) {
                    if (file.toString().toLowerCase().endsWith(".gpx")) {
                        Log.d(TAG, "Found gpx file: " + file.toString());
                        try {
                            gpxs.add(new FileInputStream(file));
                            // try to load gpx data
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "File not found", e);
                        }
                    }
                }

                final ProgressDialog progress = new ProgressDialog(mContext);

                progress.setCancelable(false);
                progress.setMax(gpxs.size());
                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progress.setMessage(mContext
                        .getString(R.string.importing_locations));

                progress.show();

                Runnable importer = new Runnable() {

                    @Override
                    public void run() {
                        ExploredProvider cache = ((UmbraApplication) ((Activity) mContext)
                                .getApplication()).getCache();

                        for (InputStream stream : gpxs) {
                            try {
                                GpxImporter.importGPXFile(stream, cache);
                                progress.incrementProgressBy(1);
                            } catch (ParserConfigurationException e) {
                                Log.e(TAG, "Error parsing file", e);
                            } catch (SAXException e) {
                                Log.e(TAG, "Error parsing file", e);
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading file", e);
                            }

                        }
                        progress.dismiss();
                    }
                };
                new Thread(importer).start();

                Log.d(TAG, "Imported GPX data.");
            }
        });
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            mFolder = getPersistedString(DEFAULT_VALUE);
        } else {
            mFolder = (String) defaultValue;
            persistString(mFolder);
        }

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // NO-OP
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        // NO-OP
    }

    @Override
    public void afterTextChanged(Editable s) {
        mFolder = s.toString();

        ArrayList<String> folders = new ArrayList<String>();
        // create array of folders
        File path = new File(mFolder);
        // if it is a path create array of folders
        File[] files = path.listFiles();

        if (path.isDirectory() && files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    folders.add(file.getAbsolutePath());
                }
            }
        }
        Log.v(TAG, "Folders found for autocomplete:" + folders);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_dropdown_item_1line, folders);

        mTxtGpxFolder.setAdapter(adapter);
        // display the dropdown only if there is a list of folders
        // to select from
        if (folders.size() > 0) {
            mTxtGpxFolder.showDropDown();
        }
        persistString(mFolder);
    }

}
