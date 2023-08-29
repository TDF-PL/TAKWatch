
package com.atakmap.android.selfmarkerdata;

import static com.atakmap.android.selfmarkerdata.PreferenceKeys.PREFERENCE_KEY_DEVICE_NAME;
import static com.atakmap.android.selfmarkerdata.PreferenceKeys.PREFERENCE_KEY_TIMERANGE;
import static com.garmin.android.connectiq.IQApp.IQAppStatus.INSTALLED;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.contact.ContactLocationView;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.cot.importer.CotImporterManager;
import com.atakmap.android.cot.importer.FriendlyMarkerImporter;
import com.atakmap.android.cotdetails.ExtendedInfoView;
import com.atakmap.android.emergency.EmergencyAlertComponent;
import com.atakmap.android.emergency.tool.EmergencyBeacon;
import com.atakmap.android.emergency.tool.EmergencyManager;
import com.atakmap.android.emergency.tool.EmergencyType;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AbstractMapComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.selfmarkerdata.plugin.HeartRatePreferenceFragment;
import com.atakmap.android.selfmarkerdata.plugin.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.ReportingRate;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class SelfMarkerDataMapComponent extends AbstractMapComponent {

    private static final String TAG = "SelfMarkerDataMapCompon";
    private static final String COMM_WATCH_ID = "a3421feed289106a538cb9547ab12095";

    private final List<Integer> heartBeatsValues = new ArrayList<>();

    private MapView view;

    private ContactLocationView.ExtendedSelfInfoFactory extendedselfinfo;

    private ConnectIQ connectIQ;
    private IQApp myApp;

    private boolean isSdkReady = false;

    private IQDevice selectedDevice;

    private CotDetailHandler healthDetail;


    private ConnectIQ.ConnectIQListener connectIQListener = new ConnectIQ.ConnectIQListener() {
        @Override
        public void onSdkReady() {
            Log.d(TAG, "onSdkReady");
            loadDevice();
            try {
                loadAppMessages();
            } catch (Exception e) {
                Log.e(TAG, "Load app failed!", e);
                throw new RuntimeException(e);
            }

            edl_pointAdded edlpA = new edl_pointAdded();
            view.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_REFRESH,edlpA);
            view.getMapEventDispatcher().addMapEventListener(MapEvent.ITEM_PERSIST,edlpA);
            //view.getMapEventDispatcher().addMapEventListener(new edl_areaChange());
            //view.getMapEventDispatcher().addMapEventListener(MapEvent.MAP_SCROLL,new edl_areaChange());
            //view.getMapEventDispatcher().addMapEventListener(MapEvent.MAP_ZOOM,new edl_areaChange());
            //view.getMapEventDispatcher().addMapEventListener(MapEvent.MAP_SCALE,new edl_areaChange());
            isSdkReady = true;
        }

        class edl_pointAdded implements MapEventDispatcher.MapEventDispatchListener {
                @Override
                public void onMapEvent(MapEvent event) {
                    MapItem target = event.getItem();
                    Log.d(TAG, "EVENT ITEM: " + target.toString());
                    Log.d(TAG, "UID: " + target.getUID());

                    if (target instanceof PointMapItem) {
                        String lat = String.valueOf(((PointMapItem)target).getPoint().getLatitude());
                        String lon = String.valueOf(((PointMapItem)target).getPoint().getLongitude());
                        String title = target.getTitle();
                        String type = target.getType();
                        String uid = target.getUID();
                        List<String> msg = Arrays.asList(new String[]{"marker", uid, lat, lon, title, type});

                        Log.d(TAG, lat + "," + lon + "," + title + ","+ type);

                        if (title != null) {
                            sendMessageToWatch(msg);
                        }
                    }



                }
        }

        private void syncWatchMapZoom() {
            //GeoBounds g = view.
        }
        class edl_areaChange implements MapEventDispatcher.MapEventDispatchListener  {
            @Override
            public void onMapEvent(MapEvent event) {

                Log.d(TAG, "EVENT TYPE: " + event.getType());
                if (event.getExtras()!=null)
                    Log.d(TAG, "EVENT EXTRAS: " + event.getExtras().toString());
//                Log.d(TAG, "EVENT GROUP: " + event.getGroup().getFriendlyName());
//                if (target instanceof PointMapItem) {
//                    String lat1 = String.valueOf(((PointMapItem)target).getPoint().getLatitude());
//                    String lon1 = String.valueOf(((PointMapItem)target).getPoint().getLongitude());
//                    String lat2 = String.valueOf(((PointMapItem)target).getPoint().getLatitude());
//                    String lon2 = String.valueOf(((PointMapItem)target).getPoint().getLongitude());
//                    String title = target.getTitle();
//                    String type = target.getType();
//                    List<String> msg = Arrays.asList(new String[]{"maparea", lat1, lon1, lat2, lon2});
//                    Log.d(TAG, lat + "," + lon + "," + title + ","+ type);
//
//                    if (title != null) {
//                        sendMessageToWatch(msg);
//                    }
//                }

            }
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
                Log.e(TAG, "nie dobrze", e);
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

            connectIQ.registerForAppEvents(selectedDevice, myApp, new ConnectIQ.IQApplicationEventListener() {
                @Override
                public void onMessageReceived(IQDevice iqDevice, IQApp iqApp, List<Object> messages, ConnectIQ.IQMessageStatus iqMessageStatus) {
                    Log.d(TAG, "onMessageReceived: " + messages);

                    if (messages.size() > 0) {
                        List<String> msg = (List<String>) messages.get(0);
                        String type = msg.get(0);
                        switch (type) {
                            case "hr" :
                                handleWatchHR(msg);
                                break;
                            case "alert" :
                                handleWatchAlert(msg);
                                break;
                            default:
                                break;
                        }


                    }
                }
            });
        } catch (InvalidStateException | ServiceUnavailableException e) {
            throw new RuntimeException(e);
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
    private void handleWatchHR(List<String> message) {
        int heartRate = parseInt((String) message.get(1));
        Log.d(TAG, "Sending COD message with heart rate: " + heartRate);

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
        Log.d(TAG, "timeRange value from preferences: " + timeRange);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss dd MMM yyyy");
        String nowString = simpleDateFormat.format(new Date());

        CotDetail cd = new CotDetail("health");
        cd.setAttribute("maxHeartRate", maxHeartRate + "");
        cd.setAttribute("averageHeartRate", averageHeartRate + "");
        cd.setAttribute("lastUpdated", nowString);
        cd.setAttribute("timeRange", timeRange + "");

        if (healthDetail != null) {
            healthDetail.toItemMetadata(view.getSelfMarker(), null, cd);
        }
        CotMapComponent.getInstance().addAdditionalDetail(cd.getElementName(), cd);

        long diffBetweenLastBroadcast = (System.currentTimeMillis() - lastBroadcasted) / 1000;
        if (diffBetweenLastBroadcast > timeRange) {
            Log.d(TAG, "----------------BROADCAST diff:" + diffBetweenLastBroadcast);
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
            Log.e(TAG, "Błąd", e);
        }
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

    public void onCreate(final Context context, Intent intent, final MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);
        this.view = view;
        this.preferencesFragment = new HeartRatePreferenceFragment(context);



        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                "TAKWatch Plugin Preferences",
                                "Preferences for TAKWatch Plugin",
                                "TAKWatchPreferences",
                                context.getResources().getDrawable(R.drawable.takwatch, null),
                                preferencesFragment));

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
        ContactLocationView.unregister(extendedselfinfo);
        try {
            connectIQ.unregisterAllForEvents();
            connectIQ.shutdown(view.getContext());
        } catch (InvalidStateException e) {
            // SDK already closed no worries
        }
    }


}