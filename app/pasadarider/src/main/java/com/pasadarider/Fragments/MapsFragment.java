package com.pasadarider.Fragments;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.pasadarider.Objects.ModesOfTransport;
import com.pasadarider.Objects.PolylineData;
import com.pasadarider.Objects.RideRequest;
import com.pasadarider.R;
import com.pasadarider.Utils.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapsFragment extends Fragment {

    FirebaseFirestore DB;
    FirebaseDatabase RTDB;
    FirebaseAuth AUTH;
    FirebaseUser USER;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        RTDB = FirebaseDatabase.getInstance();
        AUTH = FirebaseAuth.getInstance();
        USER = AUTH.getCurrentUser();
    }

    View view;
    ConstraintLayout clInfo;
    TextView tvUser, tvDestination, tvDurationAndDistance, tvFare;
    MaterialButton btnPickUpPassenger, btnStartRide, btnCompleteRide, btnCancelRide;
    TextView cords;
    GoogleMap googleMap;
    Location currentLocation;

    LocationManager locationManager;
    LocationListener locationListener;
    MaterialAlertDialogBuilder alertBuilder;
    FusedLocationProviderClient fusedLocationProviderClient;
    GeoApiContext geoApiContext;
    ArrayList<PolylineData> arrPolylineData = new ArrayList<>();
    String placeName = "";
    ArrayList<RideRequest> arrRideRequests = new ArrayList<>();
    ArrayList<Marker> arrUserMarkers = new ArrayList<>();
    LatLng dummyRiderLocation = new LatLng(9.063788825197596, 123.03955493890689);
    boolean onTrip = false;

    @Override
    public void onStart() {
        super.onStart();
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            alertBuilder = new MaterialAlertDialogBuilder(requireContext());
            alertBuilder.setMessage("Your Location Service is disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent iLocationSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(iLocationSettings, 101);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    })
                    .show();
        }
    }

    private OnMapReadyCallback onMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap map) {
            googleMap = map;
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(100));
            getCurrentLocation();
            googleMap.setOnPolylineClickListener(onPolylineClickListener);
            googleMap.setOnInfoWindowClickListener(onInfoWindowClickListener);
            googleMap.setOnMarkerClickListener(onMarkerClickListener);

            // listen for pending ride requests
            RTDB.getReference("pendingRequests")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!onTrip) {
                                arrRideRequests.clear();
                                arrUserMarkers.clear();
                                googleMap.clear();

                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    RideRequest rideRequest = dataSnapshot.getValue(RideRequest.class);
                                    arrRideRequests.add(rideRequest);
                                    Log.d("TAG", String.valueOf(rideRequest));

                                    Marker marker = googleMap.addMarker(new MarkerOptions()
                                            .icon(BitmapDescriptorFactory.fromBitmap(Utils.getBitmapFromVectorDrawable(requireContext(), R.drawable.person_solid)))
                                            .position(new LatLng(rideRequest.getUserLocation().getLatitude(), rideRequest.getUserLocation().getLongitude()))
                                            .title(rideRequest.getUserFullName())
                                            .snippet("Tap to view details")
                                    );
                                    arrUserMarkers.add(marker);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            // listen for active rides
            DB.collection("rides").whereEqualTo("riderUid", AUTH.getUid())
                    .whereIn("status", Arrays.asList("en_route_pickup", "en_route_destination"))
                    .limit(1)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                RideRequest rideRequest = snapshots.getDocuments().get(0).toObject(RideRequest.class);
                                // Toast.makeText(requireContext(), "Showing: " + rideRequest.getUid(), Toast.LENGTH_SHORT).show();
                                onTrip = true;

                                clInfo.setVisibility(View.VISIBLE);
                                tvUser.setText(rideRequest.getUserFullName());
                                tvDestination.setText("To: " + rideRequest.getDestination().getName());
                                tvDurationAndDistance.setText(rideRequest.getDistance().getHumanReadable() + " (" + rideRequest.getDuration().getHumanReadable() + ")");

                                long distance = rideRequest.getDistance().getInMeters();

                                DB.collection("rates").document("rates")
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    double minimumFare = task.getResult().getDouble("minimumFare");
                                                    double extraFarePerKm = task.getResult().getDouble("extraFarePerKm");

                                                    DecimalFormat fareFormat = new DecimalFormat("#.##");
                                                    tvFare.setText("Fare: ₱" + fareFormat.format(Utils.calculateFare(distance,minimumFare ,extraFarePerKm)));
                                                }
                                            }
                                        });

                                if (Objects.equals(rideRequest.getStatus(), "en_route_pickup")) {
                                    // calculate directions from rider location to pick up location
                                    calculateDirections(
                                            new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                            new LatLng(rideRequest.getUserLocation().getLatitude(), rideRequest.getUserLocation().getLongitude())
                                    );

                                    btnPickUpPassenger.setVisibility(View.GONE);
                                    btnStartRide.setVisibility(View.VISIBLE);
                                    btnCancelRide.setVisibility(View.GONE);
                                    btnCompleteRide.setVisibility(View.GONE);
                                }
                                else if (Objects.equals(rideRequest.getStatus(), "en_route_destination")) {
                                    // calculate directions from pick up location to destination
                                    calculateDirections(
                                            new LatLng(rideRequest.getUserLocation().getLatitude(), rideRequest.getUserLocation().getLongitude()),
                                            new LatLng(rideRequest.getDestination().getLatitude(), rideRequest.getDestination().getLongitude())
                                    );

                                    btnPickUpPassenger.setVisibility(View.GONE);
                                    btnStartRide.setVisibility(View.GONE);
                                    btnCancelRide.setVisibility(View.VISIBLE);
                                    btnCompleteRide.setVisibility(View.VISIBLE);
                                }

                                btnStartRide.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        googleMap.clear();
                                        DB.collection("rides").document(rideRequest.getUid())
                                                .update("status", "en_route_destination");

                                        // calculate directions from user location to ride destination
                                        calculateDirections(
                                                new LatLng(rideRequest.getUserLocation().getLatitude(), rideRequest.getUserLocation().getLongitude()),
                                                new LatLng(rideRequest.getDestination().getLatitude(), rideRequest.getDestination().getLongitude())
                                        );

                                        btnStartRide.setVisibility(View.GONE);
                                        btnCancelRide.setVisibility(View.VISIBLE);
                                        btnCompleteRide.setVisibility(View.VISIBLE);
                                    }
                                });

                                btnCompleteRide.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        googleMap.clear();
                                        clInfo.setVisibility(View.GONE);
                                        btnPickUpPassenger.setVisibility(View.VISIBLE);
                                        btnCancelRide.setVisibility(View.GONE);
                                        btnCompleteRide.setVisibility(View.GONE);

                                        DB.collection("rides").document(rideRequest.getUid())
                                                .update("status", "completed", "timestampEnd", System.currentTimeMillis())
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        onTrip = false;


                                                    }
                                                });
                                    }
                                });

                                btnCancelRide.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        googleMap.clear();
                                        clInfo.setVisibility(View.GONE);
                                        DB.collection("rides").document(rideRequest.getUid())
                                                .update("status", "cancelled", "timestampEnd", System.currentTimeMillis())
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        onTrip = false;
                                                    }
                                                });
                                    }
                                });
                            }
                            else {
                                // Toast.makeText(requireContext(), "No docs", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    };

    private GoogleMap.OnInfoWindowClickListener onInfoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(@NonNull Marker marker) {
            clInfo.setVisibility(View.VISIBLE);
            RideRequest rideRequest = arrRideRequests.get(arrUserMarkers.indexOf(marker));

            tvUser.setText(rideRequest.getUserFullName());
            tvDestination.setText("To: " + rideRequest.getDestination().getName());
            tvDurationAndDistance.setText(rideRequest.getDistance().getHumanReadable() + " (" + rideRequest.getDuration().getHumanReadable() + ")");

            long distance = rideRequest.getDistance().getInMeters();
            DB.collection("rates").document("rates")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                double minimumFare = task.getResult().getDouble("minimumFare");
                                double extraFarePerKm = task.getResult().getDouble("extraFarePerKm");

                                DecimalFormat fareFormat = new DecimalFormat("#.##");
                                tvFare.setText("Fare: ₱" + fareFormat.format(Utils.calculateFare(distance, minimumFare, extraFarePerKm)));
                            }
                        }
                    });

            calculateDirections(
                    new LatLng(rideRequest.getUserLocation().getLatitude(), rideRequest.getUserLocation().getLongitude()),
                    new LatLng(rideRequest.getDestination().getLatitude(), rideRequest.getDestination().getLongitude())
            );

            btnPickUpPassenger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    googleMap.clear();
                    onTrip = true;

                    // migrate data from rtdb to firestore
                    Map<String, Object> rideRequest2 = new HashMap<>();
                    rideRequest2.put("uid", rideRequest.getUid());
                    rideRequest2.put("userUid", rideRequest.getUserUid());
                    rideRequest2.put("riderUid", AUTH.getUid());
                    rideRequest2.put("duration", rideRequest.getDuration());
                    rideRequest2.put("distance", rideRequest.getDistance());
                    rideRequest2.put("userLocation", rideRequest.getUserLocation());
                    rideRequest2.put("userFullName", rideRequest.getUserFullName());
                    rideRequest2.put("destination", rideRequest.getDestination());
                    rideRequest2.put("modesOfTransport", rideRequest.getModesOfTransport());
                    rideRequest2.put("timestampStart", rideRequest.getTimestampStart());
                    rideRequest2.put("timestampEnd", rideRequest.getTimestampEnd());
                    rideRequest2.put("status", "en_route_pickup");

                    // delete rtdb data
                    DatabaseReference refRideRequest = RTDB.getReference("pendingRequests").child(rideRequest.getUserUid());
                    refRideRequest.removeValue();

                    // write firestore data
                    DB.collection("rides").document(rideRequest.getUid()).set(rideRequest2);

                    /*// calculate directions from rider location to pick up location
                    calculateDirections(
                            // dummyRiderLocation,
                            new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                            new LatLng(rideRequest.getUserLocation().getLatitude(), rideRequest.getUserLocation().getLongitude())
                    );
                    btnPickUpPassenger.setVisibility(View.GONE);
                    btnStartRide.setVisibility(View.VISIBLE);*/
                }
            });
        }
    };

    private GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(@NonNull Marker marker) {
            return false;
        }
    };

    private GoogleMap.OnPolylineClickListener onPolylineClickListener = new GoogleMap.OnPolylineClickListener() {
        @Override
        public void onPolylineClick(@NonNull Polyline polyline) {
            for(PolylineData polylineData: arrPolylineData){
                Log.d("TAG", "onPolylineClick: toString: " + polylineData.toString());
                if(polyline.getId().equals(polylineData.getPolyline().getId())){
                    polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.orange_web));
                    polylineData.getPolyline().setZIndex(1);

                    LatLng endPoint = new LatLng(
                            polylineData.getLeg().endLocation.lat,
                            polylineData.getLeg().endLocation.lng
                    );

                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(endPoint)
                            .title(placeName)
                            .snippet("Duration: "+polylineData.getLeg().duration)
                    );

                    marker.showInfoWindow();
                }
                else {
                    polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.gray_dark));
                    polylineData.getPolyline().setZIndex(0);
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_maps, container, false);

        initializeViews();
        initializeFirebase();

        // initialize places api
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyASRMRUuhg0SEHi9Su1s_3aSl6keLN1HdY");
        }

        // initialize location provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(onMapReadyCallback);
        }
        if (geoApiContext == null) {
            geoApiContext = new GeoApiContext.Builder().apiKey("AIzaSyASRMRUuhg0SEHi9Su1s_3aSl6keLN1HdY").build();
        }
        handleUserInteraction();

        return view;
    }

    private void initializeViews() {
        clInfo = view.findViewById(R.id.clInfo);
        btnPickUpPassenger = view.findViewById(R.id.btnPickUpPassenger);
        btnStartRide = view.findViewById(R.id.btnStartRide);
        btnCompleteRide = view.findViewById(R.id.btnCompleteRide);
        btnCancelRide = view.findViewById(R.id.btnCancelRide);
        tvUser = view.findViewById(R.id.tvUser);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvDurationAndDistance = view.findViewById(R.id.tvDurationAndDistance);
        tvFare = view.findViewById(R.id.tvFare);
    }

    private void handleUserInteraction() {
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, 100);
            return;
        }

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
            }
        };

        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                // Toast.makeText(getContext()," location result is  " + locationResult, Toast.LENGTH_LONG).show();

                if (locationResult == null) {
                    // Toast.makeText(getContext(),"Unable locate your current location", Toast.LENGTH_LONG).show();

                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // Toast.makeText(getContext(),"Current location is " + location.getLongitude(), Toast.LENGTH_LONG).show();

                        if (onTrip) {
                            HashMap<String, Double> riderLocation = new HashMap<>();
                            riderLocation.put("latitude", location.getLatitude());
                            riderLocation.put("longitude", location.getLongitude());

                            RTDB.getReference("riderLocations").child(AUTH.getUid())
                                    .setValue(riderLocation);
                        }
                        else {
                            RTDB.getReference("riderLocations").child(AUTH.getUid()).removeValue();
                        }

                        Log.d("TAG", "LATLNG: " + location.getLatitude() + ", " + location.getLongitude() + "; ONTRIP: " + onTrip);
                        //TODO: UI updates.
                    }
                }
            }
        };

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        task.addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;

                LatLng myLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                googleMap.setMyLocationEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));
                // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(9.064550119164466, 123.03741683822416), 15));

                if (onTrip) {
                    RTDB.getReference("userLocations").child(AUTH.getUid())
                            .setValue(location);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 101:
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    alertBuilder.show();
                    return;
                }
                break;
            case 322:
                if (resultCode == RESULT_OK) {
                    Place place = Autocomplete.getPlaceFromIntent(data);

                    Log.d("DEBUG", "Address: "+place.getAddress()+"\nLatLng: "+place.getLatLng());
                }
                else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(data);

                    Log.d("DEBUG", "STATUS: "+status);
                }
                else if (resultCode == RESULT_CANCELED) {
                    Log.d("DEBUG", "STATUS: Cancelled");
                }
        }
    }

    public void startAutocompleteActivity() {
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.NAME)
        ).setTypesFilter(new ArrayList<String>() {{
            add(TypeFilter.ESTABLISHMENT.toString().toLowerCase());
        }}).setCountries(
                Arrays.asList("PH")
        ).setLocationRestriction(
                RectangularBounds.newInstance(
                        new LatLng(9.038227026614992, 122.90762260569477),
                        new LatLng(9.228050230638061, 123.14108900435488)
                )
        ).build(requireContext());
        startActivityForResult(intent, 322);

        /*.setLocationRestriction(RectangularBounds.newInstance(
                new LatLng(9.038227026614992, 122.90762260569477),
                new LatLng(9.228050230638061, 123.14108900435488)
        ))*/
    }

    private void calculateDirections(LatLng startPoint, LatLng endPoint){
        Log.d("TAG", "calculateDirections: calculating directions.");
        com.google.maps.model.LatLng origin = new com.google.maps.model.LatLng(
                startPoint.latitude,
                startPoint.longitude
        );
        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                endPoint.latitude,
                endPoint.longitude
        );

        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

        directions.alternatives(true);
        directions.origin(origin);

        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e("TAG", "calculateDirections: Failed to get directions: " + e.getMessage() );
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (arrPolylineData.size() > 0) {
                    for (PolylineData polylineData : arrPolylineData) {
                        polylineData.getPolyline().remove();
                    }
                    arrPolylineData.clear();
                    arrPolylineData = new ArrayList<>();
                }

                double duration = 99999999;
                for(DirectionsRoute route: result.routes){
                    Log.d("TAG", "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){

//                        Log.d("TAG", "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(getActivity(), R.color.gray_dark));
                    polyline.setClickable(true);
                    arrPolylineData.add(new PolylineData(polyline, route.legs[0]));

                    double tempDuration = route.legs[0].duration.inSeconds;
                    if (tempDuration < duration) {
                        duration = tempDuration;
                        onPolylineClickListener.onPolylineClick(polyline);
                        zoomRoute(polyline.getPoints());
                    }
                }
            }
        });
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                1500,
                null
        );
    }
}