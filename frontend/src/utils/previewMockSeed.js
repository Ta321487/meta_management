import request from './request'
import { generateMockDataList, toMockFieldShape } from './mockDataGenerator'

/**
 * 为预览 Mock 按字段类型生成示例行（与列表预览同源逻辑）
 */
export function buildMockRowsForTable(tableSpec, count = 8, idStart = 1001) {
  const listFields = tableSpec?.list?.fields || tableSpec?.form?.fields || []
  const pk = tableSpec?.list?.primaryKeyCamelCase || tableSpec?.form?.primaryKeyCamelCase || 'id'
  const rows = generateMockDataList(listFields, count)
  rows.forEach((row, i) => {
    const id = idStart + i
    row.id = id
    row[pk] = id
  })
  return rows
}

export async function seedTableMock(mockApiBase, tableSpec, count = 8, clearFirst = false) {
  const rows = buildMockRowsForTable(tableSpec, count)
  const base = mockApiBase.replace(/^\//, '')
  const res = await request.post(`/${base}/seed`, { rows, clearFirst })
  return res
}

/** 业务系统下全部表填充示例数据 */
export async function seedBusinessMock(spec, options = {}) {
  const { rowsPerTable = 8, clearFirst = false, onlyEmpty = false } = options
  const tables = spec?.tables || []
  let total = 0
  const details = []

  for (const table of tables) {
    if (onlyEmpty) {
      const listRes = await request.get(`/${table.mockApiBase.replace(/^\//, '')}/page`, {
        params: { page: 1, size: 1 }
      })
      if (listRes.code === 200 && listRes.data?.total > 0) {
        details.push({ tableCode: table.tableCode, skipped: true, count: 0 })
        continue
      }
    }
    const res = await seedTableMock(table.mockApiBase, table, rowsPerTable, clearFirst)
    const n = res.code === 200 ? (res.data ?? rowsPerTable) : 0
    total += n
    details.push({ tableCode: table.tableCode, tableName: table.tableName, count: n })
  }

  return { total, details }
}

/** 当前表是否已有数据 */
export async function isTableMockEmpty(mockApiBase) {
  const base = mockApiBase.replace(/^\//, '')
  const res = await request.get(`/${base}/page`, { params: { page: 1, size: 1 } })
  return res.code === 200 && (!res.data?.total || res.data.total === 0)
}

export { toMockFieldShape }
