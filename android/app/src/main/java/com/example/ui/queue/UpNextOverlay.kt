package com.example.ui.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.QueueScheduleItem
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtitleWhite
import com.example.ui.theme.SurfaceCard

@Composable
fun UpNextOverlay(
    isVisible: Boolean,
    queueItems: List<QueueScheduleItem>,
    redditScheduleTitle: String? = null,
    redditScheduleText: String? = null,
    isRedditFallback: Boolean = false,
    // Am Fernseher bleibt das Panel ein Seitenfluegel, mobil bekommt es fast die ganze
    // Breite — 58 % minus TV-Rand waren im Hochformat nur noch rund 160 dp Leseflaeche.
    isTv: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(if (isTv) 0.58f else 0.94f)
                .padding(
                    end = if (isTv) 48.dp else 12.dp,
                    top = if (isTv) 27.dp else 12.dp,
                    bottom = if (isTv) 27.dp else 12.dp
                )
                .testTag("up_next_queue_overlay"),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                color = SurfaceDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (isRedditFallback) AccentVibrantOrange.copy(alpha = 0.5f) else AccentPurple.copy(alpha = 0.4f)),
                shadowElevation = 32.dp,
                modifier = Modifier.fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header with Live / Fallback Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isRedditFallback) AccentVibrantOrange.copy(alpha = 0.2f) else AccentPurple.copy(alpha = 0.25f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = if (isRedditFallback) AccentVibrantOrange else AccentLavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRedditFallback) stringResource(R.string.epg_queue_title_reddit) else stringResource(R.string.queue_title),
                                style = TextStyle(
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = if (isRedditFallback) "Channel-Z Announcement" else "Channel-Z Live Playlist (cytu.be/r/Channel-Z)",
                                style = TextStyle(
                                    color = if (isRedditFallback) AccentVibrantOrange else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isRedditFallback) FontWeight.Medium else FontWeight.Normal
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated Full Schedule / Announcement Text Box (scrollable)
                    if (!redditScheduleText.isNullOrBlank()) {
                        val scrollState = rememberScrollState()
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (queueItems.isEmpty()) Modifier.weight(1f)
                                    else Modifier.height(180.dp)
                                )
                                .padding(bottom = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    text = redditScheduleTitle?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.epg_reddit_title_default),
                                    style = TextStyle(
                                        color = AccentIceBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = redditScheduleText,
                                    style = TextStyle(
                                        color = TextSubtitleWhite,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }

                    // Table Column Headers & Queue (if queueItems available)
                    if (queueItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCard, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.col_start_time),
                                style = TextStyle(
                                    color = AccentLavender,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.width(68.dp)
                            )
                            Text(
                                text = stringResource(R.string.col_title),
                                style = TextStyle(
                                    color = AccentLavender,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.col_duration),
                                style = TextStyle(
                                    color = AccentLavender,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.width(56.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Table Content List
                    if (queueItems.isEmpty() && redditScheduleText.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.queue_empty),
                                style = TextStyle(
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    } else if (queueItems.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            itemsIndexed(queueItems.take(15)) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Start Time
                                    Text(
                                        text = item.startTimeFormatted,
                                        style = TextStyle(
                                            color = AccentLavender,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = Modifier.width(68.dp)
                                    )

                                    // Title
                                    Text(
                                        text = "${index + 1}. ${item.title}",
                                        style = TextStyle(
                                            color = TextSubtitleWhite,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Duration
                                    Text(
                                        text = item.durationFormatted,
                                        style = TextStyle(
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        modifier = Modifier.width(56.dp)
                                    )
                                }

                                if (index < queueItems.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(SubtleBorder.copy(alpha = 0.5f))
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
