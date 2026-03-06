package com.junkfood.seal.util

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.junkfood.seal.R
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.updateBoolean

/**
 * Detects aggressive OEM battery managers (MIUI, ColorOS, FuntouchOS, One UI, etc.) and
 * routes the user directly to that manufacturer's hidden "Auto-Start" / "Background Execution"
 * settings page so they can whitelist the app.
 *
 * ## Why this is necessary
 * Many OEM Android forks treat a swipe-to-dismiss in the Recents list as a **Force Stop**,
 * which cancels all pending [android.app.AlarmManager] alarms.  Programmatic code cannot
 * override an OS-level Force Stop — the only reliable workaround is to ask the user to
 * grant the app auto-start / background launch permission through the OEM's own UI.
 *
 * ## Usage
 * Call [showAutoStartDialogIfNeeded] exactly once, when the user first activates the
 * *Schedule Download* toggle.  The dialog is suppressed on AOSP devices (where no such
 * restriction exists) and shown **at most once** thanks to an MMKV-backed flag.
 */
object AutoStartHelper {

    private const val TAG = "AutoStartHelper"

    // ── Manufacturer identifiers (compared in lowercase) ──────────────────────────────

    private const val XIAOMI  = "xiaomi"
    private const val REDMI   = "redmi"
    private const val POCO    = "poco"
    private const val OPPO    = "oppo"
    private const val REALME  = "realme"
    private const val ONEPLUS = "oneplus"
    private const val VIVO    = "vivo"
    private const val IQOO    = "iqoo"
    private const val SAMSUNG = "samsung"
    private const val LETV    = "letv"
    private const val HONOR   = "honor"
    private const val HUAWEI  = "huawei"

    // ── Public API ─────────────────────────────────────────────────────────────────────

    /**
     * Returns an [Intent] aimed at the OEM-specific auto-start / background-launch
     * management screen, or `null` on stock Android builds where no such screen exists.
     *
     * The intent is built with an explicit [ComponentName] pointing to the known
     * Activity inside each manufacturer's security / system-manager app.  Callers
     * should wrap [Context.startActivity] in a `try-catch` for [ActivityNotFoundException]
     * because the exact Activity path may vary across minor OEM OS versions.
     */
    fun getAutoStartIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {

            // ── Xiaomi / Redmi / Poco (MIUI → HyperOS) ─────────────────────────────
            manufacturer.containsAny(XIAOMI, REDMI, POCO) -> Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── Oppo / Realme (ColorOS) ─────────────────────────────────────────────
            manufacturer.containsAny(OPPO, REALME) -> Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── Vivo / iQOO (FuntouchOS / OriginOS) ────────────────────────────────
            manufacturer.containsAny(VIVO, IQOO) -> Intent().apply {
                component = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── OnePlus (OxygenOS / ColorOS) ────────────────────────────────────────
            manufacturer.containsAny(ONEPLUS) -> Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── Samsung (One UI) ────────────────────────────────────────────────────
            // The AutoRunActivity lives in the China-market build (sm_cn).
            // Global One UI devices fall back to standard battery settings instead
            // (handled in startAutoStartActivity below).
            manufacturer.containsAny(SAMSUNG) -> Intent().apply {
                component = ComponentName(
                    "com.samsung.android.sm_cn",
                    "com.samsung.android.sm.ui.ram.AutoRunActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── LeTV / LeEco ────────────────────────────────────────────────────────
            manufacturer.containsAny(LETV) -> Intent().apply {
                component = ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── Honor / Huawei (EMUI / MagicUI) ────────────────────────────────────
            manufacturer.containsAny(HONOR, HUAWEI) -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // ── Stock Android / unknown OEM — no auto-start screen needed ───────────
            else -> null
        }
    }

    /**
     * Shows a Material [AlertDialog] explaining why auto-start permission helps, then
     * navigates the user to the OEM auto-start settings on confirmation.
     *
     * - **No-op** on stock Android (when [getAutoStartIntent] returns `null`).
     * - **One-time only**: after the user taps *Grant Permission* **or** *Not Now*, the
     *   flag [AUTOSTART_PROMPT_SHOWN] is written to MMKV so the dialog never reappears.
     *
     * @param context An Activity [Context] (required by [AlertDialog.Builder]).
     *                Pass `LocalContext.current` from a Compose call-site.
     */
    fun showAutoStartDialogIfNeeded(context: Context) {
        // Only relevant on OEMs that ship an auto-start restriction screen
        val intent = getAutoStartIntent(context) ?: return

        // Show at most once — survives process restarts via MMKV
        if (AUTOSTART_PROMPT_SHOWN.getBoolean()) return

        AlertDialog.Builder(context)
            .setTitle(R.string.autostart_dialog_title)
            .setMessage(R.string.autostart_dialog_message)
            .setPositiveButton(R.string.autostart_grant_permission) { _, _ ->
                markShown()
                startAutoStartActivity(context, intent)
            }
            .setNegativeButton(R.string.autostart_not_now) { _, _ ->
                // User dismissed — don't nag again
                markShown()
            }
            .setCancelable(false) // force an explicit choice
            .show()
    }

    // ── Private helpers ────────────────────────────────────────────────────────────────

    /**
     * Attempts to start [oemIntent].  If the OEM Activity is not present on this
     * particular device (e.g. a Samsung global build without sm_cn), falls back to
     * the standard *App Info* screen where the user can still manage battery/permissions.
     */
    private fun startAutoStartActivity(context: Context, oemIntent: Intent) {
        try {
            context.startActivity(oemIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "OEM auto-start screen not resolvable — falling back to App Info", e)
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (e2: ActivityNotFoundException) {
                Log.e(TAG, "Even App Info screen is unavailable", e2)
            }
        }
    }

    /** Persists the "already prompted" flag so the dialog is never shown again. */
    private fun markShown() = AUTOSTART_PROMPT_SHOWN.updateBoolean(true)

    /** True if the string contains *any* of the given [tokens]. */
    private fun String.containsAny(vararg tokens: String): Boolean =
        tokens.any { this.contains(it) }
}
