package pl.tdf.atak.TAKWatch.plugin;

import java.util.Arrays;
import java.util.List;

public class TAKWatchConst {

    public static final String DETAILS_META_KEY_MAX_HEART_RATE = "TAKWatchPlugin.maxHeartRate";
    public static final String DETAILS_META_KEY_AVG_HEART_RATE = "TAKWatchPlugin.averageHeartRate";
    public static final String DETAILS_META_KEY_TIME_RANGE = "TAKWatchPlugin.timeRange";
    public static final String DETAILS_META_KEY_LAST_UPDATED = "TAKWatchPlugin.lastUpdated";

    public static final String OPEN_PREFERENCES_ACTION = "pl.tdf.atak.TAKWatch.plugin.openPreferences";


    public static List<String> supportedTypes = Arrays.asList(new String[]{
            "a-f-G",
            "a-n-G",
            "a-h-G",
            "a-u-G",
            "b-m-p-s-p-i",
            "a-f-G-U-C",
            "a-f-G-U",
            "a-f-G-E-V-C"
    });
}
