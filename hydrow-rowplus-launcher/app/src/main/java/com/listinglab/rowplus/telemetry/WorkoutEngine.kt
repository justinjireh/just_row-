package com.listinglab.rowplus.telemetry

/**
 * Computes running workout metrics from raw Di2/Ds2 packet stream.
 *
 * Accumulates distance from Di2 deltas, extracts watts/SPM from Ds2 strokes,
 * and publishes WorkoutMetrics at each stroke boundary.
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
    }

    /**
     * Feed a Di2 (interval) packet into the engine.
     * Called ~100+ times per second from the data source.
     */
    fun onDi2(packet: Di2Packet) {
        if (workoutStartTimestamp == 0L) {
            workoutStartTimestamp = packet.timestamp
        }

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
    fun snapshot(currentTimeMs: Long): WorkoutMetrics {
        val elapsed = if (workoutStartTimestamp > 0) {
            (currentTimeMs - workoutStartTimestamp) / 1000L
        } else {
            0L
        }

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

        return WorkoutMetrics(
            elapsedSeconds = elapsed,
            totalDistanceMeters = distanceMeters,
            splitSeconds = splitSeconds,
            strokesPerMinute = currentSpm,
            watts = currentWatts,
            strokeCount = strokeCount,
            calories = calories,
            peakForce = maxOf(peakForce, lastCompletedPeakForce),
        )
    }
}
