package pl.tdf.atak.TAKWatch.statushb;

import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

import pl.tdf.atak.TAKWatch.WatchClient;

public class StatusHeartBeatTask extends TimerTask {

    private static final String TAG = "TAKWatchStatusHBTask";
    private final WatchClient watchClient;

    public StatusHeartBeatTask(WatchClient watchClient) {
        this.watchClient = watchClient;
    }

    @Override
    public void run() {
        List<Object> msg = Arrays.asList(new Object[]{"hb"});
        try {
            watchClient.sendMessageToWatch(msg);
        } catch (Exception e) {
            Log.e(TAG, "Error while sending HB message to watch", e);
        }
    }
}
