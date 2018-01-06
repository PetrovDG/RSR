package info.geostage.rsr;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class AboutRsrActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    LinearLayout contacts;
    ImageView imageRSRlogo;

    // flag for Internet connection status
    boolean isInternetPresent = false;

    // flag for GPS connection status
    boolean isGpsPresent = false;

    // Internet and GPS Connectivity detector class
    InetGpsConnectivityDetector inetGpsConnectivityDetector;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_rsr);

        inetGpsConnectivityDetector = new InetGpsConnectivityDetector(getApplicationContext());

        // get Internet status
        isInternetPresent = inetGpsConnectivityDetector.isConnectedToInternet();
        // check for Internet status
        if (isInternetPresent) {

            Log.w(TAG, "NetworkInfo:!= null");
        } else {
            InetGpsConnectivityDetector.showNoInternetDialog(this);
        }

        // get GPS status
        isGpsPresent = inetGpsConnectivityDetector.isGpsProviderEnabled();
        // check for GPS status
        if (isGpsPresent) {
            Log.w(TAG, "LocationManager:isProviderEnabled");
        } else {
            // Otherwise, display no connection error dialog
            InetGpsConnectivityDetector.showGPSDisabledAlertToUser(this);
        }

        // Show the RSR contacts dialog.
        contacts = (LinearLayout) findViewById(R.id.contacts);
        contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AboutRsrActivity.this)
                        .setCancelable(true)
                        .setTitle(getString(R.string.rsrContacts))
                        .setMessage(getString(R.string.contacts))
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .create()
                        .show();
            }
        });

        imageRSRlogo = (ImageView) findViewById(R.id.imageRSRlogo);

    }

}
