package com.example.ui.metadata

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import coil.compose.AsyncImage
import androidx.compose.ui.focus.focusProperties
import com.example.data.model.ConnectionStatus
import com.example.data.model.MediaItem
import com.example.data.model.MovieInfo
import com.example.data.model.QueueScheduleItem
import com.example.ui.components.StatusIndicatorDot
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtitleWhite
import java.util.Locale
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark

@Composable
fun MetadataOverlay(
    nowPlaying: MediaItem?,
    upNext: List<MediaItem>,
    queueItems: List<QueueScheduleItem> = emptyList(),
    isVisible: Boolean,
    isRedditFallback: Boolean = false,
    connectionStatus: ConnectionStatus = ConnectionStatus.LIVE,
    userCount: Int = 0,
    roomName: String = "Channel-Z",
    movieInfo: MovieInfo? = null,
    // Fernseher brauchen den grossen Rand (Overscan), Handy und Tablet brauchen den Platz.
    isTv: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 5-%-Rand fuer den Fernseher: 48/27 dp. Vorher 24/18 — auf Geraeten mit
                // Overscan lief das Panel in den beschnittenen Bereich. Mobil ist der Rand
                // klein, sonst wird das Panel besonders im Hochformat unnötig schmal.
                .padding(
                    horizontal = if (isTv) 48.dp else 16.dp,
                    vertical = if (isTv) 27.dp else 12.dp
                )
                .testTag("metadata_overlay_panel"),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                // Flaeche und Rahmen kommen aus dem Thema. Vorher stand hier ein festes
                // #0D1117 — ein kaltes Blaugrau, waehrend alle vier Paletten violett sind;
                // der Themenwechsel war hier deshalb nicht zu sehen.
                color = SurfaceDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.35f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // NOW PLAYING ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plakat, sobald eines gefunden wurde — sonst bleibt das TV-Logo stehen.
                        // Hochkant im Kinoformat, damit das Bild nicht beschnitten wirkt.
                        val poster = movieInfo?.posterUrl
                        Box(
                            modifier = Modifier
                                .then(
                                    if (poster != null) Modifier.size(width = 46.dp, height = 66.dp)
                                    else Modifier.size(48.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceCard)
                                .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (poster != null) {
                                AsyncImage(
                                    model = poster,
                                    contentDescription = movieInfo.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(11.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.now_playing_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title & Channel
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = TextStyle(
                                    color = AccentLavender,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nowPlaying?.title ?: stringResource(R.string.no_media_queued),
                                style = TextStyle(
                                    color = PureWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 4f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (movieInfo?.hasFacts == true) {
                                Spacer(modifier = Modifier.height(3.dp))
                                MovieFactsLine(movieInfo)
                            }
                            Text(
                                text = "cytu.be/r/$roomName",
                                style = TextStyle(
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Der echte Verbindungszustand statt eines festen "LIVE": faellt der
                        // Socket aus, sagt der Infobereich das jetzt, statt weiter Betrieb zu
                        // melden. canFocus = false, damit die eingeblendete Anzeige dem Spieler
                        // nicht den Fokus wegnimmt.
                        StatusIndicatorDot(
                            status = connectionStatus,
                            userCount = userCount,
                            modifier = Modifier.focusProperties { canFocus = false }
                        )
                    }

                    // Fortschritt, sobald eine Laufzeit bekannt ist. Livestreams melden 0
                    // Sekunden — dort waere jede Fuellung geraten.
                    val duration = nowPlaying?.durationSeconds ?: 0.0
                    if (duration > 0.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PlaybackProgress(
                            positionSeconds = nowPlaying?.currentTimeSeconds ?: 0.0,
                            durationSeconds = duration
                        )
                    }

                    // Bis zu drei kommende Titel. Gesammelt wurden schon immer vier, angezeigt
                    // wurde einer. Die volle Liste bleibt der eigenen Ansicht vorbehalten.
                    val nextCandidates: List<NextRow> = if (queueItems.isNotEmpty()) {
                        queueItems.take(3).map {
                            NextRow(
                                title = it.title,
                                duration = it.durationFormatted.takeIf { d -> d.isNotBlank() }
                                    ?: formatDuration(it.durationSeconds),
                                startTime = it.startTimeFormatted
                            )
                        }
                    } else {
                        upNext.take(3).map {
                            NextRow(it.title, formatDuration(it.durationSeconds.toInt()), "")
                        }
                    }
                    val nextRows = nextCandidates.filter { it.title.isNotBlank() }
                    val nextStartTime = nextRows.firstOrNull()?.startTime ?: ""

                    if (nextRows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SubtleBorder)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = AccentLavender,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRedditFallback) stringResource(R.string.epg_up_next_reddit) else stringResource(R.string.up_next),
                                style = TextStyle(
                                    color = if (isRedditFallback) AccentVibrantOrange else AccentLavender,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            if (nextStartTime.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• $nextStartTime",
                                    style = TextStyle(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        nextRows.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = TextStyle(
                                        color = AccentLavender,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = row.title,
                                    style = TextStyle(
                                        color = TextSubtitleWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (row.duration.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = row.duration,
                                        style = TextStyle(
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
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

/**
 * Anteil des bereits Gelaufenen. Getrennt vom Zeichnen, damit die Randfaelle pruefbar
 * bleiben: CyTube meldet nach einem Sprung auch mal eine Position jenseits der Laufzeit.
 */
internal fun progressFraction(positionSeconds: Double, durationSeconds: Double): Float {
    if (durationSeconds <= 0.0 || positionSeconds.isNaN() || durationSeconds.isNaN()) return 0f
    return (positionSeconds / durationSeconds).coerceIn(0.0, 1.0).toFloat()
}

/** Balken mit verstrichener und gesamter Laufzeit. Der Stand stammt vom Kanal, nicht vom Player. */
@Composable
private fun PlaybackProgress(positionSeconds: Double, durationSeconds: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SubtleBorder)
                .testTag("playback_progress")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction(positionSeconds, durationSeconds))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    // Akzentfarbe: die Designvorlage nennt Fortschrittsbalken als einen der
                    // wenigen Orte, an denen der Akzent ueberhaupt auftauchen soll. Einfarbig
                    // statt Verlauf — ein Verlauf zeigt am Anfang nur sein dunkles Ende, also
                    // genau dann am wenigsten, wenn der Balken ohnehin ein Strich ist.
                    .background(AccentIceBlue)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val stamp = TextStyle(
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(text = formatDuration(positionSeconds.toInt()).ifBlank { "00:00" }, style = stamp)
            Text(text = formatDuration(durationSeconds.toInt()), style = stamp)
        }
    }
}

/** Ein kommender Titel, egal ob er aus dem Zeitplan oder aus der Socket-Warteschlange kommt. */
private data class NextRow(val title: String, val duration: String, val startTime: String)

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}

/**
 * Eine Zeile mit dem, was ueber den Film bekannt ist: Jahr, Laufzeit, Regie, Genre, Bewertung.
 * Was fehlt, faellt weg — bei Exploitation-Titeln ist selten alles vorhanden.
 */
@Composable
private fun MovieFactsLine(info: MovieInfo) {
    val parts = buildList {
        // Bei Serienfolgen zuerst, wo man sich befindet — das ist die Angabe, die im
        // Durchlauf einer Staffel am meisten sagt.
        if (info.season != null && info.episode != null) {
            add("S%02d E%02d".format(Locale.US, info.season, info.episode))
        } else if (info.episode != null) {
            add(stringResource(R.string.details_episode_short, info.episode))
        }
        info.year?.let { add(it.toString()) }
        info.runtimeMinutes?.takeIf { it > 0 }?.let { add("$it min") }
        info.directors.firstOrNull()?.let { add(it) }
        info.genres.take(2).takeIf { it.isNotEmpty() }?.let { add(it.joinToString(", ")) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (parts.isNotEmpty()) {
            Text(
                text = parts.joinToString("  ·  "),
                style = TextStyle(color = AccentLavender.copy(alpha = 0.85f), fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        info.rating?.let { rating ->
            if (parts.isNotEmpty()) Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "★ " + String.format(Locale.US, "%.1f", rating),
                style = TextStyle(
                    color = AccentVibrantOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
