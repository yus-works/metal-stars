package com.example.metalstars.common

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException


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

var installRequested = false

var exceptionCallback: ((Exception) -> Unit)? = null
var beforeSessionResume: ((Session) -> Unit)? = null

fun tryCreateSession(activity: Activity, features: Set<Session.Feature> = setOf()): Session? {
    if (!hasCameraPermission(activity)) {
        requestCameraPermission(activity)
        return null
    }

    return try {
        // Request installation if necessary.
        when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)!!) {
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                installRequested = true
                // tryCreateSession will be called again, so we return null for now.
                return null
            }
            ArCoreApk.InstallStatus.INSTALLED -> {
                // Left empty; nothing needs to be done.
            }
        }

        // Create a session if Google Play Services for AR is installed and up to date.
        Session(activity, features)
    } catch (e: Exception) {
        exceptionCallback?.invoke(e)
        null
    }
}

fun onResume(activity: Activity, session: Session?): Session? {
    val newSession = session ?: tryCreateSession(activity) ?: return null
    return try {
        beforeSessionResume?.invoke(newSession)
        newSession.resume()
        newSession
    } catch (e: CameraNotAvailableException) {
        exceptionCallback?.invoke(e)
        null
    }
}

fun onPause(session: Session?) {
    session?.pause()
}

fun onDestroy(session: Session?) {
    session?.close()
}

fun onRequestPermissionsResult(activity: Activity) {
    if (!hasCameraPermission(activity)) {
        Toast.makeText(
            activity,
            "Camera permission is needed to run this application",
            Toast.LENGTH_LONG
        ).show()
        if (!shouldShowRequestPermissionRationale(activity)) {
            launchPermissionSettings(activity)
        }
        activity.finish()
    }
}