package com.mohamed.riderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mohamed.riderapp.databinding.ActivitySplashScreenBinding;
import com.mohamed.riderapp.model.RiderModel;
import com.mohamed.riderapp.utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class SplashScreenActivity extends AppCompatActivity {

    private ActivitySplashScreenBinding binding;

    private final static int LOGIN_REQUEST_CODE = 7171;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener listener;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (auth != null && listener != null) {
            auth.removeAuthStateListener(listener);
        }
    }

    private void delaySplashScreen() {
        binding.progressBar.setVisibility(View.VISIBLE);
        Completable.timer(3, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(() ->
                        auth.addAuthStateListener(listener)
                );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash_screen);

        init();
    }

    private void init() {
        ButterKnife.bind(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(Common.RIDER_INFO_REFERENCE);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        auth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth ->
        {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user!=null)
            {
                FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(s -> {
                            UserUtils.updateToken(SplashScreenActivity.this, s);
                        });
                checkUserFromFirebase();
            }
            else
            {
                showLoginLayout();
            }
        };
    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.signInWithPhoneBtn)
                .setGoogleButtonId(R.id.signInWithGoogleBtn)
                .build();

        startActivityForResult(AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAuthMethodPickerLayout(authMethodPickerLayout)
        .setIsSmartLockEnabled(false)
        .setTheme(R.style.LoginTheme)
        .setAvailableProviders(providers)
        .build(), LOGIN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE )
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK)
            {
                FirebaseUser user  = auth.getCurrentUser();
            }
            else
            {
                Toast.makeText(this, "" + response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void checkUserFromFirebase() {
        databaseReference.child(auth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {
                            binding.progressBar.setVisibility(View.GONE);
                            // go to home screen
                            RiderModel riderModel = snapshot.getValue(RiderModel.class);
                            goToHomeActivity(riderModel);
                        }else
                        {
                            // go to register layout
                            showRegisterLayout();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(SplashScreenActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_register, null);

        TextInputEditText firstNameEt = view.findViewById(R.id.firstNameEt);
        TextInputEditText lastNameEt = view.findViewById(R.id.lastNameEt);
        TextInputEditText phoneNumberEt = view.findViewById(R.id.phoneEt);

        Button continueBtn = view.findViewById(R.id.continueBtn);

        // set data
        if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            phoneNumberEt.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        continueBtn.setOnClickListener(view1 -> {
            if (TextUtils.isEmpty(firstNameEt.getText().toString()))
            {
                Toast.makeText(SplashScreenActivity.this, "Please Enter First Name", Toast.LENGTH_SHORT).show();
                return;
            }
            else  if (TextUtils.isEmpty(lastNameEt.getText().toString()))
            {
                Toast.makeText(SplashScreenActivity.this, "Please Enter last Name", Toast.LENGTH_SHORT).show();
                return;
            }
            else  if (TextUtils.isEmpty(phoneNumberEt.getText().toString()))
            {
                Toast.makeText(SplashScreenActivity.this, "Please Enter Phone Number", Toast.LENGTH_SHORT).show();
                return;
            }
            else
            {
                RiderModel model = new RiderModel();
                model.setFirstName(firstNameEt.getText().toString());
                model.setLastName(lastNameEt.getText().toString());
                model.setPhoneNumber(phoneNumberEt.getText().toString());

                databaseReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(SplashScreenActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                            goToHomeActivity(model);
                        });
            }

        });
    }


    private void goToHomeActivity(RiderModel model) {
        Common.currentRider = model;
        startActivity(new Intent(SplashScreenActivity.this, HomeActivity.class));
        finish();
    }
}