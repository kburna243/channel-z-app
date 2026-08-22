package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.SpeakerNotes
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerNotesOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.BuildConfig
import com.example.data.model.AppSettings
import com.example.data.model.ChatLayout
import com.example.data.model.LoginState
import com.example.data.model.SettingsPage
import com.example.data.report.BugReporter
import com.example.data.update.UpdateInfo
import com.example.data.update.UpdateManager
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.ThemePalette
import com.example.ui.theme.Palettes
import com.example.ui.theme.paletteOf
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.CardFocusedSurface
import com.example.ui.theme.FocusBorderRing
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsOverlay(
    settings: AppSettings,
    isOpen: Boolean,
    settingsPage: SettingsPage = SettingsPage.MAIN,
    onOpenPage: (SettingsPage) -> Unit = {},
    onClose: () -> Unit,
    onToggleChat: () -> Unit,
    onUpdateChatMaxLines: (Int) -> Unit = {},
    onToggleSubtitles: () -> Unit = {},
    onUpdateChatAutoHide: (Int) -> Unit = {},
    onUpdateChatTheme: (String) -> Unit = {},
    onUpdateAppTheme: (String) -> Unit = {},
    nowPlayingTitle: String? = null,
    onToggleMovieInfo: () -> Unit = {},
    chatLayout: ChatLayout = ChatLayout.SUBTITLE,
    onCycleChatLayout: () -> Unit = {},
    loginState: LoginState = LoginState.LoggedOut,
    savedChatUsername: String = "",
    onLoginChat: (String, String) -> Unit = { _, _ -> },
    onLogoutChat: () -> Unit = {},
    onToggleImdb: () -> Unit = {},
    onUpdateOpacity: (Float) -> Unit,
    onUpdateFontSize: (Int) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onPlayDemoStream: () -> Unit,
    onRetryConnection: () -> Unit,
    // Mobil reicht ein schmaler Rand — die 32 dp des Fernsehers fressen auf einem
    // 360-dp-Handy im Hochformat fast ein Fuenftel der Breite.
    isTv: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }
    val listState = rememberLazyListState()

    val closeButtonFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }
    var isInstalling by remember { mutableStateOf(false) }
    val bugReporter = remember { BugReporter() }
    var isSendingBug by remember { mutableStateOf(false) }
    // Kennung des gemeldeten Falls samt Status — sonst zeigt jeder Eintrag denselben Text.
    var bugStatus by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Pre-resolve strings for async coroutine scopes
    val txtDownloading = stringResource(R.string.update_downloading)
    val txtNeedsInstallPermission = stringResource(R.string.update_needs_install_permission)
    val txtStartingInstall = stringResource(R.string.update_starting_install)
    val txtDownloadFailed = stringResource(R.string.update_download_failed)
    val txtChecking = stringResource(R.string.update_checking)
    val txtServerUnreachable = stringResource(R.string.update_server_unreachable)
    val txtUpdateFound = stringResource(R.string.update_found)
    val txtAlreadyLatest = stringResource(R.string.update_already_latest)
    val txtErrorOpening = stringResource(R.string.update_error_opening)
    val txtBugSending = stringResource(R.string.bug_report_sending)
    val txtBugSent = stringResource(R.string.bug_report_sent)
    val txtBugFailed = stringResource(R.string.bug_report_failed)

    // Beim Schließen stale Focus clearen, beim Öffnen robust neu setzen.
    //
    // Root Cause des "nach Chat-Einstellung nichts mehr bedienbar"-Bugs: listState liegt AUSSERHALB
    // der AnimatedVisibility und überlebt das Schließen. Wer runtergescrollt hat, um eine
    // Chat-Einstellung zu ändern, findet die LazyColumn beim Reopen an derselben Stelle vor —
    // Item 0 mit dem firstItemFocusRequester ist dann gar nicht komponiert, requestFocus() wirft
    // und der Catch verschluckte es. Ergebnis: kein Node hat Focus, das D-Pad läuft ins Leere.
    // scrollToItem(0) stellt sicher, dass das Ziel überhaupt existiert; der Retry deckt das
    // Timing gegen die 200ms-Enter-Animation ab.
    LaunchedEffect(isOpen, settingsPage) {
        if (isOpen) {
            listState.scrollToItem(0)
            var focused = false
            var attempts = 0
            while (!focused && attempts < 10) {
                delay(50)
                focused = try {
                    firstItemFocusRequester.requestFocus()
                    true
                } catch (_: Exception) {
                    false
                }
                attempts++
            }
        } else {
            focusManager.clearFocus()
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + scaleIn(initialScale = 0.96f),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + scaleOut(targetScale = 0.96f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(
                    horizontal = if (isTv) 32.dp else 12.dp,
                    vertical = if (isTv) 20.dp else 10.dp
                )
                .testTag("settings_overlay_modal"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxSize(0.94f),
                color = SurfaceDark,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SubtleBorder),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentPurple.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = AccentIceBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.settings_title),
                                style = TextStyle(
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        // Close Button in Top Right (Fully clickable & D-Pad navigable)
                        var isCloseFocused by remember { mutableStateOf(false) }
                        val closeScale by animateFloatAsState(targetValue = if (isCloseFocused) 1.06f else 1.0f, label = "closeScale")

                        Row(
                            modifier = Modifier
                                .scale(closeScale)
                                .focusRequester(closeButtonFocusRequester)
                                .focusProperties {
                                    down = firstItemFocusRequester
                                }
                                .onFocusChanged { isCloseFocused = it.isFocused }
                                .focusable()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCloseFocused) AccentPurple else CardFocusedSurface)
                                .border(
                                    width = if (isCloseFocused) 2.dp else 1.dp,
                                    color = if (isCloseFocused) AccentPurple else SubtleBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER,
                                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                                onClose()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onClose() }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("close_settings_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_close),
                                style = TextStyle(
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (settingsPage == SettingsPage.MAIN) {

                            item { SectionHeading(stringResource(R.string.section_playback)) }

                            // 3. YouTube Subtitles (CC)
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_subtitles),
                                    subtitle = if (settings.subtitlesEnabled) stringResource(R.string.settings_subtitles_on) else stringResource(R.string.settings_subtitles_off),
                                    icon = Icons.Default.ClosedCaption,
                                    onClick = onToggleSubtitles,
                                    modifier = Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .focusProperties {
                                            up = closeButtonFocusRequester
                                        }
                                ) {
                                    Surface(
                                        color = if (settings.subtitlesEnabled) AccentIceBlue else SurfaceDark,
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (settings.subtitlesEnabled) null else BorderStroke(1.dp, SubtleBorder)
                                    ) {
                                        Text(
                                            text = if (settings.subtitlesEnabled) stringResource(R.string.settings_chat_active) else stringResource(R.string.settings_chat_off),
                                            color = if (settings.subtitlesEnabled) Color.Black else TextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_movie_info),
                                    subtitle = if (settings.movieInfoEnabled)
                                        stringResource(R.string.settings_movie_info_on)
                                    else stringResource(R.string.settings_movie_info_off),
                                    icon = Icons.Default.Movie,
                                    onClick = onToggleMovieInfo
                                ) {
                                    StatePill(active = settings.movieInfoEnabled)
                                }
                            }

                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_imdb),
                                    subtitle = if (settings.imdbEnabled)
                                        stringResource(R.string.settings_imdb_on)
                                    else stringResource(R.string.settings_imdb_off),
                                    icon = Icons.Default.Star,
                                    onClick = onToggleImdb
                                ) {
                                    StatePill(active = settings.imdbEnabled)
                                }
                            }

                            item { SectionHeading(stringResource(R.string.section_chat)) }

                            // 1. Chat Overlay Toggle
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat),
                                    subtitle = if (settings.chatEnabled) stringResource(R.string.settings_chat_sub_active) else stringResource(R.string.settings_chat_sub_off),
                                    icon = if (settings.chatEnabled) Icons.AutoMirrored.Filled.SpeakerNotes else Icons.Default.SpeakerNotesOff,
                                    onClick = onToggleChat,
                                ) {
                                    Surface(
                                        color = if (settings.chatEnabled) AccentIceBlue else SurfaceDark,
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (settings.chatEnabled) null else BorderStroke(1.dp, SubtleBorder)
                                    ) {
                                        Text(
                                            text = if (settings.chatEnabled) stringResource(R.string.settings_chat_active) else stringResource(R.string.settings_chat_off),
                                            color = if (settings.chatEnabled) Color.Black else TextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Sammeleintrag: fuehrt in die Unterseite mit der Chat-Darstellung.
                            // Der reine An/Aus-Schalter oben bleibt bewusst auf der ersten Ebene.
                            if (BuildConfig.HAS_CHAT_INPUT) {
                                item {
                                    FocusableSettingsItem(
                                        title = stringResource(R.string.chat_layout),
                                        subtitle = when (chatLayout) {
                                            ChatLayout.SUBTITLE -> stringResource(R.string.chat_layout_subtitle)
                                            ChatLayout.SIDEBAR -> stringResource(R.string.chat_layout_sidebar)
                                            ChatLayout.HIDDEN -> stringResource(R.string.chat_layout_hidden)
                                            ChatLayout.CHAT_ONLY -> stringResource(R.string.chat_layout_chat_only)
                                        },
                                        icon = Icons.AutoMirrored.Filled.Chat,
                                        onClick = onCycleChatLayout
                                    ) {}
                                }

                                // Konto und Anmeldung: gespeichertes Kennwort, Gastzugang,
                                // Abmelden. Die Seite dahinter kennt nur die Full-Ausgabe.
                                item {
                                    FocusableSettingsItem(
                                        title = stringResource(R.string.settings_chat_account),
                                        subtitle = when {
                                            loginState is LoginState.LoggedIn ->
                                                stringResource(R.string.chat_account_status_in, loginState.username)
                                            savedChatUsername.isNotEmpty() ->
                                                stringResource(R.string.settings_chat_account_saved, savedChatUsername)
                                            else -> stringResource(R.string.chat_account_status_out)
                                        },
                                        icon = Icons.Default.AccountCircle,
                                        onClick = { onOpenPage(SettingsPage.CHAT_ACCOUNT) }
                                    ) {
                                        Text(
                                            text = "›",
                                            color = AccentIceBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_appearance),
                                    subtitle = stringResource(R.string.settings_chat_appearance_sub),
                                    icon = Icons.Default.Tune,
                                    onClick = { onOpenPage(SettingsPage.CHAT_APPEARANCE) }
                                ) {
                                    Text(
                                        text = "›",
                                        color = AccentIceBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                            }

                            item { SectionHeading(stringResource(R.string.section_look)) }

                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_theme),
                                    subtitle = stringResource(themeNameRes(settings.appTheme)),
                                    icon = Icons.Default.Palette,
                                    onClick = { onOpenPage(SettingsPage.THEME) }
                                ) {
                                    PalettePreview(paletteOf(settings.appTheme))
                                }
                            }

                            // 4. Language
                            item {
                                val langOptions = listOf("system", "de", "en")
                                val nextLang = langOptions[(langOptions.indexOf(settings.languageCode).coerceAtLeast(0) + 1) % langOptions.size]
                                val langSubtitle = when (settings.languageCode) {
                                    "de" -> stringResource(R.string.lang_german)
                                    "en" -> stringResource(R.string.lang_english)
                                    else -> stringResource(R.string.lang_system)
                                }

                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_language),
                                    subtitle = langSubtitle,
                                    icon = Icons.Default.Language,
                                    onClick = { onUpdateLanguage(nextLang) }
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("system" to "AUTO", "de" to "DE", "en" to "EN").forEach { (code, label) ->
                                            val isSelected = settings.languageCode == code
                                            Surface(
                                                color = if (isSelected) AccentIceBlue else SurfaceDark,
                                                shape = RoundedCornerShape(6.dp),
                                                border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item { SectionHeading(stringResource(R.string.section_app)) }

                            // 5. In-App Updater Item
                            item {
                                val curCode = updateManager.currentVersionCode
                                val curName = updateManager.currentVersionName
                                val isUpdateAvail = updateInfo?.isNewerThan(curCode) == true

                                val titleText = stringResource(R.string.settings_app_update, curName, curCode)
                                val defaultSubtitle = if (isUpdateAvail) stringResource(R.string.update_available, updateInfo?.versionName ?: "") else stringResource(R.string.update_up_to_date)

                                FocusableSettingsItem(
                                    title = titleText,
                                    subtitle = updateStatusText ?: defaultSubtitle,
                                    icon = Icons.Default.SystemUpdateAlt,
                                    onClick = {
                                        // isUpdateAvail statt updateInfo != null: nach einer Pruefung ohne
                                        // Fund war updateInfo trotzdem gesetzt. Der Eintrag hiess dann
                                        // "Installieren" und lud rund 30 MB fuer genau die Version, die
                                        // bereits laeuft.
                                        val needsPermission = updateManager.installPermissionIntent()
                                        if (isUpdateAvail && !isInstalling && needsPermission != null) {
                                            // Erst die Freigabe holen, dann laden — sonst laufen
                                            // 29 MB durch und die Installation wird still geblockt.
                                            updateStatusText = txtNeedsInstallPermission
                                            try {
                                                context.startActivity(needsPermission)
                                            } catch (e: Exception) {
                                                updateStatusText = String.format(txtErrorOpening, e.localizedMessage ?: "")
                                            }
                                        } else if (isUpdateAvail && !isInstalling) {
                                            isInstalling = true
                                            updateStatusText = txtDownloading
                                            scope.launch {
                                                val intent = updateManager.downloadAndInstall(updateInfo!!)
                                                isInstalling = false
                                                if (intent != null) {
                                                    updateStatusText = txtStartingInstall
                                                    try {
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        updateStatusText = String.format(txtErrorOpening, e.localizedMessage ?: "")
                                                    }
                                                } else {
                                                    updateStatusText = txtDownloadFailed
                                                }
                                            }
                                        } else if (!isCheckingUpdate && !isInstalling) {
                                            isCheckingUpdate = true
                                            updateStatusText = txtChecking
                                            scope.launch {
                                                val info = updateManager.fetchUpdate()
                                                isCheckingUpdate = false
                                                if (info != null) {
                                                    updateInfo = info
                                                    if (info.isNewerThan(curCode)) {
                                                        updateStatusText = String.format(txtUpdateFound, info.versionName)
                                                    } else {
                                                        updateStatusText = String.format(txtAlreadyLatest, info.versionName)
                                                    }
                                                } else {
                                                    // Konkreten Grund zeigen statt pauschal "Server nicht
                                                    // erreichbar" — der Unterschied zwischen TLS-Abbruch,
                                                    // HTTP-Fehler und Timeout ist bei der Fehlersuche alles.
                                                    val reason = updateManager.lastFailureReason
                                                    updateStatusText = if (reason.isNullOrBlank()) {
                                                        txtServerUnreachable
                                                    } else {
                                                        "$txtServerUnreachable ($reason)"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Surface(
                                        color = if (isUpdateAvail) AccentPurple else SurfaceCard,
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (isUpdateAvail) null else BorderStroke(1.dp, SubtleBorder)
                                    ) {
                                        if (isCheckingUpdate || isInstalling) {
                                            CircularProgressIndicator(
                                                color = AccentIceBlue,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                                    .size(15.dp)
                                            )
                                        } else {
                                            Text(
                                                text = if (isUpdateAvail) stringResource(R.string.action_install) else stringResource(R.string.action_check),
                                                color = if (isUpdateAvail) PureWhite else AccentIceBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_bug_report),
                                    subtitle = stringResource(R.string.settings_bug_report_sub),
                                    icon = Icons.Default.BugReport,
                                    onClick = { onOpenPage(SettingsPage.BUG_REPORT) }
                                ) {
                                    Text(
                                        text = "›",
                                        color = AccentIceBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                            }

                            // 6. Test Demo Stream
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.demo_stream_title),
                                    subtitle = stringResource(R.string.demo_stream_desc),
                                    icon = Icons.Default.PlayCircleOutline,
                                    onClick = {
                                        onPlayDemoStream()
                                        onClose()
                                    }
                                ) {
                                    Surface(
                                        color = AccentPurple,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_play),
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // 7. Manual Reconnect
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.retry_connection),
                                    subtitle = stringResource(R.string.retry_connection_desc),
                                    icon = Icons.Default.Refresh,
                                    onClick = {
                                        onRetryConnection()
                                        onClose()
                                    }
                                ) {
                                    Surface(
                                        color = SurfaceCard,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, SubtleBorder)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_retry),
                                            color = AccentIceBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        } else if (settingsPage == SettingsPage.BUG_REPORT) {
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.action_back),
                                    subtitle = stringResource(R.string.settings_bug_report),
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    onClick = { onOpenPage(SettingsPage.MAIN) },
                                    modifier = Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .focusProperties {
                                            up = closeButtonFocusRequester
                                        }
                                ) {}
                            }

                            // Fertige Faelle statt Eingabefeld: auf einer Fernbedienung tippt
                            // niemand einen Fehlerbericht ab. Geraet, Systemversion und der
                            // laufende Titel gehen automatisch mit — genau die Angaben, die
                            // eine Meldung erst nachvollziehbar machen.
                            items(bugReportCases, key = { it.first }) { entry ->
                                val label = stringResource(entry.second)
                                FocusableSettingsItem(
                                    title = label,
                                    subtitle = bugStatus?.takeIf { it.first == entry.first }?.second
                                        ?: stringResource(R.string.bug_report_hint),
                                    icon = Icons.Default.BugReport,
                                    onClick = {
                                        if (!isSendingBug) {
                                            isSendingBug = true
                                            bugStatus = entry.first to txtBugSending
                                            scope.launch {
                                                val result = bugReporter.send(
                                                    description = label,
                                                    severity = if (entry.first == "crash") "high" else "medium",
                                                    nowPlaying = nowPlayingTitle
                                                )
                                                isSendingBug = false
                                                bugStatus = entry.first to when (result) {
                                                    is BugReporter.Result.Sent -> txtBugSent
                                                    is BugReporter.Result.Failed ->
                                                        String.format(txtBugFailed, result.reason)
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    if (isSendingBug && bugStatus?.first == entry.first) {
                                        CircularProgressIndicator(
                                            color = AccentIceBlue,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                                .size(15.dp)
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.bug_report_send),
                                            color = AccentIceBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        } else if (settingsPage == SettingsPage.THEME) {
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.action_back),
                                    subtitle = stringResource(R.string.settings_theme),
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    onClick = { onOpenPage(SettingsPage.MAIN) },
                                    modifier = Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .focusProperties {
                                            up = closeButtonFocusRequester
                                        }
                                ) {}
                            }

                            items(Palettes, key = { it.id }) { palette ->
                                val isSelected = settings.appTheme == palette.id
                                FocusableSettingsItem(
                                    title = stringResource(themeNameRes(palette.id)),
                                    subtitle = stringResource(themeDescriptionRes(palette.id)),
                                    icon = Icons.Default.Palette,
                                    onClick = { onUpdateAppTheme(palette.id) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PalettePreview(palette)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        // Der Haken statt eines Rahmens: die Vorschau soll die
                                        // Farben zeigen, nicht die Auswahl markieren.
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isSelected) AccentIceBlue else Color.Transparent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else if (settingsPage == SettingsPage.CHAT_ACCOUNT) {
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.action_back),
                                    subtitle = stringResource(R.string.settings_chat_account),
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    onClick = { onOpenPage(SettingsPage.MAIN) },
                                    modifier = Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .focusProperties {
                                            up = closeButtonFocusRequester
                                        }
                                ) {}
                            }

                            // Stand der Anmeldung; angemeldet wird die Zeile zum Abmelden.
                            item {
                                val loggedIn = loginState as? LoginState.LoggedIn
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_account),
                                    subtitle = when {
                                        loggedIn != null ->
                                            stringResource(R.string.chat_account_status_in, loggedIn.username)
                                        savedChatUsername.isNotEmpty() ->
                                            stringResource(R.string.settings_chat_account_saved, savedChatUsername)
                                        else -> stringResource(R.string.chat_account_status_out)
                                    },
                                    icon = Icons.Default.AccountCircle,
                                    onClick = { if (loggedIn != null) onLogoutChat() }
                                ) {
                                    when {
                                        loggedIn != null -> Surface(
                                            color = AccentVibrantOrange,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.chat_logout),
                                                color = PureWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                        loginState is LoginState.InProgress -> CircularProgressIndicator(
                                            color = AccentIceBlue,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                                .size(15.dp)
                                        )
                                        else -> {}
                                    }
                                }
                            }

                            if (loginState is LoginState.Failed) {
                                item {
                                    Text(
                                        text = loginState.error,
                                        color = AccentVibrantOrange,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                }
                            }

                            // Anmelde-Karte: Konto mit Passwort oder Gast nur mit Namen.
                            item {
                                var nameDraft by remember(savedChatUsername) { mutableStateOf(savedChatUsername) }
                                var passwordDraft by remember { mutableStateOf("") }
                                val canSubmit = nameDraft.isNotBlank() && loginState !is LoginState.InProgress

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SurfaceCard,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.chat_login_title),
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = stringResource(R.string.chat_account_hint),
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )

                                        OutlinedTextField(
                                            value = nameDraft,
                                            onValueChange = { nameDraft = it },
                                            label = { Text(stringResource(R.string.chat_username)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = passwordDraft,
                                            onValueChange = { passwordDraft = it },
                                            label = { Text(stringResource(R.string.chat_password)) },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                if (canSubmit) onLoginChat(nameDraft.trim(), passwordDraft)
                                            }),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = { onLoginChat(nameDraft.trim(), passwordDraft) },
                                                enabled = canSubmit
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.chat_login),
                                                    color = AccentPurple,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            TextButton(
                                                onClick = { onLoginChat(nameDraft.trim(), "") },
                                                enabled = canSubmit
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.chat_guest),
                                                    color = AccentIceBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = stringResource(R.string.chat_guest_hint),
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.action_back),
                                    subtitle = stringResource(R.string.settings_chat_appearance),
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    onClick = { onOpenPage(SettingsPage.MAIN) },
                                    modifier = Modifier
                                        .focusRequester(firstItemFocusRequester)
                                        .focusProperties {
                                            up = closeButtonFocusRequester
                                        }
                                ) {}
                            }

                            // 2. Chat Max Lines (1 / 2 / 3)
                            item {
                                val nextLines = when (settings.chatMaxLines) {
                                    1 -> 2
                                    2 -> 3
                                    else -> 1
                                }
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_lines),
                                    subtitle = pluralStringResource(R.plurals.settings_chat_lines_sub, settings.chatMaxLines, settings.chatMaxLines),
                                    icon = Icons.Default.FormatListNumbered,
                                    onClick = { onUpdateChatMaxLines(nextLines) }
                                ) {
                                    val lineUnit = stringResource(R.string.settings_chat_lines_unit)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(1 to "1", 2 to "2", 3 to "3").forEach { (num, label) ->
                                            val isSelected = settings.chatMaxLines == num
                                            Surface(
                                                color = if (isSelected) AccentIceBlue else SurfaceDark,
                                                shape = RoundedCornerShape(6.dp),
                                                border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder)
                                            ) {
                                                Text(
                                                    text = "$label $lineUnit",
                                                    color = if (isSelected) Color.Black else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Chat Einblenddauer (Feature #2: 5 Sekunden / Dauerhaft)
                            item {
                                val lblAlways = stringResource(R.string.label_always)
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_duration),
                                    subtitle = if (settings.chatAutoHideSeconds > 0)
                                        stringResource(R.string.settings_chat_duration_hides, settings.chatAutoHideSeconds)
                                    else stringResource(R.string.settings_chat_duration_always),
                                    icon = Icons.Default.Timer,
                                    onClick = { onUpdateChatAutoHide(if (settings.chatAutoHideSeconds > 0) 0 else 5) }
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0 to lblAlways, 5 to "5s").forEach { (sec, label) ->
                                            val isSelected = settings.chatAutoHideSeconds == sec
                                            Surface(
                                                color = if (isSelected) AccentIceBlue else SurfaceDark,
                                                shape = RoundedCornerShape(6.dp),
                                                border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Chat Farbtheme (Feature #4: Channel-Z / Classic-CyTube)
                            item {
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_theme),
                                    subtitle = if (settings.chatTheme == "classic")
                                        stringResource(R.string.settings_chat_theme_classic)
                                    else stringResource(R.string.settings_chat_theme_grindhouse),
                                    icon = Icons.Default.Palette,
                                    onClick = { onUpdateChatTheme(if (settings.chatTheme == "classic") "channelz" else "classic") }
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("channelz" to "Channel-Z", "classic" to "Classic").forEach { (code, label) ->
                                            val isSelected = settings.chatTheme == code
                                            Surface(
                                                color = if (isSelected) AccentIceBlue else SurfaceDark,
                                                shape = RoundedCornerShape(6.dp),
                                                border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Chat Opacity
                            item {
                                // 0.15f ist der Auslieferungs-Default und muss deshalb als Preset
                                // auftauchen — sonst ist beim ersten Oeffnen kein Wert markiert.
                                val opacityPresets = listOf(0.15f to "15%", 0.40f to "40%", 0.70f to "70%", 1.0f to "100%")
                                val currentOpacityIndex = opacityPresets.indexOfFirst {
                                    kotlin.math.abs(settings.chatBackgroundOpacity - it.first) < 0.05f
                                }
                                val nextOpacity = opacityPresets[(currentOpacityIndex + 1) % opacityPresets.size].first
                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_opacity),
                                    subtitle = stringResource(R.string.settings_current_opacity, (settings.chatBackgroundOpacity * 100).toInt()),
                                    icon = Icons.Default.Opacity,
                                    onClick = { onUpdateOpacity(nextOpacity) }
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        opacityPresets.forEach { (valOp, label) ->
                                            val isSelected = kotlin.math.abs(settings.chatBackgroundOpacity - valOp) < 0.05f
                                            Surface(
                                                color = if (isSelected) AccentIceBlue else SurfaceDark,
                                                shape = RoundedCornerShape(6.dp),
                                                border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Chat Font Size
                            item {
                                val nextSize = when (settings.chatFontSizeSp) {
                                    12 -> 14
                                    14 -> 16
                                    else -> 12
                                }
                                val lblSmall = stringResource(R.string.size_small)
                                val lblMedium = stringResource(R.string.size_medium)
                                val lblLarge = stringResource(R.string.size_large)

                                FocusableSettingsItem(
                                    title = stringResource(R.string.settings_chat_size),
                                    subtitle = stringResource(R.string.settings_current_fontsize, settings.chatFontSizeSp),
                                    icon = Icons.Default.FormatSize,
                                    onClick = { onUpdateFontSize(nextSize) }
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(12 to lblSmall, 14 to lblMedium, 16 to lblLarge).forEach { (sz, label) ->
                                            val isSelected = settings.chatFontSizeSp == sz
                                            Surface(
                                                color = if (isSelected) AccentIceBlue else SurfaceDark,
                                                shape = RoundedCornerShape(6.dp),
                                                border = if (isSelected) null else BorderStroke(1.dp, SubtleBorder)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.Black else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        }
    }
}

/**
 * Ueberschrift einer Gruppe. Bewusst nicht fokussierbar: das D-Pad springt darueber hinweg,
 * die Zeile ordnet nur das Auge.
 */
@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        color = AccentLavender,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp)
    )
}

/**
 * Die Faelle, die im Kanal tatsaechlich auftreten. Bewusst kurz gehalten: jede Zeile ist ein
 * fertiger Bericht, den ein einziger Klick abschickt.
 */
private val bugReportCases = listOf(
    "playback" to R.string.bug_case_playback,
    "audio" to R.string.bug_case_audio,
    "info" to R.string.bug_case_info,
    "chat" to R.string.bug_case_chat,
    "crash" to R.string.bug_case_crash,
    "other" to R.string.bug_case_other
)

/** Drei Punkte in Hintergrund-, Marken- und Akzentfarbe — reicht, um Themen zu unterscheiden. */
@Composable
private fun PalettePreview(palette: ThemePalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(palette.background, palette.brand, palette.accent).forEach { color ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun themeNameRes(id: String): Int = when (id) {
    "cyberpunk" -> R.string.theme_cyberpunk
    "editorial" -> R.string.theme_editorial
    "channelz", "grindhouse" -> R.string.theme_grindhouse
    else -> R.string.theme_cinematic
}

private fun themeDescriptionRes(id: String): Int = when (id) {
    "cyberpunk" -> R.string.theme_cyberpunk_sub
    "editorial" -> R.string.theme_editorial_sub
    "channelz", "grindhouse" -> R.string.theme_grindhouse_sub
    else -> R.string.theme_cinematic_sub
}

@Composable
private fun FocusableSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1.0f, label = "focusScale")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) FocusBorderRing else SubtleBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isFocused) CardFocusedSurface else SurfaceCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) AccentIceBlue else TextMuted,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            color = if (isFocused) AccentIceBlue else TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            trailingContent()
        }
    }
}

/** AKTIV/AUS-Abzeichen im Stil der uebrigen Schalter. */
@Composable
private fun StatePill(active: Boolean) {
    Surface(
        color = if (active) AccentIceBlue else SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = if (active) null else BorderStroke(1.dp, SubtleBorder)
    ) {
        Text(
            text = if (active) stringResource(R.string.settings_chat_active)
            else stringResource(R.string.settings_chat_off),
            color = if (active) Color.Black else TextMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
