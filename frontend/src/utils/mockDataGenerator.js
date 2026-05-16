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

function parseValidateRuleInput(field) {
  if (!field) return null
  if (field.validationRules && typeof field.validationRules === 'object') {
    return field.validationRules
  }
  const raw = field.validateRule
  if (!raw) return null
  if (typeof raw === 'object') return raw
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function pickFromValidationRules(vr) {
  if (!vr) return null
  if (vr.operator === 'IN' && Array.isArray(vr.values) && vr.values.length > 0) {
    return vr.values[Math.floor(Math.random() * vr.values.length)]
  }
  if (Array.isArray(vr)) {
    const inRule = vr.find(r => r?.operator === 'IN' && r.values?.length)
    if (inRule) return inRule.values[Math.floor(Math.random() * inRule.values.length)]
  }
  if (vr.hasOperator && vr.operator === 'IN' && Array.isArray(vr.values) && vr.values.length > 0) {
    return vr.values[Math.floor(Math.random() * vr.values.length)]
  }
  if (vr.hasOptions && Array.isArray(vr.options) && vr.options.length > 0) {
    return vr.options[Math.floor(Math.random() * vr.options.length)]
  }
  return null
}

function pickFromEnumFieldType(fieldType) {
  if (!fieldType) return null
  const ft = String(fieldType).toLowerCase()
  if (!ft.startsWith('enum(') || !ft.endsWith(')')) return null
  const inner = ft.slice(5, -1)
  const values = inner
    .split(',')
    .map(s => s.trim().replace(/^['"]|['"]$/g, ''))
    .filter(Boolean)
  if (!values.length) return null
  return values[Math.floor(Math.random() * values.length)]
}

/** 与生成器 prepareFieldList 一致的列表属性名 */
export function resolveListFieldProp(field) {
  if (!field) return ''
  if (field.camelCaseName) return field.camelCaseName
  const raw = field.field?.fieldName || field.fieldName || ''
  if (!raw) return ''
  if (!raw.includes('_')) {
    return raw.charAt(0).toLowerCase() + raw.slice(1)
  }
  return raw
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part, i) => (i === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)))
    .join('')
}

// 根据字段类型生成模拟值
export function generateMockValue(field) {
  const parsedRule = parseValidateRuleInput(field)
  const fromRules = pickFromValidationRules(parsedRule)
  if (fromRules != null && fromRules !== '') {
    return fromRules
  }

  const fromEnumType = pickFromEnumFieldType(field.fieldType)
  if (fromEnumType != null && fromEnumType !== '') {
    return fromEnumType
  }

  // 1. 优先处理IN约束（validateRule 字符串）
  if (field.validateRule && typeof field.validateRule === 'string') {
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
  
  // 布尔 / 状态位
  if (type.includes('bool') || type.includes('boolean') || type === 'bit' || type.startsWith('tinyint(1)')) {
    return Math.random() > 0.5 ? 1 : 0;
  }
  
  const label = field.label || field.fieldName || '字段'
  return `示例${label}${Math.floor(Math.random() * 1000)}`;
}

/** 将元数据/生成器字段转为 mock 生成器可用的扁平结构 */
export function toMockFieldShape(field) {
  if (!field) return {}
  const meta = field.field || field
  return {
    fieldName: resolveListFieldProp(field) || meta.fieldName || field.fieldName,
    fieldType: field.fieldType || meta.fieldType,
    fieldLength: meta.fieldLength ?? field.fieldLength,
    validateRule: meta.validateRule ?? field.validateRule,
    validationRules: field.validationRules || meta.validationRules,
    formComponent: meta.formComponent || field.formComponent,
    label: field.label || meta.label || field.label
  }
}

// 生成模拟数据列表（属性名优先 camelCaseName，与生成代码一致）
export function generateMockDataList(fields, count = 10) {
  const dataList = []

  for (let i = 0; i < count; i++) {
    const row = {}
    fields.forEach(field => {
      const mockField = toMockFieldShape(field)
      const propName = resolveListFieldProp(field) || getListFieldPropName(mockField) || mockField.fieldName
      if (!propName) return
      row[propName] = generateMockValue(mockField)
    })
    dataList.push(row)
  }

  return dataList
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

// 获取列表字段属性名（扁平 MetadataField；生成器结构请用 resolveListFieldProp）
export function getListFieldPropName(field) {
  if (!field) return ''
  if (field.camelCaseName) return field.camelCaseName
  const raw = field.field?.fieldName || field.fieldName || ''
  if (!raw) return ''
  if (raw.includes('_')) return resolveListFieldProp(field)
  return raw.replace(/^[A-Z]/, char => char.toLowerCase()) || raw
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

// 获取关联数据选项
export function getRelatedDataOptions(relatedTableName) {
  if (!relatedTableName) {
    return [];
  }
  
  // 根据表名生成更通用的模拟数据
  const tableName = relatedTableName.toLowerCase();
  const options = [];
  
  // 生成模拟数据的数量
  const count = 4;
  
  // 根据表名中的关键词生成不同的模拟数据
  if (tableName.includes('user') || tableName.includes('owner') || tableName.includes('person') || tableName.includes('member')) {
    // 用户、所有者、人员、成员相关表
    const names = ['张三', '李四', '王五', '赵六', '孙七', '周八', '吴九', '郑十'];
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: names[i]
      });
    }
  } else if (tableName.includes('type') || tableName.includes('category') || tableName.includes('kind') || tableName.includes('class')) {
    // 类型、分类、种类、类别相关表
    const types = ['类型1', '类型2', '类型3', '类型4', '类型5', '类型6', '类型7', '类型8'];
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: types[i]
      });
    }
  } else if (tableName.includes('status') || tableName.includes('state')) {
    // 状态相关表
    const statuses = ['启用', '禁用', '待审核', '已审核', '已过期', '已删除', '草稿', '发布'];
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: statuses[i]
      });
    }
  } else if (tableName.includes('department') || tableName.includes('dept') || tableName.includes('org') || tableName.includes('company')) {
    // 部门、组织、公司相关表
    const depts = ['部门1', '部门2', '部门3', '部门4', '部门5', '部门6', '总部', '分公司'];
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: depts[i]
      });
    }
  } else if (tableName.includes('product') || tableName.includes('goods') || tableName.includes('item')) {
    // 产品、商品相关表
    const products = ['产品A', '产品B', '产品C', '产品D', '商品1', '商品2', '商品3', '商品4'];
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: products[i]
      });
    }
  } else if (tableName.includes('order') || tableName.includes('bill') || tableName.includes('invoice')) {
    // 订单、账单、发票相关表
    for (let i = 0; i < count; i++) {
      options.push({
        id: 1000 + i + 1,
        name: `${relatedTableName.replace('_TABLE', '')}_${1000 + i + 1}`
      });
    }
  } else {
    // 默认通用模拟数据
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: `${relatedTableName.replace('_TABLE', '')}_${i + 1}`
      });
    }
  }
  
  return options;
}