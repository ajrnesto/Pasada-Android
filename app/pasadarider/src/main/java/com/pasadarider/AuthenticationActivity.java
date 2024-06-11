package com.pasadarider;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.makeramen.roundedimageview.RoundedImageView;
import com.pasadarider.Utils.Utils;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuthenticationActivity extends AppCompatActivity {

    FirebaseFirestore DB;
    FirebaseDatabase RTDB;
    FirebaseAuth AUTH;
    FirebaseStorage STORAGE;
    FirebaseUser USER;

    private void initializeFirebase() {
        DB = FirebaseFirestore.getInstance();
        RTDB = FirebaseDatabase.getInstance();
        AUTH = FirebaseAuth.getInstance();
        STORAGE = FirebaseStorage.getInstance();
        USER = AUTH.getCurrentUser();
    }

    ActivityResultLauncher<Intent> activityResultLauncher;
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

    RoundedImageView imgId;
    MaterialButton btnUploadId;

    RadioGroup rgVehicleType;

    Uri uriSelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        initializeFirebase();
        checkPreviousLoggedSession();
        initializeViews();
        initializeActivityResultLauncher();
        handleUserInteractions();
    }

    private void checkPreviousLoggedSession() {
        if (USER != null) {
            DB.collection("users").document(USER.getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot docSnap = task.getResult();

                            if (!docSnap.exists()) {
                                AUTH.signOut();
                                return;
                            }

                            long userType = docSnap.getLong("userType");
                            if (userType != 1) {
                                AUTH.signOut();
                                return;
                            }

                            startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
                            finish();
                        }
                    });

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
        rgVehicleType = findViewById(R.id.rgVehicleType);
        imgId = findViewById(R.id.imgId);
        btnUploadId = findViewById(R.id.btnUploadId);
    }

    private void handleUserInteractions() {
        btnUploadId.setOnClickListener(view -> {
            selectImageFromDevice();
        });

        btnLogin.setOnClickListener(view -> {
            Utils.hideKeyboard(this);
            validateAuthenticationForm();
        });

        btnSignup.setOnClickListener(view -> {
            Utils.hideKeyboard(this);
            validateRegistrationForm();
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

        String emailInput = etLoginEmail.getText().toString();
        String passwordInput = etLoginPassword.getText().toString();

        AUTH.signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DB.collection("users").document(AUTH.getUid())
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            if (task.getResult().getLong("userType") == 1) {
                                                Toast.makeText(AuthenticationActivity.this, "Signed in as "+emailInput, Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(AuthenticationActivity.this, SplashActivity.class));
                                                finish();
                                            }
                                            else {
                                                AUTH.signOut();
                                                Utils.simpleDialog(AuthenticationActivity.this, "Unauthorized account", "Your account is not authorized to log in to the Pasada Riders app. If you are a passenger, please use the Pasada Passengers app instead.", "Back");
                                                btnLogin.setEnabled(true);
                                            }
                                        }
                                    }
                                });
                    }
                    else {
                        Utils.simpleDialog(this, "Login Failed", ""+task.getException().getMessage(), "Try again");
                        btnLogin.setEnabled(true);
                    }
                });
    }

    private void validateRegistrationForm() {
        if (uriSelected == null) {
            Utils.simpleDialog(this, "ID Required", "Please upload a government-issued identification card.", "Okay");
            btnSignup.setEnabled(true);
            return;
        }

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
        String emailInput = etSignupEmail.getText().toString();
        String passwordInput = etSignupPassword.getText().toString();

        if (passwordInput.length() < 6) {
            Utils.basicDialog(this, "Please use a password with at least 6 characters.", "Okay");
            return;
        }

        btnSignup.setEnabled(false);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("firstName", firstName);
        userInfo.put("lastName", lastName);
        userInfo.put("mobile", mobile);
        userInfo.put("email", emailInput);
        userInfo.put("userType", 1);

        AUTH.createUserWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userInfo.put("uid", AUTH.getUid());
                        userInfo.put("idFileName", AUTH.getUid());

                        StorageReference bannerReference = STORAGE.getReference().child("images/"+AUTH.getUid());
                        bannerReference.putFile(uriSelected).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (!task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "Failed to upload ID. Please try again.", Toast.LENGTH_SHORT).show();
                                    clLogin.setVisibility(View.GONE);
                                    clSignup.setVisibility(View.VISIBLE);
                                }
                                else {
                                    DB.collection("users").document(AUTH.getUid())
                                            .set(userInfo)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        startActivity(new Intent(AuthenticationActivity.this, SplashActivity.class));
                                                        Utils.Cache.setInt(AuthenticationActivity.this, "user_type", 1);
                                                        finish();
                                                        btnSignup.setEnabled(true);
                                                    }
                                                    else {
                                                        Utils.simpleDialog(AuthenticationActivity.this, "Registration Error", task.getException().getMessage(), "Try again");
                                                        btnSignup.setEnabled(true);
                                                    }
                                                }
                                            });
                                }
                            }
                        });
                    }
                    else {
                        Utils.basicDialog(this, "Something went wrong when trying to create your account.", "Try again");
                        btnSignup.setEnabled(true);
                    }
                });
    }

    private void selectImageFromDevice() {
        Intent iImageSelect = new Intent();
        iImageSelect.setType("image/*");
        iImageSelect.setAction(Intent.ACTION_GET_CONTENT);

        activityResultLauncher.launch(Intent.createChooser(iImageSelect, "Select ID"));
    }

    private void initializeActivityResultLauncher() {
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uriRetrieved = Objects.requireNonNull(data).getData();
                        uriSelected = uriRetrieved;

                        // display selected image
                        Picasso.get().load(uriRetrieved).resize(800,0).centerCrop().into(imgId);
                    }
                }
        );
    }
}