package com.listinglab.rowplus

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.listinglab.rowplus.telemetry.ConnectionState
import com.listinglab.rowplus.telemetry.TelemetryListener
import com.listinglab.rowplus.telemetry.TelemetrySource
import com.listinglab.rowplus.telemetry.TelemetrySourceFactory
import com.listinglab.rowplus.telemetry.WorkoutEngine
import com.listinglab.rowplus.telemetry.WorkoutMetrics
import java.util.Locale

class SessionActivity : AppCompatActivity(), TelemetryListener {

    private lateinit var sessionStore: SessionStore
    private lateinit var activeProfile: UserProfile
    private val handler = Handler(Looper.getMainLooper())
    private var baseModeLabel: String = MODE_FREE_ROW

    private var running = false
    private var startedAtMs = 0L

    // Metric views
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
    private var telemetrySource: TelemetrySource? = null
    private var serialBlockerNote: String? = null
    @Volatile
    private var latestMetrics = WorkoutMetrics()

    // Dock
    private lateinit var dockManager: DockManager
    private lateinit var dockAdapter: DockAdapter
    private lateinit var expandMediaBtn: View

    // Overlay service — provides a floating "return" control + live metrics
    // when a media app is in the foreground.
    private var overlayService: MetricsOverlayService? = null
    private var overlayBound = false
    private var mediaAppLaunched = false

    private val overlayConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MetricsOverlayService.OverlayBinder
            overlayService = binder.getService()
            overlayBound = true
            // Keep overlay hidden until a media app is actually launched
            overlayService?.hide()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            overlayService = null
            overlayBound = false
        }
    }

    private val uiTick = object : Runnable {
        override fun run() {
            if (!running) return
            displayMetrics(latestMetrics)
            updateIdleIndicator()

            // Feed overlay with live metrics while a media app is in the foreground
            if (overlayBound && mediaAppLaunched) {
                val m = latestMetrics
                overlayService?.updateMetrics(
                    distance = String.format(Locale.US, "%,dm", m.totalDistanceMeters),
                    duration = formatDuration(m.elapsedSeconds),
                    spm = m.strokesPerMinute.toString(),
                    split = formatSplit(m.splitSeconds),
                )
            }

            handler.postDelayed(this, 1_000L)
        }
    }

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

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

        // Create the best available telemetry source
        val probeResult = TelemetrySourceFactory.create(
            context = this,
            engine = workoutEngine,
            listener = this,
            allowReplayFallback = true,
        )
        telemetrySource = probeResult.source
        serialBlockerNote = probeResult.blockerNote

        bindViews()
        sessionProfileLabel.text = profile.displayName
        baseModeLabel = intent.getStringExtra(EXTRA_MODE) ?: MODE_FREE_ROW

        // Set initial mode label based on source type
        val sourceTag = when (telemetrySource?.sourceType) {
            TelemetrySource.SourceType.SERIAL_LIVE -> "LIVE"
            TelemetrySource.SourceType.REPLAY -> "REPLAY"
            null -> "ERROR"
        }
        sessionModeLabel.text = "$baseModeLabel · $sourceTag"

        // Initialize app dock
        setupDock()

        // Stop button
        findViewById<View>(R.id.stopButton).setOnClickListener {
            if (running) {
                stopAndSave()
            }
        }

        // Expand-to-full-screen button (visible only in multi-window mode)
        expandMediaBtn = findViewById(R.id.expandMediaBtn)
        expandMediaBtn.setOnClickListener { expandMediaToFullScreen() }

        // The Hydrow build does not currently grant overlay windows reliably,
        // so keep the optional floating overlay disabled by default.

        if (telemetrySource != null) {
            startSession()
        } else {
            sessionModeLabel.text = "$baseModeLabel · NO SIGNAL"
            liveIndicator.setBackgroundColor(
                ContextCompat.getColor(this, R.color.rowplus_text_faint),
            )
            Toast.makeText(
                this,
                "Unable to connect to rower hardware. ${serialBlockerNote ?: ""}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Returning from a media app — hide the floating overlay
        if (mediaAppLaunched) {
            mediaAppLaunched = false
            overlayService?.hide()
        }
        // Refresh dock in case apps were installed/removed while away
        if (::dockManager.isInitialized) {
            dockAdapter.apps = dockManager.getDockApps()
        }
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(uiTick)
        telemetrySource?.stop()
        livePulseAnimator?.cancel()
        if (overlayBound) {
            overlayService?.hide()
            unbindService(overlayConnection)
            overlayBound = false
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        // Show "expand" button only when sharing the screen with a media app
        expandMediaBtn.visibility = if (isInMultiWindowMode) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------
    // TelemetryListener callbacks (called from telemetry thread)
    // ---------------------------------------------------------------

    override fun onMetricsUpdated(metrics: WorkoutMetrics) {
        latestMetrics = metrics
    }

    override fun onConnectionStateChanged(state: ConnectionState) {
        runOnUiThread {
            val stateTag = when (state) {
                ConnectionState.STREAMING -> "LIVE"
                ConnectionState.REPLAY -> "REPLAY"
                ConnectionState.CONNECTING -> "CONNECTING…"
                ConnectionState.HANDSHAKE -> "HANDSHAKE…"
                ConnectionState.DISCONNECTED -> "DISCONNECTED"
                ConnectionState.ERROR -> {
                    if (telemetrySource?.sourceType == TelemetrySource.SourceType.SERIAL_LIVE) {
                        "SERIAL ERROR"
                    } else {
                        "ERROR"
                    }
                }
            }
            sessionModeLabel.text = "$baseModeLabel · $stateTag"

            val dotColor = when (state) {
                ConnectionState.STREAMING -> R.color.rowplus_teal
                ConnectionState.REPLAY -> R.color.rowplus_amber
                ConnectionState.ERROR -> R.color.rowplus_stop_red
                else -> R.color.rowplus_text_faint
            }
            liveIndicator.background.setTint(ContextCompat.getColor(this, dotColor))
        }
    }

    // ---------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------

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
        telemetrySource?.start()

        // Start UI refresh loop
        handler.post(uiTick)
    }

    private fun stopAndSave() {
        running = false
        handler.removeCallbacks(uiTick)
        telemetrySource?.stop()
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

    // ---------------------------------------------------------------
    // Dock
    // ---------------------------------------------------------------

    private fun setupDock() {
        dockManager = DockManager(this)
        dockAdapter = DockAdapter { app ->
            if (app.isAddTile) {
                showAppPicker()
            } else {
                launchDockApp(app)
            }
        }
        val recyclerView = findViewById<RecyclerView>(R.id.dockRecyclerView)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = dockAdapter
        dockAdapter.apps = dockManager.getDockApps()
    }

    private fun launchDockApp(app: DockApp) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent == null) {
            Toast.makeText(
                this,
                getString(R.string.app_not_installed, app.label),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        mediaAppLaunched = true

        // FLAG_ACTIVITY_LAUNCH_ADJACENT puts the app in the other half when the
        // device is already in split-screen / multi-window mode. On devices that
        // don't support multi-window the flag is safely ignored and the media app
        // launches full-screen instead (the documented fallback path).
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK,
        )

        // Show the floating overlay so the user can see live metrics and tap
        // "ROW+" to return while the media app is in the foreground.
        overlayService?.show()

        startActivity(launchIntent)
    }

    /**
     * In multi-window (split) mode, hides RowPlus so the media app expands to
     * full screen. The floating overlay remains visible for the user to return.
     */
    private fun expandMediaToFullScreen() {
        overlayService?.show()
        mediaAppLaunched = true
        moveTaskToBack(true)
    }

    // ---------------------------------------------------------------
    // App picker dialog
    // ---------------------------------------------------------------

    private fun showAppPicker() {
        val pickerView = layoutInflater.inflate(R.layout.dialog_app_picker, null)
        val recyclerView = pickerView.findViewById<RecyclerView>(R.id.appPickerList)
        val allApps = dockManager.getAllLaunchableApps()
        val pinnedSet = dockManager.getPinnedPackages().toSet()

        val dialog = AlertDialog.Builder(this)
            .setView(pickerView)
            .create()

        // Transparent window so our dark custom layout shows cleanly
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppPickerAdapter(allApps, pinnedSet) { app ->
            if (app.packageName in pinnedSet) {
                dockManager.removePinnedApp(app.packageName)
            } else {
                dockManager.addPinnedApp(app.packageName)
            }
            dockAdapter.apps = dockManager.getDockApps()
            dialog.dismiss()
        }

        // Path from picker to app store
        pickerView.findViewById<View>(R.id.openAppStoreBtn).setOnClickListener {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=media")),
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_app_store, Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // ---------------------------------------------------------------
    // Idle indicator
    // ---------------------------------------------------------------

    private fun updateIdleIndicator() {
        if (workoutEngine.isIdle && telemetrySource?.sourceType == TelemetrySource.SourceType.SERIAL_LIVE) {
            val currentLabel = sessionModeLabel.text.toString()
            if (!currentLabel.contains("IDLE")) {
                sessionModeLabel.text = "$baseModeLabel · LIVE · IDLE"
            }
        }
    }

    // ---------------------------------------------------------------
    // UI display
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // Formatters
    // ---------------------------------------------------------------

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
