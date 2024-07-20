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
import androidx.lifecycle.DefaultLifecycleObserver
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

class SessionLifecycleHelper(
    val activity: Activity,
    val features: Set<Session.Feature> = setOf()
) : DefaultLifecycleObserver {
    var installRequested = false
    var session: Session? = null
        private set

    var exceptionCallback: ((Exception) -> Unit)? = null
    var beforeSessionResume: ((Session) -> Unit)? = null

    private fun tryCreateSession(): Session? {
        if (!hasCameraPermission(activity)) {
            requestCameraPermission(activity)
            return null
        }

        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    // tryCreateSession will be called again, so we return null for now.
                    return null
                }
                ArCoreApk.InstallStatus.INSTALLED -> {}
            }

            Session(activity, features)
        } catch (e: Exception) {
            exceptionCallback?.invoke(e)
            null
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        val session = this.session ?: tryCreateSession() ?: return
        try {
            beforeSessionResume?.invoke(session)
            session.resume()
            this.session = session
        } catch (e: CameraNotAvailableException) {
            exceptionCallback?.invoke(e)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        session?.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        session?.close()
        session = null
    }

    fun onRequestPermissionsResult() {
        if (!hasCameraPermission(activity)) {
            return
        }

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