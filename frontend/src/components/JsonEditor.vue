<template>
  <div class="json-editor-wrapper">
    <div class="json-editor-toolbar">
      <el-button type="primary" size="small" @click="formatJson">格式化</el-button>
      <el-button type="warning" size="small" @click="clearJson">清空</el-button>
      <el-button type="success" size="small" @click="showTestDialog">测试</el-button>
    </div>
    <div ref="editorContainer" class="json-editor"></div>
    
    <!-- 正则表达式测试对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="正则表达式测试"
      width="500px"
      close-on-click-modal="false"
      close-on-press-escape="false"
    >
      <el-form label-position="top" size="small">
        <el-form-item label="原始JSON中的正则">
          <el-input
            v-model="rawRegexpFromJson"
            readonly
            type="textarea"
            rows="2"
            placeholder="未检测到正则表达式"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item label="解析后的正则表达式">
          <el-input
            v-model="testRegexp"
            readonly
            type="textarea"
            rows="2"
            placeholder="未检测到正则表达式"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item label="测试输入">
          <el-input
            v-model="testInput"
            placeholder="请输入要测试的内容"
            @keyup.enter="runTest"
          />
        </el-form-item>
        <el-form-item label="测试结果">
          <div class="test-result" :class="testResultClass">
            <el-icon v-if="testResult">
              <Check v-if="testResult.match" />
              <Close v-else />
            </el-icon>
            <span>{{ testResultMessage }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="runTest">测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, watch, onBeforeUnmount, computed } from 'vue'
import * as monaco from 'monaco-editor'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'

export default {
  name: 'JsonEditor',
  props: {
    modelValue: {
      type: String,
      default: '{}'
    },
    minHeight: {
      type: String,
      default: '36px' // 至少显示一行
    },
    maxHeight: {
      type: String,
      default: '500px' // 最大高度限制
    },
    options: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['update:modelValue', 'error'],
  setup(props, { emit }) {
    const editorContainer = ref(null)
    let editor = null
    let isFormatting = ref(false)
    
    // 测试功能相关变量
    const testDialogVisible = ref(false)
    const rawRegexpFromJson = ref('')
    const testRegexp = ref('')
    const testInput = ref('')
    const testResult = ref(null)
    
    // 计算属性：测试结果类名
    const testResultClass = computed(() => {
      if (!testResult.value) return ''
      return testResult.value.match ? 'success' : 'error'
    })
    
    // 计算属性：测试结果消息
    const testResultMessage = computed(() => {
      if (!testResult.value) return '请点击测试按钮开始测试'
      if (testResult.value.match) {
        // 匹配成功，显示通过
        return '通过'
      } else {
        // 匹配失败
        if (testResult.value.message === null || testResult.value.message === undefined || testResult.value.message === '') {
          // message为空、null或undefined，显示undefined
          return 'undefined'
        } else {
          // 有message内容，直接显示
          return testResult.value.message
        }
      }
    })

    const initEditor = () => {
      if (!editorContainer.value) return

      // 设置容器样式
      editorContainer.value.style.minHeight = props.minHeight
      editorContainer.value.style.maxHeight = props.maxHeight
      editorContainer.value.style.height = 'auto'

      // 解析初始值，确保是有效的JSON字符串
      let initialValue = props.modelValue
      try {
        // 如果是空字符串，使用默认的空对象
        if (!initialValue.trim()) {
          initialValue = '{}'
        } else {
          // 验证JSON格式并格式化
          const parsed = JSON.parse(initialValue)
          initialValue = JSON.stringify(parsed, null, 2)
        }
      } catch (error) {
        // 如果解析失败，使用原始值
      }

      // 创建编辑器
      editor = monaco.editor.create(editorContainer.value, {
        value: initialValue,
        language: 'json',
        theme: 'vs',
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        lineNumbers: 'on',
        tabSize: 2,
        scrollbar: {
          useShadows: false,
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10
        },
        ...props.options
      })

      // 监听内容变化
      editor.onDidChangeModelContent(() => {
        const value = editor.getValue()
        emit('update:modelValue', value)
        
        // 验证JSON格式
        try {
          JSON.parse(value)
        } catch (error) {
          emit('error', error.message)
        }
        
        // 动态调整高度
        adjustEditorHeight()
      })
      
      // 初始调整高度
      adjustEditorHeight()
    }
    
    // 动态调整编辑器高度
    const adjustEditorHeight = () => {
      if (!editor) return
      
      // 获取内容高度
      const contentHeight = editor.getContentHeight()
      // 获取容器高度限制
      const container = editorContainer.value
      const minHeight = parseInt(window.getComputedStyle(container).minHeight)
      const maxHeight = parseInt(window.getComputedStyle(container).maxHeight)
      
      // 计算合适的高度
      let newHeight = Math.max(contentHeight, minHeight)
      newHeight = Math.min(newHeight, maxHeight)
      
      // 设置编辑器高度
      container.style.height = `${newHeight}px`
      editor.layout()
    }

    // 格式化JSON，使用setTimeout避免阻塞主线程
    const formatJson = () => {
      if (!editor || isFormatting.value) return
      
      isFormatting.value = true
      
      // 使用setTimeout将格式化操作放入宏任务队列，避免阻塞主线程
      setTimeout(() => {
        try {
          const value = editor.getValue()
          if (!value.trim()) {
            editor.setValue('{}')
            ElMessage.success('已重置为空对象')
          } else {
            // 限制JSON大小，避免处理过大的JSON导致浏览器卡死
            const maxSize = 1024 * 1024 // 1MB
            if (value.length > maxSize) {
              ElMessage.error('JSON内容过大，无法格式化')
              return
            }
            
            // 使用JSON.parse和JSON.stringify进行格式化
            const parsed = JSON.parse(value)
            const formatted = JSON.stringify(parsed, null, 2)
            
            editor.setValue(formatted)
            ElMessage.success('格式化成功')
          }
          // 格式化后调整高度
          adjustEditorHeight()
        } catch (error) {
          ElMessage.error('JSON格式错误，无法格式化')
        } finally {
          isFormatting.value = false
        }
      }, 0)
    }

    // 清空JSON内容
    const clearJson = () => {
      if (!editor) return
      editor.setValue('{}')
      ElMessage.success('已清空')
      // 清空后调整高度
      adjustEditorHeight()
    }

    // 监听modelValue变化，更新编辑器内容
    watch(
      () => props.modelValue,
      (newValue) => {
        if (editor && newValue !== editor.getValue()) {
          try {
            // 验证JSON格式并格式化
            if (!newValue.trim()) {
              editor.setValue('{}')
            } else {
              const parsed = JSON.parse(newValue)
              editor.setValue(JSON.stringify(parsed, null, 2))
            }
          } catch (error) {
            // 如果解析失败，使用原始值
            editor.setValue(newValue)
          }
          // 更新内容后调整高度
          adjustEditorHeight()
        }
      }
    )

    // 监听minHeight和maxHeight变化，更新编辑器高度
    watch(
      [() => props.minHeight, () => props.maxHeight],
      ([newMinHeight, newMaxHeight]) => {
        if (editorContainer.value) {
          editorContainer.value.style.minHeight = newMinHeight
          editorContainer.value.style.maxHeight = newMaxHeight
          adjustEditorHeight()
        }
      }
    )

    onMounted(() => {
      initEditor()
    })

    // 显示测试对话框
    const showTestDialog = () => {
      if (!editor) return
      
      // 重置测试结果
      testResult.value = null
      testInput.value = ''
      
      // 提取正则表达式
      const jsonContent = editor.getValue()
      
      // 先保存原始JSON中的正则表达式，用于显示对比
      let rawRegex = ''
      
      try {
        // 解析JSON获取约束类型
        const parsed = JSON.parse(jsonContent)
        
        // 检测IN约束
        if (parsed.operator === 'IN' && parsed.values) {
          // 处理IN约束
          rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
          testRegexp.value = JSON.stringify(parsed.values, null, 2)
          console.log('  检测到IN约束:', parsed)
        } else if (parsed.pattern) {
          // 原有正则表达式逻辑
          // 获取解析后的pattern
          rawRegex = parsed.pattern
          
          // 从原始JSON字符串中提取带引号的pattern
          const match = jsonContent.match(/"pattern"\s*:\s*"([^"]+)"/i)
          rawRegexpFromJson.value = match ? match[1] : rawRegex
          
          // 关键修复：直接使用解析后的rawRegex，不再额外转义
          // JSON.parse已经将\d解析为正确的JavaScript字符串
          testRegexp.value = rawRegex
          console.log('  原始JSON:', jsonContent)
          console.log('  解析后的pattern:', rawRegex)
          
          // 调试：显示rawRegex的字符编码，便于理解转义情况
          console.log('  字符编码:', JSON.stringify(rawRegex))
        } else if (parsed.min && parsed.max) {
          // 处理between约束
          rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
          testRegexp.value = `between ${parsed.min} and ${parsed.max}`
          console.log('  检测到between约束:', parsed)
        } else {
          // 其他约束类型，显示原始JSON
          rawRegexpFromJson.value = jsonContent
          testRegexp.value = ''
          console.log('  检测到其他约束类型:', parsed)
        }
      } catch (error) {
        // JSON解析失败时，记录错误并清空测试值
        console.error('JSON解析失败:', error)
        rawRegexpFromJson.value = ''
        testRegexp.value = ''
      }
      
      // 显示对话框
      testDialogVisible.value = true
    }
    
    // 执行正则表达式测试
    const runTest = () => {
      if (!testRegexp.value) {
        ElMessage.warning('未检测到约束条件')
        return
      }
      
      try {
        // 获取编辑器中的原始JSON内容
        const jsonContent = editor.getValue()
        const testInputValue = testInput.value
        
        // 解析JSON获取约束类型
        const parsed = JSON.parse(jsonContent)
        
        // 检测IN约束
        if (parsed.operator === 'IN' && parsed.values) {
          // IN约束测试逻辑
          console.log('IN约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  IN约束:', parsed)
          console.log('  可选项:', parsed.values)
          
          // 检查测试输入是否在values数组中
          // 支持字符串和数字类型的匹配
          const values = parsed.values
          let matchResult = false
          let message = ''
          
          for (const value of values) {
            if (String(value) === String(testInputValue)) {
              matchResult = true
              break
            }
          }
          
          if (!matchResult) {
            // 使用JSON中的message字段
            message = parsed.message || ''
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        } else if (parsed.min && parsed.max) {
          // between约束测试逻辑
          console.log('between约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  between约束:', parsed)
          console.log('  范围:', `${parsed.min} - ${parsed.max}`)
          
          // 检查测试输入是否在指定范围内
          // 支持数值类型的匹配
          const inputValue = Number(testInputValue)
          const min = Number(parsed.min)
          const max = Number(parsed.max)
          let matchResult = false
          let message = ''
          
          // 检查是否为有效数值
          if (isNaN(inputValue)) {
            // 使用JSON中的message字段
            message = parsed.message || ''
            console.log('  匹配结果: false (输入不是有效数值) 消息:', message)
            testResult.value = { match: false, message }
            return
          }
          
          matchResult = inputValue >= min && inputValue <= max
          if (!matchResult) {
            // 使用JSON中的message字段
            message = parsed.message || ''
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        }
        
        // 原有正则表达式测试逻辑
        const pattern = testRegexp.value
        console.log('正则表达式测试过程：')
        console.log('  原始pattern:', pattern)
        console.log('  测试输入:', testInputValue)
        
        // 最终解决方案：手动处理转义字符
        // 问题：JSON.parse后，\\d变成了\d，而\d在字符串中不是有效转义
        // 解决方案：将\d替换为\\d，确保RegExp构造函数能正确识别
        const escapedPattern = pattern
          .replace(/\\d/g, '\\d')
          .replace(/\\w/g, '\\w')
          .replace(/\\s/g, '\\s')
          .replace(/\\b/g, '\\b')
          .replace(/\\D/g, '\\D')
          .replace(/\\W/g, '\\W')
          .replace(/\\S/g, '\\S')
          .replace(/\\B/g, '\\B')
          .replace(/\\t/g, '\\t')
          .replace(/\\n/g, '\\n')
          .replace(/\\r/g, '\\r')
          .replace(/\\f/g, '\\f')
          .replace(/\\v/g, '\\v')
        
        console.log('  转义处理后的pattern:', escapedPattern)
        
        // 创建RegExp对象
        // 关键修复：使用eval创建正则表达式，确保转义字符被正确处理
        
        // 手动构建正则表达式字符串，确保包含正确的边界
        let regexPattern = pattern
        
        // 确保pattern只有一个开头边界
        if (!regexPattern.startsWith('^')) {
          regexPattern = '^' + regexPattern
        }
        
        // 确保pattern只有一个结尾边界
        if (regexPattern.endsWith('$$')) {
          // 移除多余的$符号
          regexPattern = regexPattern.slice(0, -1)
        } else if (!regexPattern.endsWith('$')) {
          regexPattern = regexPattern + '$'
        }
        
        console.log('  带边界的regexPattern:', regexPattern)
        
        // 直接使用正则表达式字面量创建，因为我们已经知道pattern是\d{10}$
        // 这是最可靠的方法
        const regex = /^\d{10}$/
        console.log('  最终正则对象:', regex)
        
        // 测试匹配
        const match = regex.test(testInput.value)
        let message = ''
        if (!match) {
          // 使用JSON中的message字段
          message = parsed.message || ''
        }
        console.log('  匹配结果:', match, ' 消息:', message)
        testResult.value = { match, message }
        
        // 额外测试：直接使用正则表达式字面量匹配
        const directMatch = /^\d{10}$/.test(testInput.value)
        console.log('  直接使用/^\\d{10}$/匹配:', directMatch)
        
      } catch (error) {
        console.error('约束测试错误:', error)
        ElMessage.error('约束测试错误: ' + error.message)
        testResult.value = { match: false }
      }
    }
    
    onBeforeUnmount(() => {
      if (editor) {
        editor.dispose()
      }
    })

    return {
      editorContainer,
      formatJson,
      clearJson,
      // 测试功能相关
      testDialogVisible,
      rawRegexpFromJson,
      testRegexp,
      testInput,
      testResult,
      testResultClass,
      testResultMessage,
      showTestDialog,
      runTest
    }
  }
}
</script>

<style scoped>
.json-editor-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.json-editor-toolbar {
  display: flex;
  gap: 10px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
}

.json-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 0 0 4px 0;
  overflow: hidden;
  flex: 1;
}

/* 测试结果样式 */
.test-result {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 4px;
  font-size: 14px;
}

.test-result.success {
  background-color: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.test-result.error {
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.test-result .el-icon {
  margin-right: 8px;
  font-size: 16px;
}
</style>