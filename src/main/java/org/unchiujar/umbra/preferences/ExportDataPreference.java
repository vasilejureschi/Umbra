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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra.R;
import org.unchiujar.umbra.backend.LocationRecorder;
import org.unchiujar.umbra.io.GpxExporter;
import org.unchiujar.umbra.location.ApproximateLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExportDataPreference extends Preference {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportDataPreference.class);

    private String mFolder;
    private Context mContext;
    private ProgressDialog progress;

    public ExportDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);

    }

    public ExportDataPreference(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        LOGGER.debug("Init preferences.");
        mContext = context;


        progress = new ProgressDialog(mContext);
        progress.setCancelable(false);
        progress.setMax(100);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMessage(mContext
                .getString(R.string.exporting_locations));

    }

    private void setValuesFromXml(AttributeSet attrs) {
        // mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 255);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        LayoutInflater mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        return mInflater.inflate(R.layout.folder_export, parent,
                false);

    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        LOGGER.debug("Binding view...");

        Button mBtnExportGpx = (Button) view.findViewById(R.id.btnExportGpx);
        mBtnExportGpx.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Importer importer = new Importer();
                importer.execute();
            }
        });
    }

    private class Importer extends AsyncTask<Void, Integer, Long> {
        private File exported = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.show();
        }

        @Override
        protected Long doInBackground(Void... params) {
            // TODO show the the database is being loaded
            LocationRecorder recorder = new LocationRecorder(mContext);
            //load the entire database
            List<ApproximateLocation> locations = recorder.selectAll();
            //TODO show update
            int noOfLocations = locations.size();
            LOGGER.debug("Loaded {} locations from database .", noOfLocations);
            progress.setMax(noOfLocations);
            GpxExporter exporter = new GpxExporter(mContext);

            File sdcard = Environment.getExternalStorageDirectory();
            try {
                exported = new File(sdcard, "umbra_" + System.currentTimeMillis() + "_.gpx");
                exported.createNewFile();

                exporter.prepare(new FileOutputStream(exported));
            } catch (FileNotFoundException e) {
                LOGGER.error("Error opening stream for output file", e);
            } catch (IOException e) {
                LOGGER.error("Error creating export file.", e);
            }

            exporter.writeHeader();
            if (noOfLocations > 0) {
                exporter.writeBeginTrack(locations.get(0));
                exporter.writeOpenSegment();
                int written = 0;
                int debug = Integer.MAX_VALUE;
                for (ApproximateLocation location : locations) {
                    exporter.writeLocation(location);
                    if (written % 5 == 0) {
                        publishProgress();
                    }
                    written++;

                    if (written > debug) {
                        break;
                    }
                }
                exporter.writeCloseSegment();
                exporter.writeEndTrack(locations.get(noOfLocations - 1));
            }
            exporter.writeFooter();
            exporter.close();
            return 0L;
        }


        @Override
        protected void onProgressUpdate(Integer... progress) {
            ExportDataPreference.this.progress.incrementProgressBy(1);
        }

        @Override
        protected void onPostExecute(Long result) {
            progress.dismiss();
            LOGGER.debug("Exported GPX data.");
            Toast.makeText(mContext, "Data exported to " + exported.getAbsolutePath(), Toast.LENGTH_LONG).show();


            AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setCancelable(true)
                    .setIcon(android.R.drawable.ic_dialog_info).setMessage("Do you want to share or send the exported file ?")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                        }
                    }).setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int arg1) {
                            dialog.dismiss();
                        }
                    }).setTitle("Share exported data");
            builder.setNegativeButton(
                    R.string.share_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_SEND)
                                    .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exported))
                                    .putExtra(Intent.EXTRA_SUBJECT, mContext.getString(R.string.share_data))
                                    .putExtra(Intent.EXTRA_TEXT, "Umbra data")
                                    .setType("text/plain");

                            mContext.startActivity(Intent.createChooser(intent, "Share Umbra data."));
                        }
                    }
            );

            final Dialog dialog = builder.create();
            dialog.show();

        }

    }
}
