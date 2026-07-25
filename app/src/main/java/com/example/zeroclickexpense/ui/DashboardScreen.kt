package com.example.zeroclickexpense.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zeroclickexpense.data.Transaction
import com.example.zeroclickexpense.viewmodel.ExpenseViewModel
import com.example.zeroclickexpense.viewmodel.FilterMode
import java.text.SimpleDateFormat
import java.util.*

// Dynamic Theme Palette Class
data class ZeroTrackColors(
    val bg: Color,
    val surface: Color,
    val dialogBg: Color,
    val border: Color,
    val primaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val income: Color,
    val expense: Color,
    val chipExpenseBg: Color,
    val chipIncomeBg: Color,
    val tabBg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ExpenseViewModel) {
    val transactions by viewModel.displayedTransactions.collectAsState(initial = emptyList())
    val filterMode by viewModel.filterMode.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val customStart by viewModel.customStartDate.collectAsState()
    val customEnd by viewModel.customEndDate.collectAsState()
    val defaultCurrency by viewModel.currencySymbol.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()
    
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }

    val palette = if (isDark) {
        ZeroTrackColors(
            bg = Color(0xFF101412),
            surface = Color(0xFF1C221F),
            dialogBg = Color(0xFF222925),
            border = Color(0xFF2D3631),
            primaryText = Color(0xFFECEFF1),
            mutedText = Color(0xFF909A94),
            accent = Color(0xFF2EAB6F),
            income = Color(0xFF4CAF50),
            expense = Color(0xFFFF6E6B),
            chipExpenseBg = Color(0xFF3B2020),
            chipIncomeBg = Color(0xFF1E3622),
            tabBg = Color(0xFF1A211D)
        )
    } else {
        ZeroTrackColors(
            bg = Color(0xFFF8F9F8),
            surface = Color(0xFFFFFFFF),
            dialogBg = Color(0xFFFFFFFF),
            border = Color(0xFFEDEDED),
            primaryText = Color(0xFF1A1A1A),
            mutedText = Color(0xFF6C757D),
            accent = Color(0xFF1B4D3E),
            income = Color(0xFF2E7D32),
            expense = Color(0xFFD9534F),
            chipExpenseBg = Color(0xFFFFEBEE),
            chipIncomeBg = Color(0xFFE8F5E9),
            tabBg = Color(0xFFEAEEEC)
        )
    }
    
    // Discover all unique currency wallets active in this period
    val activeCurrencies = remember(transactions, defaultCurrency) {
        (transactions.map { it.currency } + defaultCurrency).distinct().sorted()
    }

    // Automatically reset selectedWallet if it's invalid
    val currentWallet = if (selectedWallet == "ALL" || activeCurrencies.contains(selectedWallet)) selectedWallet else "ALL"
    val calculationCurrency = if (currentWallet == "ALL") defaultCurrency else currentWallet

    val filteredForCalculation = transactions.filter { 
        if (currentWallet == "ALL") it.currency == defaultCurrency else it.currency == currentWallet 
    }
    val totalIncome = filteredForCalculation.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = filteredForCalculation.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var transactionForAction by remember { mutableStateOf<Transaction?>(null) }
    var transactionForEdit by remember { mutableStateOf<Transaction?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val dateRangeFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ZEROTRACK", 
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 2.sp,
                            color = palette.primaryText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings & Currency", tint = palette.primaryText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = palette.accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Minimalist Tab Switcher (Month vs Custom Range)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.tabBg, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterTabButton(
                    text = "Month View",
                    isSelected = filterMode == FilterMode.MONTHLY,
                    palette = palette,
                    onClick = { viewModel.setFilterMode(FilterMode.MONTHLY) },
                    modifier = Modifier.weight(1f)
                )
                FilterTabButton(
                    text = "Custom Range",
                    isSelected = filterMode == FilterMode.CUSTOM_RANGE,
                    palette = palette,
                    onClick = { viewModel.setFilterMode(FilterMode.CUSTOM_RANGE) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Controls
            if (filterMode == FilterMode.MONTHLY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous", tint = palette.primaryText)
                    }
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedMonth.year)
                        set(Calendar.MONTH, selectedMonth.month)
                    }
                    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                    Text(
                        monthName.uppercase(), 
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = palette.primaryText
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = palette.primaryText)
                    }
                }
            } else {
                Button(
                    onClick = { showDateRangePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface, contentColor = palette.primaryText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${dateRangeFormat.format(Date(customStart))}  –  ${dateRangeFormat.format(Date(customEnd))}",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Wallet Currency Tabs (Option A - Separating THB, MMK, USD, etc.)
            if (activeCurrencies.size > 1 || activeCurrencies.firstOrNull() != defaultCurrency) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WalletChip(
                        label = "🌐 All Wallets ($defaultCurrency)",
                        isSelected = currentWallet == "ALL",
                        palette = palette,
                        onClick = { viewModel.setSelectedWallet("ALL") }
                    )
                    activeCurrencies.forEach { curr ->
                        val label = when(curr) {
                            "฿" -> "🇹🇭 ฿ THB Wallet"
                            "Ks" -> "🇲🇲 Ks MMK Wallet"
                            "$" -> "🇺🇸 $ USD Wallet"
                            "€" -> "🇪🇺 € EUR Wallet"
                            "£" -> "🇬🇧 £ GBP Wallet"
                            "¥" -> "🇯🇵 ¥ JPY Wallet"
                            "₩" -> "🇰🇷 ₩ KRW Wallet"
                            "₹" -> "🇮🇳 ₹ INR Wallet"
                            "RM" -> "🇲🇾 RM MYR Wallet"
                            else -> "$curr Wallet"
                        }
                        WalletChip(
                            label = label,
                            isSelected = currentWallet == curr,
                            palette = palette,
                            onClick = { viewModel.setSelectedWallet(curr) }
                        )
                    }
                }
            }

            // Minimalist Summary Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(20.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (currentWallet == "ALL") "NET BALANCE ($calculationCurrency WALLET)" else "NET BALANCE ($calculationCurrency)", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.SemiBold, 
                    color = palette.mutedText,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val balanceText = if (balance >= 0) {
                    "${calculationCurrency}${String.format(Locale.US, "%.2f", balance)}"
                } else {
                    "-${calculationCurrency}${String.format(Locale.US, "%.2f", Math.abs(balance))}"
                }
                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balance >= 0) palette.accent else palette.expense
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("INCOME", style = MaterialTheme.typography.labelSmall, color = palette.mutedText, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "+${calculationCurrency}${String.format(Locale.US, "%.2f", totalIncome)}", 
                            color = palette.income, 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(palette.border))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = palette.mutedText, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "-${calculationCurrency}${String.format(Locale.US, "%.2f", totalExpense)}", 
                            color = palette.expense, 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val displayList = if (currentWallet == "ALL") transactions else transactions.filter { it.currency == currentWallet }

            Text(
                if (currentWallet == "ALL") "ALL TRANSACTIONS" else "$currentWallet TRANSACTIONS", 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold, 
                color = palette.mutedText,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No records found in this wallet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayList, key = { it.id }) { transaction ->
                        MinimalistTransactionItem(
                            transaction = transaction,
                            palette = palette,
                            onClick = { transactionForAction = transaction },
                            onLongClick = { transactionForAction = transaction }
                        )
                    }
                }
            }
        }

        // Action Modal (Edit or Delete)
        transactionForAction?.let { tx ->
            AlertDialog(
                onDismissRequest = { transactionForAction = null },
                containerColor = palette.dialogBg,
                title = { Text("Transaction Options", fontWeight = FontWeight.Bold, color = palette.primaryText, style = MaterialTheme.typography.titleMedium) },
                text = { 
                    Text(
                        "What would you like to do with '${tx.merchant}' (${tx.currency}${String.format(Locale.US, "%.2f", tx.amount)})?",
                        color = palette.primaryText
                    ) 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            transactionForEdit = tx
                            transactionForAction = null
                        }
                    ) {
                        Text("Edit", color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                viewModel.delete(tx)
                                transactionForAction = null
                            }
                        ) {
                            Text("Delete", color = palette.expense, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { transactionForAction = null }) {
                            Text("Cancel", color = palette.mutedText)
                        }
                    }
                }
            )
        }

        // Edit Dialog
        transactionForEdit?.let { tx ->
            EditTransactionDialog(
                transaction = tx,
                palette = palette,
                onDismiss = { transactionForEdit = null },
                onSave = { updatedAmount, isIncome, updatedMerchant, updatedCurrency ->
                    viewModel.update(
                        tx.copy(
                            amount = updatedAmount,
                            isIncome = isIncome,
                            merchant = updatedMerchant,
                            currency = updatedCurrency
                        )
                    )
                    transactionForEdit = null
                }
            )
        }

        // Add Dialog
        if (showAddDialog) {
            AddTransactionDialog(
                defaultCurrency = if (currentWallet == "ALL") defaultCurrency else currentWallet,
                palette = palette,
                onDismiss = { showAddDialog = false },
                onAdd = { amount, isIncome, merchant, chosenCurrency ->
                    viewModel.insert(
                        Transaction(
                            amount = amount,
                            category = "Manual",
                            merchant = merchant,
                            source = "Manual Entry",
                            date = System.currentTimeMillis(),
                            isIncome = isIncome,
                            currency = chosenCurrency
                        )
                    )
                    showAddDialog = false
                }
            )
        }

        // Settings Dialog (Currency & Theme Selection)
        if (showSettingsDialog) {
            SettingsDialog(
                currentCurrency = defaultCurrency,
                currentTheme = themeMode,
                palette = palette,
                onDismiss = { showSettingsDialog = false },
                onSelectCurrency = { viewModel.setCurrencySymbol(it) },
                onSelectTheme = { viewModel.setThemeMode(it) }
            )
        }

        // Date Range Picker Dialog
        if (showDateRangePicker) {
            val dateRangeState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = customStart,
                initialSelectedEndDateMillis = customEnd
            )
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val s = dateRangeState.selectedStartDateMillis ?: customStart
                            val e = dateRangeState.selectedEndDateMillis ?: customEnd
                            viewModel.setCustomDateRange(s, e)
                            showDateRangePicker = false
                        }
                    ) {
                        Text("Apply", color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancel", color = palette.mutedText)
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangeState,
                    modifier = Modifier.fillMaxWidth().height(400.dp)
                )
            }
        }
    }
}

@Composable
fun WalletChip(label: String, isSelected: Boolean, palette: ZeroTrackColors, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) palette.accent else palette.surface,
            contentColor = if (isSelected) Color.White else palette.primaryText
        ),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, palette.border) else null,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(4.dp) else ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun FilterTabButton(text: String, isSelected: Boolean, palette: ZeroTrackColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) palette.surface else Color.Transparent,
            contentColor = if (isSelected) palette.primaryText else palette.mutedText
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier
    ) {
        Text(text, maxLines = 1, softWrap = false, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalistTransactionItem(
    transaction: Transaction,
    palette: ZeroTrackColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(palette.surface, RoundedCornerShape(16.dp))
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.merchant.ifEmpty { "Transaction" }, 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.bodyMedium,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dateFormat.format(Date(transaction.date)), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = palette.mutedText
                )
                Text(" • ", color = palette.mutedText, style = MaterialTheme.typography.labelSmall)
                Text(
                    transaction.source.substringAfterLast("."), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = palette.mutedText
                )
            }
        }
        Text(
            text = "${if (transaction.isIncome) "+" else "-"}${transaction.currency}${String.format(Locale.US, "%.2f", transaction.amount)}",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.bodyLarge,
            color = if (transaction.isIncome) palette.income else palette.expense
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    defaultCurrency: String,
    palette: ZeroTrackColors,
    onDismiss: () -> Unit,
    onAdd: (Double, Boolean, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }

    val allCurrencies = listOf("฿", "Ks", "$", "€", "£", "¥", "₩", "₹", "RM")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.primaryText,
        unfocusedTextColor = palette.primaryText,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.border,
        focusedLabelColor = palette.accent,
        unfocusedLabelColor = palette.mutedText,
        cursorColor = palette.accent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.dialogBg,
        title = { Text("New Transaction", fontWeight = FontWeight.Bold, color = palette.primaryText) },
        text = {
            Column {
                Text("WALLET CURRENCY", style = MaterialTheme.typography.labelSmall, color = palette.mutedText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCurrencies.forEach { sym ->
                        FilterChip(
                            selected = selectedCurrency == sym,
                            onClick = { selectedCurrency = sym },
                            label = { Text(sym, fontWeight = if (selectedCurrency == sym) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent, selectedLabelColor = Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($selectedCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.chipExpenseBg, selectedLabelColor = palette.expense)
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.chipIncomeBg, selectedLabelColor = palette.income)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onAdd(amount, isIncome, merchantText.ifEmpty { if (isIncome) "Income" else "Expense" }, selectedCurrency)
                    }
                }
            ) {
                Text("Save", color = palette.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.mutedText)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    palette: ZeroTrackColors,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", transaction.amount)) }
    var merchantText by remember { mutableStateOf(transaction.merchant) }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }
    var selectedCurrency by remember { mutableStateOf(transaction.currency) }

    val allCurrencies = listOf("฿", "Ks", "$", "€", "£", "¥", "₩", "₹", "RM")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.primaryText,
        unfocusedTextColor = palette.primaryText,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.border,
        focusedLabelColor = palette.accent,
        unfocusedLabelColor = palette.mutedText,
        cursorColor = palette.accent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.dialogBg,
        title = { Text("Edit Transaction", fontWeight = FontWeight.Bold, color = palette.primaryText) },
        text = {
            Column {
                Text("WALLET CURRENCY", style = MaterialTheme.typography.labelSmall, color = palette.mutedText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCurrencies.forEach { sym ->
                        FilterChip(
                            selected = selectedCurrency == sym,
                            onClick = { selectedCurrency = sym },
                            label = { Text(sym, fontWeight = if (selectedCurrency == sym) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.accent, selectedLabelColor = Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($selectedCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.chipExpenseBg, selectedLabelColor = palette.expense)
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = palette.chipIncomeBg, selectedLabelColor = palette.income)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onSave(amount, isIncome, merchantText.ifEmpty { if (isIncome) "Income" else "Expense" }, selectedCurrency)
                    }
                }
            ) {
                Text("Update", color = palette.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.mutedText)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentCurrency: String,
    currentTheme: String,
    palette: ZeroTrackColors,
    onDismiss: () -> Unit,
    onSelectCurrency: (String) -> Unit,
    onSelectTheme: (String) -> Unit
) {
    val currencies = listOf(
        "฿" to "Baht (THB)", 
        "Ks" to "Kyat (MMK)",
        "$" to "Dollar (USD)", 
        "€" to "Euro (EUR)", 
        "£" to "Pound (GBP)", 
        "¥" to "Yen/Yuan (JPY)", 
        "₩" to "Won (KRW)", 
        "₹" to "Rupee (INR)", 
        "RM" to "Ringgit (MYR)"
    )
    val themes = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.dialogBg,
        title = { Text("Preferences", fontWeight = FontWeight.Bold, color = palette.primaryText) },
        text = {
            Column {
                Text("APPEARANCE THEME", style = MaterialTheme.typography.labelSmall, color = palette.mutedText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    themes.forEach { (mode, label) ->
                        Button(
                            onClick = { onSelectTheme(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentTheme == mode) palette.accent else palette.surface,
                                contentColor = if (currentTheme == mode) Color.White else palette.primaryText
                            ),
                            border = if (currentTheme != mode) androidx.compose.foundation.BorderStroke(1.dp, palette.border) else null,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label, 
                                maxLines = 1, 
                                softWrap = false,
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = if (currentTheme == mode) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(16.dp))

                Text("PRIMARY DEFAULT CURRENCY", style = MaterialTheme.typography.labelSmall, color = palette.mutedText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currencies.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { (symbol, name) ->
                                Button(
                                    onClick = { onSelectCurrency(symbol) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentCurrency == symbol) palette.accent else palette.surface,
                                        contentColor = if (currentCurrency == symbol) Color.White else palette.primaryText
                                    ),
                                    border = if (currentCurrency != symbol) androidx.compose.foundation.BorderStroke(1.dp, palette.border) else null,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "$symbol  ${name.substringBefore(" ")}", 
                                        maxLines = 1, 
                                        softWrap = false,
                                        style = MaterialTheme.typography.bodySmall, 
                                        fontWeight = if (currentCurrency == symbol) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }
    )
}
