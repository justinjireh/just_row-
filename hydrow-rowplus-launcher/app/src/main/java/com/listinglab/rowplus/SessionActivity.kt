package com.listinglab.rowplus

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.listinglab.rowplus.databinding.ActivitySessionBinding
import java.util.Locale

class SessionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionBinding
    private lateinit var sessionStore: SessionStore
    private val handler = Handler(Looper.getMainLooper())

    private var running = false
    private var startedAtMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            if (!running) {
                return
            }

            val elapsedSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000L).coerceAtLeast(1L)
            renderLiveMetrics(elapsedSeconds)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionStore = SessionStore(this)
        val profileKey = intent.getStringExtra(EXTRA_PROFILE)
        val profile = profileKey?.let(sessionStore::getProfile)
        if (profile == null) {
            Toast.makeText(this, R.string.profile_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.sessionProfileValue.text = profile.displayName
        binding.metricState.text = getString(R.string.metrics_stub)

        binding.startStopButton.setOnClickListener {
            if (running) {
                stopAndSave(profile)
            } else {
                start()
            }
        }

        binding.finishButton.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun start() {
        running = true
        startedAtMs = System.currentTimeMillis()
        binding.startStopButton.text = getString(R.string.stop_and_save)
        handler.post(tick)
    }

    private fun stopAndSave(profile: UserProfile) {
        running = false
        handler.removeCallbacks(tick)

        val elapsedSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000L).coerceAtLeast(1L)
        val distanceMeters = (elapsedSeconds * 4).toInt()
        val avgSpm = 24 + (elapsedSeconds % 6).toInt()
        val avgSplitSeconds = ((500.0 / distanceMeters.coerceAtLeast(1)) * elapsedSeconds)
            .toInt()
            .coerceIn(110, 240)
        val estimatedCalories = estimateCalories(elapsedSeconds, profile.weightLbs)

        val session = RowSession(
            startedAtEpochMs = startedAtMs,
            durationSeconds = elapsedSeconds,
            distanceMeters = distanceMeters,
            avgSplitSeconds = avgSplitSeconds,
            avgSpm = avgSpm,
            estimatedCalories = estimatedCalories,
        )

        sessionStore.saveSession(profile, session)
        Toast.makeText(
            this,
            getString(R.string.session_saved, profile.displayName, estimatedCalories),
            Toast.LENGTH_SHORT,
        ).show()
        finish()
    }

    private fun renderLiveMetrics(elapsedSeconds: Long) {
        val distanceMeters = (elapsedSeconds * 4).toInt()
        val avgSpm = 24 + (elapsedSeconds % 6).toInt()
        val avgSplitSeconds = ((500.0 / distanceMeters.coerceAtLeast(1)) * elapsedSeconds)
            .toInt()
            .coerceIn(110, 240)

        binding.metricDuration.text = formatDuration(elapsedSeconds)
        binding.metricDistance.text = String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
        binding.metricSplit.text = formatSplit(avgSplitSeconds)
        binding.metricSpm.text = String.format(Locale.US, "%d", avgSpm)
    }

    private fun estimateCalories(durationSeconds: Long, weightLbs: Int): Int {
        val weightKg = weightLbs * 0.45359237
        val durationHours = durationSeconds / 3600.0
        return (7.0 * weightKg * durationHours).toInt().coerceAtLeast(1)
    }

    private fun formatDuration(durationSeconds: Long): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun formatSplit(splitSeconds: Int): String {
        val minutes = splitSeconds / 60
        val seconds = splitSeconds % 60
        return String.format(Locale.US, "%d:%02d /500m", minutes, seconds)
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
    }
}
