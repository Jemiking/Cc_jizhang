package com.ccjizhang.ui.screens.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ccjizhang.R
import com.ccjizhang.data.model.FamilyMember
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.viewmodels.FamilyMemberViewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberAddEditScreen(
    navController: NavHostController,
    memberId: Long = -1L,
    onSaveSuccess: () -> Unit,
    viewModel: FamilyMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = memberId > 0
    
    // Form state
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isOwner by remember { mutableStateOf(false) }
    var canEditTransactions by remember { mutableStateOf(false) }
    var canViewAllTransactions by remember { mutableStateOf(true) }
    var spendingLimit by remember { mutableStateOf("") }
    
    // Error states
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    
    // Initialize form if in edit mode
    LaunchedEffect(memberId) {
        if (isEditMode) {
            viewModel.loadFamilyMember(memberId)
        }
    }
    
    // Update form when member is loaded
    LaunchedEffect(uiState.editingMember) {
        uiState.editingMember?.let { member ->
            name = member.name
            email = member.email
            phone = member.phone
            isOwner = member.isOwner
            canEditTransactions = member.canEditTransactions
            canViewAllTransactions = member.canViewAllTransactions
            spendingLimit = if (member.spendingLimit > 0) member.spendingLimit.toString() else ""
        }
    }
    
    Scaffold(
        topBar = {
            CCJiZhangTopAppBar(
                title = stringResource(
                    if (isEditMode) R.string.edit_family_member 
                    else R.string.add_family_member
                ),
                canNavigateBack = true,
                onNavigateBack = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Validate form
                    nameError = name.isBlank()
                    
                    if (!nameError) {
                        val member = FamilyMember(
                            id = if (isEditMode) memberId else 0,
                            name = name,
                            email = email,
                            phone = phone,
                            avatarUri = null,
                            role = if (isOwner) 0 else 2, // 0 表示拥有者，2 表示编辑者
                            note = null,
                            isOwner = isOwner,
                            canEditTransactions = canEditTransactions,
                            canViewAllTransactions = canViewAllTransactions,
                            spendingLimit = spendingLimit.toDoubleOrNull() ?: 0.0
                        )
                        
                        if (isEditMode) {
                            viewModel.updateFamilyMember(member)
                        } else {
                            viewModel.addFamilyMember(member)
                        }
                        
                        onSaveSuccess()
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
        if (uiState.isLoading) {
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
                // Name
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
                
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = false },
                    label = { Text(stringResource(R.string.email)) },
                    isError = emailError,
                    supportingText = {
                        if (emailError) {
                            Text(stringResource(R.string.invalid_email))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                
                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = false },
                    label = { Text(stringResource(R.string.phone)) },
                    isError = phoneError,
                    supportingText = {
                        if (phoneError) {
                            Text(stringResource(R.string.invalid_phone))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )
                
                // Spending limit
                OutlinedTextField(
                    value = spendingLimit,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            spendingLimit = it
                        }
                    },
                    label = { Text(stringResource(R.string.spending_limit)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = { Text(stringResource(R.string.spending_limit_hint)) }
                )
                
                // Permissions section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.permissions),
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        // Is Owner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.account_owner))
                            Switch(
                                checked = isOwner,
                                onCheckedChange = { 
                                    isOwner = it
                                    // If owner, enable all permissions
                                    if (it) {
                                        canEditTransactions = true
                                        canViewAllTransactions = true
                                    }
                                },
                                enabled = !isEditMode || !uiState.editingMember?.isOwner!! // Can't change owner status of existing owner
                            )
                        }
                        
                        // Can edit transactions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.can_edit_transactions))
                            Switch(
                                checked = canEditTransactions,
                                onCheckedChange = { canEditTransactions = it },
                                enabled = !isOwner // Owner always has edit rights
                            )
                        }
                        
                        // Can view all transactions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.can_view_all_transactions))
                            Switch(
                                checked = canViewAllTransactions,
                                onCheckedChange = { canViewAllTransactions = it },
                                enabled = !isOwner // Owner always has view rights
                            )
                        }
                    }
                }
                
                // Info text
                Text(
                    text = stringResource(R.string.family_member_permissions_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 