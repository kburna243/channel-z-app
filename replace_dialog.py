import re

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "r") as f:
    content = f.read()

# WebQueueOtpState -> WebQueueLinkState
content = content.replace("WebQueueOtpState", "WebQueueLinkState")
content = content.replace("otpState", "linkState")

# onStartMagicLogin -> onVerifyDeviceCode
# we actually replace the signature of FirstRunLoginDialog
search_signature = """@Composable
fun FirstRunLoginDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onStartMagicLogin: (String, String) -> Unit,
    onVerifyManualOtp: (String, String) -> Unit,
    linkState: WebQueueLinkState,
    initialUsername: String = ""
) {"""

replace_signature = """@Composable
fun FirstRunLoginDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onVerifyDeviceCode: (String) -> Unit,
    linkState: WebQueueLinkState
) {"""

content = content.replace(search_signature, replace_signature)

# We have to rewrite the content of the dialogue heavily anyway.
search_dialog_body = """                    when (val state = linkState) {
                        is WebQueueLinkState.Idle, is WebQueueLinkState.Failed -> {
                            if (state is WebQueueLinkState.Failed) {
                                Text(
                                    text = state.error,
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Text(
                                text = "Mit einem CyTube-Account kannst du im TV-Modus im Chat mitlesen, Umfragen sehen und deinen WebQueue-Status synchronisieren.",
                                style = TextStyle(
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("CyTube Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentIceBlue,
                                    unfocusedBorderColor = SubtleBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password (optional)") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
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
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentIceBlue,
                                    unfocusedBorderColor = SubtleBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Tipp: Für Grindhouse reicht der reine Username völlig aus. Ein Passwort wird nur für registrierte Nicknames benötigt.",
                                style = TextStyle(
                                    color = AccentLavender,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSkip,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Text("Später", color = TextSubtitleWhite)
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

                        is WebQueueLinkState.RequestingLink,
                        is WebQueueLinkState.WaitingForCode,
                        is WebQueueLinkState.Verifying -> {
                            val stepText = when (state) {
                                is WebQueueLinkState.RequestingLink -> "1/3: Requesting authorization code from Kryten…"
                                is WebQueueLinkState.WaitingForCode -> "2/3: Waiting for PM code… (Magic auto-handshake active)"
                                is WebQueueLinkState.Verifying -> "3/3: Code received! Verifying WebQueue session…"
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

                        is WebQueueLinkState.Success -> {"""

replace_dialog_body = """                    when (val state = linkState) {
                        is WebQueueLinkState.Idle, is WebQueueLinkState.Failed -> {
                            if (state is WebQueueLinkState.Failed) {
                                Text(
                                    text = state.error,
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Text(
                                text = "1. Go to https://queue.dropsugar.co/link in your browser.",
                                style = TextStyle(color = PureWhite, fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "2. Fill in a device name to identify your device.",
                                style = TextStyle(color = PureWhite, fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "3. Enter the 5-character code below to connect your device.",
                                style = TextStyle(color = PureWhite, fontSize = 14.sp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = manualCode,
                                onValueChange = { input ->
                                    val formatted = input.take(5).uppercase()
                                    manualCode = formatted
                                    if (formatted.length == 5) {
                                        onVerifyDeviceCode(formatted)
                                    }
                                },
                                label = { Text("5-Character Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (manualCode.length == 5) {
                                            onVerifyDeviceCode(manualCode)
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentIceBlue,
                                    unfocusedBorderColor = SubtleBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSkip,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Text("Später", color = TextSubtitleWhite)
                                }

                                Button(
                                    onClick = { onVerifyDeviceCode(manualCode) },
                                    enabled = manualCode.length == 5,
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

                        is WebQueueLinkState.RequestingLink,
                        is WebQueueLinkState.Verifying -> {
                            val stepText = "Connecting..."

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

                        is WebQueueLinkState.Success -> {"""
# we also need to get rid of waitingforcode since we removed it from the enum
content = content.replace("WaitingForCode", "RequestingLink")

import sys
if search_dialog_body in content:
    content = content.replace(search_dialog_body, replace_dialog_body)
else:
    print("Could not find dialog body to replace.")
    sys.exit(1)

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "w") as f:
    f.write(content)
