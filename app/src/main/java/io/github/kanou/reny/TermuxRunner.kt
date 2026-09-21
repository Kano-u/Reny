package io.github.kanou.reny

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfoFlags
import android.content.pm.PackageManager
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
    private const val TERMUX_HOME_DIR = "/data/data/com.termux/files/home"
    private const val SCRIPT_PATH = "$TERMUX_HOME_DIR/reny.sh"

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

    fun runScript(context: Context, argument: String): Boolean {
        val intent = Intent(ACTION_RUN_COMMAND)
            .setClassName(TERMUX_PACKAGE_NAME, RUN_COMMAND_SERVICE)
            .putExtra(EXTRA_COMMAND_PATH, SCRIPT_PATH)
            .putExtra(EXTRA_ARGUMENTS, arrayOf(argument))
            .putExtra(EXTRA_WORKDIR, TERMUX_HOME_DIR)
            .putExtra(EXTRA_RUNNER, RUNNER_APP_SHELL)

        return try {
            context.startForegroundService(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
