package info.geostage.rsr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    //Button which will call RSRPechhulpActivity
    Button mainRsrButton;

    //Button which will call AboutRsrActivity
    Button mainContactRsrButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_info:
                Intent aboutIntent = new Intent(this, AboutRsrActivity.class);
                startActivity(aboutIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // flag for Internet connection status
    boolean isInternetPresent = false;

    // flag for GPS connection status
    boolean isGpsPresent = false;

    // Internet and GPS Connectivity detector class
    InetGpsConnectivityDetector inetGpsConnectivityDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inetGpsConnectivityDetector = new InetGpsConnectivityDetector(getApplicationContext());

        // get Internet status
        isInternetPresent = inetGpsConnectivityDetector.isConnectedToInternet();
        // check for Internet status
        if (isInternetPresent) {
            Log.w(TAG, "NetworkInfo:!= null");
        } else {
            // Otherwise, display no connection error dialog
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
        mainRsrButton = (Button) findViewById(R.id.mainRsrButton);
        mainContactRsrButton = (Button) findViewById(R.id.mainContactRsrButton);
    }

    //Called when the user taps the "RSR Pechhulp"  button
    public void mainRsrButton(View view) {
        Intent rsrIntent = new Intent(this, RsrPechhulpActivity.class);
        rsrIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(rsrIntent);
    }

    //Called when the user taps the "About RSR"  button
    public void mainContactRsrButton(View view) {
        Intent aboutRsrIntent = new Intent(this, AboutRsrActivity.class);
        startActivity(aboutRsrIntent);
    }

}
