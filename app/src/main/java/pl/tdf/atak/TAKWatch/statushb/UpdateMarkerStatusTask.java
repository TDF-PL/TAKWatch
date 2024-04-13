package pl.tdf.atak.TAKWatch.statushb;

import static com.atakmap.android.maps.MapView.getMapView;

import static pl.tdf.atak.TAKWatch.PreferenceKeys.*;

import android.content.SharedPreferences;
import android.util.Log;

import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.TimerTask;



public class UpdateMarkerStatusTask extends TimerTask {

    private static final String TAG = "TakWatchUpdaterTask";

    private GeoPoint lastUpdateLocation = getMapView().getSelfMarker().getPoint();
    private final OnUpdate callback;
    private final SharedPreferences sharedPref;
    private static final double FRACTION  = 0.1;

    public interface OnUpdate {
        void call();
    }

    public UpdateMarkerStatusTask(OnUpdate callback,SharedPreferences sharedPref ) {
        this.callback = callback;
        this.sharedPref = sharedPref;
    }
    @Override
    public void run() {
        GeoPoint currentLocation = getMapView().getSelfMarker().getPoint();

        double distance = currentLocation.distanceTo(lastUpdateLocation);
        int configuredDistance = Integer.parseInt(sharedPref.getString(PREFERENCE_KEY_SYNC_RANGE, "1000"));

        if (configuredDistance == 0) {
            return;
        }

        double triggerDistance = configuredDistance * FRACTION;

        if (distance > triggerDistance) {
            Log.d(TAG, "Distance " + triggerDistance + " exceeded. Updating markers");
            lastUpdateLocation = currentLocation;
            callback.call();
        } else {
            Log.d(TAG, "Distance " + triggerDistance + " not exceeded. Distance from last update:" + distance);
        }
    }
}
