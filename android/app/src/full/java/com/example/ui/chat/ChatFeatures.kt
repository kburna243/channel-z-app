package com.example.ui.chat

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ChannelUser
import com.example.data.model.ChatLayout
import com.example.data.model.LoginState
import com.example.player.PlayerViewModel
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * Die Chat-Bedienung der Full-Ausgabe: Nachrichteneingabe, Anmeldung und Nutzerliste.
 *
 * Gedacht fuer das Handy neben dem laufenden Fernseher. Es braucht keine Verbindung zwischen
 * den Geraeten: geschrieben wird direkt in den CyTube-Raum, und die Fassung am Fernseher zeigt
 * die Nachricht an, weil sie denselben Raum mithoert.
 */
@Composable
fun ChatFeatureOverlays(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isComposerOpen by viewModel.isComposerOpen.collectAsStateWithLifecycle()
    val isUserListVisible by viewModel.isUserListVisible.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()

    val chatLayout by viewModel.chatLayout.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val emotes by viewModel.emotes.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        // Spalten-Ansichten gibt es nur hier. Die Untertitel-Zeile kommt weiterhin aus dem
        // gemeinsamen Teil, damit beide Ausgaben dieselbe gewohnte Darstellung haben.
        if (chatLayout == ChatLayout.SIDEBAR || chatLayout == ChatLayout.CHAT_ONLY) {
            ChatColumn(
                messages = messages,
                emotes = emotes,
                fontSizeSp = settings.chatFontSizeSp,
                chatTheme = settings.chatTheme,
                fullWidth = chatLayout == ChatLayout.CHAT_ONLY,
                modifier = Modifier.align(
                    if (chatLayout == ChatLayout.CHAT_ONLY) Alignment.Center else Alignment.CenterEnd
                )
            )
        }

        UserListPanel(
            isVisible = isUserListVisible,
            users = users,
            onDismiss = { viewModel.hideUserList() }
        )

        ChatComposer(
            isOpen = isComposerOpen,
            loginState = loginState,
            onSend = { viewModel.sendChat(it) },
            onLogin = { name, pw -> viewModel.login(name, pw) },
            onDismiss = { viewModel.closeComposer() }
        )
    }
}

/**
 * Tasten, die nur die Full-Ausgabe kennt. Wird aus der Activity aufgerufen und meldet,
 * ob der Druck verbraucht wurde.
 */
fun handleChatKey(viewModel: PlayerViewModel, keyCode: Int): Boolean = when (keyCode) {
    // Tastatur-Kuerzel. Auf der Fernbedienung ist keine Taste mehr frei — dort fuehrt der
    // Knopf in der Bedienleiste zur Eingabe, und am Handy tippt man ohnehin auf den Schirm.
    KeyEvent.KEYCODE_M -> {
        viewModel.openComposer(); true
    }
    KeyEvent.KEYCODE_U -> {
        viewModel.toggleUserList(); true
    }
    KeyEvent.KEYCODE_C -> {
        viewModel.cycleChatLayout(); true
    }
    else -> false
}

/** In der Full-Ausgabe haengt die Untertitel-Zeile an der gewaehlten Ansicht. */
fun showsSubtitleChat(layout: ChatLayout): Boolean = layout == ChatLayout.SUBTITLE

// ---------------------------------------------------------------- Eingabe

@Composable
private fun ChatComposer(
    isOpen: Boolean,
    loginState: LoginState,
    onSend: (String) -> Boolean,
    onLogin: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var review by remember { mutableStateOf<List<SpellingIssue>>(emptyList()) }
    var checking by remember { mutableStateOf(false) }
    val inputFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isOpen, loginState) {
        if (isOpen && loginState is LoginState.LoggedIn) {
            runCatching { inputFocus.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (loginState) {
                        is LoginState.LoggedIn -> {
                            Text(
                                text = stringResource(R.string.chat_writing_as, loginState.username),
                                style = TextStyle(color = TextMuted, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it; review = emptyList() },
                                placeholder = { Text(stringResource(R.string.chat_placeholder_input)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(inputFocus),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (onSend(draft)) { draft = ""; review = emptyList(); onDismiss() }
                                })
                            )

                            if (review.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ReviewHints(
                                    issues = review,
                                    onApply = { issue, replacement ->
                                        draft = draft.replaceRange(
                                            issue.offset,
                                            issue.offset + issue.length,
                                            replacement
                                        )
                                        review = review - issue
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    scope.launch {
                                        checking = true
                                        review = checkSpelling(draft)
                                        checking = false
                                    }
                                }) {
                                    if (checking) {
                                        CircularProgressIndicator(
                                            color = AccentIceBlue,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Text(stringResource(R.string.chat_review), color = AccentIceBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onDismiss) {
                                    Text(stringResource(R.string.action_cancel), color = TextMuted)
                                }
                                TextButton(onClick = {
                                    if (onSend(draft)) { draft = ""; review = emptyList(); onDismiss() }
                                }) {
                                    Text(stringResource(R.string.chat_send), color = AccentPurple, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> LoginForm(
                            username = username,
                            password = password,
                            state = loginState,
                            onUsername = { username = it },
                            onPassword = { password = it },
                            onSubmit = { onLogin(username.trim(), password) },
                            onSubmitGuest = { onLogin(username.trim(), "") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    username: String,
    password: String,
    state: LoginState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onSubmitGuest: () -> Unit
) {
    Text(
        text = stringResource(R.string.chat_login_title),
        style = TextStyle(color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.chat_login_hint),
        style = TextStyle(color = TextMuted, fontSize = 11.sp)
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = username,
        onValueChange = onUsername,
        label = { Text(stringResource(R.string.chat_username)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPassword,
        label = { Text(stringResource(R.string.chat_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier.fillMaxWidth()
    )

    if (state is LoginState.Failed) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = state.error, style = TextStyle(color = AccentVibrantOrange, fontSize = 11.sp))
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Gast: nur Name, kein Passwort. Ob Gaeste schreiben duerfen, entscheidet
        // die Kanal-Berechtigung — die Anmeldung selbst laeuft ueber denselben Weg.
        TextButton(
            onClick = onSubmitGuest,
            enabled = username.isNotBlank() && state !is LoginState.InProgress
        ) {
            Text(stringResource(R.string.chat_guest), color = AccentIceBlue)
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onSubmit, enabled = state !is LoginState.InProgress) {
            if (state is LoginState.InProgress) {
                CircularProgressIndicator(
                    color = AccentIceBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(stringResource(R.string.chat_login), color = AccentPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.chat_guest_hint),
        style = TextStyle(color = TextMuted, fontSize = 10.sp)
    )
}

@Composable
private fun ReviewHints(
    issues: List<SpellingIssue>,
    onApply: (SpellingIssue, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        issues.take(3).forEach { issue ->
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = issue.message,
                        style = TextStyle(color = AccentVibrantOrange, fontSize = 11.sp)
                    )
                    if (issue.replacements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            issue.replacements.take(3).forEach { candidate ->
                                Surface(
                                    color = AccentPurple.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { onApply(issue, candidate) }
                                ) {
                                    Text(
                                        text = candidate,
                                        style = TextStyle(color = PureWhite, fontSize = 12.sp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Nutzerliste

@Composable
private fun UserListPanel(
    isVisible: Boolean,
    users: List<ChannelUser>,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.chat_users, users.size),
                        style = TextStyle(
                            color = AccentIceBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users) { user -> UserRow(user) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: ChannelUser) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (user.profileImage != null) {
            AsyncImage(
                model = user.profileImage,
                contentDescription = null,
                modifier = Modifier
                    .size(26.dp)
                    .background(SurfaceCard, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(SurfaceCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    style = TextStyle(color = AccentLavender, fontSize = 12.sp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = user.name,
            style = TextStyle(
                // Moderatoren und Betreiber heben sich ab, so wie im Webchat auch.
                color = if (user.rank >= 2) AccentVibrantOrange else PureWhite,
                fontSize = 13.sp,
                fontWeight = if (user.rank >= 2) FontWeight.Bold else FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (user.isAfk) {
            Text(
                text = stringResource(R.string.chat_afk),
                style = TextStyle(color = TextMuted, fontSize = 10.sp)
            )
        }
    }
}
