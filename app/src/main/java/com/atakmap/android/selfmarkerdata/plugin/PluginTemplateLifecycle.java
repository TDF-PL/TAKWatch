package com.atakmap.android.selfmarkerdata.plugin;


import com.atak.plugins.impl.AbstractPlugin;
import gov.tak.api.plugin.IServiceController;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.selfmarkerdata.SelfMarkerDataMapComponent;


/**
 *
 * 
 *
 */
public class PluginTemplateLifecycle extends AbstractPlugin {

   private final static String TAG = "PluginTemplateLifecycle";

   public PluginTemplateLifecycle(IServiceController serviceController) {
        super(serviceController, new TakWatchTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new SelfMarkerDataMapComponent());
        PluginNativeLoader.init(serviceController.getService(PluginContextProvider.class).getPluginContext());
    }
}

