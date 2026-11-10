package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DataKopiProfile
import com.example.data.model.KopiFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DataKopiStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kopi_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "kopi_user_id"
        private const val KEY_EMAIL = "kopi_email"
        private const val KEY_KOPI_ID = "kopi_id"
        private const val KEY_COOKIE_REPLACE = "kopi_cookie_replace_active"
        private const val KEY_LAST_SYNC = "kopi_last_sync_time"
        private const val KEY_SYNC_CODE = "kopi_sync_code"
        private const val KEY_AUTO_SYNC = "kopi_auto_sync"

        val DEFAULT_FLAGS = listOf(
            KopiFlag(
                id = "adblock-privacy-shield",
                name = "Kopi Privacy Shield & AdBlock",
                description = "Blocks intrusive ads, trackers, analytics, and third-party tracking scripts natively.",
                isEnabled = true,
                category = "Privacy & Security"
            ),
            KopiFlag(
                id = "dark-mode-web-contents",
                name = "Force Dark Mode for Web Contents",
                description = "Automatically renders all web pages using high-contrast dark theme styling.",
                isEnabled = false,
                category = "Appearance"
            ),
            KopiFlag(
                id = "desktop-user-agent",
                name = "Desktop Chrome User-Agent",
                description = "Requests desktop versions of websites instead of mobile layouts.",
                isEnabled = false,
                category = "Browsing Engine"
            ),
            KopiFlag(
                id = "datakopi-strict-cookie-block",
                name = "Strict Data Kopi Mode",
                description = "Replaces traditional cookie tracking with isolated Data Kopi three-key container.",
                isEnabled = true,
                category = "Data Kopi"
            ),
            KopiFlag(
                id = "hardware-acceleration",
                name = "Chromium Hardware Acceleration",
                description = "Accelerates 2D canvas and web rendering using device GPU pipeline.",
                isEnabled = true,
                category = "Performance"
            ),
            KopiFlag(
                id = "smooth-scrolling",
                name = "Smooth Inertial Scrolling",
                description = "Enables Chromium momentum scrolling algorithm for web contents.",
                isEnabled = true,
                category = "Performance"
            ),
            KopiFlag(
                id = "dev-console",
                name = "Developer Console Overlay",
                description = "Displays an in-browser floating JavaScript console for inspecting web logs.",
                isEnabled = false,
                category = "Developer Tools"
            )
        )
    }

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<DataKopiProfile> = _profile.asStateFlow()

    private val _flags = MutableStateFlow(loadFlags())
    val flags: StateFlow<List<KopiFlag>> = _flags.asStateFlow()

    private fun loadProfile(): DataKopiProfile {
        val existingUserId = prefs.getString(KEY_USER_ID, null)
        val userId = existingUserId ?: "usr_" + UUID.randomUUID().toString().take(8)
        val email = prefs.getString(KEY_EMAIL, "demoda228@gmail.com") ?: "demoda228@gmail.com"
        val existingKopiId = prefs.getString(KEY_KOPI_ID, null)
        val kopiId = existingKopiId ?: "KOPI-" + (1000..9999).random() + "-CHROMIUM"
        val cookieReplace = prefs.getBoolean(KEY_COOKIE_REPLACE, true)

        if (existingUserId == null || existingKopiId == null) {
            prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_KOPI_ID, kopiId)
                .putBoolean(KEY_COOKIE_REPLACE, cookieReplace)
                .apply()
        }

        return DataKopiProfile(
            userId = userId,
            email = email,
            kopiId = kopiId,
            isCookieReplacementActive = cookieReplace
        )
    }

    fun updateProfile(userId: String, email: String, kopiId: String, cookieReplace: Boolean = true) {
        prefs.edit()
            .putString(KEY_USER_ID, userId.trim())
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_KOPI_ID, kopiId.trim())
            .putBoolean(KEY_COOKIE_REPLACE, cookieReplace)
            .apply()
        _profile.value = DataKopiProfile(userId.trim(), email.trim(), kopiId.trim(), cookieReplace)
    }

    private fun loadFlags(): List<KopiFlag> {
        return DEFAULT_FLAGS.map { flag ->
            val isEnabled = prefs.getBoolean("flag_${flag.id}", flag.isEnabled)
            flag.copy(isEnabled = isEnabled)
        }
    }

    fun toggleFlag(flagId: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("flag_$flagId", isEnabled).apply()
        _flags.value = _flags.value.map {
            if (it.id == flagId) it.copy(isEnabled = isEnabled) else it
        }
    }

    fun resetFlags() {
        val editor = prefs.edit()
        DEFAULT_FLAGS.forEach {
            editor.remove("flag_${it.id}")
        }
        editor.apply()
        _flags.value = DEFAULT_FLAGS
    }

    fun isFlagEnabled(flagId: String): Boolean {
        return _flags.value.find { it.id == flagId }?.isEnabled ?: false
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun updateLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    fun getSyncCode(): String {
        var code = prefs.getString(KEY_SYNC_CODE, null)
        if (code == null) {
            code = "KP-" + (100000..999999).random()
            prefs.edit().putString(KEY_SYNC_CODE, code).apply()
        }
        return code
    }

    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SYNC, true)

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }
}
