package io.github.kanou.reny

import android.content.Context

data class TermuxSettings(
    val commandPath: String = "",
    val arguments: List<String> = emptyList(),
    val workdir: String = "",
)

private const val PREFERENCES_NAME = "settings"
private const val COMMAND_PATH_KEY = "termux_command_path"
private const val ARGUMENTS_KEY = "termux_arguments"
private const val WORKDIR_KEY = "termux_workdir"

fun loadTermuxSettings(context: Context): TermuxSettings {
    val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    return TermuxSettings(
        commandPath = preferences.getString(COMMAND_PATH_KEY, "").orEmpty(),
        arguments =
            preferences
                .getString(ARGUMENTS_KEY, "")
                .orEmpty()
                .split('\n')
                .filter { it.isNotBlank() },
        workdir = preferences.getString(WORKDIR_KEY, "").orEmpty(),
    )
}

fun saveTermuxSettings(
    context: Context,
    settings: TermuxSettings,
) {
    context
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(COMMAND_PATH_KEY, settings.commandPath)
        .putString(ARGUMENTS_KEY, settings.arguments.joinToString("\n"))
        .putString(WORKDIR_KEY, settings.workdir)
        .apply()
}
