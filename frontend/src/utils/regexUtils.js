/**
 * 正则表达式工具函数
 * 用于归一化和修复从数据库读取的正则表达式
 */

/**
 * 归一化并修复正则表达式
 * @param {string} pattern - 原始正则表达式字符串
 * @param {Object} options - 选项
 * @param {boolean} options.createRegExp - 是否创建RegExp对象，默认false
 * @param {boolean} options.fixCorrupted - 是否尝试修复损坏的正则表达式，默认true
 * @returns {string|RegExp} - 处理后的正则表达式字符串或RegExp对象
 * @throws {Error} - 如果正则表达式无效且无法修复
 */
export function normalizeRegexPattern(pattern, options = {}) {
  const { createRegExp = false, fixCorrupted = true } = options;
  
  // 处理空字符串
  if (typeof pattern !== 'string' || pattern.trim() === '') {
    if (createRegExp) {
      throw new Error('正则表达式不能为空');
    }
    return pattern;
  }
  
  // 归一化处理：将双反斜杠转换为单反斜杠
  // 这是因为数据库中存储时会将单个反斜杠转义为双反斜杠
  // JSON解析后，如果看到"\\d"（两个字符：反斜杠+反斜杠+d），需要转换为"\d"（反斜杠+d）
  let normalizedPattern = pattern.replace(/\\\\/g, '\\');
  
  // 尝试修复常见的损坏情况（如果正则表达式看起来被损坏了）
  if (fixCorrupted) {
    // 检查是否有 [数字] 而不是 {数字}，或者单独的 d/w/s 等字符
    const hasBracketNumbers = /\[(\d+)\]/.test(normalizedPattern);
    const hasUnescapedChars = /(^|[^\\])\b[dwsDWS]\b/.test(normalizedPattern);
    
    if (hasBracketNumbers || hasUnescapedChars) {
      console.warn(`检测到可能损坏的正则表达式，尝试修复: ${normalizedPattern}`);
      
      // 先修复 [数字] 格式（应该是 {数字}）
      if (hasBracketNumbers) {
        normalizedPattern = normalizedPattern.replace(/\[(\d+)\]/g, '{$1}');
        console.log(`修复 [数字] 格式后的正则: ${normalizedPattern}`);
      }
      
      // 尝试修复：将单独的 d, w, s, D, W, S 替换为转义形式
      const originalPattern = normalizedPattern;
      normalizedPattern = normalizedPattern
        .replace(/(^|[^\\])\bd\b/g, '$1\\d')  // 单独的 d 替换为 \d
        .replace(/(^|[^\\])\bw\b/g, '$1\\w')  // 单独的 w 替换为 \w
        .replace(/(^|[^\\])\bs\b/g, '$1\\s')  // 单独的 s 替换为 \s
        .replace(/(^|[^\\])\bD\b/g, '$1\\D')  // 单独的 D 替换为 \D
        .replace(/(^|[^\\])\bW\b/g, '$1\\W')  // 单独的 W 替换为 \W
        .replace(/(^|[^\\])\bS\b/g, '$1\\S'); // 单独的 S 替换为 \S
      
      if (normalizedPattern !== originalPattern) {
        console.log(`修复未转义字符后的正则: ${normalizedPattern}`);
      }
    }
  }
  
  // 如果需要创建RegExp对象
  if (createRegExp) {
    try {
      return new RegExp(normalizedPattern);
    } catch (error) {
      throw new Error(`正则表达式无效: ${normalizedPattern} - ${error.message}`);
    }
  }
  
  return normalizedPattern;
}

/**
 * 验证并处理规则对象中的pattern字段
 * @param {Object} rule - 验证规则对象
 * @param {Object} options - 选项
 * @returns {Object|null} - 处理后的规则对象，如果无效则返回null
 */
export function processPatternRule(rule, options = {}) {
  if (!rule || !rule.pattern) {
    return rule;
  }
  
  // 处理空字符串pattern的情况
  if (typeof rule.pattern === 'string' && rule.pattern.trim() === '') {
    console.warn('检测到空字符串pattern，跳过该规则');
    return null;
  }
  
  try {
    // 归一化并修复正则表达式
    const normalizedPattern = normalizeRegexPattern(rule.pattern, {
      fixCorrupted: options.fixCorrupted !== false
    });
    
    // 创建RegExp对象（Element Plus需要）
    const regex = normalizeRegexPattern(normalizedPattern, {
      createRegExp: true,
      fixCorrupted: false
    });
    
    // 返回更新后的规则对象
    return {
      ...rule,
      pattern: regex
    };
  } catch (error) {
    console.error(`正则表达式无效: ${rule.pattern}`, error);
    return null;
  }
}
