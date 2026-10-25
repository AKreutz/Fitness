package com.akreutz.fitness.data.watch

import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Transport to the Fitness watch app (see `garmin/`) through the Connect IQ Mobile SDK. It only
 * moves messages: what they contain is up to whoever uses [send] and [messages]. All SDK callbacks
 * arrive on the main thread, so call [start], [stop] and [send] from there too.
 *
 * @param appId the watch app's id from its Connect IQ manifest; the connection does nothing when
 * it is blank.
 * @param useSimulator connect to the Connect IQ simulator over adb instead of a real watch.
 */
class GarminWatchConnection(
    private val context: Context,
    appId: String,
    private val useSimulator: Boolean,
) {
    private val isConfigured = appId.isNotBlank()
    private val watchApp = IQApp(appId)
    private val connectedDevices = mutableSetOf<IQDevice>()
    private var connectIQ: ConnectIQ? = null
    private var sdkReady = false

    private val _isWatchConnected = MutableStateFlow(false)

    /** Whether at least one watch is currently reachable. */
    val isWatchConnected: StateFlow<Boolean> = _isWatchConnected.asStateFlow()

    private val _messages = MutableSharedFlow<Map<String, Any?>>(extraBufferCapacity = MESSAGE_BUFFER)

    /** Every message the watch app sends. */
    val messages: SharedFlow<Map<String, Any?>> = _messages.asSharedFlow()

    fun start() {
        if (!isConfigured || connectIQ != null) return
        val sdk = ConnectIQ.getInstance(
            context,
            if (useSimulator) ConnectIQ.IQConnectType.TETHERED else ConnectIQ.IQConnectType.WIRELESS,
        )
        connectIQ = sdk
        sdk.initialize(
            context,
            /* autoUI = */ false,
            object : ConnectIQ.ConnectIQListener {
                override fun onSdkReady() {
                    sdkReady = true
                    registerDevices(sdk)
                }

                override fun onInitializeError(errorStatus: ConnectIQ.IQSdkErrorStatus) {
                    Log.w(TAG, "Connect IQ SDK unavailable: $errorStatus")
                    connectIQ = null
                }

                override fun onSdkShutDown() {
                    sdkReady = false
                }
            },
        )
    }

    fun stop() {
        val sdk = connectIQ ?: return
        connectIQ = null
        connectedDevices.clear()
        _isWatchConnected.value = false
        if (!sdkReady) return
        runCatching {
            sdk.unregisterAllForEvents()
            sdk.shutdown(context)
        }.onFailure { Log.w(TAG, "Connect IQ shutdown failed", it) }
        sdkReady = false
    }

    /** Sends [message] to every connected watch. Returns false when none is connected. */
    fun send(message: Map<String, Any>): Boolean = deliver(message).isNotEmpty()

    /**
     * Like [send], but waits until each watch has taken the message, failed to, or [timeoutMillis] have passed. Returns
     * whether every watch took it.
     */
    suspend fun sendAndAwait(message: Map<String, Any>, timeoutMillis: Long = SEND_AWAIT_MILLIS): Boolean {
        val deliveries = deliver(message)
        return withTimeoutOrNull(timeoutMillis) { deliveries.awaitAll().all { it } } ?: false
    }

    /** Starts sending [message] to every connected watch; each result completes with whether that watch took it. */
    private fun deliver(message: Map<String, Any>): List<CompletableDeferred<Boolean>> {
        val sdk = connectIQ ?: return emptyList()
        return connectedDevices.toList().map { device ->
            val done = CompletableDeferred<Boolean>()
            runCatching {
                Log.i(TAG, "Sending to watch: $message")
                sdk.sendMessage(device, watchApp, message) { _, _, status ->
                    if (status != ConnectIQ.IQMessageStatus.SUCCESS) {
                        Log.w(TAG, "Sending to watch failed: $status")
                    } else {
                        Log.i(TAG, "Sent to watch")
                    }
                    done.complete(status == ConnectIQ.IQMessageStatus.SUCCESS)
                }
            }.onFailure {
                Log.w(TAG, "Sending to watch failed", it)
                done.complete(false)
            }
            done
        }
    }

    private fun registerDevices(sdk: ConnectIQ) {
        val devices = runCatching { sdk.knownDevices }.getOrElse {
            Log.w(TAG, "Could not list Garmin devices", it)
            return
        }
        devices.orEmpty().forEach { device ->
            runCatching {
                sdk.registerForDeviceEvents(device) { d, status -> onDeviceStatus(d, status) }
                sdk.registerForAppEvents(device, watchApp) { _, _, messages, status ->
                    Log.i(TAG, "From watch: status $status, ${messages?.size} item(s)")
                    if (status == ConnectIQ.IQMessageStatus.SUCCESS) onMessages(messages)
                }
                onDeviceStatus(device, sdk.getDeviceStatus(device))
            }.onFailure { Log.w(TAG, "Could not register for ${device.friendlyName}", it) }
        }
    }

    private fun onDeviceStatus(device: IQDevice, status: IQDevice.IQDeviceStatus) {
        Log.i(TAG, "Device ${device.friendlyName}: $status")
        if (status == IQDevice.IQDeviceStatus.CONNECTED) {
            connectedDevices.add(device)
        } else {
            connectedDevices.remove(device)
        }
        _isWatchConnected.value = connectedDevices.isNotEmpty()
    }

    private fun onMessages(messages: List<Any>?) {
        messages.orEmpty().forEach { message ->
            if (message is Map<*, *>) {
                _messages.tryEmit(message.entries.associate { (key, value) -> key.toString() to value })
            } else {
                Log.w(TAG, "From watch: ignoring ${message?.javaClass?.name}: $message")
            }
        }
    }

    private companion object {
        const val TAG = "GarminWatch"
        const val MESSAGE_BUFFER = 16
        const val SEND_AWAIT_MILLIS = 4_000L
    }
}
