package com.listinglab.rowplus.telemetry

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Live telemetry source that reads the Hydrow rower's UART serial link.
 *
 * Attempts to open the serial device node (default `/dev/ttyMT1`) using
 * standard Java file I/O at 921600 baud. Performs the documented handshake,
 * enables continuous mode (`Cm 1`), and parses CR/LF-terminated ASCII lines
 * through [PacketParser] → [WorkoutEngine].
 *
 * ## Permission reality
 *
 * On production Hydrow tablets the serial device node is typically owned by
 * `root:system` with mode 0660. A normal (non-system, non-root) app will
 * receive a "Permission denied" IOException on open. This is expected.
 *
 * When that happens the source transitions to [ConnectionState.ERROR] and
 * reports the failure through [TelemetryListener.onConnectionStateChanged].
 * The caller (SessionActivity via [TelemetrySourceFactory]) can then decide
 * whether to fall back to replay.
 *
 * If the device node permissions are relaxed (e.g. via `adb shell chmod`
 * during development, or if a future OTA changes them) this adapter will
 * work without any code changes.
 *
 * ## Baud rate note
 *
 * Java's [FileInputStream]/[FileOutputStream] do not configure baud rate —
 * they rely on the kernel's default or whatever `stty` was used beforehand.
 * The stock app uses a JNI `libserial_port.so` that calls `tcsetattr()`.
 * For a pure-Java fallback we attempt to configure baud via `stty` through
 * [Runtime.exec]. If that fails, the stream is opened anyway in case the
 * kernel default is already correct.
 */
class SerialTelemetrySource(
    private val engine: WorkoutEngine,
    private val listener: TelemetryListener,
) : TelemetrySource {

    override val sourceType = TelemetrySource.SourceType.SERIAL_LIVE

    @Volatile private var running = false
    private var serialThread: Thread? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    /** Human-readable error message if open/handshake fails. */
    var lastError: String? = null
        private set

    override fun start() {
        if (running) return
        running = true
        engine.reset()
        lastError = null

        listener.onConnectionStateChanged(ConnectionState.CONNECTING)

        serialThread = Thread({
            runSerial()
        }, "SerialTelemetry").apply {
            isDaemon = true
            start()
        }
    }

    override fun stop() {
        running = false
        closeStreams()
        serialThread?.interrupt()
        serialThread = null
    }

    // ----- internal serial loop -----

    private fun runSerial() {
        try {
            val deviceFile = openDeviceNode()
            if (deviceFile == null) {
                lastError = "No accessible serial device found. " +
                    "Tried: ${DEVICE_PATHS.joinToString()}. " +
                    "This is expected on production builds without root."
                Log.w(TAG, lastError!!)
                listener.onConnectionStateChanged(ConnectionState.ERROR)
                running = false
                return
            }

            configureBaud(deviceFile.absolutePath)

            inputStream = FileInputStream(deviceFile)
            outputStream = FileOutputStream(deviceFile)

            if (!performHandshake()) {
                lastError = "Handshake failed — device did not respond OK."
                Log.w(TAG, lastError!!)
                listener.onConnectionStateChanged(ConnectionState.ERROR)
                closeStreams()
                running = false
                return
            }

            listener.onConnectionStateChanged(ConnectionState.STREAMING)

            enableContinuousMode()

            readLoop()

        } catch (e: SecurityException) {
            lastError = "Permission denied opening serial device: ${e.message}"
            Log.w(TAG, lastError!!, e)
            listener.onConnectionStateChanged(ConnectionState.ERROR)
        } catch (e: IOException) {
            if (running) {
                lastError = "Serial I/O error: ${e.message}"
                Log.w(TAG, lastError!!, e)
                listener.onConnectionStateChanged(ConnectionState.ERROR)
            }
        } catch (e: Exception) {
            if (running) {
                lastError = "Unexpected error: ${e.message}"
                Log.e(TAG, lastError!!, e)
                listener.onConnectionStateChanged(ConnectionState.ERROR)
            }
        } finally {
            disableContinuousMode()
            closeStreams()
            running = false
        }
    }

    /**
     * Try each known device path in order. Return the first that exists
     * and is readable, or null.
     */
    private fun openDeviceNode(): File? {
        for (path in DEVICE_PATHS) {
            val f = File(path)
            if (f.exists() && f.canRead()) {
                Log.i(TAG, "Found readable serial device: $path")
                return f
            }
            Log.d(TAG, "Skipping $path — exists=${f.exists()} canRead=${f.canRead()}")
        }
        return null
    }

    /**
     * Best-effort baud rate configuration via `stty`.
     * This is a fallback for the absence of JNI `tcsetattr()`.
     */
    private fun configureBaud(devicePath: String) {
        try {
            val proc = Runtime.getRuntime().exec(
                arrayOf("stty", "-F", devicePath, BAUD_RATE.toString())
            )
            val exitCode = proc.waitFor()
            if (exitCode == 0) {
                Log.i(TAG, "Baud rate set to $BAUD_RATE via stty")
            } else {
                Log.w(TAG, "stty exited with code $exitCode — baud may not be configured")
            }
        } catch (e: Exception) {
            Log.w(TAG, "stty not available or failed: ${e.message}")
        }
    }

    /**
     * Execute the Hydrow handshake sequence.
     *
     * ```
     * → "?"
     * ← expect "OK"
     * → "A 0\r\n"
     * → "J\r\n"
     * → "U 23130\r\n"
     * ```
     */
    private fun performHandshake(): Boolean {
        listener.onConnectionStateChanged(ConnectionState.HANDSHAKE)

        val out = outputStream ?: return false
        val inp = inputStream ?: return false

        for (attempt in 1..HANDSHAKE_RETRIES) {
            if (!running) return false

            // Send sync probe
            out.write("?".toByteArray())
            out.flush()

            // Wait for "OK" response (with timeout)
            val response = readLineWithTimeout(inp, HANDSHAKE_TIMEOUT_MS)
            if (response != null && response.trim().startsWith("OK")) {
                Log.i(TAG, "Handshake sync OK on attempt $attempt")

                // Disable echo
                out.write("A 0\r\n".toByteArray())
                out.flush()
                Thread.sleep(50)

                // Part ID confirmation
                out.write("J\r\n".toByteArray())
                out.flush()
                Thread.sleep(50)

                // Unlock code
                out.write("U 23130\r\n".toByteArray())
                out.flush()
                Thread.sleep(50)

                return true
            }

            Log.d(TAG, "Handshake attempt $attempt — no OK, retrying in ${HANDSHAKE_RETRY_MS}ms")
            Thread.sleep(HANDSHAKE_RETRY_MS)
        }

        return false
    }

    private fun enableContinuousMode() {
        try {
            outputStream?.write("Cm 1\r".toByteArray())
            outputStream?.flush()
            Log.i(TAG, "Sent Cm 1 — continuous mode enabled")
        } catch (e: IOException) {
            Log.w(TAG, "Failed to send Cm 1: ${e.message}")
        }
    }

    private fun disableContinuousMode() {
        try {
            outputStream?.write("Cm 0\r".toByteArray())
            outputStream?.flush()
            Log.d(TAG, "Sent Cm 0 — continuous mode disabled")
        } catch (_: Exception) {
            // best effort on teardown
        }
    }

    /**
     * Main data loop. Reads CR/LF-delimited lines, parses them, and feeds
     * the engine. Pushes UI updates at ~1 Hz.
     */
    private fun readLoop() {
        val inp = inputStream ?: return
        val lineBuffer = StringBuilder(MAX_LINE_LENGTH)
        val buf = ByteArray(1024)
        var lastUiUpdate = 0L

        while (running) {
            val bytesRead = try {
                inp.read(buf)
            } catch (_: InterruptedException) {
                break
            } catch (e: IOException) {
                if (running) {
                    Log.w(TAG, "Read error: ${e.message}")
                    listener.onConnectionStateChanged(ConnectionState.ERROR)
                }
                break
            }

            if (bytesRead <= 0) {
                // EOF or no data — brief pause to avoid busy-spin
                try { Thread.sleep(10) } catch (_: InterruptedException) { break }
                continue
            }

            for (i in 0 until bytesRead) {
                val c = buf[i].toInt().toChar()
                if (c == '\n') {
                    val line = lineBuffer.toString().trimEnd('\r')
                    lineBuffer.clear()
                    processLine(line)
                } else if (c != '\r' || lineBuffer.isEmpty()) {
                    // Accumulate into line buffer, but skip standalone \r
                    if (c != '\r') {
                        lineBuffer.append(c)
                    }
                }

                // Guard against absurdly long lines
                if (lineBuffer.length > MAX_LINE_LENGTH) {
                    lineBuffer.clear()
                }
            }

            // Push UI updates at ~1 Hz
            val now = System.currentTimeMillis()
            if (now - lastUiUpdate >= 1000L) {
                lastUiUpdate = now
                val metrics = engine.snapshot(now)
                listener.onMetricsUpdated(metrics)
            }
        }
    }

    private fun processLine(line: String) {
        if (line.isBlank()) return

        val parsed = PacketParser.parseLine(line) ?: return

        when (parsed) {
            is PacketParser.ParsedPacket.Interval -> engine.onDi2(parsed.packet)
            is PacketParser.ParsedPacket.Stroke -> engine.onDs2(parsed.packet)
            is PacketParser.ParsedPacket.ModeConfirm -> {
                Log.d(TAG, "Mode confirmed: ${parsed.mode}")
            }
            is PacketParser.ParsedPacket.Version -> {
                Log.i(TAG, "Rower firmware=${parsed.firmware} hardware=${parsed.hardware}")
            }
            is PacketParser.ParsedPacket.SerialNumber -> {
                Log.i(TAG, "Rower serial: ${parsed.serial}")
            }
            is PacketParser.ParsedPacket.ResistanceLevel -> {
                Log.i(TAG, "Drag factor: ${parsed.dragFactor}")
            }
            else -> { /* unknown / comment — ignore */ }
        }
    }

    /**
     * Read a single CR/LF-terminated line from the stream, with a timeout.
     * Returns null if the timeout expires before a complete line arrives.
     */
    private fun readLineWithTimeout(inp: FileInputStream, timeoutMs: Long): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        val sb = StringBuilder()
        val oneByte = ByteArray(1)

        while (System.currentTimeMillis() < deadline && running) {
            val available = inp.available()
            if (available > 0) {
                val read = inp.read(oneByte)
                if (read <= 0) continue
                val c = oneByte[0].toInt().toChar()
                if (c == '\n') {
                    return sb.toString().trimEnd('\r')
                }
                sb.append(c)
            } else {
                try { Thread.sleep(10) } catch (_: InterruptedException) { return null }
            }
        }
        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private fun closeStreams() {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
    }

    companion object {
        private const val TAG = "SerialTelemetry"

        /** Device paths to try, in priority order (same as stock app). */
        val DEVICE_PATHS = listOf(
            "/dev/ttyMT1",
            "/dev/ttyACM0",
            "/dev/ttyUSB0",
        )

        const val BAUD_RATE = 921600
        private const val MAX_LINE_LENGTH = 500
        private const val HANDSHAKE_RETRIES = 8
        private const val HANDSHAKE_TIMEOUT_MS = 500L
        private const val HANDSHAKE_RETRY_MS = 250L
    }
}
