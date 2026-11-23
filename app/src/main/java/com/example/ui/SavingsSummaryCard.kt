package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TableRowSlot
import com.example.data.formatRupiah
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun SavingsSummaryCard(
    data: SavingsDashboardData,
    isExportingPdf: Boolean,
    onAutoFill: () -> Unit,
    onEditConfig: () -> Unit,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onJumpToRow: (Int) -> Unit,
) {
    var showChart by remember { mutableStateOf(false) }

    val nextEmptySlot = remember(data.allSlots) {
        data.allSlots.firstOrNull { !it.hasContent }
    }
    val averagePerFilled = remember(data.totalSaved, data.filledCount) {
        if (data.filledCount > 0) data.totalSaved / data.filledCount else 0L
    }
    val remainingToTarget = remember(data.totalSaved, data.config.targetAmount) {
        if (data.config.targetAmount > 0L) {
            (data.config.targetAmount - data.totalSaved).coerceAtLeast(0L)
        } else {
            0L
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Terkumpul",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatRupiah(data.totalSaved),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (data.config.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = data.config.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "${data.filledCount}/${data.allSlots.size} terisi",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (data.config.targetAmount > 0L) {
                val rawProgress = (data.totalSaved.toFloat() / data.config.targetAmount.toFloat())
                    .coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = spring(),
                    label = "target_progress",
                )
                val percentInt = (rawProgress * 100).toInt()

                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Target: ${formatRupiah(data.config.targetAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (remainingToTarget == 0L && data.totalSaved > 0L) {
                            "100% Tercapai!"
                        } else {
                            "$percentInt% • Kurang ${formatRupiah(remainingToTarget)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (percentInt >= 100) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(25, 50, 75, 100).forEach { milestone ->
                        val reached = percentInt >= milestone
                        Surface(
                            color = if (reached) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            contentColor = if (reached) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (reached) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                }
                                Text(
                                    text = "$milestone%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // Compact Analytics Strip
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(
                            text = "Rata-rata / Baris",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatRupiah(averagePerFilled),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                FilterChip(
                    selected = showChart,
                    onClick = { showChart = !showChart },
                    label = { Text(if (showChart) "Tutup Grafik" else "Grafik") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.testTag("toggle_chart_chip"),
                )
            }

            AnimatedVisibility(visible = showChart) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Distribusi Nominal per Baris (1–${data.allSlots.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    SavingsBarSparkline(
                        slots = data.allSlots,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp),
                    )
                }
            }

            if (nextEmptySlot != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onJumpToRow(nextEmptySlot.rowIndex) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("jump_next_empty_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Isi Baris Kosong Berikutnya (#${nextEmptySlot.rowIndex})")
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onAutoFill,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_autofill_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Otomatis", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onEditConfig,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hero_config_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Pengaturan Target",
                        modifier = Modifier.size(16.dp),
                    )
                }
                OutlinedButton(
                    onClick = onSharePdf,
                    enabled = !isExportingPdf,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hero_share_pdf_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan PDF",
                        modifier = Modifier.size(16.dp),
                    )
                }
                Button(
                    onClick = onOpenPdf,
                    enabled = !isExportingPdf,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hero_open_pdf_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Buka PDF", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SavingsBarSparkline(
    slots: List<TableRowSlot>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.outlineVariant
    val maxNominal = remember(slots) {
        (slots.maxOfOrNull { it.nominal } ?: 0L).coerceAtLeast(1L)
    }

    Canvas(modifier = modifier) {
        val count = slots.size.coerceAtLeast(1)
        val gap = (size.width / count) * 0.28f
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1.5f)
        val baselineY = size.height

        slots.forEachIndexed { idx, slot ->
            val x = idx * (barWidth + gap)
            if (slot.nominal > 0L) {
                val ratio = (slot.nominal.toFloat() / maxNominal.toFloat()).coerceIn(0.08f, 1f)
                val barHeight = size.height * ratio
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, baselineY - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            } else {
                val stubH = 4f
                drawRoundRect(
                    color = emptyBarColor,
                    topLeft = Offset(x, baselineY - stubH),
                    size = Size(barWidth, stubH),
                    cornerRadius = CornerRadius(1f, 1f),
                )
            }
        }
    }
}
