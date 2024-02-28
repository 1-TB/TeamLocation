// Jordan, Shawn B, Jaime
package com.jordan.teamlocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import java.util.Arrays;

public class MainActivity
        extends AppCompatActivity implements OnMapReadyCallback {
    // UI elements
    private TextView latLong;
    private AutocompleteSupportFragment autocompleteFragment;
    private Button btnLocation;
    private Button btnAddress;

    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // default location
    private final LatLng mDefaultLocation = new LatLng(37.2083341, -93.2106085);
    // default zoom
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // have they given us permission to use their location
    private boolean mLocationPermissionGranted;

    // The last location where the device is currently located.
    private Location mLastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the layout
        setContentView(R.layout.activity_main);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(
                        R.id.map);
        mapFragment.getMapAsync(this);
        // set the ui elements
        btnLocation = findViewById(R.id.btnLocation);
        btnLocation.setOnClickListener(this::handleLocateBtn);
        latLong = findViewById(R.id.txtLatLong);
        btnAddress = findViewById(R.id.btnAddress);
        // set button click listener
        btnAddress.setOnClickListener(this::handleAddressBtn);
        // start up the places sdk/autocomplete fragment
        //(the search bar for the address)
        if (!Places.isInitialized()) {
            ApplicationInfo app = null;
            try {
                app = this.getPackageManager().getApplicationInfo(
                        this.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            Bundle bundle = app.metaData;
            String apiKey = bundle.getString("com.google.android.geo.API_KEY");
            Places.initialize(getApplicationContext(), apiKey);
        }

        autocompleteFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.inputAddress);
        autocompleteFragment.setPlaceFields(
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        View fragmentView = autocompleteFragment.getView();
        View autoCompleteInput = fragmentView.findViewById(R.id.inputAddress);
        autoCompleteInput.setBackgroundColor(Color.WHITE);
        autocompleteFragment.setOnPlaceSelectedListener(
                new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(@NonNull Place place) {
                        // Get info about the selected place.
                        Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());

                        LatLng latLng = place.getLatLng();
                        if (latLng != null) {
                            Log.i(TAG,
                                    "Location: " + latLng.latitude + ", " + latLng.longitude);
                            // set the location of the map to the selected place
                            mLastKnownLocation.setLatitude(latLng.latitude);
                            mLastKnownLocation.setLongitude(latLng.longitude);
                        }
                    }

                    @Override
                    public void onError(@NonNull Status status) {
                        // error
                        Log.i(TAG, "An error occurred: " + status);
                    }
                });
    }
    @SuppressLint("SetTextI18n")
    // locate button handler
    private void handleLocateBtn(View v) {
        getDeviceLocation();
        latLong.setVisibility(View.VISIBLE);
        latLong.setText("Latitude: " + mLastKnownLocation.getLatitude()
                + " Longitude: " + mLastKnownLocation.getLongitude());
    }
    // address button handler
    private void handleAddressBtn(View v) {
        LatLng latLng = new LatLng(
                mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        latLong.setText("Latitude: " + mLastKnownLocation.getLatitude()
                + " Longitude: " + mLastKnownLocation.getLongitude());
        latLong.setVisibility(View.VISIBLE);
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mMap != null) {
            outState.putParcelable("camera_position", mMap.getCameraPosition());
            outState.putParcelable("location", mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in
         * rare cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult =
                        mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(
                        this, new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful()) {
                                    // Set the map's camera position to the current last known
                                    // location of the device.
                                    mLastKnownLocation = task.getResult();
                                    if (mLastKnownLocation != null) {
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                                new LatLng(mLastKnownLocation.getLatitude(),
                                                        mLastKnownLocation.getLongitude()),
                                                DEFAULT_ZOOM));
                                    }
                                } else {
                                    Log.d(TAG, "Current location is null. Using defaults.");
                                    Log.e(TAG, "Exception: %s", task.getException());
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                            mDefaultLocation, DEFAULT_ZOOM));
                                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                                }
                            }
                        });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted
     * location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
