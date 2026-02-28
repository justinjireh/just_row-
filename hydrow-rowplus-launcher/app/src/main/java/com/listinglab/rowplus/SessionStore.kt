package com.listinglab.rowplus

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("rowplus_store", Context.MODE_PRIVATE)

    fun getActiveProfile(): UserProfile {
        return UserProfile.fromStorageKey(prefs.getString(KEY_ACTIVE_PROFILE, null))
    }

    fun setActiveProfile(profile: UserProfile) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profile.storageKey).apply()
    }

    fun listSessions(profile: UserProfile): List<RowSession> {
        val raw = prefs.getString(historyKey(profile), "[]") ?: "[]"
        val array = JSONArray(raw)
        val sessions = mutableListOf<RowSession>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            sessions += RowSession(
                startedAtEpochMs = item.getLong("startedAtEpochMs"),
                durationSeconds = item.getLong("durationSeconds"),
                distanceMeters = item.getInt("distanceMeters"),
                avgSplitSeconds = item.getInt("avgSplitSeconds"),
                avgSpm = item.getInt("avgSpm"),
            )
        }
        return sessions.sortedByDescending { it.startedAtEpochMs }
    }

    fun saveSession(profile: UserProfile, session: RowSession) {
        val updated = listSessions(profile).toMutableList()
        updated += session
        updated.sortByDescending { it.startedAtEpochMs }

        val array = JSONArray()
        updated.take(MAX_SESSIONS).forEach { row ->
            array.put(
                JSONObject()
                    .put("startedAtEpochMs", row.startedAtEpochMs)
                    .put("durationSeconds", row.durationSeconds)
                    .put("distanceMeters", row.distanceMeters)
                    .put("avgSplitSeconds", row.avgSplitSeconds)
                    .put("avgSpm", row.avgSpm),
            )
        }

        prefs.edit().putString(historyKey(profile), array.toString()).apply()
    }

    private fun historyKey(profile: UserProfile): String = "sessions_${profile.storageKey}"

    companion object {
        private const val KEY_ACTIVE_PROFILE = "active_profile"
        private const val MAX_SESSIONS = 200
    }
}
