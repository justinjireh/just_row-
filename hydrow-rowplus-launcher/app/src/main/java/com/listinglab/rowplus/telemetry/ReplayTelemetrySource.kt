package com.listinglab.rowplus.telemetry

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Replays a captured workout log file as if it were a live serial stream.
 *
 * Implements [TelemetrySource] so it can be used as a drop-in fallback
 * when live serial hardware is unavailable. The replay is real-time-paced
 * using the timestamps embedded in the log file.
 */
class ReplayTelemetrySource(
    private val context: Context,
    private val engine: WorkoutEngine,
    private val listener: TelemetryListener,
    private val assetFileName: String = "sample_workout.log",
    private val speedMultiplier: Float = 1.0f,
) : TelemetrySource {

    override val sourceType = TelemetrySource.SourceType.REPLAY

    private var replayThread: Thread? = null
    @Volatile private var running = false

    private var replayStartRealTime: Long = 0L
    private var replayStartLogTime: Long = 0L
    private var lastPlaybackLogTime: Long = 0L

    override fun start() {
        if (running) return
        running = true
        engine.reset()

        listener.onConnectionStateChanged(ConnectionState.REPLAY)

        replayThread = Thread({
            runReplay()
        }, "ReplayTelemetry").apply {
            isDaemon = true
            start()
        }
    }

    override fun stop() {
        running = false
        replayThread?.interrupt()
        replayThread = null
    }

    private fun runReplay() {
        try {
            val stream = context.assets.open(assetFileName)
            val reader = BufferedReader(InputStreamReader(stream))
            replayStartRealTime = System.currentTimeMillis()
            replayStartLogTime = 0L
            lastPlaybackLogTime = 0L

            var lastUiUpdate = 0L

            reader.useLines { lines ->
                for (line in lines) {
                    if (!running) break

                    val parsed = PacketParser.parseLine(line) ?: continue

                    val logTimestamp = extractTimestamp(line)
                    if (logTimestamp > 0) {
                        if (replayStartLogTime == 0L) {
                            replayStartLogTime = logTimestamp
                        }
                        lastPlaybackLogTime = logTimestamp

                        val logElapsed = logTimestamp - replayStartLogTime
                        val targetRealElapsed = (logElapsed / speedMultiplier).toLong()
                        val realElapsed = System.currentTimeMillis() - replayStartRealTime
                        val sleepMs = targetRealElapsed - realElapsed

                        if (sleepMs > 1) {
                            try {
                                Thread.sleep(sleepMs.coerceAtMost(100))
                            } catch (_: InterruptedException) {
                                if (!running) break
                            }
                        }
                    }

                    when (parsed) {
                        is PacketParser.ParsedPacket.Interval -> engine.onDi2(parsed.packet)
                        is PacketParser.ParsedPacket.Stroke -> engine.onDs2(parsed.packet)
                        else -> { /* ignore non-data packets during replay */ }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdate >= 1000L) {
                        lastUiUpdate = now
                        val metrics = engine.snapshot(currentPlaybackTime(now))
                        listener.onMetricsUpdated(metrics)
                    }
                }
            }

            if (running) {
                val finalMetrics = engine.snapshot(currentPlaybackTime(System.currentTimeMillis()))
                listener.onMetricsUpdated(finalMetrics)
                listener.onConnectionStateChanged(ConnectionState.DISCONNECTED)
            }
        } catch (e: Exception) {
            listener.onConnectionStateChanged(ConnectionState.ERROR)
        }

        running = false
    }

    private fun extractTimestamp(line: String): Long {
        val firstSpace = line.indexOf(' ')
        if (firstSpace <= 0) return 0L
        val candidate = line.substring(0, firstSpace)
        return if (candidate.all { it.isDigit() } && candidate.length >= 10) {
            candidate.toLongOrNull() ?: 0L
        } else {
            0L
        }
    }

    private fun currentPlaybackTime(now: Long): Long {
        if (replayStartLogTime == 0L) return now
        val replayElapsed = ((now - replayStartRealTime) * speedMultiplier).toLong()
        val scaledPlayback = replayStartLogTime + replayElapsed
        return maxOf(scaledPlayback, lastPlaybackLogTime)
    }
}
