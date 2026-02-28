package com.listinglab.rowplus.telemetry

/**
 * High-frequency interval data (~100+ samples/sec from rower MCU).
 * Each sample represents one flywheel measurement interval.
 */
data class Di2Packet(
    val timestamp: Long,
    val distance: Double,        // delta distance (meters) since last sample
    val handleForce: Double,     // force on handle (N), 0 during recovery
    val handlePosition: Double,  // handle position / travel
    val power: Double,           // accumulated metric from MCU
    val sequence: Int,           // monotonic sample counter
)

/**
 * Per-stroke summary emitted once per complete stroke cycle.
 */
data class Ds2Packet(
    val timestamp: Long,
    val averagePower: Double,         // average watts for this stroke
    val distance: Double,             // total accumulated distance
    val endHandlePosition: Double,
    val startHandlePosition: Double,
    val endOfDriveSequence: Int,      // Di2 seq at end of drive
    val endOfRecoverySequence: Int,   // Di2 seq at end of recovery
    val sequence: Int,                // stroke counter
)

/**
 * Live workout metrics computed from the packet stream.
 * Updated at display refresh rate (~1 Hz) for UI consumption.
 */
data class WorkoutMetrics(
    val elapsedSeconds: Long = 0,
    val totalDistanceMeters: Int = 0,
    val splitSeconds: Int = 0,       // current pace per 500m in seconds
    val strokesPerMinute: Int = 0,
    val watts: Int = 0,
    val strokeCount: Int = 0,
    val calories: Int = 0,
    val peakForce: Double = 0.0,
)

/**
 * Callback for telemetry consumers (e.g. SessionActivity).
 */
interface TelemetryListener {
    fun onMetricsUpdated(metrics: WorkoutMetrics)
    fun onConnectionStateChanged(state: ConnectionState)
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    HANDSHAKE,
    STREAMING,
    REPLAY,
    ERROR,
}
