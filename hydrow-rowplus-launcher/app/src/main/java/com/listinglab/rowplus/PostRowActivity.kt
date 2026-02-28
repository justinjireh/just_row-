package com.listinglab.rowplus

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale

class PostRowActivity : AppCompatActivity() {

    private lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_row)

        sessionStore = SessionStore(this)

        val profileKey = intent.getStringExtra(EXTRA_PROFILE)
        val profile = profileKey?.let(sessionStore::getProfile)
        if (profile == null) {
            Toast.makeText(this, R.string.profile_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val distance = intent.getIntExtra(EXTRA_DISTANCE, 0)
        val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
        val split = intent.getIntExtra(EXTRA_SPLIT, 0)
        val spm = intent.getIntExtra(EXTRA_SPM, 0)
        val calories = intent.getIntExtra(EXTRA_CALORIES, 0)
        val strokes = intent.getIntExtra(EXTRA_STROKES, 0)

        findViewById<TextView>(R.id.summaryTitle).text = "Great session, ${profile.displayName}!"
        findViewById<TextView>(R.id.summaryDistance).text = formatDistance(distance)
        findViewById<TextView>(R.id.summaryDuration).text = formatDuration(duration)
        findViewById<TextView>(R.id.summarySplit).text = formatSplitFull(split)
        findViewById<TextView>(R.id.summarySpm).text = spm.toString()
        findViewById<TextView>(R.id.summaryCalories).text = calories.toString()
        findViewById<TextView>(R.id.summaryStrokes).text = strokes.toString()

        val sessions = sessionStore.listSessions(profile)
        val historicalSessions = sessions.drop(1)
        bindComparison(distance, split, historicalSessions.firstOrNull())
        bindStreakAndGoal(sessions)
        bindPr(distance, spm, split, historicalSessions)

        findViewById<View>(R.id.doneButton).setOnClickListener {
            finish()
        }

        runEntryAnimation()
    }

    private fun bindComparison(distance: Int, split: Int, previous: RowSession?) {
        val comparisonCard = findViewById<View>(R.id.comparisonCard)
        if (previous == null) {
            comparisonCard.visibility = View.GONE
            return
        }

        val deltaDistance = distance - previous.distanceMeters
        val deltaSplit = split - previous.avgSplitSeconds

        val deltaDistanceView = findViewById<TextView>(R.id.deltaDistance)
        val deltaSplitView = findViewById<TextView>(R.id.deltaSplit)

        val distanceKm = deltaDistance / 1000.0
        val distanceSign = if (deltaDistance >= 0) "+" else ""
        deltaDistanceView.text = String.format(Locale.US, "%s%.2f km", distanceSign, distanceKm)
        deltaDistanceView.setTextColor(
            ContextCompat.getColor(
                this,
                if (deltaDistance >= 0) R.color.rowplus_teal else R.color.rowplus_amber_soft,
            ),
        )

        val splitMinutes = kotlin.math.abs(deltaSplit) / 60
        val splitSeconds = kotlin.math.abs(deltaSplit) % 60
        val splitSign = if (deltaSplit <= 0) "-" else "+"
        deltaSplitView.text = String.format(Locale.US, "%s%d:%02d", splitSign, splitMinutes, splitSeconds)
        deltaSplitView.setTextColor(
            ContextCompat.getColor(
                this,
                if (deltaSplit <= 0) R.color.rowplus_teal else R.color.rowplus_amber_soft,
            ),
        )

        comparisonCard.visibility = View.VISIBLE
    }

    private fun bindStreakAndGoal(sessions: List<RowSession>) {
        val streak = computeStreak(sessions)
        findViewById<TextView>(R.id.streakMessage).text = "$streak day streak"

        val weeklyRows = sessionsThisWeek(sessions)
        val goalTarget = 5
        findViewById<TextView>(R.id.weekProgress).text =
            "${weeklyRows.size} of $goalTarget weekly rows complete"
    }

    private fun bindPr(
        distance: Int,
        spm: Int,
        split: Int,
        historicalSessions: List<RowSession>,
    ) {
        val prContainer = findViewById<View>(R.id.prContainer)
        val prLabel = findViewById<TextView>(R.id.prLabel)

        if (historicalSessions.isEmpty()) {
            prContainer.visibility = View.GONE
            return
        }

        val bestDistance = historicalSessions.maxOf { it.distanceMeters }
        val bestSpm = historicalSessions.maxOf { it.avgSpm }
        val bestSplit = historicalSessions.minOf { it.avgSplitSeconds }

        val prName = when {
            distance > bestDistance -> "Best Distance"
            spm > bestSpm -> "Best SPM"
            split < bestSplit -> "Best Split"
            else -> null
        }

        if (prName == null) {
            prContainer.visibility = View.GONE
            return
        }

        prLabel.text = "NEW PR - $prName"
        prContainer.visibility = View.VISIBLE
    }

    private fun runEntryAnimation() {
        val left = findViewById<View>(R.id.summaryTitle).parent as View
        left.alpha = 0f
        left.translationX = -30f
        left.animate().alpha(1f).translationX(0f).setDuration(600L).setStartDelay(100L).start()

        val right = findViewById<View>(R.id.comparisonCard).parent as View
        right.alpha = 0f
        right.translationY = 20f
        right.animate().alpha(1f).translationY(0f).setDuration(600L).setStartDelay(250L).start()
    }

    private fun computeStreak(sessions: List<RowSession>): Int {
        if (sessions.isEmpty()) {
            return 0
        }

        val daysWithRows = sessions.map { session ->
            dayBucket(session.startedAtEpochMs)
        }.toSet().sortedDescending()

        val today = dayBucket(System.currentTimeMillis())
        var expectedDay = when (daysWithRows.firstOrNull()) {
            today -> today
            today - 1 -> today - 1
            else -> return 0
        }

        var streak = 0
        for (day in daysWithRows) {
            if (day == expectedDay) {
                streak++
                expectedDay--
            } else if (day < expectedDay) {
                break
            }
        }
        return streak
    }

    private fun sessionsThisWeek(sessions: List<RowSession>): List<RowSession> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis > System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }
        val start = cal.timeInMillis
        return sessions.filter { it.startedAtEpochMs >= start }
    }

    private fun dayBucket(epochMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86_400_000L
    }

    private fun formatDistance(meters: Int): String {
        return String.format(Locale.US, "%.2f km", meters / 1000.0)
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60L
        val remainder = seconds % 60L
        return String.format(Locale.US, "%d:%02d", minutes, remainder)
    }

    private fun formatSplitFull(splitSeconds: Int): String {
        val minutes = splitSeconds / 60
        val remainder = splitSeconds % 60
        return String.format(Locale.US, "%d:%02d /500m", minutes, remainder)
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_SPLIT = "split"
        const val EXTRA_SPM = "spm"
        const val EXTRA_CALORIES = "calories"
        const val EXTRA_STROKES = "strokes"
    }
}
