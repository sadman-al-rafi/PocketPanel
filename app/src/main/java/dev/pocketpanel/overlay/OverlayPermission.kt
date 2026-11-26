package dev.pocketpanel.overlay

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Wraps the `SYSTEM_ALERT_WINDOW` special app access.
 *
 * The permission is granted outside the normal runtime-permission dialog, so
 * it can only be checked and requested through [Settings].
 */
object OverlayPermission {

    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Opens the system screen for the overlay permission.
     *
     * Returns false when no matching settings screen exists, so the caller
     * can explain the limitation instead of crashing.
     */
    fun openSettings(context: Context): Boolean {
        val packageIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        if (tryStart(context, packageIntent)) return true

        val genericIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        return tryStart(context, genericIntent)
    }

    private fun tryStart(context: Context, intent: Intent): Boolean {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
