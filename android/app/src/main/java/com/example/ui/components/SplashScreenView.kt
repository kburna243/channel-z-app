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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * Animated Retro Synthwave / Grindhouse Splash & Loading Screen.
 * Uses the composite artwork from splashscreen_combi.psd with an animated, filling neon progress bar.
 */
@Composable
fun SplashScreenView(
    onFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E)),
        contentAlignment = Alignment.Center
    ) {
        // 1. Splash Screen Main Background Artwork (Characters + Neon Sign)
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = "Channel-Z Splash Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. CRT Television Scanlines Overlay
        ScanlinesOverlay(modifier = Modifier.fillMaxSize())

        // 3. Subtle Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // 4. Center Animated Loading Unit (Text + Filling Capsule Bar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 44.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedRetroLoadingBar(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(0.58f)
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
                text = "[PLAY ▶]",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88).copy(alpha = 0.85f)
                )
            )
            Text(
                text = "STEREO • HI-FI",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00DDFF).copy(alpha = 0.75f)
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
                color = Color.Black.copy(alpha = 0.20f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            y += scanlineSpacing
        }
    }
}

/**
 * Animated Neon Progress Bar with filling segments and pulsing "LOADING..." header.
 */
@Composable
fun AnimatedRetroLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashNeonPulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Neon "LOADING..." Graphic with gentle pulsing glow
        Image(
            painter = painterResource(id = R.drawable.splash_loading_text),
            contentDescription = "Loading...",
            modifier = Modifier.height(48.dp),
            alpha = pulseAlpha
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Capsule Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // A. Base Container / Frame
            Image(
                painter = painterResource(id = R.drawable.splash_capsule_frame),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // B. Animated Green Segments Fill (Clipped by progress fraction)
            Image(
                painter = painterResource(id = R.drawable.splash_capsule_fill),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val fillWidth = size.width * progress.coerceIn(0f, 1f)
                        clipRect(0f, 0f, fillWidth, size.height) {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monospace percentage counter & status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CYTUBE LIVE FEED // INITIALIZING",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF55BB).copy(alpha = 0.9f),
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "${(progress * 100).toInt()}%",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00FF66),
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

/**
 * Backwards compatibility alias for ChannelZRetroLoadingBar.
 */
@Composable
fun ChannelZRetroLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    AnimatedRetroLoadingBar(progress = progress, modifier = modifier)
}
