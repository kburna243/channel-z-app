package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import com.example.ui.theme.CardFocusedSurface

@Composable
fun ExitConfirmationDialog(
    isOpen: Boolean,
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val cancelFocusRequester = remember { FocusRequester() }

    // Ausserhalb des Dialogs gelesen, drinnen wieder bereitgestellt — siehe Kommentar unten.
    val localizedContext = LocalContext.current
    val localizedConfig = LocalConfiguration.current

    // Auto-focus on Dismiss button by default so accidental exit is prevented
    LaunchedEffect(Unit) {
        delay(120)
        try {
            cancelFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus error
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = true
        )
    ) {
        // Der Dialog rendert in einem eigenen Fenster und setzt LocalContext/LocalConfiguration
        // dabei auf den Activity-Context zurueck. Ohne diese Zeilen ignoriert er die in den
        // Einstellungen gewaehlte Sprache und zeigt immer die Systemsprache.
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .testTag("exit_dialog"),
            color = SurfaceDark,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SubtleBorder),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.exit_dialog_title),
                    style = TextStyle(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.exit_dialog_msg),
                    style = TextStyle(
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Cancel / Return Button (Neutral Gray when unfocused, Purple when focused)
                    var isCancelFocused by remember { mutableStateOf(false) }
                    val cancelScale by animateFloatAsState(targetValue = if (isCancelFocused) 1.05f else 1.0f, label = "cancelScale")

                    Box(
                        modifier = Modifier
                            .scale(cancelScale)
                            .focusRequester(cancelFocusRequester)
                            .onFocusChanged { isCancelFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCancelFocused) AccentPurple else CardFocusedSurface)
                            .border(
                                width = if (isCancelFocused) 2.dp else 1.dp,
                                color = if (isCancelFocused) AccentPurple else SubtleBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() }
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                            .testTag("cancel_exit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = TextStyle(
                                color = PureWhite,
                                fontWeight = if (isCancelFocused) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // 2. Confirm Exit Button (Neutral Gray when unfocused, Purple when focused)
                    var isExitFocused by remember { mutableStateOf(false) }
                    val exitScale by animateFloatAsState(targetValue = if (isExitFocused) 1.05f else 1.0f, label = "exitScale")

                    Box(
                        modifier = Modifier
                            .scale(exitScale)
                            .onFocusChanged { isExitFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExitFocused) AccentPurple else CardFocusedSurface)
                            .border(
                                width = if (isExitFocused) 2.dp else 1.dp,
                                color = if (isExitFocused) AccentPurple else SubtleBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onConfirmExit() }
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                            .testTag("confirm_exit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_exit),
                            style = TextStyle(
                                color = PureWhite,
                                fontWeight = if (isExitFocused) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
        }
    }
}
