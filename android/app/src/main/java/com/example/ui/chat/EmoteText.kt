package com.example.ui.chat

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import coil.compose.AsyncImage
import com.example.data.model.ChannelEmote

/**
 * Zerlegt eine Chat-Nachricht in Text und Kanal-Emotes.
 *
 * Der Kanal meldet rund 1700 Emotes. Jede Nachricht gegen alle Muster zu pruefen waere
 * verschwenderisch, deshalb wird sie an Leerzeichen zerlegt und jedes Wort nachgeschlagen —
 * so stehen Emotes im Chat ohnehin fast immer. Geladen wird nur, was tatsaechlich vorkommt.
 */
fun buildEmoteString(
    text: String,
    emotes: Map<String, String>,
    fontSizeSp: TextUnit
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    if (emotes.isEmpty() || text.isBlank()) {
        return AnnotatedString(text) to emptyMap()
    }

    val inline = mutableMapOf<String, InlineTextContent>()
    val annotated = buildAnnotatedString {
        val tokens = text.split(" ")
        tokens.forEachIndexed { index, token ->
            val url = emotes[token]
            if (url != null) {
                val id = "emote:$token"
                appendInlineContent(id, token)
                if (id !in inline) {
                    inline[id] = InlineTextContent(
                        Placeholder(
                            width = 1.8.em,
                            height = 1.8.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = token,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                append(token)
            }
            if (index < tokens.lastIndex) append(" ")
        }
    }
    return annotated to inline
}

/** Nachschlagetabelle aus der Emote-Liste des Kanals. */
@Composable
fun rememberEmoteMap(emotes: List<ChannelEmote>): Map<String, String> =
    remember(emotes) { emotes.associate { it.name to it.imageUrl } }
