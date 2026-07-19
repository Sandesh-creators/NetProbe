package com.netprobe.diagnostics.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprobe.diagnostics.data.model.ChannelOccupancy
import com.netprobe.diagnostics.data.model.CongestionLevel
import com.netprobe.diagnostics.data.model.WifiBand
import com.netprobe.diagnostics.ui.theme.*

@Composable
fun ChannelCongestionChart(
    occupancy: List<ChannelOccupancy>,
    modifier: Modifier = Modifier,
    bandFilter: WifiBand? = null
) {
    val filtered = remember(occupancy, bandFilter) {
        if (bandFilter != null) occupancy.filter { it.band == bandFilter }
        else occupancy
    }

    val maxCount = remember(filtered) {
        (filtered.maxOfOrNull { it.networkCount } ?: 1).coerceAtLeast(1)
    }

    val barColors = remember(filtered) {
        filtered.map { occ ->
            when (occ.congestionLevel) {
                CongestionLevel.CLEAR -> CongestionClear
                CongestionLevel.LOW -> CongestionLow
                CongestionLevel.MODERATE -> CongestionModerate
                CongestionLevel.HIGH -> CongestionHigh
                CongestionLevel.CRITICAL -> CongestionCritical
            }
        }
    }

    val animatedProgress = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "chart_anim"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (filtered.isEmpty()) return@Canvas

                val barCount = filtered.size
                val totalBarSpace = size.width - 40f
                val barWidth = totalBarSpace / barCount * 0.65f
                val gap = totalBarSpace / barCount * 0.35f
                val chartHeight = size.height - 10f

                for (i in 0..4) {
                    val y = chartHeight - (chartHeight * i / 4f)
                    drawLine(
                        color = SurfaceOverlay,
                        start = Offset(20f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5f
                    )
                }

                filtered.forEachIndexed { index, occ ->
                    val barHeight = (occ.networkCount.toFloat() / maxCount) * chartHeight * animatedProgress.value
                    val x = 25f + index * (barWidth + gap)
                    val y = chartHeight - barHeight

                    drawRoundRect(
                        color = TerminalDim,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(3f, 3f)
                    )

                    drawRoundRect(
                        color = barColors[index],
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            if (filtered.isNotEmpty()) {
                val barCount = filtered.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(start = 25.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    filtered.forEach { occ ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (occ.networkCount > 0) {
                                Text(
                                    text = "${occ.networkCount}",
                                    fontSize = 9.sp,
                                    color = when (occ.congestionLevel) {
                                        CongestionLevel.CLEAR -> CongestionClear
                                        CongestionLevel.LOW -> CongestionLow
                                        CongestionLevel.MODERATE -> CongestionModerate
                                        CongestionLevel.HIGH -> CongestionHigh
                                        CongestionLevel.CRITICAL -> CongestionCritical
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${occ.channel}",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        val axisLabel = if (bandFilter == WifiBand.BAND_5_GHZ) "5 GHz Channels" else "2.4 GHz Channels"
        Text(
            text = axisLabel,
            fontSize = 10.sp,
            color = TextDisabled,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun CongestionLegend(modifier: Modifier = Modifier) {
    val items = listOf(
        "Clear" to CongestionClear,
        "Low" to CongestionLow,
        "Moderate" to CongestionModerate,
        "High" to CongestionHigh,
        "Critical" to CongestionCritical
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
