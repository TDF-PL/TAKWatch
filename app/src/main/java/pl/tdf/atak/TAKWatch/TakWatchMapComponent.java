package pl.tdf.atak.TAKWatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.ContactLocationView;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.data.DataMgmtReceiver;
import com.atakmap.android.dropdown.DropDownManager;
import com.atakmap.android.emergency.tool.EmergencyManager;
import com.atakmap.android.emergency.tool.EmergencyType;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.missionpackage.MissionPackageMapComponent;
import com.atakmap.android.routes.Route;
import com.atakmap.app.SettingsActivity;
import com.atakmap.app.preferences.PreferenceControl;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.ReportingRate;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;
import com.garmin.android.connectiq.ConnectIQ;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.ui.MotionEvent;
import pl.tdf.atak.TAKWatch.debouncer.MessageDebouncer;
import pl.tdf.atak.TAKWatch.heartrate.ExtendedUserDetails;
import pl.tdf.atak.TAKWatch.heartrate.HeartRateCodHandler;
import pl.tdf.atak.TAKWatch.plugin.HeartRatePreferenceFragment;
import pl.tdf.atak.TAKWatch.plugin.R;
import pl.tdf.atak.TAKWatch.plugin.TAKWatchConst;
import pl.tdf.atak.TAKWatch.radialmenu.RadialMenuDetailsExtender;
import pl.tdf.atak.TAKWatch.statushb.StatusHeartBeatTask;
import pl.tdf.atak.TAKWatch.statushb.UpdateMarkerStatusTask;

import static java.lang.Integer.parseInt;
import static pl.tdf.atak.TAKWatch.heartrate.HeartRateCodHandler.HEART_RATE_COD_KEY;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_AVG_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_LAST_UPDATED;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_MAX_HEART_RATE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.DETAILS_META_KEY_TIME_RANGE;
import static pl.tdf.atak.TAKWatch.plugin.TAKWatchConst.OPEN_PREFERENCES_ACTION;
import static pl.tdf.atak.TAKWatch.radialmenu.RadialMenuDetailsExtenderAlert.createOnPressDialog;


public class TakWatchMapComponent extends AbstractMapComponent {

    private static final String TAG = "TAKWatchSelfMarker";

    private final List<Integer> heartBeatsValues = new ArrayList<>();

    private MapView view;

    private ContactLocationView.ExtendedSelfInfoFactory extendedselfinfo;

    private CotDetailHandler healthDetail;

    private MED_Listener med_listener;

    private MapItem _target;

    private BroadcastReceiver openPreferencesReceiver;

    private final MessageDebouncer messageDebouncer = new MessageDebouncer();
    private SharedPreferences sharedPref;

    private long lastBroadcasted = System.currentTimeMillis();

    private WatchClient watchClient;

    private Timer heartBeatConnectionStatusTimer;
    private Timer updateMarkersStatusTimer;
    private final ConnectIQ.IQApplicationEventListener mapEventsListener = (iqDevice, iqApp, messages, iqMessageStatus) -> {
        if (messageDebouncer.alreadyHandled(messages)) {
            Log.d(TAG, "onMessageReceived - message '" + messages + "' already handled skipping: ");
            return;
        }
        Log.d(TAG, "onMessageReceived - message '" + messages + "' not handle yet proceeding: ");
        messageDebouncer.remember(messages);

        if (messages.size() > 0) {
            List<String> msg = (List<String>) messages.get(0);
            String type = msg.get(0);
            switch (type) {
                case "stats":
                    handleWatchStats(msg);
                    break;
                case "alert":
                    handleWatchAlert(msg);
                    break;
                case "ready":
                    sendAllMarkersToWatch();
                    break;
                case "wipe":
                    wipeWatch();
                    break;
                case "message":
                    String m = msg.get(1);
                    sendTAKChatMessage(m);
                    break;
                default:
                    break;
            }
        }
    };


    class MED_Listener implements MapEventDispatcher.MapEventDispatchListener {
        @Override
        public void onMapEvent(MapEvent event) {
            MapItem target = event.getItem();
            if (target == null) return;


            String targetType = target.getType();
            String eventType = event.getType();
            String uid = target.getUID();
            Log.d(TAG, "EVENT ITEM: " + target.toString());
            Log.d(TAG, "EVENT UID: " + uid);
            Log.d(TAG, "EVENT TYPE: " + eventType);
            Log.d(TAG, "TARGET TYPE: " + targetType);

            switch (eventType) {
                case MapEvent.ITEM_SHARED:
                case MapEvent.ITEM_PERSIST:
                case MapEvent.ITEM_REFRESH: {
                    if (!TAKWatchConst.supportedTypes.contains(targetType)) {
                        Log.d(TAG, "Type " + targetType + " not supported. Skipping.");
                    } else {
                        sendMarkerToWatch(target);
                    }
                    break;
                }

                case MapEvent.ITEM_REMOVED: {
                    if (!TAKWatchConst.supportedTypes.contains(targetType)) {
                        Log.d(TAG, "Type " + targetType + " not supported. Skipping.");
                    } else {
                        List<Object> msg = Arrays.asList(new String[]{"remove", uid});
                        watchClient.sendMessageToWatch(msg);
                    }

                    break;
                }

                case MapEvent.ITEM_PRESS: {
                    _target = target;
                    break;
                }

                default:
                    break;

            }

        }
    }

    private void wipeWatch() {
        Thread.currentThread().setName(TAG);

        // work to be performed by background thread
        Log.d(TAG, "Executing...");

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(view.getContext());
        prefs.edit().putBoolean("clearingContent", true).apply();

        //close dropdowns/tools
        AtakBroadcast.getInstance().sendBroadcast(new Intent(
                "com.atakmap.android.maps.toolbar.END_TOOL"));
        DropDownManager.getInstance().closeAllDropDowns();

        // Prevent errors during secure delete
        MissionPackageMapComponent mp = MissionPackageMapComponent
                .getInstance();
        if (mp != null)
            mp.getFileIO().disableFileWatching();

        //delete majority of files here on background thread rather then tying up UI
        //thread by having components delete large numbers of files
        //while processing ZEROIZE_CONFIRMED_ACTION intent
        DataMgmtReceiver.deleteDirs(new String[] {
                "grg", "attachments", "cert", "overlays",
                FileSystemUtils.EXPORT_DIRECTORY,
                FileSystemUtils.TOOL_DATA_DIRECTORY,
                FileSystemUtils.SUPPORT_DIRECTORY,
                FileSystemUtils.CONFIG_DIRECTORY
        }, true);

        // reset all prefs and stored credentials
        AtakAuthenticationDatabase.clear();
        AtakCertificateDatabase.clear();

        //Clear all pref groups
        Log.d(TAG, "Clearing preferences");
        for (String name : PreferenceControl
                .getInstance(view.getContext()).PreferenceGroups) {
            prefs = view.getContext().getSharedPreferences(name,
                    Context.MODE_PRIVATE);

            if (prefs != null)
                prefs.edit().clear().apply();
        }


        final File databaseDir = FileSystemUtils.getItem("Databases");
        final File[] files = IOProviderFactory.listFiles(databaseDir);
        if (files != null) {
            for (File file : files) {
                if (IOProviderFactory.isFile(file)) {
                    final String name = file.getName();
                    // skip list for now
                    if (name.equals("files.sqlite3")
                            || name.equals("GRGs2.sqlite") ||
                            name.equals("layers3.sqlite")
                            || name.equals("GeoPackageImports.sqlite")) {

                        Log.d(TAG, "skipping: " + name);

                    } else {

                        Log.d(TAG, "purging: " + name);
                        IOProviderFactory.delete(file,
                                IOProvider.SECURE_DELETE);

                    }
                }
            }
        }
         DataMgmtReceiver.deleteDirs(new String[] {
                    "layers", "native", "mobac", "mrsid", "imagery", "pri",
                    "pfi", "imagecache"
            }, true);
            DataMgmtReceiver.deleteDirs(new String[] {
                    "DTED", "pfps",
            }, false);


        AtakBroadcast.getInstance().sendBroadcast(
                new Intent("com.atakmap.app.QUITAPP")
                        .putExtra("FORCE_QUIT", true));
    }
    private void sendRouteToWatch(Route r) {

        List<Object> msg = new ArrayList<Object>();
        Collections.addAll(msg, new String[]{"route", r.getUID()});

        for (int c = 0; c < r.getPointMapItemArray().length; c++) {
            Double lat = r.getPointMapItemArray()[c].getPoint().getLatitude();
            Double lon = r.getPointMapItemArray()[c].getPoint().getLongitude();
            msg.add(lat + ";" + lon);
        }

        watchClient.sendMessageToWatch(msg);
    }

    private void sendMarkerToWatch(MapItem target) {
        if (target instanceof PointMapItem) {
            GeoPoint me = view.getSelfMarker().getPoint();
            double distance = me.distanceTo(((PointMapItem) target).getPoint());
            Integer configuredDistance = Integer.valueOf(sharedPref.getString(PreferenceKeys.PREFERENCE_KEY_SYNC_RANGE, "1000"));

            if (configuredDistance != 0 && distance > configuredDistance) return;

            String lat = String.valueOf(((PointMapItem) target).getPoint().getLatitude());
            String lon = String.valueOf(((PointMapItem) target).getPoint().getLongitude());
            String title = target.getTitle();
            List<Object> msg = Arrays.asList(new Object[]{"marker", target.getUID(), lat, lon, title, target.getType()});
            if (title != null) {
                watchClient.sendMessageToWatch(msg);
            }
        }
    }

    private void drawVectorOnWatch(String uid) {
        List<Object> msg = Arrays.asList(new Object[]{"vector", uid});
        watchClient.sendMessageToWatch(msg);
    }

    private void storeWaypointOnWatch(MapItem target) {
        if (target instanceof PointMapItem) {
            String lat = String.valueOf(((PointMapItem) target).getPoint().getLatitude());
            String lon = String.valueOf(((PointMapItem) target).getPoint().getLongitude());
            String title = target.getTitle();
            List<Object> msg = Arrays.asList(new Object[]{"waypoint", lat, lon, title});
            watchClient.sendMessageToWatch(msg);
        }
    }

    private void sendTAKChatMessage(String msg) {
        List<Contact> c = new ArrayList<Contact>();
        c.add(ChatManagerMapComponent.getChatBroadcastContact());
        ChatManagerMapComponent.getInstance().sendMessage(msg, c);
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(
                new android.content.Intent("com.atakmap.android.chat.HISTORY_UPDATE"));

    }

    private void sendAllMarkersToWatch() {

        Iterator it = view.getRootGroup().getAllItems().iterator();
        while (it.hasNext()) {
            MapItem mi = (MapItem) it.next();
            if (!(mi instanceof PointMapItem)) continue;

            String targetType = mi.getType();
            if (TAKWatchConst.supportedTypes.contains(targetType)) {
                sendMarkerToWatch(mi);
            }
        }
    }

    private void handleWatchAlert(List<String> message) {
        Log.d(TAG, "User triggered ALERT!");
        EmergencyManager.getInstance().setEmergencyType(EmergencyType.TroopsInContact);
        EmergencyManager.getInstance().initiateRepeat(EmergencyType.TroopsInContact, false);
        EmergencyManager.getInstance().setEmergencyOn(true);
    }

    private void handleWatchStats(List<String> message) {
        int heartRate = parseInt((String) message.get(1));
        String battery = (String) message.get(2);
        heartBeatsValues.add(heartRate);
        int sum = 0;
        int maxHeartRate = 0;
        for (int v : heartBeatsValues) {
            sum += v;
            maxHeartRate = Math.max(maxHeartRate, v);
        }
        int averageHeartRate = sum / heartBeatsValues.size();

        int timeRange;
        try {
            String timeRangeString = sharedPref.getString(PreferenceKeys.PREFERENCE_KEY_TIMERANGE, "60");
            timeRange = parseInt(timeRangeString);
        } catch (Exception e) {
            timeRange = 60;
            Log.d(TAG, "timeRange value cannot be read from preferences. setting default: " + timeRange + ", error: " + e.getMessage());
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss dd MMM yyyy");
        String nowString = simpleDateFormat.format(new Date());

        CotDetail cd = new CotDetail(HEART_RATE_COD_KEY);
        cd.setAttribute(DETAILS_META_KEY_MAX_HEART_RATE, maxHeartRate + "");
        cd.setAttribute(DETAILS_META_KEY_AVG_HEART_RATE, averageHeartRate + "");
        cd.setAttribute(DETAILS_META_KEY_LAST_UPDATED, nowString);
        cd.setAttribute(DETAILS_META_KEY_TIME_RANGE, timeRange + "");
        cd.setAttribute("watchBattery", battery);

        if (healthDetail != null) {
            healthDetail.toItemMetadata(view.getSelfMarker(), null, cd);
        }
        CotMapComponent.getInstance().addAdditionalDetail(cd.getElementName(), cd);

        long diffBetweenLastBroadcast = (System.currentTimeMillis() - lastBroadcasted) / 1000;
        if (diffBetweenLastBroadcast > timeRange) {
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ReportingRate.REPORT_LOCATION)
                            .putExtra("reason", "detail update for heart rate"));
            lastBroadcasted = System.currentTimeMillis();
        }
    }

    class TAKWatchPressListener implements IMapWidget.OnPressListener {
        @Override
        public void onMapWidgetPress(IMapWidget iMapWidget, MotionEvent motionEvent) {
            Log.d(TAG, "onMapWidgetPress");

            if (_target == null) return;

            MapItem mi = view.getMapItem(_target.getUID());
            if (mi.getType().equals("b-m-r")) {
                Route r = (Route) mi;
                sendRouteToWatch(r);
                return;
            }

            DialogInterface.OnClickListener onPositiveButtonClick = (dialogInterface, i) -> {
                MapItem mi1 = view.getMapItem(_target.getUID());
                sendMarkerToWatch(mi1);
                drawVectorOnWatch(_target.getUID());

            };
            DialogInterface.OnClickListener onNegativeButtonClick = (dialogInterface, i) -> {
                MapItem mi12 = view.getMapItem(_target.getUID());
                storeWaypointOnWatch(mi12);
            };

            createOnPressDialog(view.getContext(), onPositiveButtonClick, onNegativeButtonClick).show();
        }
    }

    public void onCreate(final Context context, Intent intent, final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);

        this.view = view;
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this.view.getContext());
        extendedselfinfo = new ExtendedUserDetails(context, view.getContext());

        RadialMenuDetailsExtender.extend(context, view.getContext(), new TAKWatchPressListener());
        ContactLocationView.register(extendedselfinfo);
        healthDetail = new HeartRateCodHandler();
        CotDetailManager.getInstance().registerHandler(healthDetail);

        watchClient = WatchClient.initialize(view.getContext(), mapEventsListener);

        med_listener = new MED_Listener();
        view.getMapEventDispatcher().addMapEventListener(med_listener);

        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                "TAKWatch Plugin Preferences",
                                "Preferences for TAKWatch Plugin",
                                "TAKWatchPreferences",
                                context.getResources().getDrawable(R.drawable.takwatch, null),
                                new HeartRatePreferenceFragment(context, watchClient)));

        openPreferencesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent prefIntent = new Intent(MapView.getMapView().getContext(), SettingsActivity.class);
                prefIntent.putExtra("toolkey", "TAKWatchPreferences");
                ((Activity) MapView.getMapView().getContext()).startActivityForResult(prefIntent, 0);
            }
        };
        AtakBroadcast.getInstance().registerReceiver(openPreferencesReceiver, new AtakBroadcast.DocumentedIntentFilter(OPEN_PREFERENCES_ACTION));

        sharedPref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            String configuredDeviceName = sharedPref.getString(PreferenceKeys.PREFERENCE_KEY_DEVICE_NAME, null);
            if (key.equals(PreferenceKeys.PREFERENCE_KEY_DEVICE_NAME)) {
                Log.d(TAG, "Device preference changed to '" + configuredDeviceName + "' reloading IQConnector");
                watchClient.reload();
            }
        });
        registerStatusHeartBeat();
        registerUpdateMarkersCheck();
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "onDestroyImpl");

        CotDetailManager.getInstance().unregisterHandler(healthDetail);
        view.getMapEventDispatcher().removeMapEventListener(med_listener);
        ContactLocationView.unregister(extendedselfinfo);
        watchClient.cleanup();

        AtakBroadcast.getInstance().unregisterReceiver(openPreferencesReceiver);
        unregisterStatusHeartBeat();
        unregisterUpdateMarkersCheck();
    }

    private void registerStatusHeartBeat() {
        Log.d(TAG, "Registering status heart beat timer");
        StatusHeartBeatTask task = new StatusHeartBeatTask(watchClient);
        heartBeatConnectionStatusTimer = new Timer();
        heartBeatConnectionStatusTimer.schedule(task, 5000, 5000);
    }

    private void unregisterStatusHeartBeat() {
        Log.d(TAG, "Unregistering status heart beat timer");
        if (heartBeatConnectionStatusTimer != null) {
            heartBeatConnectionStatusTimer.purge();
            heartBeatConnectionStatusTimer.cancel();
        }
    }

    private void registerUpdateMarkersCheck() {
        Log.d(TAG, "Registering update marker timer");
        UpdateMarkerStatusTask task = new UpdateMarkerStatusTask(this::sendAllMarkersToWatch, sharedPref);
        updateMarkersStatusTimer = new Timer();
        updateMarkersStatusTimer.schedule(task, 5000, 5000);
    }

    private void unregisterUpdateMarkersCheck() {
        Log.d(TAG, "Unregistering update marker timer");
        if (updateMarkersStatusTimer != null) {
            updateMarkersStatusTimer.purge();
            updateMarkersStatusTimer.cancel();
        }
    }
}