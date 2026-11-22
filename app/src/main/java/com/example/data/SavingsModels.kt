package com.example.data

import androidx.annotation.Keep
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

data class TableRowSlot(
    val rowIndex: Int,
    val tabungan: String = "",
    val nominal: Long = 0L,
    val tanggal: String = "",
    val persisted: Boolean = false,
) {
    val hasContent: Boolean
        get() = tabungan.isNotBlank() || nominal > 0L || tanggal.isNotBlank()
}

data class SavingsRowInput(
    val rowIndex: Int,
    val tabungan: String,
    val nominal: Long,
    val tanggal: String,
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

fun docIdForRow(rowIndex: Int): String =
    "row_${rowIndex.toString().padStart(2, '0')}"

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return "Rp ${formatter.format(amount)}"
}

fun formatNumberOnly(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    return formatter.format(amount)
}
