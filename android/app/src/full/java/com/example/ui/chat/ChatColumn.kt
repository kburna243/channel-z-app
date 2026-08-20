package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEmote
import com.example.data.model.ChatMessage
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.ClassicGreen
import com.example.ui.theme.ClassicOrange
import com.example.ui.theme.ClassicSystem
import com.example.ui.theme.ClassicCyan
import com.example.ui.theme.TextSubtitleWhite

/**
 * Der Chat als fortlaufende Spalte — als Leiste neben dem Bild oder, in voller Breite, als
 * reines Chat-Fenster mit angehaltenem Video.
 *
 * Anders als die Untertitel-Zeile zeigt diese Ansicht den Verlauf und scrollt bei neuen
 * Nachrichten mit. Gedacht fuer das Handy in der Hand, waehrend der Film auf dem Fernseher
 * laeuft.
 */
@Composable
fun ChatColumn(
    messages: List<ChatMessage>,
    emotes: List<ChannelEmote>,
    fontSizeSp: Int,
    chatTheme: String,
    fullWidth: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val emoteMap = rememberEmoteMap(emotes)

    // Bei neuen Nachrichten nach unten nachziehen, so wie es ein Chatfenster tut.
    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(
        modifier = modifier
            // Seitenleiste: hoechstens 340 dp, auf schmalen Handys im Hochformat aber nur
            // 62 % der Breite — sonst bleibt neben der Spalte fast nichts vom Bild.
            .then(
                if (fullWidth) Modifier.fillMaxWidth()
                else Modifier.fillMaxWidth(0.62f).widthIn(max = 340.dp)
            )
            .fillMaxHeight()
            .padding(12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = if (fullWidth) 0.92f else 0.55f))
            .padding(12.dp)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { message ->
                ChatLine(message, emoteMap, fontSizeSp, chatTheme)
            }
        }
    }
}

@Composable
private fun ChatLine(
    message: ChatMessage,
    emotes: Map<String, String>,
    fontSizeSp: Int,
    chatTheme: String
) {
    val hash = message.username.hashCode()
    val nameColor = when {
        message.isSystem -> if (chatTheme == "classic") ClassicSystem else AccentVibrantOrange
        message.userRank >= 2 -> if (chatTheme == "classic") ClassicCyan else AccentIceBlue
        hash % 3 == 0 -> if (chatTheme == "classic") ClassicCyan else AccentIceBlue
        hash % 3 == 1 -> if (chatTheme == "classic") ClassicOrange else AccentCoral
        else -> if (chatTheme == "classic") ClassicGreen else AccentLavender
    }

    val (body, inline) = buildEmoteString(message.text, emotes, fontSizeSp.sp)

    Column {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                    append(message.username)
                    append(": ")
                }
                withStyle(SpanStyle(color = TextSubtitleWhite)) {
                    append(body)
                }
            },
            inlineContent = inline,
            style = TextStyle(
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 6).sp
            )
        )
        Spacer(modifier = Modifier.height(1.dp))
    }
}
