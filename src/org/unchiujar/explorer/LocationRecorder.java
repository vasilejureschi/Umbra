package org.unchiujar.explorer;

import static org.unchiujar.explorer.LogUtilities.numberLogList;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class LocationRecorder implements LocationProvider {
    private static final String TAG = LocationRecorder.class.getName();
    private static final String DATABASE_NAME = "visited.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "coordinates";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private static final String DATABASE_PROVIDER = "Visited";

    private Context context;
    private SQLiteDatabase db;
    private SQLiteStatement insertStmt;
    private static final String INSERT = "insert into " + TABLE_NAME + "(" + LATITUDE + "," + LONGITUDE
            + ") values (?,?)";

    private static LocationRecorder instance;

    private LocationRecorder(Context context) {
        this.context = context;
        OpenHelper openHelper = new OpenHelper(this.context);
        this.db = openHelper.getWritableDatabase();
        // this.db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // openHelper.onCreate(db);
        this.insertStmt = this.db.compileStatement(INSERT);
        
    }

    public static LocationRecorder getInstance(Context context) {
        return (instance == null) ? instance = new LocationRecorder(context) : instance;
    }

    public long insert(AproximateLocation location) {

        this.insertStmt.bindDouble(1, location.getLatitude());
        this.insertStmt.bindDouble(2, location.getLongitude());
        long index = this.insertStmt.executeInsert();
        Log.d(TAG, DATABASE_NAME + "Inserted latitude and longitude: "
                + numberLogList(location.getLatitude(), location.getLongitude()));

        return index;
    }

    public void insert(List<AproximateLocation> locations) {
        DatabaseUtils.InsertHelper batchInserter = new DatabaseUtils.InsertHelper(db, TABLE_NAME);
        int latitudeIndex = batchInserter.getColumnIndex(LATITUDE);
        int longitudeIndex = batchInserter.getColumnIndex(LONGITUDE);
        
        // see http://notes.theorbis.net/2010/02/batch-insert-to-sqlite-on-android.html
        for (AproximateLocation aproximateLocation : locations) {
            batchInserter.prepareForInsert();
            batchInserter.bind(latitudeIndex, aproximateLocation.getLatitude());
            batchInserter.bind(longitudeIndex, aproximateLocation.getLatitude());
            batchInserter.execute();
            Log.d(TAG, "Batch inserted latitude and longitude: "
                    + numberLogList(aproximateLocation.getLatitude(), aproximateLocation.getLongitude()));

        }
        batchInserter.close();
    }

    public void deleteAll() {
        this.db.delete(TABLE_NAME, null, null);
    }

    public List<AproximateLocation> selectAll() {

        List<AproximateLocation> list = new ArrayList<AproximateLocation>();
        Cursor cursor = this.db.query(TABLE_NAME, new String[] { LATITUDE, LONGITUDE }, null, null, null,
                null, LONGITUDE + " desc");
        if (cursor.moveToFirst()) {
            do {
                AproximateLocation location = new AproximateLocation(DATABASE_PROVIDER);
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                list.add(location);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return list;
    }

    public List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation lowerRight) {

        List<AproximateLocation> list = new ArrayList<AproximateLocation>();
        double longitudeMin = upperLeft.getLongitude();
        double latitudeMax = upperLeft.getLatitude();
        double longitudeMax = lowerRight.getLongitude();
        double latitudeMin = lowerRight.getLatitude();

        String condition = LONGITUDE + " >= " + longitudeMin + " AND " + LONGITUDE + " <= " + longitudeMax
                + " AND " + LATITUDE + " >= " + latitudeMin + " AND " + LATITUDE + " <= " + latitudeMax;

        Cursor cursor = this.db.query(TABLE_NAME, new String[] { LATITUDE, LONGITUDE }, condition, null,
                null, null, LONGITUDE + " desc");
        Log.d(TAG, "Results obtained: " + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                AproximateLocation location = new AproximateLocation(DATABASE_PROVIDER);
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                Log.v(TAG, "Added to list of results obtained: " + location);
                list.add(location);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
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
            db.execSQL("CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY, " + LATITUDE + " REAL, "
                    + LONGITUDE + " REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
