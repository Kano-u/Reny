package io.github.kanou.reny.ui.theme

import android.content.Context

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromPreference(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

private const val PREFERENCES_NAME = "settings"
private const val THEME_MODE_KEY = "theme_mode"

fun loadThemeMode(context: Context): ThemeMode {
    val value =
        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(THEME_MODE_KEY, null)
    return ThemeMode.fromPreference(value)
}

fun saveThemeMode(
    context: Context,
    mode: ThemeMode,
) {
    context
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(THEME_MODE_KEY, mode.name)
        .apply()
}
