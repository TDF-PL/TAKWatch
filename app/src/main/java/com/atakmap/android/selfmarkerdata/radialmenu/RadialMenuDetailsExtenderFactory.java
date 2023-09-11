package com.atakmap.android.selfmarkerdata.radialmenu;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.maps.MapDataRef;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.assets.MapAssets;
import com.atakmap.android.menu.MapMenuButtonWidget;
import com.atakmap.android.menu.MapMenuFactory;
import com.atakmap.android.menu.MapMenuWidget;
import com.atakmap.android.menu.MenuMapAdapter;
import com.atakmap.android.menu.MenuResourceFactory;
import com.atakmap.android.menu.PluginMenuParser;
import com.atakmap.android.selfmarkerdata.plugin.TAKWatchConst;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.WidgetIcon;

import java.io.IOException;

import gov.tak.api.widgets.IMapWidget;

public class RadialMenuDetailsExtenderFactory implements MapMenuFactory {

    private static final String TAG = "SelfMarkerMenuFactory";
    private static final String BUTTON_ASSET_NAME = "takwatch-radial-menu-btn.png";

    private final Context pluginContext;
    private final Context mapContext;
    private final IMapWidget.OnPressListener onPressHandler;

    public RadialMenuDetailsExtenderFactory(Context pluginContext, Context mapContext, IMapWidget.OnPressListener onPressHandler) {
        this.pluginContext = pluginContext;
        this.mapContext = mapContext;
        this.onPressHandler = onPressHandler;
    }

    @Override
    public MapMenuWidget create(MapItem mapItem) {
        MapMenuWidget menuWidget = getDefaultMapMenuWidget(mapItem);

        for (MapWidget child : menuWidget.getChildWidgets()) {
            if (child instanceof MapMenuButtonWidget) {
                MapMenuButtonWidget mapMenuButtonWidget = (MapMenuButtonWidget) child;
                if (isWaypointType(mapItem) && isDetailsButton(mapMenuButtonWidget)) {
                    if (hasSubmenu(mapMenuButtonWidget)) {
                        MapMenuWidget detailsSubmenu = mapMenuButtonWidget.getSubmenuWidget();
                        MapMenuButtonWidget buttonWidget = createWatchButton(detailsSubmenu, pluginContext, mapContext);
                        buttonWidget.addOnPressListener(onPressHandler);
                        detailsSubmenu.addWidget(buttonWidget);
                    } else {
                        MapMenuWidget submenu = new MapMenuWidget();
                        MapMenuButtonWidget buttonWidget = createWatchButton(pluginContext, mapContext);
                        submenu.addWidget(buttonWidget);
                        buttonWidget.addOnPressListener(onPressHandler);
                        mapMenuButtonWidget.setSubmenuWidget(submenu);
                    }
                }
            }
        }

        return menuWidget;
    }

    private MapMenuWidget getDefaultMapMenuWidget(MapItem mapItem) {
        MapView mapView = MapView.getMapView();
        final MapAssets mapAssets = new MapAssets(mapView.getContext());
        final MenuMapAdapter adapter = new MenuMapAdapter();
        try {
            adapter.loadMenuFilters(mapAssets, "filters/menu_filters.xml");
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        MenuResourceFactory defaultFactory = new MenuResourceFactory(mapView, mapView.getMapData(), mapAssets, adapter);
        return defaultFactory.create(mapItem);
    }

    private boolean isDetailsButton(MapMenuButtonWidget mapMenuButtonWidget) {
        return mapMenuButtonWidget.getWidgetIcon().getImageUri(0).equals("asset://icons/details.png");
    }

    private boolean hasSubmenu(MapMenuButtonWidget mapMenuButtonWidget) {
        return mapMenuButtonWidget.getSubmenuWidget() != null;
    }

    private boolean isWaypointType(MapItem mapItem) {
        return TAKWatchConst.supportedTypes.contains(mapItem.getType());
    }

    private MapMenuButtonWidget createWatchButton(MapMenuWidget menuWidget, Context pluginContext, Context mapContext) {

        MapMenuButtonWidget buttonWidget = new MapMenuButtonWidget(mapContext);
        buttonWidget.setOrientation(buttonWidget.getOrientationAngle(), menuWidget.getInnerRadius());
        buttonWidget.setButtonSize(buttonWidget.getButtonSpan(), menuWidget.getButtonWidth());

        float buttonWeight = 0f;
        for (MapWidget child : menuWidget.getChildWidgets()) {
            if (child instanceof MapMenuButtonWidget) {
                MapMenuButtonWidget childButton = (MapMenuButtonWidget) child;
                buttonWeight += childButton.getLayoutWeight();
            }
        }
        buttonWeight /= menuWidget.getChildCount();
        buttonWidget.setLayoutWeight(buttonWeight);

        setButtonIcon(pluginContext, buttonWidget);

        return buttonWidget;
    }

    private MapMenuButtonWidget createWatchButton(Context pluginContext, Context mapContext) {
        MapMenuButtonWidget buttonWidget = new MapMenuButtonWidget(mapContext);
        buttonWidget.setOrientation(buttonWidget.getOrientationAngle(), 130);

        setButtonIcon(pluginContext, buttonWidget);

        return buttonWidget;
    }

    private static void setButtonIcon(Context pluginContext, MapMenuButtonWidget buttonWidget) {
        String asset = PluginMenuParser.getItem(pluginContext, BUTTON_ASSET_NAME);
        final MapDataRef mapDataRef = MapDataRef.parseUri(asset);

        final WidgetIcon widgetIcon = new WidgetIcon.Builder()
                .setAnchor(16, 16)
                .setSize(32, 32)
                .setImageRef(0, mapDataRef)
                .build();

        buttonWidget.setIcon(widgetIcon);
    }
}
