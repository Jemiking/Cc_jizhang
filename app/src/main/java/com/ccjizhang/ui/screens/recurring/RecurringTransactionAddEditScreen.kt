package com.ccjizhang.ui.screens.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.R
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.RecurringTransaction
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.common.DatePickerDialog
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.viewmodels.RecurringTransactionViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionAddEditScreen(
    navController: NavHostController,
    transactionId: Long = -1L,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onSaveSuccess: () -> Unit = { navController.navigateUp() },
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val isEditMode = transactionId > 0
    val coroutineScope = rememberCoroutineScope()

    // Form state
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(0) } // 0: Expense, 1: Income, 2: Transfer
    var notes by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedToAccount by remember { mutableStateOf<Account?>(null) } // For Transfers
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var startDate by remember { mutableStateOf(Date()) } // Use Date for consistency with Model/DatePicker
    var endDate by remember { mutableStateOf<Date?>(null) }
    var hasEndDate by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf(3) } // Default to Monthly (index 3)
    var customRecurrenceDays by remember { mutableStateOf("") } // For custom type 6
    var specificRecurrenceDay by remember { mutableStateOf<String?>(null) } // For monthly/yearly
    var weekdayMask by remember { mutableStateOf<Int?>(null) } // For weekly
    var maxExecutions by remember { mutableStateOf("0") } // 0 for infinite
    var notifyBefore by remember { mutableStateOf(false) }
    var notifyDays by remember { mutableStateOf("1") }

    // Derived state for UI consistency
    val isExpense = transactionType == 0
    val isIncome = transactionType == 1
    val isTransfer = transactionType == 2

    // UI state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showToAccountDropdown by remember { mutableStateOf(false) } // For Transfers
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }

    // Data from ViewModel
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val incomeCategories by viewModel.incomeCategories.collectAsState(initial = emptyList())

    val categoriesToShow = if (isExpense) expenseCategories else incomeCategories

    // TODO: Add loading state handling from ViewModel if needed
    val isLoading = false // Placeholder

    // Error states
    var descriptionError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf(false) }
    var toAccountError by remember { mutableStateOf(false) } // For Transfers
    var categoryError by remember { mutableStateOf(false) }
    var customDaysError by remember { mutableStateOf(false) }
    var specificDayError by remember { mutableStateOf(false) }
    var maxExecutionsError by remember { mutableStateOf(false) }
    var notifyDaysError by remember { mutableStateOf(false) }

    // Observe selected details from ViewModel
    val selectedTransactionDetails by viewModel.selectedTransactionDetails.collectAsState()

    // Initialize form if in edit mode
    LaunchedEffect(transactionId) {
        if (isEditMode) {
            // Trigger loading in ViewModel
            viewModel.selectTransactionDetails(transactionId)
        } else {
            // Clear selection if not in edit mode
            viewModel.selectTransactionDetails(-1L) // Or a dedicated clear method
        }
    }

    // Update form state when selected details change
    LaunchedEffect(selectedTransactionDetails) {
        if (isEditMode) {
            selectedTransactionDetails?.let { transaction ->
                description = transaction.description
                amount = transaction.amount.toString()
                transactionType = transaction.type
                notes = transaction.note ?: ""
                // Ensure lists are loaded before finding
                selectedAccount = accounts.find { it.id == transaction.fromAccountId }
                selectedToAccount = accounts.find { it.id == transaction.toAccountId }
                selectedCategory = (expenseCategories + incomeCategories).find { it.id == transaction.categoryId }
                startDate = transaction.firstExecutionDate
                endDate = transaction.endDate
                hasEndDate = transaction.endDate != null
                recurrenceType = transaction.recurrenceType
                customRecurrenceDays = transaction.customRecurrenceDays?.toString() ?: ""
                specificRecurrenceDay = transaction.specificRecurrenceDay
                weekdayMask = transaction.weekdayMask
                maxExecutions = transaction.maxExecutions.toString()
                notifyBefore = transaction.notifyBeforeExecution
                notifyDays = transaction.notifyDaysBefore?.toString() ?: "1"
            }
        }
    }

    UnifiedScaffold(
        title = stringResource(
            if (isEditMode) R.string.edit_recurring_transaction
            else R.string.add_recurring_transaction
        ),
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = true,
        floatingActionButtonContent = {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = stringResource(R.string.save),
                tint = androidx.compose.ui.graphics.Color.White
            )
        },
        onFloatingActionButtonClick = {
            // Validate form
            descriptionError = description.isBlank()
            amountError = amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0
            accountError = selectedAccount == null
            toAccountError = isTransfer && selectedToAccount == null
            categoryError = !isTransfer && selectedCategory == null

            if (!descriptionError && !amountError && !accountError && !toAccountError && !categoryError) {
                coroutineScope.launch {
                    val amountValue = amount.toDouble()
                    val maxExecutionsValue = maxExecutions.toIntOrNull() ?: 0
                    val notifyDaysValue = if (notifyBefore) notifyDays.toIntOrNull() ?: 1 else null
                    val customDaysValue = if (recurrenceType == 6) customRecurrenceDays.toIntOrNull() else null

                    if (isEditMode && selectedTransactionDetails != null) {
                        // Update
                        val updatedTransaction = selectedTransactionDetails!!.copy(
                            type = transactionType,
                            amount = amountValue,
                            description = description,
                            categoryId = if (isTransfer) null else selectedCategory?.id,
                            fromAccountId = selectedAccount!!.id,
                            toAccountId = if (isTransfer) selectedToAccount?.id else null,
                            firstExecutionDate = startDate,
                            endDate = if (hasEndDate) endDate else null,
                            recurrenceType = recurrenceType,
                            customRecurrenceDays = customDaysValue,
                            specificRecurrenceDay = if (recurrenceType == 3 || recurrenceType == 5) specificRecurrenceDay else null,
                            weekdayMask = if (recurrenceType == 1) weekdayMask else null,
                            // nextExecutionDate might need recalculation here if start date/pattern changed
                            nextExecutionDate = selectedTransactionDetails!!.nextExecutionDate, // Keep original for now
                            maxExecutions = maxExecutionsValue,
                            note = notes.takeIf { it.isNotBlank() },
                            notifyBeforeExecution = notifyBefore,
                            notifyDaysBefore = notifyDaysValue,
                            updatedAt = Date()
                        )
                        viewModel.updateRecurringTransaction(updatedTransaction)
                    }
                    onSaveSuccess()
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type toggle (Income/Expense/Transfer)
                SecondaryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = transactionType == 0,
                            onClick = { transactionType = 0; selectedCategory = null }, // Reset category
                            label = { Text(stringResource(R.string.expense)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.error,
                                selectedLabelColor = MaterialTheme.colorScheme.onError
                            )
                        )

                        FilterChip(
                            selected = transactionType == 1,
                            onClick = { transactionType = 1; selectedCategory = null }, // Reset category
                            label = { Text(stringResource(R.string.income)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        FilterChip(
                            selected = transactionType == 2,
                            onClick = { transactionType = 2; selectedCategory = null }, // Reset category
                            label = { Text(stringResource(R.string.transfer)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; descriptionError = false },
                    label = { Text(stringResource(R.string.description)) },
                    isError = descriptionError,
                    supportingText = {
                        if (descriptionError) {
                            Text(stringResource(R.string.description_required))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountError = false },
                    label = { Text(stringResource(R.string.amount)) },
                    isError = amountError,
                    supportingText = {
                        if (amountError) {
                            Text(stringResource(R.string.valid_amount_required))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                // From Account dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: stringResource(R.string.select_from_account),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.from_account)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountDropdown = true },
                        readOnly = true,
                        isError = accountError,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        accounts.forEach { acc ->
                            if (acc != selectedAccount) { // Cannot transfer to the same account
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        selectedAccount = acc
                                        showAccountDropdown = false
                                        accountError = false
                                    }
                                )
                            }
                        }
                    }
                }

                // To Account dropdown (Only for Transfers)
                if (isTransfer) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedToAccount?.name ?: stringResource(R.string.select_to_account),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.to_account)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showToAccountDropdown = true },
                            readOnly = true,
                            isError = toAccountError,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showToAccountDropdown,
                            onDismissRequest = { showToAccountDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            accounts.forEach { acc ->
                                if (acc != selectedAccount) { // Cannot transfer to the same account
                                    DropdownMenuItem(
                                        text = { Text(acc.name) },
                                        onClick = {
                                            selectedToAccount = acc
                                            showToAccountDropdown = false
                                            toAccountError = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Category dropdown (Not for Transfers)
                if (!isTransfer) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: stringResource(R.string.select_category),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.category)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDropdown = true },
                            readOnly = true,
                            isError = categoryError,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            categoriesToShow.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Start Date
                val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                OutlinedTextField(
                    value = dateFormatter.format(startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()), // Format Date
                    onValueChange = {},
                    label = { Text(stringResource(R.string.start_date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null
                        )
                    }
                )

                // Frequency dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stringResource(
                            when (recurrenceType) {
                                0 -> R.string.daily
                                1 -> R.string.weekly
                                2 -> R.string.bi_weekly // Added Bi-Weekly
                                3 -> R.string.monthly
                                4 -> R.string.quarterly // Added Quarterly
                                5 -> R.string.yearly
                                6 -> R.string.custom_days
                                else -> R.string.unknown
                            }
                        ),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.frequency)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFrequencyDropdown = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showFrequencyDropdown,
                        onDismissRequest = { showFrequencyDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        listOf(0, 1, 2, 3, 4, 5, 6).forEach { typeIndex ->
                            val resourceId = when (typeIndex) {
                                0 -> R.string.daily
                                1 -> R.string.weekly
                                2 -> R.string.bi_weekly
                                3 -> R.string.monthly
                                4 -> R.string.quarterly
                                5 -> R.string.yearly
                                6 -> R.string.custom_days
                                else -> R.string.unknown
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(resourceId)) },
                                onClick = {
                                    recurrenceType = typeIndex
                                    showFrequencyDropdown = false
                                }
                            )
                        }
                    }
                }

                // Specific day/week/month input (depends on frequency)
                when (recurrenceType) {
                    1 -> { // Weekly
                        // TODO: Implement Weekday Picker (using weekdayMask)
                        OutlinedTextField(
                            value = weekdayMask?.toString() ?: "", // Placeholder
                            onValueChange = { /* TODO */ },
                            label = { Text("星期几 (TODO)") }, // Placeholder
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = { Text("使用位掩码, 1=周日, 2=周一...") }
                        )
                    }
                    3 -> { // Monthly
                        OutlinedTextField(
                            value = specificRecurrenceDay ?: "",
                            onValueChange = {
                                val value = it.toIntOrNull()
                                if (value != null && value in 1..31) {
                                    specificRecurrenceDay = it
                                    specificDayError = false
                                } else if (it.isEmpty()) {
                                    specificRecurrenceDay = null
                                }
                            },
                            label = { Text(stringResource(R.string.day_of_month)) },
                            isError = specificDayError,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = { Text(if (specificDayError) "请输入有效的日期 (1-31)" else "每月执行的日期 (1-31)") }
                        )
                    }
                    5 -> { // Yearly
                        OutlinedTextField(
                            value = specificRecurrenceDay ?: "",
                            onValueChange = { /* TODO: Validate MM-DD format */
                                specificRecurrenceDay = it
                                specificDayError = false
                            },
                            label = { Text("每年执行日期 (MM-DD)") },
                            isError = specificDayError,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            supportingText = { Text(if (specificDayError) "请输入有效的格式 (MM-DD)" else "例如 03-15") }
                        )
                    }
                    6 -> { // Custom Days
                        OutlinedTextField(
                            value = customRecurrenceDays,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    customRecurrenceDays = it
                                    customDaysError = false
                                }
                            },
                            label = { Text(stringResource(R.string.custom_days_interval)) },
                            isError = customDaysError,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            supportingText = { Text(if (customDaysError) "请输入有效的天数" else "每隔多少天执行一次") }
                        )
                    }
                    else -> {} // Daily, Bi-Weekly, Quarterly don't need specific day input here
                }

                // End Date Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasEndDate,
                        onCheckedChange = { hasEndDate = it }
                    )
                    Text(
                        text = stringResource(R.string.set_end_date),
                        modifier = Modifier.clickable { hasEndDate = !hasEndDate }
                    )
                }

                // End Date Picker
                if (hasEndDate) {
                    OutlinedTextField(
                        value = if (endDate != null) dateFormatter.format(endDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) else "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.end_date)) },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                }

                // Max Executions
                OutlinedTextField(
                    value = maxExecutions,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            maxExecutions = it
                            maxExecutionsError = false
                        }
                    },
                    label = { Text(stringResource(R.string.max_executions)) },
                    isError = maxExecutionsError,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = { Text(if (maxExecutionsError) "请输入有效的数字" else "0 表示无限次") }
                )

                // Notification Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = notifyBefore,
                        onCheckedChange = { notifyBefore = it }
                    )
                    Text(
                        text = stringResource(R.string.notify_before_execution),
                        modifier = Modifier.clickable { notifyBefore = !notifyBefore }
                    )
                }

                // Notify Days Before
                if (notifyBefore) {
                    OutlinedTextField(
                        value = notifyDays,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                notifyDays = it
                                notifyDaysError = false
                            }
                        },
                        label = { Text(stringResource(R.string.notify_days_before)) },
                        isError = notifyDaysError,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        supportingText = { Text(if (notifyDaysError) "请输入有效的天数" else "提前几天通知") }
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    minLines = 3
                )
            }
        }
    }

    // Start Date picker dialog
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { newDate ->
                startDate = newDate
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    // End Date picker dialog
    if (showEndDatePicker && hasEndDate) {
        DatePickerDialog(
            initialDate = endDate ?: Date(),
            onDateSelected = { newDate ->
                endDate = newDate
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}