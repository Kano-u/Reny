package io.github.kanou.reny

import android.content.Context

enum class SendBehavior {
    NONE,
    TERMUX,
    ;

    companion object {
        fun fromPreference(value: String?): SendBehavior = entries.firstOrNull { it.name == value } ?: NONE
    }
}

private const val PREFERENCES_NAME = "settings"
private const val SEND_BEHAVIOR_KEY = "send_behavior"

fun loadSendBehavior(context: Context): SendBehavior {
    val value =
        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(SEND_BEHAVIOR_KEY, null)
    return SendBehavior.fromPreference(value)
}

fun saveSendBehavior(
    context: Context,
    behavior: SendBehavior,
) {
    context
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(SEND_BEHAVIOR_KEY, behavior.name)
        .apply()
}
