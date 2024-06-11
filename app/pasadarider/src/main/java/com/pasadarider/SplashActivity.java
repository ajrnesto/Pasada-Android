package com.pasadarider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    FirebaseFirestore DB;
    FirebaseAuth AUTH;
    FirebaseUser USER;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        AUTH = FirebaseAuth.getInstance();
        USER = AUTH.getCurrentUser();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initializeFirebase();
        checkPreviousLoggedSession();
    }

    private void checkPreviousLoggedSession() {
        if (USER != null) {
            DB.collection("users").document(USER.getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot docSnap = task.getResult();
                            long userType = docSnap.getLong("userType");
                            boolean isVerified = false;
                            if (docSnap.getBoolean("isVerified") != null) {
                                isVerified = docSnap.getBoolean("isVerified");
                            }

                            if (!docSnap.exists()) {
                                AUTH.signOut();

                                startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
                                finish();
                                return;
                            }

                            if (userType != 1) {
                                AUTH.signOut();

                                startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
                                finish();
                                return;
                            }

                            if (isVerified) {
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();
                            }
                            else {
                                startActivity(new Intent(SplashActivity.this, PendingVerificationActivity.class));
                                finish();
                            }
                        }
                    });

        }
        else {
            startActivity(new Intent(SplashActivity.this, AuthenticationActivity.class));
            finish();
        }
    }
}