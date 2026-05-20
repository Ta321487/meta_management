/**
 * 批量启/禁用合一按钮：选中行中只要存在未启用项 → 批量启用，否则 → 批量禁用。
 * @param {Array} selectedRows
 * @param {'isEnabled'|'status'} statusKey
 */
export function resolveBatchEnableToggle(selectedRows, statusKey = 'isEnabled') {
  const rows = selectedRows || []
  if (!rows.length) {
    return { disabled: true, label: '批量启用', type: 'success', targetStatus: 1 }
  }
  const needEnable = rows.some((row) => row[statusKey] === 0)
  return {
    disabled: false,
    label: needEnable ? '批量启用' : '批量禁用',
    type: needEnable ? 'success' : 'warning',
    targetStatus: needEnable ? 1 : 0
  }
}
