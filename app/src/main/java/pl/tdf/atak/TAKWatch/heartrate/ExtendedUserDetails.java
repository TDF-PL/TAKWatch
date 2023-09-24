package pl.tdf.atak.TAKWatch.heartrate;

import static java.lang.String.format;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_AVG_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_LAST_UPDATED;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_MAX_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_TIME_RANGE;

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
                TextView label = new TextView(pluginContext);
                label.setText("Heart rate");
                label.setTextColor(Color.parseColor("#d4b246"));
                label.setTextSize(10);
                label.setPadding(0, 0, 0, 0);

                TextView heartBeatTextField = new TextView(viewContext);
                heartBeatTextField.setLayoutParams(new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT));
                heartBeatTextField.setTextColor(Color.WHITE);

                LinearLayout parent = new LinearLayout(pluginContext);
                parent.setOrientation(VERTICAL);
                parent.addView(label);
                parent.addView(heartBeatTextField);
                this.addView(parent);


                int maxHeartRate = m.getMetaInteger(DETAILS_META_KEY_MAX_HEART_RATE, -1);
                int averageHeartRate = m.getMetaInteger(DETAILS_META_KEY_AVG_HEART_RATE, -1);
                int timeRange = m.getMetaInteger(DETAILS_META_KEY_TIME_RANGE, -1);
                String lastUpdated = m.getMetaString(DETAILS_META_KEY_LAST_UPDATED, "Unknown");

                if (maxHeartRate == -1) {
                    heartBeatTextField.setText("No data");
                } else {
                    heartBeatTextField.setText(format("Max : %d \nAvg: %d\nInterval: %ds \nLast updated: %s", maxHeartRate, averageHeartRate, timeRange, lastUpdated));
                }

            }
        };
    }
}
