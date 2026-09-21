package io.github.kanou.reny

/**
 * 发送栏输入框的私有 IME 标记。
 *
 * [RenyVoiceHook] 运行在豆包输入法进程内，靠这个值区分发送栏与设置页的普通输入框，因此
 * 写入方与读取方共用同一处字面量。
 */
const val VOICE_IME_OPTION = "io.github.kanou.reny.voice"
