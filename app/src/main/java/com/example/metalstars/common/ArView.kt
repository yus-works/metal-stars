package com.example.metalstars.common

import com.example.metalstars.MainActivity

import android.opengl.GLSurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.metalstars.R

class ArView(val activity: MainActivity) : DefaultLifecycleObserver {
    val root = View.inflate(activity, R.layout.activity_main, null)
    val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)
    val settingsButton =
        root.findViewById<ImageButton>(R.id.settings_button).apply {
            setOnClickListener { v ->
                PopupMenu(activity, v).apply {
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.option_1 -> launchDialog1()
                            R.id.option_2 -> launchDialog2()
                            else -> null
                        } != null
                    }
                    inflate(R.menu.settings_menu)
                    show()
                }
            }
        }

    val session
        get() = activity.arCoreSessionHelper.session

    override fun onResume(owner: LifecycleOwner) {
        surfaceView.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        surfaceView.onPause()
    }

    private fun launchDialog1() {
        AlertDialog.Builder(activity)
            .setTitle("This isn't so bad")
            .setPositiveButton("Ok bro") { _, _ ->
                val session = session ?: return@setPositiveButton
                activity.configureSession(session)
            }
            .show()
    }

    private fun launchDialog2() {
        // TODO: do stuff
        AlertDialog.Builder(activity)
            .setTitle("This isn't so bad 2")
            .setPositiveButton("Ok bro") { _, _ ->
                val session = session ?: return@setPositiveButton
                activity.configureSession(session)
            }
            .show()
    }
}