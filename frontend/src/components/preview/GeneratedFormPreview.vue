<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left row">
        <el-button link type="primary" class="back-btn" @click="$emit('cancel')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div>
          <h2 class="page-title">{{ formModel.menuTitle || formModel.tableName }}</h2>
          <span class="page-desc">{{ recordId ? '编辑' : '新增' }}记录</span>
        </div>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" label-position="right">
        <el-row :gutter="24">
          <el-col
            v-for="field in formFields"
            :key="field.camelCaseName"
            :xs="24"
            :sm="24"
            :md="fc(field) === 'textarea' ? 24 : 12"
            :lg="fc(field) === 'textarea' ? 24 : 12"
          >
            <el-form-item :label="fieldLabel(field)" :prop="field.camelCaseName">
              <el-select
                v-if="field.isForeignKey"
                v-model="form[field.camelCaseName]"
                filterable
                style="width: 100%"
                :placeholder="`请选择${fieldLabel(field)}`"
              >
                <el-option v-for="o in fkOptions(field)" :key="o.id" :label="o.label" :value="o.value" />
              </el-select>
              <el-switch
                v-else-if="switchMeta(field)"
                v-model="form[field.camelCaseName]"
                :active-value="switchMeta(field).activeValue"
                :inactive-value="switchMeta(field).inactiveValue"
                :active-text="switchMeta(field).activeLabel"
                :inactive-text="switchMeta(field).inactiveLabel"
              />
              <el-select
                v-else-if="fc(field) === 'select'"
                v-model="form[field.camelCaseName]"
                style="width: 100%"
              >
                <el-option
                  v-for="opt in selectOptions(field)"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-date-picker
                v-else-if="fc(field) === 'datepicker' || fc(field) === 'date'"
                v-model="form[field.camelCaseName]"
                type="date"
                style="width: 100%"
              />
              <el-input-number
                v-else-if="fc(field) === 'number'"
                v-model="form[field.camelCaseName]"
                style="width: 100%"
                :precision="numberPrecision(field)"
                :step="isIntegerField(field) ? 1 : undefined"
              />
              <el-input
                v-else-if="fc(field) === 'textarea'"
                v-model="form[field.camelCaseName]"
                type="textarea"
                :rows="4"
              />
              <el-input v-else v-model="form[field.camelCaseName]" :placeholder="`请输入${fieldLabel(field)}`" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-actions">
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
          <el-button @click="$emit('cancel')">取消</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  buildGeneratedFormRules,
  isPreviewIntegerField,
  previewNumberPrecision,
  validateUniqueCombo
} from '../../utils/generatedPreviewRules'
import { createPreviewMockApi } from '../../utils/previewMockApi'
import { loadPreviewFkOptions } from '../../utils/previewFkOptions'
import { resolveSwitchMeta, selectOptions } from '../../utils/previewFieldUtils'

function switchMeta(field) {
  return resolveSwitchMeta(field)
}

const props = defineProps({
  formModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true },
  recordId: { type: [String, Number], default: null }
})

const emit = defineEmits(['saved', 'cancel'])

const formRef = ref(null)
const form = reactive({})
const saving = ref(false)
const fkOptionsMap = reactive({})
const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.formModel.primaryKeyCamelCase)
)

const formFields = computed(() => (props.formModel.fields || []).filter(f => fc(f) !== 'primary_key'))

const formRules = computed(() =>
  buildGeneratedFormRules(props.formModel.fields, props.formModel.businessRules, form, api.value)
)

function fc(field) {
  return field.formComponent || field.field?.formComponent || 'input'
}

function isNumericFormField(field) {
  const t = (field.fieldType || field.field?.fieldType || '').toLowerCase()
  return fc(field) === 'number' || /int|decimal|numeric|float|double/.test(t)
}

function isIntegerField(field) {
  return isPreviewIntegerField(field)
}

function numberPrecision(field) {
  return previewNumberPrecision(field)
}

function coerceRecordNumericFields(target, fields) {
  ;(fields || []).forEach(f => {
    const prop = f.camelCaseName
    if (!prop || !isNumericFormField(f)) return
    const v = target[prop]
    if (v === null || v === undefined || v === '') return
    const n = Number(v)
    if (!Number.isNaN(n)) {
      target[prop] = isIntegerField(f) ? Math.trunc(n) : n
    }
  })
}
function fieldLabel(field) {
  return field.label || field.field?.label || field.camelCaseName
}
function fkOptions(field) {
  return fkOptionsMap[field.camelCaseName] || []
}

async function loadAllFkOptions() {
  const code = props.formModel.businessCode
  if (!code) return
  for (const field of formFields.value) {
    if (!field.isForeignKey) continue
    fkOptionsMap[field.camelCaseName] = await loadPreviewFkOptions(code, field)
  }
}

function initForm() {
  Object.keys(form).forEach(k => delete form[k])
  formFields.value.forEach(f => {
    const sw = resolveSwitchMeta(f)
    if (sw) {
      form[f.camelCaseName] = sw.inactiveValue
    } else if (fc(f) === 'number') {
      form[f.camelCaseName] = null
    } else {
      form[f.camelCaseName] = ''
    }
  })
}

async function loadRecord() {
  initForm()
  if (!props.recordId) return
  const res = await api.value.getById(props.recordId)
  if (res.code === 200 && res.data) {
    Object.assign(form, res.data)
    coerceRecordNumericFields(form, formFields.value)
  }
}

watch(
  () => [props.formModel, props.recordId],
  async () => {
    await loadAllFkOptions()
    await loadRecord()
  },
  { immediate: true, deep: true }
)

async function submit() {
  await formRef.value.validate(async valid => {
    if (!valid) return
    const combo = await validateUniqueCombo(form, props.formModel.businessRules, api.value)
    if (!combo.ok) {
      ElMessage.error(combo.message)
      return
    }
    saving.value = true
    try {
      const pk = props.formModel.primaryKeyCamelCase || 'id'
      const payload = { ...form }
      if (props.recordId != null && props.recordId !== '') {
        payload[pk] = props.recordId
        payload.id = props.recordId
        await api.value.update(payload)
      } else {
        await api.value.add(payload)
      }
      ElMessage.success('保存成功（预览 Mock）')
      emit('saved')
    } catch (e) {
      ElMessage.error(e.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

function reset() {
  formRef.value?.resetFields()
  initForm()
}
</script>
