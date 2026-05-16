<template>
  <div class="json-editor-wrapper">
    <div class="json-editor-toolbar">
      <el-button type="primary" size="small" @click="formatJson">格式化</el-button>
      <el-button type="warning" size="small" @click="clearJson">清空</el-button>
      <el-button v-if="showTestAndExample" type="success" size="small" @click="showTestDialog">测试</el-button>
      <el-button v-if="showTestAndExample" type="info" size="small" @click="showExampleDialog">查看示例</el-button>
    </div>
    <div ref="editorContainer" class="json-editor"></div>
    
    <!-- 正则表达式测试对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="正则表达式测试"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form label-position="top" size="small">
        <el-form-item label="原始JSON中的正则">
          <el-input
            v-model="rawRegexpFromJson"
            readonly
            type="textarea"
            rows="4"
            placeholder="未检测到正则表达式"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item label="解析后的正则表达式">
          <el-input
            v-model="testRegexp"
            readonly
            type="textarea"
            rows="4"
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
    
    <!-- 示例模板对话框 -->
    <el-dialog
      v-model="exampleDialogVisible"
      title="校验规则示例"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="example-dialog-content">
        <el-select
          v-model="selectedExample"
          placeholder="请选择示例模板"
          style="width: 100%; margin-bottom: 15px;"
          @change="loadExampleTemplate"
        >
          <el-option
            v-for="example in exampleTemplates"
            :key="example.key"
            :label="example.title"
            :value="example.key"
          >
            <div class="example-option">
              <div class="example-title">{{ example.title }}</div>
              <div class="example-desc">{{ example.description }}</div>
            </div>
          </el-option>
        </el-select>
        
        <el-form label-position="top" size="small">
          <el-form-item label="示例说明">
            <el-input
              v-model="currentExample.description"
              readonly
              type="textarea"
              rows="2"
              style="resize: none;"
            />
          </el-form-item>
          <el-form-item label="示例代码">
            <el-input
              v-model="currentExampleCode"
              readonly
              type="textarea"
              rows="6"
              style="font-family: monospace; resize: none;"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="exampleDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="applyExampleTemplate">应用到编辑器</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, watch, onBeforeUnmount, computed } from 'vue'
import * as monaco from 'monaco-editor'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { normalizeRegexPattern } from '../utils/regexUtils'

export default {
  name: 'JsonEditor',
  props: {
    modelValue: {
      type: String,
      default: '{}'
    },
    minHeight: {
      type: String,
      default: '100px' // 至少显示多行
    },
    maxHeight: {
      type: String,
      default: '500px' // 最大高度限制
    },
    options: {
      type: Object,
      default: () => ({})
    },
    showTestAndExample: {
      type: Boolean,
      default: true // 是否显示测试和查看示例按钮
    },
    ruleType: {
      type: String,
      default: '' // 规则类型，用于区分字段管理和业务规则
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
    
    // 示例功能相关变量
    const exampleDialogVisible = ref(false)
    const selectedExample = ref('')
    const currentExample = ref({ title: '', description: '', code: '{}' })
    const currentExampleCode = ref('{}')
    
    // 内置正则表达式映射表，根据type值提供相应的正则表达式
    const builtInRegexMap = {
      email: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
      url: '^(https?:\\/\\/)?(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3})(?:[/\\w .-]*)*\\/?$',
      number: '^-?\\d+(\\.\\d+)?$',
      integer: '^-?\\d+$'
    }
    
    // 字段管理示例模板列表
    const fieldExampleTemplates = ref([
      {
        key: 'required',
        title: '必填字段',
        description: '验证字段是否为必填项',
        code: '{"required": true, "message": "该字段为必填项", "trigger": "blur"}'
      },
      {
        key: 'min_length',
        title: '最小长度限制',
        description: '验证字符串最小长度',
        code: '{"min": 5, "message": "长度不能小于5个字符", "trigger": "blur"}'
      },
      {
        key: 'max_length',
        title: '最大长度限制',
        description: '验证字符串最大长度',
        code: '{"max": 20, "message": "长度不能超过20个字符", "trigger": "blur"}'
      },
      {
        key: 'length_range',
        title: '长度范围限制',
        description: '验证字符串长度范围',
        code: '{"min": 5, "max": 20, "message": "长度必须在5到20个字符之间", "trigger": "blur"}'
      },
      {
        key: 'number_range',
        title: '数值范围限制',
        description: '验证数值在指定范围内',
        code: '{"type": "number", "min": 0, "max": 100, "message": "数值必须在0到100之间", "trigger": "blur"}'
      },
      {
        key: 'positive_number',
        title: '正数验证',
        description: '验证数值是否为正数',
        code: '{"type": "number", "min": 0.1, "message": "必须输入正数", "trigger": "blur"}'
      },
      {
        key: 'integer',
        title: '整数验证',
        description: '验证数值是否为整数',
        code: '{"type": "number", "integer": true, "message": "必须输入整数", "trigger": "blur"}'
      },
      {
        key: 'email',
        title: '邮箱格式验证',
        description: '验证邮箱格式是否正确',
        code: '{"type": "email", "message": "请输入正确的邮箱地址", "trigger": "blur"}'
      },
      {
        key: 'phone',
        title: '手机号格式验证',
        description: '验证手机号格式是否正确',
        code: '{"pattern": "^1[3-9]\\\\d{9}$", "message": "请输入正确的手机号", "trigger": "blur"}'
      },
      {
        key: 'url',
        title: 'URL格式验证',
        description: '验证URL格式是否正确',
        code: '{"type": "url", "message": "请输入正确的URL地址", "trigger": "blur"}'
      },
      {
        key: 'in_array',
        title: '枚举值验证',
        description: '验证值是否在指定的枚举列表中',
        code: '{"operator": "IN", "values": ["value1", "value2", "value3"], "message": "请选择有效值", "trigger": "blur"}'
      },
      {
        key: 'regexp',
        title: '正则表达式验证',
        description: '使用正则表达式验证输入格式',
        code: '{"pattern": "^\\\\w{4,20}$", "message": "只能包含字母数字下划线，长度4-20位", "trigger": "blur"}'
      },
      {
        key: 'password_strength',
        title: '密码强度验证',
        description: '验证密码强度，要求包含大小写字母和数字',
        code: '{"pattern": "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)[a-zA-Z\\\\d@$!%*?&]{8,}$", "message": "密码至少8位，包含大小写字母和数字", "trigger": "blur"}'
      }
    ])
    
    // 业务规则示例模板列表
    const businessRuleExampleTemplates = ref([
      {
        key: 'unique',
        title: '单字段唯一性验证',
        description: '验证单个字段的唯一性',
        code: '{"field": "student_code", "type": "unique", "message": "学号已存在"}'
      },
      {
        key: 'unique_combo',
        title: '组合字段唯一性验证',
        description: '验证多个字段组合的唯一性',
        code: '{"fields": ["student_id", "course_id"], "type": "unique_combo", "message": "该学生的该课程成绩已存在"}'
      },
      {
        key: 'business_logic',
        title: '业务逻辑验证',
        description: '根据业务逻辑验证数据',
        code: '{"type": "business_logic", "condition": "score > 100", "message": "分数不能超过100分"}'
      },
      {
        key: 'data_range',
        title: '数据范围验证',
        description: '验证数据在指定范围内',
        code: '{"field": "age", "type": "range", "min": 6, "max": 30, "message": "年龄必须在6-30岁之间"}'
      },
      {
        key: 'conditional',
        title: '条件性规则验证',
        description: '根据条件应用不同的验证规则',
        code: '{"type": "conditional", "condition": "status == \"active\"", "rules": [{"required": true, "field": "active_date", "message": "激活状态必须填写激活日期"}]}'
      }
    ])
    
    // 根据ruleType动态计算当前使用的示例模板
    const exampleTemplates = computed(() => {
      // 如果ruleType包含"RULE"，则使用业务规则示例
      if (props.ruleType && props.ruleType.includes('RULE')) {
        return businessRuleExampleTemplates.value
      }
      // 否则使用字段管理示例
      return fieldExampleTemplates.value
    })
    
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
    
    // JSON验证方法，检查JSON格式是否正确
    const validateJson = (jsonString) => {
      if (!jsonString || !jsonString.trim()) {
        return { valid: true, error: '' }
      }
      try {
        JSON.parse(jsonString)
        return { valid: true, error: '' }
      } catch (error) {
        // 解析错误信息，提取有用部分
        // 安全获取错误信息，处理error为Event对象的情况
        let errorMessage = error instanceof Error ? error.message : String(error)
        // 尝试提取更友好的错误信息
        if (errorMessage.includes('Unexpected token')) {
          // 处理未转义的反斜杠问题
          if (errorMessage.includes('in JSON at position')) {
            const position = parseInt(errorMessage.match(/position (\d+)/)[1])
            errorMessage = `JSON格式错误：在位置${position}处检测到未转义的特殊字符，请确保所有反斜杠已转义（如\\d应写作\\\\d）`
          } else {
            errorMessage = `JSON格式错误：${errorMessage}。请检查是否有未转义的反斜杠或其他特殊字符`
          }
        }
        return { valid: false, error: errorMessage }
      }
    }

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
        contextmenu: true,
        locale: 'zh-CN',
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
        const validation = validateJson(value)
        if (!validation.valid) {
          emit('error', validation.error)
          console.error('JSON格式错误:', validation.error)
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

    // 自动转义正则表达式中的反斜杠
    const escapeRegexp = (regexpString) => {
      if (typeof regexpString !== 'string') return regexpString
      
      // 转义正则表达式中的反斜杠
      return regexpString.replace(/\\/g, '\\\\')
    }
    
    // 递归处理对象，转义所有正则表达式
    const escapeRegexpInObject = (obj) => {
      if (obj === null || typeof obj !== 'object') {
        return escapeRegexp(obj)
      }
      
      if (Array.isArray(obj)) {
        return obj.map(item => escapeRegexpInObject(item))
      }
      
      const result = {}
      for (const key in obj) {
        if (obj.hasOwnProperty(key)) {
          result[key] = escapeRegexpInObject(obj[key])
        }
      }
      return result
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
            
            // 尝试直接解析JSON
            let parsed
            let originalValue = value
            
            try {
              parsed = JSON.parse(value)
              
              // JSON解析成功，JSON.stringify会自动处理转义
              // 确保反斜杠在JSON字符串中被正确转义为\\
              // 不需要额外处理，JSON.stringify已经能够正确处理
            } catch (error) {
              // JSON解析失败，尝试自动修复
              ElMessage.warning('JSON解析失败，尝试自动修复...')
              console.log('JSON解析失败，尝试自动修复:', error)
              
              // 保存原始值用于后续错误信息
              originalValue = value
              
              // 1. 自动修复：处理pattern和其他可能包含正则表达式的字段
              let processedValue = value
              
              // 改进的正则表达式自动转义逻辑，处理更多可能的字段
              // 使用更精确的正则表达式来匹配JSON字符串值，支持转义字符
              // 匹配 pattern: "..." 格式，其中 ... 可能包含转义的引号和反斜杠
              // 注意：这个正则表达式需要能够匹配包含转义字符的JSON字符串值
              // 使用非贪婪匹配和转义字符处理
              processedValue = processedValue.replace(/"(pattern|regex|validate|format)"\s*:\s*"((?:[^"\\]|\\.)*)"/g, (match, field, pattern) => {
                // pattern字符串中，反斜杠需要被转义
                // 在JSON字符串中，单个反斜杠需要写成 \\
                // 所以我们需要将 pattern 中的每个反斜杠转义为双反斜杠
                // 但要注意：如果已经是转义序列（如 \\d），需要保持原样
                // 实际上，在JSON字符串中，\d 应该写成 \\d
                const escapedPattern = pattern.replace(/\\/g, '\\\\')
                return `"${field}": "${escapedPattern}"`
              })
              
              // 2. 尝试重新解析处理后的JSON
              try {
                parsed = JSON.parse(processedValue)
                ElMessage.success('成功自动修复JSON格式')
              } catch (secondError) {
                // 3. 尝试更全面的修复
                // 处理可能的JSON语法错误，如缺少引号、冒号等
                let comprehensiveFix = processedValue
                
                // 示例：修复常见的语法错误（根据实际情况调整）
                // 注意：这些修复可能不适用所有情况，仅作为辅助手段
                
                // 尝试修复：添加缺失的引号（简单情况）
                comprehensiveFix = comprehensiveFix.replace(/(\w+)\s*:/g, '"$1":')
                
                // 尝试修复：添加缺失的闭合引号
                comprehensiveFix = comprehensiveFix.replace(/:"([^"\\\n]+)$/gm, ':"$1"')
                
                // 尝试重新解析
                try {
                  parsed = JSON.parse(comprehensiveFix)
                  ElMessage.success('成功自动修复JSON格式')
                } catch (thirdError) {
                  // 所有修复尝试失败，抛出原始错误
                  throw error
                }
              }
            }
            
            // 使用JSON.parse和JSON.stringify进行格式化
            // JSON.stringify会自动处理转义，确保反斜杠被正确转义为\\
            const formatted = JSON.stringify(parsed, null, 2)
            
            editor.setValue(formatted)
            ElMessage.success('格式化成功')
          }
          // 格式化后调整高度
          adjustEditorHeight()
        } catch (error) {
          // 提供更详细的错误信息，安全处理error为Event对象的情况
          const baseErrorMessage = error instanceof Error ? error.message : String(error)
          let errorMessage = `JSON格式错误，无法格式化：${baseErrorMessage}`
          
          // 提取错误位置信息，安全处理error为Event对象的情况
          if (error instanceof Error && error.message.includes('position')) {
            const positionMatch = error.message.match(/position (\d+)/)
            if (positionMatch) {
              const position = parseInt(positionMatch[1])
              errorMessage += `\n错误位置：${position}`
              errorMessage += `\n建议：请检查该位置附近的语法，确保所有引号、冒号、逗号等符号都正确使用`
            }
          }
          
          ElMessage.error(errorMessage)
          console.error('JSON格式化错误:', error)
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
        // 验证JSON格式
        const validation = validateJson(jsonContent)
        if (!validation.valid) {
          // JSON格式错误，显示错误信息
          ElMessage.warning(`JSON格式错误：${validation.error}`)
          console.error('JSON格式错误:', validation.error)
          // 显示对话框，让用户看到错误
          testDialogVisible.value = true
          return
        }
        
        // 解析JSON获取约束类型
        let parsed = null
        try {
            parsed = JSON.parse(jsonContent)
        } catch (error) {
            console.error('JSON解析失败:', error)
            rawRegexpFromJson.value = jsonContent
            testRegexp.value = ''
            return
        }
        
        // 检测IN约束
        if (parsed && parsed.operator === 'IN' && parsed.values) {
            // 处理IN约束
            rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
            testRegexp.value = JSON.stringify(parsed.values, null, 2)
            console.log('  检测到IN约束:', parsed)
        } else if (parsed && parsed.required) {
            // 处理必填字段约束
            rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
            testRegexp.value = 'required: true'
            console.log('  检测到必填字段约束:', parsed)
        } else if (parsed && parsed.min !== undefined && parsed.max !== undefined) {
            // 处理between约束 - 增强检测，只要存在min和max属性就识别为between约束
            rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
            testRegexp.value = `between ${parsed.min} and ${parsed.max}`
            console.log('  检测到between约束:', parsed)
        } else if (parsed && parsed.min !== undefined) {
            // 处理只有min属性的约束
            rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
            testRegexp.value = `min: ${parsed.min}`
            console.log('  检测到min约束:', parsed)
        } else if (parsed && parsed.max !== undefined) {
            // 处理只有max属性的约束
            rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
            testRegexp.value = `max: ${parsed.max}`
            console.log('  检测到max约束:', parsed)
        } else if (parsed && parsed.type) {
            // 处理带有type属性的约束
            rawRegexpFromJson.value = JSON.stringify(parsed, null, 2)
            
            // 检查是否有对应的内置正则表达式
            if (builtInRegexMap[parsed.type]) {
                testRegexp.value = builtInRegexMap[parsed.type]
                console.log('  检测到type约束:', parsed)
                console.log('  使用内置正则表达式:', testRegexp.value)
            } else {
                // 没有对应内置正则的type，显示原始JSON
                testRegexp.value = ''
                console.log('  检测到type约束，但没有对应内置正则:', parsed)
            }
        } else if (parsed && parsed.pattern) {
            // 处理pattern约束
            // 获取解析后的pattern
            rawRegex = parsed.pattern
            
            // 使用公共工具函数归一化并修复正则表达式
            let normalizedRegex;
            try {
                normalizedRegex = normalizeRegexPattern(rawRegex, { fixCorrupted: true });
            } catch (error) {
                console.error('  正则表达式处理失败:', error);
                normalizedRegex = rawRegex;
            }
            
            // 直接使用JSON解析后的pattern值（用于显示）
            rawRegexpFromJson.value = rawRegex
            
            // 检测是否为URL相关的pattern，如果是，使用builtInRegexMap.url
            if (rawRegex.includes('https?') || rawRegex.includes('http?')) {
                console.log('  检测到URL pattern，使用builtInRegexMap.url')
                testRegexp.value = builtInRegexMap.url
            } else {
                testRegexp.value = normalizedRegex
            }
            
            console.log('  原始JSON:', jsonContent)
            console.log('  解析后的pattern:', rawRegex)
            console.log('  归一化后的正则:', normalizedRegex)
            console.log('  使用的测试正则:', testRegexp.value)
            
            // 调试：显示rawRegex的字符编码，便于理解转义情况
            console.log('  字符编码:', JSON.stringify(rawRegex))
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
        
        // 验证JSON格式
        const validation = validateJson(jsonContent)
        if (!validation.valid) {
          // JSON格式错误，显示错误信息
          ElMessage.error(`JSON格式错误：${validation.error}`)
          console.error('JSON格式错误:', validation.error)
          testResult.value = { match: false, message: `JSON格式错误：${validation.error}` }
          return
        }
        
        // 解析JSON获取约束类型
        let parsed = null
        try {
            parsed = JSON.parse(jsonContent)
        } catch (error) {
            console.error('JSON解析失败:', error)
            testResult.value = { match: false, message: 'JSON格式错误，无法测试' }
            return
        }
        
        // 检测IN约束
        if (parsed && parsed.operator === 'IN' && parsed.values) {
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
        } else if (parsed && parsed.required) {
          // 必填字段约束测试逻辑
          console.log('必填字段约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  必填约束:', parsed)
          
          let matchResult = false
          let message = ''
          
          // 检查测试输入是否为空
          matchResult = testInputValue !== '' && testInputValue !== undefined && testInputValue !== null
          if (!matchResult) {
            // 使用JSON中的message字段
            message = parsed.message || ''
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        } else if (parsed && parsed.min !== undefined && parsed.max !== undefined) {
          // between约束测试逻辑
          console.log('between约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  between约束:', parsed)
          console.log('  范围:', `${parsed.min} - ${parsed.max}`)
          
          let matchResult = false
          let message = ''
          const min = Number(parsed.min)
          const max = Number(parsed.max)
          
          // 判断是否为字符串长度约束（message中包含"长度"）
          const isLengthConstraint = parsed.message && parsed.message.includes('长度')
          console.log('  是否为长度约束:', isLengthConstraint)
          
          if (isLengthConstraint) {
            // 字符串长度约束
            const inputLength = String(testInputValue).length
            console.log('  输入长度:', inputLength)
            
            matchResult = inputLength >= min && inputLength <= max
            if (!matchResult) {
              // 使用JSON中的message字段
              message = parsed.message || ''
            }
          } else {
            // 数值约束
            const inputValue = Number(testInputValue)
            console.log('  数值输入:', inputValue)
            
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
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        } else if (parsed && parsed.min !== undefined) {
          // 只有min属性的约束测试逻辑
          console.log('min约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  min约束:', parsed)
          console.log('  最小值:', parsed.min)
          
          let matchResult = false
          let message = ''
          const min = Number(parsed.min)
          
          // 判断是否为字符串长度约束（message中包含"长度"）
          const isLengthConstraint = parsed.message && parsed.message.includes('长度')
          console.log('  是否为长度约束:', isLengthConstraint)
          
          if (isLengthConstraint) {
            // 字符串长度约束
            const inputLength = String(testInputValue).length
            console.log('  输入长度:', inputLength)
            
            matchResult = inputLength >= min
            if (!matchResult) {
              // 使用JSON中的message字段
              message = parsed.message || ''
            }
          } else {
            // 数值约束
            const inputValue = Number(testInputValue)
            console.log('  数值输入:', inputValue)
            
            // 检查是否为有效数值
            if (isNaN(inputValue)) {
              // 使用JSON中的message字段
              message = parsed.message || ''
              console.log('  匹配结果: false (输入不是有效数值) 消息:', message)
              testResult.value = { match: false, message }
              return
            }
            
            matchResult = inputValue >= min
            if (!matchResult) {
              // 使用JSON中的message字段
              message = parsed.message || ''
            }
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        } else if (parsed && parsed.max !== undefined) {
          // 只有max属性的约束测试逻辑
          console.log('max约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  max约束:', parsed)
          console.log('  最大值:', parsed.max)
          
          let matchResult = false
          let message = ''
          const max = Number(parsed.max)
          
          // 判断是否为字符串长度约束（message中包含"长度"）
          const isLengthConstraint = parsed.message && parsed.message.includes('长度')
          console.log('  是否为长度约束:', isLengthConstraint)
          
          if (isLengthConstraint) {
            // 字符串长度约束
            const inputLength = String(testInputValue).length
            console.log('  输入长度:', inputLength)
            
            matchResult = inputLength <= max
            if (!matchResult) {
              // 使用JSON中的message字段
              message = parsed.message || ''
            }
          } else {
            // 数值约束
            const inputValue = Number(testInputValue)
            console.log('  数值输入:', inputValue)
            
            // 检查是否为有效数值
            if (isNaN(inputValue)) {
              // 使用JSON中的message字段
              message = parsed.message || ''
              console.log('  匹配结果: false (输入不是有效数值) 消息:', message)
              testResult.value = { match: false, message }
              return
            }
            
            matchResult = inputValue <= max
            if (!matchResult) {
              // 使用JSON中的message字段
              message = parsed.message || ''
            }
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        } else if (parsed && parsed.type && builtInRegexMap[parsed.type]) {
          // type属性约束测试逻辑
          console.log('type约束测试过程：')
          console.log('  测试输入:', testInputValue)
          console.log('  type约束:', parsed)
          
          let matchResult = false
          let message = ''
          
          // 先使用内置正则表达式测试type约束
          const pattern = builtInRegexMap[parsed.type]
          console.log('  使用内置正则表达式:', pattern)
          
          // 构建正则表达式对象
          const regex = new RegExp(pattern)
          console.log('  最终正则对象:', regex)
          console.log('  正则表达式源:', regex.source)
          
          // 测试匹配，去除输入前后的空格和不可见字符
          const trimmedInput = testInputValue.trim()
          console.log('  修剪后的测试输入:', trimmedInput)
          console.log('  修剪后的输入长度:', trimmedInput.length)
          matchResult = regex.test(trimmedInput)
          
          // 如果type约束通过，再检查是否有min/max/integer属性
          if (matchResult) {
            if (parsed.min !== undefined || parsed.max !== undefined) {
              // 处理type与min/max属性的组合
              const inputValue = Number(testInputValue)
              const min = Number(parsed.min)
              const max = Number(parsed.max)
              
              console.log('  type约束通过，检查min/max约束:', parsed.min, parsed.max)
              
              // 检查是否为有效数值
              if (!isNaN(inputValue)) {
                // 只有当输入是有效数值时，才检查min/max约束
                if (parsed.min !== undefined && inputValue < min) {
                  matchResult = false
                } else if (parsed.max !== undefined && inputValue > max) {
                  matchResult = false
                }
              }
            } else if (parsed.integer) {
              // 处理type与integer属性的组合
              console.log('  type约束通过，检查integer约束:', parsed.integer)
              
              // 检查是否为整数
              const inputValue = Number(testInputValue)
              if (!isNaN(inputValue)) {
                // 只有当输入是有效数值时，才检查integer约束
                matchResult = Number.isInteger(inputValue)
              }
            }
          }
          
          if (!matchResult) {
            // 使用JSON中的message字段
            message = parsed.message || ''
          }
          
          console.log('  匹配结果:', matchResult, ' 消息:', message)
          testResult.value = { match: matchResult, message }
          return
        } else if (parsed && parsed.pattern) {
          const pattern = parsed.pattern
          console.log('正则表达式测试过程：')
          console.log('  原始pattern:', pattern)
          console.log('  测试输入:', testInputValue)
          
          // 使用公共工具函数归一化并修复正则表达式
          try {
            const normalizedPattern = normalizeRegexPattern(pattern, { fixCorrupted: true });
            const regex = normalizeRegexPattern(normalizedPattern, { createRegExp: true, fixCorrupted: false });
            
            console.log('  最终正则对象:', regex)
            console.log('  正则表达式源:', regex.source)
            const trimmedInput = testInputValue.trim()
            console.log('  修剪后的测试输入:', trimmedInput)
            console.log('  修剪后的输入长度:', trimmedInput.length)
            const match = regex.test(trimmedInput)
            let message = ''
            if (!match) {
              message = parsed.message || ''
            }
            console.log('  匹配结果:', match, ' 消息:', message)
            testResult.value = { match, message }
            return
          } catch (error) {
            console.error('  正则表达式无效:', pattern, error)
            testResult.value = { match: false, message: `正则表达式无效: ${error.message}` }
            return
          }
        } else {
          // 其他约束类型，无法测试
          console.log('  无法测试的约束类型:', parsed)
          testResult.value = { match: false, message: '无法测试的约束类型' }
      }
      
      } catch (error) {
        console.error('约束测试错误:', error)
        // 安全获取错误信息，处理error为Event对象的情况
        const errorMsg = error instanceof Error ? error.message : String(error)
        ElMessage.error('约束测试错误: ' + errorMsg)
        testResult.value = { match: false }
      }
    }
    
    // 显示示例对话框
    const showExampleDialog = () => {
      console.log('showExampleDialog called')
      console.log('before exampleDialogVisible:', exampleDialogVisible.value)
      
      // 重置示例选择
      selectedExample.value = ''
      currentExample.value = { title: '', description: '', code: '{}' }
      currentExampleCode.value = '{}'
      
      // 显示对话框
      exampleDialogVisible.value = true
      console.log('after exampleDialogVisible:', exampleDialogVisible.value)
    }
    
    // 加载选中的示例模板
    const loadExampleTemplate = (key) => {
      const example = exampleTemplates.value.find(item => item.key === key)
      if (example) {
        currentExample.value = { ...example }
        // 格式化示例代码以便更好地显示
        try {
          const parsed = JSON.parse(example.code)
          currentExampleCode.value = JSON.stringify(parsed, null, 2)
        } catch (error) {
          currentExampleCode.value = example.code
        }
      }
    }
    
    // 应用示例模板到编辑器
    const applyExampleTemplate = () => {
      if (!selectedExample.value) {
        ElMessage.warning('请先选择一个示例模板')
        return
      }
      
      try {
        // 格式化示例代码
        const parsed = JSON.parse(currentExample.value.code)
        const formattedCode = JSON.stringify(parsed, null, 2)
        
        // 设置到编辑器
        if (editor) {
          editor.setValue(formattedCode)
          ElMessage.success('示例模板已应用')
        }
        
        // 关闭对话框
        exampleDialogVisible.value = false
        
        // 调整编辑器高度
        if (editor) {
          adjustEditorHeight()
        }
      } catch (error) {
        ElMessage.error('示例模板格式错误，无法应用')
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
      runTest,
      // 示例功能相关
      exampleDialogVisible,
      selectedExample,
      currentExample,
      currentExampleCode,
      exampleTemplates,
      showExampleDialog,
      loadExampleTemplate,
      applyExampleTemplate
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

<style scoped>
/* 示例对话框样式 */
.example-dialog-content {
  max-height: 500px;
  overflow-y: auto;
}

.example-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.example-title {
  font-weight: bold;
  font-size: 14px;
}

.example-desc {
  font-size: 12px;
  color: #606266;
}
</style>
