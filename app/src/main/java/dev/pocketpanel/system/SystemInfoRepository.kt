package dev.pocketpanel.system

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Produces [DeviceInfo] snapshots on a configurable interval.
 *
 * Sampling runs on [Dispatchers.Default] and only exists while the flow is
 * collected, so polling stops when the screen is not observing it.
 */
class SystemInfoRepository(context: Context) {

    private val appContext: Context = context.applicationContext
    private val cpuUsageSampler = CpuUsageSampler()

    fun deviceInfoFlow(intervalMillis: Long): Flow<DeviceInfo> = flow {
        val safeInterval = intervalMillis.coerceAtLeast(MIN_INTERVAL_MILLIS)
        while (currentCoroutineContext().isActive) {
            emit(DeviceInfo.load(appContext, cpuUsageSampler.sample()))
            delay(safeInterval)
        }
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val MIN_INTERVAL_MILLIS = 500L
    }
}
