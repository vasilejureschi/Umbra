package org.unchiujar.umbra.overlays;

import android.content.Context;
import android.graphics.*;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra.activities.Preferences;
import org.unchiujar.umbra.location.ApproximateLocation;
import org.unchiujar.umbra.location.LocationOrder;
import org.unchiujar.umbra.utils.LocationUtilities;

import java.util.List;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.Style;

public class OverlayFactory {

    private static final int DOWN_SCALE_FACTOR = 8;
    private static final int SHADING_PASSES = 15;
    private static final String TAG = ExploredOverlay.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayFactory.class);
    /**
     * Transparency level of explored area. Lower value means more transparent.
     */
    public static final int TRANSPARENCY = 170;
    private static int mAlpha;
    private List<ApproximateLocation> mLocations;
    private Paint mRectPaint;

    private Paint mClearPaint;

    private boolean mBitmapCreated;
    private Bitmap mCover;
    private Canvas mCoverCanvas;

    private Rect mScreenCover;
    private Paint mShadePaint;

    private static OverlayFactory instance;

    private OverlayFactory() {
        mRectPaint = new Paint(ANTI_ALIAS_FLAG);
        mRectPaint.setColor(BLACK);
        mRectPaint.setStyle(Style.FILL_AND_STROKE);

        mClearPaint = new Paint(ANTI_ALIAS_FLAG);
        // set PorterDuff mode in order to create transparent holes in
        // the canvas
        // see http://developer.android.com/reference/android/graphics/PorterDuff.Mode.html
        // see http://en.wikipedia.org/wiki/Alpha_compositing
        // see http://groups.google.com/group/android-developers/browse_thread/thread/5b0a498664b17aa0/de4aab6fb7e97e38?lnk=gst&q=erase+transparent&pli=1
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mClearPaint.setAlpha(255);
        mClearPaint.setColor(BLACK);
        mClearPaint.setStyle(Style.FILL_AND_STROKE);

        mShadePaint = new Paint(ANTI_ALIAS_FLAG);
        mShadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        mShadePaint.setColor(BLACK);
        mShadePaint.setStyle(Style.FILL);
        mShadePaint.setAlpha(TRANSPARENCY);

    }

    public static OverlayFactory getInstance(Context context) {
        if (instance == null) {
            instance = new OverlayFactory();
        }
        //FIXME if an instance is initialized and then used the alpha value will not be updated as getInstance is not called
        // use a settings listener to update this value ?
        mAlpha = 255 - PreferenceManager.getDefaultSharedPreferences(context).getInt(Preferences.TRANSPARENCY, 120);
        return instance;
    }


    public GroundOverlayOptions getCompleteOverlay(GoogleMap map) {
        Projection projection = map.getProjection();
        VisibleRegion vr = projection.getVisibleRegion();

        // construct bitmap descriptor
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(draw(map));

        return new GroundOverlayOptions()
                .image(descriptor)
                .positionFromBounds(vr.latLngBounds).zIndex(Float.MAX_VALUE);
    }


    public double pixelsPerMeter(int width, LatLng start, LatLng stop) {
        //calculate distance in meters
        float[] results = new float[3];
        Location.distanceBetween(start.latitude, start.longitude, stop.latitude, stop.longitude, results);
        return width / results[0] * DOWN_SCALE_FACTOR;
    }

    public Bitmap draw(GoogleMap map) {

        Projection projection = map.getProjection();

        VisibleRegion vr = projection.getVisibleRegion();
        Point farLeft = projection.toScreenLocation(vr.farLeft);
        Point farRight = projection.toScreenLocation(vr.farRight);
        Point nearLeft = projection.toScreenLocation(vr.nearLeft);
        Point nearRight = projection.toScreenLocation(vr.nearRight);

        LOGGER.debug("Far left {} ", farLeft);
        LOGGER.debug("Far right {} ", farRight);
        LOGGER.debug("Near left {} ", nearLeft);
        LOGGER.debug("Near right {} ", nearRight);

        // get the screen width in pixels
        int width = (int) Math.sqrt(Math.pow(farLeft.x - farRight.x, 2) + Math.pow(farLeft.y - farRight.y, 2));
        width = width / DOWN_SCALE_FACTOR;
        // get the screen height in pixels
        int height = (int) Math.sqrt(Math.pow(farLeft.x - nearLeft.x, 2) + Math.pow(farLeft.y - nearLeft.y, 2));
        height = height / DOWN_SCALE_FACTOR;

        LOGGER.debug("Width is {} height is {} ", width, height);
        int zoom = (int) map.getCameraPosition().zoom;
        int passes = zoom < 14 ? 1 : zoom - 8;
        Log.v(TAG, "Shading passes " + passes + " zoom level " + zoom);

        // zoom level, in the range of 2.0 to 21.0. Values below this range are set to 2.0,
        // and values above it are set to 21.0. Increase the value to zoom in.
        // Not all areas have tiles at the largest zoom levels.

        // the size of the displayed area is dependent on the zoom level
        // 2 - 21 levels

        final double pixelsMeter = pixelsPerMeter(width, vr.nearLeft, vr.nearRight);
        int radius = (int) (LocationOrder.METERS_RADIUS * pixelsMeter);
        radius = radius / DOWN_SCALE_FACTOR;

        radius = (radius <= 3) ? 3 : radius;

        Log.v(TAG, "View distance is " + LocationOrder.METERS_RADIUS
                + " meters, radius in pixels is " + radius
                + " pixel per meter is " + pixelsMeter);
        if (!mBitmapCreated) {
            mCover = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCoverCanvas = new Canvas(mCover);

            // TODO check is width, height is always the same - rotation may be
            // a problem
            mScreenCover = new Rect(0, 0, width, height);

            mBitmapCreated = true;
        } else {
            mCover.eraseColor(TRANSPARENT);
        }

        mCoverCanvas.drawRect(mScreenCover, mRectPaint);
        LOGGER.debug("Processing locations {}", mLocations);
        for (ApproximateLocation location : mLocations) {

            Point currentPoint = projection.toScreenLocation(LocationUtilities.locationToLatLng(location));
            currentPoint.set(currentPoint.x / DOWN_SCALE_FACTOR, currentPoint.y / DOWN_SCALE_FACTOR);

            // Log.v(TAG, "GeoPoint to screen point: " + mTempPoint);
            // for display use only visible points
            if (vr.latLngBounds.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                drawShadedDisc(radius, passes, currentPoint);

            }

        }

        mRectPaint.setAlpha(mAlpha);

        mCoverCanvas.drawBitmap(mCover, 0, 0, mRectPaint);


        return mCover;
    }

    /**
     * Calculate shading passes from zoom level
     * if the map is zoom out do not try to shade so much
     * shading passes are equivalent to zoom level minus a constant
     * after a certain zoom level only clear the area instead of
     * shading
     */

    private void drawShadedDisc(int radius, int passes, Point currentPoint) {
        int x = currentPoint.x;
        int y = currentPoint.y;
        // if the passes are only one do not shade, just clear
        if (passes == 1) {
            mCoverCanvas.drawCircle(x, y, radius, mClearPaint);
            return;
        }

        for (int i = 0; i < passes; i++) {
            mCoverCanvas.drawCircle(x, y, (SHADING_PASSES - i) * radius / SHADING_PASSES * 0.8f + radius * 0.2f, mShadePaint);
        }
    }

    public void setExplored(List<ApproximateLocation> locations) {
        this.mLocations = locations;
        Log.d(TAG, "Explored size is: " + this.mLocations.size());
    }

}
