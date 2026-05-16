# -*- coding: utf-8 -*-
from pathlib import Path

path = Path(r"d:\Java\meta-management\src\main\resources\templates\vue_form.vue.ftl")
s = path.read_text(encoding="utf-8")

old_tpl = s[s.index("<template>"):s.index("</template>") + len("</template>")]

new_tpl = r'''<template>
  <div class="admin-page ${componentName?lower_case}-form">
    <div class="page-header">
      <div class="page-header-left">
        <el-button link type="primary" class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <motion>
          <h2 class="page-title">${table.tableName}</h2>
          <span class="page-desc">{{ isEdit ? '编辑' : '新增' }}记录</span>
        </div>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" label-position="right">
        <el-row :gutter="24">
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
        <el-col :xs="24" :sm="24" :md="${(field.field.formComponent == 'textarea')?then(24, 12)}" :lg="${(field.field.formComponent == 'textarea')?then(24, 12)}">
        <el-form-item label="${field.field.label}" prop="${field.camelCaseName}">
          <#if field.isForeignKey!false>
          <el-select v-model="form.${field.camelCaseName}" placeholder="请选择${field.field.label}" style="width: 100%" filterable>
            <el-option
              v-for="item in ${field.relatedTableCamelCaseName}List"
              :key="item.id"
              :label="item.${field.relatedTableFieldName}"
              :value="item.${field.relatedTableFieldName}"
            />
          </el-select>
          <#elseif field.field.formComponent == "input">
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          <#elseif field.field.formComponent == "select">
          <el-select v-model="form.${field.camelCaseName}" placeholder="请选择" style="width: 100%">
            <#if (field.validationRules?? && field.validationRules.hasOptions!false)>
              <#if field.validationRules.options?is_sequence>
                <#list field.validationRules.options as option>
                  <#if option?is_string>
            <el-option label="${option}" value="${option}" />
                  <#else>
            <el-option label="${option.label!option.value}" value="${option.value!option}" />
                  </#if>
                </#list>
              </#if>
            </#if>
          </el-select>
          <#elseif field.field.formComponent == "datepicker" || field.field.formComponent == "date">
          <el-date-picker v-model="form.${field.camelCaseName}" type="date" placeholder="请选择日期" style="width: 100%" />
          <#elseif field.field.formComponent == "number">
          <el-input-number v-model="form.${field.camelCaseName}" style="width: 100%" />
          <#elseif field.field.formComponent == "textarea">
          <el-input v-model="form.${field.camelCaseName}" type="textarea" :rows="4" />
          <#else>
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          </#if>
        </el-form-item>
        </el-col>
        </#if>
</#list>
        </el-row>
        <div class="form-actions">
          <el-button type="primary" @click="handleSubmit">保存</el-button>
          <el-button @click="goBack">取消</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>'''

new_tpl = new_tpl.replace("<motion>", "<div>").replace("</motion>", "</div>")
# fix inner div
new_tpl = new_tpl.replace(
    """        <motion>
          <h2 class="page-title">${table.tableName}</h2>
          <span class="page-desc">{{ isEdit ? '编辑' : '新增' }}记录</span>
        </div>""",
    """        <div>
          <h2 class="page-title">${table.tableName}</h2>
          <span class="page-desc">{{ isEdit ? '编辑' : '新增' }}记录</span>
        </div>""",
)

s = s.replace(old_tpl, new_tpl)

if "import { ArrowLeft }" not in s:
    s = s.replace(
        "import { ref, reactive, onMounted } from 'vue'",
        "import { ref, reactive, onMounted, computed } from 'vue'",
    )
    s = s.replace(
        "import { useRoute } from 'vue-router'",
        "import { useRoute, useRouter } from 'vue-router'\nimport { ArrowLeft } from '@element-plus/icons-vue'",
    )

if "const router = useRouter()" not in s:
    s = s.replace(
        "    const route = useRoute()\n    const formRef = ref(null)",
        """    const route = useRoute()
    const router = useRouter()
    const formRef = ref(null)
    const isEdit = computed(() => !!form.${primaryKeyCamelCase})
    const goBack = () => router.back()""",
    )

path.write_text(s, encoding="utf-8")
print("ok")
