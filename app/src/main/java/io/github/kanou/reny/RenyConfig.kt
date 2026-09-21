package io.github.kanou.reny

import android.content.Context
import io.github.kanou.reny.ui.theme.ThemeMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class RenyConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sendBehavior: SendBehavior = SendBehavior.NONE,
    val termux: TermuxConfig = TermuxConfig(),
)

@Serializable
data class TermuxConfig(
    val commandPath: String = "",
    val arguments: List<String> = emptyList(),
    val workdir: String = "",
    val executionMode: TermuxExecutionMode = TermuxExecutionMode.BACKGROUND,
)

object ConfigStore {
    const val FILE_NAME = "config.json"

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    fun load(context: Context): RenyConfig {
        val file = configFile(context)
        if (!file.exists()) return RenyConfig()
        return runCatching { json.decodeFromString<RenyConfig>(file.readText()) }
            .getOrElse { RenyConfig() }
    }

    fun update(
        context: Context,
        transform: (RenyConfig) -> RenyConfig,
    ) {
        configFile(context).writeText(json.encodeToString(transform(load(context))))
    }

    fun export(context: Context): String = json.encodeToString(load(context))

    fun import(
        context: Context,
        text: String,
    ): Boolean =
        runCatching { json.decodeFromString<RenyConfig>(text) }
            .onSuccess { configFile(context).writeText(json.encodeToString(it)) }
            .isSuccess

    private fun configFile(context: Context): File = File(context.filesDir, FILE_NAME)
}
