package com.example

import com.example.data.AutoFillInterval
import com.example.data.TableRowSlot
import com.example.data.docIdForRow
import com.example.data.formatNumberOnly
import com.example.data.formatRupiah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
