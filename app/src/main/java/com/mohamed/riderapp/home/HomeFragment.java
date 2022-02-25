package com.mohamed.riderapp.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mohamed.riderapp.R;
import com.mohamed.riderapp.callback.IFFirebaseDriverInfoListener;
import com.mohamed.riderapp.callback.IFFirebaseFailedListener;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // load driver
    private double instance = 1.0; // default in kms
    private static final double LIMIT_RANGE = 10.0;
    private Location previousLocation, currentLocation;

    private boolean isFirstTime = true;

    // listener
    IFFirebaseDriverInfoListener ifFirebaseDriverInfoListener;
    IFFirebaseFailedListener ifFirebaseFailedListener;

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, null, false);
        init();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return view;
    }

    private void init() {
        ifFirebaseDriverInfoListener = this;
        ifFirebaseFailedListener = this;
        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                // if user has change location, calculate and load driver again
                if (isFirstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    isFirstTime = false;
                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }

                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE)
                    loadAvailableDrivers();
                else {
                    // Do nothing
                }
            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(() -> {
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            fusedLocationProviderClient.getLastLocation()
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show()).addOnSuccessListener(location -> {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
            });
            return true;
        });
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);

                        // layout button
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                                .findViewById(Integer.parseInt("2"));

                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        // right button
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 50);

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "" + permissionDeniedResponse.getPermissionName() + " need enable", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                })
                .check();

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success)
                Toast.makeText(getContext(), "load map style failed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }
}