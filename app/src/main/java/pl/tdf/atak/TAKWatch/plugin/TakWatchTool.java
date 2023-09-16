package pl.tdf.atak.TAKWatch.plugin;

import static pl.tdf.atak.TAKWatch.SelfMarkerDataMapComponent.OPEN_PREFERENCES_ACTION;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;

public class TakWatchTool extends AbstractPluginTool {

    public TakWatchTool(final Context context) {
        super(context, context.getString(R.string.app_name), context.getString(R.string.app_name), context.getResources().getDrawable(R.drawable.takwatch), OPEN_PREFERENCES_ACTION);
    }
}
