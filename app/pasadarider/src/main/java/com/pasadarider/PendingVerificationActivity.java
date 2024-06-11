package com.pasadarider;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class PendingVerificationActivity extends AppCompatActivity {

    FirebaseFirestore DB;
    FirebaseAuth AUTH;
    FirebaseUser USER;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        AUTH = FirebaseAuth.getInstance();
        USER = AUTH.getCurrentUser();
    }
    MaterialButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_verification);

        initializeFirebase();
        initializeViews();
        handleUserInteraction();
        listenForVerification();
    }

    private void listenForVerification() {
        DB.collection("users").document(AUTH.getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                        boolean isVerified = false;

                        if (documentSnapshot.getBoolean("isVerified") != null) {
                            isVerified = documentSnapshot.getBoolean("isVerified");
                        }

                        if (isVerified) {
                            startActivity(new Intent(PendingVerificationActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                });
    }

    private void handleUserInteraction() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AUTH.signOut();
                startActivity(new Intent(PendingVerificationActivity.this, SplashActivity.class));
                finish();
            }
        });
    }

    private void initializeViews() {
        btnLogout = findViewById(R.id.btnLogout);
    }
}