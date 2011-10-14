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

import static org.unchiujar.umbra.LocationUtilities.locationToGeoPoint;

import java.util.List;

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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class ExploredOverlay extends Overlay {
    private static final String TAG = ExploredOverlay.class.getName();
    private List<AproximateLocation> locations;
    private Context context;
    private Paint rectPaint;
    private double currentLat;
    private double currentLong;

    private Paint topBarPaint;
    private Paint currentPaint;
    private Paint circlePaint;
    private Paint accuracyPaint;

    private Point tempPoint = new Point();
    private boolean bitmapCreated;
    private Bitmap cover;
    private Canvas coverCanvas;

    // TODO remove - DEBUG code
    private Paint textPaint = new Paint();
    private int i;
    // TODO remove - DEBUG code
    private Rect topBar;

    private Rect screenCover;
    private double currentAccuracy;

    public ExploredOverlay(Context context) {
        this.context = context;


        topBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        topBarPaint.setColor(Color.WHITE);
        topBarPaint.setAlpha(120);
        topBarPaint.setStyle(Style.FILL_AND_STROKE);


        accuracyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accuracyPaint.setColor(Color.RED);
        accuracyPaint.setAlpha(70);
        accuracyPaint.setStyle(Style.FILL_AND_STROKE);

        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(Color.BLACK);
        rectPaint.setStyle(Style.FILL_AND_STROKE);

        currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentPaint.setColor(Color.BLUE);
        currentPaint.setStyle(Style.STROKE);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set PorterDuff mode in order to create transparent holes in
        // the canvas
        // see http://developer.android.com/reference/android/graphics/PorterDuff.Mode.html
        // see http://en.wikipedia.org/wiki/Alpha_compositing
        // see
        // http://groups.google.com/group/android-developers/browse_thread/thread/5b0a498664b17aa0/de4aab6fb7e97e38?lnk=gst&q=erase+transparent&pli=1
        circlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        circlePaint.setAlpha(255);
        circlePaint.setColor(Color.BLACK);
        circlePaint.setStyle(Style.FILL_AND_STROKE);

        // TODO remove - DEBUG code
        i = -1000000000;
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(15);
        // TODO remove - DEBUG code

    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {

        super.draw(canvas, mapView, shadow);

        Log.d(TAG, "########Called");
        assert !shadow : "The overlay does not need a shadow as it only covers the explored areas.";
        Projection projection = mapView.getProjection();

        //top bar rectangle
        
        topBar = new Rect(0, 0, mapView.getMeasuredWidth(), 30);

        // the size of the displayed area is dependent on the zoom level
        // 1 - 19 levels
        // level 19 = 4 pixels/meter
        // level 18 = 2 pixels/meter
        // level 17 = 1 pixel/meter
        // etc

        // 4 / 2 ^ (21 - level)

        double pixelsMeter = 4d / Math.pow(2, 19 - mapView.getZoomLevel());
        int radius = (int) ((double) LocationOrder.METERS_RADIUS * pixelsMeter);
        radius = (radius <= 1) ? 1 : radius;

        int accuracy = (int) ((double) currentAccuracy * pixelsMeter);
        accuracy = (accuracy <= 1) ? 1 : accuracy;


        Log.v(TAG, "View distance is " + LocationOrder.METERS_RADIUS + " meters, radius in pixels is "
                + radius + " pixel per meter is " + pixelsMeter);
        if (!bitmapCreated) {
            cover = Bitmap.createBitmap(mapView.getMeasuredWidth(), mapView.getMeasuredHeight(),
                    Bitmap.Config.ALPHA_8);
            coverCanvas = new Canvas(cover);
            // TODO check is width, height is always the same - rotation may be a problem
            screenCover = new Rect(0, 0, mapView.getMeasuredWidth(), mapView.getMeasuredHeight());

            bitmapCreated = true;
        } else {
            cover.eraseColor(Color.TRANSPARENT);
        }

        coverCanvas.drawRect(screenCover, rectPaint);
        
        
        for (AproximateLocation location : locations) {
            // BUG - do not use
            // point = mapView.getProjection().toPixels(geoPoint, null);
            // returns an incorrect value in point
            // you'll cry debugger tears if you do
            projection.toPixels(locationToGeoPoint(location), tempPoint);
            // Log.v(TAG, "GeoPoint to screen point: " + tempPoint);
            // for display use only visible points
            if (tempPoint.x >= 0 && tempPoint.x <= mapView.getWidth() && tempPoint.y >= 0
                    && tempPoint.y <= mapView.getHeight()) {
                coverCanvas.drawCircle(tempPoint.x, tempPoint.y, radius, circlePaint);
            }

        }
        // draw blue location circle
        projection.toPixels(new GeoPoint((int) (currentLat * 1e6), (int) (currentLong * 1e6)), tempPoint);
        coverCanvas.drawCircle(tempPoint.x, tempPoint.y, radius, currentPaint);
        coverCanvas.drawCircle(tempPoint.x, tempPoint.y, accuracy, accuracyPaint);

        
        SharedPreferences settings = context.getSharedPreferences(Settings.UMBRA_PREFS, 0);
        rectPaint.setAlpha(settings.getInt(Settings.TRANSPARENCY, 120));

        canvas.drawBitmap(cover, 0, 0, rectPaint);
        canvas.drawRect(topBar, topBarPaint);
        canvas.drawText("Redraw:" + i++ + " | Accuracy: " + currentAccuracy + " m ", 17, 17, textPaint);

        // super.draw(canvadb des, mapView, false);
    }

    public void setExplored(List<AproximateLocation> locations) {
        this.locations = locations;
    }

    public void setCurrent(double currentLat, double currentLong, double currentAccuracy) {
        this.currentLat = currentLat;
        this.currentLong = currentLong;
        this.currentAccuracy = currentAccuracy;
    }
}
