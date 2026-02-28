package com.listinglab.rowplus.telemetry

import android.content.Context
import android.util.Log

/**
 * Factory that creates the best available [TelemetrySource].
 *
 * Priority order:
 * 1. [SerialTelemetrySource] — live hardware UART
 * 2. [ReplayTelemetrySource] — bundled log replay (only if [allowReplayFallback] is true)
 *
 * The factory probes serial device availability synchronously (file existence
 * and read permission) so it can return quickly. The actual open/handshake
 * happens asynchronously inside [TelemetrySource.start].
 */
object TelemetrySourceFactory {

    private const val TAG = "TelemetryFactory"

    /**
     * Result of the factory probe.
     *
     * @param source The telemetry source to use, or null if nothing is available.
     * @param blockerNote Human-readable explanation if serial was unavailable.
     */
    data class ProbeResult(
        val source: TelemetrySource?,
        val blockerNote: String?,
    )

    /**
     * Create the best available telemetry source.
     *
     * @param context Android context (needed for asset access in replay mode)
     * @param engine The shared workout engine instance
     * @param listener Callback receiver for metrics and state changes
     * @param allowReplayFallback If true, fall back to replay when serial is unavailable.
     *                            If false, return null source with a blocker note.
     */
    fun create(
        context: Context,
        engine: WorkoutEngine,
        listener: TelemetryListener,
        allowReplayFallback: Boolean = true,
    ): ProbeResult {

        // Probe serial device availability
        val serialProbe = probeSerialAccess()

        if (serialProbe.accessible) {
            Log.i(TAG, "Serial device accessible at ${serialProbe.path} — using live source")
            return ProbeResult(
                source = SerialTelemetrySource(engine, listener),
                blockerNote = null,
            )
        }

        // Serial not available — explain why
        val note = serialProbe.reason
        Log.w(TAG, "Serial unavailable: $note")

        if (allowReplayFallback) {
            Log.i(TAG, "Falling back to replay source")
            return ProbeResult(
                source = ReplayTelemetrySource(context, engine, listener),
                blockerNote = note,
            )
        }

        return ProbeResult(
            source = null,
            blockerNote = note,
        )
    }

    private data class SerialProbe(
        val accessible: Boolean,
        val path: String?,
        val reason: String,
    )

    private fun probeSerialAccess(): SerialProbe {
        val tried = mutableListOf<String>()

        for (path in SerialTelemetrySource.DEVICE_PATHS) {
            val f = java.io.File(path)
            when {
                !f.exists() -> {
                    tried.add("$path (does not exist)")
                }
                !f.canRead() -> {
                    tried.add("$path (exists but permission denied)")
                }
                else -> {
                    return SerialProbe(
                        accessible = true,
                        path = path,
                        reason = "OK",
                    )
                }
            }
        }

        return SerialProbe(
            accessible = false,
            path = null,
            reason = "No accessible serial device. Tried: ${tried.joinToString("; ")}. " +
                "This is expected on production Hydrow builds — the device node is " +
                "typically owned by root:system (0660). A non-root app cannot open it. " +
                "To test live telemetry, run: adb shell chmod 666 /dev/ttyMT1",
        )
    }
}
