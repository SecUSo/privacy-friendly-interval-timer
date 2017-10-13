/**
 * This file is part of Privacy Friendly Interval Timer.
 * Privacy Friendly Interval Timer is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or any later version.
 * Privacy Friendly Interval Timer is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Privacy Friendly Interval Timer. If not, see <http://www.gnu.org/licenses/>.
 */

package org.secuso.privacyfriendlyintervaltimer.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.secuso.privacyfriendlyintervaltimer.R;
import org.secuso.privacyfriendlyintervaltimer.database.PFASQLiteHelper;

/**
 * Pref dialog to ask the user if he wants to delete all saved workout statistics.
 * If confirmed the data is deleted.
 *
 * @author Alexander Karakuz
 * @version 20170702
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