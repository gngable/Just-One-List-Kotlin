package com.mercangelsoftware.JustOneList.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, true))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_KEEP_SCREEN_ON, enabled) }
        _keepScreenOn.value = enabled
    }

    private companion object {
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }
}
