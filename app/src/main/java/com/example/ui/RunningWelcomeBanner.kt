package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.roundToInt

@Composable
fun RunningWelcomeBanner(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.welcome_marquee_text),
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var textWidthPx by remember { mutableIntStateOf(0) }
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(containerWidthPx, textWidthPx, text) {
        if (containerWidthPx > 0 && textWidthPx > 0) {
            val speedPxPerSec = with(density) { 64.dp.toPx() }
            val totalDistance = (containerWidthPx + textWidthPx).toFloat()
            val durationMillis = ((totalDistance / speedPxPerSec) * 1000f)
                .toInt()
                .coerceAtLeast(3500)
            while (true) {
                offsetX.snapTo(containerWidthPx.toFloat())
                offsetX.animateTo(
                    targetValue = -textWidthPx.toFloat(),
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing,
                    ),
                )
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = text }
            .testTag("running_welcome_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(26.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
                    .onSizeChanged { containerWidthPx = it.width },
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .onSizeChanged { textWidthPx = it.width }
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) },
                )
            }
        }
    }
}
