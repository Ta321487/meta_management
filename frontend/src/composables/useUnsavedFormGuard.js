import { ref, watch, nextTick, onUnmounted, toRaw } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox } from 'element-plus'

export function snapshotValue(value) {
  try {
    return JSON.parse(JSON.stringify(toRaw(value)))
  } catch {
    return value
  }
}

export function valuesEqual(a, b) {
  return JSON.stringify(a) === JSON.stringify(b)
}

export function isDirtyCompared(current, baseline) {
  if (baseline == null) return false
  return !valuesEqual(snapshotValue(current), baseline)
}

/**
 * 表单未保存检测（弹窗 / 任意 reactive 对象）
 */
export function useUnsavedFormGuard(formSource, options = {}) {
  const baseline = ref(null)
  const {
    message = '当前有未保存的修改，确定要放弃吗？',
    title = '未保存的修改',
    confirmButtonText = '放弃修改',
    cancelButtonText = '继续编辑'
  } = options

  function captureSnapshot() {
    baseline.value = snapshotValue(formSource)
  }

  function markClean() {
    baseline.value = snapshotValue(formSource)
  }

  function checkDirty() {
    return isDirtyCompared(formSource, baseline.value)
  }

  async function confirmDiscard() {
    if (!checkDirty()) return true
    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText,
        cancelButtonText,
        type: 'warning',
        distinguishCancelAndClose: true
      })
      return true
    } catch {
      return false
    }
  }

  async function handleBeforeClose(done) {
    if (!(await confirmDiscard())) return
    markClean()
    if (typeof options.onReset === 'function') {
      options.onReset()
    }
    done()
  }

  function requestClose(setVisible) {
    if (typeof setVisible === 'function') {
      setVisible(false)
    }
  }

  return {
    baseline,
    captureSnapshot,
    markClean,
    checkDirty,
    confirmDiscard,
    handleBeforeClose,
    requestClose
  }
}

/**
 * 编辑弹窗：打开时拍快照，关闭前确认；取消按钮调用 requestCloseDialog
 */
export function useDialogFormGuard(form, dialogVisible, options = {}) {
  const guard = useUnsavedFormGuard(form, options)

  watch(dialogVisible, async (visible) => {
    if (visible) {
      await nextTick()
      guard.captureSnapshot()
    }
  })

  function requestCloseDialog() {
    dialogVisible.value = false
  }

  return {
    ...guard,
    requestCloseDialog
  }
}

/**
 * 页面级大段编辑（如 SQL 编辑器）：路由离开 / 刷新前提示
 */
export function useRouteLeaveGuard(getContent, options = {}) {
  const baseline = ref('')
  const {
    message = '当前有未保存的 SQL，确定要离开吗？',
    title = '未保存的修改',
    confirmButtonText = '离开',
    cancelButtonText = '继续编辑',
    enableBeforeUnload = true
  } = options

  function captureSnapshot() {
    baseline.value = typeof getContent === 'function' ? getContent() : ''
  }

  function markClean() {
    captureSnapshot()
  }

  function checkDirty() {
    const current = typeof getContent === 'function' ? getContent() : ''
    return (current || '').trim() !== (baseline.value || '').trim()
  }

  async function confirmDiscard() {
    if (!checkDirty()) return true
    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText,
        cancelButtonText,
        type: 'warning',
        distinguishCancelAndClose: true
      })
      return true
    } catch {
      return false
    }
  }

  function onBeforeUnload(e) {
    if (!checkDirty()) return
    e.preventDefault()
    e.returnValue = ''
  }

  if (enableBeforeUnload && typeof window !== 'undefined') {
    window.addEventListener('beforeunload', onBeforeUnload)
    onUnmounted(() => {
      window.removeEventListener('beforeunload', onBeforeUnload)
    })
  }

  onBeforeRouteLeave(async (_to, _from, next) => {
    if (await confirmDiscard()) {
      next()
    } else {
      next(false)
    }
  })

  return {
    captureSnapshot,
    markClean,
    checkDirty,
    confirmDiscard,
    disposeBeforeUnload: () => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('beforeunload', onBeforeUnload)
      }
    }
  }
}
