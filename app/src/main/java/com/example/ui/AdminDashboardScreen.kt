package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionRecord
import com.example.data.model.UserWallet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.allWallets.collectAsStateWithLifecycle()
    val filteredTxns by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle(initialValue = DashboardMetrics())
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val selectedTxnForAction by viewModel.selectedTransactionForAction.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Transactions, 1: User Wallets, 2: Simulator console
    var showAddUserDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Display Toast / Snackbar when message changes
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            val prefix = when (it) {
                is UiMessage.Success -> "✅ Success"
                is UiMessage.Error -> "❌ Error"
            }
            val text = when (it) {
                is UiMessage.Success -> it.message
                is UiMessage.Error -> it.message
            }
            snackbarHostState.showSnackbar("$prefix: $text", duration = SnackbarDuration.Short)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Admin icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DepoWD Admin",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Website Management Console",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetDatabaseToDefaults() },
                        modifier = Modifier.testTag("reset_db_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset database to default seed state",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Metrics Overview Section (Visually rich & detailed)
            MetricsWidget(metrics = metrics)

            // Dynamic Tab Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabItem(
                    label = "Queue (${filteredTxns.size})",
                    icon = Icons.Default.List,
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.testTag("tab_transactions")
                )
                TabItem(
                    label = "Wallets (${wallets.size})",
                    icon = Icons.Default.Person,
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.testTag("tab_wallets")
                )
                TabItem(
                    label = "Live Simulator",
                    icon = Icons.Default.PlayArrow,
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    modifier = Modifier.testTag("tab_simulator")
                )
            }

            // Main Content Area inside animated transition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> TransactionsTabContent(
                        transactions = filteredTxns,
                        searchQuery = searchQuery,
                        selectedTypeFilter = selectedTypeFilter,
                        selectedStatusFilter = selectedStatusFilter,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onTypeFilterChange = { viewModel.selectedTypeFilter.value = it },
                        onStatusFilterChange = { viewModel.selectedStatusFilter.value = it },
                        onSelectTransaction = { viewModel.selectedTransactionForAction.value = it }
                    )
                    1 -> WalletsTabContent(
                        wallets = wallets,
                        onAddNewUserClick = { showAddUserDialog = true }
                    )
                    2 -> SimulatorTabContent(
                        wallets = wallets,
                        onSimulateRequest = { userId, type, amt, method, details ->
                            viewModel.simulateClientRequest(userId, type, amt, method, details)
                        }
                    )
                }
            }
        }
    }

    // Modal dialogue for approving/rejecting transactions with note config
    selectedTxnForAction?.let { txn ->
        ActionTransactionDialog(
            transaction = txn,
            onDismiss = { viewModel.selectedTransactionForAction.value = null },
            onApprove = { note -> viewModel.approveTransaction(txn.id, note) },
            onReject = { note -> viewModel.rejectTransaction(txn.id, note) }
        )
    }

    // Modal dialogue to create a brand new client wallet
    if (showAddUserDialog) {
        AddUserWalletDialog(
            onDismiss = { showAddUserDialog = false },
            onSubmit = { name, email, bal ->
                viewModel.createNewUserWallet(name, email, bal)
                showAddUserDialog = false
            }
        )
    }
}

@Composable
fun TabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Custom styled Widget showing visual metrics & ledger balance comparison
@Composable
fun MetricsWidget(
    metrics: DashboardMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL SYSTEM LIQUIDITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%,.2f", metrics.totalSystemLiquidity)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done, // represent ledger check
                        contentDescription = "Success tick icon",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom built mini canvas chart representing ratio of deposit vs withdraw
            Text(
                text = "Approved Ledger Volume (Ratio)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Horizontal ratio progress bar
            val totalProcessed = metrics.totalDepositsVolume + metrics.totalWithdrawalsVolume
            val depositRatio = if (totalProcessed > 0) (metrics.totalDepositsVolume / totalProcessed).toFloat() else 0.5f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(depositRatio.coerceAtLeast(0.02f))
                            .background(Color(0xFF2E7D32)) // Green for Deposits
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - depositRatio).coerceAtLeast(0.02f))
                            .background(Color(0xFFC62828)) // Red for Withdrawals
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Deposits: $${String.format("%,.0f", metrics.totalDepositsVolume)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFC62828)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Withdraws: $${String.format("%,.0f", metrics.totalWithdrawalsVolume)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom grid for remaining metadata
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pending counts
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PENDING QUEUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${metrics.pendingDepositsCount + metrics.pendingWithdrawalsCount} Requests",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (metrics.pendingDepositsCount + metrics.pendingWithdrawalsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ACTION REQ",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }

                // Total registered users
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "REGISTERED USERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${metrics.totalRegisteredUsers} Wallets",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// TAB 1: Transactions list with beautiful filter UI
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsTabContent(
    transactions: List<TransactionRecord>,
    searchQuery: String,
    selectedTypeFilter: String,
    selectedStatusFilter: String,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onSelectTransaction: (TransactionRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("txn_search_field"),
            placeholder = { Text("Search by user, ID, Ref ID, payment method...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Type filter row
        Text(
            text = "Filter by Type:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf("ALL", "DEPOSIT", "WITHDRAWAL")
            types.forEach { type ->
                val active = selectedTypeFilter == type
                FilterChip(
                    selected = active,
                    onClick = { onTypeFilterChange(type) },
                    label = { Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("filter_type_$type"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Status filter row
        Text(
            text = "Filter by Status:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statuses = listOf("ALL", "PENDING", "APPROVED", "REJECTED")
            statuses.forEach { status ->
                val active = selectedStatusFilter == status
                FilterChip(
                    selected = active,
                    onClick = { onStatusFilterChange(status) },
                    label = { Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("filter_status_$status"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No transactions found",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No matching transactions in queue",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your filters or use the 'Live Simulator' tab to request a new deposit/withdrawal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.id }
                ) { txn ->
                    TransactionItemCard(
                        transaction = txn,
                        onClick = { onSelectTransaction(txn) }
                    )
                }
            }
        }
    }
}

// Beautiful individual card displaying transaction status, amount, and quick info
@Composable
fun TransactionItemCard(
    transaction: TransactionRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeposit = transaction.transactionType == "DEPOSIT"
    val timestampFormatted = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    val statusColor = when (transaction.status) {
        "APPROVED" -> Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFC62828)
        else -> Color(0xFFE65100) // Pending
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("txn_card_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (transaction.status == "PENDING") MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDeposit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDeposit) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isDeposit) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = transaction.transactionType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeposit) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = transaction.referenceId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount text
                Text(
                    text = "${if (isDeposit) "+" else "-"}$${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = if (isDeposit) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User info & details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = transaction.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "User ID: ${transaction.userId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = transaction.paymentMethod,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = transaction.paymentDetails,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer with date and status tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timestampFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                // Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = transaction.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }

            if (!transaction.adminNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Admin private note details",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Note: ${transaction.adminNote}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// TAB 2: List of User Wallets setup
@Composable
fun WalletsTabContent(
    wallets: List<UserWallet>,
    onAddNewUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Customer Ledgers",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Simulated client wallets linked to web panel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Button(
                onClick = onAddNewUserClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_user_fab_alternative")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add user")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New User")
            }
        }

        if (wallets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded wallets. Click 'New User' to seed.",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = wallets,
                    key = { it.userId }
                ) { wallet ->
                    WalletItemRow(wallet = wallet)
                }
            }
        }
    }
}

@Composable
fun WalletItemRow(
    wallet: UserWallet,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = wallet.fullName.firstOrNull()?.toString() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = wallet.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${wallet.email}  •  ${wallet.userId}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Active indicators
                    if (wallet.activeDepositRequests > 0 || wallet.activeWithdrawRequests > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (wallet.activeDepositRequests > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Pending Depo (${wallet.activeDepositRequests})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            if (wallet.activeWithdrawRequests > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFFEBEE))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Pending WD (${wallet.activeWithdrawRequests})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Wallet Balance",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "$${String.format("%,.2f", wallet.balance)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// TAB 3: Client Simulator center allowing simulation of user actions on front-end website
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorTabContent(
    wallets: List<UserWallet>,
    onSimulateRequest: (userId: String, type: String, amt: Double, method: String, details: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (wallets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Please add users first before using Simulation center.",
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var selectedUser by remember { mutableStateOf(wallets.first()) }
    var transactionType by remember { mutableStateOf("DEPOSIT") } // DEPOSIT, WITHDRAWAL
    var inputAmount by remember { mutableStateOf("250.00") }
    var inputDetails by remember { mutableStateOf("TXoY7qP1uKWeSGe8b...") }
    var selectedMethod by remember { mutableStateOf("USDT (TRC-20)") }

    var userDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Web user Actions Simulator",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Simulates a user on the main website creating a deposit/withdrawal request, placing it directly into the admin's database queue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // User Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "1. SELECT SUBMITTING USER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = userDropdownExpanded,
                            onExpandedChange = { userDropdownExpanded = !userDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                readOnly = true,
                                value = "${selectedUser.fullName} (Bal: $${String.format("%.2f", selectedUser.balance)})",
                                onValueChange = {},
                                label = { Text("Choose User") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = userDropdownExpanded,
                                onDismissRequest = { userDropdownExpanded = false }
                            ) {
                                wallets.forEach { userWallet ->
                                    DropdownMenuItem(
                                        text = {
                                            Row {
                                                Text(userWallet.fullName, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("(Bal: $${String.format("%.2f", userWallet.balance)})", color = MaterialTheme.colorScheme.outline)
                                            }
                                        },
                                        onClick = {
                                            selectedUser = userWallet
                                            userDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Request Type Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "2. TRANSACTION REQUEST TYPE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Deposit Button
                            Button(
                                onClick = {
                                    transactionType = "DEPOSIT"
                                    selectedMethod = "USDT (TRC-20)"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transactionType == "DEPOSIT") Color(0xFF2E7D32) else Color.LightGray.copy(alpha = 0.5f),
                                    contentColor = if (transactionType == "DEPOSIT") Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DEPOSIT")
                            }

                            // Withdraw Button
                            Button(
                                onClick = {
                                    transactionType = "WITHDRAWAL"
                                    selectedMethod = "Bank Transfer"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (transactionType == "WITHDRAWAL") Color(0xFFC62828) else Color.LightGray.copy(alpha = 0.5f),
                                    contentColor = if (transactionType == "WITHDRAWAL") Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WITHDRAW (WD)")
                            }
                        }
                    }
                }
            }

            item {
                // Payment Fields and Amount
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "3. TRANSACTION DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("Amount ($ USD)") },
                            modifier = Modifier.fillMaxWidth().testTag("simulate_amt_field"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Method drop down
                        val methods = if (transactionType == "DEPOSIT") {
                            listOf("USDT (TRC-20)", "Bank Transfer", "PayPal", "Pix", "Visa/Mastercard")
                        } else {
                            listOf("Bank Transfer", "USDT (TRC-20)", "PayPal", "Pix")
                        }

                        ExposedDropdownMenuBox(
                            expanded = methodDropdownExpanded,
                            onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                readOnly = true,
                                value = selectedMethod,
                                onValueChange = {},
                                label = { Text("Payment Method") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = methodDropdownExpanded,
                                onDismissRequest = { methodDropdownExpanded = false }
                            ) {
                                methods.forEach { mth ->
                                    DropdownMenuItem(
                                        text = { Text(mth) },
                                        onClick = {
                                            selectedMethod = mth
                                            methodDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputDetails,
                            onValueChange = { inputDetails = it },
                            label = { Text("Payment Target / Details (e.g. Account count or TRON Address)") },
                            modifier = Modifier.fillMaxWidth().testTag("simulate_details_field"),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val amtParsed = inputAmount.toDoubleOrNull() ?: 0.0
                        if (amtParsed <= 0) {
                            return@Button
                        }
                        onSimulateRequest(selectedUser.userId, transactionType, amtParsed, selectedMethod, inputDetails)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("simulate_submit_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inject Simulated Request into Database")
                }
            }
        }
    }
}

// Dialog window shown to take action (Approve/Reject) on transaction
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionTransactionDialog(
    transaction: TransactionRecord,
    onDismiss: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var adminNote by remember { mutableStateOf("") }
    val isDeposit = transaction.transactionType == "DEPOSIT"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Review Request",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close overlay Dialog")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Information display block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Applicant:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = transaction.userName,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Action Type:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${transaction.transactionType} (${transaction.paymentMethod})",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDeposit) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Value Amount:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "$${String.format("%.2f", transaction.amount)}",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Target details:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = transaction.paymentDetails,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Ref Hash ID:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = transaction.referenceId,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enter admin proof notes
                OutlinedTextField(
                    value = adminNote,
                    onValueChange = { adminNote = it },
                    label = { Text("Private / Public Admin Note") },
                    placeholder = { Text("e.g. Txn verified in Binance wallet, payment sent...") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_dialog_note_input"),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action controls (Approve vs Reject)
                if (transaction.status == "PENDING") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Reject button
                        Button(
                            onClick = { onReject(adminNote) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC62828),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("reject_action_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Reject query")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject Request")
                        }

                        // Approve button
                        Button(
                            onClick = { onApprove(adminNote) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("approve_action_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Approve query")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                    }
                } else {
                    // Already processed status indication info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (transaction.status == "APPROVED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This transaction was already ${transaction.status}",
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.status == "APPROVED") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}

// Dialog to add simulated customer wallet ledgers
@Composable
fun AddUserWalletDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, initialBal: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("150.00") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Client Wallet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_user_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth().testTag("add_user_email_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Initial Balance ($ USD)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_user_balance_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val balParsed = balance.toDoubleOrNull() ?: 0.0
                        onSubmit(name, email, balParsed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_user_submit_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Wallet")
                }
            }
        }
    }
}
