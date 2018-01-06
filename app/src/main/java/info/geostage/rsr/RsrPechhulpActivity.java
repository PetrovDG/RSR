package info.geostage.rsr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static info.geostage.rsr.Constants.RESULT_DATA_KEY;

public class RsrPechhulpActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * The formatted location address.
     */
    public String mAddressOutput;
    /**
     * Displays the location address.
     */
    TextView locationAddressTextView, title, rememberText, contactTextPhone, belkostenTitle, belkosten;
    Button rsrButton;
    RelativeLayout map_screen_layout;
    LinearLayout rsrContactLayout, belRsrButtonLayout, closePopUpButtonLayout;
    ImageView imgMyLocation;

    //Default location parameters
    private static final int CURR_LOC_ZOOM = 17;
    private static final int DEFAULT_ZOOM = 7;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 77;
    private GoogleApiClient mGoogleApiClient;
    private Marker mCurrLocationMarker;
    private GoogleMap m_map;
    CameraPosition mCameraPosition;
    LocationRequest mLocationRequest;
    boolean mapReady = false;
    MapFragment mapFragment;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    //The fastest rate for active location updates. Exact. Updates will never be more frequent
    //than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // A default location to use when location permission is not granted.
    private final LatLng mDefaultLocation = new LatLng(52.370216, 4.895168);

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastLocation;

    // flag for Internet connection status
    boolean isInternetPresent = false;

    // flag for GPS connection status
    boolean isGpsPresent = false;

    // Internet and GPS Connectivity detector class
    InetGpsConnectivityDetector inetGpsConnectivityDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_rsr_pechhulp);

        // Build the mGoogleApiClient
        buildGoogleAPIClient();

        imgMyLocation = (ImageView) findViewById(R.id.imgMyLocation);
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

        mResultReceiver = new AddressResultReceiver(new Handler());

        map_screen_layout = (RelativeLayout) findViewById(R.id.map_screen_layout);
        rsrContactLayout = (LinearLayout) findViewById(R.id.rsrContactLayout);
        belRsrButtonLayout = (LinearLayout) findViewById(R.id.belRsrButtonLayout);
        closePopUpButtonLayout = (LinearLayout) findViewById(R.id.closePopUpButtonLayout);
        belkostenTitle = (TextView) findViewById(R.id.belkostenTitle);
        belkosten = (TextView) findViewById(R.id.belkosten);

        boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
        if (tabletSize) {
            rsrContactLayout.setVisibility(View.VISIBLE);
            belRsrButtonLayout.setVisibility(View.GONE);
            closePopUpButtonLayout.setVisibility(View.GONE);
            belkostenTitle.setVisibility(View.GONE);
            belkosten.setVisibility(View.GONE);
        } else {
            rsrContactLayout.setVisibility(View.GONE);
            belRsrButtonLayout.setVisibility(View.VISIBLE);
            closePopUpButtonLayout.setVisibility(View.GONE);
            belkostenTitle.setVisibility(View.GONE);
            belkosten.setVisibility(View.GONE);
        }

        rsrButton = (Button) findViewById(R.id.belRsrButton);
        final int imgCallRsrNu = R.drawable.main_btn_tel;
        final int imgCallNu = R.drawable.main_btn_phone;
        rsrButton.setTag(1);
        rsrButton.setCompoundDrawablesWithIntrinsicBounds(imgCallRsrNu, 0, 0, 0);
        rsrButton.setText(getString(R.string.belRsrNu));
        rsrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int status = (Integer) v.getTag();
                if (status == 1) {
                    rsrButton.setText(getString(R.string.belRsr));
                    v.setTag(0);
                    rsrButton.setCompoundDrawablesWithIntrinsicBounds(imgCallNu, 0, 0, 0);
                    closePopUpButtonLayout.setVisibility(View.VISIBLE);
                    belkostenTitle.setVisibility(View.VISIBLE);
                    belkosten.setVisibility(View.VISIBLE);
                    belRsrButtonLayout.setBackgroundColor(Color.parseColor("#e0bbdc00"));
                    map_screen_layout.setBackgroundColor(Color.parseColor("#b3000000"));
                } else {
                    rsrButton.setText(getString(R.string.belRsrNu));
                    v.setTag(1);
                    rsrButton.setCompoundDrawablesWithIntrinsicBounds(imgCallRsrNu, 0, 0, 0);
                    closePopUpButtonLayout.setVisibility(View.GONE);
                    belkostenTitle.setVisibility(View.GONE);
                    belkosten.setVisibility(View.GONE);
                    belRsrButtonLayout.setBackgroundColor(0x00000000);
                    map_screen_layout.setBackgroundColor(0x00000000);
                    Intent intentDial = new Intent(Intent.ACTION_DIAL);
                    intentDial.setData(Uri.parse("tel:" + getString(R.string.rsr_phone)));
                    startActivity(intentDial);
                }

            }

        });

        contactTextPhone = (TextView) findViewById(R.id.contactTextPhone);
        contactTextPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + getString(R.string.rsr_phone)));
                startActivity(intent);
            }
        });

        closePopUpButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rsrButton.setTag(1);
                rsrButton.setText(getString(R.string.belRsrNu));
                rsrButton.setCompoundDrawablesWithIntrinsicBounds(imgCallRsrNu, 0, 0, 0);
                closePopUpButtonLayout.setVisibility(View.GONE);
                belkostenTitle.setVisibility(View.GONE);
                belkosten.setVisibility(View.GONE);
                belRsrButtonLayout.setBackgroundColor(0x00000000);
                map_screen_layout.setBackgroundColor(0x00000000);
            }
        });

        // Set defaults, then update using values stored in the Bundle.
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Build the options menu..
        invalidateOptionsMenu();
    }

    // Build the GoogleApiClient
    private synchronized void buildGoogleAPIClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        if (m_map != null) {
            savedInstanceState.putParcelable(KEY_CAMERA_POSITION, m_map.getCameraPosition());
            savedInstanceState.putParcelable(KEY_LOCATION, mLastLocation);
            // Save the address string.
            savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
            savedInstanceState.putParcelable(Constants.RECEIVER, mResultReceiver);
            savedInstanceState.putParcelable(Constants.LOCATION_DATA_EXTRA, mLastLocation);
            super.onSaveInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Retrieve location and camera position from saved instance state.
        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        imgMyLocation.setVisibility(View.VISIBLE);
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPermissions()) {
            // Prompt the user for permission.
            getLocationPermission();

        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,
                        this);
            }
            getAddress();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    // To prevent this activity from being destroyed.
    @Override
    public void onBackPressed() {
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent backIntent = new Intent(RsrPechhulpActivity.this, MainActivity.class);
                        backIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(backIntent);
                    }
                };

        showBackDialog(discardButtonClickListener);
    }

    /**
     * Show a dialog that warns the user
     * if they go back to the MainActivity.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to go back
     */
    private void showBackDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.go_back_button_pressed);
        builder.setPositiveButton(R.string.yes, discardButtonClickListener);
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_info);
        menuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // To prevent this activity from being destroyed.
                                // It is better to use SharedPreferences.
                                Intent backIntent = new Intent(RsrPechhulpActivity.this, MainActivity.class);
                                backIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(backIntent);
                            }
                        };

                // Show a dialog that notifies the user
                showBackDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates fields based on data stored in the bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Retrieve location and camera position from saved instance state.
            mLastLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
            }

            mResultReceiver = savedInstanceState.getParcelable(Constants.RECEIVER);
            mLastLocation = savedInstanceState.getParcelable(Constants.LOCATION_DATA_EXTRA);
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    private void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in RsrPechulp.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(RESULT_DATA_KEY);

            // Updates the map's UI settings based on whether the user has granted location permission.
            if (resultCode == Constants.SUCCESS_RESULT) {
                updateLocationUI();
            }
        }
    }

    /**
     * Shows a toast with the given text if a Geocoder is available.
     */
    private void showToastGeocoder(String text) {
        Toast.makeText(this, text,
                Toast.LENGTH_LONG).show();
    }


    @Override
    public void onConnected(Bundle bundle) {
        // Gets the best and most recent location currently available,
        // which may be null in rare cases when a location is not available.
        @SuppressLint("MissingPermission")
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            startIntentService();

            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                showToastGeocoder(getString(R.string.no_geocoder_available));
            }
            createLocationRequest();

            getDeviceLocation();

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapReady = true;
        m_map = googleMap;

        // Use a custom info window adapter to handle multiple lines of text in the info window contents.
        m_map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker marker) {
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window,
                        (FrameLayout) findViewById(R.id.map), false);
                title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(getString(R.string.your_location));
                locationAddressTextView = ((TextView) infoWindow.findViewById(R.id.snippet));
                locationAddressTextView.setText(mAddressOutput);
                rememberText = ((TextView) infoWindow.findViewById(R.id.remember_text));
                rememberText.setText(getString(R.string.remember_location));

                return infoWindow;
            }

            @Override
            public View getInfoContents(Marker arg0) {
                return null;
            }
        });

    }

    /**
     * Gets the address for the last known location.
     */
    @SuppressLint("MissingPermission")
    private void getAddress() {
        mFusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            startIntentService();
                            Log.w(TAG, "onSuccess:null");
                        }
                        mLastLocation = location;

                        // Determine whether a Geocoder is available.
                        if (!Geocoder.isPresent()) {
                            showToastGeocoder(getString(R.string.no_geocoder_available));
                        }

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getLastLocation:onFailure", e);
                    }
                });
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        mLastLocation = task.getResult();
                        m_map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastLocation.getLatitude(),
                                        mLastLocation.getLongitude()), CURR_LOC_ZOOM));

                    } else {
                        m_map.animateCamera(CameraUpdateFactory
                                .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                        m_map.getUiSettings().setMyLocationButtonEnabled(false);
                        m_map.getUiSettings().setZoomControlsEnabled(false);
                        m_map.getUiSettings().setMapToolbarEnabled(false);
                    }
                }
            });

        } catch (
                SecurityException e)

        {
            Log.e("Exception: %s", e.getMessage());
        }

    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        //move map camera
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, CURR_LOC_ZOOM);
        m_map.animateCamera(cameraUpdate);

    }

    /**
     * Sets up the location request.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            // Show an explanation to the user
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.permission_title))
                    .setMessage(getString(R.string.permission_text))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(RsrPechhulpActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                    })
                    .create()
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,
                            this);
                    getDeviceLocation();
                    getAddress();
                }
            } else {
                // Permission denied.
                // Notify the user via a AlertDialog that they have rejected a core permission for the
                // app, which makes the Activity useless.
                // Show an explanation to the user
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(getString(R.string.permission_title))
                        .setMessage(getString(R.string.permission_denied_explanation))
                        .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("info.geostage.rsr",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });

            }
        }
    }

    /**
     * Updates the map's UI settings.
     */
    private void updateLocationUI() {
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker));
        mCurrLocationMarker = m_map.addMarker(markerOptions);
        mCurrLocationMarker.setVisible(true);
        mCurrLocationMarker.showInfoWindow();
        //move map camera
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, CURR_LOC_ZOOM);
        m_map.animateCamera(cameraUpdate);
        m_map.getUiSettings().setMyLocationButtonEnabled(false);
        m_map.getUiSettings().setZoomControlsEnabled(false);
        m_map.getUiSettings().setMapToolbarEnabled(false);
        imgMyLocation.setVisibility(View.INVISIBLE);
    }

}
