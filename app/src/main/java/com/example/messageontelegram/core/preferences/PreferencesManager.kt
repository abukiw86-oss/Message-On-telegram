package com.example.messageontelegram.core.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "sms_relay_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_TG_USERNAME = "tg_username"
        private const val KEY_TG_FIRST_NAME = "tg_first_name"
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
        private const val KEY_BOT_USERNAME = "bot_username"

        const val DEFAULT_SERVER_URL = "https://abubeker.alwaysdata.net"
        const val DEFAULT_BOT_USERNAME = "dsjfbsbfkbot"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim().removeSuffix("/")).apply()

    var deviceToken: String?
        get() = prefs.getString(KEY_DEVICE_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_TOKEN, value).apply()

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, defaultDeviceModel()) ?: defaultDeviceModel()
        set(value) = prefs.edit().putString(KEY_DEVICE_NAME, value).apply()

    var telegramUsername: String?
        get() = prefs.getString(KEY_TG_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_TG_USERNAME, value).apply()

    var telegramFirstName: String?
        get() = prefs.getString(KEY_TG_FIRST_NAME, null)
        set(value) = prefs.edit().putString(KEY_TG_FIRST_NAME, value).apply()

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var botUsername: String
        get() = prefs.getString(KEY_BOT_USERNAME, DEFAULT_BOT_USERNAME) ?: DEFAULT_BOT_USERNAME
        set(value) = prefs.edit().putString(KEY_BOT_USERNAME, value.trim().removePrefix("@")).apply()

    val isPaired: Boolean
        get() = !deviceToken.isNullOrBlank()

    fun savePairing(token: String, firstName: String?, username: String?) {
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, token)
            .putString(KEY_TG_FIRST_NAME, firstName)
            .putString(KEY_TG_USERNAME, username)
            .putBoolean(KEY_FORWARDING_ENABLED, true)
            .apply()
    }

    fun clearPairing() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_TG_FIRST_NAME)
            .remove(KEY_TG_USERNAME)
            .apply()
    }

    private fun defaultDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
}
