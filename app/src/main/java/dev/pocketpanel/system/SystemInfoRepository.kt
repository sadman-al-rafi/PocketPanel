package dev.pocketpanel.system

import android.content.Context
import dev.pocketpanel.elevated.ElevatedHardwareAccess
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
class SystemInfoRepository(
    context: Context,
    private val elevatedAccess: ElevatedHardwareAccess? = null,
) {

    private val appContext: Context = context.applicationContext
    private val cpuUsageSampler = CpuUsageSampler()
    private val thermalProvider = ThermalProvider(context)
    private val gpuInfoProvider = GpuInfoProvider()

    fun deviceInfoFlow(intervalMillis: Long): Flow<DeviceInfo> = flow {
        val safeInterval = intervalMillis.coerceAtLeast(MIN_INTERVAL_MILLIS)
        while (currentCoroutineContext().isActive) {
            val elevated = elevatedAccess?.snapshot()
            val hardware = HardwareSnapshot(
                // A real counter reading replaces the labelled estimate.
                cpuUsage = elevated?.cpuUsage ?: cpuUsageSampler.sample(),
                thermal = thermalProvider.snapshot(),
                gpu = gpuInfoProvider.info(),
                socTemperature = elevated?.socTemperature,
                gpuBusyPercent = elevated?.gpuBusyPercent,
                gpuClockKhz = elevated?.gpuClockKhz,
            )
            emit(DeviceInfo.load(appContext, hardware))
            delay(safeInterval)
        }
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val MIN_INTERVAL_MILLIS = 500L
    }
}
