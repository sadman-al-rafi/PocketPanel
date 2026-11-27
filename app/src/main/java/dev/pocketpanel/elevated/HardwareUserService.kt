package dev.pocketpanel.elevated

import androidx.annotation.Keep
import java.io.File

/**
 * Shizuku user service. It runs in a separate process with shell (or root)
 * identity and only exposes raw file access.
 *
 * No Android framework APIs that require an application Context are used
 * here: a user service process is not a normal app process.
 */
@Keep
class HardwareUserService : IHardwareService.Stub() {

    override fun readTextFile(path: String?): String? = try {
        if (path == null) null else File(path).readText()
    } catch (_: Exception) {
        null
    }

    override fun listNames(path: String?): Array<String>? = try {
        if (path == null) null else File(path).list()
    } catch (_: Exception) {
        null
    }

    override fun destroy() {
        System.exit(0)
    }
}
