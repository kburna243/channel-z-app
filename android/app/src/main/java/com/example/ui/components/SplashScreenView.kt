package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * Authentic 1970s/1980s Retro Grindhouse Cinema Marquee Loading Bar.
 * Hand-crafted vintage theater sign with incandescent marquee light bulbs,
 * aged cream marquee panel, crimson neon glow, and bold comic-book outlines.
 */
@Composable
fun SplashScreenView(
    onFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "marqueeProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bulbChaser")
    val bulbFlash by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bulbFlash"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2300)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 1. Splash Screen Artwork (Full bleed)
        Image(
            painter = painterResource(id = R.drawable.splash_screen_image),
            contentDescription = "Grindhouse 420 Cinema",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Vintage Vignette & Atmosphere
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        // 3. Vintage Grindhouse Marquee Sign Loading Element
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 44.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            GrindhouseMarqueeLoadingBar(
                progress = animatedProgress,
                leadBulbFlash = bulbFlash,
                modifier = Modifier
                    .fillMaxWidth(0.56f)
                    .shadow(16.dp, RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun GrindhouseMarqueeLoadingBar(
    progress: Float,
    leadBulbFlash: Float,
    modifier: Modifier = Modifier
) {
    val totalBulbs = 16
    val activeBulbsCount = (progress * totalBulbs).toInt().coerceIn(0, totalBulbs)

    // Outer Chunky Marquee Theater Frame with Crimson Neon Glow
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F0B10))
            .border(3.dp, Color(0xFF1C1318), RoundedCornerShape(10.dp))
            .border(1.5.dp, Color(0xFFD61828).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Inner Vintage Marquee Aged Cream / Gold Board
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF9F2DC),
                            Color(0xFFE8D7B5),
                            Color(0xFFD8C29D)
                        )
                    )
                )
                .border(2.dp, Color(0xFF281C10), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Retro Grindhouse Theater Title
            Text(
                text = "★ 420 GRINDHOUSE THEATRE ★",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = Color(0xFF1A0A05)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "CULT CINEMA CONTINUOUS SHOWING",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF8B1A1A) // Crimson ink stamp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Marquee Bulb Chase Strip (Vintage Incandescent Light Bulbs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF140D0B))
                    .border(1.5.dp, Color(0xFF3A241A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalBulbs) {
                    val isActive = i < activeBulbsCount
                    val isLeadBulb = i == activeBulbsCount - 1 && isActive
                    val bulbAlpha = if (isLeadBulb) leadBulbFlash else 1.0f

                    VintageMarqueeBulb(
                        isActive = isActive,
                        alpha = bulbAlpha
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vintage Ticket Stamped Loading Progress Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ADMIT ONE // 35MM REEL",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A3425)
                    )
                )

                Text(
                    text = "REEL LOADING: ${(progress * 100).toInt()}%",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFF9E1010)
                    )
                )
            }
        }
    }
}

/**
 * Individual vintage incandescent marquee light bulb with warm filament glow.
 */
@Composable
private fun VintageMarqueeBulb(
    isActive: Boolean,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                if (isActive) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFFFEA79).copy(alpha = alpha),
                            Color(0xFFFF9800).copy(alpha = alpha),
                            Color(0xFFB25000)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF382215),
                            Color(0xFF20130B),
                            Color(0xFF0F0804)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFFFFD54F) else Color(0xFF422818),
                shape = CircleShape
            )
    )
}
