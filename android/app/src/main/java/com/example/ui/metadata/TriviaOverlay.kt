package com.example.ui.metadata

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.focusable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import coil.compose.AsyncImage
import com.example.data.model.MovieInfo
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Einblendbare Trivia-Liste zum laufenden Film. Wird per Fernbedienung geholt und
 * verschwindet auf Zurueck — bewusst als eigenes Overlay und nicht im Now-Playing-HUD,
 * weil die Listen lang sind und man darin scrollen koennen muss.
 */
@Composable
fun TriviaOverlay(
    isVisible: Boolean,
    isLoading: Boolean,
    movieInfo: MovieInfo?,
    onDismiss: () -> Unit,
    // Mobil ist der Bildschirm schmaler als das TV-Bild: kleinerer Rand, damit von der
    // Liste etwas zu lesen bleibt, besonders im Hochformat.
    isTv: Boolean = true,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val listFocus = remember { FocusRequester() }

    // Bei jedem Oeffnen oben anfangen, sonst haengt die Liste beim naechsten Film
    // noch an der alten Position (dieselbe Falle wie im Einstellungsmenue).
    // Der Fokus muss auf die Liste, sonst laufen die D-Pad-Tasten ins Leere und man
    // kommt an laengeren Texten nicht vorbei.
    LaunchedEffect(isVisible, movieInfo?.imdbId) {
        if (isVisible) {
            listState.scrollToItem(0)
            var placed = false
            var attempts = 0
            while (!placed && attempts < 10) {
                delay(50)
                placed = try {
                    listFocus.requestFocus()
                    true
                } catch (_: Exception) {
                    false
                }
                attempts++
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.97f),
        exit = fadeOut() + scaleOut(targetScale = 0.97f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(
                    horizontal = if (isTv) 40.dp else 16.dp,
                    vertical = if (isTv) 28.dp else 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .fillMaxSize(0.86f),
                color = SurfaceDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SubtleBorder),
                shadowElevation = 24.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.trivia_title),
                                style = TextStyle(
                                    color = AccentIceBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp
                                )
                            )
                            movieInfo?.title?.let { title ->
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = title,
                                    style = TextStyle(
                                        color = PureWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.trivia_dismiss_hint),
                            style = TextStyle(color = TextMuted, fontSize = 11.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when {
                        isLoading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentIceBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        movieInfo == null -> Text(
                            text = stringResource(R.string.trivia_empty),
                            style = TextStyle(color = TextMuted, fontSize = 13.sp)
                        )

                        else -> LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .focusRequester(listFocus)
                                .focusable()
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                                        return@onKeyEvent false
                                    }
                                    // Eine Seite pro Druck — bei Absaetzen dieser Laenge ist
                                    // zeilenweises Scrollen zaeh.
                                    val step = when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> 420f
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> -420f
                                        else -> return@onKeyEvent false
                                    }
                                    scope.launch { listState.animateScrollBy(step) }
                                    true
                                }
                        ) {
                            // Erst die Angaben zum Film, danach die Trivia — beides in derselben
                            // Liste, damit man in einem Rutsch durchscrollen kann.
                            movieInfo?.let { info ->
                                item { MovieDetails(info) }
                            }
                            itemsIndexed(movieInfo?.trivia.orEmpty()) { index, entry ->
                                TriviaEntry(index + 1, entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Die ausfuehrlichen Angaben zum Film: Inhalt, Regie, Genre, Laufzeit, Bewertung.
 * Im HUD steht davon nur eine Zeile — hier ist Platz fuer alles, was bekannt ist.
 *
 * Im schmalen Portrait-Handy (<360dp) steht das Poster ueber dem Text, nicht daneben —
 * sonst frass das feste 224dp-Plakat die ganze Breite und der Plot quetschte sich in
 * einen Streifen. Breit bleibt die Row wie bisher.
 */
@Composable
private fun MovieDetails(info: MovieInfo) {
    BoxWithConstraints {
        val narrow = maxWidth < 360.dp
        val posterW = if (info.posterIsWide) (if (narrow) 160.dp else 224.dp) else (if (narrow) 100.dp else 132.dp)
        val posterH = if (info.posterIsWide) (if (narrow) 90.dp else 126.dp) else (if (narrow) 145.dp else 192.dp)
        // Das Plakat aus der Infoleiste, hier deutlich groesser — es ist der Blickfang
        // des Panels und im HUD nur briefmarkengross.
        val poster = @Composable {
            info.posterUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = info.title,
                    modifier = Modifier
                        .size(width = posterW, height = posterH)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (narrow) {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                poster()
                info.posterUrl?.let { Spacer(modifier = Modifier.height(12.dp)) }
                MovieDetailsText(info)
            }
        } else {
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                poster()
                info.posterUrl?.let { Spacer(modifier = Modifier.width(18.dp)) }
                MovieDetailsText(info)
            }
        }
    }
}

@Composable
private fun MovieDetailsText(info: MovieInfo) {
    Column {
        info.plot?.let { plot ->
            Text(
                text = plot,
                style = TextStyle(
                    color = PureWhite.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val rows = buildList {
            if (info.season != null || info.episode != null) {
                val value = when {
                    info.season != null && info.episode != null ->
                        stringResource(R.string.details_season_episode, info.season, info.episode)
                    info.episode != null -> stringResource(R.string.details_episode_short, info.episode)
                    else -> info.season.toString()
                }
                add(stringResource(R.string.details_episode) to value)
            }
            info.year?.let { add(stringResource(R.string.details_year) to it.toString()) }
            info.runtimeMinutes?.takeIf { it > 0 }?.let { add(stringResource(R.string.details_runtime) to "$it min") }
            info.directors.takeIf { it.isNotEmpty() }
                ?.let { add(stringResource(R.string.details_director) to it.joinToString(", ")) }
            info.genres.takeIf { it.isNotEmpty() }
                ?.let { add(stringResource(R.string.details_genre) to it.joinToString(", ")) }
            info.rating?.let { rating ->
                val votes = info.voteCount?.let { " (%,d)".format(Locale.US, it) }.orEmpty()
                add(stringResource(R.string.details_rating) to "★ %.1f%s".format(Locale.US, rating, votes))
            }
        }

        rows.forEach { (label, value) ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = label,
                    style = TextStyle(color = TextMuted, fontSize = 12.sp),
                    modifier = Modifier.width(96.dp)
                )
                Text(
                    text = value,
                    style = TextStyle(color = AccentLavender, fontSize = 12.sp)
                )
            }
        }

        // Ueberschrift nur setzen, wenn darunter auch etwas steht — bei Trailern und
        // Sketchen gibt es keinen IMDb-Eintrag und damit keine Trivia.
        if (info.trivia.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.trivia_section),
                style = TextStyle(
                    color = AccentIceBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/** Ein Trivia-Eintrag: nummeriert, mit Luft und einer ruhigen Trennlinie darunter. */
@Composable
private fun TriviaEntry(number: Int, text: String) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "%02d".format(Locale.US, number),
                style = TextStyle(
                    color = AccentLavender.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.width(30.dp)
            )
            Text(
                // IMDb liefert die Absaetze teils mit harten Umbruechen und doppelten
                // Leerzeichen — das laeuft im Kasten sonst unruhig.
                text = text.replace(Regex("\\s*\\n\\s*"), " ").replace(Regex("\\s{2,}"), " ").trim(),
                style = TextStyle(
                    color = PureWhite.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = SubtleBorder.copy(alpha = 0.5f), thickness = 1.dp)
    }
}
