// 图标中文映射文件 - 实现动态生成图标中文名

// 基础词汇映射表 - 只存储需要特殊处理的词汇
const baseWordMap = {
  // 常用词汇映射
  User: '用户',
  Setting: '设置',
  Home: '首页',
  Menu: '菜单',
  Search: '搜索',
  Edit: '编辑',
  Delete: '删除',
  Plus: '加号',
  Check: '对勾',
  Close: '关闭',
  Warning: '警告',
  Success: '成功',
  Info: '信息',
  Error: '错误',
  
  // 方向词汇
  Arrow: '箭头',
  Up: '上',
  Down: '下',
  Left: '左',
  Right: '右',
  DArrow: '双箭头',
  Caret: '三角',
  
  // 数据相关
  Data: '数据',
  Line: '线',
  Histogram: '柱状图',
  Board: '面板',
  Chart: '图',
  Pie: '饼',
  Trend: '趋势',
  Sort: '排序',
  Rank: '排名',
  
  // 文件相关
  Document: '文档',
  Folder: '文件夹',
  File: '文件',
  
  // 媒体相关
  Picture: '图片',
  Video: '视频',
  Play: '播放',
  Pause: '暂停',
  Camera: '相机',
  Microphone: '麦克风',
  Headset: '耳机',
  Monitor: '显示器',
  
  // 其他
  Filled: '填充',
  Rounded: '圆角',
  Add: '添加',
  Checked: '确认',
  Copy: '复制'
}

// 特殊图标名称完全映射 - 用于处理不符合常规命名规则的特殊情况
const specialIconMap = {
  // 完全不符合规则的特殊情况
  Menu: '菜单',
  Plus: '加号',
  Check: '对勾',
  Close: '关闭',
  'ArrowRightBold': '右箭头加粗',
  'ArrowLeftBold': '左箭头加粗'
}

/**
 * 动态生成图标中文名的函数
 * @param {string} iconName - 图标英文名称
 * @returns {string} 图标中文名称
 */
function generateChineseName(iconName) {
  // 检查输入是否有效
  if (typeof iconName !== 'string' || iconName === '') {
    return ''
  }
  
  // 1. 先检查是否在特殊映射表中
  if (specialIconMap[iconName]) {
    return specialIconMap[iconName]
  }
  
  // 2. 拆分驼峰命名
  const words = splitCamelCase(iconName)
  
  // 3. 转换每个词汇
  const chineseWords = words.map(word => {
    // 查找词汇映射
    if (baseWordMap[word]) {
      return baseWordMap[word]
    }
    // 如果没有映射，保留英文原词（首字母大写）
    return word
  })
  
  // 4. 组合成中文名称
  return chineseWords.join('')
}

/**
 * 将驼峰命名的字符串拆分为单词数组
 * @param {string} str - 驼峰命名的字符串
 * @returns {string[]} 单词数组
 */
function splitCamelCase(str) {
  // 检查输入是否为字符串类型
  if (typeof str !== 'string' || str === '') {
    return []
  }
  // 匹配大写字母前的位置（除了第一个字符）
  return str.split(/(?=[A-Z])/)
}

// 创建一个代理对象，在访问属性时动态生成中文名称
const iconChineseNames = new Proxy({}, {
  get(target, prop) {
    if (typeof prop === 'string') {
      return generateChineseName(prop)
    }
    return target[prop]
  }
})

// 导出图标中文映射对象
export default iconChineseNames

// 导出辅助函数，供其他地方使用
export { generateChineseName, splitCamelCase }