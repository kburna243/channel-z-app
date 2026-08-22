package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.AccentVibrantOrange
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark

/**
 * Geometric Balance streaming background with high-res artwork asset (PlayerBackground.png)
 * and modern cinematic typography.
 */
@Composable
fun VortexBackground(
    modifier: Modifier = Modifier,
    titleText: String = "CHANNEL-Z",
    subtitleText: String? = null,
    showAnimation: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GeometricAnimation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightCanvas),
        contentAlignment = Alignment.Center
    ) {
        // 1. High-Resolution Player Background Artwork
        Image(
            painter = painterResource(id = R.drawable.splash_screen_image),
            contentDescription = "Player Background Artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Subtle Dark Tint Overlay for Text Contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // 3. Geometric Concentric Dashed Orbital Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (showAnimation) rotation else 0f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.width.coerceAtLeast(size.height)) * 0.7f

            val ringCount = 8
            for (i in 1..ringCount) {
                val radius = (maxRadius / ringCount) * i
                val alpha = (0.08f + (i * 0.02f)) * pulseGlow
                val isDashed = i % 2 == 0

                drawCircle(
                    color = if (i % 3 == 0) AccentVibrantOrange.copy(alpha = alpha * 0.8f) else AccentIceBlue.copy(alpha = alpha),
                    center = center,
                    radius = radius,
                    style = Stroke(
                        width = 2f,
                        pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
                    )
                )
            }
        }

        // Center Content Container
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Geometric Center Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard.copy(alpha = 0.85f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentIceBlue.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AccentIceBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = titleText,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = 4.sp,
                    color = PureWhite
                ),
                textAlign = TextAlign.Center
            )

            if (!subtitleText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = SurfaceDark.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
                ) {
                    Text(
                        text = subtitleText.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AccentIceBlue,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
