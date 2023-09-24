package pl.tdf.atak.TAKWatch.plugin;


import com.atak.plugins.impl.AbstractPlugin;
import gov.tak.api.plugin.IServiceController;
import com.atak.plugins.impl.PluginContextProvider;
import pl.tdf.atak.TAKWatch.TakWatchMapComponent;


public class PluginTemplateLifecycle extends AbstractPlugin {

   public PluginTemplateLifecycle(IServiceController serviceController) {
        super(serviceController, new TakWatchTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new TakWatchMapComponent());
        PluginNativeLoader.init(serviceController.getService(PluginContextProvider.class).getPluginContext());
    }
}

