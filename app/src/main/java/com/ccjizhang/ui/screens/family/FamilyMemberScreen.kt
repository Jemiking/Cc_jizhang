package com.ccjizhang.ui.screens.family

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.R
import com.ccjizhang.data.model.FamilyMember
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.viewmodels.FamilyMemberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberScreen(
    navController: NavHostController,
    onNavigateToAddFamilyMember: () -> Unit,
    onNavigateToEditFamilyMember: (Long) -> Unit,
    viewModel: FamilyMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    
    Scaffold(
        topBar = {
            CCJiZhangTopAppBar(
                title = stringResource(R.string.family_members),
                canNavigateBack = true,
                onNavigateBack = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddFamilyMember,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_family_member)
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            uiState.familyMembers.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(R.string.no_family_members),
                    message = stringResource(R.string.no_family_members_message),
                    onActionClick = onNavigateToAddFamilyMember,
                    actionText = stringResource(R.string.add_family_member)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.familyMembers) { member ->
                        FamilyMemberItem(
                            member = member,
                            onMemberClick = { onNavigateToEditFamilyMember(member.id) },
                            onDeleteClick = {
                                selectedMember = member
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog && selectedMember != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_family_member),
            message = stringResource(
                R.string.delete_family_member_confirmation,
                selectedMember?.name ?: ""
            ),
            onConfirm = {
                viewModel.deleteFamilyMember(selectedMember!!.id)
                showDeleteDialog = false
                selectedMember = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedMember = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberItem(
    member: FamilyMember,
    onMemberClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMemberClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (member.isOwner) Icons.Default.Star else Icons.Default.Person,
                contentDescription = null,
                tint = if (member.isOwner) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = if (member.isOwner) 
                        stringResource(R.string.account_owner) 
                    else 
                        stringResource(R.string.family_member),
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (member.email.isNotBlank()) {
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (!member.isOwner) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            }
        }
    }
} 