package com.pasada;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasada.Utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationActivity extends AppCompatActivity {

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

    // global
    ConstraintLayout clLogo;

    // authentication
    ConstraintLayout clLogin;
    TextInputLayout tilLoginMobile;
    TextInputEditText etLoginEmail, etLoginPassword;
    MaterialButton btnGotoSignup, btnLogin;

    // registration
    ConstraintLayout clSignup;
    TextInputEditText etSignupFirstName, etSignupLastName, etMobile, etSignupEmail, etSignupPassword;
    MaterialButton btnGotoLogin, btnSignup;

    // verification
    ConstraintLayout clVerification;
    TextInputLayout tilVerificationCode;
    TextInputEditText etVerificationCode;
    MaterialButton btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        initializeFirebase();
        checkPreviousLoggedSession();
        initializeViews();
        handleUserInteractions();
    }

    private void checkPreviousLoggedSession() {
        if (USER != null) {
            startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        // global
        clLogo = findViewById(R.id.clLogo);

        // authentication
        clLogin = findViewById(R.id.clLogin);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnGotoSignup = findViewById(R.id.btnGotoSignup);
        btnLogin = findViewById(R.id.btnLogin);

        // registration
        clSignup = findViewById(R.id.clSignup);
        etSignupFirstName = findViewById(R.id.etSignupFirstName);
        etSignupLastName = findViewById(R.id.etSignupLastName);
        etMobile = findViewById(R.id.etMobile);
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        btnGotoLogin = findViewById(R.id.btnGotoLogin);
        btnSignup = findViewById(R.id.btnSignup);
    }

    private void handleUserInteractions() {
        btnSignup.setOnClickListener(view -> {
            Utils.hideKeyboard(this);
            validateRegistrationForm();
        });

        btnLogin.setOnClickListener(view -> {
            Utils.hideKeyboard(this);
            validateAuthenticationForm();
        });

        btnGotoSignup.setOnClickListener(view -> {
            clLogin.setVisibility(View.GONE);
            clSignup.setVisibility(View.VISIBLE);
        });

        btnGotoLogin.setOnClickListener(view -> {
            clLogin.setVisibility(View.VISIBLE);
            clSignup.setVisibility(View.GONE);
        });
    }

    private void validateAuthenticationForm() {
        if (etLoginEmail.getText().toString().isEmpty() ||
                etLoginPassword.getText().toString().isEmpty())
        {
            Toast.makeText(this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        String email = etLoginEmail.getText().toString();
        String password = etLoginPassword.getText().toString();

        AUTH.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DB.collection("users").document(AUTH.getUid())
                                .get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                if (task.getResult().getLong("userType") == 0) {
                                                    Toast.makeText(AuthenticationActivity.this, "Signed in as "+email, Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
                                                    finish();
                                                }
                                                else {
                                                    AUTH.signOut();
                                                    Utils.simpleDialog(AuthenticationActivity.this, "Incompatible Account", "Your account is incompatible with the Pasada Passenger app. If you are a rider, please use the Pasada Riders app instead.", "Back");
                                                    btnLogin.setEnabled(true);
                                                }
                                            }
                                        }
                                    });
                    }
                    else {
                        Utils.basicDialog(this, "Incorrect email or password.", "Try again");
                        btnLogin.setEnabled(true);
                    }
                });
    }

    private void validateRegistrationForm() {
        if (etSignupFirstName.getText().toString().isEmpty() ||
                etSignupLastName.getText().toString().isEmpty() ||
                etMobile.getText().toString().isEmpty() ||
                etSignupEmail.getText().toString().isEmpty() ||
                etSignupPassword.getText().toString().isEmpty())
        {
            Toast.makeText(this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = etSignupFirstName.getText().toString().toUpperCase();
        String lastName = etSignupLastName.getText().toString().toUpperCase();
        String mobile = etMobile.getText().toString().toUpperCase();
        String email = etSignupEmail.getText().toString();
        String password = etSignupPassword.getText().toString();

        if (password.length() < 6) {
            Utils.basicDialog(this, "Please use a password with at least 6 characters.", "Okay");
            return;
        }

        btnSignup.setEnabled(false);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("firstName", firstName);
        userInfo.put("lastName", lastName);
        userInfo.put("mobile", mobile);
        userInfo.put("email", email);
        userInfo.put("userType", 0);

        AUTH.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userInfo.put("uid", AUTH.getUid());

                        DB.collection("users").document(AUTH.getUid())
                                .set(userInfo)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
                                            Utils.Cache.setInt(AuthenticationActivity.this, "user_type", 0);
                                            finish();
                                            btnSignup.setEnabled(true);
                                        }
                                        else {
                                            Utils.simpleDialog(AuthenticationActivity.this, "Registration Failed", "" + task.getException().getLocalizedMessage(), "Try again");
                                            btnSignup.setEnabled(true);
                                        }
                                    }
                                });
                    }
                    else {
                        Utils.simpleDialog(this, "Registration Failed", "" + task.getException().getLocalizedMessage(), "Try again");
                        btnSignup.setEnabled(true);
                    }
                });
    }
}