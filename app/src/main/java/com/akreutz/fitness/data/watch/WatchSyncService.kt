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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
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

        // The watch gets the current state whenever it changes, when a watch connects, and when it is asked to be sent
        // again (the watch asks when its app starts, and the bridge when a press came from an out-of-date screen).
        // States go out one at a time, and while one is on its way only the latest of the newer ones is kept, so the
        // watch is never sent a stale state after a fresh one. One that fails is sent again a few times.
        val resend = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        scope.launch {
            var failures = 0
            combine(bridge.state, connection.isWatchConnected, resend.onStart { emit(Unit) }) { state, connected, _ ->
                state.takeIf { connected }
            }.filterNotNull().conflate().collect { state ->
                if (connection.sendAndAwait(state)) {
                    failures = 0
                } else if (++failures <= MAX_STATE_RETRIES) {
                    delay(STATE_RETRY_MILLIS)
                    resend.tryEmit(Unit)
                }
            }
        }
        scope.launch { connection.messages.collect(bridge::onWatchMessage) }
        scope.launch { bridge.resendRequests.collect { resend.tryEmit(Unit) } }

        // The session waits for the watch's heart rate readings only if there is a watch to send them.
        scope.launch { connection.isWatchConnected.collect { bridge.watchConnected.value = it } }
        connection.start()

        scope.launch {
            bridge.sessionActive.filter { !it }.first()
            if (bridge.endedFinished) {
                // A finished workout stays on the watch so its summary can be read. Its last state, which says it is
                // saved, may not have got through yet, so it is sent once more before the connection closes.
                bridge.state.value?.let { connection.sendAndAwait(it) }
            } else {
                // Leaving or discarding a workout signs the watch off, so it does not keep showing one that is gone.
                connection.sendAndAwait(WatchProtocol.encodeSignOff())
            }
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
        // This only brings the running activity to the front,
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
        private const val MAX_STATE_RETRIES = 3
        private const val STATE_RETRY_MILLIS = 1_000L

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
