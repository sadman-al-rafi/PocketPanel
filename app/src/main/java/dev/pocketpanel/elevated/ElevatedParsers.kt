package dev.pocketpanel.elevated

/** Parsing of raw sysfs/proc text returned by the elevated user service. */
object ElevatedParsers {

    /** How a vendor GPU busy node encodes its value. */
    enum class BusyKind { PERCENT, KGSL_RATIO }

    /** Parses a driver busy value, rejecting impossible readings. */
    fun busyPercent(text: String?, kind: BusyKind): Int? {
        if (text.isNullOrEmpty()) return null
        return when (kind) {
            BusyKind.PERCENT -> {
                val value = text.toIntOrNull() ?: return null
                if (value in 0..100) value else null
            }

            BusyKind.KGSL_RATIO -> {
                val parts = text.split(WHITESPACE)
                if (parts.size != 2) return null
                val busy = parts[0].toLongOrNull() ?: return null
                val total = parts[1].toLongOrNull() ?: return null
                if (total <= 0L || busy < 0L || busy > total) return null
                (busy * 100L / total).toInt().coerceIn(0, 100)
            }
        }
    }

    /** Converts a thermal zone value in millidegrees Celsius. */
    fun thermalCelsius(text: String?): Float? {
        val millidegrees = text?.trim()?.toLongOrNull() ?: return null
        val celsius = millidegrees / 1000f
        return if (celsius in 0f..150f) celsius else null
    }

    /** Accepts only a positive clock value in kHz. */
    fun gpuClockKhz(text: String?): Long? =
        text?.trim()?.toLongOrNull()?.takeIf { it > 0L }

    private val WHITESPACE = Regex("\\s+")
}
