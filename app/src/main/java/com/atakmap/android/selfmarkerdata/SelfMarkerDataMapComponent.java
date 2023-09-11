
package com.atakmap.android.selfmarkerdata;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.ContactLocationView;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.cotdetails.ExtendedInfoView;
import com.atakmap.android.emergency.tool.EmergencyManager;
import com.atakmap.android.emergency.tool.EmergencyType;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.preference.AtakPreferenceFragment;
import com.atakmap.android.selfmarkerdata.debouncer.MessageDebouncer;
import com.atakmap.android.selfmarkerdata.plugin.HeartRatePreferenceFragment;
import com.atakmap.android.selfmarkerdata.plugin.R;
import com.atakmap.android.selfmarkerdata.plugin.TAKWatchConst;
import com.atakmap.android.selfmarkerdata.radialmenu.RadialMenuDetailsExtender;
import com.atakmap.app.preferences.PreferenceManagementFragment;
import com.atakmap.app.preferences.PreferenceSearchDialog;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.ReportingRate;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import gov.tak.api.util.Disposable;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.ui.MotionEvent;

import static com.atakmap.android.selfmarkerdata.PreferenceKeys.PREFERENCE_KEY_DEVICE_NAME;
import static com.atakmap.android.selfmarkerdata.PreferenceKeys.PREFERENCE_KEY_TIMERANGE;
import static com.garmin.android.connectiq.IQApp.IQAppStatus.INSTALLED;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;


public class SelfMarkerDataMapComponent extends AbstractMapComponent {

    private static final String TAG = "SelfMarkerDataMapCompon";
    private static final String COMM_WATCH_ID = "a3421feed289106a538cb9547ab12095";
    public static final String OPEN_PREFERENCES_ACTION = "com.atakmap.android.selfmarkerdata.plugin.openPreferences";

    private final List<Integer> heartBeatsValues = new ArrayList<>();

    private MapView view;

    private ContactLocationView.ExtendedSelfInfoFactory extendedselfinfo;

    private ConnectIQ connectIQ;
    private IQApp myApp;

    private boolean isSdkReady = false;

    private IQDevice selectedDevice;

    private CotDetailHandler healthDetail;

    private MED_Listener med_listener;

    private boolean sent = false;

    private MapItem _target;

    private BroadcastReceiver openPreferencesReceiver;

    private final MessageDebouncer messageDebouncer = new MessageDebouncer();

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
                        List<String> msg = Arrays.asList(new String[]{"remove", uid});
                        sendMessageToWatch(msg);
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

    private void sendMarkerToWatch (MapItem target) {
        GeoPoint me = view.getSelfMarker().getPoint();
        double distance = me.distanceTo(((PointMapItem) target).getPoint());
        // TODO: Add configuration slider for max sync distance
        // if (distance > configuredDistance) return;

        if (target instanceof PointMapItem) {
            String lat = String.valueOf(((PointMapItem) target).getPoint().getLatitude());
            String lon = String.valueOf(((PointMapItem) target).getPoint().getLongitude());
            String title = target.getTitle();
            List<String> msg = Arrays.asList(new String[]{"marker", target.getUID(), lat, lon, title, target.getType()});
            if (title != null) {
                sendMessageToWatch(msg);
            }
        }
    }
    private void drawVectorOnWatch ( String uid ) {
        List<String> msg = Arrays.asList(new String[]{"vector", uid});
        sendMessageToWatch(msg);
    }

    private void storeWaypointOnWatch ( MapItem target ) {
        if (target instanceof PointMapItem) {
            String lat = String.valueOf(((PointMapItem) target).getPoint().getLatitude());
            String lon = String.valueOf(((PointMapItem) target).getPoint().getLongitude());
            String title = target.getTitle();
            List<String> msg = Arrays.asList(new String[]{"waypoint", lat, lon, title});
            sendMessageToWatch(msg);
        }
    }

    private final ConnectIQ.ConnectIQListener connectIQListener = new ConnectIQ.ConnectIQListener() {
        @Override
        public void onSdkReady() {
            Log.d(TAG, "onSdkReady");
            if (!isSdkReady) {
                Log.d(TAG, "SDK not ready - initializing");

                loadDevice();
                loadAppMessages();

                med_listener = new MED_Listener();
                view.getMapEventDispatcher().addMapEventListener(med_listener);
                isSdkReady = true;
            }
        }

        private void syncWatchMapZoom() {
            //GeoBounds g = view.
        }

        @Override
        public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {
            Log.d(TAG, "Error");
            isSdkReady = false;
        }

        @Override
        public void onSdkShutDown() {
            Log.d(TAG, "onSdkShutDown");
            isSdkReady = false;
        }
    };
    private HeartRatePreferenceFragment preferencesFragment;
    private long lastBroadcasted = System.currentTimeMillis();

    public void loadAppMessages() {
        if (selectedDevice == null) {
            Log.d(TAG, "No devices selected in the preferences - skipping connectIQ initialization");
            return;
        }
        try {
            connectIQ.registerForDeviceEvents(selectedDevice, (iqDevice, iqDeviceStatus) -> {
                Log.d(TAG, "Inside registerForDeviceEvents ");
                Log.d(TAG, "DEVICE: " + iqDeviceStatus.toString());
            });


            myApp = new IQApp(COMM_WATCH_ID);

            try {
                connectIQ.openApplication(selectedDevice, myApp, (iqDevice, iqApp, iqOpenApplicationStatus) -> {
                    Log.d(TAG, "openApplication " + iqDevice.getFriendlyName() + ":" + iqApp.getApplicationId() + ":" + iqOpenApplicationStatus);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }


            Log.d(TAG, selectedDevice.getFriendlyName());

            connectIQ.getApplicationInfo(COMM_WATCH_ID, selectedDevice, new ConnectIQ.IQApplicationInfoListener() {
                @Override
                public void onApplicationInfoReceived(IQApp app) {
                    if (app != null) {
                        myApp = app;
                        if (app.getStatus() == INSTALLED) {
                            Log.d(TAG, "Version:" + app.version());
                        }
                    }
                }

                @Override
                public void onApplicationNotInstalled(String s) {
                    Log.d(TAG, "Missing Application, Corresponding IQ application not installed");
                }
            });

            Log.d(TAG, "Before registerForAppEvents");
            startIQListener();

        } catch (InvalidStateException | ServiceUnavailableException e) {
            throw new RuntimeException(e);
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
            MapItem mi = (MapItem)it.next();
            if (!(mi instanceof PointMapItem)) continue;

            String targetType = mi.getType();
            if (TAKWatchConst.supportedTypes.contains(targetType)) {
                sendMarkerToWatch(mi);
            }
        }
    }
    private void handleWatchAlert(List<String> message) {
        Log.d(TAG, "User triggered ALERT!");
        EmergencyManager.getInstance()
                .setEmergencyType(EmergencyType.TroopsInContact);
        EmergencyManager.getInstance().initiateRepeat(EmergencyType.TroopsInContact,
                false);
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

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.view.getContext());
        int timeRange;
        try {
            String timeRangeString = sharedPref.getString(PREFERENCE_KEY_TIMERANGE, "60");
            timeRange = parseInt(timeRangeString);
        } catch (Exception e) {
            timeRange = 60;
            Log.d(TAG, "timeRange value cannot be read from preferences. setting default: " + timeRange + ", error: " + e.getMessage());
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss dd MMM yyyy");
        String nowString = simpleDateFormat.format(new Date());

        CotDetail cd = new CotDetail("health");
        cd.setAttribute("maxHeartRate", maxHeartRate + "");
        cd.setAttribute("averageHeartRate", averageHeartRate + "");
        cd.setAttribute("lastUpdated", nowString);
        cd.setAttribute("watchBattery", battery);
        cd.setAttribute("timeRange", timeRange + "");

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

    private void loadDevice() {
        try {
            List<IQDevice> devices = connectIQ.getConnectedDevices();
            preferencesFragment.setDevices(devices);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.view.getContext());

            sharedPref.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
                String sharedPrefString = sharedPref.getString(PREFERENCE_KEY_DEVICE_NAME, null);
                if (key.equals(PREFERENCE_KEY_DEVICE_NAME)) {
                    Log.d(TAG, "device preference changed to " + sharedPrefString);
                    loadAppMessages();
                    initializeDevice(devices, sharedPref);
                }
            });

            initializeDevice(devices, sharedPref);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    private void startIQListener() throws InvalidStateException {
        Log.d(TAG, "startIQListener()");
        connectIQ.registerForAppEvents(selectedDevice, myApp, new ConnectIQ.IQApplicationEventListener() {
            @Override
            public void onMessageReceived(IQDevice iqDevice, IQApp iqApp, List<Object> messages, ConnectIQ.IQMessageStatus iqMessageStatus) {
                Log.d(TAG, "onMessageReceived: " + messages);
                if (messageDebouncer.alreadyHandled(messages)) {
                    return;
                }
                messageDebouncer.remember(messages);

                if (messages.size() > 0) {
                    List<String> msg = (List<String>) messages.get(0);
                    String type = msg.get(0);
                    switch (type) {
                        case "stats" :
                            handleWatchStats(msg);
                            break;
                        case "alert" :
                            handleWatchAlert(msg);
                            break;
                        case "ready" :
                            sendAllMarkersToWatch();
                            break;
                        case "message" :
                            String m = msg.get(1);
                            sendTAKChatMessage(m);
                            break;
                        default:
                            break;
                    }


                }
            }
        });
    }
    private void initializeDevice(List<IQDevice> devices, SharedPreferences sharedPref) {
        String deviceNameFromPreferences = sharedPref.getString(PREFERENCE_KEY_DEVICE_NAME, null);
        Log.d(TAG, "Value from preferences: " + deviceNameFromPreferences);

        if (deviceNameFromPreferences != null) {
            for (IQDevice d : devices) {
                Log.d(TAG, "Lookup of: '" + d.getFriendlyName() + "' vs '" + deviceNameFromPreferences + "'");
                if (d.getFriendlyName().equals(deviceNameFromPreferences)) {
                    selectedDevice = d;
                    Log.d(TAG, "Device matched with preferences selection: " + selectedDevice);
                    return;
                }
            }
            Log.d(TAG, "No device found for name: " + deviceNameFromPreferences);
        }
    }


    @Override
    public void onResume(Context context, MapView view) {
        super.onResume(context, view);
        if (isSdkReady) {
            Log.d(TAG, "resuming");
            loadDevice();
        }
    }

     class TAKWatchPressListener implements IMapWidget.OnPressListener {
        @Override
        public void onMapWidgetPress(IMapWidget iMapWidget, MotionEvent motionEvent) {
            Log.d(TAG, "onMapWidgetPress");
            Log.d(TAG, iMapWidget.getAbsolutePath());
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
            alertBuilder.setTitle("TAK Watch ");
            alertBuilder.setMessage("Please select the desired action.");
            final AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Navigate on watch", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    MapItem mi = view.getMapItem(_target.getUID());
                    sendMarkerToWatch(mi);
                    drawVectorOnWatch(_target.getUID());

            }});
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Save on watch",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    MapItem mi = view.getMapItem(_target.getUID());
                    storeWaypointOnWatch(mi);

                }
            });
            alertDialog.setCancelable(true);

            alertDialog.show();

        }
    }
    public void onCreate(final Context context, Intent intent, final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);
        this.view = view;
        this.preferencesFragment = new HeartRatePreferenceFragment(context);

        RadialMenuDetailsExtender.extend(context, view.getContext(), new TAKWatchPressListener());

        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                "TAKWatch Plugin Preferences",
                                "Preferences for TAKWatch Plugin",
                                "TAKWatchPreferences",
                                context.getResources().getDrawable(R.drawable.takwatch, null),
                                preferencesFragment));

        openPreferencesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive in openPreferencesReceiver, handling: " + intent.getAction());
            }
        };
        AtakBroadcast.getInstance().registerReceiver(openPreferencesReceiver, new AtakBroadcast.DocumentedIntentFilter(OPEN_PREFERENCES_ACTION));

        ContactLocationView.register(
                extendedselfinfo = new ContactLocationView.ExtendedSelfInfoFactory() {
                    @Override
                    public ExtendedInfoView createView() {
                        return new ExtendedInfoView(view.getContext()) {
                            @Override
                            public void setMarker(PointMapItem m) {
                                try {
                                    TextView label = new TextView(context);
                                    label.setText("Heart rate");
                                    label.setTextColor(Color.parseColor("#d4b246"));
                                    label.setTextSize(10);
                                    label.setPadding(0, 0, 0, 0);

                                    TextView heartBeatTextField = new TextView(view.getContext());
                                    heartBeatTextField.setLayoutParams(new LayoutParams(
                                            LayoutParams.WRAP_CONTENT,
                                            LayoutParams.WRAP_CONTENT));
                                    heartBeatTextField.setTextColor(Color.WHITE);

                                    LinearLayout parent = new LinearLayout(context);
                                    parent.setOrientation(VERTICAL);
                                    parent.addView(label);
                                    view.removeView(heartBeatTextField);
                                    parent.addView(heartBeatTextField);
                                    this.addView(parent);


                                    int maxHeartRate = m.getMetaInteger("SelfMarkerDataPlugin.maxHeartRate", -1);
                                    int averageHeartRate = m.getMetaInteger("SelfMarkerDataPlugin.averageHeartRate", -1);
                                    int timeRange = m.getMetaInteger("SelfMarkerDataPlugin.timeRange", -1);
                                    String lastUpdated = m.getMetaString("SelfMarkerDataPlugin.lastUpdated", "Unknown");

                                    if (maxHeartRate == -1) {
                                        heartBeatTextField.setText("No data");
                                    } else {
                                        heartBeatTextField.setText(format("Max : %d \nAvg: %d\nInterval: %ds \nLast updated: %s", maxHeartRate, averageHeartRate, timeRange, lastUpdated));
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "niedobrze :(", e);
                                }
                            }
                        };
                    }
                });

        CotDetailManager.getInstance()
                .registerHandler(healthDetail = new CotDetailHandler("health") {
                    @Override
                    public CommsMapComponent.ImportResult toItemMetadata(
                            MapItem item, CotEvent event, CotDetail detail) {
//                        Log.d(TAG, "detail received from [ " + item.getMetaString("callsign", null) + " - " + detail + " in:  " + event);

                        try {

                            String maxHeartRateString = detail.getAttribute("maxHeartRate");
                            String averageHeartRateString = detail.getAttribute("averageHeartRate");
                            String timeRangeString = detail.getAttribute("timeRange");
                            String lastUpdatedString = detail.getAttribute("lastUpdated");

                            if (FileSystemUtils.isEmpty(maxHeartRateString) || FileSystemUtils.isEmpty(averageHeartRateString)) {
                                return CommsMapComponent.ImportResult.FAILURE;
                            }

                            int maxHeartRate = Integer.parseInt(maxHeartRateString);
                            int averageHeartRate = Integer.parseInt(averageHeartRateString);
                            int timeRange = Integer.parseInt(timeRangeString);

                            item.setMetaInteger("SelfMarkerDataPlugin.maxHeartRate", maxHeartRate);
                            item.setMetaInteger("SelfMarkerDataPlugin.averageHeartRate", averageHeartRate);
                            item.setMetaInteger("SelfMarkerDataPlugin.timeRange", timeRange);
                            item.setMetaString("SelfMarkerDataPlugin.lastUpdated", lastUpdatedString);

                        } catch (Exception e) {
                            Log.e(TAG, "Error", e);
                        }
                        return CommsMapComponent.ImportResult.SUCCESS;
                    }

                    @Override
                    public boolean toCotDetail(MapItem item, CotEvent event,
                                               CotDetail root) {
                        return true;
                    }
                });

        setupConnectIQSdk();


    }

    private void setupConnectIQSdk() {
        connectIQ = ConnectIQ.getInstance();
        connectIQ.initialize(view.getContext(), true, connectIQListener);
    }


    private void sendMessageToWatch(List<String> msg) {
        try {
            Log.d(TAG, "sendMessageToWatch: " + msg.toString());
            connectIQ.sendMessage(selectedDevice, myApp, msg, (iqDevice, iqApp, iqMessageStatus) -> {
                Log.d(TAG, "MessageStatus: " + iqMessageStatus);
            });
        } catch (InvalidStateException e) {
            throw new RuntimeException(e);
        } catch (ServiceUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "onDestroyImpl");

        CotDetailManager.getInstance().unregisterHandler(healthDetail);
        view.getMapEventDispatcher().removeMapEventListener(med_listener);
        ContactLocationView.unregister(extendedselfinfo);
        try {
            connectIQ.unregisterAllForEvents();
            connectIQ.shutdown(view.getContext());
        } catch (InvalidStateException e) {
            // SDK already closed no worries
        }

        AtakBroadcast.getInstance().unregisterReceiver(openPreferencesReceiver);
    }


}