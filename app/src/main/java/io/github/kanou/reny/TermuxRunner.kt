package io.github.kanou.reny

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import androidx.core.content.ContextCompat

object TermuxRunner {
    const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"

    private const val TERMUX_PACKAGE_NAME = "com.termux"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_RUNNER = "com.termux.RUN_COMMAND_RUNNER"
    private const val RUNNER_APP_SHELL = "app-shell"
    private const val RUNNER_TERMINAL_SESSION = "terminal-session"

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(
                TERMUX_PACKAGE_NAME,
                PackageInfoFlags.of(0),
            )
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, RUN_COMMAND_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun runScript(
        context: Context,
        config: TermuxConfig,
        argument: String,
    ): Boolean {
        val intent =
            Intent(ACTION_RUN_COMMAND)
                .setClassName(TERMUX_PACKAGE_NAME, RUN_COMMAND_SERVICE)
                .putExtra(EXTRA_COMMAND_PATH, config.commandPath)
                .putExtra(EXTRA_ARGUMENTS, (config.arguments + argument).toTypedArray())
                .putExtra(EXTRA_WORKDIR, config.workdir)
                .putExtra(
                    EXTRA_RUNNER,
                    when (config.executionMode) {
                        TermuxExecutionMode.BACKGROUND -> RUNNER_APP_SHELL
                        TermuxExecutionMode.TERMINAL -> RUNNER_TERMINAL_SESSION
                    },
                )

        return try {
            context.startForegroundService(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
