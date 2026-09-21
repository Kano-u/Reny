package io.github.kanou.reny

/**
 * 发送栏输入框的私有 IME 标记。
 *
 * [RenyVoiceHook] 运行在豆包输入法进程内，靠这个值区分发送栏与设置页的普通输入框，因此
 * 写入方与读取方共用同一处格式定义。
 *
 * 标记格式为 `io.github.kanou.reny.voice:<延迟毫秒>`，把「是否启用」和「等待多久」一并
 * 捎给输入法进程，避免跨进程读取 `config.json`。
 */
const val VOICE_IME_OPTION = "io.github.kanou.reny.voice"

/** 键盘弹出到触发语音之间的默认等待时间。 */
const val DEFAULT_VOICE_DELAY_MS = 200

/** 生成发送栏使用的标记值。 */
fun voiceImeOptionValue(delayMs: Int): String = "$VOICE_IME_OPTION:$delayMs"

/** 解析标记值中的等待时间，不是本应用的标记时返回 null。 */
fun parseVoiceDelayMs(value: String?): Int? {
    val prefix = "$VOICE_IME_OPTION:"
    return value
        ?.takeIf { it.startsWith(prefix) }
        ?.removePrefix(prefix)
        ?.toIntOrNull()
        ?.takeIf { it >= 0 }
}
