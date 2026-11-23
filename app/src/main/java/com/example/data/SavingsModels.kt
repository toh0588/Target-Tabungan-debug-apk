package com.example.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

const val DEFAULT_DATA_ROWS = 60
const val MAX_DATA_ROWS = 120
const val MAX_NOMINAL = 1_000_000_000_000L

@Keep
@IgnoreExtraProperties
data class SavingsRow(
    @DocumentId val id: String = "",
    val userId: String = "",
    val rowIndex: Long = 1L,
    val tabungan: String = "",
    val nominal: Long = 0L,
    val tanggal: String = "",
    val keterangan: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class TargetConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Target Tabungan",
    val subtitle: String = "",
    val targetAmount: Long = 0L,
    val totalDataRows: Long = DEFAULT_DATA_ROWS.toLong(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Immutable
data class TableRowSlot(
    val rowIndex: Int,
    val tabungan: String = "",
    val nominal: Long = 0L,
    val tanggal: String = "",
    val keterangan: String = "",
    val persisted: Boolean = false,
) {
    val hasContent: Boolean
        get() = tabungan.isNotBlank() ||
            nominal > 0L ||
            tanggal.isNotBlank() ||
            keterangan.isNotBlank()
}

data class SavingsRowInput(
    val rowIndex: Int,
    val tabungan: String,
    val nominal: Long,
    val tanggal: String,
    val keterangan: String = "",
)

enum class AutoFillInterval(val label: String, val daysStep: Long, val monthsStep: Long) {
    HARIAN("Harian", 1L, 0L),
    MINGGUAN("Mingguan", 7L, 0L),
    BULANAN("Bulanan", 0L, 1L),
}

enum class RowFilter(val label: String) {
    SEMUA("Semua"),
    TERISI("Terisi"),
    KOSONG("Kosong"),
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Immutable
data class TableSummaryInfo(
    val totalNominal: Long,
    val filledRows: Int,
    val totalDataRows: Int,
    val targetAmount: Long = 0L,
    val filteredNominal: Long = totalNominal,
    val filteredRows: Int = totalDataRows,
    val isFiltered: Boolean = false,
) {
    val completionPercent: Int
        get() = if (targetAmount > 0L) {
            ((totalNominal.toDouble() / targetAmount.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        } else {
            0
        }

    val remainingToTarget: Long
        get() = if (targetAmount > 0L) (targetAmount - totalNominal).coerceAtLeast(0L) else 0L
}

fun computeTableSummary(
    allSlots: List<TableRowSlot>,
    filteredSlots: List<TableRowSlot>,
    targetAmount: Long = 0L,
    isFiltered: Boolean = false,
): TableSummaryInfo {
    val totalNominal = allSlots.sumOf { it.nominal }
    val filledRows = allSlots.count { it.hasContent }
    val filteredNominal = if (isFiltered) filteredSlots.sumOf { it.nominal } else totalNominal
    return TableSummaryInfo(
        totalNominal = totalNominal,
        filledRows = filledRows,
        totalDataRows = allSlots.size,
        targetAmount = targetAmount,
        filteredNominal = filteredNominal,
        filteredRows = filteredSlots.size,
        isFiltered = isFiltered,
    )
}

private val idNumberFormatter: ThreadLocal<NumberFormat> = ThreadLocal.withInitial {
    NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
}

fun docIdForRow(rowIndex: Int): String =
    "row_${rowIndex.toString().padStart(2, '0')}"

fun formatRupiah(amount: Long): String {
    val formatter = idNumberFormatter.get()
        ?: NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp ${formatter.format(amount)}"
}

fun formatNumberOnly(amount: Long): String {
    val formatter = idNumberFormatter.get()
        ?: NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return formatter.format(amount)
}
