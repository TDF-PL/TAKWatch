package pl.tdf.atak.TAKWatch.heartrate;

import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static java.lang.String.format;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_AVG_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_LAST_UPDATED;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_MAX_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_TIME_RANGE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_WATCH_BATTERY;

import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.contact.ContactLocationView;
import com.atakmap.android.cotdetails.ExtendedInfoView;
import com.atakmap.android.maps.PointMapItem;

public class ExtendedUserDetails implements ContactLocationView.ExtendedSelfInfoFactory {

    private static final String TAG = "TAKWatchUserDetails";
    private final Context pluginContext;
    private final Context viewContext;

    public ExtendedUserDetails(Context pluginContext, Context viewContext) {
        this.pluginContext = pluginContext;
        this.viewContext = viewContext;
    }

    @Override
    public ExtendedInfoView createView() {
        return new ExtendedInfoView(viewContext) {
            @Override
            public void setMarker(PointMapItem m) {
                TextView hrLabel = new TextView(pluginContext);
                hrLabel.setText("Heart rate");
                hrLabel.setTextColor(Color.parseColor("#d4b246"));
                hrLabel.setTextSize(10);
                hrLabel.setPadding(0, 0, 0, 0);
                hrLabel.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

                TextView hrValue = new TextView(viewContext);
                hrValue.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                hrValue.setTextColor(Color.WHITE);

                LinearLayout hrParent = new LinearLayout(pluginContext);
                hrParent.setOrientation(VERTICAL);
                hrParent.addView(hrLabel);
                hrParent.addView(hrValue);

                TextView watchBatteryLabel = new TextView(pluginContext);
                watchBatteryLabel.setText("Watch battery");
                watchBatteryLabel.setTextColor(Color.parseColor("#d4b246"));
                watchBatteryLabel.setTextSize(10);
                watchBatteryLabel.setPadding(0, 0, 0, 0);
                watchBatteryLabel.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

                TextView watchBatteryValue = new TextView(viewContext);
                watchBatteryValue.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                watchBatteryValue.setTextColor(Color.WHITE);

                LinearLayout watchBatteryParent = new LinearLayout(pluginContext);
                watchBatteryParent.setOrientation(VERTICAL);
                watchBatteryParent.addView(watchBatteryLabel);
                watchBatteryParent.addView(watchBatteryValue);
                watchBatteryParent.setPadding(16, 0, 0, 0);

                LinearLayout container = new LinearLayout(pluginContext);
                container.setOrientation(HORIZONTAL);
                container.addView(hrParent);
                container.addView(watchBatteryParent);
                this.addView(container);


                int maxHeartRate = m.getMetaInteger(DETAILS_META_KEY_MAX_HEART_RATE, -1);
                int averageHeartRate = m.getMetaInteger(DETAILS_META_KEY_AVG_HEART_RATE, -1);
                int timeRange = m.getMetaInteger(DETAILS_META_KEY_TIME_RANGE, -1);
                String lastUpdated = m.getMetaString(DETAILS_META_KEY_LAST_UPDATED, "Unknown");
                int watchBattery = m.getMetaInteger(DETAILS_META_KEY_WATCH_BATTERY, -1);

                if (maxHeartRate == -1) {
                    hrValue.setText("No data");
                } else {
                    hrValue.setText(format("Max : %d \nAvg: %d\nInterval: %ds \nLast updated: %s", maxHeartRate, averageHeartRate, timeRange, lastUpdated));
                }

                if (watchBattery == -1) {
                    watchBatteryValue.setText("No data");
                } else {
                    watchBatteryValue.setText(watchBattery+"%");
                }

            }
        };
    }
}
