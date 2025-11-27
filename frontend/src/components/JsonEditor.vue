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
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as monaco from 'monaco-editor'
import { ElMessage } from 'element-plus'

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

    onBeforeUnmount(() => {
      if (editor) {
        editor.dispose()
      }
    })

    return {
      editorContainer,
      formatJson,
      clearJson
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
  border-radius: 0 0 4px 4px;
  overflow: hidden;
  flex: 1;
}
</style>