package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ConnectionStatus
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusIdleBlue
import com.example.ui.theme.StatusLiveGreen
import com.example.ui.theme.StatusLiveGreenBg
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusReconnectingYellow
import com.example.ui.theme.SubtleBorder

@Composable
fun StatusIndicatorDot(
    status: ConnectionStatus,
    userCount: Int = 0,
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    val (dotColor, statusText, containerBg) = when (status) {
        ConnectionStatus.LIVE -> Triple(StatusLiveGreen, stringResource(R.string.status_live), StatusLiveGreenBg)
        ConnectionStatus.RECONNECTING -> Triple(StatusReconnectingYellow, stringResource(R.string.status_reconnecting), Color(0x33FBBF24))
        ConnectionStatus.OFFLINE -> Triple(StatusOfflineRed, stringResource(R.string.status_offline), Color(0x33F87171))
        ConnectionStatus.IDLE -> Triple(StatusIdleBlue, stringResource(R.string.status_idle), Color(0x3360A5FA))
    }

    Surface(
        modifier = modifier
            .testTag("status_indicator_badge")
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = status == ConnectionStatus.OFFLINE) { onRetryClick() },
        color = containerBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .scale(if (status == ConnectionStatus.RECONNECTING || status == ConnectionStatus.LIVE) pulseScale else 1.0f)
                    .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = statusText.uppercase(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = dotColor
                )
            )

            if (userCount > 0 && status == ConnectionStatus.LIVE) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• $userCount",
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = PureWhite.copy(alpha = 0.85f)
                    )
                )
            }
        }
    }
}
