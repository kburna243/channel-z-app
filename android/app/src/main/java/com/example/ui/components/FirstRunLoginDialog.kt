package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WebQueueOtpState
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtitleWhite
import kotlinx.coroutines.delay

@Composable
fun FirstRunLoginDialog(
    isOpen: Boolean,
    otpState: WebQueueOtpState,
    initialUsername: String = "",
    onStartMagicLogin: (username: String, password: String) -> Unit,
    onVerifyManualOtp: (username: String, code: String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var manualCode by remember { mutableStateOf("") }
    var showManualCodeInput by remember { mutableStateOf(false) }

    val usernameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isOpen) {
        if (isOpen && initialUsername.isBlank()) {
            delay(200)
            try { usernameFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(otpState) {
        if (otpState is WebQueueOtpState.Success) {
            delay(1500)
            onDismiss()
        }
    }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            if (otpState !is WebQueueOtpState.RequestingOtp && otpState !is WebQueueOtpState.Verifying) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .imePadding()
                .testTag("first_run_login_dialog"),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(16.dp)),
                color = SurfaceDark.copy(alpha = 0.98f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AccentPurple.copy(alpha = 0.6f)),
                shadowElevation = 32.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Logo & Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🟩🟨🟧🟥 CHANNEL-Z 🟥🟧🟨🟩",
                            style = TextStyle(
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "WebQueue & CyTube Connect",
                        style = TextStyle(
                            color = AccentLavender,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Connect your account once to unlock real-time EPG schedules, upcoming movie marathons, and the interactive queue.",
                        style = TextStyle(
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val state = otpState) {
                        is WebQueueOtpState.Idle, is WebQueueOtpState.Failed -> {
                            if (state is WebQueueOtpState.Failed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AccentVibrantOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, AccentVibrantOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = state.error,
                                        style = TextStyle(color = AccentVibrantOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Username input
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("CyTube Username (Case-Sensitive)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AccentLavender)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(usernameFocusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = SubtleBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedLabelColor = AccentLavender,
                                    unfocusedLabelColor = TextMuted,
                                    cursorColor = AccentIceBlue
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Optional Password input
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password (Optional - leave empty for Guest)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentLavender)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = SubtleBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedLabelColor = AccentLavender,
                                    unfocusedLabelColor = TextMuted,
                                    cursorColor = AccentIceBlue
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (username.isNotBlank()) {
                                            onStartMagicLogin(username, password)
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSkip,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SubtleBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSubtitleWhite)
                                ) {
                                    Text("Skip / Guest", fontSize = 14.sp)
                                }

                                Button(
                                    onClick = { onStartMagicLogin(username, password) },
                                    enabled = username.isNotBlank(),
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentPurple,
                                        contentColor = PureWhite,
                                        disabledContainerColor = AccentPurple.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect ✨", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        is WebQueueOtpState.RequestingOtp,
                        is WebQueueOtpState.WaitingForCode,
                        is WebQueueOtpState.Verifying -> {
                            val stepText = when (state) {
                                is WebQueueOtpState.RequestingOtp -> "1/3: Requesting authorization code from Kryten…"
                                is WebQueueOtpState.WaitingForCode -> "2/3: Waiting for PM code… (Magic auto-handshake active)"
                                is WebQueueOtpState.Verifying -> "3/3: Code received! Verifying WebQueue session…"
                                else -> "Connecting…"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = AccentIceBlue,
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.5.dp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = stepText,
                                    style = TextStyle(
                                        color = AccentLavender,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Kryten hat dir einen 6-stelligen Code per PM im CyTube-Chat geschickt. Der Code wird automatisch erkannt – oder du kannst ihn hier direkt eingeben:",
                                    style = TextStyle(
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Always-visible 6-digit code entry with auto-submit
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = manualCode,
                                        onValueChange = { input ->
                                            val digitsOnly = input.filter { it.isDigit() }.take(6)
                                            manualCode = digitsOnly
                                            if (digitsOnly.length == 6) {
                                                onVerifyManualOtp(username, digitsOnly)
                                            }
                                        },
                                        placeholder = { Text("6-stelliger Code (z.B. 123456)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                if (manualCode.length == 6) {
                                                    onVerifyManualOtp(username, manualCode)
                                                }
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentPurple,
                                            unfocusedBorderColor = SubtleBorder,
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = PureWhite,
                                            cursorColor = AccentIceBlue
                                        )
                                    )

                                    Button(
                                        onClick = { onVerifyManualOtp(username, manualCode) },
                                        enabled = manualCode.length == 6,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPurple,
                                            disabledContainerColor = AccentPurple.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Text("Verify", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = onSkip,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Text("Skip / Watch as Guest", fontSize = 12.sp, color = TextSubtitleWhite)
                                }
                            }
                        }

                        is WebQueueOtpState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF3DDC84),
                                    modifier = Modifier.size(54.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Connected as ${state.username}!",
                                    style = TextStyle(
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "✨ WebQueue session saved. Real-time EPG and queue are now live.",
                                    style = TextStyle(
                                        color = AccentLavender,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                                ) {
                                    Text("Let's Watch 🍿", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
