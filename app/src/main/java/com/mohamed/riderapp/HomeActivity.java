package com.mohamed.riderapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mohamed.riderapp.databinding.ActivityHomeBinding;
import com.mohamed.riderapp.utils.UserUtils;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 7172;
    private ActivityHomeBinding binding;

    private AlertDialog waitingDialog;

    private CircleImageView avatarIv;

    private Uri imageUri;

    private StorageReference storageReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);


        binding.menuImage.setOnClickListener(view -> binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navHost.setItemIconTintList(null);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(binding.navHost, navController);

        init();

    }

    private void init()
    {
        storageReference = FirebaseStorage.getInstance().getReference();
        waitingDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("waiting...")
                .create();

        binding.navHost.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_sign_out)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Sign out")
                        .setMessage("Do You want to sign out ?")
                        .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .setPositiveButton("SIGN OUT", (dialogInterface, i) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(HomeActivity.this, SplashScreenActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialogInterface -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                });

                dialog.show();
            }
            return true;
        });

        // set data for user
        View headerView = binding.navHost.getHeaderView(0);
        TextView txtName = headerView.findViewById(R.id.txtName);
        TextView txtPhone = headerView.findViewById(R.id.txtPhone);
        avatarIv = headerView.findViewById(R.id.avatarIv);

        txtName.setText(Common.buildWelcomeMessage());
        txtPhone.setText(Common.currentRider != null ? Common.currentRider.getPhoneNumber() : "");

        avatarIv.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        if (Common.currentRider != null && Common.currentRider.getAvatar() != null && !TextUtils.isEmpty(Common.currentRider.getAvatar())) {
            Glide.with(this)
                    .load(Common.currentRider.getAvatar())
                    .into(avatarIv);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK)
        {
            if (data!=null && data.getData()!=null)
            {
                imageUri = data.getData();
                avatarIv.setImageURI(imageUri);
                showDialogUpload();
            }
        }
    }

    private void showDialogUpload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setTitle("Change Avatar")
                .setMessage("Do You want to change avatar ?")
                .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .setPositiveButton("UPLOAD", (dialogInterface, i) -> {
                    if (imageUri != null) {
                        waitingDialog.setMessage("Uploading...");
                        waitingDialog.show();

                        String uniqueName = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        StorageReference avatarRef = storageReference.child("avatars/" + uniqueName);

                        avatarRef.putFile(imageUri)
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        waitingDialog.dismiss();
                                        Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                            waitingDialog.dismiss();
                                            Map<String, Object> updateData = new HashMap<>();
                                            updateData.put("avatar", uri.toString());
                                            UserUtils.updateUser(HomeActivity.this, updateData);
                                        });
                                    }
                                })
                                .addOnProgressListener(snapshot -> {
                                    double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                    waitingDialog.setMessage(
                                            new StringBuilder("Uploading : ").append(progress).append("%")
                                    );
                                });
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        });

        dialog.show();
    }

}