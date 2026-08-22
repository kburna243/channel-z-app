package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Die Farben der Oberflaeche liegen als beobachtbarer Zustand vor, nicht als Konstanten.
 *
 * Dadurch kostet ein Themenwechsel keine einzige Aenderung an den rund 300 Stellen, die diese
 * Namen benutzen: Compose merkt sich beim Zeichnen, wer welche Farbe gelesen hat, und zeichnet
 * genau diese Stellen neu, sobald [applyPalette] andere Werte einsetzt. Ein App-weites Thema
 * gibt es immer nur einmal, deshalb ist globaler Zustand hier richtig; ein CompositionLocal
 * waere derselbe Effekt mit 300 Zeilen Umbau.
 *
 * Geschrieben wird ausschliesslich ausserhalb der Composition — beim Start und beim Umschalten
 * in den Einstellungen. Waehrend des Zeichnens zu schreiben wuerde einen zusaetzlichen
 * Zeichendurchlauf ausloesen.
 */

// ------------------------------------------------------------------ Flaechen
var MidnightCanvas by mutableStateOf(Color(0xFF0B0813))
    private set
var SurfaceDark by mutableStateOf(Color(0xFF161124))
    private set
var SurfaceCard by mutableStateOf(Color(0xFF1E1830))
    private set
/** Fokussierte Kachel. Zieht zur Markenfarbe: der alte Wert lag 1,15:1 neben der
 *  ruhenden Kachel und war am Fernseher nicht als Fokus zu erkennen. */
var CardFocusedSurface by mutableStateOf(Color(0xFF4F3085))
    private set

// ------------------------------------------------------------------ Akzente
/**
 * Historischer Name: dies ist der Hauptakzent — aktive Schalter, Fokusrahmen, Buttons.
 * Er traegt haeufig schwarzen Text, muss also in jeder Palette hell genug bleiben.
 */
var AccentIceBlue by mutableStateOf(Color(0xFF9D65FF))
    private set

/** Markenfarbe (dunkler als der Akzent), fuer Logo, Systemzeilen und aktive Menuepunkte. */
var AccentVibrantOrange by mutableStateOf(Color(0xFF633AA8))
    private set
var AccentPurple by mutableStateOf(Color(0xFF633AA8))
    private set
var AccentLavender by mutableStateOf(Color(0xFFC4A8FF))
    private set
var AccentDeepViolet by mutableStateOf(Color(0xFF4B2C85))
    private set

// Bleiben ueber alle Paletten gleich: sie unterscheiden Chat-Teilnehmer voneinander und
// duerfen dabei nicht mit der Markenfarbe verschmelzen.
val AccentCoral = Color(0xFFFF8A8A)
val AccentAmber = Color(0xFFFFB300)

// ------------------------------------------------------------------ Text
var PureWhite by mutableStateOf(Color(0xFFF5F3F7))
    private set
var TextSubtitleWhite by mutableStateOf(Color(0xFFF5F3F7))
    private set
var TextMuted by mutableStateOf(Color(0xFF9A93A8))
    private set

// ------------------------------------------------------------------ Fokus & Rahmen
var FocusGlowIceBlue by mutableStateOf(Color(0xFF9D65FF))
    private set
var FocusBorderRing by mutableStateOf(Color(0xFFC4A8FF))
    private set
val SubtleBorder = Color(0x26FFFFFF)

// ------------------------------------------------------------------ Status
// Bedeutungstragend (verbunden / verbindet / getrennt) und deshalb bewusst themenunabhaengig.
val StatusLiveGreen = Color(0xFF4ADE80)
val StatusLiveGreenBg = Color(0x334ADE80)
val StatusReconnectingYellow = Color(0xFFFBBF24)
val StatusOfflineRed = Color(0xFFF87171)
val StatusIdleBlue = Color(0xFF60A5FA)

// Klassische CyTube-Palette fuer die Chat-Namen (Alternative zur Grindhouse-Darstellung)
val ClassicCyan = Color(0xFF1E90FF)
val ClassicOrange = Color(0xFFFF4500)
val ClassicGreen = Color(0xFF2ECC71)
val ClassicAmber = Color(0xFFF39C12)
val ClassicSystem = Color(0xFFFFB300)

/** Ein vollstaendiges Farbthema. Die Kommentare nennen die Namen aus der Designvorlage. */
data class ThemePalette(
    val id: String,
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val cardFocused: Color,
    val accent: Color,
    val brand: Color,
    val lavender: Color,
    val deepViolet: Color,
    val textPrimary: Color,
    val textMuted: Color
)

/**
 * Die vier waehlbaren Themen. Reihenfolge = Reihenfolge im Menue, das erste ist die Vorgabe.
 *
 * Alle drei neuen Paletten setzen auf tiefe, rauchige Violetttoene statt greller Neonfarben:
 * Filmplakate und Vorschaubilder sollen aus dem Hintergrund herausleuchten, nicht mit ihm
 * konkurrieren. Reines Schwarz kommt bewusst nirgends vor, es laesst Violett schmutzig wirken.
 */
val Palettes = listOf(
    // "The Cinematic Deep" — fast schwarzes Violett, damit Poster herausstechen.
    ThemePalette(
        id = "cinematic",
        background = Color(0xFF0B0813),   // Midnight Obsidian
        surface = Color(0xFF161124),      // Deep Iris
        surfaceCard = Color(0xFF1E1830),
        cardFocused = Color(0xFF4F3085),
        accent = Color(0xFF9D65FF),       // Electric Amethyst
        brand = Color(0xFF633AA8),        // Regal Violet
        lavender = Color(0xFFC4A8FF),
        deepViolet = Color(0xFF4B2C85),
        textPrimary = Color(0xFFF5F3F7),  // Crisp Silk
        textMuted = Color(0xFF9A93A8)
    ),
    // "Premium Cyber Punk" — kraeftiger und kaelter, fuer Live-Events und Clips.
    ThemePalette(
        id = "cyberpunk",
        background = Color(0xFF0D0C15),   // Dark Eclipse
        surface = Color(0xFF1C182E),      // Cyber Velvet
        surfaceCard = Color(0xFF241F3A),
        cardFocused = Color(0xFF612898),
        accent = Color(0xFFE0AAFF),       // Neon Orchid
        brand = Color(0xFF7B2CBF),        // Royal Plum
        lavender = Color(0xFFB388FF),
        deepViolet = Color(0xFF5A1E90),
        textPrimary = Color(0xFFFFFFFF),  // Pure Ice
        textMuted = Color(0xFFA9A2BC)
    ),
    // "Mystic Editorial" — warmes Schwarzviolett, ruhig und erwachsen, fuer Dokus und Arthouse.
    ThemePalette(
        id = "editorial",
        background = Color(0xFF0E0911),   // Shadow Blackberry
        surface = Color(0xFF22162B),      // Muted Plum
        surfaceCard = Color(0xFF2A1C35),
        cardFocused = Color(0xFF523F62),
        accent = Color(0xFFC77DFF),       // Soft Lavender
        brand = Color(0xFF451E3E),        // Antique Violet
        lavender = Color(0xFFDDB8FF),
        deepViolet = Color(0xFF451E3E),
        textPrimary = Color(0xFFEAE6E8),  // Warm Ash
        textMuted = Color(0xFF9C919A)
    ),
    // Der bisherige Look, damit niemand das gewohnte Bild verliert.
    ThemePalette(
        id = "channelz",
        background = Color(0xFF050505),
        surface = Color(0xFF141418),
        surfaceCard = Color(0xFF1A1A22),
        cardFocused = Color(0xFF543174),
        accent = Color(0xFFA8C7FA),
        brand = Color(0xFF9D4EDD),
        lavender = Color(0xFFC4A8FF),
        deepViolet = Color(0xFF7B2CBF),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF9AA0A6)
    )
)

val DefaultPaletteId: String = Palettes.first().id

fun paletteOf(id: String): ThemePalette = Palettes.firstOrNull { it.id == id } ?: Palettes.first()

/** Setzt das Thema. Nur ausserhalb der Composition aufrufen (Start, Einstellungen). */
fun applyPalette(id: String) {
    val p = paletteOf(id)
    MidnightCanvas = p.background
    SurfaceDark = p.surface
    SurfaceCard = p.surfaceCard
    CardFocusedSurface = p.cardFocused
    AccentIceBlue = p.accent
    AccentVibrantOrange = p.brand
    AccentPurple = p.brand
    AccentLavender = p.lavender
    AccentDeepViolet = p.deepViolet
    PureWhite = p.textPrimary
    TextSubtitleWhite = p.textPrimary
    TextMuted = p.textMuted
    FocusGlowIceBlue = p.accent
    FocusBorderRing = p.lavender
}
