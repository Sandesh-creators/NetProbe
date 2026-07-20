package com.netprobe.diagnostics.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprobe.diagnostics.data.db.dao.DeviceRssiSummary
import com.netprobe.diagnostics.ui.theme.*

fun rssiToHeatColor(rssi: Int): Color {
    return when {
        rssi > -40 -> HeatmapHot
        rssi > -60 -> HeatmapWarm
        rssi > -80 -> HeatmapCool
        else -> HeatmapCold
    }
}

@Composable
fun SignalHeatmapGrid(
    summaries: List<DeviceRssiSummary>,
    selectedDevice: String?,
    onDeviceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = 4
    val rows = (summaries.size + columns - 1) / columns

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardDark)
            .border(1.dp, SurfaceOverlay, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        if (summaries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "START COLLECTION TO VIEW HEATMAP",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabled
                )
            }
        } else {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < summaries.size) {
                            val summary = summaries[index]
                            val isSelected = selectedDevice == summary.sourceAddress
                            val heatColor = rssiToHeatColor(summary.avgRssi)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(heatColor.copy(alpha = 0.15f))
                                    .border(
                                        1.dp,
                                        if (isSelected) TerminalGreen else heatColor.copy(alpha = 0.3f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onDeviceSelected(summary.sourceAddress) }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Canvas(modifier = Modifier.size(24.dp)) {
                                    drawCircle(
                                        color = heatColor,
                                        radius = size.minDimension / 2
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${summary.avgRssi}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = heatColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = summary.sourceName?.take(8) ?: summary.sourceAddress.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = TextDisabled,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(HeatmapHot, "Strong")
            LegendItem(HeatmapWarm, "Good")
            LegendItem(HeatmapCool, "Weak")
            LegendItem(HeatmapCold, "Poor")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextDisabled)
    }
}

@Composable
fun RssiTimeSeriesChart(
    summaries: List<DeviceRssiSummary>,
    selectedDevice: String?,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardDark)
            .border(1.dp, SurfaceOverlay, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        if (summaries.isEmpty()) return@Canvas

        val minRssi = -100f
        val maxRssi = -20f
        val range = maxRssi - minRssi

        summaries.forEachIndexed { index, summary ->
            val x = (size.width / (summaries.size + 1)) * (index + 1)
            val normalizedRssi = (summary.avgRssi - minRssi) / range
            val y = size.height * (1f - normalizedRssi.coerceIn(0f, 1f))
            val color = rssiToHeatColor(summary.avgRssi)
            val isHighlighted = selectedDevice == summary.sourceAddress

            drawCircle(
                color = color,
                radius = if (isHighlighted) 8f else 5f,
                center = Offset(x, y)
            )

            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(x, y),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }

        for (rssi in listOf(-90, -70, -50, -30)) {
            val y = size.height * (1f - (rssi - minRssi) / range)
            drawLine(
                color = Color(0xFF26262B),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
    }
}
