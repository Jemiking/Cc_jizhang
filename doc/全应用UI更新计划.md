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
- [ ] 账户详情页面
- [ ] 添加/编辑账户页面
- [ ] 账户分类管理页面
- [ ] 账户转账页面
- [ ] 账户余额调整页面

**交易相关页面**：
- [ ] 交易详情页面
- [ ] 添加/编辑交易页面

**预算相关页面**：
- [ ] 预算详情页面
- [ ] 添加/编辑预算页面

**统计相关页面**：
- [ ] 统计详情页面

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
- [ ] 家庭成员页面

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
- [ ] 其他列表页面

#### 详情页面更新
- [x] 账户详情页面
- [x] 交易详情页面
- [x] 预算详情页面
- [ ] 其他详情页面

#### 表单页面更新
- [x] 添加/编辑账户页面
- [x] 添加/编辑交易页面
- [x] 添加/编辑预算页面
- [ ] 其他表单页面

#### 统计/分析页面更新
- [x] 统计概览页面
- [x] 统计详情页面

### 第四阶段：测试和优化 [进行中]

- [x] UI一致性测试
- [ ] 用户体验测试
- [ ] 性能优化

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
