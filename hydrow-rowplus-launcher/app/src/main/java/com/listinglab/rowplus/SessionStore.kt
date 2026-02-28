package com.listinglab.rowplus

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("rowplus_store", Context.MODE_PRIVATE)

    fun getActiveProfile(): UserProfile? {
        val activeSlotKey = prefs.getString(KEY_ACTIVE_PROFILE_SLOT, null)
        val active = activeSlotKey?.let(::getProfile)
        if (active != null) {
            return active
        }
        return enabledSlotKeys().firstNotNullOfOrNull(::getProfile)
    }

    fun setActiveProfile(profile: UserProfile) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_SLOT, profile.slotKey).apply()
    }

    fun isSecondProfileEnabled(): Boolean {
        return prefs.getBoolean(KEY_SECOND_PROFILE_ENABLED, true)
    }

    fun setSecondProfileEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECOND_PROFILE_ENABLED, enabled).apply()

        if (!enabled && getActiveProfile()?.slotKey == UserProfile.SLOT_TWO) {
            getProfile(UserProfile.SLOT_ONE)?.let(::setActiveProfile)
        }
    }

    fun listSelectableProfiles(): List<UserProfile> {
        return enabledSlotKeys().mapNotNull(::getProfile)
    }

    fun getProfile(slotKey: String): UserProfile? {
        val raw = prefs.getString(profileKey(slotKey), null) ?: return null
        return runCatching {
            val item = JSONObject(raw)
            UserProfile(
                slotKey = slotKey,
                displayName = item.optString("displayName").trim(),
                heightInches = item.optInt("heightInches"),
                weightLbs = item.optInt("weightLbs"),
            )
        }.getOrNull()?.takeIf { profile ->
            profile.displayName.isNotBlank() && profile.heightInches > 0 && profile.weightLbs > 0
        }
    }

    fun saveProfile(profile: UserProfile) {
        val payload = JSONObject()
            .put("displayName", profile.displayName)
            .put("heightInches", profile.heightInches)
            .put("weightLbs", profile.weightLbs)

        prefs.edit().putString(profileKey(profile.slotKey), payload.toString()).apply()

        if (getActiveProfile() == null) {
            setActiveProfile(profile)
        }
    }

    fun firstEmptySlot(): String? = enabledSlotKeys().firstOrNull { getProfile(it) == null }

    fun listSessions(profile: UserProfile): List<RowSession> {
        val raw = prefs.getString(historyKey(profile.slotKey), "[]") ?: "[]"
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
                estimatedCalories = item.optInt("estimatedCalories", 0),
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
                    .put("avgSpm", row.avgSpm)
                    .put("estimatedCalories", row.estimatedCalories),
            )
        }

        prefs.edit().putString(historyKey(profile.slotKey), array.toString()).apply()
    }

    private fun profileKey(slotKey: String): String = "profile_$slotKey"

    private fun historyKey(slotKey: String): String = "sessions_$slotKey"

    private fun enabledSlotKeys(): List<String> {
        return if (isSecondProfileEnabled()) {
            UserProfile.SLOT_KEYS
        } else {
            listOf(UserProfile.SLOT_ONE)
        }
    }

    companion object {
        private const val KEY_ACTIVE_PROFILE_SLOT = "active_profile_slot"
        private const val KEY_SECOND_PROFILE_ENABLED = "second_profile_enabled"
        private const val MAX_SESSIONS = 200
    }
}
