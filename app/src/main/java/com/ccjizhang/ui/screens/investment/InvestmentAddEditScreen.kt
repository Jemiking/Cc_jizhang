package com.ccjizhang.ui.screens.investment

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.R
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Investment
import com.ccjizhang.data.model.Investment.Type as InvestmentType
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.common.DatePickerDialog
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.viewmodels.InvestmentViewModel
import com.ccjizhang.util.DateUtils.toDate
import com.ccjizhang.util.EnumUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentAddEditScreen(
    navController: NavHostController,
    investmentId: Long = -1L,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onSaveSuccess: () -> Unit = { navController.navigateUp() },
    viewModel: InvestmentViewModel = hiltViewModel()
) {
    val isEditMode = investmentId > 0
    val coroutineScope = rememberCoroutineScope()
    
    // Form state
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) }
    var initialAmount by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var expectedAnnualReturn by remember { mutableStateOf("") }
    var riskLevel by remember { mutableStateOf(0) }
    var account by remember { mutableStateOf<Account?>(null) }
    var institution by remember { mutableStateOf("") }
    var productCode by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var hasEndDate by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    
    // 获取账户列表
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    
    // UI state
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showMaturityDatePicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showRiskDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    
    // Error states
    var nameError by remember { mutableStateOf(false) }
    var initialAmountError by remember { mutableStateOf(false) }
    var currentValueError by remember { mutableStateOf(false) }
    var expectedAnnualReturnError by remember { mutableStateOf(false) }
    
    // Observe selected details from ViewModel
    val selectedInvestmentDetails by viewModel.selectedInvestmentDetails.collectAsState()
    
    // Initialize form if in edit mode
    LaunchedEffect(investmentId) {
        if (isEditMode) {
            // Trigger loading in ViewModel
            viewModel.selectInvestmentDetails(investmentId)
        } else {
            // Clear selection if not in edit mode (e.g., navigating back from edit)
            viewModel.selectInvestmentDetails(-1L) // Or a dedicated clear method
        }
    }
    
    // Update form state when selected details change
    LaunchedEffect(selectedInvestmentDetails) {
        if (isEditMode) {
            selectedInvestmentDetails?.let { investment ->
                name = investment.name
                type = EnumUtils.toInt(investment.type)
                initialAmount = investment.initialAmount.toString()
                currentValue = investment.currentValue.toString()
                expectedAnnualReturn = investment.expectedAnnualReturn?.toString() ?: ""
                riskLevel = investment.riskLevel ?: 0
                // Ensure accounts list is loaded before finding
                account = accounts.find { acc -> acc.id == investment.accountId }
                institution = investment.institution ?: ""
                productCode = investment.productCode ?: ""
                startDate = investment.startDate
                endDate = investment.endDate
                hasEndDate = investment.endDate != null
                notes = investment.note ?: ""
            }
        }
    }
    
    Scaffold(
        topBar = {
            CCJiZhangTopAppBar(
                title = stringResource(
                    if (isEditMode) R.string.edit_investment 
                    else R.string.add_investment
                ),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Validate form
                    nameError = name.isBlank()
                    initialAmountError = initialAmount.isBlank() || initialAmount.toDoubleOrNull() == null
                    currentValueError = currentValue.isBlank() || currentValue.toDoubleOrNull() == null
                    expectedAnnualReturnError = expectedAnnualReturn.isNotBlank() && expectedAnnualReturn.toDoubleOrNull() == null
                    
                    if (!nameError && !initialAmountError && !currentValueError && !expectedAnnualReturnError) {
                        coroutineScope.launch {
                            // Prepare data for saving
                            val initialAmountValue = initialAmount.toDoubleOrNull() ?: 0.0
                            val currentValueValue = currentValue.toDoubleOrNull() ?: 0.0
                            val expectedAnnualReturnValue = expectedAnnualReturn.toDoubleOrNull()

                            // Handle adding or updating
                            if (isEditMode && selectedInvestmentDetails != null) {
                                // Update existing investment
                                val updatedInvestment = selectedInvestmentDetails!!.copy(
                                    name = name,
                                    type = EnumUtils.fromInt(type, InvestmentType.OTHER),
                                    initialAmount = initialAmountValue,
                                    currentValue = currentValueValue,
                                    accountId = account?.id,
                                    institution = institution.takeIf { it.isNotBlank() },
                                    productCode = productCode.takeIf { it.isNotBlank() },
                                    expectedAnnualReturn = expectedAnnualReturnValue,
                                    riskLevel = riskLevel,
                                    startDate = startDate,
                                    endDate = if (hasEndDate) endDate else null,
                                    note = notes.takeIf { it.isNotBlank() }
                                )
                                viewModel.updateInvestment(updatedInvestment)
                            } else {
                                // Add new investment
                                viewModel.addInvestment(
                                    name = name,
                                    type = EnumUtils.fromInt(type, InvestmentType.OTHER),
                                    initialAmount = initialAmountValue,
                                    currentValue = currentValueValue,
                                    accountId = account?.id,
                                    institution = institution.takeIf { it.isNotBlank() },
                                    productCode = productCode.takeIf { it.isNotBlank() },
                                    expectedAnnualReturn = expectedAnnualReturnValue,
                                    riskLevel = riskLevel,
                                    startDate = startDate,
                                    endDate = if (hasEndDate) endDate else null,
                                    note = notes.takeIf { it.isNotBlank() },
                                    autoUpdateFrequencyDays = null
                                )
                            }
                            onSaveSuccess()
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.save)
                )
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
                // 名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.name)) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text(stringResource(R.string.name_required))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                
                // 投资类型
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stringResource(
                            when (type) {
                                0 -> R.string.deposit
                                1 -> R.string.stock
                                2 -> R.string.fund
                                3 -> R.string.bond
                                else -> R.string.other_investment
                            }
                        ),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.investment_type)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTypeDropdown = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.deposit)) },
                            onClick = { 
                                type = EnumUtils.toInt(InvestmentType.DEPOSIT)
                                showTypeDropdown = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stock)) },
                            onClick = { 
                                type = EnumUtils.toInt(InvestmentType.STOCK)
                                showTypeDropdown = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fund)) },
                            onClick = { 
                                type = EnumUtils.toInt(InvestmentType.FUND)
                                showTypeDropdown = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bond)) },
                            onClick = { 
                                type = EnumUtils.toInt(InvestmentType.BOND)
                                showTypeDropdown = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.other_investment)) },
                            onClick = { 
                                type = EnumUtils.toInt(InvestmentType.OTHER)
                                showTypeDropdown = false 
                            }
                        )
                    }
                }
                
                // 风险等级
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stringResource(
                            when (riskLevel) {
                                0 -> R.string.risk_low
                                1 -> R.string.risk_medium
                                2 -> R.string.risk_high
                                3 -> R.string.risk_unknown
                                else -> R.string.risk_unknown
                            }
                        ),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.risk_level)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRiskDropdown = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showRiskDropdown,
                        onDismissRequest = { showRiskDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        listOf(0, 1, 2, 3).forEach { riskLevelValue ->
                            val resourceId = when (riskLevelValue) {
                                0 -> R.string.risk_low
                                1 -> R.string.risk_medium
                                2 -> R.string.risk_high
                                else -> R.string.risk_unknown
                            }
                            
                            DropdownMenuItem(
                                text = { Text(stringResource(resourceId)) },
                                onClick = {
                                    riskLevel = riskLevelValue
                                    showRiskDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // 初始金额
                OutlinedTextField(
                    value = initialAmount,
                    onValueChange = { initialAmount = it; initialAmountError = false },
                    label = { Text(stringResource(R.string.initial_amount)) },
                    isError = initialAmountError,
                    supportingText = {
                        if (initialAmountError) {
                            Text(stringResource(R.string.valid_amount_required))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                
                // 当前价值
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { currentValue = it; currentValueError = false },
                    label = { Text(stringResource(R.string.current_value)) },
                    isError = currentValueError,
                    supportingText = {
                        if (currentValueError) {
                            Text(stringResource(R.string.valid_amount_required))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                
                // 预期年收益率
                OutlinedTextField(
                    value = expectedAnnualReturn,
                    onValueChange = { expectedAnnualReturn = it; expectedAnnualReturnError = false },
                    label = { Text(stringResource(R.string.return_rate)) },
                    isError = expectedAnnualReturnError,
                    supportingText = {
                        if (expectedAnnualReturnError) {
                            Text(stringResource(R.string.valid_percentage_required))
                        } else {
                            Text(stringResource(R.string.return_rate_hint))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // 账户
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = account?.name ?: "请选择账户",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.account)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountDropdown = true },
                        readOnly = true,
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
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                enabled = true,
                                text = { Text(acc.name) },
                                onClick = {
                                    account = acc
                                    showAccountDropdown = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_associated_account)) },
                            onClick = {
                                account = null
                                showAccountDropdown = false
                            }
                        )
                    }
                }
                
                // 购买日期
                OutlinedTextField(
                    value = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.purchase_date)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPurchaseDatePicker = true },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )
                
                // 到期日期选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasEndDate,
                        onCheckedChange = { hasEndDate = it }
                    )
                    
                    Text(
                        text = "设置到期日",
                        modifier = Modifier.clickable { hasEndDate = !hasEndDate }
                    )
                }
                
                // 到期日期
                if (hasEndDate) {
                    OutlinedTextField(
                        value = endDate?.let { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(it.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) } ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.maturity_date)) },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMaturityDatePicker = true },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                }
                
                // 备注
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
    
    // 购买日期选择器
    if (showPurchaseDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { date ->
                startDate = date
                showPurchaseDatePicker = false
            },
            onDismiss = { showPurchaseDatePicker = false }
        )
    }
    
    // 到期日期选择器
    if (showMaturityDatePicker && hasEndDate) {
        DatePickerDialog(
            initialDate = endDate ?: Date(),
            onDateSelected = { date ->
                endDate = date
                showMaturityDatePicker = false
            },
            onDismiss = { showMaturityDatePicker = false }
        )
    }
} 