package com.listinglab.rowplus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.json.JSONArray

class DockManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val pm = context.packageManager

    /** Returns pinned installed apps plus the Add App tile at the end. */
    fun getDockApps(): List<DockApp> {
        val pinned = getPinnedPackages()
        val installed = pinned.mapNotNull { pkg ->
            if (isInstalled(pkg)) {
                DockApp(
                    packageName = pkg,
                    label = getAppLabel(pkg),
                    icon = getAppIcon(pkg),
                )
            } else null
        }
        return installed + DockApp(
            packageName = "",
            label = "Add App",
            icon = null,
            isAddTile = true,
        )
    }

    fun getPinnedPackages(): List<String> {
        val raw = prefs.getString(KEY_PINNED_APPS, null)
        if (raw != null) {
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { array.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }
        }
        // First launch: seed with defaults that are installed
        val defaults = DEFAULT_PINS.filter { isInstalled(it) }
        savePinnedPackages(defaults)
        return defaults
    }

    fun savePinnedPackages(packages: List<String>) {
        val array = JSONArray()
        packages.forEach { array.put(it) }
        prefs.edit().putString(KEY_PINNED_APPS, array.toString()).apply()
    }

    fun addPinnedApp(packageName: String) {
        val current = getPinnedPackages().toMutableList()
        if (packageName !in current) {
            current.add(packageName)
            savePinnedPackages(current)
        }
    }

    fun removePinnedApp(packageName: String) {
        val current = getPinnedPackages().toMutableList()
        current.remove(packageName)
        savePinnedPackages(current)
    }

    /** All launchable apps on the device except RowPlus itself. */
    fun getAllLaunchableApps(): List<DockApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val pinned = getPinnedPackages().toSet()
        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                DockApp(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<DockApp> { it.packageName in pinned }
                    .thenBy { it.label.lowercase() },
            )
    }

    fun isInstalled(packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        }
    }

    companion object {
        private const val PREFS_NAME = "rowplus_dock"
        private const val KEY_PINNED_APPS = "pinned_apps"
        val DEFAULT_PINS = listOf(
            "com.spotify.music",
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.hulu.plus",
            "com.android.browser",
        )
    }
}
