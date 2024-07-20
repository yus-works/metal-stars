package com.example.metalstars.common

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

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

fun setFullScreenOnWindowFocusChanged(activity: Activity, hasFocus: Boolean) {
    if (!hasFocus) {
        return
    }

    activity.window?.let { window ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}