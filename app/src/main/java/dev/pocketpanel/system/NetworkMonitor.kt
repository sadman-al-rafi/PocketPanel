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

/**
 * Reports the active network transport and device-wide throughput.
 *
 * Throughput is derived from the difference between two
 * [TrafficStats.getTotalRxBytes] / [TrafficStats.getTotalTxBytes]
 * samples. When a counter is unsupported, or a sample would require a
 * negative delta, the rate for that sample is null. The first emission
 * carries the transport only, because a rate needs two samples.
 */
class NetworkMonitor(context: Context) {

    private val appContext: Context = context.applicationContext

    fun networkInfoFlow(intervalMillis: Long): Flow<NetworkInfo> = flow {
        val safeInterval = intervalMillis.coerceAtLeast(MIN_INTERVAL_MILLIS)
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        var previousElapsed = SystemClock.elapsedRealtime()

        emit(NetworkInfo(transport = activeTransport()))

        while (currentCoroutineContext().isActive) {
            delay(safeInterval)
            val elapsed = SystemClock.elapsedRealtime()
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val elapsedMillis = elapsed - previousElapsed

            emit(
                NetworkInfo(
                    transport = activeTransport(),
                    receiveBytesPerSecond = bytesPerSecond(previousRx, rx, elapsedMillis),
                    transmitBytesPerSecond = bytesPerSecond(previousTx, tx, elapsedMillis),
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
    }
}
