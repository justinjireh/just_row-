package com.listinglab.rowplus

import android.graphics.drawable.Drawable

data class DockApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isAddTile: Boolean = false,
)
