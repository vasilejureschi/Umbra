package org.unchiujar.explorer;

import static org.unchiujar.explorer.LocationUtilities.locationToGeoPoint;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.google.android.maps.Projection;

public class ExploredOverlay extends Overlay {
    private static final String TAG = ExploredOverlay.class.getName();
    private List<AproximateLocation> locations;
    private Context context;
    private boolean pointsUpdated;
    private Paint rectPaint;
    private double currentLat;
    private double currentLong;

    private Paint currentPaint;
    private Paint circlePaint;
    private Point tempPoint = new Point();
    private boolean bitmapCreated;
    private Bitmap cover;
    private Canvas coverCanvas;

    // TODO remove - DEBUG code
    private Paint paint1 = new Paint();
    private int i;
    // TODO remove - DEBUG code
    private Rect screenCover;

    public ExploredOverlay(Context context) {
        this.context = context;

        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        rectPaint.setColor(Color.BLACK);
        rectPaint.setAlpha(150);
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
        paint1.setARGB(90, 0, 0, 255); // blue
        paint1.setAntiAlias(true);
        // TODO remove - DEBUG code

    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {

        super.draw(canvas, mapView, shadow);
        canvas.drawText("redraw? " + i++, 5, 10, paint1);

        Log.d(TAG, "########Called");
        assert !shadow : "The overlay does not need a shadow as it only covers the explored areas.";
        Projection projection = mapView.getProjection();

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
        // TODO research what PorterDuffMode does and set to simulate transparency

        if (pointsUpdated) {

            if (!bitmapCreated) {
                cover = Bitmap.createBitmap(mapView.getMeasuredWidth(), mapView.getMeasuredHeight(),
                        Bitmap.Config.ALPHA_8);
                coverCanvas = new Canvas(cover);
                // TODO check is width, heigh is always the same - rotation may be a problem
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
                Log.v(TAG, "GeoPoint to screen point: " + tempPoint);
                // for display use only visible points
                if (tempPoint.x >= 0 && tempPoint.x <= mapView.getWidth() && tempPoint.y >= 0
                        && tempPoint.y <= mapView.getHeight()) {
                    coverCanvas.drawCircle(tempPoint.x, tempPoint.y, radius, circlePaint);
                }

            }
            // draw blue location circle
            projection.toPixels(new GeoPoint((int) (currentLat * 1e6), (int) (currentLong * 1e6)), tempPoint);
            coverCanvas.drawCircle(tempPoint.x, tempPoint.y, radius, currentPaint);
            canvas.drawBitmap(cover, 0, 0, rectPaint);
        }

        pointsUpdated = false;
        // super.draw(canvadb des, mapView, false);
    }

    public void setExplored(List<AproximateLocation> locations) {
        this.locations = locations;
        pointsUpdated = true;

    }

    public void setCurrent(double currentLat, double currentLong) {
        this.currentLat = currentLat;
        this.currentLong = currentLong;
    }
}
