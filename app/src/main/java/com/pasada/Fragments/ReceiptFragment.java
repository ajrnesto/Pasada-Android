package com.pasada.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasada.AuthenticationActivity;
import com.pasada.MainActivity;
import com.pasada.R;
import com.pasada.Utils.Utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class ReceiptFragment extends Fragment {

    FirebaseFirestore DB;
    FirebaseAuth AUTH;
    FirebaseUser USER;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        AUTH = FirebaseAuth.getInstance();
        USER = AUTH.getCurrentUser();
    }

    View view;
    TextView tvRoute, tvRiderFullName, tvDistance, tvFare, tvTimestamp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_receipt, container, false);

        initializeFirebase();
        initializeViews();
        loadReceipt();

        return view;
    }

    private void initializeViews() {
        tvRoute = view.findViewById(R.id.tvRoute);
        tvRiderFullName = view.findViewById(R.id.tvRiderFullName);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvFare = view.findViewById(R.id.tvFare);
        tvTimestamp = view.findViewById(R.id.tvTimestamp);
    }

    private void loadReceipt() {
        String destination = getArguments().getString("destination");
        String riderUid = getArguments().getString("rider_uid");
        String distanceReadable = getArguments().getString("distance_readable");
        long distanceMeters = getArguments().getLong("distance_meters");
        long timestamp = getArguments().getLong("timestamp");

        DB.collection("users").document(riderUid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            tvRiderFullName.setText(task.getResult().getString("firstName") + " " + task.getResult().getString("lastName"));
                        }
                    }
                });

        tvRoute.setText(destination);
        tvDistance.setText(distanceReadable);

        DB.collection("rates").document("rates")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            double minimumFare = task.getResult().getDouble("minimumFare");
                            double extraFarePerKm = task.getResult().getDouble("extraFarePerKm");

                            DecimalFormat fareFormat = new DecimalFormat("#.##");
                            tvFare.setText("Fare: ₱" + fareFormat.format(Utils.calculateFare(distanceMeters, minimumFare, extraFarePerKm)));
                        }
                    }
                });

        SimpleDateFormat sdfTimestamp = new SimpleDateFormat("MMM dd, yyyy HH:mm aa");
        tvTimestamp.setText(sdfTimestamp.format(timestamp));
    }
}