package com.example.ui.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.model.ConnectionStatus
import com.example.player.PlayerViewModel
import com.example.data.model.ChatLayout
import com.example.ui.chat.ChatFeatureOverlays
import com.example.ui.chat.showsSubtitleChat
import com.example.ui.chat.SubtitleChatOverlay
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.components.SplashScreenView
import com.example.ui.components.StatusIndicatorDot
import com.example.ui.components.VortexBackground
import com.example.ui.metadata.MetadataOverlay
import com.example.ui.metadata.TriviaOverlay
import com.example.ui.queue.UpNextOverlay
import com.example.ui.settings.SettingsOverlay
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceDark
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrindhouseMainScreen(
    viewModel: PlayerViewModel,
    isInPipMode: Boolean = false,
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTv = remember { context.packageManager.hasSystemFeature("android.software.leanback") }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val upNext by viewModel.upNext.collectAsStateWithLifecycle()
    val userCount by viewModel.userCount.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isMetadataVisible by viewModel.isMetadataVisible.collectAsStateWithLifecycle()
    val isUpNextVisible by viewModel.isUpNextVisible.collectAsStateWithLifecycle()
    val isRemoteHintsVisible by viewModel.isRemoteHintsVisible.collectAsStateWithLifecycle()
    val metadataOverlayState by viewModel.metadataOverlayState.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val settingsPage by viewModel.settingsPage.collectAsStateWithLifecycle()
    val movieInfo by viewModel.movieInfo.collectAsStateWithLifecycle()
    val isTriviaVisible by viewModel.isTriviaVisible.collectAsStateWithLifecycle()
    val isTriviaLoading by viewModel.isTriviaLoading.collectAsStateWithLifecycle()
    val castUnavailable = stringResource(R.string.cast_unavailable)
    val chatLayout by viewModel.chatLayout.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val savedChatUsername by viewModel.savedChatUsername.collectAsStateWithLifecycle()
    val emotes by viewModel.emotes.collectAsStateWithLifecycle()
    val showExitDialog by viewModel.showExitDialog.collectAsStateWithLifecycle()
    val mediaSyncUpdate by viewModel.mediaSyncEvent.collectAsStateWithLifecycle(initialValue = null)

    // Handle system back gestures and back button on Android phones smoothly
    BackHandler(enabled = true) {
        viewModel.handleBackPress()
    }

    var showSplashScreen by remember { mutableStateOf(true) }
    var showTouchControls by remember { mutableStateOf(false) }

    // Auto-hide der Touch-Bar nach 4s Inaktivitaet. touchInteractionTick resettet den
    // Timer bei jedem Tap (Button oder freie Flaeche) — ohne den fliegt die Bar sonst
    // mitten in der Bedienung zu, weil showTouchControls bereits true ist und ein
    // erneutes `= true` keine State-Aenderung ausloest.
    var touchInteractionTick by remember { mutableStateOf(0) }
    fun keepTouchAwake() { touchInteractionTick++ }

    LaunchedEffect(showTouchControls, touchInteractionTick) {
        if (showTouchControls) {
            delay(4000)
            showTouchControls = false
        }
    }

    // Feature #2: Chat-Auto-Hide — 0 = dauerhaft sichtbar, >0 = Sekunden bis Ausblendung nach letzter Nachricht.
    // ponytail: LaunchedEffect keyed auf last message id + autoHide-Wert → bei neuer Nachricht Timer resetten.
    var isChatAutoVisible by remember { mutableStateOf(true) }
    LaunchedEffect(chatMessages.lastOrNull()?.id, settings.chatAutoHideSeconds) {
        if (settings.chatAutoHideSeconds > 0 && chatMessages.isNotEmpty()) {
            isChatAutoVisible = true
            delay(settings.chatAutoHideSeconds * 1000L)
            isChatAutoVisible = false
        } else {
            isChatAutoVisible = true  // dauerhaft sichtbar
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightCanvas)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.onRemoteActivity()
                if (!isTv) {
                    showTouchControls = !showTouchControls
                }
            }
            .testTag("grindhouse_main_screen")
    ) {
        if (showSplashScreen) {
            // 0. Cold-boot Splash Screen
            SplashScreenView(
                onFinished = { showSplashScreen = false }
            )
        } else {
            // 1. Fullscreen Video Surface (Hybrid WebView & ExoPlayer Engine)
            VideoPlayerSurface(
                exoPlayer = viewModel.getExoPlayer(),
                mediaItem = nowPlaying,
                isPlaying = isPlaying,
                isMuted = isMuted,
                subtitlesEnabled = settings.subtitlesEnabled,
                mediaSyncUpdate = mediaSyncUpdate,
                modifier = Modifier.fillMaxSize()
            )

            // Transparent Tap Interceptor for Tablets & Smartphones to guarantee touch controls responsiveness
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.onRemoteActivity()
                        if (!isTv) {
                            showTouchControls = !showTouchControls
                        }
                    }
            )

            // 2. Custom Background State when No Active Video or Disconnected
            val isVideoActive = nowPlaying != null && connectionStatus != ConnectionStatus.IDLE && connectionStatus != ConnectionStatus.OFFLINE
            if (!isVideoActive) {
                val subtitle = when {
                    connectionStatus == ConnectionStatus.IDLE -> stringResource(R.string.status_idle)
                    connectionStatus == ConnectionStatus.OFFLINE -> stringResource(R.string.status_disconnected)
                    isBuffering -> stringResource(R.string.status_connecting)
                    else -> stringResource(R.string.status_live_feed)
                }

                VortexBackground(
                    modifier = Modifier.fillMaxSize(),
                    titleText = "CHANNEL-Z",
                    subtitleText = subtitle,
                    showAnimation = isBuffering || connectionStatus == ConnectionStatus.RECONNECTING
                )

                if (isBuffering && connectionStatus != ConnectionStatus.IDLE) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = if (isTv) 120.dp else 64.dp)
                        ) {
                            com.example.ui.components.ChannelZRetroLoadingBar(
                                progress = 0.65f,
                                modifier = Modifier.fillMaxWidth(if (isTv) 0.45f else 0.75f)
                            )
                        }
                    }
                }
            }

            // Hide overlays completely in Picture-in-Picture (PiP) mode
            if (!isInPipMode) {
                // 3. Top Bar: Status Indicator Dot & Remote Quick-Hints (Fades out automatically after 5s; awakens on remote action)
                AnimatedVisibility(
                    // Nicht gleichzeitig mit dem Infobereich: der traegt seit dem Umbau
                    // dieselbe Statusanzeige, und die obere Leiste schimmerte nur noch als
                    // Fleck durch das Panel.
                    visible = (isRemoteHintsVisible || connectionStatus != ConnectionStatus.LIVE) &&
                            !isMetadataVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isTv) 48.dp else 16.dp,
                                vertical = if (isTv) 27.dp else 12.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicatorDot(
                            status = connectionStatus,
                            userCount = userCount,
                            onRetryClick = { viewModel.retryConnection() }
                        )

                        // Remote Hints Pill — nur auf TV. Auf dem Handy gibt es kein D-Pad,
                        // die Fernbedienungs-Symbole sind dort sinnlos und druecken in der
                        // schmalen Hochkant-Leiste Status-Dot und Pill uebereinander (Frieds
                        // "komisch im Hochkant").
                        if (isTv) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RemoteKeySquare("▲", stringResource(R.string.remote_hint_up).substringAfter(" "))
                                    RemoteKeySquare("▼", stringResource(R.string.remote_hint_down).substringAfter(" "))
                                    RemoteKeySquare("◄", stringResource(R.string.remote_hint_left).substringAfter(" "))
                                    RemoteKeySquare("►", stringResource(R.string.remote_hint_right).substringAfter(" "))
                                    RemoteKeySquare("OK", stringResource(R.string.remote_hint_center).substringAfter(" "))
                                    RemoteKeySquare("≡", stringResource(R.string.remote_hint_menu).substringAfter(" "))
                                }
                            }
                        }
                    }
                }

                // 4. Metadata Overlay ("Now Playing" & "Up Next" preview)
                MetadataOverlay(
                    nowPlaying = metadataOverlayState.nowPlaying,
                    upNext = metadataOverlayState.upNext,
                    queueItems = metadataOverlayState.queueItems,
                    isVisible = isMetadataVisible && !isUpNextVisible,
                    isRedditFallback = metadataOverlayState.isRedditFallback,
                    connectionStatus = connectionStatus,
                    userCount = userCount,
                    movieInfo = movieInfo,
                    isTv = isTv,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 5. Up Next Full Queue Schedule Overlay (D-Pad Right / ->)
                UpNextOverlay(
                    isVisible = isUpNextVisible,
                    queueItems = metadataOverlayState.queueItems,
                    redditScheduleTitle = metadataOverlayState.redditScheduleTitle,
                    redditScheduleText = metadataOverlayState.redditScheduleText,
                    isRedditFallback = metadataOverlayState.isRedditFallback,
                    isTv = isTv,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )

                // 6. Subtitle-Style Native Chat Overlay (Bottom 20-30%) - Disabled while Up Next is visible
                // Untertitel-Zeile. In der Light-Ausgabe immer, in der Full-Ausgabe nur, wenn
                // dort nicht gerade eine der Spalten-Ansichten gewaehlt ist.
                if (showsSubtitleChat(chatLayout)) {
                    SubtitleChatOverlay(
                        messages = chatMessages,
                        isVisible = settings.chatEnabled && !isUpNextVisible && isChatAutoVisible,
                        maxLines = settings.chatMaxLines,
                        backgroundOpacity = settings.chatBackgroundOpacity,
                        fontSizeSp = settings.chatFontSizeSp,
                        chatTheme = settings.chatTheme,
                        emotes = emotes,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // Chat-Ansichten, Eingabe, Anmeldung und Nutzerliste — in der Light-Ausgabe leer.
                ChatFeatureOverlays(viewModel = viewModel)

                // 7. Touch Controls Bar for Mobile Smartphones (Shown only on touchscreen tap)
                if (!isTv) {
                    AnimatedVisibility(
                        visible = showTouchControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("mobile_touch_controls")
                    ) {
                        Surface(
                            color = SurfaceDark.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder),
                            shadowElevation = 16.dp
                        ) {
                            // FlowRow statt Row: auf einem 360dp-Handy in Portrait reicht eine
                            // einzelne Zeile fuer sieben 44dp-Buttons nicht — FlowRow bricht
                            // sauber in eine zweite Zeile um, statt Buttons zu quetschen.
                            // clickable auf der Leiste selbst: ein Tap auf die freie Flaeche
                            // zwischen den Buttons haelt die Bar offen, statt zur root-Box
                            // durchzufallen und sie zu schliessen.
                            FlowRow(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showTouchControls = true; keepTouchAwake() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Play / Pause Button
                                IconButton(
                                    onClick = {
                                        viewModel.togglePlayPause()
                                        showTouchControls = true; keepTouchAwake()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(AccentPurple, CircleShape)
                                        .testTag("touch_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_play_pause),
                                        tint = PureWhite,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Mute / Unmute Button
                                IconButton(
                                    onClick = {
                                        viewModel.toggleMute()
                                        showTouchControls = true; keepTouchAwake()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(SurfaceDark, CircleShape)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .testTag("touch_mute_button")
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = stringResource(R.string.cd_mute),
                                        tint = if (isMuted) AccentPurple else AccentLavender
                                    )
                                }

                                // YouTube Subtitles (CC) Button
                                IconButton(
                                    onClick = {
                                        viewModel.toggleSubtitles()
                                        showTouchControls = true; keepTouchAwake()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(if (settings.subtitlesEnabled) AccentPurple else SurfaceDark, CircleShape)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .testTag("touch_subtitles_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = stringResource(R.string.cd_subtitles),
                                        tint = PureWhite
                                    )
                                }

                                // Chat schreiben — gibt es nur in der Full-Ausgabe.
                                if (BuildConfig.HAS_CHAT_INPUT) {
                                    IconButton(
                                        onClick = {
                                            viewModel.openComposer()
                                            showTouchControls = true; keepTouchAwake()
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(AccentPurple, CircleShape)
                                            .border(1.dp, SubtleBorder, CircleShape)
                                            .testTag("touch_compose_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = stringResource(R.string.chat_send),
                                            tint = PureWhite
                                        )
                                    }
                                }

                                // Vollbild-Chat: macht das Geraet zum reinen Chat-Fenster
                                // (Video pausiert). Zurueck geht es in die Ansicht, die vor
                                // dem Wechsel aktiv war. Auf Handy und Tablet der einzige Weg
                                // in die Nur-Chat-Ansicht — eine Layout-Taste gibt es dort nicht.
                                if (BuildConfig.HAS_CHAT_INPUT) {
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleFullChatMode()
                                            showTouchControls = true; keepTouchAwake()
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(if (chatLayout == ChatLayout.CHAT_ONLY) AccentPurple else SurfaceDark, CircleShape)
                                            .border(1.dp, SubtleBorder, CircleShape)
                                            .testTag("touch_full_chat_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forum,
                                            contentDescription = stringResource(R.string.cd_full_chat),
                                            tint = PureWhite
                                        )
                                    }
                                }

                                // Drahtlose Bildschirmuebertragung. Die App oeffnet nur die
                                // Systemeinstellung — spiegeln macht Android selbst. Genau so war
                                // es im Vorgaenger vor dem Rewrite geloest.
                                IconButton(
                                    onClick = {
                                        val opened = listOf(
                                            "android.settings.CAST_SETTINGS",
                                            "android.settings.WIFI_DISPLAY_SETTINGS"
                                        ).any { action ->
                                            try {
                                                context.startActivity(Intent(action))
                                                true
                                            } catch (e: ActivityNotFoundException) {
                                                false
                                            }
                                        }
                                        if (!opened) {
                                            Toast.makeText(context, castUnavailable, Toast.LENGTH_SHORT).show()
                                        }
                                        showTouchControls = true; keepTouchAwake()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(SurfaceDark, CircleShape)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .testTag("touch_cast_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cast,
                                        contentDescription = stringResource(R.string.cd_cast),
                                        tint = PureWhite
                                    )
                                }

                                // Info / Metadata HUD Toggle
                                IconButton(
                                    onClick = {
                                        viewModel.showMetadataOverlay()
                                        showTouchControls = true; keepTouchAwake()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(SurfaceDark, CircleShape)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .testTag("touch_info_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = stringResource(R.string.cd_now_playing),
                                        tint = AccentLavender
                                    )
                                }

                                // Trivia / Filmdetails — auf dem Handy fehlt das D-Pad, daher
                                // der Einstieg hier. Nur wenn Filminfos vorliegen, sonst toggelt
                                // toggleTrivia() ins Leere.
                                if (movieInfo != null) {
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleTrivia()
                                            showTouchControls = true; keepTouchAwake()
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(if (isTriviaVisible) AccentPurple else SurfaceDark, CircleShape)
                                            .border(1.dp, SubtleBorder, CircleShape)
                                            .testTag("touch_trivia_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = stringResource(R.string.trivia_title),
                                            tint = PureWhite
                                        )
                                    }
                                }

                                // Up Next Toggle
                                IconButton(
                                    onClick = {
                                        viewModel.toggleUpNext()
                                        showTouchControls = true; keepTouchAwake()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(if (isUpNextVisible) AccentPurple else SurfaceDark, CircleShape)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .testTag("touch_up_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = stringResource(R.string.cd_up_next),
                                        tint = PureWhite
                                    )
                                }

                                // Settings Menu Button
                                IconButton(
                                    onClick = {
                                        viewModel.openSettings()
                                        showTouchControls = false
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(SurfaceDark, CircleShape)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .testTag("touch_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.cd_settings),
                                        tint = AccentLavender
                                    )
                                }
                            }
                        }
                    }
                }

                // 7b. Trivia zum laufenden Film
                TriviaOverlay(
                    isVisible = isTriviaVisible,
                    isLoading = isTriviaLoading,
                    movieInfo = movieInfo,
                    onDismiss = { viewModel.hideTrivia() },
                    isTv = isTv,
                    modifier = Modifier.fillMaxSize()
                )

                // 8. Centralized Settings Menu Modal
                SettingsOverlay(
                    settings = settings,
                    isOpen = isSettingsOpen,
                    settingsPage = settingsPage,
                    onOpenPage = { viewModel.openSettingsPage(it) },
                    onClose = { viewModel.closeSettings() },
                    onToggleChat = { viewModel.toggleChat() },
                    onUpdateChatMaxLines = { viewModel.updateChatMaxLines(it) },
                    onToggleSubtitles = { viewModel.toggleSubtitles() },
                    onUpdateChatAutoHide = { viewModel.updateChatAutoHide(it) },
                    onUpdateChatTheme = { viewModel.updateChatTheme(it) },
                    onUpdateAppTheme = { viewModel.updateAppTheme(it) },
                    nowPlayingTitle = nowPlaying?.title,
                    onToggleMovieInfo = { viewModel.toggleMovieInfo() },
                    chatLayout = chatLayout,
                    onCycleChatLayout = { viewModel.cycleChatLayout() },
                    loginState = loginState,
                    savedChatUsername = savedChatUsername,
                    onLoginChat = { name, pw -> viewModel.login(name, pw) },
                    onLogoutChat = { viewModel.logout() },
                    onToggleImdb = { viewModel.toggleImdb() },
                    onUpdateOpacity = { viewModel.updateChatOpacity(it) },
                    onUpdateFontSize = { viewModel.updateChatFontSize(it) },
                    onUpdateLanguage = { viewModel.updateLanguage(it) },
                    onPlayDemoStream = { viewModel.playDemoStream() },
                    onRetryConnection = { viewModel.retryConnection() },
                    isTv = isTv
                )

                // 9. Exit Confirmation Dialog
                ExitConfirmationDialog(
                    isOpen = showExitDialog,
                    onConfirmExit = onExitApp,
                    onDismiss = { viewModel.dismissExitDialog() }
                )
            }
        }
    }
}

@Composable
private fun RemoteKeySquare(key: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                style = TextStyle(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 9.sp,
                color = PureWhite.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        )
    }
}
