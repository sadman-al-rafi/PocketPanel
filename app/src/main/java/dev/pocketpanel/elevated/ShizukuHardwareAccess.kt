package dev.pocketpanel.elevated

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import dev.pocketpanel.system.CpuUsage
import dev.pocketpanel.system.CpuUsageMath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * Optional elevated readings through a Shizuku user service.
 *
 * The service runs with shell or root identity and only returns raw file
 * contents. Parsing, validation and caching stay in the app process. If
 * Shizuku is absent, denied or dies, every call returns null and the
 * standard providers continue to work.
 */
class ShizukuHardwareAccess(context: Context) : ElevatedHardwareAccess {

    override val status = ShizukuCapability.status

    private val appContext = context.applicationContext
    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext, HardwareUserService::class.java),
    ).tag(USER_SERVICE_TAG).version(1).daemon(false)

    private var service: IHardwareService? = null
    private var connected = CompletableDeferred<IHardwareService?>()
    private var bindAttempted = false
    private var previousCpu: Pair<Long, Long>? = null
    private var socZone: Pair<String, String>? = null
    private var gpuBusyPath: Pair<String, BusyKind>? = null
    private var gpuClockPath: String? = null
    private var discoveryDone = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder?.let { IHardwareService.Stub.asInterface(it) }
            connected.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            connected = CompletableDeferred()
            bindAttempted = false
        }
    }

    override fun initialize() {
        ShizukuCapability.initialize()
    }

    override fun requestPermission() {
        ShizukuCapability.requestPermission()
    }

    override fun uid(): Int? = ShizukuCapability.uid()

    override suspend fun snapshot(): ElevatedSnapshot? {
        if (status.value != ShizukuStatus.Granted) return null
        val remote = getService() ?: return null
        return withContext(Dispatchers.IO) {
            discover(remote)
            ElevatedSnapshot(
                cpuUsage = readCpu(remote),
                socTemperature = readSocTemperature(remote),
                gpuBusyPercent = readGpuBusy(remote),
                gpuClockKhz = readGpuClock(remote),
            )
        }
    }

    override fun close() {
        runCatching { Shizuku.unbindUserService(serviceArgs, connection, true) }
        service = null
        connected = CompletableDeferred()
        bindAttempted = false
    }

    private suspend fun getService(): IHardwareService? {
        service?.let { return it }
        if (status.value != ShizukuStatus.Granted) return null
        if (!bindAttempted) {
            bindAttempted = true
            val bound = runCatching {
                Shizuku.bindUserService(serviceArgs, connection)
            }.isSuccess
            if (!bound) {
                bindAttempted = false
                return null
            }
        }
        return withTimeoutOrNull(BIND_TIMEOUT_MS) { connected.await() }
    }

    private fun discover(remote: IHardwareService) {
        if (discoveryDone) return
        discoveryDone = true
        socZone = findSocZone(remote)
        gpuBusyPath = GPU_BUSY_CANDIDATES.firstNotNullOfOrNull { (path, kind) ->
            val value = parseBusy(remote.readTextFile(path)?.trim(), kind)
            if (value == null) null else path to kind
        }
        gpuClockPath = GPU_CLOCK_CANDIDATES.firstOrNull { path ->
            remote.readTextFile(path)?.trim()?.toLongOrNull()?.let { it > 0 } == true
        }
    }

    private fun findSocZone(remote: IHardwareService): Pair<String, String>? {
        val names = remote.listNames(THERMAL_ROOT) ?: return null
        val preferred = setOf("cpu", "soc", "package", "tsens")
        val zones = names.filter { it.startsWith("thermal_zone") }
            .mapNotNull { name ->
                val type = remote.readTextFile("$THERMAL_ROOT/$name/type")?.trim()?.ifEmpty { null }
                type?.let { name to it }
            }
            .sortedBy { (_, type) ->
                if (preferred.any { type.contains(it, ignoreCase = true) }) 0 else 1
            }

        for ((name, type) in zones) {
            val temperature = remote.readTextFile("$THERMAL_ROOT/$name/temp")
                ?.trim()
                ?.toLongOrNull()
                ?: continue
            val celsius = temperature / 1000f
            if (celsius in 0f..150f) return "$THERMAL_ROOT/$name/temp" to type
        }
        return null
    }

    private fun readCpu(remote: IHardwareService): CpuUsage? {
        val text = remote.readTextFile(PROC_STAT) ?: return null
        val counters = text.lineSequence()
            .firstOrNull { it.startsWith("cpu ") }
            ?.let(CpuUsageMath::parseProcStatLine)
            ?: return null
        val previous = previousCpu
        previousCpu = counters
        if (previous == null) return null
        val percent = CpuUsageMath.fromCounterDeltas(
            totalDelta = counters.first - previous.first,
            idleDelta = counters.second - previous.second,
        ) ?: return null
        return CpuUsage(percent, estimated = false)
    }

    private fun readSocTemperature(remote: IHardwareService): SocTemperature? {
        val (path, type) = socZone ?: return null
        val raw = remote.readTextFile(path)?.trim()?.toLongOrNull() ?: return null
        val celsius = raw / 1000f
        return if (celsius in 0f..150f) SocTemperature(celsius, type) else null
    }

    private fun readGpuBusy(remote: IHardwareService): Int? {
        val (path, kind) = gpuBusyPath ?: return null
        return parseBusy(remote.readTextFile(path)?.trim(), kind)
    }

    private fun readGpuClock(remote: IHardwareService): Long? {
        val path = gpuClockPath ?: return null
        return remote.readTextFile(path)?.trim()?.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun parseBusy(text: String?, kind: BusyKind): Int? {
        if (text.isNullOrEmpty()) return null
        return when (kind) {
            BusyKind.PERCENT -> text.toIntOrNull()?.coerceIn(0, 100)
            BusyKind.KGSL_RATIO -> {
                val parts = text.split(WHITESPACE)
                if (parts.size != 2) {
                    null
                } else {
                    val busy = parts[0].toLongOrNull() ?: return null
                    val total = parts[1].toLongOrNull() ?: return null
                    if (total <= 0L || busy < 0L || busy > total) {
                        null
                    } else {
                        (busy * 100L / total).toInt().coerceIn(0, 100)
                    }
                }
            }
        }
    }

    private enum class BusyKind { PERCENT, KGSL_RATIO }

    private companion object {
        const val USER_SERVICE_TAG = "pocketpanel-hardware"
        const val BIND_TIMEOUT_MS = 3_000L
        const val PROC_STAT = "/proc/stat"
        const val THERMAL_ROOT = "/sys/class/thermal"
        val WHITESPACE = Regex("\\s+")
        val GPU_BUSY_CANDIDATES = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage" to BusyKind.PERCENT,
            "/sys/class/kgsl/kgsl-3d0/gpubusy" to BusyKind.KGSL_RATIO,
            "/sys/class/misc/mali0/device/utilisation" to BusyKind.PERCENT,
            "/sys/kernel/ged/hal/gpu_utilization" to BusyKind.PERCENT,
            "/sys/module/ged/parameters/gpu_loading" to BusyKind.PERCENT,
            "/sys/class/drm/card0/device/gpu_busy_percent" to BusyKind.PERCENT,
        )
        val GPU_CLOCK_CANDIDATES = listOf(
            "/sys/kernel/gpu/gpu_clock",
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
            "/sys/class/misc/mali0/device/devfreq/cur_freq",
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
        )
    }
}
