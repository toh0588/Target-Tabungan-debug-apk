package com.example

import com.example.data.AutoFillInterval
import com.example.data.TableRowSlot
import com.example.data.computeTableSummary
import com.example.data.docIdForRow
import com.example.data.formatNumberOnly
import com.example.data.formatRupiah
import com.example.pdf.SavingsPdfGenerator
import com.example.ui.parseIndonesianDateMillisOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun docIdForRow_padsSingleDigits() {
        assertEquals("row_01", docIdForRow(1))
        assertEquals("row_09", docIdForRow(9))
        assertEquals("row_60", docIdForRow(60))
    }

    @Test
    fun tableRowSlot_hasContentReflectsPopulatedFields() {
        assertFalse(TableRowSlot(rowIndex = 1).hasContent)
        assertTrue(TableRowSlot(rowIndex = 1, tabungan = "Setoran").hasContent)
        assertTrue(TableRowSlot(rowIndex = 2, nominal = 50000L).hasContent)
        assertTrue(TableRowSlot(rowIndex = 3, tanggal = "22/11/2026").hasContent)
        assertTrue(TableRowSlot(rowIndex = 4, keterangan = "Transfer bank").hasContent)
    }

    @Test
    fun formatRupiah_formatsIndonesianCurrency() {
        assertEquals("Rp 50.000", formatRupiah(50000L))
        assertEquals("1.000.000", formatNumberOnly(1000000L))
    }

    @Test
    fun autoFillInterval_stepsAreValid() {
        assertEquals(1L, AutoFillInterval.HARIAN.daysStep)
        assertEquals(7L, AutoFillInterval.MINGGUAN.daysStep)
        assertEquals(1L, AutoFillInterval.BULANAN.monthsStep)
    }

    @Test
    fun computeTableSummary_calculatesTotalsAndProgress() {
        val slots = (1..60).map { idx ->
            when (idx) {
                1 -> TableRowSlot(rowIndex = 1, tabungan = "Minggu 1", nominal = 150_000L)
                2 -> TableRowSlot(rowIndex = 2, tabungan = "Minggu 2", nominal = 350_000L)
                else -> TableRowSlot(rowIndex = idx)
            }
        }
        val filtered = listOf(slots[0])

        val summary = computeTableSummary(
            allSlots = slots,
            filteredSlots = filtered,
            targetAmount = 1_000_000L,
            isFiltered = true,
        )

        assertEquals(500_000L, summary.totalNominal)
        assertEquals(2, summary.filledRows)
        assertEquals(60, summary.totalDataRows)
        assertEquals(50, summary.completionPercent)
        assertEquals(500_000L, summary.remainingToTarget)
        assertEquals(150_000L, summary.filteredNominal)
        assertEquals(1, summary.filteredRows)
    }

    @Test
    fun pdfPagination_splits60DataRowsIntoTwoPages() {
        val slots = (1..60).map { TableRowSlot(rowIndex = it) }
        val pages = SavingsPdfGenerator.paginateRows(slots)

        assertEquals(2, pages.size)
        assertEquals(28, pages[0].size)
        assertEquals(32, pages[1].size)
        assertEquals(1, pages[0].first().rowIndex)
        assertEquals(60, pages[1].last().rowIndex)
    }

    @Test
    fun buildSafePdfFileName_sanitizesTitleAndAddsExtension() {
        assertEquals("Target_Tabungan.pdf", SavingsPdfGenerator.buildSafePdfFileName("   "))
        assertEquals("Tabungan_Rumah_2026.pdf", SavingsPdfGenerator.buildSafePdfFileName("Tabungan Rumah/2026!"))
    }

    @Test
    fun parseIndonesianDateMillisOrNull_parsesValidDdMmYyyy() {
        assertNotNull(parseIndonesianDateMillisOrNull("22/11/2026"))
        assertNull(parseIndonesianDateMillisOrNull("31/02/2026"))
        assertNull(parseIndonesianDateMillisOrNull(""))
    }
}
