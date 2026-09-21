package io.github.kanou.reny

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.kanou.reny.ui.theme.RenyTheme
import io.github.kanou.reny.ui.theme.loadThemeMode

class TermuxSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val themeMode = loadThemeMode(this)
        val initialSettings = loadTermuxSettings(this)

        setContent {
            RenyTheme(themeMode = themeMode) {
                TermuxSettingsScreen(
                    initialSettings = initialSettings,
                    onBack = ::finish,
                    onSave = { settings ->
                        saveTermuxSettings(applicationContext, settings)
                        Toast
                            .makeText(
                                this,
                                R.string.termux_settings_saved,
                                Toast.LENGTH_SHORT,
                            ).show()
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TermuxSettingsScreen(
    initialSettings: TermuxSettings,
    onBack: () -> Unit,
    onSave: (TermuxSettings) -> Unit,
) {
    val context = LocalContext.current
    val requiredMessage = stringResource(R.string.termux_settings_required)
    var commandPath by rememberSaveable { mutableStateOf(initialSettings.commandPath) }
    var argumentsText by rememberSaveable {
        mutableStateOf(initialSettings.arguments.joinToString("\n"))
    }
    var workdir by rememberSaveable { mutableStateOf(initialSettings.workdir) }
    var executionMode by rememberSaveable { mutableStateOf(initialSettings.executionMode) }

    fun save() {
        val normalizedCommandPath = commandPath.trim()
        val normalizedWorkdir = workdir.trim()
        if (normalizedCommandPath.isEmpty() || normalizedWorkdir.isEmpty()) {
            Toast.makeText(context, requiredMessage, Toast.LENGTH_SHORT).show()
            return
        }

        val arguments =
            argumentsText
                .split('\n')
                .filter { it.isNotBlank() }
        onSave(
            TermuxSettings(
                commandPath = normalizedCommandPath,
                arguments = arguments,
                workdir = normalizedWorkdir,
                executionMode = executionMode,
            ),
        )
    }

    BackHandler(onBack = onBack)

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
                    ).padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }

                Text(
                    text = stringResource(R.string.settings_termux),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = ::save) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.action_save),
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp),
            ) {
                OutlinedTextField(
                    value = commandPath,
                    onValueChange = { commandPath = it },
                    label = { Text(stringResource(R.string.termux_settings_command_path)) },
                    placeholder = {
                        Text(stringResource(R.string.termux_settings_command_path_hint))
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.termux_settings_execution_mode),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TermuxExecutionMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = executionMode == mode,
                            onClick = { executionMode = mode },
                            shape =
                                SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = TermuxExecutionMode.entries.size,
                                ),
                            label = {
                                Text(text = stringResource(mode.labelRes))
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = argumentsText,
                    onValueChange = { argumentsText = it },
                    label = { Text(stringResource(R.string.termux_settings_arguments)) },
                    placeholder = {
                        Text(stringResource(R.string.termux_settings_arguments_hint))
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = workdir,
                    onValueChange = { workdir = it },
                    label = { Text(stringResource(R.string.termux_settings_workdir)) },
                    placeholder = {
                        Text(stringResource(R.string.termux_settings_workdir_hint))
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
