# 改进JSON输入体验实现文档

## 问题分析
前端代码中有两个主要的JSON输入区域：
1. `FieldManage.vue` - 校验规则输入框（第94行）
2. `RuleManage.vue` - 规则内容输入框（第72行）

这两个区域原本使用普通的`<textarea>`元素，仅提供了JSON格式的提示，缺乏语法高亮、格式化、实时校验等增强功能，用户体验较差。

## 解决方案
基于项目已有的`monaco-editor`和`@monaco-editor/loader`创建了自定义的`JsonEditor.vue`组件，为JSON输入提供更好的用户体验，包括：
- 语法高亮
- 实时JSON格式校验
- 一键格式化功能
- 一键清空功能
- 动态高度调整
- 行号显示
- 结构缩进

## 技术选型
- **monaco-editor 0.55.1**：功能强大的代码编辑器，提供良好的JSON编辑体验
- **@monaco-editor/loader 1.7.0**：用于加载monaco-editor
- **Vue 3 Composition API**：使用现代Vue 3语法开发组件

## 实施步骤

### 1. 创建自定义JSON编辑器组件
- 创建了`src/components/JsonEditor.vue`组件
- 基于monaco-editor实现JSON编辑功能
- 支持动态高度调整
- 添加了格式化和清空功能
- 实现了实时JSON验证

### 2. 改造FieldManage.vue
- 在第94行替换普通textarea为自定义JSON编辑器组件
- 配置了`min-height="60px"`和`max-height="200px"`
- 实现了校验规则为空对象时转换为null的逻辑
- 确保与现有表单验证集成

### 3. 改造RuleManage.vue
- 在第72行替换普通textarea为自定义JSON编辑器组件
- 配置了`min-height="100px"`和`max-height="400px"`
- 移除了现有的手动JSON.parse验证（第222-227行），使用组件内置验证
- 实现了规则内容为空对象时转换为null的逻辑
- 保持与现有预览功能的兼容性

### 4. 性能优化
- 实现了动态高度调整，避免编辑器过大影响性能
- 添加了JSON大小限制（1MB），避免处理过大的JSON导致浏览器卡死
- 使用`setTimeout`将格式化操作放入宏任务队列，避免阻塞主线程
- 添加了`isFormatting`标志，防止重复点击导致的多次格式化

## 实现效果

### 核心功能
- **语法高亮**：JSON结构颜色区分，提高可读性
- **实时校验**：输入时自动检测JSON格式错误
- **一键格式化**：快速格式化JSON内容，保持代码整洁
- **一键清空**：快速将JSON内容重置为空对象
- **动态高度**：根据内容自动调整高度，至少显示一行
- **行号显示**：方便查看和定位

### 数据处理
- 当JSON内容为`{}`或格式化后的`{\n}`时，自动转换为`null`存储到数据库
- 确保数据库中不存储空对象，节省存储空间

### 性能优化
- 合理限制编辑器大小，避免影响页面性能
- 优化格式化逻辑，避免浏览器卡死
- 实现了编辑器的高效初始化和更新

## 测试要点
- ✅ 验证JSON编辑器在不同屏幕尺寸下的显示效果
- ✅ 测试JSON格式错误的提示是否准确
- ✅ 确保一键格式化功能正常工作
- ✅ 验证一键清空功能正常工作
- ✅ 验证动态高度调整功能正常工作
- ✅ 验证表单提交时的JSON验证逻辑
- ✅ 检查页面加载和响应性能
- ✅ 验证空对象转换为null的逻辑

## 代码结构

### JsonEditor.vue 核心代码
```vue
<template>
  <div class="json-editor-wrapper">
    <div class="json-editor-toolbar">
      <el-button type="primary" size="small" @click="formatJson">格式化</el-button>
      <el-button type="warning" size="small" @click="clearJson">清空</el-button>
    </div>
    <div ref="editorContainer" class="json-editor"></div>
  </div>
</template>

<script>
// 基于monaco-editor的自定义JSON编辑器实现
// 包含动态高度调整、格式化、清空等功能
</script>
```

### 调用示例
```vue
<json-editor
  v-model="form.validateRule"
  min-height="60px"
  max-height="200px"
  :options="{
    maxLines: 10,
    minLines: 1
  }"
/>
```

## 总结
通过创建基于monaco-editor的自定义JSON编辑器组件，我们成功改进了JSON输入体验，提供了语法高亮、实时校验、一键格式化、动态高度调整等功能。同时，我们实现了空对象转换为null的逻辑，确保数据库中不存储无用的空对象。性能优化措施确保了编辑器在各种情况下都能流畅运行，为用户提供了良好的JSON编辑体验。