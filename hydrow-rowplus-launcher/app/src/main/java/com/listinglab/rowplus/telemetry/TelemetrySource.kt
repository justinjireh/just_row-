package com.listinglab.rowplus.telemetry

/**
 * Common interface for telemetry data sources.
 *
 * Implementations provide either live hardware data (serial UART) or
 * replayed data from captured log files. SessionActivity depends only
 * on this interface, never on a concrete adapter.
 */
interface TelemetrySource {

    /** The kind of source backing this instance. */
    val sourceType: SourceType

    /**
     * Start producing telemetry data.
     * Implementations must call [TelemetryListener.onConnectionStateChanged]
     * and begin feeding [TelemetryListener.onMetricsUpdated] at ~1 Hz.
     */
    fun start()

    /**
     * Stop producing telemetry data and release resources.
     * Safe to call multiple times.
     */
    fun stop()

    enum class SourceType {
        SERIAL_LIVE,
        REPLAY,
    }
}
