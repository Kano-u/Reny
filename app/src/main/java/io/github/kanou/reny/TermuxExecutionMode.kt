package io.github.kanou.reny

import androidx.annotation.StringRes

enum class TermuxExecutionMode {
    BACKGROUND,
    TERMINAL,
    ;

    @get:StringRes
    val labelRes: Int
        get() =
            when (this) {
                BACKGROUND -> R.string.termux_execution_mode_background
                TERMINAL -> R.string.termux_execution_mode_terminal
            }

    companion object {
        fun fromPreference(value: String?): TermuxExecutionMode = entries.firstOrNull { it.name == value } ?: BACKGROUND
    }
}
