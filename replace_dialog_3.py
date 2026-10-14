with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "r") as f:
    content = f.read()

# Replace WebQueueOtpState references to WebQueueLinkState
content = content.replace("WebQueueOtpState", "WebQueueLinkState")
content = content.replace("otpState", "linkState")
content = content.replace("onStartMagicLogin: (String, String) -> Unit,", "onVerifyDeviceCode: (String) -> Unit,")
content = content.replace("onVerifyManualOtp: (String, String) -> Unit,", "")
content = content.replace("initialUsername: String = \"\"", "")


# Find the start of the when block
when_start = content.find("                    when (val state = linkState) {")
if when_start != -1:
    when_end = content.find("                        is WebQueueLinkState.Success -> {", when_start)
    if when_end != -1:
        # We replace everything from when_start to when_end with our new UI
        replace_with = """                    when (val state = linkState) {
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

"""
        content = content[:when_start] + replace_with + content[when_end:]
        with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "w") as f:
            f.write(content)
        print("Success")
    else:
        print("Could not find when_end")
else:
    print("Could not find when_start")
