// 生成模拟数据的工具函数

// 生成指定长度的随机数字字符串
function generateRandomDigitString(length) {
  let result = '';
  for (let i = 0; i < length; i++) {
    // 允许首位为0，适应不同业务需求
    result += Math.floor(Math.random() * 10).toString();
  }
  return result;
}

// 根据字段类型生成模拟值
export function generateMockValue(field) {
  // 1. 优先处理IN约束
  if (field.validateRule) {
    try {
      const validateRule = JSON.parse(field.validateRule);
      
      // 处理单个IN约束对象
      if (validateRule.operator === 'IN' && validateRule.values && validateRule.values.length > 0) {
        return validateRule.values[Math.floor(Math.random() * validateRule.values.length)];
      }
      // 处理多个规则对象组成的数组中的IN约束
      else if (Array.isArray(validateRule)) {
        const inRule = validateRule.find(rule => rule.operator === 'IN' && rule.values && rule.values.length > 0);
        if (inRule) {
          return inRule.values[Math.floor(Math.random() * inRule.values.length)];
        }
      }
    } catch (e) {
      console.error('Failed to parse validateRule for IN constraint:', e);
    }
  }
  
  // 2. 接着处理正则表达式约束
  if (field.validateRule) {
    try {
      const validateRule = JSON.parse(field.validateRule);
      let pattern = '';
      
      // 获取正则表达式
      if (validateRule.pattern) {
        pattern = validateRule.pattern;
      } else if (Array.isArray(validateRule)) {
        const regexRule = validateRule.find(rule => rule.pattern);
        if (regexRule) {
          pattern = regexRule.pattern;
        }
      }
      
      if (pattern) {
        // 修复转义问题
        const normalizedPattern = pattern.replace(/\\/g, '\\');
        
        // 使用更灵活的匹配方式
        // 匹配数字位数，如\d{8}、\d{12}等，支持锚点
        const digitMatch = normalizedPattern.match(/\\d\{(\d+)\}/);
        if (digitMatch) {
          // 提取数字位数n
          const n = parseInt(digitMatch[1]);
          return generateRandomDigitString(n);
        } else if (normalizedPattern.includes('1[3-9]\\d{9}')) {
          // 手机号
          const prefixes = ['13', '14', '15', '16', '17', '18', '19'];
          const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
          const suffix = Math.floor(100000000 + Math.random() * 900000000).toString();
          return `${prefix}${suffix}`;
        } else if (normalizedPattern.includes('@') && !field.fieldName?.toLowerCase().includes('password') && !field.fieldName?.toLowerCase().includes('pwd') && !field.fieldName?.toLowerCase().includes('密码')) {
          // 邮箱 - 排除密码字段
          const username = Math.random().toString(36).substr(2, 10);
          const domains = ['gmail.com', 'example.com', 'test.com', 'yahoo.com', 'hotmail.com'];
          return `${username}@${domains[Math.floor(Math.random() * domains.length)]}`;
        }
      }
    } catch (e) {
      console.error('Failed to parse validateRule for regex:', e);
    }
  }
  
  // 3. 处理数值范围约束
  if (field.validateRule) {
    try {
      const validateRule = JSON.parse(field.validateRule);
      let min = null;
      let max = null;
      
      // 处理单个对象形式的约束
      const extractRangeFromRule = (rule) => {
        if (rule.min !== undefined && rule.min !== null && rule.min !== '') {
          min = Number(rule.min);
        }
        if (rule.max !== undefined && rule.max !== null && rule.max !== '') {
          max = Number(rule.max);
        }
      };
      
      // 处理单个对象
      if (typeof validateRule === 'object' && !Array.isArray(validateRule)) {
        extractRangeFromRule(validateRule);
      }
      // 处理数组形式
      else if (Array.isArray(validateRule)) {
        for (const rule of validateRule) {
          extractRangeFromRule(rule);
          // 如果已经找到min和max，就不再继续查找
          if (min !== null && max !== null) {
            break;
          }
        }
      }
      
      // 生成符合范围的随机数
      const { fieldType } = field;
      if (fieldType && (min !== null || max !== null)) {
        const type = fieldType.toLowerCase();
        let result;
        
        // 设置默认值
        const defaultMin = 0;
        const defaultMax = 1000;
        const finalMin = min !== null ? min : defaultMin;
        const finalMax = max !== null ? max : defaultMax;
        
        // 确保min <= max
        const actualMin = Math.min(finalMin, finalMax);
        const actualMax = Math.max(finalMin, finalMax);
        
        if (type.includes('int') || type.includes('bigint') || type.includes('smallint')) {
          // 整数类型
          result = Math.floor(Math.random() * (actualMax - actualMin + 1)) + actualMin;
        } else if (type.includes('decimal') || type.includes('numeric') || type.includes('float') || type.includes('double')) {
          // 小数类型
          result = (Math.random() * (actualMax - actualMin) + actualMin).toFixed(2);
        } else if (type === 'number') {
          // 通用数字类型
          result = Math.floor(Math.random() * (actualMax - actualMin + 1)) + actualMin;
        }
        
        if (result !== undefined) {
          return result;
        }
      }
    } catch (e) {
      console.error('Failed to parse validateRule for range constraint:', e);
    }
  }
  
  // 4. 然后处理字段名特殊情况
  const fieldName = field.fieldName?.toLowerCase() || '';
  
  // 密码字段
  if (fieldName.includes('password') || fieldName.includes('pwd') || fieldName.includes('密码')) {
    // 生成包含字母、数字和特殊字符的随机密码
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+~`|}{[]:;?><,./-=';
    const passwordLength = 8;
    let password = '';
    for (let i = 0; i < passwordLength; i++) {
      password += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return password;
  }
  
  // 邮箱字段
  if (fieldName.includes('email') || fieldName.includes('mail')) {
    const username = Math.random().toString(36).substr(2, 10);
    const domains = ['gmail.com', 'example.com', 'test.com', 'yahoo.com', 'hotmail.com'];
    return `${username}@${domains[Math.floor(Math.random() * domains.length)]}`;
  }
  
  // 性别字段
  if (fieldName.includes('gender') || fieldName.includes('sex')) {
    return Math.random() > 0.5 ? '男' : '女';
  }
  
  // 状态字段
  if (fieldName.includes('status')) {
    const statuses = ['在读', '休学', '毕业', '退学'];
    return statuses[Math.floor(Math.random() * statuses.length)];
  }
  
  // 手机号字段
  if (fieldName.includes('phone') || fieldName.includes('mobile')) {
    const prefixes = ['13', '14', '15', '16', '17', '18', '19'];
    const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
    const suffix = Math.floor(100000000 + Math.random() * 900000000).toString();
    return `${prefix}${suffix}`;
  }
  
  // 编号字段
  if (fieldName.includes('no') || fieldName.includes('code')) {
    const fieldLength = field.fieldLength || 10;
    return generateRandomDigitString(fieldLength);
  }
  
  // 4. 最后使用原有逻辑
  const { fieldType, fieldLength = 255 } = field;
  
  if (!fieldType) return '';
  
  const type = fieldType.toLowerCase();
  
  // 字符串类型
  if (type.includes('char') || type.includes('text') || type === 'varchar') {
    const length = Math.min(fieldLength, 20);
    return `模拟文本${Math.random().toString(36).substr(2, length - 4)}`;
  }
  
  // 数字类型
  if (type.includes('int') || type.includes('bigint') || type.includes('smallint')) {
    return Math.floor(Math.random() * 1000);
  }
  
  if (type.includes('decimal') || type.includes('numeric') || type.includes('float') || type.includes('double')) {
    return (Math.random() * 1000).toFixed(2);
  }
  
  // 日期时间类型
  if (type.includes('date')) {
    return '2023-01-01';
  }
  
  if (type.includes('time')) {
    return '12:00:00';
  }
  
  if (type.includes('datetime') || type.includes('timestamp')) {
    return '2023-01-01 12:00:00';
  }
  
  // 布尔类型
  if (type.includes('bool') || type.includes('boolean')) {
    return Math.random() > 0.5 ? true : false;
  }
  
  // 默认返回空字符串
  return '';
}

// 生成模拟数据列表
export function generateMockDataList(fields, count = 10) {
  const dataList = [];
  
  for (let i = 0; i < count; i++) {
    const row = {};
    fields.forEach(field => {
      const propName = field.fieldName?.replace(/^[A-Z]/, char => char.toLowerCase()) || field.fieldName;
      row[propName] = generateMockValue(field);
    });
    dataList.push(row);
  }
  
  return dataList;
}

// 格式化预览日期
export function formatPreviewDate(dateStr) {
  if (!dateStr) return '';
  return dateStr;
}

// 格式化预览数字
export function formatPreviewNumber(num) {
  if (num === undefined || num === null) return '';
  return num.toString();
}

// 获取列表字段属性名
export function getListFieldPropName(field) {
  if (!field) return '';
  return field.fieldName?.replace(/^[A-Z]/, char => char.toLowerCase()) || field.fieldName;
}

// 获取表单字段属性名
export function getFormFieldPropName(field) {
  if (!field) return '';
  return field.fieldName?.replace(/^[A-Z]/, char => char.toLowerCase()) || field.fieldName;
}

// 获取表单字段选项
export function getFormFieldOptions(field) {
  if (!field) return [];
  
  // 首先尝试从validateRule获取IN约束选项
  if (field.validateRule) {
    try {
      const validateRule = JSON.parse(field.validateRule);
      // 处理单个IN约束对象
      if (validateRule.operator === 'IN' && validateRule.values) {
        return validateRule.values.map(value => ({ label: value, value }));
      }
      // 处理多个约束对象组成的数组
      if (Array.isArray(validateRule)) {
        for (const rule of validateRule) {
          if (rule.operator === 'IN' && rule.values) {
            return rule.values.map(value => ({ label: value, value }));
          }
        }
      }
    } catch (e) {
      console.error('Failed to parse validateRule:', e);
    }
  }
  
  // 原有逻辑：从enumValues获取选项（兼容原有代码）
  if (field.enumValues) {
    try {
      return JSON.parse(field.enumValues);
    } catch (e) {
      console.error('Failed to parse enumValues:', e);
    }
  }
  
  return [];
}