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

package org.unchiujar.umbra.overlays;

import static org.unchiujar.umbra.utils.LocationUtilities.locationToGeoPoint;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import org.unchiujar.umbra.activities.Settings;
import org.unchiujar.umbra.location.ApproximateLocation;
import org.unchiujar.umbra.location.LocationOrder;
import org.unchiujar.umbra.utils.LocationUtilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;

import java.util.List;

public class ExploredOverlay extends Overlay {
    private static final int SHADING_PASSES = 15;
    private static final String TAG = ExploredOverlay.class.getName();
    private List<ApproximateLocation> mLocations;
    private Context mContext;
    private Paint mRectPaint;
    private double mCurrentLat;
    private double mCurrentLong;

    private Paint mTopBarPaint;
    private Paint mCurrentPaint;
    private Paint mCirclePaint;
    private Paint mAccuracyPaint;

    private Point mTempPoint = new Point();
    private boolean mBitmapCreated;
    private Bitmap mCover;
    private Canvas mCoverCanvas;

    private Paint mTextPaint = new Paint();
    private Paint mAlertPaint = new Paint();
    private Rect mTopBar;

    private Rect mScreenCover;
    private double mCurrentAccuracy;
    private Paint mShadePaint;

    public ExploredOverlay(Context context) {
        this.mContext = context;

        mTopBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTopBarPaint.setColor(Color.WHITE);
        mTopBarPaint.setAlpha(120);
        mTopBarPaint.setStyle(Style.FILL_AND_STROKE);

        mAccuracyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAccuracyPaint.setColor(Color.RED);
        mAccuracyPaint.setAlpha(70);
        mAccuracyPaint.setStyle(Style.FILL_AND_STROKE);

        mRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRectPaint.setColor(Color.BLACK);
        mRectPaint.setStyle(Style.FILL_AND_STROKE);

        mCurrentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurrentPaint.setColor(Color.BLUE);
        mCurrentPaint.setStyle(Style.STROKE);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set PorterDuff mode in order to create transparent holes in
        // the canvas
        // see
        // http://developer.android.com/reference/android/graphics/PorterDuff.Mode.html
        // see http://en.wikipedia.org/wiki/Alpha_compositing
        // see
        // http://groups.google.com/group/android-developers/browse_thread/thread/5b0a498664b17aa0/de4aab6fb7e97e38?lnk=gst&q=erase+transparent&pli=1
        mCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCirclePaint.setAlpha(255);
        mCirclePaint.setColor(Color.BLACK);
        mCirclePaint.setStyle(Style.FILL_AND_STROKE);

        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(15);
        mTextPaint.setFakeBoldText(true);

        mAlertPaint.setAntiAlias(true);
        mAlertPaint.setTextSize(15);
        mAlertPaint.setFakeBoldText(true);
        mAlertPaint.setColor(Color.RED);

        mShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        mShadePaint.setColor(Color.BLACK);
        mShadePaint.setStyle(Style.FILL);
        mShadePaint.setAlpha(220);

    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {

        super.draw(canvas, mapView, shadow);
        assert !shadow : "The overlay does not need a shadow as it only covers the explored areas.";
        Projection projection = mapView.getProjection();

        // top bar rectangle

        mTopBar = new Rect(0, 0, mapView.getMeasuredWidth(), 30);

        // the size of the displayed area is dependent on the zoom level
        // 1 - 19 levels
        // level 19 = 4 pixels/meter
        // level 18 = 2 pixels/meter
        // level 17 = 1 pixel/meter
        // etc

        // 4 / 2 ^ (21 - level)

        final double pixelsMeter = 4d / Math.pow(2, 19 - mapView.getZoomLevel());
        int radius = (int) ((double) LocationOrder.METERS_RADIUS * pixelsMeter);
        radius = (radius <= 1) ? 1 : radius;

        int accuracy = (int) ((double) mCurrentAccuracy * pixelsMeter);
        accuracy = (accuracy <= 1) ? 1 : accuracy;

        Log.v(TAG, "View distance is " + LocationOrder.METERS_RADIUS
                + " meters, radius in pixels is "
                + radius + " pixel per meter is " + pixelsMeter);
        if (!mBitmapCreated) {
            mCover = Bitmap.createBitmap(mapView.getMeasuredWidth(), mapView.getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888);
            mCoverCanvas = new Canvas(mCover);
            // TODO check is width, height is always the same - rotation may be
            // a problem
            mScreenCover = new Rect(0, 0, mapView.getMeasuredWidth(), mapView.getMeasuredHeight());

            mBitmapCreated = true;
        } else {
            mCover.eraseColor(Color.TRANSPARENT);
        }

        mCoverCanvas.drawRect(mScreenCover, mRectPaint);

        for (ApproximateLocation location : mLocations) {
            // XXX BUG - do not use
            // point = mapView.getProjection().toPixels(geoPoint, null);
            // returns an incorrect value in point
            // you'll cry debugger tears if you do
            projection.toPixels(locationToGeoPoint(location), mTempPoint);
            // Log.v(TAG, "GeoPoint to screen point: " + mTempPoint);
            // for display use only visible points
            if (mTempPoint.x >= 0 && mTempPoint.x <= mapView.getWidth() && mTempPoint.y >= 0
                    && mTempPoint.y <= mapView.getHeight()) {

                for (int i = 0; i < SHADING_PASSES; i++) {
                    mCoverCanvas.drawCircle(mTempPoint.x, mTempPoint.y, (SHADING_PASSES - i) * radius / SHADING_PASSES *0.8f + radius * 0.2f,
                            mShadePaint);
                }

            }

        }

//        for (ApproximateLocation location : mLocations) {
//            // XXX BUG - do not use
//            // point = mapView.getProjection().toPixels(geoPoint, null);
//            // returns an incorrect value in point
//            // you'll cry debugger tears if you do
//            projection.toPixels(locationToGeoPoint(location), mTempPoint);
//            // Log.v(TAG, "GeoPoint to screen point: " + mTempPoint);
//            // for display use only visible points
//            if (mTempPoint.x >= 0 && mTempPoint.x <= mapView.getWidth() && mTempPoint.y >= 0
//                    && mTempPoint.y <= mapView.getHeight()) {
//                mCoverCanvas.drawCircle(mTempPoint.x, mTempPoint.y, radius * 0.4f, mCirclePaint);
//            }
//
//        }

        // draw blue location circle
        projection.toPixels(new GeoPoint((int) (mCurrentLat * 1e6), (int) (mCurrentLong * 1e6)),
                mTempPoint);
        mCoverCanvas.drawCircle(mTempPoint.x, mTempPoint.y, radius, mCurrentPaint);
        mCoverCanvas.drawCircle(mTempPoint.x, mTempPoint.y, accuracy, mAccuracyPaint);

        SharedPreferences settings = mContext.getSharedPreferences(Settings.UMBRA_PREFS, 0);
        mRectPaint.setAlpha(settings.getInt(Settings.TRANSPARENCY, 120));

        canvas.drawBitmap(mCover, 0, 0, mRectPaint);
        canvas.drawRect(mTopBar, mTopBarPaint);

        String accuracyText = LocationUtilities.getFormattedDistance(mCurrentAccuracy,
                settings.getBoolean(Settings.MEASUREMENT_SYSTEM, false));

        if (mCurrentAccuracy < LocationOrder.METERS_RADIUS * 2) {
            canvas.drawText(" Accuracy: " + accuracyText, 17, 19,
                    mTextPaint);
        } else {
            canvas.drawText(" Accuracy is too low: " + accuracyText, 17, 19,
                    mAlertPaint);
        }

        // super.draw(canvadb des, mapView, false);
    }

    public void setExplored(List<ApproximateLocation> locations) {
        this.mLocations = locations;
        Log.d(TAG, "Explored size is: " + this.mLocations.size());
    }

    public void setCurrent(double currentLat, double currentLong, double currentAccuracy) {
        this.mCurrentLat = currentLat;
        this.mCurrentLong = currentLong;
        this.mCurrentAccuracy = currentAccuracy;
    }
}
