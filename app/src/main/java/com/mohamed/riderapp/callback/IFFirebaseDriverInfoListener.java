package com.mohamed.riderapp.callback;

import com.mohamed.riderapp.model.DriverGeoModel;

public interface IFFirebaseDriverInfoListener {
    void onDriverInfoLoadSuccess(DriverGeoModel model);
}
