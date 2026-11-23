package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DEFAULT_DATA_ROWS
import com.example.data.TableRowSlot
import com.example.data.TableSummaryInfo
import com.example.data.formatNumberOnly
import com.example.data.formatRupiah
import com.example.ui.theme.JetBrainsMonoFamily

private const val COL_TABUNGAN_WEIGHT = 0.28f
private const val COL_NOMINAL_WEIGHT = 0.23f
private const val COL_TANGGAL_WEIGHT = 0.24f
private const val COL_KETERANGAN_WEIGHT = 0.25f

private val TableHeaderShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
private val TableBottomShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)

/**
 * Komponen tabel 4 kolom (Tabungan, Nominal, Tanggal/Bulan/Tahun, Keterangan) dengan dukungan
 * default 61 baris (1 baris header sticky + 60 baris data) berbasis [LazyColumn] yang dioptimalkan:
 * - Menggunakan stable `key` (`rowIndex`) dan `contentType` untuk daur ulang slot Compose.
 * - Menggunakan model data `@Immutable` ([TableRowSlot]) dan `remember` untuk meminimalkan rekomposisi.
 * - Menggambar garis pemisah kolom langsung di `drawBehind` untuk mengurangi node layout per baris.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavingsLedgerTable(
    slots: List<TableRowSlot>,
    onRowClick: (TableRowSlot) -> Unit,
    modifier: Modifier = Modifier,
    totalTableRows: Int = DEFAULT_DATA_ROWS + 1,
    summaryInfo: TableSummaryInfo? = null,
    listState: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null,
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val rowBorder = remember(outlineColor) { BorderStroke(0.5.dp, outlineColor) }
    val emptyBorder = remember(outlineColor) { BorderStroke(1.dp, outlineColor) }

    val colTabunganLabel = stringResource(R.string.col_tabungan)
    val colNominalLabel = stringResource(R.string.col_nominal)
    val colTanggalLabel = stringResource(R.string.col_tanggal)
    val colKeteranganLabel = stringResource(R.string.col_keterangan)
    val emptyRowHint = stringResource(R.string.empty_row_hint)

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("savings_ledger_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        if (headerContent != null) {
            item(
                key = "summary_header",
                contentType = "summary_header",
            ) {
                headerContent()
            }
        }

        // Baris 1 dari 61: Kartu Ringkasan Total Nominal + Header Sticky 4 Kolom
        stickyHeader(
            key = "table_header_row",
            contentType = "table_header",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
            ) {
                if (summaryInfo != null) {
                    SavingsTableTopSummaryCard(
                        summaryInfo = summaryInfo,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                }
                SavingsTableHeaderRow(
                    colTabunganLabel = colTabunganLabel,
                    colNominalLabel = colNominalLabel,
                    colTanggalLabel = colTanggalLabel,
                    colKeteranganLabel = colKeteranganLabel,
                    dividerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.24f),
                )
            }
        }

        if (slots.isEmpty()) {
            item(
                key = "empty_filter",
                contentType = "empty_state",
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = emptyBorder,
                    shape = TableBottomShape,
                ) {
                    Text(
                        text = "Tidak ada baris yang sesuai dengan filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        } else {
            // Baris 2..61: 60 Baris Data dengan key & contentType agar performa scroll tetap optimal
            itemsIndexed(
                items = slots,
                key = { _, slot -> slot.rowIndex },
                contentType = { _, _ -> "table_data_row" },
            ) { index, slot ->
                SavingsTableDataRow(
                    slot = slot,
                    isLast = index == slots.lastIndex,
                    emptyRowHint = emptyRowHint,
                    rowBorder = rowBorder,
                    columnDividerColor = outlineColor,
                    onClick = { onRowClick(slot) },
                )
            }
        }

        item(
            key = "table_footer_note",
            contentType = "table_footer",
        ) {
            Text(
                text = "Total $totalTableRows baris (1 baris header + ${totalTableRows - 1} baris data). Ketuk baris mana saja untuk mengisi atau mengedit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun SavingsTableTopSummaryCard(
    summaryInfo: TableSummaryInfo,
    modifier: Modifier = Modifier,
) {
    val totalFormatted = remember(summaryInfo.totalNominal) {
        formatRupiah(summaryInfo.totalNominal)
    }
    val filteredFormatted = remember(summaryInfo.filteredNominal, summaryInfo.isFiltered) {
        if (summaryInfo.isFiltered) formatRupiah(summaryInfo.filteredNominal) else ""
    }
    val cardBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("table_top_summary_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.padding(7.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.table_summary_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = totalFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("table_top_summary_total"),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "${summaryInfo.filledRows}/${summaryInfo.totalDataRows} baris",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    if (summaryInfo.targetAmount > 0L) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (summaryInfo.remainingToTarget == 0L && summaryInfo.totalNominal > 0L) {
                                "Target tercapai"
                            } else {
                                "${summaryInfo.completionPercent}% dari target"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (summaryInfo.targetAmount > 0L) {
                val progress = (summaryInfo.totalNominal.toFloat() / summaryInfo.targetAmount.toFloat())
                    .coerceIn(0f, 1f)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            if (summaryInfo.isFiltered) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Subtotal filter (${summaryInfo.filteredRows} baris): $filteredFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavingsTableHeaderRow(
    colTabunganLabel: String,
    colNominalLabel: String,
    colTanggalLabel: String,
    colKeteranganLabel: String,
    dividerColor: Color,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = TableHeaderShape,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("table_header_row"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 0.75.dp.toPx()
                    val col1X = size.width * COL_TABUNGAN_WEIGHT
                    val col2X = size.width * (COL_TABUNGAN_WEIGHT + COL_NOMINAL_WEIGHT)
                    val col3X = size.width * (COL_TABUNGAN_WEIGHT + COL_NOMINAL_WEIGHT + COL_TANGGAL_WEIGHT)
                    drawLine(
                        color = dividerColor,
                        start = Offset(col1X, 0f),
                        end = Offset(col1X, size.height),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = dividerColor,
                        start = Offset(col2X, 0f),
                        end = Offset(col2X, size.height),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = dividerColor,
                        start = Offset(col3X, 0f),
                        end = Offset(col3X, size.height),
                        strokeWidth = strokeWidth,
                    )
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = colTabunganLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(COL_TABUNGAN_WEIGHT)
                    .padding(end = 4.dp),
            )
            Text(
                text = colNominalLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(COL_NOMINAL_WEIGHT)
                    .padding(horizontal = 4.dp),
            )
            Text(
                text = colTanggalLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(COL_TANGGAL_WEIGHT)
                    .padding(horizontal = 4.dp),
            )
            Text(
                text = colKeteranganLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(COL_KETERANGAN_WEIGHT)
                    .padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun SavingsTableDataRow(
    slot: TableRowSlot,
    isLast: Boolean,
    emptyRowHint: String,
    rowBorder: BorderStroke,
    columnDividerColor: Color,
    onClick: () -> Unit,
) {
    val bgColor = if (slot.rowIndex % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val shape = if (isLast) TableBottomShape else RectangleShape

    val formattedRowNumber = remember(slot.rowIndex) {
        slot.rowIndex.toString().padStart(2, '0')
    }
    val formattedNominal = remember(slot.nominal) {
        if (slot.nominal > 0L) formatNumberOnly(slot.nominal) else "—"
    }
    val rowAccessibilityText = remember(slot, formattedNominal) {
        val label = slot.tabungan.ifBlank { "Kosong" }
        val dateText = slot.tanggal.ifBlank { "Belum ada tanggal" }
        val notesText = slot.keterangan.ifBlank { "Tidak ada keterangan" }
        val actionHint = if (slot.hasContent) "Ketuk untuk mengedit" else "Ketuk untuk mengisi"
        "Baris ${slot.rowIndex}, Tabungan $label, Nominal $formattedNominal, Tanggal $dateText, Keterangan $notesText. $actionHint"
    }

    Surface(
        color = bgColor,
        shape = shape,
        border = rowBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 52.dp)
            .semantics { contentDescription = rowAccessibilityText }
            .clickable(onClick = onClick)
            .testTag("table_row_${slot.rowIndex}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 0.5.dp.toPx()
                    val dividerAlpha = columnDividerColor.copy(alpha = 0.6f)
                    val col1X = size.width * COL_TABUNGAN_WEIGHT
                    val col2X = size.width * (COL_TABUNGAN_WEIGHT + COL_NOMINAL_WEIGHT)
                    val col3X = size.width * (COL_TABUNGAN_WEIGHT + COL_NOMINAL_WEIGHT + COL_TANGGAL_WEIGHT)
                    drawLine(
                        color = dividerAlpha,
                        start = Offset(col1X, 0f),
                        end = Offset(col1X, size.height),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = dividerAlpha,
                        start = Offset(col2X, 0f),
                        end = Offset(col2X, size.height),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = dividerAlpha,
                        start = Offset(col3X, 0f),
                        end = Offset(col3X, size.height),
                        strokeWidth = strokeWidth,
                    )
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Kolom 1: Tabungan (beserta nomor baris 01..60)
            Row(
                modifier = Modifier
                    .weight(COL_TABUNGAN_WEIGHT)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formattedRowNumber,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                    ),
                    color = if (slot.hasContent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    text = slot.tabungan.ifBlank { emptyRowHint },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (slot.tabungan.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                    color = if (slot.tabungan.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // Kolom 2: Nominal
            Text(
                text = formattedNominal,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = if (slot.nominal > 0L) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (slot.nominal > 0L) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(COL_NOMINAL_WEIGHT)
                    .padding(horizontal = 4.dp),
            )

            // Kolom 3: Tanggal/Bulan/Tahun
            Text(
                text = slot.tanggal.ifBlank { "—" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                ),
                color = if (slot.tanggal.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(COL_TANGGAL_WEIGHT)
                    .padding(horizontal = 4.dp),
            )

            // Kolom 4: Keterangan
            Row(
                modifier = Modifier
                    .weight(COL_KETERANGAN_WEIGHT)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.keterangan.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (slot.keterangan.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (slot.hasContent) {
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
