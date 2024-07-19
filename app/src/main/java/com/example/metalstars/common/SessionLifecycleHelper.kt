package com.example.metalstars.common

import android.app.Activity
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

var CAMERA_PERMISSION_CODE = 0;
const val CAMERA_PERMISSION = Manifest.permission.CAMERA;

fun hasCameraPermission(activity: Activity): Boolean {
    val cameraPermission = ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION)
    val grantedPermission = PackageManager.PERMISSION_GRANTED

    return (cameraPermission == grantedPermission)
}