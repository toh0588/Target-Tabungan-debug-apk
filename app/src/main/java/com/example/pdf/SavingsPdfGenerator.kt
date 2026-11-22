package com.example.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.TableRowSlot
import com.example.data.TargetConfig
import com.example.data.formatRupiah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SavingsPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 height in points (72 dpi)
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 40f
    private const val ROWS_PER_FIRST_PAGE = 28
    private const val ROWS_PER_NEXT_PAGE = 32

    suspend fun writePdfToUri(
        context: Context,
        uri: Uri,
        config: TargetConfig,
        rows: List<TableRowSlot>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Tidak dapat membuka file tujuan."))
            outputStream.use { stream ->
                generatePdf(config, rows, stream)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writePdfToCacheForShare(
        context: Context,
        config: TargetConfig,
        rows: List<TableRowSlot>,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val pdfFile = File(pdfDir, "Target_Tabungan.pdf")
            FileOutputStream(pdfFile).use { stream ->
                generatePdf(config, rows, stream)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generatePdf(
        config: TargetConfig,
        rows: List<TableRowSlot>,
        outputStream: OutputStream,
    ) {
        val document = PdfDocument()
        try {
            val contentWidth = PAGE_WIDTH - (MARGIN_H * 2)
            val col1Width = contentWidth * 0.42f // Tabungan
            val col2Width = contentWidth * 0.29f // Nominal
            val col3Width = contentWidth * 0.29f // Tanggal/Bulan/Tahun

            val emeraldColor = Color.rgb(15, 107, 78)
            val softTintColor = Color.rgb(243, 248, 245)
            val summaryTintColor = Color.rgb(228, 243, 236)
            val borderColor = Color.rgb(198, 209, 203)
            val inkColor = Color.rgb(24, 29, 26)
            val mutedColor = Color.rgb(92, 104, 98)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldColor
                textSize = 20f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedColor
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedColor
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val metaValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val headerBgPaint = Paint().apply {
                color = emeraldColor
                style = Paint.Style.FILL
            }
            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val rowAltPaint = Paint().apply {
                color = softTintColor
                style = Paint.Style.FILL
            }
            val summaryBgPaint = Paint().apply {
                color = summaryTintColor
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = borderColor
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val cellIndexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedColor
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val cellMonoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 9.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            val summaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = emeraldColor
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedColor
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }

            val totalSaved = rows.sumOf { it.nominal }
            val filledCount = rows.count { it.hasContent }
            val printDate = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID")).format(Date())

            // Split rows into pages
            val pages = mutableListOf<List<TableRowSlot>>()
            var cursor = 0
            while (cursor < rows.size) {
                val limit = if (pages.isEmpty()) ROWS_PER_FIRST_PAGE else ROWS_PER_NEXT_PAGE
                val end = minOf(cursor + limit, rows.size)
                pages.add(rows.subList(cursor, end))
                cursor = end
            }
            if (pages.isEmpty()) {
                pages.add(emptyList())
            }

            val totalPages = pages.size
            val rowHeight = 20.5f
            val headerRowHeight = 25f

            pages.forEachIndexed { pageIdx, pageRows ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                var y = MARGIN_TOP

                if (pageIdx == 0) {
                    // Accent bar
                    val accentBar = RectF(MARGIN_H, y, MARGIN_H + 36f, y + 4f)
                    canvas.drawRoundRect(accentBar, 2f, 2f, headerBgPaint)
                    y += 22f

                    val displayTitle = config.title.ifBlank { "Target Tabungan" }
                    canvas.drawText(displayTitle, MARGIN_H, y, titlePaint)
                    y += 15f

                    val subLine = buildString {
                        if (config.subtitle.isNotBlank()) {
                            append(config.subtitle)
                            append("  •  ")
                        }
                        append("Tabel 3 Kolom × ${rows.size + 1} Baris (1 Header + ${rows.size} Data)")
                    }
                    canvas.drawText(subLine, MARGIN_H, y, subtitlePaint)
                    y += 14f

                    // Summary box
                    val boxHeight = 42f
                    val boxRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + boxHeight)
                    canvas.drawRoundRect(boxRect, 6f, 6f, rowAltPaint)
                    canvas.drawRoundRect(boxRect, 6f, 6f, borderPaint)

                    val colStep = contentWidth / 4f
                    val labelY = y + 15f
                    val valY = y + 31f

                    canvas.drawText("Total Terkumpul", MARGIN_H + 10f, labelY, metaLabelPaint)
                    canvas.drawText(formatRupiah(totalSaved), MARGIN_H + 10f, valY, metaValuePaint)

                    canvas.drawText("Target Tabungan", MARGIN_H + colStep + 8f, labelY, metaLabelPaint)
                    val targetStr = if (config.targetAmount > 0L) formatRupiah(config.targetAmount) else "-"
                    canvas.drawText(targetStr, MARGIN_H + colStep + 8f, valY, metaValuePaint)

                    canvas.drawText("Baris Terisi", MARGIN_H + colStep * 2 + 8f, labelY, metaLabelPaint)
                    canvas.drawText("$filledCount / ${rows.size} baris", MARGIN_H + colStep * 2 + 8f, valY, metaValuePaint)

                    canvas.drawText("Tanggal Cetak", MARGIN_H + colStep * 3 + 8f, labelY, metaLabelPaint)
                    canvas.drawText(printDate, MARGIN_H + colStep * 3 + 8f, valY, metaValuePaint)

                    y += boxHeight + 14f
                } else {
                    canvas.drawText(
                        "${config.title.ifBlank { "Target Tabungan" }} (Lanjutan)",
                        MARGIN_H,
                        y + 12f,
                        subtitlePaint
                    )
                    y += 22f
                }

                // Draw Table Header Row (Row 1 of 61)
                val x1 = MARGIN_H
                val x2 = x1 + col1Width
                val x3 = x2 + col2Width
                val x4 = x3 + col3Width

                canvas.drawRect(x1, y, x4, y + headerRowHeight, headerBgPaint)
                val headerBaseline = y + headerRowHeight / 2f + 4f
                canvas.drawText("Tabungan", x1 + 10f, headerBaseline, headerTextPaint)
                canvas.drawText("Nominal", x2 + 10f, headerBaseline, headerTextPaint)
                canvas.drawText("Tanggal/Bulan/Tahun", x3 + 10f, headerBaseline, headerTextPaint)

                // Vertical header dividers
                val whiteDividerPaint = Paint().apply {
                    color = Color.argb(75, 255, 255, 255)
                    strokeWidth = 0.8f
                }
                canvas.drawLine(x2, y, x2, y + headerRowHeight, whiteDividerPaint)
                canvas.drawLine(x3, y, x3, y + headerRowHeight, whiteDividerPaint)
                y += headerRowHeight

                // Draw Data Rows
                pageRows.forEachIndexed { idx, row ->
                    if (idx % 2 == 1) {
                        canvas.drawRect(x1, y, x4, y + rowHeight, rowAltPaint)
                    }
                    canvas.drawRect(x1, y, x4, y + rowHeight, borderPaint)
                    canvas.drawLine(x2, y, x2, y + rowHeight, borderPaint)
                    canvas.drawLine(x3, y, x3, y + rowHeight, borderPaint)

                    val baseline = y + rowHeight / 2f + 3.5f
                    val indexStr = row.rowIndex.toString().padStart(2, '0') + "."
                    canvas.drawText(indexStr, x1 + 8f, baseline, cellIndexPaint)

                    if (row.tabungan.isNotBlank()) {
                        val trimmed = fitText(row.tabungan, cellTextPaint, col1Width - 40f)
                        canvas.drawText(trimmed, x1 + 32f, baseline, cellTextPaint)
                    }

                    if (row.nominal > 0L) {
                        val nominalStr = formatRupiah(row.nominal)
                        canvas.drawText(nominalStr, x2 + 10f, baseline, cellMonoPaint)
                    }

                    if (row.tanggal.isNotBlank()) {
                        canvas.drawText(row.tanggal, x3 + 10f, baseline, cellMonoPaint)
                    }

                    y += rowHeight
                }

                // Draw Total Summary Row on Last Page
                if (pageIdx == totalPages - 1 && y + 24f < PAGE_HEIGHT - MARGIN_BOTTOM) {
                    val totalRowH = 24f
                    canvas.drawRect(x1, y, x4, y + totalRowH, summaryBgPaint)
                    canvas.drawRect(x1, y, x4, y + totalRowH, borderPaint)
                    canvas.drawLine(x2, y, x2, y + totalRowH, borderPaint)
                    canvas.drawLine(x3, y, x3, y + totalRowH, borderPaint)

                    val base = y + totalRowH / 2f + 4f
                    canvas.drawText("TOTAL TABUNGAN", x1 + 10f, base, summaryTextPaint)
                    canvas.drawText(formatRupiah(totalSaved), x2 + 10f, base, summaryTextPaint)
                    canvas.drawText("$filledCount/${rows.size} terisi", x3 + 10f, base, cellIndexPaint)
                }

                // Page Footer
                val footerText = "Target Tabungan  •  Halaman ${pageIdx + 1} dari $totalPages"
                canvas.drawText(footerText, MARGIN_H, PAGE_HEIGHT - 18f, footerPaint)

                document.finishPage(page)
            }

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + ellipsis else ellipsis
    }
}
