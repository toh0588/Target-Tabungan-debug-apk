package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.data.RowFilter
import com.example.data.TableRowSlot
import com.example.data.TargetConfig
import com.example.data.UiState
import com.example.pdf.SavingsPdfGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetTabunganScreen(
    viewModel: SavingsViewModel,
    userEmail: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tableListState = rememberLazyListState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    var editingRowIndex by remember { mutableStateOf<Int?>(null) }
    var showAutoFillDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val successState = uiState as? UiState.Success ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                isExportingPdf = true
                val res = SavingsPdfGenerator.writePdfToUri(
                    context = context,
                    uri = uri,
                    config = successState.data.config,
                    rows = successState.data.allSlots,
                )
                isExportingPdf = false
                res.onSuccess {
                    snackbarHostState.showSnackbar("PDF Target Tabungan berhasil diunduh")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Gagal menyimpan PDF")
                }
            }
        }
    }

    fun sharePdf(config: TargetConfig, slots: List<TableRowSlot>) {
        scope.launch {
            isExportingPdf = true
            val res = SavingsPdfGenerator.writePdfToCacheForShare(context, config, slots)
            isExportingPdf = false
            res.onSuccess { fileUri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, config.title.ifBlank { "Target Tabungan" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Bagikan PDF"))
            }.onFailure { err ->
                snackbarHostState.showSnackbar(err.localizedMessage ?: "Gagal menyiapkan PDF")
            }
        }
    }

    fun openPdf(config: TargetConfig, slots: List<TableRowSlot>) {
        scope.launch {
            isExportingPdf = true
            val res = SavingsPdfGenerator.writePdfToCacheForShare(context, config, slots)
            isExportingPdf = false
            res.onSuccess { fileUri ->
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(viewIntent)
                } catch (_: Exception) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Buka / Bagikan PDF"))
                }
            }.onFailure { err ->
                snackbarHostState.showSnackbar(err.localizedMessage ?: "Gagal membuka PDF")
            }
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionError()
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val currentData = (uiState as? UiState.Success)?.data

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentData?.config?.title?.ifBlank { "Target Tabungan" }
                                ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val rowCountText = currentData?.let {
                            "4 Kolom × ${it.allSlots.size + 1} Baris"
                        } ?: "4 Kolom × 61 Baris"
                        Text(
                            text = rowCountText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    if (currentData != null) {
                        IconButton(
                            onClick = { showAutoFillDialog = true },
                            modifier = Modifier.testTag("topbar_autofill_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = stringResource(R.string.auto_fill),
                            )
                        }
                        IconButton(
                            onClick = {
                                createPdfLauncher.launch(
                                    SavingsPdfGenerator.buildSafePdfFileName(currentData.config.title)
                                )
                            },
                            enabled = !isExportingPdf,
                            modifier = Modifier.testTag("topbar_pdf_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = stringResource(R.string.download_pdf),
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("overflow_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu lainnya",
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            if (currentData != null) {
                                DropdownMenuItem(
                                    text = { Text("Pengaturan Target") },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showConfigDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Bagikan / Buka PDF") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        sharePdf(currentData.config, currentData.allSlots)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Kosongkan Semua Baris") },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showClearAllDialog = true
                                    },
                                )
                                HorizontalDivider()
                            }
                            if (!userEmail.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    onClick = {},
                                    enabled = false,
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Keluar Akun") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    signOut(credentialManager, scope)
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (currentData != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        createPdfLauncher.launch(
                            SavingsPdfGenerator.buildSafePdfFileName(currentData.config.title)
                        )
                    },
                    icon = {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                            )
                        }
                    },
                    text = {
                        Text(
                            text = if (isExportingPdf) "Membuat PDF..." else stringResource(R.string.download_pdf),
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("download_pdf_fab"),
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Memuat Target Tabungan...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Gagal Memuat Data",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val data = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        RunningWelcomeBanner(
                            modifier = Modifier
                                .widthIn(max = 1120.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val isWideScreen = maxWidth >= 760.dp

                        fun jumpAndScrollToRow(rowIdx: Int) {
                            editingRowIndex = rowIdx
                            val filteredIndex = data.filteredSlots.indexOfFirst { it.rowIndex == rowIdx }
                            if (filteredIndex >= 0) {
                                val listOffset = if (isWideScreen) 1 else 2
                                scope.launch {
                                    tableListState.animateScrollToItem(filteredIndex + listOffset)
                                }
                            }
                        }

                        if (isWideScreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(340.dp)
                                        .navigationBarsPadding(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SavingsSummaryCard(
                                        data = data,
                                        isExportingPdf = isExportingPdf,
                                        onAutoFill = { showAutoFillDialog = true },
                                        onEditConfig = { showConfigDialog = true },
                                        onOpenPdf = { openPdf(data.config, data.allSlots) },
                                        onSharePdf = { sharePdf(data.config, data.allSlots) },
                                        onJumpToRow = ::jumpAndScrollToRow,
                                    )
                                    FilterAndSearchBar(
                                        filter = data.filter,
                                        searchQuery = data.searchQuery,
                                        onFilterChange = viewModel::setFilter,
                                        onSearchChange = viewModel::setSearchQuery,
                                    )
                                }

                                SavingsLedgerTable(
                                    slots = data.filteredSlots,
                                    totalTableRows = data.allSlots.size + 1,
                                    summaryInfo = data.summaryInfo,
                                    listState = tableListState,
                                    onRowClick = { editingRowIndex = it.rowIndex },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                SavingsLedgerTable(
                                    slots = data.filteredSlots,
                                    totalTableRows = data.allSlots.size + 1,
                                    summaryInfo = data.summaryInfo,
                                    listState = tableListState,
                                    onRowClick = { editingRowIndex = it.rowIndex },
                                    headerContent = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .padding(top = 4.dp, bottom = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            SavingsSummaryCard(
                                                data = data,
                                                isExportingPdf = isExportingPdf,
                                                onAutoFill = { showAutoFillDialog = true },
                                                onEditConfig = { showConfigDialog = true },
                                                onOpenPdf = { openPdf(data.config, data.allSlots) },
                                                onSharePdf = { sharePdf(data.config, data.allSlots) },
                                                onJumpToRow = ::jumpAndScrollToRow,
                                            )
                                            FilterAndSearchBar(
                                                filter = data.filter,
                                                searchQuery = data.searchQuery,
                                                onFilterChange = viewModel::setFilter,
                                                onSearchChange = viewModel::setSearchQuery,
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .widthIn(max = 680.dp)
                                        .fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeSlot = currentData?.allSlots?.firstOrNull { it.rowIndex == editingRowIndex }
    if (activeSlot != null) {
        EditRowSheet(
            slot = activeSlot,
            totalRows = currentData.allSlots.size,
            isMutating = isMutating,
            onDismiss = { editingRowIndex = null },
            onSave = { rowIdx, tabungan, nominal, tanggal, keterangan, moveNext ->
                viewModel.saveRow(rowIdx, tabungan, nominal, tanggal, keterangan) {
                    if (moveNext && rowIdx < currentData.allSlots.size) {
                        editingRowIndex = rowIdx + 1
                    } else {
                        editingRowIndex = null
                    }
                }
            },
            onClear = { rowIdx ->
                viewModel.clearRow(rowIdx) {
                    editingRowIndex = null
                }
            },
            onNavigateRow = { targetIdx ->
                editingRowIndex = targetIdx.coerceIn(1, currentData.allSlots.size)
            },
        )
    }

    if (showAutoFillDialog && currentData != null) {
        AutoFillDialog(
            targetAmount = currentData.config.targetAmount,
            totalRows = currentData.allSlots.size,
            isMutating = isMutating,
            onDismiss = { showAutoFillDialog = false },
            onConfirm = { prefix, nominal, startMillis, interval, onlyEmpty ->
                viewModel.autoFillRows(
                    labelPrefix = prefix,
                    nominalPerRow = nominal,
                    startDateMillis = startMillis,
                    interval = interval,
                    onlyEmptyRows = onlyEmpty,
                    currentSlots = currentData.allSlots,
                ) {
                    showAutoFillDialog = false
                }
            },
        )
    }

    if (showConfigDialog && currentData != null) {
        TargetConfigDialog(
            config = currentData.config,
            isMutating = isMutating,
            onDismiss = { showConfigDialog = false },
            onSave = { title, subtitle, target, totalRows ->
                viewModel.updateConfig(title, subtitle, target, totalRows) {
                    showConfigDialog = false
                }
            },
        )
    }

    if (showClearAllDialog) {
        ClearAllConfirmDialog(
            isMutating = isMutating,
            onDismiss = { showClearAllDialog = false },
            onConfirm = {
                viewModel.clearAllRows {
                    showClearAllDialog = false
                }
            },
        )
    }
}

@Composable
private fun FilterAndSearchBar(
    filter: RowFilter,
    searchQuery: String,
    onFilterChange: (RowFilter) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari tabungan, nominal, tanggal, atau keterangan...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan pencarian")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_rows_input"),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RowFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { onFilterChange(item) },
                    label = { Text(item.label) },
                    modifier = Modifier.testTag("filter_${item.name.lowercase()}"),
                )
            }
        }
    }
}
