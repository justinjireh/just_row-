package com.listinglab.rowplus.telemetry

/**
 * Computes running workout metrics from raw Di2/Ds2 packet stream.
 *
 * Accumulates distance from Di2 deltas, extracts watts/SPM from Ds2 strokes,
 * and publishes WorkoutMetrics at each stroke boundary.
 *
 * ## Idle detection
 *
 * When no Di2 or Ds2 packet arrives for [IDLE_THRESHOLD_MS], the engine
 * considers the rower idle. During idle:
 * - Elapsed time stops advancing (paused at last-data moment)
 * - SPM drops to 0
 * - Other accumulated metrics (distance, strokes, calories) are preserved
 *
 * Metrics resume advancing as soon as a new packet arrives.
 */
class WorkoutEngine {

    private var totalDistanceMeters: Double = 0.0
    private var strokeCount: Int = 0
    private var lastStrokeTimestamp: Long = 0L
    private var currentSpm: Int = 0
    private var currentWatts: Int = 0
    private var peakForce: Double = 0.0
    private var lastCompletedPeakForce: Double = 0.0

    private var workoutStartTimestamp: Long = 0L
    private var lastDi2Timestamp: Long = 0L

    // Calorie computation accumulator
    private var totalWattSeconds: Double = 0.0

    // Idle detection — tracks wall-clock time of last data packet
    @Volatile private var lastDataWallClock: Long = 0L

    // Accumulated active rowing time (only advances while data is flowing)
    private var activeRowingMs: Long = 0L
    private var lastActiveTickWallClock: Long = 0L

    /** True when the rower is idle (no packets for [IDLE_THRESHOLD_MS]). */
    val isIdle: Boolean
        get() {
            if (lastDataWallClock == 0L) return true
            return System.currentTimeMillis() - lastDataWallClock > IDLE_THRESHOLD_MS
        }

    fun reset() {
        totalDistanceMeters = 0.0
        strokeCount = 0
        lastStrokeTimestamp = 0L
        currentSpm = 0
        currentWatts = 0
        peakForce = 0.0
        lastCompletedPeakForce = 0.0
        workoutStartTimestamp = 0L
        lastDi2Timestamp = 0L
        totalWattSeconds = 0.0
        lastDataWallClock = 0L
        activeRowingMs = 0L
        lastActiveTickWallClock = 0L
    }

    /**
     * Feed a Di2 (interval) packet into the engine.
     * Called ~100+ times per second from the data source.
     */
    fun onDi2(packet: Di2Packet) {
        if (workoutStartTimestamp == 0L) {
            workoutStartTimestamp = packet.timestamp
        }

        markActive()

        // Accumulate delta distance (each sample is a small increment in meters)
        totalDistanceMeters += packet.distance

        // Track peak handle force for the current stroke
        if (packet.handleForce > peakForce) {
            peakForce = packet.handleForce
        }

        // Accumulate watt-seconds for calorie estimation
        if (lastDi2Timestamp > 0 && currentWatts > 0) {
            val dtSeconds = (packet.timestamp - lastDi2Timestamp) / 1000.0
            totalWattSeconds += currentWatts * dtSeconds
        }
        lastDi2Timestamp = packet.timestamp
    }

    /**
     * Feed a Ds2 (stroke) packet into the engine.
     * Called once per completed stroke (~22-30 times per minute).
     */
    fun onDs2(packet: Ds2Packet) {
        strokeCount++
        markActive()

        // Average watts for this stroke
        currentWatts = packet.averagePower.toInt()

        // Compute SPM from inter-stroke interval
        if (lastStrokeTimestamp > 0) {
            val intervalMs = packet.timestamp - lastStrokeTimestamp
            if (intervalMs > 0) {
                currentSpm = (60_000.0 / intervalMs).toInt().coerceIn(10, 50)
            }
        }
        lastStrokeTimestamp = packet.timestamp

        // Preserve the completed-stroke peak before clearing the live accumulator.
        lastCompletedPeakForce = peakForce
        peakForce = 0.0
    }

    /**
     * Snapshot the current workout metrics for UI display.
     */
    fun snapshot(@Suppress("UNUSED_PARAMETER") currentTimeMs: Long): WorkoutMetrics {
        updateActiveTime()

        val elapsed = activeRowingMs / 1000L

        val distanceMeters = totalDistanceMeters.toInt()

        // Split = seconds per 500m
        val splitSeconds = if (distanceMeters > 0 && elapsed > 0) {
            ((500.0 / distanceMeters) * elapsed).toInt().coerceIn(60, 600)
        } else {
            0
        }

        // Calories: standard rowing formula
        // ~1 calorie per 4.2 kJ of work, 1 watt-second = 1 joule
        val calories = (totalWattSeconds / 4184.0).toInt().coerceAtLeast(
            if (elapsed > 0) 1 else 0
        )

        // When idle, show SPM as 0 (rower has stopped)
        val displaySpm = if (isIdle) 0 else currentSpm

        return WorkoutMetrics(
            elapsedSeconds = elapsed,
            totalDistanceMeters = distanceMeters,
            splitSeconds = splitSeconds,
            strokesPerMinute = displaySpm,
            watts = if (isIdle) 0 else currentWatts,
            strokeCount = strokeCount,
            calories = calories,
            peakForce = maxOf(peakForce, lastCompletedPeakForce),
        )
    }

    /** Record that live data just arrived. */
    private fun markActive() {
        val now = System.currentTimeMillis()
        updateActiveTime()
        lastDataWallClock = now

        // Start a new active segment if we were idle
        if (lastActiveTickWallClock == 0L || now - lastActiveTickWallClock > IDLE_THRESHOLD_MS) {
            lastActiveTickWallClock = now
        }
    }

    /**
     * Advance [activeRowingMs] by the time since [lastActiveTickWallClock],
     * but only if we are not idle.
     */
    private fun updateActiveTime() {
        val now = System.currentTimeMillis()
        if (lastActiveTickWallClock > 0 && !isIdle) {
            val delta = now - lastActiveTickWallClock
            if (delta in 1..MAX_ACTIVE_TICK_MS) {
                activeRowingMs += delta
            }
        }
        if (!isIdle) {
            lastActiveTickWallClock = now
        }
    }

    companion object {
        /** If no Di2/Ds2 data for this long, consider rower idle. */
        const val IDLE_THRESHOLD_MS = 3_000L

        /** Cap single active-time increments to avoid clock jumps. */
        private const val MAX_ACTIVE_TICK_MS = 5_000L
    }
}
