package dev.pocketpanel.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Reports the active network transport, device-wide throughput and
 * ping latency.
 *
 * Throughput is derived from the difference between two
 * [TrafficStats.getTotalRxBytes] / [TrafficStats.getTotalTxBytes]
 * samples. When a counter is unsupported, or a sample would require a
 * negative delta, the rate for that sample is null. The first emission
 * carries the transport only, because a rate needs two samples.
 *
 * Ping is measured with the platform command and cached for
 * [PING_CACHE_MILLIS], so a slow or blocked probe never runs on every
 * tick. A failed probe yields null, shown as "Not available".
 */
class NetworkMonitor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val pingProbe = PingProbe()

    fun networkInfoFlow(intervalMillis: Long): Flow<NetworkInfo> = flow {
        val safeInterval = intervalMillis.coerceAtLeast(MIN_INTERVAL_MILLIS)
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        var previousElapsed = SystemClock.elapsedRealtime()

        var pingMeasured = false
        var lastPingElapsed = 0L
        var lastPingMillis: Int? = null

        emit(NetworkInfo(transport = activeTransport()))

        while (currentCoroutineContext().isActive) {
            delay(safeInterval)
            val elapsed = SystemClock.elapsedRealtime()
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val elapsedMillis = elapsed - previousElapsed

            if (!pingMeasured || elapsed - lastPingElapsed >= PING_CACHE_MILLIS) {
                lastPingMillis = withContext(Dispatchers.IO) { pingProbe.pingOnce() }
                lastPingElapsed = elapsed
                pingMeasured = true
            }

            emit(
                NetworkInfo(
                    transport = activeTransport(),
                    receiveBytesPerSecond = bytesPerSecond(previousRx, rx, elapsedMillis),
                    transmitBytesPerSecond = bytesPerSecond(previousTx, tx, elapsedMillis),
                    pingMillis = lastPingMillis,
                ),
            )

            previousRx = rx
            previousTx = tx
            previousElapsed = elapsed
        }
    }.flowOn(Dispatchers.Default)

    private fun activeTransport(): NetworkTransport? {
        val connectivityManager =
            appContext.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = connectivityManager.activeNetwork ?: return NetworkTransport.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkTransport.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                NetworkTransport.WIFI

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkTransport.CELLULAR

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                NetworkTransport.ETHERNET

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
                NetworkTransport.VPN

            else -> NetworkTransport.OTHER
        }
    }

    private companion object {
        const val MIN_INTERVAL_MILLIS = 500L
        const val PING_CACHE_MILLIS = 5_000L
    }
}
