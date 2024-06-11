package com.pasada.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.pasada.Adapters.RideHistoryAdapter;
import com.pasada.Objects.RideRequest;
import com.pasada.R;

import java.util.ArrayList;
import java.util.Arrays;

public class RideHistoryFragment extends Fragment {

    FirebaseFirestore DB;
    FirebaseAuth AUTH;
    FirebaseUser USER;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        AUTH = FirebaseAuth.getInstance();
        USER = AUTH.getCurrentUser();
    }
    View view;
    TabLayout tlRideRequestReports;

    ArrayList<RideRequest> arrRideHistory;
    RideHistoryAdapter rideHistoryAdapter;


    RecyclerView rvRideHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_ride_history, container, false);

        initializeFirebase();
        initializeViews();
        loadRecyclerView(tlRideRequestReports.getSelectedTabPosition());
        handleUserInteraction();

        return view;
    }

    private void initializeViews() {
        rvRideHistory = view.findViewById(R.id.rvRideHistory);
        tlRideRequestReports = view.findViewById(R.id.tlRideRequestReports);
    }

    private void loadRecyclerView(int tabIndex) {
        arrRideHistory = new ArrayList<>();
        rvRideHistory = view.findViewById(R.id.rvRideHistory);
        rvRideHistory.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        rvRideHistory.setLayoutManager(linearLayoutManager);

        CollectionReference refRideHistory = DB.collection("rides");
        Query qryRideHistory = refRideHistory.whereEqualTo("userUid", AUTH.getUid())
                .whereIn("status", Arrays.asList("completed", "cancelled"))
                .orderBy("timestampEnd", Query.Direction.ASCENDING);

        /*if (tabIndex == 0) {
            qryRideHistory = refRideHistory.whereEqualTo("userUid", AUTH.getUid()).whereEqualTo("status", "PENDING").orderBy("timestamp", Query.Direction.ASCENDING);
        }
        else if (tabIndex == 1) {
            qryRideHistory = refRideHistory.whereEqualTo("userUid", AUTH.getUid()).whereEqualTo("status", "HEARING SCHEDULED").orderBy("timestamp", Query.Direction.ASCENDING);
        }
        else if (tabIndex == 2) {
            qryRideHistory = refRideHistory.whereEqualTo("userUid", AUTH.getUid()).whereEqualTo("status", "UNDER INVESTIGATION").orderBy("timestamp", Query.Direction.ASCENDING);
        }
        else {
            qryRideHistory = refRideHistory.whereEqualTo("userUid", AUTH.getUid()).whereIn("status", Arrays.asList("RESOLVED", "CLOSED")).orderBy("timestamp", Query.Direction.ASCENDING);
        }*/

        qryRideHistory.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
                arrRideHistory.clear();

                if (queryDocumentSnapshots == null) {
                    return;
                }

                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                    RideRequest rideRequest = documentSnapshot.toObject(RideRequest.class);

                    arrRideHistory.add(rideRequest);
                    rideHistoryAdapter.notifyDataSetChanged();
                }

                if (arrRideHistory.isEmpty()) {
                    rvRideHistory.setVisibility(View.GONE);
                }
                else {
                    rvRideHistory.setVisibility(View.VISIBLE);
                }
            }
        });

        rideHistoryAdapter = new RideHistoryAdapter(getContext(), arrRideHistory);
        rvRideHistory.setAdapter(rideHistoryAdapter);
        rideHistoryAdapter.notifyDataSetChanged();
    }

    private void handleUserInteraction() {
        tlRideRequestReports.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadRecyclerView(tlRideRequestReports.getSelectedTabPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
}