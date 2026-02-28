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
import android.widget.EditText
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

    private lateinit var logoText: TextView
    private lateinit var profileName: TextView
    private lateinit var profileSubtitle: TextView
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

    private fun bindViews() {
        logoText = findViewById(R.id.logoText)
        profileName = findViewById(R.id.profileName)
        profileSubtitle = findViewById(R.id.profileSubtitle)
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

    private fun setupListeners() {
        profileBtnPrimary.setOnClickListener { onProfileSlotTapped(UserProfile.SLOT_ONE) }
        profileBtnSpouse.setOnClickListener { onProfileSlotTapped(UserProfile.SLOT_TWO) }
        profileBtnPrimary.setOnLongClickListener {
            showProfileEditor(UserProfile.SLOT_ONE, sessionStore.getProfile(UserProfile.SLOT_ONE))
            true
        }
        profileBtnSpouse.setOnLongClickListener {
            showProfileEditor(UserProfile.SLOT_TWO, sessionStore.getProfile(UserProfile.SLOT_TWO))
            true
        }

        startRowButton.setOnClickListener { startRowSession() }

        findViewById<View>(R.id.quick20min).setOnClickListener { startRowSession() }
        findViewById<View>(R.id.quick5k).setOnClickListener { startRowSession() }
        findViewById<View>(R.id.quick30min).setOnClickListener { startRowSession() }
        findViewById<View>(R.id.quick10k).setOnClickListener { startRowSession() }

        findViewById<View>(R.id.dockHistory).setOnClickListener { showHistory() }
        findViewById<View>(R.id.dockBrowser).setOnClickListener { openBrowser() }
        findViewById<View>(R.id.dockSpotify).setOnClickListener {
            launchPackage("com.spotify.music", getString(R.string.spotify_missing))
        }

        logoText.setOnLongClickListener {
            showAdminActions()
            true
        }

        lastRowCard.setOnClickListener { showHistory() }
    }

    private fun onProfileSlotTapped(slotKey: String) {
        val profile = sessionStore.getProfile(slotKey)
        if (profile == null) {
            showProfileEditor(slotKey, null)
            return
        }

        sessionStore.setActiveProfile(profile)
        refreshUi()
    }

    private fun showProfileEditor(slotKey: String, existing: UserProfile?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_setup, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.profileNameInput)
        val heightInput = dialogView.findViewById<EditText>(R.id.profileHeightInput)
        val weightInput = dialogView.findViewById<EditText>(R.id.profileWeightInput)

        if (existing != null) {
            nameInput.setText(existing.displayName)
            heightInput.setText(existing.heightInches.toString())
            weightInput.setText(existing.weightLbs.toString())
        }

        val titleRes = if (existing == null) {
            R.string.profile_dialog_add_title
        } else {
            R.string.profile_dialog_edit_title
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.profile_dialog_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val displayName = nameInput.text.toString().trim()
                val heightInches = heightInput.text.toString().trim().toIntOrNull()
                val weightLbs = weightInput.text.toString().trim().toIntOrNull()

                var hasError = false

                if (displayName.isBlank()) {
                    nameInput.error = getString(R.string.profile_name_required)
                    hasError = true
                }

                if (heightInches == null || heightInches !in 36..96) {
                    heightInput.error = getString(R.string.profile_height_required)
                    hasError = true
                }

                if (weightLbs == null || weightLbs !in 50..700) {
                    weightInput.error = getString(R.string.profile_weight_required)
                    hasError = true
                }

                if (hasError) {
                    return@setOnClickListener
                }

                val safeHeight = heightInches ?: return@setOnClickListener
                val safeWeight = weightLbs ?: return@setOnClickListener

                val saved = UserProfile(
                    slotKey = slotKey,
                    displayName = displayName,
                    heightInches = safeHeight,
                    weightLbs = safeWeight,
                )

                sessionStore.saveProfile(saved)
                sessionStore.setActiveProfile(saved)
                refreshUi()
                Toast.makeText(
                    this,
                    getString(R.string.profile_saved, saved.displayName),
                    Toast.LENGTH_SHORT,
                ).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun refreshUi() {
        val primaryProfile = sessionStore.getProfile(UserProfile.SLOT_ONE)
        val secondaryProfile = sessionStore.getProfile(UserProfile.SLOT_TWO)
        val activeProfile = sessionStore.getActiveProfile()

        profileBtnPrimary.text = primaryProfile?.displayName ?: getString(R.string.add_profile_short)
        profileBtnSpouse.text = secondaryProfile?.displayName ?: getString(R.string.add_profile_short)
        updateProfileButtonStates(primaryProfile, secondaryProfile, activeProfile?.slotKey)

        if (activeProfile == null) {
            profileName.text = getString(R.string.profile_empty_title)
            profileSubtitle.text = getString(R.string.profile_empty_subtitle)
            statRowsValue.text = "0"
            statDistanceValue.text = getString(R.string.stat_distance_empty)
            statStreakValue.text = "0"
            updateWeekChartBars(emptyList())
            goalPromptText.text = getString(R.string.goal_prompt_empty)
            lastRowCard.visibility = View.GONE
            noHistoryText.visibility = View.VISIBLE
            noHistoryText.text = getString(R.string.no_profile_history)
            return
        }

        val sessions = sessionStore.listSessions(activeProfile)

        profileName.text = activeProfile.displayName
        profileSubtitle.text = getString(
            R.string.profile_stats_summary,
            activeProfile.heightInches,
            activeProfile.weightLbs,
        )

        val weekSessions = sessionsThisWeek(sessions)
        statRowsValue.text = weekSessions.size.toString()
        val totalKm = weekSessions.sumOf { it.distanceMeters } / 1000.0
        statDistanceValue.text = String.format(Locale.US, "%.1f km", totalKm)
        statStreakValue.text = computeStreak(sessions).toString()
        updateWeekChartBars(sessions)

        val goalTarget = 5
        val done = weekSessions.size
        val remaining = (goalTarget - done).coerceAtLeast(0)
        goalPromptText.text = getString(R.string.goal_prompt, done, goalTarget, remaining)

        if (sessions.isNotEmpty()) {
            val latest = sessions.first()
            lastRowCard.visibility = View.VISIBLE
            noHistoryText.visibility = View.GONE

            lastRowDate.text = formatRelativeDate(latest.startedAtEpochMs)
            lastRowDistance.text = formatDistance(latest.distanceMeters)
            lastRowDuration.text = formatDuration(latest.durationSeconds)
            lastRowSplit.text = formatSplitShort(latest.avgSplitSeconds)
            lastRowSpm.text = latest.avgSpm.toString()

            val bestSpm = sessions.maxOf { it.avgSpm }
            prBadge.visibility = if (latest.avgSpm >= bestSpm && sessions.size > 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            lastRowCard.visibility = View.GONE
            noHistoryText.visibility = View.VISIBLE
            noHistoryText.text = getString(R.string.no_history)
        }
    }

    private fun updateProfileButtonStates(
        primaryProfile: UserProfile?,
        secondaryProfile: UserProfile?,
        activeSlotKey: String?,
    ) {
        applyProfileButtonState(
            button = profileBtnPrimary,
            profile = primaryProfile,
            isSelected = activeSlotKey == UserProfile.SLOT_ONE,
        )
        applyProfileButtonState(
            button = profileBtnSpouse,
            profile = secondaryProfile,
            isSelected = activeSlotKey == UserProfile.SLOT_TWO,
        )
    }

    private fun applyProfileButtonState(
        button: TextView,
        profile: UserProfile?,
        isSelected: Boolean,
    ) {
        button.isSelected = isSelected
        button.alpha = if (profile == null) 0.82f else 1f

        val colorRes = when {
            isSelected -> R.color.rowplus_amber_soft
            profile == null -> R.color.rowplus_text_muted
            else -> R.color.rowplus_text_secondary
        }
        button.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun buildWeekChart() {
        val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
        val todayDow = todayDayOfWeek()

        weekChart.removeAllViews()
        weekLabels.removeAllViews()

        for (index in 0..6) {
            val bar = View(this)
            val barParams = LinearLayout.LayoutParams(0, 3.dpToPx()).apply { weight = 1f }
            if (index < 6) {
                barParams.marginEnd = 4.dpToPx()
            }
            bar.layoutParams = barParams
            bar.setBackgroundColor(ContextCompat.getColor(this, R.color.rowplus_bar_inactive))
            weekChart.addView(bar)

            val label = TextView(this)
            label.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT).apply { weight = 1f }
            label.text = dayLabels[index]
            label.textSize = 9f
            label.gravity = Gravity.CENTER
            label.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (index == todayDow) R.color.rowplus_teal else R.color.rowplus_text_muted,
                ),
            )
            weekLabels.addView(label)
        }
    }

    private fun updateWeekChartBars(sessions: List<RowSession>) {
        val todayDow = todayDayOfWeek()
        val cal = Calendar.getInstance()
        val weekStart = weekStartEpoch()
        val metersPerDay = IntArray(7)

        for (session in sessions) {
            if (session.startedAtEpochMs < weekStart) {
                continue
            }
            cal.timeInMillis = session.startedAtEpochMs
            val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            metersPerDay[dayOfWeek] += session.distanceMeters
        }

        val maxMeters = metersPerDay.maxOrNull()?.coerceAtLeast(1) ?: 1
        val chartHeight = weekChart.height.takeIf { it > 0 } ?: 36.dpToPx()

        for (index in 0..6) {
            val bar = weekChart.getChildAt(index) ?: continue
            val fraction = metersPerDay[index].toFloat() / maxMeters
            val barHeight = (fraction * chartHeight)
                .toInt()
                .coerceAtLeast(if (metersPerDay[index] > 0) 3.dpToPx() else 1.dpToPx())

            val layoutParams = bar.layoutParams as LinearLayout.LayoutParams
            layoutParams.height = barHeight
            bar.layoutParams = layoutParams

            val colorRes = when {
                index == todayDow && metersPerDay[index] > 0 -> R.color.rowplus_bar_today
                metersPerDay[index] > 0 -> R.color.rowplus_bar_active
                else -> R.color.rowplus_bar_inactive
            }
            bar.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }
    }

    private fun updateClock() {
        val now = Date()
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val timeFmt = SimpleDateFormat("h:mm a", Locale.US)
        dockClock.text = "${dayFmt.format(now).uppercase()} ${timeFmt.format(now)}"
    }

    private fun startRowSession() {
        val activeProfile = sessionStore.getActiveProfile()
        if (activeProfile == null) {
            Toast.makeText(this, R.string.profile_required, Toast.LENGTH_SHORT).show()
            showProfileEditor(sessionStore.firstEmptySlot() ?: UserProfile.SLOT_ONE, null)
            return
        }

        val intent = Intent(this, SessionActivity::class.java)
            .putExtra(SessionActivity.EXTRA_PROFILE, activeProfile.slotKey)
        startActivity(intent)
    }

    private fun showHistory() {
        val activeProfile = sessionStore.getActiveProfile()
        if (activeProfile == null) {
            Toast.makeText(this, R.string.profile_required, Toast.LENGTH_SHORT).show()
            return
        }

        val sessions = sessionStore.listSessions(activeProfile)
        if (sessions.isEmpty()) {
            Toast.makeText(this, R.string.no_history, Toast.LENGTH_SHORT).show()
            return
        }

        val message = buildString {
            sessions.take(12).forEachIndexed { index, row ->
                if (index > 0) {
                    append("\n\n")
                }
                append(DateFormat.getDateTimeInstance().format(Date(row.startedAtEpochMs)))
                append("\n")
                append(formatDistance(row.distanceMeters))
                append(" in ")
                append(formatDuration(row.durationSeconds))
                append("\n")
                append("Split ")
                append(formatSplitFull(row.avgSplitSeconds))
                append(" | ")
                append(row.avgSpm)
                append(" spm")
                if (row.estimatedCalories > 0) {
                    append("\n")
                    append(getString(R.string.history_calories, row.estimatedCalories))
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_title, activeProfile.displayName))
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

    private fun runEntryAnimations() {
        val leftPanel = findViewById<View>(R.id.leftPanel)
        leftPanel.alpha = 0f
        leftPanel.translationX = -40f
        leftPanel.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(600)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
            .setStartDelay(100)
            .start()

        startRowButton.alpha = 0f
        startRowButton.translationY = 30f
        startRowButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
            .setStartDelay(250)
            .start()

        dockClock.parent?.let { dock ->
            val dockView = dock as View
            dockView.alpha = 0f
            dockView.translationY = 16f
            dockView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                .setStartDelay(400)
                .start()
        }
    }

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
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    private fun computeStreak(sessions: List<RowSession>): Int {
        if (sessions.isEmpty()) {
            return 0
        }

        val cal = Calendar.getInstance()
        val daysWithRows = sessions.map { session ->
            cal.timeInMillis = session.startedAtEpochMs
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

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
