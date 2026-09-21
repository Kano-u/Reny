package io.github.kanou.reny

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.kanou.reny.ui.theme.RenyTheme

private const val SEND_BAR_WIDTH_FRACTION = 0.88f

class InputActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val config = ConfigStore.load(this)

        setContent {
            RenyTheme(themeMode = config.themeMode) {
                SendBar(
                    config = config,
                    onDismiss = ::finish,
                )
            }
        }
    }
}

@Composable
private fun SendBar(
    config: RenyConfig,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val hint = stringResource(R.string.hint_input)
    var text by rememberSaveable { mutableStateOf("") }
    var pendingTermuxText by rememberSaveable { mutableStateOf("") }
    val canSend = text.isNotBlank()

    fun sendToTermux(command: String) {
        if (TermuxRunner.runScript(context, config.termux, command)) {
            text = ""
            onDismiss()
        } else {
            Toast.makeText(context, R.string.termux_send_failed, Toast.LENGTH_SHORT).show()
        }
    }

    val requestRunCommandPermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val command = pendingTermuxText
            pendingTermuxText = ""
            if (granted && command.isNotEmpty()) {
                sendToTermux(command)
            } else if (!granted) {
                Toast
                    .makeText(
                        context,
                        R.string.termux_permission_denied,
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(onDismiss) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
        )

        Row(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(SEND_BAR_WIDTH_FRACTION)
                    .widthIn(max = 560.dp)
                    .height(64.dp)
                    .shadow(elevation = 6.dp, shape = shape, clip = false)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }.padding(start = 24.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        platformImeOptions =
                            PlatformImeOptions(
                                if (config.voiceEnabled) {
                                    voiceImeOptionValue(config.voiceDelayMs)
                                } else {
                                    null
                                },
                            ),
                    ),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            IconButton(
                onClick = {
                    when (config.sendBehavior) {
                        SendBehavior.NONE -> {
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            text = ""
                            onDismiss()
                        }

                        SendBehavior.TERMUX -> {
                            when {
                                !TermuxRunner.isInstalled(context) -> {
                                    Toast
                                        .makeText(
                                            context,
                                            R.string.termux_not_installed,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }

                                !TermuxRunner.hasPermission(context) -> {
                                    pendingTermuxText = text
                                    requestRunCommandPermission.launch(
                                        TermuxRunner.RUN_COMMAND_PERMISSION,
                                    )
                                }

                                else -> {
                                    sendToTermux(text)
                                }
                            }
                        }
                    }
                },
                enabled = canSend,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.action_send),
                    tint =
                        if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                )
            }
        }
    }
}
