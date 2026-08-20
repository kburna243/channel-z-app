package com.example.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.ChatLayout
import com.example.player.PlayerViewModel

/**
 * Leere Gegenstuecke fuer die Light-Ausgabe.
 *
 * Die Light-Variante ist zum Zuschauen am Fernseher gedacht — mitreden geht in der Full-Ausgabe.
 * Weil die Funktionen hier leer sind, faellt der gesamte Chat-Code beim Bauen weg statt nur
 * ausgeblendet zu werden.
 */
@Composable
fun ChatFeatureOverlays(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    // absichtlich leer
}

/** In der Light-Ausgabe gibt es nichts zu tun. */
fun handleChatKey(viewModel: PlayerViewModel, keyCode: Int): Boolean = false

/**
 * Die Light-Ausgabe zeigt den Chat immer als Untertitel-Zeile, so wie sie es immer getan hat.
 * Andere Ansichten gibt es hier nicht — wer den Chat als Spalte oder in voller Breite will,
 * nimmt die Full-Ausgabe auf dem Handy.
 */
fun showsSubtitleChat(layout: ChatLayout): Boolean = true
