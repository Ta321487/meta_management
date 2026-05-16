/**
 * 根据与代码生成器同源的 validationRules / businessRules 构建 Element Plus 表单 rules
 */

function isNumberField(field) {
  const t = (field.fieldType || field.field?.fieldType || '').toLowerCase()
  return field.formComponent === 'number' || ['int', 'number', 'decimal', 'double', 'float'].some(x => t.includes(x))
}

export function buildGeneratedFormRules(fields, businessRules, formData, api) {
  const rules = {}
  const fieldList = fields || []

  const findField = (name) => fieldList.find(f => (f.fieldName || f.field?.fieldName) === name)

  fieldList.forEach(field => {
    const prop = field.camelCaseName
    if (!prop) return
    const label = field.label || field.field?.label || prop
    const vr = field.validationRules || {}
    const list = []

    const required = field.isRequired === 1 || field.field?.isRequired === 1
    if (required) {
      const isSelect = (field.formComponent || field.field?.formComponent) === 'select'
      list.push({
        required: true,
        message: isSelect ? `请选择${label}` : `请输入${label}`,
        trigger: 'blur'
      })
    }

    if (isNumberField(field)) {
      list.push({ type: 'number', message: '请输入有效的数字', trigger: 'blur' })
    }

    if (vr.hasPattern && vr.pattern) {
      list.push({
        pattern: new RegExp(vr.pattern),
        message: vr.patternMessage || '格式不正确',
        trigger: 'blur'
      })
    }

    if (vr.hasLength) {
      const item = { message: vr.lengthMessage || '长度不符合要求', trigger: 'blur' }
      if (vr.minLength != null) item.min = Number(vr.minLength)
      if (vr.maxLength != null) item.max = Number(vr.maxLength)
      list.push(item)
    }

    if (vr.hasRange) {
      const item = {
        type: isNumberField(field) ? 'number' : 'string',
        message: vr.rangeMessage || '数值超出范围',
        trigger: 'blur'
      }
      if (vr.min != null) item.min = Number(vr.min)
      if (vr.max != null) item.max = Number(vr.max)
      list.push(item)
    }

    if (vr.hasCrossField && vr.crossField2) {
      const compareField = findField(vr.crossField2)
      const compareProp = compareField?.camelCaseName
      const op = vr.crossFieldOperator || '>='
      const msg = vr.crossFieldMessage || vr.crossFieldCondition || '校验失败'
      if (compareProp) {
        list.push({
          validator: (_rule, value, callback) => {
            const compareValue = formData[compareProp]
            if (value == null || value === '' && (compareValue == null || compareValue === '')) {
              callback()
              return
            }
            let ok = false
            const a = Number(value)
            const b = Number(compareValue)
            const useNum = !Number.isNaN(a) && !Number.isNaN(b)
            const v1 = useNum ? a : value
            const v2 = useNum ? b : compareValue
            switch (op) {
              case '>=': ok = v1 >= v2; break
              case '<=': ok = v1 <= v2; break
              case '>': ok = v1 > v2; break
              case '<': ok = v1 < v2; break
              case '=':
              case '==': ok = v1 === v2; break
              case '!=':
              case '<>': ok = v1 !== v2; break
              default: ok = true
            }
            ok ? callback() : callback(new Error(msg))
          },
          trigger: 'blur'
        })
      }
    }

    ;(businessRules || []).forEach(br => {
      if (br.ruleType === 'unique' && br.fields) {
        br.fields.forEach(rf => {
          if (rf.fieldName !== (field.fieldName || field.field?.fieldName)) return
          list.push({
            validator: async (_rule, value, callback) => {
              if (!value) {
                callback()
                return
              }
              try {
                const params = { [rf.fieldName]: value }
                const pk = api.primaryKeyCamelCase
                if (formData[pk]) params[pk] = formData[pk]
                const res = await api.checkUnique(params)
                if (res.code === 200 && !res.data) callback()
                else callback(new Error(br.message || '已存在'))
              } catch {
                callback(new Error('验证失败'))
              }
            },
            trigger: 'blur'
          })
        })
      }
    })

    if (list.length) rules[prop] = list
  })

  return rules
}

export async function validateUniqueCombo(formData, businessRules, api) {
  for (const br of businessRules || []) {
    if (br.ruleType !== 'unique_combo' || !br.fields) continue
    const params = {}
    br.fields.forEach(rf => {
      params[rf.fieldName] = formData[rf.camelCaseName]
    })
    const pk = api.primaryKeyCamelCase
    if (formData[pk]) params[pk] = formData[pk]
    try {
      const res = await api.checkUniqueCombo(params)
      if (res.code === 200 && res.data) {
        return { ok: false, message: br.message || '组合已存在' }
      }
    } catch {
      return { ok: false, message: '验证失败' }
    }
  }
  return { ok: true }
}
