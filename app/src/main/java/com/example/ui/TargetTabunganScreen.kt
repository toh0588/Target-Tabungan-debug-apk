package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Button
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.data.formatNumberOnly
import com.example.data.formatRupiah
import com.example.pdf.SavingsPdfGenerator
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TargetTabunganScreen(
    viewModel: SavingsViewModel,
    userEmail: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                            "3 Kolom × ${it.allSlots.size + 1} Baris"
                        } ?: "3 Kolom × 61 Baris"
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
                                val safeName = currentData.config.title
                                    .ifBlank { "Target_Tabungan" }
                                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                                createPdfLauncher.launch("$safeName.pdf")
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
                        val safeName = currentData.config.title
                            .ifBlank { "Target_Tabungan" }
                            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                        createPdfLauncher.launch("$safeName.pdf")
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
                                        onJumpToRow = { rowIdx -> editingRowIndex = rowIdx },
                                    )
                                    FilterAndSearchBar(
                                        filter = data.filter,
                                        searchQuery = data.searchQuery,
                                        onFilterChange = viewModel::setFilter,
                                        onSearchChange = viewModel::setSearchQuery,
                                    )
                                }

                                LedgerTableList(
                                    slots = data.filteredSlots,
                                    totalTableRows = data.allSlots.size + 1,
                                    onRowClick = { editingRowIndex = it.rowIndex },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                LedgerTableList(
                                    slots = data.filteredSlots,
                                    totalTableRows = data.allSlots.size + 1,
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
                                                onJumpToRow = { rowIdx -> editingRowIndex = rowIdx },
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
            onSave = { rowIdx, tabungan, nominal, tanggal, moveNext ->
                viewModel.saveRow(rowIdx, tabungan, nominal, tanggal) {
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
            placeholder = { Text("Cari tabungan, nominal, atau tanggal...") },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerTableList(
    slots: List<TableRowSlot>,
    totalTableRows: Int,
    onRowClick: (TableRowSlot) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        if (headerContent != null) {
            item(key = "summary_header") {
                headerContent()
            }
        }

        // Row 1 of 61: Sticky 3-Column Table Header
        stickyHeader(key = "table_header_row") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("table_header_row"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.col_tabungan),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.42f),
                        )
                        Text(
                            text = stringResource(R.string.col_nominal),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(0.28f)
                                .padding(end = 8.dp),
                        )
                        Text(
                            text = stringResource(R.string.col_tanggal),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.30f),
                        )
                    }
                }
            }
        }

        if (slots.isEmpty()) {
            item(key = "empty_filter") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
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
            items(
                items = slots,
                key = { it.rowIndex },
            ) { slot ->
                val isLast = slot == slots.last()
                TableDataRow(
                    slot = slot,
                    isLast = isLast,
                    onClick = { onRowClick(slot) },
                )
            }
        }

        item(key = "table_footer_note") {
            Text(
                text = "Total $totalTableRows baris (1 baris header + ${totalTableRows - 1} baris data). Ketuk baris mana saja untuk mengisi.",
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
private fun TableDataRow(
    slot: TableRowSlot,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (slot.rowIndex % 2 == 0) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val shape = if (isLast) {
        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(0.dp)
    }

    Surface(
        color = bgColor,
        shape = shape,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .testTag("table_row_${slot.rowIndex}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Kolom 1: Tabungan (with subtle 01..60 row number badge)
            Row(
                modifier = Modifier.weight(0.42f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.rowIndex.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                    ),
                    color = if (slot.hasContent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.width(24.dp),
                )
                Text(
                    text = slot.tabungan.ifBlank { stringResource(R.string.empty_row_hint) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (slot.tabungan.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                    color = if (slot.tabungan.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Kolom 2: Nominal
            Text(
                text = if (slot.nominal > 0L) formatNumberOnly(slot.nominal) else "—",
                style = MaterialTheme.typography.bodyMedium.copy(
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
                modifier = Modifier
                    .weight(0.28f)
                    .padding(end = 8.dp),
            )

            // Kolom 3: Tanggal/Bulan/Tahun
            Text(
                text = slot.tanggal.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                ),
                color = if (slot.tanggal.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.weight(0.30f),
            )
        }
    }
}
