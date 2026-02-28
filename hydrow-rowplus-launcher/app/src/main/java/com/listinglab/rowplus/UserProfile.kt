package com.listinglab.rowplus

enum class UserProfile(val storageKey: String, val displayName: String) {
    PRIMARY("primary", "You"),
    SPOUSE("spouse", "Wife");

    companion object {
        fun fromStorageKey(value: String?): UserProfile {
            return entries.firstOrNull { it.storageKey == value } ?: PRIMARY
        }
    }
}

