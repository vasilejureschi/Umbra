package org.unchiujar.umbra.overlays;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra.activities.Preferences;
import org.unchiujar.umbra.location.ApproximateLocation;
import org.unchiujar.umbra.location.LocationOrder;
import org.unchiujar.umbra.utils.LocationUtilities;

import java.util.List;

import static android.graphics.Color.*;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.Style;

public class OverlayFactory {

    private static final int SHADING_PASSES = 18;
    private static final String TAG = ExploredOverlay.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayFactory.class);
    private List<ApproximateLocation> mLocations;
    private Context mContext;
    private Paint mRectPaint;

    private Paint mTopBarPaint;
    private Paint mCurrentPaint;
    private Paint mClearPaint;
    private Paint mAccuracyPaint;

    private Point mTempPoint = new Point();
    private boolean mBitmapCreated;
    private Bitmap mCover;
    private Canvas mCoverCanvas;

    private Paint mTextPaint = new Paint();
    private Paint mAlertPaint = new Paint();
    private Rect mTopBar;

    private Rect mScreenCover;
    private Paint mShadePaint;
    private SharedPreferences mSettings;

    private static OverlayFactory instance;

    private OverlayFactory(Context context) {
        mContext = context;

        mTopBarPaint = new Paint(ANTI_ALIAS_FLAG);
        mTopBarPaint.setColor(WHITE);
        mTopBarPaint.setAlpha(120);
        mTopBarPaint.setStyle(Style.FILL_AND_STROKE);

        mAccuracyPaint = new Paint(ANTI_ALIAS_FLAG);
        mAccuracyPaint.setColor(RED);
        mAccuracyPaint.setAlpha(70);
        mAccuracyPaint.setStyle(Style.FILL_AND_STROKE);

        mRectPaint = new Paint(ANTI_ALIAS_FLAG);
        mRectPaint.setColor(BLACK);
        mRectPaint.setStyle(Style.FILL_AND_STROKE);

        mCurrentPaint = new Paint(ANTI_ALIAS_FLAG);
        mCurrentPaint.setColor(BLUE);
        mCurrentPaint.setStyle(Style.STROKE);

        mClearPaint = new Paint(ANTI_ALIAS_FLAG);
        // set PorterDuff mode in order to create transparent holes in
        // the canvas
        // see
        // http://developer.android.com/reference/android/graphics/PorterDuff.Mode.html
        // see http://en.wikipedia.org/wiki/Alpha_compositing
        // see
        // http://groups.google.com/group/android-developers/browse_thread/thread/5b0a498664b17aa0/de4aab6fb7e97e38?lnk=gst&q=erase+transparent&pli=1
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mClearPaint.setAlpha(255);
        mClearPaint.setColor(BLACK);
        mClearPaint.setStyle(Style.FILL_AND_STROKE);

        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(15);
        mTextPaint.setFakeBoldText(true);

        mAlertPaint.setAntiAlias(true);
        mAlertPaint.setTextSize(15);
        mAlertPaint.setFakeBoldText(true);
        mAlertPaint.setColor(RED);

        mShadePaint = new Paint(ANTI_ALIAS_FLAG);
        mShadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        mShadePaint.setColor(BLACK);
        mShadePaint.setStyle(Style.FILL);
        mShadePaint.setAlpha(220);
        mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

    }

    public static OverlayFactory getInstance(Context context) {
        if (instance == null) {


            instance = new OverlayFactory(context);
        }

        return instance;
    }


    public GroundOverlayOptions getCompleteOverlay(GoogleMap map) {

        VisibleRegion vr = map.getProjection().getVisibleRegion();
        double left = vr.latLngBounds.southwest.longitude;
        double top = vr.latLngBounds.northeast.latitude;
        double right = vr.latLngBounds.northeast.longitude;
        double bottom = vr.latLngBounds.southwest.latitude;

        // construct bitmap descriptor
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(draw(map));

        return new GroundOverlayOptions()
                .image(descriptor)
                .positionFromBounds(vr.latLngBounds)
                .transparency(0.5f);
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


        int width = (int) Math.sqrt(Math.pow(farLeft.x - farRight.x, 2) + Math.pow(farLeft.y - farRight.y, 2));
        int height = (int) Math.sqrt(Math.pow(farLeft.x - nearLeft.x, 2) + Math.pow(farLeft.y - nearLeft.y, 2));

        LOGGER.debug("Width is {} height is {} ", width, height);
        int zoom = (int) map.getCameraPosition().zoom;
        LOGGER.debug("Zoom is {}", zoom);

        // top bar rectangle

        mTopBar = new Rect(0, 0, width, 30);

        // the size of the displayed area is dependent on the zoom level
        // 1 - 19 levels
        // level 19 = 4 pixels/meter
        // level 18 = 2 pixels/meter
        // level 17 = 1 pixel/meter
        // etc

        // 4 / 2 ^ (21 - level)

        final double pixelsMeter = 4d / Math.pow(2, 19 - zoom);
        int radius = (int) (LocationOrder.METERS_RADIUS * pixelsMeter);
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

        for (ApproximateLocation location : mLocations) {

            mTempPoint = projection.toScreenLocation(LocationUtilities.locationToLatLng(location));
            // Log.v(TAG, "GeoPoint to screen point: " + mTempPoint);
            // for display use only visible points
            if (mTempPoint.x >= 0 && mTempPoint.x <= width
                    && mTempPoint.y >= 0 && mTempPoint.y <= height) {

                // calculate shading passes from zoom level
                // if the map is zoom out do not try to shade so much
                // shading passes are equivalent to zoom level minus a constant
                // after a certain zoom level only clear the area instead of
                // shading
                int passes = zoom < 14 ? 1 : zoom - 8;
                Log.v(TAG, "Shading passes " + passes + " zoom level "
                        + zoom);

                // if the passes are only one do not shade, just clear
                if (passes != 1) {
                    for (int i = 0; i < passes; i++) {
                        mCoverCanvas.drawCircle(mTempPoint.x, mTempPoint.y,
                                (SHADING_PASSES - i) * radius / SHADING_PASSES
                                        * 0.8f + radius * 0.2f, mShadePaint
                        );
                    }
                } else {
                    mCoverCanvas.drawCircle(mTempPoint.x, mTempPoint.y, radius,
                            mClearPaint);

                }

            }

        }

        mRectPaint.setAlpha(255 - mSettings.getInt(Preferences.TRANSPARENCY,
                120));

        mCoverCanvas.drawBitmap(mCover, 0, 0, mRectPaint);
        mCoverCanvas.drawRect(mTopBar, mTopBarPaint);

        int accuracy = 1;


        String accuracyText = LocationUtilities.getFormattedDistance(
                accuracy,
                mSettings.getBoolean(Preferences.MEASUREMENT_SYSTEM, false));

        if (accuracy < LocationOrder.METERS_RADIUS * 2) {
            mCoverCanvas.drawText(" Accuracy: " + accuracyText, 17, 19, mTextPaint);
        } else {
            mCoverCanvas.drawText(" Accuracy is too low: " + accuracyText, 17, 19,
                    mAlertPaint);
        }

        return mCover;
    }

    public void setExplored(List<ApproximateLocation> locations) {
        this.mLocations = locations;
        Log.d(TAG, "Explored size is: " + this.mLocations.size());
    }
}
