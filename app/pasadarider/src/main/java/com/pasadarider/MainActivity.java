package com.pasadarider;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.pasadarider.Fragments.MapsFragment;
import com.pasadarider.Fragments.ProfileFragment;
import com.pasadarider.Fragments.RideHistoryFragment;
import com.pasadarider.Objects.RideRequest;
import com.pasadarider.Utils.Utils;

public class MainActivity extends AppCompatActivity {

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

    BottomNavigationView bottom_navbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkNotificationPermsission();
        initializeFirebase();
        initializeViews();
        backstackListener();
        softKeyboardListener();
        handleUserInteraction();
        listenForPendingRequests();
        listenForSuccessfulRides();

        bottom_navbar.findViewById(R.id.miMap).performClick();
    }

    private void checkNotificationPermsission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void listenForPendingRequests() {
        RTDB.getReference("pendingRequests")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            if (snap.exists()) {
                                RideRequest rideRequest = snap.getValue(RideRequest.class);

                                buildRideRequestNotification(rideRequest.getDestination().getName());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void listenForSuccessfulRides() {
        DB.collection("rides")
                        .whereEqualTo("riderUid", AUTH.getUid())
                        .whereEqualTo("status", "completed")
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                        if (!value.isEmpty()) {
                                            for (DocumentSnapshot docSnap : value.getDocuments()) {
                                                if (!docSnap.contains("notified")) {
                                                    DB.collection("rides").document(docSnap.getString("uid"))
                                                            .update("notified", true);
                                                    buildRideSuccessNotification(docSnap.getString("userFullName"));
                                                }
                                            }
                                        }
                                    }
                                });
    }

    private void buildRideRequestNotification(String destination) {
        String channelID = "Ride Request";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelID);
        builder.setContentTitle("PasadaRiders")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("A nearby passenger is requesting for a ride to " + destination)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_MUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
    }

    private void buildRideSuccessNotification(String userFullName) {
        String channelID = "Ride Success";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelID);
        builder.setContentTitle("PasadaRiders")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Your ride with " + userFullName + " was successful")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_MUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelID);

            if (notificationChannel == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                notificationChannel = new NotificationChannel(channelID, "Ride Success", importance);
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void handleUserInteraction() {
        bottom_navbar.setOnItemSelectedListener(item -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (item.getItemId() == R.id.miBookings) {
                Utils.hideKeyboard(this);
                if (bottom_navbar.getSelectedItemId() != R.id.miBookings) {
                    Fragment rideHistoryFragment = new RideHistoryFragment();
                    fragmentTransaction.replace(R.id.frameLayout, rideHistoryFragment, "RIDE_HISTORY_FRAGMENT");
                    fragmentTransaction.addToBackStack("RIDE_HISTORY_FRAGMENT");
                    fragmentTransaction.commit();
                }
            }
            else if (item.getItemId() == R.id.miMap) {
                Utils.hideKeyboard(this);
                if (bottom_navbar.getSelectedItemId() != R.id.miMap) {
                    Fragment mapsFragment = new MapsFragment();
                    fragmentTransaction.replace(R.id.frameLayout, mapsFragment, "MAPS_FRAGMENT");
                    fragmentTransaction.addToBackStack("MAPS_FRAGMENT");
                    fragmentTransaction.commit();
                }
            }
            else if (item.getItemId() == R.id.miProfile ) {
                Utils.hideKeyboard(this);
                if (bottom_navbar.getSelectedItemId() != R.id.miProfile) {
                    Fragment profileFragment = new ProfileFragment();
                    fragmentTransaction.replace(R.id.frameLayout, profileFragment, "PROFILE_FRAGMENT");
                    fragmentTransaction.addToBackStack("PROFILE_FRAGMENT");
                    fragmentTransaction.commit();
                }
            }
            return true;
        });
    }

    private void initializeViews() {
        bottom_navbar = findViewById(R.id.bottom_navbar);
    }

    private void backstackListener() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(() -> {
            RideHistoryFragment rideHistoryFragment = (RideHistoryFragment) getSupportFragmentManager().findFragmentByTag("RIDE_HISTORY_FRAGMENT");
            MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("MAPS_FRAGMENT");
            ProfileFragment profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("PROFILE_FRAGMENT");
            /*MenuFragment menuFragment = (MenuFragment) getSupportFragmentManager().findFragmentByTag("MENU_FRAGMENT");
            ReportsFragment reportsFragment = (ReportsFragment) getSupportFragmentManager().findFragmentByTag("REPORTS_FRAGMENT");*/

            if (rideHistoryFragment != null && rideHistoryFragment.isVisible()) {
                bottom_navbar.getMenu().getItem(0).setChecked(true);
            }
            else if (mapsFragment != null && mapsFragment.isVisible()) {
                bottom_navbar.getMenu().getItem(1).setChecked(true);
            }
            else if (profileFragment != null && profileFragment.isVisible()) {
                softKeyboardListener();
                bottom_navbar.getMenu().getItem(2).setChecked(true);
            }
            /*else if (menuFragment != null && menuFragment.isVisible()) {
                softKeyboardListener();
                bottom_navbar.getMenu().getItem(0).setChecked(true);
            }
            else if (reportsFragment != null && reportsFragment.isVisible()) {
                bottom_navbar.getMenu().getItem(2).setChecked(true);
            }*/
        });
    }

    private void softKeyboardListener() {
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, windowInsets) -> {
            WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view);
            if (insetsCompat.isVisible(WindowInsetsCompat.Type.ime())) {
                bottom_navbar.setVisibility(View.GONE);
            }
            else {
                bottom_navbar.setVisibility(View.VISIBLE);
            }
            return windowInsets;
        });
    }
}