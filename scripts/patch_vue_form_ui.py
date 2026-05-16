# -*- coding: utf-8 -*-
from pathlib import Path

path = Path(r"d:\Java\meta-management\src\main\resources\templates\vue_form.vue.ftl")
s = path.read_text(encoding="utf-8")

start = s.index('  <div class="${componentName?lower_case}-form">')
end = s.index('<el-form :model="form"', start)
header_new = """  <div class="admin-page ${componentName?lower_case}-form">
    <div class="page-header">
      <div class="page-header-left">
        <el-button link type="primary" class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div>
          <h2 class="page-title">${table.tableName}</h2>
          <span class="page-desc">{{ isEdit ? '编辑' : '新增' }}记录</span>
        </div>
      </motion>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form :model="form" """
header_new = header_new.replace("</motion>", "</div>").replace("    </motion>", "    </motion>").replace("<motion", "<motion")
header_new = header_new.replace("    </motion>\n    </motion>", "    </div>\n    </div>")
# fix: only one closing
header_new = """  <motion class="admin-page ${componentName?lower_case}-form">
    <div class="page-header">
      <div class="page-header-left">
        <el-button link type="primary" class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div>
          <h2 class="page-title">${table.tableName}</h2>
          <span class="page-desc">{{ isEdit ? '编辑' : '新增' }}记录</span>
        </div>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form :model="form" """
header_new = header_new.replace("<motion class=", "<motion class=").replace("admin-page", "admin-page")
header_new = header_new.replace('<motion class="admin-page', '<div class="admin-page')

s = s[:start] + header_new + s[end + len('<el-form :model="form" '):]

# change label-width and add row/col for fields - complex in ftl
s = s.replace(
    ':rules="rules" ref="formRef" label-width="100px">',
    ':rules="rules" ref="formRef" label-width="120px" label-position="right">',
)

# wrap form items in el-row - insert after form open tag line
s = s.replace(
    'label-position="right">\n<#list fields as field>',
    'label-position="right">\n        <el-row :gutter="24">\n<#list fields as field>',
)

# before form actions el-form-item, close row
s = s.replace(
    """</#list>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>""",
    """</#list>
        </el-row>
        <div class="form-actions">
          <el-button type="primary" @click="handleSubmit">保存</el-button>
          <el-button @click="goBack">取消</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </el-card>""",
)

# wrap each form item in el-col - patch in list loop
import re
def wrap_field_item(m):
    inner = m.group(0)
    comp = 'textarea' if 'textarea' in inner else 'other'
    span = '24' if 'type="textarea"' in inner or "type='textarea'" in inner else '12'
    return f'        <el-col :xs="24" :sm="24" :md="{span}" :lg="{span}">\n{inner}        </el-col>\n'

pattern = r'        <#if field\.field\.formComponent != "primary_key">[\s\S]*?        </#if>\n'
# simpler: add el-col in template manually via replace on form-item line
s = s.replace(
    '        <#if field.field.formComponent != "primary_key">\n        <el-form-item',
    '        <#if field.field.formComponent != "primary_key">\n        <el-col :xs="24" :sm="24" :md="${(field.field.formComponent == \'textarea\')?then(24, 12)}" :lg="${(field.field.formComponent == \'textarea\')?then(24, 12)}">\n        <el-form-item',
)
s = s.replace(
    '        </el-form-item>\n        </#if>',
    '        </el-form-item>\n        </el-col>\n        </#if>',
)

# remove old card header inside form
s = s.replace("""    <el-card>
      <template #header>
        <div class="card-header">
          <span>${table.tableName}表单</span>
        </div>
      </template>

      <el-form""", "<el-form", 1)  # may already removed

s = s.replace(
    "import { ref, reactive, onMounted } from 'vue'\nimport { ElMessage } from 'element-plus'\nimport { useRoute } from 'vue-router'",
    "import { ref, reactive, onMounted, computed } from 'vue'\nimport { ElMessage } from 'element-plus'\nimport { useRoute, useRouter } from 'vue-router'\nimport { ArrowLeft } from '@element-plus/icons-vue'",
)

# add goBack and isEdit in setup - insert after const route = useRoute()
if 'const goBack' not in s:
    s = s.replace(
        '    const route = useRoute()\n    const formRef = ref(null)',
        """    const route = useRoute()
    const router = useRouter()
    const formRef = ref(null)
    const isEdit = computed(() => !!form.${primaryKeyCamelCase})
    const goBack = () => router.back()""",
    )

s = s.replace(
    """    return {
      formRef,
      form,
      rules,
      handleSubmit,
      handleReset""",
    """    return {
      formRef,
      form,
      rules,
      isEdit,
      goBack,
      handleSubmit,
      handleReset""",
)

s = s.replace(
    """<style scoped>
.${componentName?lower_case}-form {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>""",
    """<style scoped>
.page-header-left { display: flex; align-items: center; gap: 12px; }
.back-btn { padding-left: 0; }
</style>""",
)

path.write_text(s, encoding="utf-8")
print('patched vue_form')
