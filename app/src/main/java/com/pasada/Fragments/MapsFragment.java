package com.pasada.Fragments;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.api.Result;
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
import com.google.android.libraries.places.api.model.LocationRestriction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
import com.pasada.MainActivity;
import com.pasada.Objects.Destination;
import com.pasada.Objects.Distance;
import com.pasada.Objects.Duration;
import com.pasada.Objects.ModesOfTransport;
import com.pasada.Objects.PolylineData;
import com.pasada.Objects.RideRequest;
import com.pasada.R;
import com.pasada.Utils.Utils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MapsFragment extends Fragment {

    FirebaseFirestore DB;
    FirebaseDatabase RTDB;
    FirebaseAuth AUTH;
    FirebaseUser USER;
    DatabaseReference refRiderLocation;
    ValueEventListener velRiderLocation;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        RTDB = FirebaseDatabase.getInstance();
        AUTH = FirebaseAuth.getInstance();
        USER = AUTH.getCurrentUser();
    }

    View view;
    TextView tvStatusMessage, tvDestination, tvDurationAndDistance, tvFare, tvRider;
    LinearProgressIndicator linearProgressIndicator;
    GoogleMap googleMap;
    Location currentLocation;

    LocationManager locationManager;
    MaterialAlertDialogBuilder alertBuilder;
    FusedLocationProviderClient fusedLocationProviderClient;
    GeoApiContext geoApiContext;
    ArrayList<PolylineData> arrPolylineData = new ArrayList<>();
    String placeName = "";
    LatLng userLocation = null;
    boolean onTrip = false;
    String lastRideUid = "";
    String rideUid = "";
    ArrayList<Marker> arrRiderMarkers = new ArrayList<>();

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
            // if user is booked on a ride, listen for rider location
            getCurrentLocation();
            googleMap.setOnPolylineClickListener(onPolylineClickListener);
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
                            .snippet("Duration: "+polylineData.getLeg().duration+"\n"+
                                    "Distance: "+polylineData.getLeg().distance)
                    );

                    marker.showInfoWindow();
                }
                else{
                    polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.gray_dark));
                    polylineData.getPolyline().setZIndex(0);
                }
            }
        }
    };

    ConstraintLayout clSearch;
    MaterialCardView cvBroadcast;
    TextInputLayout tilSearch;
    TextInputEditText etSearch;
    ChipGroup cgModesOfTransport;
    MaterialButton btnFindRide, btnCancel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_maps, container, false);

        checkNotificationPermsission();
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

    private void checkNotificationPermsission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void listenForRideUpdates() {
        RTDB.getReference("pendingRequests").child(USER.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    RideRequest rideRequest = snapshot.getValue(RideRequest.class);

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
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // listen for ride booking; if booked, hide broadcast cardView
        DB.collection("rides").whereEqualTo("userUid", AUTH.getUid()).whereIn("status", Arrays.asList("en_route_pickup", "en_route_destination"))
                .limit(1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        if (snapshots.isEmpty()) {
                            clSearch.setVisibility(View.VISIBLE);
                            cvBroadcast.setVisibility(View.GONE);
                            btnCancel.setVisibility(View.VISIBLE);
                            tvRider.setVisibility(View.GONE);
                            linearProgressIndicator.setVisibility(View.VISIBLE);

                            if (googleMap != null) {
                                googleMap.clear();
                            }

                            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("fixcare_cache", Context.MODE_PRIVATE);
                            if (sharedPreferences.contains("last_ride_uid")) {
                                // if shared preference still has a copy of the last ride uid,
                                // that means that the previous ride has recently been completed
                                // therefore, the app should now trigger 2 notifications:
                                // 1. ride completion notification
                                // 2. payment receipt notification
                                // then the app should then clear the last ride uid shared preference value
                                buildRideSuccessNotification(Utils.Cache.getString(requireContext(), "last_ride_uid"));

                                Utils.Cache.removeKey(requireContext(), "last_ride_uid");
                            }
                        }
                        else {
                            for (DocumentSnapshot snapshot : snapshots) {
                                if (snapshot.exists()) {
                                    RideRequest ride = snapshot.toObject(RideRequest.class);

                                    if (Objects.equals(ride.getStatus(), "en_route_pickup") ||
                                            Objects.equals(ride.getStatus(), "en_route_destination"))
                                    {
                                        // get rider location updates
                                        refRiderLocation = RTDB.getReference("riderLocations").child(ride.getRiderUid());
                                        velRiderLocation = refRiderLocation
                                                        .addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                if (snapshot.child("latitude").exists()) {
                                                                    double riderLatitude = snapshot.child("latitude").getValue(Double.class);
                                                                    double riderLongitude = snapshot.child("longitude").getValue(Double.class);

                                                                    if (!arrRiderMarkers.isEmpty()) {
                                                                        for (Marker marker : arrRiderMarkers) {
                                                                            marker.remove();
                                                                        }
                                                                    }

                                                                    Marker marker = googleMap.addMarker(new MarkerOptions()
                                                                            .icon(BitmapDescriptorFactory.fromBitmap(Utils.getBitmapFromVectorDrawable(requireContext(), R.drawable.motorcycle_solid)))
                                                                            .position(new LatLng(riderLatitude, riderLongitude)));
                                                                    // .title(ride.getUserFullName()));
                                                                    // .snippet("Tap to view details");
                                                                    arrRiderMarkers.add(marker);
                                                                }
                                                                else {
                                                                    if (!arrRiderMarkers.isEmpty()) {
                                                                        for (Marker marker : arrRiderMarkers) {
                                                                            marker.remove();
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });

                                        clSearch.setVisibility(View.GONE);
                                        cvBroadcast.setVisibility(View.VISIBLE);
                                        btnCancel.setVisibility(View.GONE);
                                        tvRider.setVisibility(View.VISIBLE);
                                        linearProgressIndicator.setVisibility(View.GONE);
                                        onTrip = true;
                                        Utils.Cache.setString(requireContext(), "last_ride_uid", ride.getUid());
                                        DB.collection("rates").document("rates")
                                                .get()
                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            double minimumFare = task.getResult().getDouble("minimumFare");
                                                            double extraFarePerKm = task.getResult().getDouble("extraFarePerKm");
                                                            long distance = ride.getDistance().getInMeters();

                                                            DecimalFormat fareFormat = new DecimalFormat("#.##");
                                                            Utils.Cache.setString(requireContext(), "last_ride_fee", "Fare: ₱" + fareFormat.format(Utils.calculateFare(distance, minimumFare, extraFarePerKm)));
                                                            tvFare.setText("Fare: ₱" + fareFormat.format(Utils.calculateFare(distance, minimumFare, extraFarePerKm)));
                                                        }
                                                    }
                                                });
                                        rideUid = ride.getUid();

                                        if (Objects.equals(ride.getStatus(), "en_route_pickup")) {
                                            tvStatusMessage.setText("Hang tight! Your rider is on their way to pick you up.");
                                        }
                                        else if (Objects.equals(ride.getStatus(), "en_route_destination")) {
                                            tvStatusMessage.setText("Your rider has arrived at your location. Have a safe trip to your destination!");
                                        }

                                        tvDestination.setText(ride.getDestination().getName());
                                        tvDurationAndDistance.setText(ride.getDistance().getHumanReadable() + " (" + ride.getDuration().getHumanReadable() + ")");


                                        DB.collection("users").document(ride.getRiderUid())
                                                .get()
                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            String firstName = task.getResult().getString("firstName");
                                                            String lastName = task.getResult().getString("lastName");

                                                            tvRider.setText(Utils.capitalizeEachWord("Rider: " + firstName + " " + lastName));
                                                        }
                                                    }
                                                });
                                    }


                                    if (currentLocation != null) {
                                        calculateDirections(new LatLng(ride.getDestination().getLatitude(), ride.getDestination().getLongitude()), true);
                                    }
                                    else {
                                        Utils.simpleDialog(requireContext(), "Can't get your location data 1", "Please check your internet connection and GPS settings and permissions.", "Okay");
                                    }
                                }
                                else {
                                    if (refRiderLocation != null && velRiderLocation != null) {
                                        refRiderLocation.removeEventListener(velRiderLocation);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void buildRideSuccessNotification(String lastRideUid) {
        String channelID = "Ride Completion";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), channelID);
        builder.setContentTitle("PasadaApp")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Your ride has been successfully completed. " + Utils.Cache.getString(requireContext(), "last_ride_fee"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_MUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) requireActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelID);

            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(channelID, "Ride Request", importance);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());


        DB.collection("rides").document(lastRideUid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            RideRequest rideRequest = task.getResult().toObject(RideRequest.class);

                            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            Fragment receiptFragment = new ReceiptFragment();
                            Bundle args = new Bundle();
                            args.putString("destination", rideRequest.getDestination().getName());
                            args.putString("rider_uid", rideRequest.getRiderUid());
                            args.putString("distance_readable", rideRequest.getDistance().getHumanReadable());
                            args.putLong("distance_meters", rideRequest.getDistance().getInMeters());
                            args.putLong("timestamp", rideRequest.getTimestampEnd());
                            receiptFragment.setArguments(args);
                            fragmentTransaction.replace(R.id.frameLayout, receiptFragment, "RECEIPT_FRAGMENT");
                            fragmentTransaction.addToBackStack("RECEIPT_FRAGMENT");
                            fragmentTransaction.commit();
                        }
                    }
                });
    }

    private void initializeViews() {
        cvBroadcast = view.findViewById(R.id.cvBroadcast);
        clSearch = view.findViewById(R.id.clSearch);
        tilSearch = view.findViewById(R.id.tilSearch);
        etSearch = view.findViewById(R.id.etSearch);
        cgModesOfTransport = view.findViewById(R.id.cgModesOfTransport);
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvDurationAndDistance = view.findViewById(R.id.tvDurationAndDistance);
        tvFare = view.findViewById(R.id.tvFare);
        tvRider = view.findViewById(R.id.tvRider);
        linearProgressIndicator = view.findViewById(R.id.linearProgressIndicator);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnFindRide = view.findViewById(R.id.btnFindRide);
    }

    private void handleUserInteraction() {
        etSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAutocompleteActivity();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clSearch.setVisibility(View.VISIBLE);
                cvBroadcast.setVisibility(View.GONE);
                googleMap.clear();

                RTDB.getReference("pendingRequests").child(AUTH.getUid()).removeValue();
            }
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, 100);
            return;
        }

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Toast.makeText(getContext()," location result is  " + locationResult, Toast.LENGTH_LONG).show();

                if (locationResult == null) {
                    Toast.makeText(getContext(),"Unable locate your current location", Toast.LENGTH_LONG).show();

                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // Toast.makeText(getContext(),"Current location is " + location.getLongitude(), Toast.LENGTH_LONG).show();

                        //TODO: UI updates.
                    }
                }
            }
        };

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;

                userLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                // userLocation = new LatLng(9.06677915639886, 123.03435888074313);
                googleMap.setMyLocationEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                listenForRideUpdates();
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
                    googleMap.clear();
                    Place place = Autocomplete.getPlaceFromIntent(data);

                    Log.d("DEBUG", "Address: "+place.getAddress()+"\nLatLng: "+place.getLatLng());
                    String[] arrAddress = place.getAddress().split(",");
                    String cleanAddress = arrAddress[0] + ", " + arrAddress[1] + ", " + arrAddress[2];
                    etSearch.setText(cleanAddress);

                    btnFindRide.setOnClickListener(view -> {
                        clSearch.setVisibility(View.GONE);
                        cvBroadcast.setVisibility(View.VISIBLE);
                        placeName = place.getName();

                        if (currentLocation != null) {
                            calculateDirections(place.getLatLng(), false);
                        }
                        else {
                            Utils.simpleDialog(requireContext(), "Can't get your location data", "Please check your internet connection and GPS settings and permissions.", "Okay");
                        }
                    });
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
                        new LatLng(9.038227026614992, 122.90762260569477), // siaton bottom left
                       //  new LatLng(9.228050230638061, 123.14108900435488) // siaton top right
                        new LatLng(9.332461380330095, 123.31541240241734) // dumaguete top right
                )
        ).build(requireContext());
        startActivityForResult(intent, 322);
    }

    private void calculateDirections(LatLng latLng, boolean hasOngoingRide){
        Log.d("TAG", "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                latLng.latitude,
                latLng.longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

        directions.alternatives(true);
            directions.origin(
                    new com.google.maps.model.LatLng(
                            /*9.06677915639886, 123.03435888074313*/
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude()
                    )
            );
        Log.d("TAG", "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d("TAG", "calculateDirections: routes: " + result.routes[0].toString());
                Log.d("TAG", "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d("TAG", "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d("TAG", "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                addPolylinesToMap(result);

                if (!hasOngoingRide) {
                    // broadcast ride request to riders
                    DatabaseReference refRideRequest = RTDB.getReference("pendingRequests").child(AUTH.getUid());
                    Map<String, Object> rideRequest = new HashMap<>();
                    String rideRequestUid = refRideRequest.push().getKey();
                    rideRequest.put("uid", rideRequestUid);
                    rideRequest.put("userUid", AUTH.getUid());
                    rideRequest.put("riderUid", "");
                    rideRequest.put("duration", result.routes[0].legs[0].duration);
                    rideRequest.put("distance", result.routes[0].legs[0].distance);
                    rideRequest.put("userLocation", userLocation);
                    rideRequest.put("destination", new Destination(
                            placeName,
                            destination.lat,
                            destination.lng
                    ));
                    rideRequest.put("modesOfTransport", new ModesOfTransport(
                            cgModesOfTransport.getCheckedChipIds().contains(R.id.chipHabalhabal),
                            cgModesOfTransport.getCheckedChipIds().contains(R.id.chipMotorpot),
                            cgModesOfTransport.getCheckedChipIds().contains(R.id.chipTrisikad)
                    ));
                    rideRequest.put("timestampStart", System.currentTimeMillis());
                    rideRequest.put("timestampEnd", 0);
                    rideRequest.put("status", "pending");

                    DB.collection("users").document(AUTH.getUid())
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot snapshot = task.getResult();
                                        rideRequest.put("userFullName", snapshot.get("firstName") + " " + snapshot.get("lastName"));

                                        refRideRequest.setValue(rideRequest);
                                    }
                                }
                            });
                }
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
                Log.d("TAG", "run: result routes: " + result.routes.length);
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
                600,
                null
        );
    }
}