package pl.tdf.atak.TAKWatch.heartrate;

import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_AVG_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_LAST_UPDATED;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_MAX_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_TIME_RANGE;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.maps.MapItem;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;

public class HeartRateCodHandler extends CotDetailHandler {

    public static final String HEART_RATE_COD_KEY = "HEART_RATE_COD_KEY";

    public HeartRateCodHandler() {
        super(HEART_RATE_COD_KEY);
    }

    @Override
    public CommsMapComponent.ImportResult toItemMetadata(
            MapItem item, CotEvent event, CotDetail detail) {
        String maxHeartRateString = detail.getAttribute(DETAILS_META_KEY_MAX_HEART_RATE);
        String averageHeartRateString = detail.getAttribute(DETAILS_META_KEY_AVG_HEART_RATE);
        String timeRangeString = detail.getAttribute(DETAILS_META_KEY_TIME_RANGE);
        String lastUpdatedString = detail.getAttribute(DETAILS_META_KEY_LAST_UPDATED);

        if (FileSystemUtils.isEmpty(maxHeartRateString) || FileSystemUtils.isEmpty(averageHeartRateString)) {
            return CommsMapComponent.ImportResult.FAILURE;
        }

        int maxHeartRate = Integer.parseInt(maxHeartRateString);
        int averageHeartRate = Integer.parseInt(averageHeartRateString);
        int timeRange = Integer.parseInt(timeRangeString);

        item.setMetaInteger(DETAILS_META_KEY_MAX_HEART_RATE, maxHeartRate);
        item.setMetaInteger(DETAILS_META_KEY_AVG_HEART_RATE, averageHeartRate);
        item.setMetaInteger(DETAILS_META_KEY_TIME_RANGE, timeRange);
        item.setMetaString(DETAILS_META_KEY_LAST_UPDATED, lastUpdatedString);

        return CommsMapComponent.ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event,
                               CotDetail root) {
        return true;
    }
}
