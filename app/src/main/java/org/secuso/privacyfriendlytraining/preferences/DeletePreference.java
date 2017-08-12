package org.secuso.privacyfriendlytraining.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.database.PFASQLiteHelper;

/**
 * Pref dialog to ask the user if he wants to delete all saved workout statistics.
 * If confirmed the data is deleted.
 *
 * @author Alexander Karakuz
 * @version 20170702
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class DeletePreference extends DialogPreference {

    public DeletePreference(Context oContext, AttributeSet attrs){
        super(oContext, attrs);
    }

    @Override
    protected void onClick()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(getContext().getResources().getString(R.string.pref_delete_statistics_dialog_title));
        dialog.setMessage(getContext().getResources().getString(R.string.pref_delete_statistics_dialog_info));
        dialog.setCancelable(true);
        dialog.setPositiveButton(getContext().getResources().getString(R.string.alert_confirm_dialog_positive), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                PFASQLiteHelper database  = new PFASQLiteHelper(getContext());
                database.deleteAllWorkokutData();

                Toast.makeText(getContext(), getContext().getResources().getString(R.string.pref_delete_statistics_dialog_toast), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setNegativeButton(getContext().getResources().getString(R.string.alert_confirm_dialog_negative), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }
}