package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AutoFillInterval
import com.example.data.DEFAULT_DATA_ROWS
import com.example.data.MAX_DATA_ROWS
import com.example.data.RowFilter
import com.example.data.SavingsRepository
import com.example.data.SavingsRow
import com.example.data.SavingsRowInput
import com.example.data.TableRowSlot
import com.example.data.TargetConfig
import com.example.data.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SavingsDashboardData(
    val config: TargetConfig,
    val allSlots: List<TableRowSlot>,
    val filteredSlots: List<TableRowSlot>,
    val totalSaved: Long,
    val filledCount: Int,
    val filter: RowFilter,
    val searchQuery: String,
)

class SavingsViewModel(
    private val repository: SavingsRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _filter = MutableStateFlow(RowFilter.SEMUA)
    private val _searchQuery = MutableStateFlow("")
    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val uiState: StateFlow<UiState<SavingsDashboardData>> = combine(
        repository.observeSavingsRows(currentUserId),
        repository.observeTargetConfig(currentUserId),
        _filter,
        _searchQuery,
    ) { rows: List<SavingsRow>, config: TargetConfig?, filter: RowFilter, query: String ->
        val effectiveConfig = config ?: TargetConfig(
            userId = currentUserId,
            title = "Target Tabungan",
            subtitle = "",
            targetAmount = 0L,
            totalDataRows = DEFAULT_DATA_ROWS.toLong(),
        )
        val totalRows = effectiveConfig.totalDataRows.toInt().coerceIn(1, MAX_DATA_ROWS)
        val rowMap = rows.associateBy { it.rowIndex.toInt() }

        val allSlots = (1..totalRows).map { idx ->
            val existing = rowMap[idx]
            if (existing != null) {
                TableRowSlot(
                    rowIndex = idx,
                    tabungan = existing.tabungan,
                    nominal = existing.nominal,
                    tanggal = existing.tanggal,
                    persisted = true,
                )
            } else {
                TableRowSlot(rowIndex = idx)
            }
        }

        val idLocale = Locale.forLanguageTag("id-ID")
        val normalizedQuery = query.trim().lowercase(idLocale)
        val filteredSlots = allSlots.filter { slot ->
            val matchesFilter = when (filter) {
                RowFilter.SEMUA -> true
                RowFilter.TERISI -> slot.hasContent
                RowFilter.KOSONG -> !slot.hasContent
            }
            val matchesQuery = if (normalizedQuery.isBlank()) {
                true
            } else {
                slot.tabungan.lowercase(idLocale).contains(normalizedQuery) ||
                    slot.tanggal.lowercase(idLocale).contains(normalizedQuery) ||
                    slot.nominal.toString().contains(normalizedQuery) ||
                    slot.rowIndex.toString() == normalizedQuery
            }
            matchesFilter && matchesQuery
        }

        val totalSaved = allSlots.sumOf { it.nominal }
        val filledCount = allSlots.count { it.hasContent }

        UiState.Success(
            SavingsDashboardData(
                config = effectiveConfig,
                allSlots = allSlots,
                filteredSlots = filteredSlots,
                totalSaved = totalSaved,
                filledCount = filledCount,
                filter = filter,
                searchQuery = query,
            )
        ) as UiState<SavingsDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing savings data", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data tabungan."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    fun setFilter(filter: RowFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun saveRow(
        rowIndex: Int,
        tabungan: String,
        nominal: Long,
        tanggal: String,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveRow(
                userId = currentUserId,
                rowIndex = rowIndex,
                tabungan = tabungan,
                nominal = nominal,
                tanggal = tanggal,
            )
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "Baris #$rowIndex disimpan"
                onComplete()
            }.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Gagal menyimpan baris #$rowIndex"
            }
        }
    }

    fun clearRow(rowIndex: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.clearRow(currentUserId, rowIndex)
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "Baris #$rowIndex dikosongkan"
                onComplete()
            }.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Gagal mengosongkan baris #$rowIndex"
            }
        }
    }

    fun autoFillRows(
        labelPrefix: String,
        nominalPerRow: Long,
        startDateMillis: Long,
        interval: AutoFillInterval,
        onlyEmptyRows: Boolean,
        currentSlots: List<TableRowSlot>,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val targets = if (onlyEmptyRows) {
                currentSlots.filter { !it.hasContent }
            } else {
                currentSlots
            }
            if (targets.isEmpty()) {
                _actionMessage.value = "Semua baris sudah terisi"
                onComplete()
                return@launch
            }

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID"))
            val calendar = Calendar.getInstance().apply { time = Date(startDateMillis) }
            val prefix = labelPrefix.trim().ifBlank { "Tabungan" }

            val inputs = targets.mapIndexed { stepIdx, slot ->
                val calCopy = calendar.clone() as Calendar
                if (interval.daysStep > 0) {
                    calCopy.add(Calendar.DAY_OF_YEAR, (stepIdx * interval.daysStep).toInt())
                }
                if (interval.monthsStep > 0) {
                    calCopy.add(Calendar.MONTH, (stepIdx * interval.monthsStep).toInt())
                }
                val dateStr = dateFormatter.format(calCopy.time)
                SavingsRowInput(
                    rowIndex = slot.rowIndex,
                    tabungan = "$prefix ${slot.rowIndex}",
                    nominal = nominalPerRow,
                    tanggal = dateStr,
                )
            }

            _isMutating.value = true
            val result = repository.batchFillRows(currentUserId, inputs)
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "${inputs.size} baris berhasil diisi otomatis"
                onComplete()
            }.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Gagal mengisi baris otomatis"
            }
        }
    }

    fun clearAllRows(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.clearAllRows(currentUserId)
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "Seluruh baris dikosongkan"
                onComplete()
            }.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Gagal mengosongkan tabel"
            }
        }
    }

    fun updateConfig(
        title: String,
        subtitle: String,
        targetAmount: Long,
        totalDataRows: Int,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveTargetConfig(
                userId = currentUserId,
                title = title,
                subtitle = subtitle,
                targetAmount = targetAmount,
                totalDataRows = totalDataRows,
            )
            _isMutating.value = false
            result.onSuccess {
                _actionMessage.value = "Pengaturan Target Tabungan disimpan"
                onComplete()
            }.onFailure { e ->
                _actionError.value = e.localizedMessage ?: "Gagal menyimpan pengaturan"
            }
        }
    }

    private companion object {
        const val TAG = "SavingsVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
