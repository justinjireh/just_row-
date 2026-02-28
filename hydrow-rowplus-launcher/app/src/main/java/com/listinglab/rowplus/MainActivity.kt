package com.listinglab.rowplus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sessionStore: SessionStore
    private val clockHandler = Handler(Looper.getMainLooper())

    // View references (using findViewById since viewBinding still works but
    // the new layout has different IDs)
    private lateinit var logoText: TextView
    private lateinit var profileName: TextView
    private lateinit var profileBtnPrimary: TextView
    private lateinit var profileBtnSpouse: TextView
    private lateinit var statRowsValue: TextView
    private lateinit var statDistanceValue: TextView
    private lateinit var statStreakValue: TextView
    private lateinit var weekChart: LinearLayout
    private lateinit var weekLabels: LinearLayout
    private lateinit var lastRowCard: LinearLayout
    private lateinit var lastRowDate: TextView
    private lateinit var lastRowDistance: TextView
    private lateinit var lastRowDuration: TextView
    private lateinit var lastRowSplit: TextView
    private lateinit var lastRowSpm: TextView
    private lateinit var prBadge: TextView
    private lateinit var noHistoryText: TextView
    private lateinit var startRowButton: LinearLayout
    private lateinit var goalPromptText: TextView
    private lateinit var dockClock: TextView

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionStore = SessionStore(this)
        bindViews()
        setupListeners()
        buildWeekChart()
        refreshUi()
        runEntryAnimations()

        clockHandler.post(clockTick)
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        updateClock()
    }

    override fun onDestroy() {
        clockHandler.removeCallbacks(clockTick)
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────────────────
    // View binding
    // ──────────────────────────────────────────────────────────────

    private fun bindViews() {
        logoText = findViewById(R.id.logoText)
        profileName = findViewById(R.id.profileName)
        profileBtnPrimary = findViewById(R.id.profileBtnPrimary)
        profileBtnSpouse = findViewById(R.id.profileBtnSpouse)
        statRowsValue = findViewById(R.id.statRowsValue)
        statDistanceValue = findViewById(R.id.statDistanceValue)
        statStreakValue = findViewById(R.id.statStreakValue)
        weekChart = findViewById(R.id.weekChart)
        weekLabels = findViewById(R.id.weekLabels)
        lastRowCard = findViewById(R.id.lastRowCard)
        lastRowDate = findViewById(R.id.lastRowDate)
        lastRowDistance = findViewById(R.id.lastRowDistance)
        lastRowDuration = findViewById(R.id.lastRowDuration)
        lastRowSplit = findViewById(R.id.lastRowSplit)
        lastRowSpm = findViewById(R.id.lastRowSpm)
        prBadge = findViewById(R.id.prBadge)
        noHistoryText = findViewById(R.id.noHistoryText)
        startRowButton = findViewById(R.id.startRowButton)
        goalPromptText = findViewById(R.id.goalPromptText)
        dockClock = findViewById(R.id.dockClock)
    }

    // ──────────────────────────────────────────────────────────────
    // Click listeners
    // ──────────────────────────────────────────────────────────────

    private fun setupListeners() {
        profileBtnPrimary.setOnClickListener { switchProfile(UserProfile.PRIMARY) }
        profileBtnSpouse.setOnClickListener { switchProfile(UserProfile.SPOUSE) }

        startRowButton.setOnClickListener { startRowSession() }

        // Quick presets – pass target info to SessionActivity (future)
        findViewById<View>(R.id.quick20min).setOnClickListener { startRowSession() }
        findViewById<View>(R.id.quick5k).setOnClickListener { startRowSession() }
        findViewById<View>(R.id.quick30min).setOnClickListener { startRowSession() }
        findViewById<View>(R.id.quick10k).setOnClickListener { startRowSession() }

        // Dock
        findViewById<View>(R.id.dockHistory).setOnClickListener { showHistory() }
        findViewById<View>(R.id.dockBrowser).setOnClickListener { openBrowser() }
        findViewById<View>(R.id.dockSpotify).setOnClickListener {
            launchPackage("com.spotify.music", getString(R.string.spotify_missing))
        }

        // Admin: long-press logo
        logoText.setOnLongClickListener {
            showAdminActions()
            true
        }

        // Last row card opens full history
        lastRowCard.setOnClickListener { showHistory() }
    }

    // ──────────────────────────────────────────────────────────────
    // Profile switching
    // ──────────────────────────────────────────────────────────────

    private fun switchProfile(profile: UserProfile) {
        sessionStore.setActiveProfile(profile)
        refreshUi()
    }

    // ──────────────────────────────────────────────────────────────
    // UI refresh
    // ──────────────────────────────────────────────────────────────

    private fun refreshUi() {
        val active = sessionStore.getActiveProfile()
        val sessions = sessionStore.listSessions(active)

        // Profile area
        profileName.text = active.displayName
        profileBtnPrimary.text = UserProfile.PRIMARY.displayName
        profileBtnSpouse.text = UserProfile.SPOUSE.displayName
        updateProfileButtonStates(active)

        // Weekly stats (computed from sessions in the current week)
        val weekSessions = sessionsThisWeek(sessions)
        statRowsValue.text = weekSessions.size.toString()
        val totalKm = weekSessions.sumOf { it.distanceMeters } / 1000.0
        statDistanceValue.text = String.format(Locale.US, "%.1f km", totalKm)

        // Streak (simplified: consecutive days with at least one row)
        val streak = computeStreak(sessions)
        statStreakValue.text = streak.toString()

        // Week chart bars
        updateWeekChartBars(sessions)

        // Goal prompt
        val goalTarget = 5
        val done = weekSessions.size
        val remaining = (goalTarget - done).coerceAtLeast(0)
        goalPromptText.text = getString(R.string.goal_prompt, done, goalTarget, remaining)

        // Last row card
        if (sessions.isNotEmpty()) {
            val latest = sessions.first()
            lastRowCard.visibility = View.VISIBLE
            noHistoryText.visibility = View.GONE

            lastRowDate.text = formatRelativeDate(latest.startedAtEpochMs)
            lastRowDistance.text = formatDistance(latest.distanceMeters)
            lastRowDuration.text = formatDuration(latest.durationSeconds)
            lastRowSplit.text = formatSplitShort(latest.avgSplitSeconds)
            lastRowSpm.text = latest.avgSpm.toString()

            // PR badge: show if this is the best SPM ever
            val bestSpm = sessions.maxOf { it.avgSpm }
            prBadge.visibility = if (latest.avgSpm >= bestSpm && sessions.size > 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            lastRowCard.visibility = View.GONE
            noHistoryText.visibility = View.VISIBLE
        }
    }

    private fun updateProfileButtonStates(active: UserProfile) {
        profileBtnPrimary.isSelected = active == UserProfile.PRIMARY
        profileBtnSpouse.isSelected = active == UserProfile.SPOUSE

        profileBtnPrimary.setTextColor(
            ContextCompat.getColor(
                this,
                if (active == UserProfile.PRIMARY) R.color.rowplus_amber_soft else R.color.rowplus_text_secondary,
            ),
        )
        profileBtnSpouse.setTextColor(
            ContextCompat.getColor(
                this,
                if (active == UserProfile.SPOUSE) R.color.rowplus_amber_soft else R.color.rowplus_text_secondary,
            ),
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Week chart
    // ──────────────────────────────────────────────────────────────

    private fun buildWeekChart() {
        val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
        val todayDow = todayDayOfWeek() // 0=Mon .. 6=Sun

        weekChart.removeAllViews()
        weekLabels.removeAllViews()

        for (i in 0..6) {
            // Bar
            val bar = View(this)
            val lp = LinearLayout.LayoutParams(0, 3.dpToPx()).apply { weight = 1f }
            if (i < 6) lp.marginEnd = 4.dpToPx()
            bar.layoutParams = lp
            bar.setBackgroundColor(ContextCompat.getColor(this, R.color.rowplus_bar_inactive))
            weekChart.addView(bar)

            // Label
            val label = TextView(this)
            val llp = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT).apply { weight = 1f }
            label.layoutParams = llp
            label.text = dayLabels[i]
            label.textSize = 9f
            label.gravity = Gravity.CENTER
            label.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (i == todayDow) R.color.rowplus_teal else R.color.rowplus_text_muted,
                ),
            )
            weekLabels.addView(label)
        }
    }

    private fun updateWeekChartBars(sessions: List<RowSession>) {
        val todayDow = todayDayOfWeek()
        val cal = Calendar.getInstance()
        val weekStart = weekStartEpoch()

        // Aggregate meters per day of week (0=Mon)
        val metersPerDay = IntArray(7)
        for (s in sessions) {
            if (s.startedAtEpochMs < weekStart) continue
            cal.timeInMillis = s.startedAtEpochMs
            val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // shift to Mon=0
            metersPerDay[dow] += s.distanceMeters
        }

        val maxMeters = metersPerDay.maxOrNull()?.coerceAtLeast(1) ?: 1
        val chartHeight = weekChart.height.takeIf { it > 0 } ?: 36.dpToPx()

        for (i in 0..6) {
            val bar = weekChart.getChildAt(i) ?: continue
            val fraction = metersPerDay[i].toFloat() / maxMeters
            val barHeight = (fraction * chartHeight).toInt().coerceAtLeast(if (metersPerDay[i] > 0) 3.dpToPx() else 1.dpToPx())

            val lp = bar.layoutParams as LinearLayout.LayoutParams
            lp.height = barHeight
            bar.layoutParams = lp

            val colorRes = when {
                i == todayDow && metersPerDay[i] > 0 -> R.color.rowplus_bar_today
                metersPerDay[i] > 0 -> R.color.rowplus_bar_active
                else -> R.color.rowplus_bar_inactive
            }
            bar.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Clock
    // ──────────────────────────────────────────────────────────────

    private fun updateClock() {
        val now = Date()
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val timeFmt = SimpleDateFormat("h:mm a", Locale.US)
        dockClock.text = "${dayFmt.format(now).uppercase()} ${timeFmt.format(now)}"
    }

    // ──────────────────────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────────────────────

    private fun startRowSession() {
        val intent = Intent(this, SessionActivity::class.java)
            .putExtra(SessionActivity.EXTRA_PROFILE, sessionStore.getActiveProfile().storageKey)
        startActivity(intent)
    }

    private fun showHistory() {
        val active = sessionStore.getActiveProfile()
        val sessions = sessionStore.listSessions(active)
        if (sessions.isEmpty()) {
            Toast.makeText(this, R.string.no_history, Toast.LENGTH_SHORT).show()
            return
        }

        val message = buildString {
            sessions.take(12).forEachIndexed { index, row ->
                if (index > 0) append("\n\n")
                append(DateFormat.getDateTimeInstance().format(Date(row.startedAtEpochMs)))
                append("\n")
                append(formatDistance(row.distanceMeters))
                append(" in ")
                append(formatDuration(row.durationSeconds))
                append("\n")
                append("Split ")
                append(formatSplitFull(row.avgSplitSeconds))
                append(" • ")
                append(row.avgSpm)
                append(" spm")
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_title, active.displayName))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
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

    private fun showAdminActions() {
        val actions = arrayOf(
            getString(R.string.admin_open_hydrow),
            getString(R.string.admin_open_settings),
            getString(R.string.admin_open_launcher_chooser),
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.admin_title)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> launchPackage("com.truerowing.crew", getString(R.string.hydrow_missing))
                    1 -> startActivity(Intent(Settings.ACTION_SETTINGS))
                    2 -> openHomeSettings()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun launchPackage(packageName: String, missingMessage: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, missingMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Entry animations
    // ──────────────────────────────────────────────────────────────

    private fun runEntryAnimations() {
        val leftPanel = findViewById<View>(R.id.leftPanel)
        leftPanel.alpha = 0f
        leftPanel.translationX = -40f
        leftPanel.animate()
            .alpha(1f).translationX(0f)
            .setDuration(600).setInterpolator(DecelerateInterpolator(2f))
            .setStartDelay(100).start()

        startRowButton.alpha = 0f
        startRowButton.translationY = 30f
        startRowButton.animate()
            .alpha(1f).translationY(0f)
            .setDuration(600).setInterpolator(DecelerateInterpolator(2f))
            .setStartDelay(250).start()

        dockClock.parent?.let { dock ->
            val dockView = dock as View
            dockView.alpha = 0f
            dockView.translationY = 16f
            dockView.animate()
                .alpha(1f).translationY(0f)
                .setDuration(500).setInterpolator(DecelerateInterpolator(2f))
                .setStartDelay(400).start()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private fun sessionsThisWeek(sessions: List<RowSession>): List<RowSession> {
        val start = weekStartEpoch()
        return sessions.filter { it.startedAtEpochMs >= start }
    }

    private fun weekStartEpoch(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis > System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    private fun todayDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 .. Sun=6
    }

    private fun computeStreak(sessions: List<RowSession>): Int {
        if (sessions.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val daysWithRows = sessions.map { s ->
            cal.timeInMillis = s.startedAtEpochMs
            cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR)
        }.toSet().sorted().reversed()

        var streak = 0
        cal.timeInMillis = System.currentTimeMillis()
        var expectedDay = cal.get(Calendar.YEAR) * 400 + cal.get(Calendar.DAY_OF_YEAR)

        for (day in daysWithRows) {
            if (day == expectedDay || day == expectedDay - 1) {
                streak++
                expectedDay = day - 1
            } else {
                break
            }
        }
        return streak
    }

    private fun formatRelativeDate(epochMs: Long): String {
        val cal = Calendar.getInstance()
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        cal.timeInMillis = epochMs
        val rowDay = cal.get(Calendar.DAY_OF_YEAR)
        val rowYear = cal.get(Calendar.YEAR)

        val timeFmt = SimpleDateFormat("h:mm a", Locale.US)
        val timeStr = timeFmt.format(Date(epochMs))

        return when {
            rowYear == todayYear && rowDay == todayDay -> "Today, $timeStr"
            rowYear == todayYear && rowDay == todayDay - 1 -> "Yesterday, $timeStr"
            else -> {
                val dateFmt = SimpleDateFormat("MMM d", Locale.US)
                "${dateFmt.format(Date(epochMs))}, $timeStr"
            }
        }
    }

    private fun formatDistance(distanceMeters: Int): String {
        val km = distanceMeters / 1000.0
        return String.format(Locale.US, "%.2f km", km)
    }

    private fun formatDuration(durationSeconds: Long): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun formatSplitShort(splitSeconds: Int): String {
        val minutes = splitSeconds / 60
        val seconds = splitSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun formatSplitFull(splitSeconds: Int): String {
        val minutes = splitSeconds / 60
        val seconds = splitSeconds % 60
        return String.format(Locale.US, "%d:%02d /500m", minutes, seconds)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
