package org.unchiujar.explorer;

import static org.unchiujar.explorer.LocationUtilities.locationToGeoPoint;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ExploredOverlay extends Overlay {
    private static final String TAG = ExploredOverlay.class.getName();
    private List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();
    private Context context;
    private List<Point> screenPts;
    private boolean pointsUpdated;

    public ExploredOverlay(Context context) {
        this.context = context;
    }

    // @Override
    // public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
    // // TODO Auto-generated method stub
    // return super.draw(canvas, mapView, shadow, when);
    // }

    @Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when ) {

        assert !shadow : "The overlay does not need a shadow as it only covers the explored areas.";

        if (pointsUpdated) {
            // translate the GeoPoint to screen pixels
            screenPts = new ArrayList<Point>(geoPoints.size());
            for (GeoPoint geoPoint : geoPoints) {

                // BUG - do not use
                // point = mapView.getProjection().toPixels(geoPoint, null);
                // returns an incorrect value in point
                // you'll cry debugger tears if you do
                Point point = new Point();
                mapView.getProjection().toPixels(geoPoint, point);
                Log.v(TAG, "GeoPoint: " + geoPoint + " converts to screen point: " + point);
                // for processing add only visible points
                if (point.x >= 0 && point.x <= mapView.getWidth() && point.y >= 0
                        && point.y <= mapView.getHeight()) {
                    screenPts.add(point);
                }

            }
            Log.d(TAG, "Number of screen points added: " + screenPts.size());
        }
        //	
        // the size of the displayed area is dependent on the zoom level
        // 1 - 19 levels
        // level 19 = 4 pixels/meter
        // level 18 = 2 pixels/meter
        // level 17 = 1 pixel/meter
        // etc

        // 4 / 2 ^ (21 - level)

        int viewDistance = 50;
        double pixelsMeter = 4d / Math.pow(2, 21 - mapView.getZoomLevel());
        int radius = (int) ((double) viewDistance * pixelsMeter);
        radius = (radius <= 2) ? 3 : radius;

        Log.v(TAG, "View distance is " + viewDistance + " meters, radius in pixels is " + radius
                + " pixel per meter is " + pixelsMeter);
        //TODO research what PorterDuffMode does and set to simulate transparency
        Paint rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        rectPaint.setColor(Color.DKGRAY);
        rectPaint.setAlpha(50);
        rectPaint.setStyle(Style.FILL_AND_STROKE);
        Rect screenCover = new Rect(0, 0, mapView.getMeasuredWidth(), mapView.getMeasuredHeight());
        canvas.drawRect(screenCover, rectPaint);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // set PorterDuff mode in order to create transparent holes in
        // the canvas
        // see http://developer.android.com/reference/android/graphics/PorterDuff.Mode.html
        // see http://en.wikipedia.org/wiki/Alpha_compositing
        // see
        // http://groups.google.com/group/android-developers/browse_thread/thread/5b0a498664b17aa0/de4aab6fb7e97e38?lnk=gst&q=erase+transparent&pli=1
        circlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
//        circlePaint.setAlpha(0);
        circlePaint.setColor(Color.RED);
        circlePaint.setStyle(Style.FILL_AND_STROKE);

        for (Point point : screenPts) {
            canvas.drawCircle(point.x, point.y, radius, circlePaint);
        }

        pointsUpdated = false;
        // super.draw(canvas, mapView, false);
        return false;
    }

    public void setExplored(List<AproximateLocation> locations) {
        geoPoints.clear();
        Log.d(TAG, "Number of points to be displayed is :" + locations.size());
        for (AproximateLocation location : locations) {
            geoPoints.add(locationToGeoPoint(location));
        }
        pointsUpdated = true;
    }
}
