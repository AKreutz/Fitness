package com.akreutz.fitness.ui.session

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.akreutz.fitness.MainActivity
import com.akreutz.fitness.R

private const val CHANNEL_ID = "rest_timer"
private const val NOTIFICATION_ID = 1001

/**
 * Alerts the user when a rest period between sets is over, by posting a notification when
 * permitted (its channel is [IMPORTANCE_HIGH][NotificationManager.IMPORTANCE_HIGH], so it vibrates
 * and heads-up by default). Created once per process; see [ensureChannel], called from
 * `FitnessApplication.onCreate`.
 */
class RestTimerAlerter(private val context: Context) {

    /** Posts a "rest is over" notification, if permitted. */
    fun onRestGoalReached() {
        showNotification()
    }

    private fun showNotification() {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        // Deliberately no FLAG_ACTIVITY_CLEAR_TOP/NEW_TASK: those can force MainActivity through
        // a fresh onCreate even when it's already running, which would reset its in-memory nav
        // state and drop the in-progress workout session back to Home. Since it's a single,
        // already-running activity, simply reordering it to the front (its own existing state
        // intact) is all a notification tap needs to do.
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("Rest is over")
            .setContentText("Time for your next set.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        /**
         * Creates the notification channel used by [onRestGoalReached], if it doesn't already
         * exist. Safe to call repeatedly. No-op below API 26, where channels don't exist and
         * notification importance is set per-notification instead.
         */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rest timer",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts you when a rest period between sets is over."
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
