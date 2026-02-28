package com.listinglab.rowplus

data class RowSession(
    val startedAtEpochMs: Long,
    val durationSeconds: Long,
    val distanceMeters: Int,
    val avgSplitSeconds: Int,
    val avgSpm: Int,
)

