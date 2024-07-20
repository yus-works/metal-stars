package com.example.metalstars.common

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.provider.Settings
import android.net.Uri

var CAMERA_PERMISSION_CODE = 0;
const val CAMERA_PERMISSION = Manifest.permission.CAMERA;

fun hasCameraPermission(activity: Activity): Boolean {
    val cameraPermission = ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION)
    val grantedPermission = PackageManager.PERMISSION_GRANTED

    return (cameraPermission == grantedPermission)
}

fun requestCameraPermission(activity: Activity?) {
    if (activity != null) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE
        )
    } else {
        println("Failed to request camera permission, activity is null")
    }
}

// idk
fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION);
}

fun launchPermissionSettings(activity: Activity) {
    var intent = Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.fromParts("package", activity.packageName, null));
    activity.startActivity(intent);
}