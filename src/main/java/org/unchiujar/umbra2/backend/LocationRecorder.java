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

package org.unchiujar.umbra2.backend;

import static org.unchiujar.umbra2.utils.LogUtilities.numberLogList;

import java.util.ArrayList;
import java.util.List;

import org.unchiujar.umbra2.location.ApproximateLocation;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class LocationRecorder implements ExploredProvider {
    private static final String TAG = LocationRecorder.class.getName();
    private static final String DATABASE_NAME = "visited.db";
    /**
     * The version number for the database used by SQLiteOpenHelper when for database upgrades.
     * Increment when database structure is modified.
     */
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "coordinates";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private static final String DATABASE_PROVIDER = "Visited";

    private static final String INSERT = "insert into " + TABLE_NAME + "("
            + LATITUDE + "," + LONGITUDE + ") values (?,?)";
    private SQLiteDatabase mDatabase;
    private SQLiteStatement mInsertStmt;

    public LocationRecorder(Context context) {
        OpenHelper openHelper = new OpenHelper(context);
        this.mDatabase = openHelper.getWritableDatabase();
        // this.db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // openHelper.onCreate(mDatabase);
        this.mInsertStmt = this.mDatabase.compileStatement(INSERT);

    }

    @Override
    public long insert(ApproximateLocation location) {

        this.mInsertStmt.bindDouble(1, location.getLatitude());
        this.mInsertStmt.bindDouble(2, location.getLongitude());
        long index = this.mInsertStmt.executeInsert();
        Log.d(TAG,
                DATABASE_NAME
                        + "Inserted latitude and longitude: "
                        + numberLogList(location.getLatitude(),
                                location.getLongitude()));

        return index;
    }

    public void insert(List<ApproximateLocation> locations) {
        // TODO test batch insert speed
        DatabaseUtils.InsertHelper batchInserter = new DatabaseUtils.InsertHelper(
                mDatabase, TABLE_NAME);
        int latitudeIndex = batchInserter.getColumnIndex(LATITUDE);
        int longitudeIndex = batchInserter.getColumnIndex(LONGITUDE);

        for (ApproximateLocation aproximateLocation : locations) {
            batchInserter.prepareForInsert();
            batchInserter.bind(latitudeIndex, aproximateLocation.getLatitude());
            batchInserter.bind(longitudeIndex,
                    aproximateLocation.getLongitude());
            batchInserter.execute();
            Log.d(TAG,
                    "Batch inserted latitude and longitude: "
                            + numberLogList(aproximateLocation.getLatitude(),
                                    aproximateLocation.getLongitude()));

        }
        batchInserter.close();
    }

    @Override
    public void deleteAll() {
        this.mDatabase.delete(TABLE_NAME, null, null);
    }

    public List<ApproximateLocation> selectAll() {

        List<ApproximateLocation> list = new ArrayList<ApproximateLocation>();
        Cursor cursor = this.mDatabase.query(TABLE_NAME, new String[]{
                LATITUDE, LONGITUDE
        }, null, null, null, null, LONGITUDE
                + " desc");
        Log.d(TAG, "Results obtained: " + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                ApproximateLocation location = new ApproximateLocation(
                        DATABASE_PROVIDER);
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                list.add(location);
            } while (cursor.moveToNext());
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }

        return list;
    }

    @Override
    public List<ApproximateLocation> selectVisited(
            ApproximateLocation upperLeft, ApproximateLocation lowerRight) {

        List<ApproximateLocation> list = new ArrayList<ApproximateLocation>();
        double longitudeMin = upperLeft.getLongitude();
        double latitudeMax = upperLeft.getLatitude();
        double longitudeMax = lowerRight.getLongitude();
        double latitudeMin = lowerRight.getLatitude();

        String condition = LONGITUDE + " >= " + longitudeMin + " AND "
                + LONGITUDE + " <= " + longitudeMax + " AND " + LATITUDE
                + " >= " + latitudeMin + " AND " + LATITUDE + " <= "
                + latitudeMax;

        Log.v(TAG, "Select condition is " + condition);
        Cursor cursor = this.mDatabase.query(TABLE_NAME, new String[] {
                LATITUDE, LONGITUDE
        }, condition, null, null, null, LATITUDE
                + " desc");
        Log.d(TAG, "Results obtained: " + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                ApproximateLocation location = new ApproximateLocation(
                        DATABASE_PROVIDER);
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                Log.v(TAG, "Added to list of results obtained: " + location);
                list.add(location);
            } while (cursor.moveToNext());
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    private static class OpenHelper extends SQLiteOpenHelper {

        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "Creating database: " + TABLE_NAME);
            db.execSQL("CREATE TABLE " + TABLE_NAME
                    + "(id INTEGER PRIMARY KEY, " + LATITUDE + " REAL, "
                    + LONGITUDE + " REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion > oldVersion) {
                db.beginTransaction();
                boolean success = true;
                for (int i = oldVersion; i < newVersion; ++i) {
                    int nextVersion = i + 1;
                    switch (nextVersion) {
                        case 2:
                            // success = upgradeToVersion2(mDatabase);
                            break;
                        case 3:
                            // success = upgrateToVersion3(mDatabase);
                            break;
                    // etc. for later versions.
                    }
                    if (!success) {
                        break;
                    }
                }
                if (success) {
                    db.setTransactionSuccessful();
                }
                db.endTransaction();
            } else {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        }
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }
}
