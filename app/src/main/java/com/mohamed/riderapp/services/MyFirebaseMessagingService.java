package com.mohamed.riderapp.services;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mohamed.riderapp.Common;
import com.mohamed.riderapp.utils.UserUtils;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        UserUtils.updateToken(this, s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> dataRec = remoteMessage.getData();
        if (dataRec!=null)
        {
            Common.showNotification(this, new Random().nextInt(), Common.NOTI_TITLE, Common.NOTI_CONTENT, null);
        }
    }
}
