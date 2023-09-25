package pl.tdf.atak.TAKWatch.radialmenu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class RadialMenuDetailsExtenderAlert {
    public static AlertDialog createOnPressDialog(Context context, DialogInterface.OnClickListener onPositiveButtonClick, DialogInterface.OnClickListener onNegativeButtonClick) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle("TAK Watch ");
        alertBuilder.setMessage("Please select the desired action.");
        final AlertDialog alertDialog = alertBuilder.create();

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Navigate on watch", onPositiveButtonClick);


        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Save on watch", onNegativeButtonClick);
        alertDialog.setCancelable(true);
        return alertDialog;
    }
}
