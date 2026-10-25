package com.akreutz.fitness.data.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.akreutz.fitness.BuildConfig
import com.akreutz.fitness.FitnessApplication
import com.akreutz.fitness.MainActivity
import com.akreutz.fitness.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Keeps the watch connection alive while a workout session is open (see
 * [WatchSessionBridge.sessionActive]), so the session can be run from the watch with the phone's
 * screen off. Passes the session's state to the watch and the watch's commands to the session.
 * Stops itself once the session is over.
 */
class WatchSyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connection: GarminWatchConnection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as FitnessApplication
        startInForeground()

        val bridge = app.watchSessionBridge
        val connection = GarminWatchConnection(
            context = this,
            appId = BuildConfig.GARMIN_APP_ID,
            useSimulator = BuildConfig.GARMIN_USE_SIMULATOR,
        )
        this.connection = connection

        // The watch gets the current state whenever it changes, when a watch connects, and when
        // it asks for it (it does so when its app starts).
        val resend = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        scope.launch {
            combine(bridge.state, connection.isWatchConnected, resend.onStart { emit(Unit) }) { state, connected, _ ->
                state.takeIf { connected }
            }.filterNotNull().collect { connection.send(it) }
        }
        scope.launch { connection.messages.collect(bridge::onWatchMessage) }
        scope.launch {
            bridge.commands.filterIsInstance<WatchCommand.Sync>().collect { resend.tryEmit(Unit) }
        }
        connection.start()

        scope.launch {
            bridge.sessionActive.filter { !it }.first()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        connection?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val channelId = "watch_sync"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.watch_sync_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        // Like RestTimerAlerter's notification, this only brings the running activity to the front,
        // so a tap does not reset its in-memory navigation state.
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT },
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(getString(R.string.watch_sync_notification_title))
            .setContentText(getString(R.string.watch_sync_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0,
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1002

        /**
         * Does nothing unless a watch app id is configured. Must be called while the app is in the
         * foreground, or Android refuses the start.
         */
        fun start(context: Context) {
            if (BuildConfig.GARMIN_APP_ID.isBlank()) return
            ContextCompat.startForegroundService(context, Intent(context, WatchSyncService::class.java))
        }
    }
}
