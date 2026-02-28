package com.listinglab.rowplus

import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.listinglab.rowplus.telemetry.ConnectionState
import com.listinglab.rowplus.telemetry.LogReplayAdapter
import com.listinglab.rowplus.telemetry.TelemetryListener
import com.listinglab.rowplus.telemetry.WorkoutEngine
import com.listinglab.rowplus.telemetry.WorkoutMetrics
import java.util.Locale

class SessionActivity : AppCompatActivity(), TelemetryListener {

    private lateinit var sessionStore: SessionStore
    private lateinit var activeProfile: UserProfile
    private val handler = Handler(Looper.getMainLooper())

    private var running = false
    private var startedAtMs = 0L

    private lateinit var metricDistanceHero: TextView
    private lateinit var metricSpm: TextView
    private lateinit var metricDuration: TextView
    private lateinit var metricSplit: TextView
    private lateinit var metricCalories: TextView
    private lateinit var metricWatts: TextView
    private lateinit var metricStrokes: TextView
    private lateinit var sessionModeLabel: TextView
    private lateinit var sessionProfileLabel: TextView
    private lateinit var liveIndicator: View
    private lateinit var splitChartBars: LinearLayout

    private var splitBarCount = 0
    private var livePulseAnimator: ObjectAnimator? = null

    // Telemetry stack
    private val workoutEngine = WorkoutEngine()
    private lateinit var replayAdapter: LogReplayAdapter
    @Volatile
    private var latestMetrics = WorkoutMetrics()

    private val uiTick = object : Runnable {
        override fun run() {
            if (!running) return
            displayMetrics(latestMetrics)
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_session)

        sessionStore = SessionStore(this)
        val profileKey = intent.getStringExtra(EXTRA_PROFILE)
        val profile = profileKey?.let(sessionStore::getProfile)
        if (profile == null) {
            Toast.makeText(this, R.string.profile_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        activeProfile = profile

        replayAdapter = LogReplayAdapter(
            context = this,
            engine = workoutEngine,
            listener = this,
            speedMultiplier = 1f,
        )

        bindViews()
        sessionProfileLabel.text = profile.displayName
        sessionModeLabel.text = intent.getStringExtra(EXTRA_MODE) ?: MODE_FREE_ROW

        setupMediaLaunchers()

        findViewById<View>(R.id.stopButton).setOnClickListener {
            if (running) {
                stopAndSave()
            }
        }

        startSession()
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(uiTick)
        replayAdapter.stop()
        livePulseAnimator?.cancel()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    // -- TelemetryListener callbacks (called from replay thread) --

    override fun onMetricsUpdated(metrics: WorkoutMetrics) {
        latestMetrics = metrics
    }

    override fun onConnectionStateChanged(state: ConnectionState) {
        // Could update a status indicator in the future
    }

    // -- Session lifecycle --

    private fun startSession() {
        running = true
        startedAtMs = System.currentTimeMillis()

        livePulseAnimator = ObjectAnimator.ofFloat(liveIndicator, "alpha", 1f, 0.3f, 1f).apply {
            duration = 1_500L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Start telemetry data feed
        replayAdapter.start()

        // Start UI refresh loop
        handler.post(uiTick)
    }

    private fun stopAndSave() {
        running = false
        handler.removeCallbacks(uiTick)
        replayAdapter.stop()
        livePulseAnimator?.cancel()

        val m = latestMetrics
        val elapsed = latestMetrics.elapsedSeconds.coerceAtLeast(1L)

        val session = RowSession(
            startedAtEpochMs = startedAtMs,
            durationSeconds = elapsed,
            distanceMeters = m.totalDistanceMeters,
            avgSplitSeconds = m.splitSeconds,
            avgSpm = m.strokesPerMinute,
            estimatedCalories = m.calories,
        )
        sessionStore.saveSession(activeProfile, session)

        val intent = Intent(this, PostRowActivity::class.java).apply {
            putExtra(PostRowActivity.EXTRA_PROFILE, activeProfile.slotKey)
            putExtra(PostRowActivity.EXTRA_DISTANCE, m.totalDistanceMeters)
            putExtra(PostRowActivity.EXTRA_DURATION, elapsed)
            putExtra(PostRowActivity.EXTRA_SPLIT, m.splitSeconds)
            putExtra(PostRowActivity.EXTRA_SPM, m.strokesPerMinute)
            putExtra(PostRowActivity.EXTRA_CALORIES, m.calories)
            putExtra(PostRowActivity.EXTRA_STROKES, m.strokeCount)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // -- UI display --

    private fun displayMetrics(m: WorkoutMetrics) {
        metricDistanceHero.text = String.format(Locale.US, "%,d", m.totalDistanceMeters)
        metricSpm.text = m.strokesPerMinute.toString()
        metricDuration.text = formatDuration(m.elapsedSeconds)
        metricSplit.text = formatSplit(m.splitSeconds)
        metricCalories.text = m.calories.toString()
        metricWatts.text = m.watts.toString()
        metricStrokes.text = m.strokeCount.toString()

        val expectedBars = (m.elapsedSeconds / 30L).toInt()
        while (splitBarCount < expectedBars && splitBarCount < 20) {
            addSplitBar(m.splitSeconds)
            splitBarCount++
        }
    }

    private fun addSplitBar(splitSeconds: Int) {
        val bar = View(this)
        val maxBarHeight = 60.dpToPx()
        val fraction = 1f - ((splitSeconds - 90).toFloat() / 150f).coerceIn(0f, 1f)
        val barHeight = (fraction * maxBarHeight).toInt().coerceAtLeast(4.dpToPx())

        val params = LinearLayout.LayoutParams(0, barHeight).apply {
            weight = 1f
            marginEnd = 2.dpToPx()
        }

        bar.layoutParams = params
        bar.setBackgroundColor(ContextCompat.getColor(this, R.color.rowplus_teal_40))
        bar.alpha = 0f
        bar.translationY = 10f
        splitChartBars.addView(bar)
        bar.animate().alpha(1f).translationY(0f).setDuration(300L).start()
    }

    private fun bindViews() {
        metricDistanceHero = findViewById(R.id.metricDistanceHero)
        metricSpm = findViewById(R.id.metricSpm)
        metricDuration = findViewById(R.id.metricDuration)
        metricSplit = findViewById(R.id.metricSplit)
        metricCalories = findViewById(R.id.metricCalories)
        metricWatts = findViewById(R.id.metricWatts)
        metricStrokes = findViewById(R.id.metricStrokes)
        sessionModeLabel = findViewById(R.id.sessionModeLabel)
        sessionProfileLabel = findViewById(R.id.sessionProfileLabel)
        liveIndicator = findViewById(R.id.liveIndicator)
        splitChartBars = findViewById(R.id.splitChartBars)
    }

    // -- Media launchers (unchanged) --

    private fun setupMediaLaunchers() {
        val mediaMap = mapOf(
            R.id.mediaNetflix to "com.netflix.mediaclient",
            R.id.mediaYoutube to "com.google.android.youtube",
            R.id.mediaSpotify to "com.spotify.music",
            R.id.mediaHulu to "com.hulu.plus",
            R.id.mediaDisney to "com.disney.disneyplus",
            R.id.mediaHbo to "com.hbo.hbonow",
            R.id.mediaBrowser to null,
        )

        mediaMap.forEach { (viewId, packageName) ->
            findViewById<View>(viewId).setOnClickListener {
                if (packageName == null) {
                    openBrowser()
                } else {
                    launchMediaApp(packageName)
                }
            }
        }
    }

    private fun launchMediaApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
            return
        }

        Toast.makeText(
            this,
            "${packageName.substringAfterLast('.')} is not installed",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun openBrowser() {
        val browserIntent = packageManager.getLaunchIntentForPackage("com.android.browser")
        if (browserIntent != null) {
            startActivity(browserIntent)
            return
        }

        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        try {
            startActivity(fallback)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.browser_missing, Toast.LENGTH_SHORT).show()
        }
    }

    // -- Formatters --

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60L
        val remainder = seconds % 60L
        return String.format(Locale.US, "%d:%02d", minutes, remainder)
    }

    private fun formatSplit(splitSeconds: Int): String {
        val minutes = splitSeconds / 60
        val remainder = splitSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, remainder)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_MODE = "mode"

        const val MODE_FREE_ROW = "FREE ROW"
        const val MODE_TIME_TARGET = "TIME TARGET"
        const val MODE_DISTANCE_TARGET = "DISTANCE TARGET"
    }
}
