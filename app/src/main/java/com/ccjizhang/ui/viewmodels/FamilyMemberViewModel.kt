package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.FamilyMember
import com.ccjizhang.data.repository.FamilyMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * 家庭成员UI状态
 */
data class FamilyMemberUiState(
    val isLoading: Boolean = false,
    val familyMembers: List<FamilyMember> = emptyList(),
    val editingMember: FamilyMember? = null,
    val errorMessage: String? = null
)

/**
 * 家庭成员标签页
 */
enum class FamilyMemberTab {
    ACTIVE, PENDING, OWNERS, ADMINS, EDITORS, VIEWERS
}

/**
 * 家庭成员视图模型
 * 负责家庭共享记账功能的数据处理和业务逻辑
 */
@HiltViewModel
class FamilyMemberViewModel @Inject constructor(
    private val familyMemberRepository: FamilyMemberRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(FamilyMemberUiState(isLoading = true))
    val uiState: StateFlow<FamilyMemberUiState> = _uiState

    // 所有家庭成员
    val allFamilyMembers: Flow<List<FamilyMember>> = familyMemberRepository.getAllFamilyMembers()

    // 活跃的家庭成员
    val activeFamilyMembers: Flow<List<FamilyMember>> = familyMemberRepository.getActiveFamilyMembers()

    // 待接受邀请的家庭成员
    val pendingFamilyMembers: Flow<List<FamilyMember>> = familyMemberRepository.getPendingFamilyMembers()

    // 通过角色筛选的成员（拥有者/管理员/编辑者/查看者）
    val ownerMembers: Flow<List<FamilyMember>> = familyMemberRepository.getFamilyMembersByRole(0)
    val adminMembers: Flow<List<FamilyMember>> = familyMemberRepository.getFamilyMembersByRole(1)
    val editorMembers: Flow<List<FamilyMember>> = familyMemberRepository.getFamilyMembersByRole(2)
    val viewerMembers: Flow<List<FamilyMember>> = familyMemberRepository.getFamilyMembersByRole(3)

    // 当前选中的标签页
    private val _selectedTab = MutableStateFlow(FamilyMemberTab.ACTIVE)
    val selectedTab: StateFlow<FamilyMemberTab> = _selectedTab

    // 当前选中的家庭成员ID
    private val _selectedMemberId = MutableStateFlow<Long?>(null)
    val selectedMemberId: StateFlow<Long?> = _selectedMemberId

    // 搜索查询文本
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 初始化
    init {
        viewModelScope.launch {
            try {
                // 设置初始成员列表
                activeFamilyMembers.collect { members ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        familyMembers = members
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // 搜索结果
    val searchResults: Flow<List<FamilyMember>> = _searchQuery.map { query ->
        if (query.isBlank()) emptyList()
        else familyMemberRepository.searchFamilyMembers(query).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        ).value
    }

    // 根据当前选择的标签页显示相应的家庭成员
    val currentTabMembers = combine(
        selectedTab,
        activeFamilyMembers,
        pendingFamilyMembers,
        ownerMembers,
        adminMembers,
        editorMembers,
        viewerMembers,
        searchResults
    ) { args -> 
        val tab = args[0] as FamilyMemberTab
        val active = args[1] as List<FamilyMember>
        val pending = args[2] as List<FamilyMember>
        val owners = args[3] as List<FamilyMember>
        val admins = args[4] as List<FamilyMember>
        val editors = args[5] as List<FamilyMember>
        val viewers = args[6] as List<FamilyMember>
        val search = args[7] as List<FamilyMember>
        
        when {
            searchQuery.value.isNotBlank() -> search
            tab == FamilyMemberTab.ACTIVE -> active
            tab == FamilyMemberTab.PENDING -> pending
            tab == FamilyMemberTab.OWNERS -> owners
            tab == FamilyMemberTab.ADMINS -> admins
            tab == FamilyMemberTab.EDITORS -> editors
            tab == FamilyMemberTab.VIEWERS -> viewers
            else -> active
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 选择标签页
    fun selectTab(tab: FamilyMemberTab) {
        _selectedTab.value = tab
    }

    // 选择家庭成员
    fun selectMember(id: Long?) {
        _selectedMemberId.value = id
    }

    // 设置搜索查询
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // 加载指定ID的家庭成员
    fun loadFamilyMember(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val member = familyMemberRepository.getFamilyMemberById(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    editingMember = member
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // 添加新的家庭成员
    fun addFamilyMember(familyMember: FamilyMember) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                familyMemberRepository.addFamilyMember(familyMember)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // 更新家庭成员
    fun updateFamilyMember(familyMember: FamilyMember) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                familyMemberRepository.updateFamilyMember(familyMember)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // 删除家庭成员
    fun deleteFamilyMember(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val member = familyMemberRepository.getFamilyMemberById(id)
                member?.let {
                    familyMemberRepository.deleteFamilyMember(it)
                    if (_selectedMemberId.value == id) {
                        _selectedMemberId.value = null
                    }
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    // 更新家庭成员状态
    fun updateMemberStatus(id: Long, status: Int) {
        viewModelScope.launch {
            familyMemberRepository.updateMemberStatus(id, status)
        }
    }

    // 更新家庭成员角色
    fun updateMemberRole(id: Long, role: Int) {
        viewModelScope.launch {
            familyMemberRepository.updateMemberRole(id, role)
        }
    }

    // 更新家庭成员最后活跃时间
    fun updateMemberLastActiveTime(id: Long) {
        viewModelScope.launch {
            familyMemberRepository.updateMemberLastActiveTime(id)
        }
    }

    // 创建邀请链接
    fun createInvitationLink(email: String, role: Int): String {
        return familyMemberRepository.createInvitationLink(email, role)
    }

    // 获取角色名称
    fun getRoleName(role: Int): String {
        return when (role) {
            0 -> "拥有者"
            1 -> "管理员"
            2 -> "编辑者"
            3 -> "查看者"
            else -> "未知"
        }
    }

    // 获取状态名称
    fun getStatusName(status: Int): String {
        return when (status) {
            0 -> "已接受"
            1 -> "待接受邀请"
            2 -> "已拒绝"
            3 -> "已离开"
            else -> "未知"
        }
    }
} 