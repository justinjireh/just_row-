package com.listinglab.rowplus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.listinglab.rowplus.databinding.ActivityMainBinding
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionStore = SessionStore(this)

        binding.appTitle.setOnLongClickListener {
            showAdminActions()
            true
        }

        binding.primaryProfileButton.setOnClickListener { switchProfile(UserProfile.PRIMARY) }
        binding.spouseProfileButton.setOnClickListener { switchProfile(UserProfile.SPOUSE) }
        binding.startRowButton.setOnClickListener { startRowSession() }
        binding.historyButton.setOnClickListener { showHistory() }
        binding.browserButton.setOnClickListener { openBrowser() }
        binding.spotifyButton.setOnClickListener {
            launchPackage("com.spotify.music", getString(R.string.spotify_missing))
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun switchProfile(profile: UserProfile) {
        sessionStore.setActiveProfile(profile)
        refreshUi()
    }

    private fun refreshUi() {
        val active = sessionStore.getActiveProfile()
        binding.activeProfileValue.text = active.displayName
        binding.profileHint.text = getString(R.string.profile_hint, active.displayName)

        val sessions = sessionStore.listSessions(active)
        binding.historySummary.text = if (sessions.isEmpty()) {
            getString(R.string.no_history)
        } else {
            val latest = sessions.first()
            getString(
                R.string.history_summary,
                sessions.size,
                formatDistance(latest.distanceMeters),
                formatDuration(latest.durationSeconds),
            )
        }
    }

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
                append(formatSplit(row.avgSplitSeconds))
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
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        try {
            startActivity(intent)
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

    private fun formatDistance(distanceMeters: Int): String {
        val km = distanceMeters / 1000.0
        return String.format(Locale.US, "%.2f km", km)
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
}

