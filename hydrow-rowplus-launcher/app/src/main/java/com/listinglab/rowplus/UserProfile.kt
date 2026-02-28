package com.listinglab.rowplus

data class UserProfile(
    val slotKey: String,
    val displayName: String,
    val heightInches: Int,
    val weightLbs: Int,
) {
    companion object {
        const val SLOT_ONE = "slot_one"
        const val SLOT_TWO = "slot_two"

        val SLOT_KEYS = listOf(SLOT_ONE, SLOT_TWO)
    }
}
