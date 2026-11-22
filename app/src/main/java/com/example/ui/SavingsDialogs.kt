package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AutoFillInterval
import com.example.data.MAX_DATA_ROWS
import com.example.data.TableRowSlot
import com.example.data.TargetConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRowSheet(
    slot: TableRowSlot,
    totalRows: Int,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSave: (rowIndex: Int, tabungan: String, nominal: Long, tanggal: String, moveNext: Boolean) -> Unit,
    onClear: (rowIndex: Int) -> Unit,
    onNavigateRow: (targetRowIndex: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabungan by remember(slot.rowIndex) { mutableStateOf(slot.tabungan) }
    var nominalText by remember(slot.rowIndex) {
        mutableStateOf(if (slot.nominal > 0L) slot.nominal.toString() else "")
    }
    var tanggal by remember(slot.rowIndex) { mutableStateOf(slot.tanggal) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID")) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            tanggal = dateFormatter.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Baris #${slot.rowIndex} dari $totalRows",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigateRow(slot.rowIndex - 1) },
                        enabled = slot.rowIndex > 1 && !isMutating,
                        modifier = Modifier.testTag("prev_row_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Baris sebelumnya",
                        )
                    }
                    IconButton(
                        onClick = { onNavigateRow(slot.rowIndex + 1) },
                        enabled = slot.rowIndex < totalRows && !isMutating,
                        modifier = Modifier.testTag("next_row_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Baris berikutnya",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = tabungan,
                onValueChange = { if (it.length <= 120) tabungan = it },
                label = { Text("Tabungan (Kolom 1)") },
                placeholder = { Text("Contoh: Setoran ${slot.rowIndex}") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tabungan"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nominalText,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(12)
                    nominalText = digits
                },
                label = { Text("Nominal Rp (Kolom 2)") },
                placeholder = { Text("Contoh: 50000") },
                prefix = { Text("Rp ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_nominal"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = tanggal,
                onValueChange = { if (it.length <= 20) tanggal = it },
                label = { Text("Tanggal/Bulan/Tahun (Kolom 3)") },
                placeholder = { Text("DD/MM/YYYY") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("pick_date_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pilih tanggal",
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tanggal"),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = false,
                    onClick = { tanggal = dateFormatter.format(Date()) },
                    label = { Text("Hari Ini") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.testTag("today_chip"),
                )
                if (tabungan.isBlank()) {
                    FilterChip(
                        selected = false,
                        onClick = { tabungan = "Tabungan ${slot.rowIndex}" },
                        label = { Text("Tabungan ${slot.rowIndex}") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (slot.persisted || slot.hasContent) {
                    OutlinedButton(
                        onClick = { onClear(slot.rowIndex) },
                        enabled = !isMutating,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_row_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus")
                    }
                }

                if (slot.rowIndex < totalRows) {
                    OutlinedButton(
                        onClick = {
                            val parsedNominal = nominalText.toLongOrNull() ?: 0L
                            onSave(slot.rowIndex, tabungan, parsedNominal, tanggal, true)
                        },
                        enabled = !isMutating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_next_button"),
                    ) {
                        Text("Simpan & Lanjut")
                    }
                }

                Button(
                    onClick = {
                        val parsedNominal = nominalText.toLongOrNull() ?: 0L
                        onSave(slot.rowIndex, tabungan, parsedNominal, tanggal, false)
                    },
                    enabled = !isMutating,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_row_button"),
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoFillDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        labelPrefix: String,
        nominalPerRow: Long,
        startDateMillis: Long,
        interval: AutoFillInterval,
        onlyEmptyRows: Boolean,
    ) -> Unit,
) {
    var prefix by remember { mutableStateOf("Tabungan") }
    var nominalText by remember { mutableStateOf("50000") }
    var startDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var interval by remember { mutableStateOf(AutoFillInterval.HARIAN) }
    var onlyEmpty by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID")) }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { startDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Isi Tabel Otomatis") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Isi baris tabel secara otomatis dengan nominal dan jadwal rutin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { if (it.length <= 60) prefix = it },
                    label = { Text("Nama Awalan Tabungan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = nominalText,
                    onValueChange = { raw -> nominalText = raw.filter { it.isDigit() }.take(12) },
                    label = { Text("Nominal per Baris") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tanggal Mulai: ${dateFormatter.format(Date(startDateMillis))}")
                }

                Text(
                    text = "Interval Tanggal",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AutoFillInterval.entries.forEach { item ->
                        FilterChip(
                            selected = interval == item,
                            onClick = { interval = item },
                            label = { Text(item.label) },
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = onlyEmpty,
                        onCheckedChange = { onlyEmpty = it },
                    )
                    Text(
                        text = "Hanya isi baris yang masih kosong",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nominal = nominalText.toLongOrNull() ?: 0L
                    onConfirm(prefix, nominal, startDateMillis, interval, onlyEmpty)
                },
                enabled = !isMutating,
                modifier = Modifier.testTag("confirm_autofill_button"),
            ) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text("Batal")
            }
        },
    )
}

@Composable
fun TargetConfigDialog(
    config: TargetConfig,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, subtitle: String, targetAmount: Long, totalDataRows: Int) -> Unit,
) {
    var title by remember { mutableStateOf(config.title.ifBlank { "Target Tabungan" }) }
    var subtitle by remember { mutableStateOf(config.subtitle) }
    var targetText by remember {
        mutableStateOf(if (config.targetAmount > 0L) config.targetAmount.toString() else "")
    }
    var rowsText by remember { mutableStateOf(config.totalDataRows.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Target Tabungan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Judul Dokumen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { if (it.length <= 150) subtitle = it },
                    label = { Text("Catatan / Tujuan (Opsional)") },
                    placeholder = { Text("Contoh: Dana Darurat 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(12) },
                    label = { Text("Target Nominal Total") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rowsText,
                    onValueChange = { rowsText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Jumlah Baris Data (Default 60 + 1 Header = 61)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toLongOrNull() ?: 0L
                    val rows = (rowsText.toIntOrNull() ?: 60).coerceIn(1, MAX_DATA_ROWS)
                    onSave(title, subtitle, target, rows)
                },
                enabled = !isMutating,
                modifier = Modifier.testTag("confirm_config_button"),
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text("Batal")
            }
        },
    )
}

@Composable
fun ClearAllConfirmDialog(
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kosongkan Semua Baris?") },
        text = {
            Text("Semua isi pada kolom Tabungan, Nominal, dan Tanggal akan dihapus, namun struktur 61 baris tabel tetap dipertahankan.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isMutating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Kosongkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text("Batal")
            }
        },
    )
}
