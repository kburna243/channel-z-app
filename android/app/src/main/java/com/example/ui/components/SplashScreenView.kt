package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * Channel-Z Retro Trash Player Splash & Loading Screen.
 * Inspired by classic VHS tracking, cyberpunk neon aesthetics, and the Stitch Trash Player concept.
 */
@Composable
fun SplashScreenView(
    onFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
        label = "channelZProgress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131313)),
        contentAlignment = Alignment.Center
    ) {
        // 1. Splash Screen Background Artwork
        Image(
            painter = painterResource(id = R.drawable.splash_screen_image),
            contentDescription = "Channel-Z Splash Artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Scanline & Atmosphere Overlay
        ScanlinesOverlay(modifier = Modifier.fillMaxSize())

        // 3. Dark Gradient Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 4. Center Loading Canvas & Progress Unit
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            ChannelZRetroLoadingBar(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
            )
        }

        // 5. VCR Bottom Status Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[SP]",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00DDDD).copy(alpha = 0.75f)
                )
            )
            Text(
                text = "0:00:00",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00DDDD).copy(alpha = 0.75f)
                )
            )
            Text(
                text = "STEREO",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00DDDD).copy(alpha = 0.75f)
                )
            )
        }
    }
}

/**
 * Scanlines visual overlay replicating vintage CRT television scanlines.
 */
@Composable
fun ScanlinesOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scanlineSpacing = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.22f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            y += scanlineSpacing
        }
    }
}

/**
 * Stitch Trash Player Retro VHS Loading Bar with spinning Z reel,
 * glitch text header, neon progress bar, and blinking status chip.
 */
@Composable
fun ChannelZRetroLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "channelZTransitions")

    val reelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelSpin"
    )

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusBlink"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barPulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotating Film Reel with Neon Glow and Center 'Z'
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color(0xFFFFABF3), spotColor = Color(0xFFFFABF3)),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Reel Graphics
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(reelRotation)
            ) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer circle
                drawCircle(
                    color = Color(0xFFFFABF3),
                    radius = radius - 4.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 4 Spokes / Holes
                for (angle in listOf(0.0, 90.0, 180.0, 270.0)) {
                    val rad = Math.toRadians(angle)
                    val spokeX = center.x + (radius * 0.55f * Math.cos(rad)).toFloat()
                    val spokeY = center.y + (radius * 0.55f * Math.sin(rad)).toFloat()
                    drawCircle(
                        color = Color(0xFF00DDDD),
                        radius = 4.dp.toPx(),
                        center = Offset(spokeX, spokeY)
                    )
                }
            }

            // Central Glowing 'Z'
            Text(
                text = "Z",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color(0xFF00DDDD)
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Glitch Text Header: "LOADING MOVIE..."
        GlitchHeader(
            text = "LOADING MOVIE...",
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Segmented VHS Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                .border(2.dp, Color(0xFF00DDDD), RoundedCornerShape(4.dp))
                .padding(3.dp)
        ) {
            // Animated Progress Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress.coerceIn(0.01f, 1.0f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6C0DBF),
                                Color(0xFFFFABF3),
                                Color(0xFF00DDDD)
                            )
                        )
                    )
            )

            // Tracking Markers (4 vertical lines)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(14.dp)
                            .background(Color(0xFF00DDDD).copy(alpha = 0.65f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Blinking Status Chip [PLEASE WAIT]
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(2.dp),
                    spotColor = Color(0xFFFF0000)
                )
                .background(Color(0xFF6C0DBF), RoundedCornerShape(2.dp))
                .border(2.dp, Color(0xFFCCC597), RoundedCornerShape(2.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "[PLEASE WAIT]",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFFFFFFF).copy(alpha = if (blinkAlpha > 0.4f) 1f else 0.2f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Glitch Text Header with simulated chromatic aberration / RGB shift.
 */
@Composable
private fun GlitchHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Red Shadow Offset
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = Color(0xFFFF0000).copy(alpha = 0.8f)
            ),
            modifier = Modifier.offset { IntOffset(x = 2, y = 2) }
        )

        // Cyan Shadow Offset
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = Color(0xFF00DDDD).copy(alpha = 0.8f)
            ),
            modifier = Modifier.offset { IntOffset(x = -2, y = -1) }
        )

        // Main VHS Yellow Text
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = Color(0xFFCCC597)
            )
        )
    }
}
