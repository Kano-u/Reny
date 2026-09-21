package io.github.kanou.reny

enum class TermuxExecutionMode {
    BACKGROUND,
    TERMINAL,
    ;

    companion object {
        fun fromPreference(value: String?): TermuxExecutionMode =
            entries.firstOrNull { it.name == value } ?: BACKGROUND
    }
}
