package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEmote
import com.example.data.model.ChatMessage
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.ClassicAmber
import com.example.ui.theme.ClassicCyan
import com.example.ui.theme.ClassicGreen
import com.example.ui.theme.ClassicOrange
import com.example.ui.theme.ClassicSystem
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSubtitleWhite

/**
 * Clean, stream-style Subtitle Chat Overlay.
 * Displays up to 3 recent incoming chat messages continuously as long as chat is enabled.
 */
@Composable
fun SubtitleChatOverlay(
    messages: List<ChatMessage>,
    isVisible: Boolean,
    maxLines: Int = 3,
    backgroundOpacity: Float = 0.60f,
    fontSizeSp: Int = 15,
    chatTheme: String = "channelz",
    emotes: List<ChannelEmote> = emptyList(),
    modifier: Modifier = Modifier
) {
    val recentMessages = messages.takeLast(maxLines.coerceIn(1, 3))
    val emoteMap = rememberEmoteMap(emotes)

    AnimatedVisibility(
        visible = isVisible && recentMessages.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(400)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                .testTag("subtitle_chat_container"),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                recentMessages.forEach { msg ->
                    SingleSubtitleLine(
                        message = msg,
                        backgroundOpacity = backgroundOpacity,
                        fontSizeSp = fontSizeSp,
                        chatTheme = chatTheme,
                        emotes = emoteMap
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleSubtitleLine(
    message: ChatMessage,
    backgroundOpacity: Float,
    fontSizeSp: Int,
    chatTheme: String = "channelz",
    emotes: Map<String, String> = emptyMap()
) {
    val strongSubtitleShadow = Shadow(
        color = Color.Black,
        offset = Offset(0f, 2f),
        blurRadius = 4f
    )

    val hash = message.username.hashCode()
    // Zwei Farb-Paletten: Channel-Z (Purple/IceBlue/Coral) vs Classic (CyTube-Original: Cyan/Orange/Green)
    val nameColor = when {
        message.isSystem -> if (chatTheme == "classic") ClassicSystem else AccentVibrantOrange
        message.userRank >= 3 -> if (chatTheme == "classic") ClassicAmber else AccentLavender
        hash % 3 == 0 -> if (chatTheme == "classic") ClassicCyan else AccentIceBlue
        hash % 3 == 1 -> if (chatTheme == "classic") ClassicOrange else AccentCoral
        else -> if (chatTheme == "classic") ClassicGreen else AccentLavender
    }

    // Kanal-Emotes im Nachrichtentext durch ihre Bilder ersetzen.
    val (bodyText, inlineEmotes) = buildEmoteString(message.text, emotes, fontSizeSp.sp)

    val annotatedText = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = nameColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                shadow = strongSubtitleShadow
            )
        ) {
            append(message.username)
            append(": ")
        }
        withStyle(
            style = SpanStyle(
                color = TextSubtitleWhite,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                shadow = strongSubtitleShadow
            )
        ) {
            append(bodyText)
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = backgroundOpacity.coerceIn(0.2f, 0.9f)))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(
            text = annotatedText,
            inlineContent = inlineEmotes,
            style = TextStyle(
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 4).sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
