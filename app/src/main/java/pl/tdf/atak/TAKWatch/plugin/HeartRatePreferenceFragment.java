package pl.tdf.atak.TAKWatch.plugin;

import static pl.tdf.atak.TAKWatch.PreferenceKeys.PREFERENCE_KEY_DEVICE_NAME;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.atakmap.android.gui.PanListPreference;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.garmin.android.connectiq.IQDevice;

import java.util.ArrayList;
import java.util.List;

import pl.tdf.atak.TAKWatch.WatchClient;

public class HeartRatePreferenceFragment extends PluginPreferenceFragment {

    private static Context staticPluginContext;
    private WatchClient watchClient;
    public static final String TAG = "TAKWatchPreferences";

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public HeartRatePreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    @SuppressLint("ValidFragment")
    public HeartRatePreferenceFragment(final Context pluginContext, WatchClient watchClient) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
        this.watchClient = watchClient;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<IQDevice> devices = watchClient.getDevices();

        List<String> friendlyNames = new ArrayList<>();
        Log.d(TAG, "Available devices " + devices.size());
        for (IQDevice d : devices) {
            friendlyNames.add(d.getFriendlyName());
        }
        String[] entries = friendlyNames.toArray(new String[0]);

        PanListPreference selectInput = (PanListPreference) findPreference(PREFERENCE_KEY_DEVICE_NAME);
        selectInput.setEntries(entries);
        selectInput.setEntryValues(entries);
    }
}
