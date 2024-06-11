package com.pasada;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pasada.Fragments.MapsFragment;
import com.pasada.Fragments.ProfileFragment;
import com.pasada.Fragments.RideHistoryFragment;
import com.pasada.R;
import com.pasada.Utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottom_navbar;
    TextInputLayout tilSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        backstackListener();
        softKeyboardListener();
        handleUserInteraction();


        bottom_navbar.findViewById(R.id.miMap).performClick();
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
        tilSearch = findViewById(R.id.tilSearch);
        bottom_navbar = findViewById(R.id.bottom_navbar);
    }

    private void backstackListener() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(() -> {
            MapsFragment mapsFragment = (MapsFragment) getSupportFragmentManager().findFragmentByTag("MAPS_FRAGMENT");
            ProfileFragment profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("PROFILE_FRAGMENT");
            /*MenuFragment menuFragment = (MenuFragment) getSupportFragmentManager().findFragmentByTag("MENU_FRAGMENT");
            ReportsFragment reportsFragment = (ReportsFragment) getSupportFragmentManager().findFragmentByTag("REPORTS_FRAGMENT");*/

            if (mapsFragment != null && mapsFragment.isVisible()) {
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