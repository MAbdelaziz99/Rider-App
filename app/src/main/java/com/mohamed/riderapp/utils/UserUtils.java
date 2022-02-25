package com.mohamed.riderapp.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.mohamed.riderapp.Common;
import com.mohamed.riderapp.HomeActivity;
import com.mohamed.riderapp.model.TokenModel;

import java.util.Map;

public class UserUtils {
    public static void updateUser(Context context, Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RIDER_INFO_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(updateData)
                .addOnSuccessListener(unused -> Toast.makeText(context, "Data is Updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Log.d("TAG", "onFailure: " + e.getMessage()));
    }

    public static void updateToken(Context context, String token) {
        TokenModel model = new TokenModel();
        model.setToken(token);
        FirebaseDatabase.getInstance().getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(model);
    }
}
