package com.atakmap.android.selfmarkerdata.radialmenu;

import android.content.Context;

import com.atakmap.android.menu.MapMenuFactory;
import com.atakmap.android.menu.MapMenuReceiver;

import gov.tak.api.widgets.IMapWidget;

public class RadialMenuDetailsExtender {

    public static void extend(Context pluginContext, Context mapContext, IMapWidget.OnPressListener onPressHandler) {
        MapMenuFactory extendedDetailsMenuFactory = new RadialMenuDetailsExtenderFactory(pluginContext, mapContext, onPressHandler);
        MapMenuReceiver.getInstance().registerMapMenuFactory(extendedDetailsMenuFactory);
    }
}
