package io.github.kanou.reny

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.kanou.reny.ui.theme.RenyTheme
import io.github.kanou.reny.ui.theme.ThemeMode

class SettingsActivity : ComponentActivity() {
    private val requestRunCommandPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                ConfigStore.update(applicationContext) { it.copy(sendBehavior = SendBehavior.TERMUX) }
                recreate()
            } else {
                Toast
                    .makeText(
                        this,
                        R.string.termux_permission_denied,
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    private fun exportClipboard() {
        val text = ConfigStore.export(this)
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(ConfigStore.FILE_NAME, text))
        Toast.makeText(this, R.string.settings_export_done, Toast.LENGTH_SHORT).show()
    }

    private fun importClipboard() {
        val text =
            getSystemService(ClipboardManager::class.java)
                .primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
        when {
            text.isNullOrBlank() -> {
                Toast.makeText(this, R.string.settings_clipboard_empty, Toast.LENGTH_SHORT).show()
            }

            ConfigStore.import(this, text) -> {
                Toast.makeText(this, R.string.settings_import_done, Toast.LENGTH_SHORT).show()
                recreate()
            }

            else -> {
                Toast.makeText(this, R.string.settings_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val config = ConfigStore.load(this)
        val initialThemeMode = config.themeMode
        val initialSendBehavior =
            config.sendBehavior.let { behavior ->
                if (behavior == SendBehavior.TERMUX && !TermuxRunner.isInstalled(this)) {
                    ConfigStore.update(this) { it.copy(sendBehavior = SendBehavior.NONE) }
                    SendBehavior.NONE
                } else {
                    behavior
                }
            }

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(initialThemeMode) }
            var sendBehavior by rememberSaveable { mutableStateOf(initialSendBehavior) }

            RenyTheme(themeMode = themeMode) {
                SettingsScreen(
                    themeMode = themeMode,
                    sendBehavior = sendBehavior,
                    onThemeModeChange = { selectedMode ->
                        themeMode = selectedMode
                        ConfigStore.update(applicationContext) { it.copy(themeMode = selectedMode) }
                    },
                    onOpenTermuxSettings = {
                        startActivity(Intent(this, TermuxSettingsActivity::class.java))
                    },
                    onImportClipboard = ::importClipboard,
                    onExportClipboard = ::exportClipboard,
                    onSendBehaviorChange = { selectedBehavior ->
                        when (selectedBehavior) {
                            SendBehavior.NONE -> {
                                sendBehavior = selectedBehavior
                                ConfigStore.update(
                                    applicationContext,
                                ) { it.copy(sendBehavior = selectedBehavior) }
                            }

                            SendBehavior.TERMUX -> {
                                when {
                                    !TermuxRunner.isInstalled(applicationContext) -> {
                                        Toast
                                            .makeText(
                                                this,
                                                R.string.termux_not_installed,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }

                                    TermuxRunner.hasPermission(applicationContext) -> {
                                        sendBehavior = selectedBehavior
                                        ConfigStore.update(
                                            applicationContext,
                                        ) { it.copy(sendBehavior = selectedBehavior) }
                                    }

                                    else -> {
                                        requestRunCommandPermission.launch(
                                            TermuxRunner.RUN_COMMAND_PERMISSION,
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsMenu(
    onImportClipboard: () -> Unit,
    onExportClipboard: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.settings_more_options),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_import_clipboard)) },
                onClick = {
                    expanded = false
                    onImportClipboard()
                },
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_export_clipboard)) },
                onClick = {
                    expanded = false
                    onExportClipboard()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    themeMode: ThemeMode,
    sendBehavior: SendBehavior,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenTermuxSettings: () -> Unit,
    onImportClipboard: () -> Unit,
    onExportClipboard: () -> Unit,
    onSendBehaviorChange: (SendBehavior) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout),
                    ).padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )

                MoreOptionsMenu(
                    onImportClipboard = onImportClipboard,
                    onExportClipboard = onExportClipboard,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                        label = {
                            Text(
                                text =
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> R.string.theme_system
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.DARK -> R.string.theme_dark
                                        },
                                    ),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_send_behavior),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SendBehavior.entries.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = sendBehavior == behavior,
                        onClick = { onSendBehaviorChange(behavior) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = SendBehavior.entries.size,
                            ),
                        label = {
                            Text(
                                text =
                                    stringResource(
                                        when (behavior) {
                                            SendBehavior.NONE -> R.string.send_behavior_none
                                            SendBehavior.TERMUX -> R.string.send_behavior_termux
                                        },
                                    ),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.settings_termux),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onOpenTermuxSettings) {
                Text(stringResource(R.string.settings_termux_configure))
            }
        }
    }
}
