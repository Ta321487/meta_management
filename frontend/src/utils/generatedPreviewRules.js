/**

 * 根据与代码生成器同源的 validationRules / businessRules 构建 Element Plus 表单 rules

 */



const BUILTIN_INTEGER_PATTERN = /^-?\d+$/

const BUILTIN_NUMBER_PATTERN = /^-?\d+(\.\d+)?$/



function isNumberField(field) {

  const t = (field.fieldType || field.field?.fieldType || '').toLowerCase()

  return field.formComponent === 'number' || ['int', 'number', 'decimal', 'double', 'float'].some(x => t.includes(x))

}



function parseValidateRuleObject(field) {

  const raw = field?.validateRule ?? field?.field?.validateRule

  if (!raw) return null

  if (typeof raw === 'object') return raw

  try {

    return JSON.parse(raw)

  } catch {

    return null

  }

}



function isIntegerValidateField(field, vr) {

  const raw = parseValidateRuleObject(field)

  if (raw?.integer === true) return true

  if (vr?.integer === true) return true

  const t = (field.fieldType || field.field?.fieldType || '').toLowerCase()

  if (/^(tiny|small|medium|big)?int(\(\d+\))?$/.test(t.replace(/\s/g, ''))) return true

  const pattern = vr?.pattern ? String(vr.pattern) : ''

  return pattern === '^-?\\d+$' || pattern === BUILTIN_INTEGER_PATTERN.source

}



function isBuiltinNumberPattern(pattern) {

  if (!pattern) return false

  const p = String(pattern).trim()

  return p === '^-?\\d+(\\.\\d+)?$' || p === BUILTIN_NUMBER_PATTERN.source

}



function pushNumericTypeRule(list) {

  list.push({

    validator: (_rule, value, callback) => {

      if (value === null || value === undefined || value === '') {

        callback()

        return

      }

      const n = Number(value)

      if (Number.isNaN(n) || !Number.isFinite(n)) {

        callback(new Error('请输入有效的数字'))

      } else {

        callback()

      }

    },

    trigger: 'blur'

  })

}



function pushIntegerRule(list, message) {

  list.push({

    validator: (_rule, value, callback) => {

      if (value === null || value === undefined || value === '') {

        callback()

        return

      }

      const n = Number(value)

      if (Number.isNaN(n) || !Number.isFinite(n)) {

        callback(new Error(message || '必须输入整数'))

        return

      }

      if (!Number.isInteger(n)) {

        callback(new Error(message || '必须输入整数'))

        return

      }

      callback()

    },

    trigger: 'blur'

  })

}



function pushPatternRule(list, pattern, message) {

  const re = pattern instanceof RegExp ? pattern : new RegExp(pattern)

  list.push({

    validator: (_rule, value, callback) => {

      if (value === null || value === undefined || value === '') {

        callback()

        return

      }

      if (re.test(String(value))) {

        callback()

      } else {

        callback(new Error(message || '格式不正确'))

      }

    },

    trigger: 'blur'

  })

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



    const numericField = isNumberField(field)

    const integerField = numericField && isIntegerValidateField(field, vr)



    if (numericField) {

      pushNumericTypeRule(list)

    }



    if (vr.hasPattern && vr.pattern) {
      const patternKind =
        vr.patternKind ||
        (integerField ? 'integer' : numericField && isBuiltinNumberPattern(vr.pattern) ? 'builtinNumber' : 'custom')
      if (patternKind === 'integer' || integerField) {
        pushIntegerRule(list, vr.patternMessage)
      } else if (patternKind === 'builtinNumber' && numericField) {
        // 内置 number：Number() + hasRange，不用正则
      } else {
        pushPatternRule(list, vr.pattern, vr.patternMessage || '格式不正确')
      }
    }



    if (vr.hasLength) {

      const item = { message: vr.lengthMessage || '长度不符合要求', trigger: 'blur' }

      if (vr.minLength != null) item.min = Number(vr.minLength)

      if (vr.maxLength != null) item.max = Number(vr.maxLength)

      if (!numericField) {

        list.push(item)

      }

    }



    if (vr.hasRange) {

      const min = vr.min != null ? Number(vr.min) : null

      const max = vr.max != null ? Number(vr.max) : null

      const rangeMsg = vr.rangeMessage || vr.patternMessage || '数值超出范围'

      list.push({

        validator: (_rule, value, callback) => {

          if (value === null || value === undefined || value === '') {

            callback()

            return

          }

          const n = Number(value)

          if (Number.isNaN(n) || !Number.isFinite(n)) {

            callback(new Error('请输入有效的数字'))

            return

          }

          if (min != null && n < min) {

            callback(new Error(rangeMsg))

            return

          }

          if (max != null && n > max) {

            callback(new Error(rangeMsg))

            return

          }

          callback()

        },

        trigger: 'blur'

      })

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



export function isPreviewIntegerField(field) {

  const vr = field?.validationRules || {}

  return isNumberField(field) && isIntegerValidateField(field, vr)

}



export function previewNumberPrecision(field) {

  if (isPreviewIntegerField(field)) return 0

  const t = (field.fieldType || field.field?.fieldType || '').toLowerCase()

  const m = t.match(/decimal\s*\(\s*\d+\s*,\s*(\d+)\s*\)/i)

  if (m) return parseInt(m[1], 10)

  if (/decimal|numeric|float|double/.test(t)) return 4

  return undefined

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


