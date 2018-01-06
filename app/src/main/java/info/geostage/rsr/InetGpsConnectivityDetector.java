package info.geostage.rsr;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;

import static android.content.Context.LOCATION_SERVICE;

class InetGpsConnectivityDetector {

    private Context mcontext;

    InetGpsConnectivityDetector(Context context) {
        this.mcontext = context;
    }

    boolean isConnectedToInternet() {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                mcontext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            // Get details on the currently active default data network
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
        }
        return false;
    }

    boolean isGpsProviderEnabled() {
        // Get a reference to the LocationManager to check state of GPS connectivity
        LocationManager locationManager = (LocationManager) mcontext.getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Show a dialog that warns the user there are is no Internet connection.
     */
    static void showNoInternetDialog(final Context mcontext) {

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(mcontext);
        builder.setCancelable(false);
        builder.setMessage(R.string.no_internet_connection);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked "Cancel" button, close the current activity.
                ((Activity) mcontext).finish();
            }
        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Ok" button
                ((Activity) mcontext).finish();
                Intent alertDialog = new Intent(mcontext, mcontext.getClass());
                (mcontext).startActivity(alertDialog);
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Show a dialog that warns the user there are is no GPS connection.
     */
    static void showGPSDisabledAlertToUser(final Context mcontext) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mcontext);
        alertDialogBuilder.setMessage(R.string.no_gps_message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked "Cancel" button, close the current activity.
                        ((Activity) mcontext).finish();
                    }
                });

        alertDialogBuilder.setPositiveButton(R.string.settings,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        (mcontext).startActivity(callGPSSettingIntent);
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

}


