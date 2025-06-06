# CC记账 全应用UI更新计划

## 更新目标

基于主页的UI设计为模板，统一更新应用中所有页面的UI设计，提升整体视觉一致性和用户体验。

## 主页UI设计特点

1. **统一状态栏颜色**：系统状态栏与APP顶部栏颜色统一为蓝色
2. **简化浮动按钮**：只在主页和交易列表页面保留中央底部的主浮动按钮，其他页面不使用浮动按钮
3. **卡片底色优化**：采用单色方案，所有卡片使用白色背景，通过不同的阴影高度区分卡片重要性
   - 主要信息卡片：阴影高度 3dp
   - 次要信息卡片：阴影高度 1dp
4. **简化布局**：移除冗余元素，减少重复功能区

## 更新进度跟踪

### 第一阶段：分析和准备 [已完成]

- [x] 整理页面清单
- [x] 制定设计规范
- [x] 创建UI组件库

#### 页面清单

**主要页面**：
- [x] 主页 (已完成)
- [x] 交易列表页面
- [x] 账户管理页面
- [x] 预算管理页面
- [x] 统计分析页面
- [x] 设置页面

**账户相关页面**：
- [x] 账户详情页面
- [x] 添加/编辑账户页面
- [x] 账户分类管理页面
- [x] 账户转账页面
- [x] 账户余额调整页面

**交易相关页面**：
- [x] 交易详情页面
- [x] 添加/编辑交易页面

**预算相关页面**：
- [x] 预算详情页面
- [x] 添加/编辑预算页面

**统计相关页面**：
- [x] 统计详情页面

**设置相关页面**：
- [x] 分类管理页面
- [x] 标签管理页面
- [x] 数据备份页面
- [x] 主题设置页面
- [x] 货币设置页面
- [x] 通知设置页面

**高级功能页面**：
- [x] 储蓄目标页面
- [x] 定期交易页面
- [x] 投资管理页面
- [x] 财务报告页面
- [x] 家庭成员页面

### 第二阶段：UI组件统一更新 [已完成]

- [x] 顶部栏统一
- [x] 卡片样式统一
- [x] 按钮样式统一
- [x] 列表项样式统一

### 第三阶段：页面类型更新 [已完成]

#### 列表页面更新
- [x] 交易列表页面
- [x] 账户列表页面
- [x] 预算列表页面
- [x] 分类管理列表页面
- [x] 标签管理列表页面
- [x] 其他列表页面

#### 详情页面更新
- [x] 账户详情页面
- [x] 交易详情页面
- [x] 预算详情页面
- [x] 其他详情页面

#### 表单页面更新
- [x] 添加/编辑账户页面
- [x] 添加/编辑交易页面
- [x] 添加/编辑预算页面
- [x] 其他表单页面

#### 统计/分析页面更新
- [x] 统计概览页面
- [x] 统计详情页面

### 第四阶段：测试和优化 [已完成]

- [x] UI一致性测试
- [x] 用户体验测试
- [x] 性能优化

## 设计规范

### 颜色方案

- 主色调：蓝色 (#1976D2)
- 背景色：白色 (#FFFFFF)
- 文本颜色：
  - 主要文本：深灰色 (#212121)
  - 次要文本：中灰色 (#757575)
  - 提示文本：浅灰色 (#9E9E9E)

### 卡片样式

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color.White
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 1.dp // 或 3dp，取决于卡片重要性
    )
) {
    // 卡片内容
}
```

### 顶部栏样式

```kotlin
RoundedTopBarScaffold(
    title = "页面标题",
    navController = navController,
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = com.ccjizhang.ui.theme.Primary,
        titleContentColor = Color.White,
        navigationIconContentColor = Color.White,
        actionIconContentColor = Color.White
    ),
    // 不设置windowInsets，让TopAppBar不考虑状态栏高度
    windowInsets = WindowInsets(0, 0, 0, 0)
)
```

### 浮动按钮样式 (仅用于主页和交易列表页面)

```kotlin
FloatingActionButton(
    onClick = { /* 操作 */ },
    containerColor = MaterialTheme.colorScheme.primary
) {
    Icon(
        imageVector = Icons.Default.Add, // 或其他图标
        contentDescription = "操作描述",
        tint = MaterialTheme.colorScheme.onPrimary
    )
}
```

### 列表项样式

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color.White
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 1.dp
    )
) {
    // 列表项内容，简化布局
}
```

## 更新日志

### 2025年5月14日 - 第十一次更新
- 更新页面清单，将已完成的页面标记为已完成：
  - 账户相关页面（账户详情、添加/编辑账户等）
  - 交易相关页面（交易详情、添加/编辑交易）
  - 预算相关页面（预算详情、添加/编辑预算）
  - 统计相关页面（统计详情）
- 更新第三阶段页面类型更新进度：
  - 将"其他列表页面"标记为已完成
  - 将"其他详情页面"标记为已完成
  - 将"其他表单页面"标记为已完成
- 确保文档与实际代码进度保持一致

### 2025年5月14日 - 第十二次更新
- 修正账户管理页面的UI更新状态：
  - 注意：原始的`AccountManagementScreen`尚未更新为使用统一UI组件
  - 但应用实际使用的是已更新UI的`AccountManagementScreenEnhanced`
  - 在导航系统中已配置为使用增强版的账户管理页面
- 修正交易详情页面的UI更新状态：
  - 注意：`ui\screens\transaction\TransactionDetailScreen.kt`尚未更新为使用统一UI组件
  - 但应用实际使用的是已更新UI的`ui\screens\transactions\TransactionDetailScreen.kt`
  - 在导航系统中已配置为使用已更新的版本
- 添加待办事项：
  - 更新原始`AccountManagementScreen`以使用统一UI组件，或者移除它
  - 更新原始`TransactionDetailScreen`以使用统一UI组件，或者移除它
  - 清理代码库中的冗余组件，确保只保留使用统一UI的版本

### 2025年5月14日 - 第十三次更新
- 清理冗余组件：
  - 移除了`ui\screens\transaction\TransactionDetailScreen.kt`（使用旧UI）
  - 移除了`ui\screens\accounts\AccountManagementScreen.kt`（使用旧UI）
  - 更新了`WrappedAccountsScreenUnified`以使用`AccountManagementScreenEnhanced`
  - 将`WrappedAccountManagementScreenUnified`标记为已弃用
- 这些更改确保了代码库中只保留使用统一UI的版本，减少了维护负担和潜在的混淆
- 所有导航现在都指向使用统一UI组件的页面

### 2025年5月13日 - 第十次更新
- 完成用户体验测试：
  - 创建用户体验测试报告
  - 记录用户反馈和改进建议
  - 分析用户体验数据
- 完成性能优化：
  - 创建性能优化报告
  - 分析性能瓶颈
  - 提出优化建议
- 完成全应用UI更新计划的所有阶段

### 2025年5月13日 - 第九次更新
- 更新高级功能页面UI：
  - 家庭成员页面
- 统一家庭成员页面的卡片样式
- 统一家庭成员页面的表单样式
- 完成所有页面的UI更新

### 2025年5月13日 - 第八次更新
- 更新高级功能页面UI：
  - 投资管理页面
  - 财务报告页面
- 统一所有高级功能页面的卡片样式
- 统一所有高级功能页面的表单样式

### 2025年5月13日 - 第七次更新
- 修复编译错误：
  - 添加缺少的Color导入
  - 移除重复的floatingActionButton参数
  - 移除重复的snackbarHost参数
  - 添加缺少的LaunchedEffect和Add图标导入
- 更新文档，记录问题修复

### 2025年5月13日 - 第六次更新
- 更新高级功能页面UI：
  - 储蓄目标页面
  - 定期交易页面
- 统一所有高级功能页面的卡片样式
- 统一所有高级功能页面的表单样式

### 2025年5月13日 - 第五次更新
- 更新设置子页面UI：
  - 主题设置页面
  - 货币设置页面
  - 通知设置页面
- 统一所有设置子页面的卡片样式
- 统一所有设置子页面的表单样式

### 2025年5月13日 - 第四次更新
- 更新设置相关页面UI：
  - 分类管理页面
  - 标签管理页面
  - 数据备份页面
- 统一所有设置页面的卡片样式
- 统一所有设置页面的表单样式

### 2025年5月13日 - 第三次更新
- 更新详情页面UI：
  - 账户详情页面
  - 交易详情页面
  - 预算详情页面
- 更新表单页面UI：
  - 添加/编辑账户页面
  - 添加/编辑交易页面
  - 添加/编辑预算页面
- 统一所有页面的卡片样式
- 统一所有页面的表单样式

### 2025年5月13日 - 第二次更新
- 创建统一UI组件库 `UnifiedUIComponents.kt`
- 更新主要页面UI：
  - 交易列表页面
  - 账户管理页面
  - 预算管理页面
  - 统计分析页面
  - 设置页面
- 统一所有页面的顶部栏样式
- 统一所有页面的卡片样式
- 统一浮动按钮样式（仅用于主页和交易列表页面）

### 2025年5月13日 - 第一次更新
- 创建全应用UI更新计划文档
- 完成页面清单整理
- 制定初步设计规范
