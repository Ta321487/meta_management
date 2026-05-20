<template>
  <div class="field-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字段管理</span>
          <div>
            <el-select v-model="selectedBusinessCode" placeholder="请选择业务系统"
                       style="width: 200px; margin-right: 10px">
              <el-option
                  v-for="system in businessSystems"
                  :key="system.businessCode"
                  :label="system.businessName"
                  :value="system.businessCode"
              />
            </el-select>
            <el-select v-model="selectedTableCode" placeholder="请选择表" style="width: 200px; margin-right: 10px"
                       @change="handleTableChange">
              <el-option
                  v-for="table in tables"
                  :key="table.tableCode"
                  :label="table.tableName"
                  :value="table.tableCode"
              />
            </el-select>
            <el-button type="danger" @click="handleBatchDelete"
                       :disabled="!selectedRows || selectedRows.length === 0 || !selectedTableCode">批量删除
            </el-button>
            <el-button
                :type="batchEnableToggle.type"
                @click="handleBatchToggleEnable(batchEnableToggle.targetStatus)"
                :disabled="!selectedTableCode || batchEnableToggle.disabled"
            >
              {{ batchEnableToggle.label }}
            </el-button>
            <el-button
                type="warning"
                plain
                @click="openBatchMigrateDialog"
                :disabled="!selectedTableCode || batchMigratableCount === 0"
            >
              批量迁移
            </el-button>
            <el-button type="primary" @click="handleAdd" :disabled="!selectedTableCode">新增字段</el-button>
            <el-button type="success" @click="openCommonFieldDialog" :disabled="!selectedTableCode">常用字段</el-button>
            <el-button
              type="warning"
              :loading="syncMissingFieldsSubmitting"
              :disabled="!selectedTableCode"
              @click="handleSyncMissingFieldsFromPhysical"
            >
              同步缺失字段
            </el-button>
            <el-button type="info" @click="handleViewConstraints" :disabled="!selectedTableCode">查看约束</el-button>
          </div>
        </div>
      </template>

      <el-table :data="fieldData" border style="width: 100%" v-loading="loading" ref="tableRef"
                @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55"/>
        <el-table-column prop="fieldCode" label="字段编码" width="150"/>
        <el-table-column prop="fieldName" label="字段名称"/>
        <el-table-column prop="businessCode" label="业务系统" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.businessCode || '未关联' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldType" label="字段类型" width="150"/>
        <el-table-column prop="label" label="显示名" width="120"/>
        <el-table-column prop="isRequired" label="必填" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRequired === 1 ? 'success' : 'info'">
              {{ row.isRequired === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="formComponent" label="表单组件" width="120"/>
        <el-table-column label="选项" min-width="200">
          <template #default="{ row }">
            <span v-if="!parseFieldOptionItems(row).length" class="field-options-empty">—</span>
            <div v-else class="field-options-cell">
              <el-tag
                v-for="(opt, idx) in parseFieldOptionItems(row)"
                :key="`${opt.value}-${idx}`"
                size="small"
                :type="optionTagType(opt, idx)"
                effect="light"
                class="field-opt-tag"
              >
                <span
                  v-if="showOptionCode(opt)"
                  class="field-opt-code"
                >{{ opt.value }}</span>
                <span class="field-opt-label">{{ opt.label }}</span>
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="inForm" label="表单录入" width="100">
          <template #default="{ row }">
            <el-tag :type="row.inForm === 0 ? 'info' : 'success'">
              {{ row.inForm === 0 ? '否' : '是' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80"/>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-space wrap>
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                  type="warning"
                  size="small"
                  plain
                  :disabled="isPrimaryKey(row)"
                  @click="openMigrateDialog(row)"
              >
                迁移
              </el-button>
              <el-button
                  :type="row.isEnabled === 1 ? 'warning' : 'success'"
                  size="small"
                  @click="handleToggleEnable(row)"
                  :disabled="isPrimaryKey(row)"
              >
                {{ row.isEnabled === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total || 0"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600px"
        :before-close="formGuard.handleBeforeClose"
        @close="handleDialogClose"
    >
      <el-form :model="form" :rules="fieldRules" ref="formRef" label-width="100px">
        <el-form-item label="字段编码" prop="fieldCode" v-if="!form.id">
          <el-input
            v-model="form.fieldCode"
            placeholder="如：FIELD_001（只能包含字母、数字和下划线）"
            @blur="applyIdentifierBlur(form, 'fieldCode', 'code')"
          />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input
            v-model="form.fieldName"
            :disabled="isEditingPrimaryKey"
            :placeholder="isEditingPrimaryKey ? '物理列名已随建表固定，不可在此修改' : '请输入字段名称（对应业务库列名）'"
            @blur="applyIdentifierBlur(form, 'fieldName', 'column')"
          />
        </el-form-item>
        <el-alert
          v-if="isEditingPrimaryKey"
          type="warning"
          :closable="false"
          show-icon
          class="pk-field-alert"
          title="主键列名已固定"
          description="该字段为新增表/补缺物理表时自动生成的主键，保存时仅更新显示名等元数据，不会 ALTER 改物理列名。若要改用其它列名作主键，请删表后重建并事先规划字段名称。"
        />
        <el-form-item label="字段类型" prop="baseFieldType">
          <el-select
            v-model="form.baseFieldType"
            placeholder="请选择基础字段类型"
            style="width: 100%"
            :disabled="isEditingPrimaryKey"
          >
            <el-option
                v-for="type in baseFieldTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
            />
          </el-select>
        </el-form-item>

        <!-- 长度输入框（用于VARCHAR, CHAR等） -->
        <el-form-item
            v-if="form.baseFieldType === 'VARCHAR' || form.baseFieldType === 'CHAR'"
            label="长度"
        >
          <el-input-number
              v-model="typeParams.length"
              :min="1"
              :max="form.baseFieldType === 'CHAR' ? 255 : 65535"
              style="width: 100%"
              placeholder="请输入长度"
          />
        </el-form-item>

        <!-- 精度和小数位数输入框（用于DECIMAL, NUMERIC等） -->
        <el-form-item
            v-if="form.baseFieldType === 'DECIMAL' || form.baseFieldType === 'NUMERIC'"
            label="精度和小数位数"
            class="precision-scale-form-item"
        >
          <div class="precision-scale-inputs">
            <el-input-number
                v-model="typeParams.precision"
                :min="1"
                :max="65"
                style="width: 120px; margin-right: 10px"
                placeholder="精度"
            />
            <span style="margin-right: 10px">,</span>
            <el-input-number
                v-model="typeParams.scale"
                :min="0"
                :max="Math.min(typeParams.precision, 30)"
                style="width: 120px"
                placeholder="小数位数"
            />
          </div>
        </el-form-item>

        <!-- 状态值（TINYINT/INT 状态列，同步到校验规则 IN + options） -->
        <el-form-item v-if="isStatusFieldContext" label="状态值">
          <div style="display: flex; gap: 10px; width: 100%;">
            <el-input
                v-model="typeParams.statusValues"
                style="flex: 1"
                placeholder="格式：0:草稿,1:生效,2:作废（仅数字可写 0,1,2；支持中文逗号）"
            />
            <el-button type="primary" @click="syncStatusToValidateRule">刷新</el-button>
          </div>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            填写存库值与展示名，点「刷新」写入下方校验规则；改 JSON 后可点校验区「刷新到状态值」
          </div>
        </el-form-item>

        <!-- 枚举值输入框（用于ENUM类型） -->
        <el-form-item
            v-if="form.baseFieldType === 'ENUM'"
            label="枚举值"
        >
          <div style="display: flex; gap: 10px; width: 100%;">
            <el-input
                v-model="typeParams.enumValues"
                style="flex: 1"
                placeholder="请输入逗号分隔的枚举值，如：value1,value2,value3（支持中文逗号，会自动转换）"
            />
            <el-button type="primary" @click="syncEnumToValidateRule">刷新</el-button>
          </div>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            提示：填写枚举值后点击"刷新"按钮，会自动同步到校验规则中
          </div>
        </el-form-item>
        <el-form-item label="显示名" prop="label">
          <el-input v-model="form.label" placeholder="请输入显示名"/>
        </el-form-item>
        <el-form-item
            v-if="form.formComponent !== 'primary_key'"
            label="参与表单录入"
        >
          <el-switch v-model="form.inForm" :active-value="1" :inactive-value="0"/>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            关闭后不在新增/编辑页展示该字段（如业务编码由系统生成）；列表仍可展示。
          </div>
        </el-form-item>
        <el-form-item label="是否必填" prop="isRequired">
          <el-radio-group v-model="form.isRequired">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
          <div style="margin-top: 5px; font-size: 12px; color: #909399;">
            提示：选择"是"将在数据库层面添加NOT NULL约束
          </div>
        </el-form-item>
        <el-form-item
            v-if="form.inForm === 1 && form.formComponent !== 'primary_key'"
            label="表单组件"
            prop="formComponent"
        >
          <el-select v-model="form.formComponent" placeholder="请选择" style="width: 100%">
            <el-option label="主键字段" value="primary_key"/>
            <el-option label="输入框" value="input"/>
            <el-option label="下拉框" value="select"/>
            <el-option label="日期选择器" value="datepicker"/>
            <el-option label="数字输入框" value="number"/>
            <el-option label="文本域" value="textarea"/>
          </el-select>
          <div
            v-if="isDiscreteStatusFieldName(form.fieldName) && form.formComponent === 'number'"
            style="margin-top: 6px; font-size: 12px; color: #e6a23c;"
          >
            状态类字段建议用「下拉框」，并在校验规则中配置 options 与 IN（勿仅用数字框填 0/1/2）。
          </div>
          <div
            v-else-if="isDiscreteStatusFieldName(form.fieldName) && form.formComponent === 'select'"
            style="margin-top: 6px; font-size: 12px; color: #909399;"
          >
            库类型建议 TINYINT；展示文案在上方「状态值」填写（如 0:草稿,1:生效）。
          </div>
        </el-form-item>
        <el-form-item label="业务系统" prop="businessCode">
          <el-select v-model="form.businessCode" placeholder="请选择业务系统" style="width: 100%">
            <el-option
                v-for="system in businessSystems"
                :key="system.businessCode"
                :label="system.businessName"
                :value="system.businessCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="校验规则" prop="validateRule">
          <div
            v-if="isStatusFieldContext"
            style="display: flex; gap: 10px; margin-bottom: 5px;"
          >
            <el-button size="small" type="primary" @click="syncValidateRuleToStatus">刷新到状态值</el-button>
            <div style="font-size: 12px; color: #909399; line-height: 32px;">
              修改 JSON 后点此同步到上方「状态值」；也可在编辑器「查看示例」选状态模板
            </div>
          </div>
          <div style="display: flex; gap: 10px; margin-bottom: 5px;" v-if="form.baseFieldType === 'ENUM'">
            <el-button size="small" type="primary" @click="syncValidateRuleToEnum">刷新到枚举值</el-button>
            <div style="font-size: 12px; color: #909399; line-height: 32px;">
              提示：修改校验规则后点击"刷新到枚举值"可同步到枚举值文本框
            </div>
          </div>
          <json-editor
              v-model="form.validateRule"
              min-height="100px"
              max-height="300px"
              :options="{
              maxLines: 15,
              minLines: 5
            }"
              @example-applied="onValidateRuleExampleApplied"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formGuard.requestCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 常用字段一键添加 -->
    <el-dialog
        v-model="commonFieldDialogVisible"
        title="常用字段"
        width="640px"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        @closed="handleCommonFieldDialogClosed"
    >
      <p class="common-field-intro">
        为当前表 <strong>{{ selectedTableCode }}</strong> 批量添加审计/软删列；字段编码按表名自动生成（如
        <code>{{ commonFieldCodeExample }}</code>）。<strong>已登记</strong>的跳过；
        业务库<strong>已有同名列</strong>时只补元数据，不再执行 ADD COLUMN。
      </p>
      <div class="common-field-toolbar">
        <el-button size="small" link type="primary" @click="selectAllAddableCommonFields">全选可添加</el-button>
        <el-button size="small" link @click="clearCommonFieldSelection">清空</el-button>
      </div>
      <el-table
          :data="commonPresetRows"
          border
          size="small"
          max-height="320"
          @selection-change="handleCommonPresetSelectionChange"
          ref="commonPresetTableRef"
      >
        <el-table-column type="selection" width="48" :selectable="(row) => row.selectable"/>
        <el-table-column prop="label" label="显示名" width="88"/>
        <el-table-column prop="fieldName" label="物理列" width="110"/>
        <el-table-column prop="fieldCode" label="字段编码" min-width="120" show-overflow-tooltip/>
        <el-table-column prop="fieldType" label="类型" width="100"/>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="commonPresetStatusTagType(row)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="commonFieldDialogVisible = false">取消</el-button>
        <el-button
            type="primary"
            :loading="commonFieldSubmitting"
            :disabled="addableCommonFieldCount === 0"
            @click="handleCommonFieldSubmit"
        >
          添加选中（{{ addableCommonFieldCount }}）
        </el-button>
      </template>
    </el-dialog>

    <!-- 迁移到其他表 -->
    <el-dialog
        v-model="migrateDialogVisible"
        :title="migrateBatchMode ? '批量迁移字段到其他表' : '迁移字段到其他表'"
        width="620px"
        :close-on-click-modal="false"
        destroy-on-close
        @closed="onMigrateDialogClosed"
    >
      <el-alert
        v-if="!migrateBatchMode && migrateSourceRow"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        将「{{ migrateSourceRow.label || migrateSourceRow.fieldName }}」（{{ migrateSourceRow.fieldCode }} /
        {{ migrateSourceRow.fieldName }}）从表「{{ selectedTableCode }}」迁到目标表：登记元数据，缺列时 ADD COLUMN。
      </el-alert>
      <el-alert
        v-else-if="migrateBatchMode && migrateSourceRows.length"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      >
        <div>将以下 <strong>{{ migrateSourceRows.length }}</strong> 个字段从表「{{ selectedTableCode }}」迁到同一目标表（已跳过主键）：</div>
        <div class="migrate-batch-tags">
          <el-tag
            v-for="row in migrateSourceRows"
            :key="row.id"
            size="small"
            type="info"
            effect="light"
            class="migrate-batch-tag"
          >
            {{ row.label || row.fieldName }}（{{ row.fieldCode }}）
          </el-tag>
        </div>
      </el-alert>
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
        <div class="migrate-hints-title">操作前请知悉</div>
        <ul class="migrate-hints-list">
          <li>主键字段不可迁移；目标表不能已有相同<strong>字段编码</strong>或<strong>物理列名</strong>。</li>
          <li>「迁移物理数据」仅当源表与目标表在<strong>同一物理库</strong>时可用；跨库请关闭此项，自行在库工具中搬数据。</li>
          <li>下方「行对齐键」是<strong>主键列</strong>（多为 <code>id</code>），用于判断两表哪一行对应哪一行；<strong>被迁移的是你选中的业务列</strong>（如 <code>status</code>），不是 <code>id</code>。</li>
          <li>关闭「迁移数据」时，目标表新列仅为空或默认值，不会从源表拷贝。</li>
          <li>关闭「删除源表字段」时，源表仍保留元数据与物理列，需自行再删。</li>
          <li>迁移后请检查<strong>表关联</strong>、<strong>业务规则</strong>是否仍指向源表字段；预览/ZIP 请在对表重新生成。</li>
          <li v-if="!migrateBatchMode">单字段迁移：任一步失败将整体回滚，不会只改一半。</li>
          <li v-else>批量迁移：按字段逐条执行；已成功的不回滚，失败项仍留在源表。</li>
        </ul>
      </el-alert>
      <el-form label-width="120px">
        <el-form-item label="目标表" required>
          <el-select
            v-model="migrateForm.targetTableCode"
            placeholder="请选择目标表"
            filterable
            style="width: 100%"
            @change="loadMigrateTargetJoinFields"
          >
            <el-option
              v-for="t in migrateTargetTableOptions"
              :key="t.tableCode"
              :label="`${t.tableName}（${t.tableCode}）`"
              :value="t.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="迁移物理数据">
          <el-switch v-model="migrateForm.migrateData" />
          <span v-if="migrateForm.migrateData" style="margin-left: 8px; font-size: 12px; color: #909399;">
            执行 UPDATE 目标表 JOIN 源表
          </span>
          <span v-else style="margin-left: 8px; font-size: 12px; color: #e6a23c;">
            不拷贝数据，仅元数据 + ADD COLUMN
          </span>
        </el-form-item>
        <template v-if="migrateForm.migrateData && !migrateBatchMode && migrateSourceRow">
          <div class="migrate-join-hint">
            被迁移列：<strong>{{ migrateSourceRow.fieldName }}</strong>（{{ migrateSourceRow.label }}）。
            下方填写的是<strong>主键对齐键</strong>，用于 JOIN 找「同一行」，不是要迁移的列。
          </div>
          <p v-if="migrateJoinExample" class="migrate-join-sql">{{ migrateJoinExample }}</p>
        </template>
        <div v-else-if="migrateForm.migrateData && migrateBatchMode" class="migrate-join-hint">
          批量模式下各字段分别按列名拷贝；下方为主键对齐键（JOIN 用），不是要迁移的列。
        </div>
        <el-form-item v-if="migrateForm.migrateData" label="源表行对齐键">
          <el-input :model-value="migrateForm.joinSourceField" readonly>
            <template #append>主键</template>
          </el-input>
          <div class="migrate-field-tip">自动取当前源表主键列，不可改</div>
        </el-form-item>
        <el-form-item v-if="migrateForm.migrateData" label="目标表行对齐键">
          <el-select
            v-model="migrateForm.joinTargetField"
            placeholder="请先选择目标表"
            :disabled="!migrateForm.targetTableCode"
            :loading="migrateTargetFieldsLoading"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="opt in migrateTargetJoinOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <div class="migrate-field-tip">默认目标表主键；与源表主键列名相同时会优先选中</div>
        </el-form-item>
        <el-form-item label="删除源表字段">
          <el-switch v-model="migrateForm.removeFromSource" />
          <span v-if="migrateForm.removeFromSource" style="margin-left: 8px; font-size: 12px; color: #909399;">
            成功后 DROP COLUMN 并删源表元数据
          </span>
          <span v-else style="margin-left: 8px; font-size: 12px; color: #e6a23c;">
            源表字段仍保留
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="migrateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="migrateSubmitting" @click="handleMigrateSubmit">
          {{ migrateBatchMode ? '开始批量迁移' : '开始迁移' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 约束列表对话框 -->
    <el-dialog
        v-model="constraintDialogVisible"
        title="约束列表"
        width="1000px"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        @open="loadConstraints"
    >
      <el-table :data="constraints" border style="width: 100%" v-loading="constraintLoading">
        <el-table-column prop="constraintName" label="约束名" width="200"/>
        <el-table-column prop="constraintContent" label="约束内容" width="400"/>
        <el-table-column prop="constraintType" label="约束类型" width="150"/>
        <el-table-column prop="fieldName" label="作用列名" width="150"/>
        <el-table-column prop="constraintLevel" label="约束级别" width="150"/>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button type="danger" size="small" @click="handleDeleteConstraint(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="constraintDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {computed, nextTick, onActivated, onMounted, reactive, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  addField,
  batchDeleteField,
  batchUpdateFieldStatus,
  deleteConstraint,
  deleteField,
  getBusinessSystemList,
  getConstraintList,
  getFieldList,
  getPhysicalColumnNames,
  getTableList,
  migrateFieldBatchToTable,
  migrateFieldToTable,
  syncMissingFieldsFromPhysical,
  updateField
} from '../api'
import JsonEditor from '../components/JsonEditor'
import {
  buildCommonFieldPresetRows,
  buildPresetFieldCode,
  toAddFieldPayload
} from '../constants/commonFieldPresets'
import {
  applyIdentifierBlur,
  metadataCodeRules,
  normalizeFormCodes,
  physicalColumnRules
} from '../utils/identifierInput'
import { useDialogFormGuard } from '../composables/useUnsavedFormGuard'
import {
  formatStatusEntriesFromValidateRule,
  isStatusIntegerFieldType,
  isStatusStyleValidateRule,
  mergeOptionsWithValues,
  parseStatusEntriesString,
  resolveFieldOptionItems
} from '../utils/fieldOptionUtils'
import { resolveBatchEnableToggle } from '../utils/batchEnableToggle'

export default {
  name: 'FieldManage',
  components: {
    JsonEditor
  },
  setup() {
    const tables = ref([])
    const fieldData = ref([])
    const businessSystems = ref([])
    const selectedBusinessCode = ref('')
    const selectedTableCode = ref('')
    const currentTableBusinessCode = ref('')
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增字段')
    const formRef = ref(null)
    const tableRef = ref(null)
    const selectedRows = ref([])
    const batchEnableToggle = computed(() => resolveBatchEnableToggle(selectedRows.value, 'isEnabled'))
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    // 约束相关
    const constraintDialogVisible = ref(false)
    const constraints = ref([])
    const constraintLoading = ref(false)
    const commonFieldDialogVisible = ref(false)
    const commonPresetRows = ref([])
    const commonFieldSelectedKeys = ref([])
    const commonFieldSubmitting = ref(false)
    const syncingPresetSelection = ref(false)
    const syncMissingFieldsSubmitting = ref(false)
    const migrateDialogVisible = ref(false)
    const migrateSubmitting = ref(false)
    const migrateBatchMode = ref(false)
    const migrateSourceRow = ref(null)
    const migrateSourceRows = ref([])
    const migrateForm = reactive({
      targetTableCode: '',
      migrateData: true,
      removeFromSource: true,
      joinSourceField: 'id',
      joinTargetField: ''
    })
    const migrateTargetFields = ref([])
    const migrateTargetFieldsLoading = ref(false)
    const commonPresetTableRef = ref(null)
    const allFieldsForTable = ref([])

    const migrateTargetTableOptions = computed(() =>
      tables.value.filter((t) => t.tableCode && t.tableCode !== selectedTableCode.value)
    )

    const batchMigratableRows = computed(() =>
      (selectedRows.value || []).filter((row) => !isPrimaryKey(row))
    )

    const batchMigratableCount = computed(() => batchMigratableRows.value.length)

    const resolveTablePrimaryKeyFieldName = (fields) => {
      const pk = (fields || []).find(
        (f) => f.formComponent === 'primary_key' || f.fieldName === 'id' || f.fieldName === 'uuid'
      )
      return pk?.fieldName || 'id'
    }

    const migrateJoinExample = computed(() => {
      const col = migrateSourceRow.value?.fieldName
      if (!col || !migrateForm.migrateData) return ''
      const srcKey = (migrateForm.joinSourceField || 'id').trim() || 'id'
      const tgtKey = (migrateForm.joinTargetField || '').trim() || srcKey
      return `UPDATE 目标表 t JOIN 源表 s ON t.${tgtKey} = s.${srcKey} SET t.${col} = s.${col}`
    })

    const buildJoinKeyOptions = (fields, sourcePkName) => {
      const pkName = resolveTablePrimaryKeyFieldName(fields)
      const names = [...new Set((fields || []).map((f) => f.fieldName).filter(Boolean))]
      const sorted = names.sort((a, b) => {
        if (a === pkName) return -1
        if (b === pkName) return 1
        if (a === sourcePkName) return -1
        if (b === sourcePkName) return 1
        return a.localeCompare(b)
      })
      return sorted.map((name) => {
        let suffix = ''
        if (name === pkName) suffix = '（主键，推荐）'
        else if (name === sourcePkName && name !== pkName) suffix = '（与源表主键同名）'
        return { value: name, label: `${name}${suffix}` }
      })
    }

    const migrateTargetJoinOptions = computed(() =>
      buildJoinKeyOptions(migrateTargetFields.value, migrateForm.joinSourceField)
    )

    const pickDefaultTargetJoinField = (fields, sourcePkName) => {
      const pkName = resolveTablePrimaryKeyFieldName(fields)
      const names = (fields || []).map((f) => f.fieldName).filter(Boolean)
      if (names.includes(sourcePkName)) return sourcePkName
      return pkName
    }

    const loadMigrateTargetJoinFields = async (tableCode) => {
      if (!tableCode) {
        migrateTargetFields.value = []
        migrateForm.joinTargetField = ''
        return
      }
      const targetTable = tables.value.find((t) => t.tableCode === tableCode)
      const bc = targetTable?.businessCode || currentTableBusinessCode.value || ''
      migrateTargetFieldsLoading.value = true
      try {
        const res = await getFieldList(tableCode, {
          businessCode: bc,
          includeDisabled: true,
          current: 1,
          size: 500
        })
        let fields = []
        if (res.code === 200 && res.data) {
          if (Array.isArray(res.data)) {
            fields = res.data
          } else if (Array.isArray(res.data.records)) {
            fields = res.data.records
          }
        }
        migrateTargetFields.value = fields
        migrateForm.joinTargetField = pickDefaultTargetJoinField(fields, migrateForm.joinSourceField)
      } catch {
        migrateTargetFields.value = []
        migrateForm.joinTargetField = migrateForm.joinSourceField
      } finally {
        migrateTargetFieldsLoading.value = false
      }
    }

    const commonFieldCodeExample = computed(() =>
      buildPresetFieldCode(selectedTableCode.value || 'mdm_customer', 'F_CT')
    )

    const addableCommonFieldCount = computed(() =>
      commonPresetRows.value.filter((r) => r.selectable && commonFieldSelectedKeys.value.includes(r.key)).length
    )

    const commonPresetStatusTagType = (row) => {
      if (row.metadataExists) return 'info'
      if (row.physicalExists) return 'warning'
      return 'success'
    }
    const form = reactive({
      id: null,
      fieldCode: '',
      tableCode: '',
      fieldName: '',
      fieldType: '',
      baseFieldType: '',
      label: '',
      isRequired: 0,
      formComponent: 'input',
      validateRule: '',
      sort: 0,
      isEnabled: 1,
      inForm: 1,
      businessCode: ''
    })

    const isEditingPrimaryKey = computed(() => {
      if (!form.id) return false
      return form.formComponent === 'primary_key' || form.fieldName === 'id' || form.fieldName === 'uuid'
    })

    const isStatusFieldContext = computed(() => {
      if (!isStatusIntegerFieldType(form.baseFieldType)) {
        return false
      }
      return (
        isDiscreteStatusFieldName(form.fieldName) ||
        form.formComponent === 'select' ||
        isStatusStyleValidateRule(form.validateRule)
      )
    })

    // 监听字段名称变化，当字段名为'id'或'uuid'时自动设置排序号为0和表单组件为primary_key
    // 当字段名为create_time或update_time时自动设置合适的属性
    watch(() => form.fieldName, (newValue) => {
      if (newValue === 'id' || newValue === 'uuid') {
        form.sort = 0
        form.formComponent = 'primary_key'
      } else if (newValue === 'create_time' || newValue === 'update_time') {
        // 时间字段自动设置为DATETIME类型和datetime表单组件
        form.baseFieldType = 'DATETIME'
        form.fieldType = 'DATETIME'
        form.formComponent = 'datepicker'
        // 时间字段由数据库自动生成，不需要用户输入
        form.isRequired = 0
      } else if (isDiscreteStatusFieldName(newValue)) {
        if (form.formComponent !== 'primary_key') {
          form.formComponent = 'select'
        }
        if (!form.baseFieldType || form.baseFieldType === 'VARCHAR' || form.baseFieldType === 'ENUM') {
          applyStatusFieldTypePreset(false)
        }
        // 校验规则文案由业务自行配置，不自动写入默认 options
      }
    })

    /** 状态列：TINYINT/INT + IN/options，不用 MySQL ENUM */
    const applyStatusFieldTypePreset = (showMessage = true) => {
      const wasEnum = form.baseFieldType === 'ENUM'
      if (wasEnum || !form.baseFieldType || form.baseFieldType === 'VARCHAR') {
        form.baseFieldType = 'TINYINT'
        typeParams.enumValues = ''
        if (!typeParams.statusValues) {
          typeParams.statusValues = '0:草稿,1:生效,2:作废'
        }
      }
      if (form.formComponent !== 'primary_key') {
        form.formComponent = 'select'
      }
      if (showMessage && wasEnum) {
        ElMessage.info('状态列已切换为 TINYINT：请用校验规则 IN/options，勿使用 MySQL ENUM 类型')
      }
    }

    const onValidateRuleExampleApplied = ({ key }) => {
      if (key === 'status_with_options') {
        applyStatusFieldTypePreset(true)
        typeParams.statusValues = '0:选项一,1:选项二,2:选项三'
        syncStatusToValidateRule(false)
      }
    }

    const buildValidateRuleFromStatusEntries = (statusStr, prevRuleStr) => {
      const { values, options } = parseStatusEntriesString(statusStr)
      if (!values.length) {
        return null
      }
      let prevRule = {}
      try {
        if (prevRuleStr?.trim() && prevRuleStr !== '{}') {
          prevRule = JSON.parse(prevRuleStr) || {}
        }
      } catch {
        prevRule = {}
      }
      const validateRuleObj = {
        operator: 'IN',
        values,
        options: mergeOptionsWithValues(values, options.length ? options : prevRule.options || []),
        message: prevRule.message || '请选择有效值',
        trigger: prevRule.trigger || 'change'
      }
      if ('value' in validateRuleObj) {
        delete validateRuleObj.value
      }
      return JSON.stringify(validateRuleObj, null, 2)
    }

    const syncStatusToValidateRule = (showMessage = true) => {
      if (!isStatusIntegerFieldType(form.baseFieldType)) {
        ElMessage.warning('状态值仅用于 TINYINT/INT/SMALLINT 类型字段')
        return
      }
      const next = buildValidateRuleFromStatusEntries(typeParams.statusValues, form.validateRule)
      if (!next) {
        ElMessage.warning('请先填写状态值，例如：0:草稿,1:生效,2:作废')
        return
      }
      form.validateRule = next
      if (showMessage) {
        ElMessage.success('状态值已同步到校验规则')
      }
    }

    const syncValidateRuleToStatus = () => {
      if (!isStatusIntegerFieldType(form.baseFieldType)) {
        ElMessage.warning('当前字段类型不支持状态值同步')
        return
      }
      const text = formatStatusEntriesFromValidateRule(form.validateRule)
      if (!text) {
        ElMessage.warning('校验规则中未找到 IN/values 或 options')
        return
      }
      typeParams.statusValues = text
      ElMessage.success('校验规则已同步到状态值')
    }
    // 基础字段类型列表
    const baseFieldTypes = ref([
      {label: 'INT', value: 'INT'},
      {label: 'BIGINT', value: 'BIGINT'},
      {label: 'TINYINT', value: 'TINYINT'},
      {label: 'VARCHAR', value: 'VARCHAR'},
      {label: 'CHAR', value: 'CHAR'},
      {label: 'TEXT', value: 'TEXT'},
      {label: 'LONGTEXT', value: 'LONGTEXT'},
      {label: 'DECIMAL', value: 'DECIMAL'},
      {label: 'NUMERIC', value: 'NUMERIC'},
      {label: 'ENUM', value: 'ENUM'},
      {label: 'DATE', value: 'DATE'},
      {label: 'DATETIME', value: 'DATETIME'},
      {label: 'TIMESTAMP', value: 'TIMESTAMP'},
      {label: 'BOOLEAN', value: 'BOOLEAN'}
    ])

    // 字段类型与表单组件的映射关系
    const fieldTypeToFormComponentMap = {
      // 数字类型
      'INT': 'number',
      'BIGINT': 'number',
      'TINYINT': 'number',
      'DECIMAL': 'number',
      'NUMERIC': 'number',
      // 日期时间类型
      'DATE': 'datepicker',
      'DATETIME': 'datepicker',
      'TIMESTAMP': 'datepicker',
      // 长文本类型
      'TEXT': 'textarea',
      'LONGTEXT': 'textarea',
      // 枚举类型
      'ENUM': 'select',
      // 布尔类型
      'BOOLEAN': 'select',
      // 默认类型
      'DEFAULT': 'input'
    }

    /** 离散状态类字段名：应用 select，不用 number（即使用 TINYINT 存 0/1/2） */
    const isDiscreteStatusFieldName = (fieldName) => {
      if (!fieldName) return false
      const n = String(fieldName).toLowerCase()
      if (n === 'status' || n === 'state' || n.endsWith('_status') || n.endsWith('_state')) return true
      if (n === 'enabled' || n === 'is_enabled' || n === 'is_enable') return true
      return false
    }

    // 根据字段类型（及字段名）获取合适的表单组件
    const getRecommendedFormComponent = (fieldType, fieldName = form.fieldName) => {
      const base = fieldTypeToFormComponentMap[fieldType] || fieldTypeToFormComponentMap['DEFAULT']
      if (
        isDiscreteStatusFieldName(fieldName) &&
        (fieldType === 'TINYINT' || fieldType === 'INT' || fieldType === 'SMALLINT')
      ) {
        return 'select'
      }
      return base
    }

    // 需要参数的字段类型
    const typesWithParams = ['VARCHAR', 'CHAR', 'DECIMAL', 'NUMERIC', 'ENUM']

    // 类型参数
    const typeParams = reactive({
      length: 50, // 用于VARCHAR, CHAR等
      precision: 10, // 用于DECIMAL, NUMERIC等
      scale: 2, // 用于DECIMAL, NUMERIC等
      enumValues: '', // 用于ENUM类型，默认空字符串
      statusValues: '' // 状态列：0:草稿,1:生效
    })

    // 解析字段类型，提取基础类型和参数
    const parseFieldType = (fullType) => {
      if (!fullType) return {baseType: '', length: 50, precision: 10, scale: 2, enumValues: ''}

      // 匹配VARCHAR(50)或CHAR(10)格式
      const varcharMatch = fullType.match(/^(VARCHAR|CHAR)\((\d+)\)$/i)
      if (varcharMatch) {
        return {
          baseType: varcharMatch[1].toUpperCase(),
          length: parseInt(varcharMatch[2]),
          precision: 10,
          scale: 2,
          enumValues: ''
        }
      }

      // 匹配DECIMAL(10,2)或NUMERIC(8,3)格式
      const decimalMatch = fullType.match(/^(DECIMAL|NUMERIC)\((\d+),(\d+)\)$/i)
      if (decimalMatch) {
        return {
          baseType: decimalMatch[1].toUpperCase(),
          length: 50,
          precision: parseInt(decimalMatch[2]),
          scale: parseInt(decimalMatch[3]),
          enumValues: ''
        }
      }

      // 匹配ENUM('value1','value2')格式
      const enumMatch = fullType.match(/^ENUM\((.*)\)$/i)
      if (enumMatch) {
        // 提取枚举值，去除引号并转换为逗号分隔的字符串
        const enumValues = enumMatch[1]
            .split(',')
            .map(val => val.trim().replace(/^['"]|['"]$/g, ''))
            .join(',')
        return {
          baseType: 'ENUM',
          length: 50,
          precision: 10,
          scale: 2,
          enumValues: enumValues
        }
      }

      // 其他类型直接返回
      return {
        baseType: fullType.toUpperCase(),
        length: 50,
        precision: 10,
        scale: 2,
        enumValues: ''
      }
    }

    // 计算完整字段类型
    const computedFieldType = computed(() => {
      const baseType = form.baseFieldType
      if (!baseType) return ''

      if (baseType === 'VARCHAR' || baseType === 'CHAR') {
        // 使用有效的长度值，默认50
        const length = typeParams.length || 50
        return `${baseType}(${length})`
      }

      if (baseType === 'DECIMAL' || baseType === 'NUMERIC') {
        // 使用有效的精度和小数位值，默认10,2
        const precision = typeParams.precision || 10
        const scale = typeParams.scale || 2
        return `${baseType}(${precision},${scale})`
      }

      if (baseType === 'ENUM') {
        // 将逗号分隔的枚举值转换为带引号的格式，如'value1','value2','value3'
        const enumValues = typeParams.enumValues
            .split(',')
            .map(val => `'${val.trim()}'`)
            .join(',')
        return `${baseType}(${enumValues})`
      }

      return baseType
    })

    // 监听计算字段类型变化，更新表单字段类型
    watch(computedFieldType, (newValue) => {
      form.fieldType = newValue
    })

    // 监听表单字段类型变化（用于编辑场景）
    watch(() => form.fieldType, (newValue) => {
      if (newValue) {
        const {baseType, length, precision, scale} = parseFieldType(newValue)
        // 保存当前校验规则，避免被baseFieldType的watch清空
        const savedRule = form.validateRule || ''
        form.baseFieldType = baseType
        typeParams.length = length
        typeParams.precision = precision
        typeParams.scale = scale

        // 恢复保存的校验规则
        form.validateRule = savedRule

        // 如果不是主键字段，根据字段类型自动更新表单组件
        if (form.formComponent !== 'primary_key' && form.fieldName !== 'id' && form.fieldName !== 'uuid') {
          form.formComponent = getRecommendedFormComponent(baseType, form.fieldName)
        }
      }
    })

    // 监听基础字段类型变化（用于新增和编辑场景）
    watch(() => form.baseFieldType, (newValue, oldValue) => {
      if (newValue) {
        // 如果不是主键字段，根据字段类型自动更新表单组件
        if (form.formComponent !== 'primary_key' && form.fieldName !== 'id' && form.fieldName !== 'uuid') {
          form.formComponent = getRecommendedFormComponent(newValue, form.fieldName)
        }

        // 当字段类型切换时，清空校验规则
        // 但是，在以下情况下不要清空：
        // 1. oldValue 为空字符串（初始化阶段）
        // 2. 编辑模式下有校验规则且新类型是ENUM
        // 3. 编辑模式下有校验规则且是从空字符串切换过来（handleEdit场景）
        if (oldValue && oldValue !== '' && oldValue !== newValue) {
          // 如果是编辑模式且有校验规则，且新类型是ENUM，保留校验规则
          if (form.id && form.validateRule && form.validateRule.trim() !== '' && form.validateRule !== '{}') {
            // 编辑模式下有校验规则，保留规则，不清空
          } else {
            // 其他情况清空校验规则
            form.validateRule = '{}'
          }
        }
      }
    })

    // 监听表切换，自动刷新约束列表
    watch(selectedTableCode, (newValue, oldValue) => {
      if (newValue && newValue !== oldValue && constraintDialogVisible.value) {
        loadConstraints()
      }
    })

    watch(() => form.inForm, (v) => {
      if (form.formComponent === 'primary_key') {
        return
      }
      if (v === 0) {
        form.formComponent = 'none'
      } else if (v === 1 && form.formComponent === 'none') {
        form.formComponent = getRecommendedFormComponent(form.baseFieldType || 'VARCHAR', form.fieldName)
      }
    })

    // 监听业务系统变化，重新加载表列表
    watch(selectedBusinessCode, () => {
      loadTables()
    })

    const formGuard = useDialogFormGuard(form, dialogVisible, {
      onReset: () => formRef.value?.resetFields()
    })

    const fieldRules = computed(() => ({
      fieldCode: metadataCodeRules('字段编码'),
      fieldName: physicalColumnRules(),
      baseFieldType: [{required: true, message: '请选择基础字段类型', trigger: 'change'}],
      fieldType: [{required: true, message: '请选择字段类型', trigger: 'change'}],
      label: [{required: true, message: '请输入显示名', trigger: 'blur'}],
      formComponent:
          form.formComponent === 'primary_key' || form.inForm === 0
              ? []
              : [{required: true, message: '请选择表单组件', trigger: 'change'}]
    }))

    // 加载业务系统列表
    const loadBusinessSystems = async () => {
      try {
        const res = await getBusinessSystemList({})
        if (res.code === 200) {
          businessSystems.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载业务系统列表失败')
      }
    }

    const loadTables = async () => {
      try {
        // 只有选择了业务系统，才加载表列表
        if (selectedBusinessCode.value) {
          const res = await getTableList({
            businessCode: selectedBusinessCode.value,
            includeDisabled: true
          })
          if (res.code === 200) {
            tables.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
          }
        } else {
          // 未选择业务系统时，清空表列表
          tables.value = []
        }
        // 清空当前选择的表，确保表列表与业务系统同步
        selectedTableCode.value = ''
        fieldData.value = []
        pagination.total = 0
      } catch (error) {
        ElMessage.error('加载表列表失败')
      }
    }

    // 获取当前表的业务系统
    const getCurrentTableBusinessCode = () => {
      if (!selectedTableCode.value) {
        currentTableBusinessCode.value = ''
        return
      }
      const table = tables.value.find(t => t.tableCode === selectedTableCode.value)
      if (table) {
        currentTableBusinessCode.value = table.businessCode || ''
      } else {
        currentTableBusinessCode.value = ''
      }
    }

    const loadFields = async () => {
      if (!selectedTableCode.value) {
        fieldData.value = []
        pagination.total = 0
        return
      }
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size,
          includeDisabled: true
        }
        if (currentTableBusinessCode.value) {
          params.businessCode = currentTableBusinessCode.value
        }
        const res = await getFieldList(selectedTableCode.value, params)
        if (res.code === 200) {
          let fields = []
          // 检查响应数据结构
          if (res.data) {
            if (Array.isArray(res.data)) {
              // 兼容旧接口（非分页数据，直接是数组）
              fields = res.data
              pagination.total = res.data.length || 0
            } else if (res.data.records && Array.isArray(res.data.records)) {
              // 分页数据
              fields = res.data.records
              pagination.total = Number(res.data.total) || 0
            } else {
              // 其他情况，尝试直接使用 data
              fields = []
              pagination.total = 0
              console.warn('Unexpected response data structure:', res.data)
            }
          } else {
            fields = []
            pagination.total = 0
          }

          // 处理主键字段，确保其必填状态正确显示在表格中
          fieldData.value = fields.map(field => {
            if (field.fieldName === 'id') {
              return {...field, isRequired: 1}
            }
            return field
          })
        } else {
          ElMessage.error(res.message || '加载字段列表失败')
          fieldData.value = []
          pagination.total = 0
        }
      } catch (error) {
        console.error('加载字段列表失败:', error)
        ElMessage.error(error.message || '加载字段列表失败')
        fieldData.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    // 处理表选择变化
    const handleTableChange = () => {
      getCurrentTableBusinessCode()
      loadFields()
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadFields()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadFields()
    }

    const fetchAllFieldsForTable = async () => {
      if (!selectedTableCode.value) return []
      const params = {
        current: 1,
        size: 500,
        includeDisabled: true
      }
      if (currentTableBusinessCode.value) {
        params.businessCode = currentTableBusinessCode.value
      }
      const res = await getFieldList(selectedTableCode.value, params)
      if (res.code !== 200 || !res.data) return []
      if (Array.isArray(res.data)) return res.data
      if (res.data.records && Array.isArray(res.data.records)) return res.data.records
      return []
    }

    const syncCommonPresetTableSelection = async () => {
      const table = commonPresetTableRef.value
      if (!table) return
      const keysToSelect = [...commonFieldSelectedKeys.value]
      syncingPresetSelection.value = true
      await nextTick()
      table.clearSelection()
      await nextTick()
      commonPresetRows.value.forEach((row) => {
        if (row.selectable && keysToSelect.includes(row.key)) {
          table.toggleRowSelection(row, true)
        }
      })
      await nextTick()
      commonFieldSelectedKeys.value = keysToSelect
      syncingPresetSelection.value = false
    }

    const openCommonFieldDialog = async () => {
      if (!selectedTableCode.value) {
        ElMessage.warning('请先选择表')
        return
      }
      getCurrentTableBusinessCode()
      try {
        allFieldsForTable.value = await fetchAllFieldsForTable()
        let physicalColumns = []
        try {
          const phyRes = await getPhysicalColumnNames(
            selectedTableCode.value,
            currentTableBusinessCode.value
          )
          if (phyRes.code === 200 && Array.isArray(phyRes.data)) {
            physicalColumns = phyRes.data
          }
        } catch {
          physicalColumns = []
        }
        commonPresetRows.value = buildCommonFieldPresetRows(
          selectedTableCode.value,
          allFieldsForTable.value,
          physicalColumns
        )
        commonFieldSelectedKeys.value = commonPresetRows.value
          .filter((r) => r.selectable && r.defaultSelected)
          .map((r) => r.key)
        commonFieldDialogVisible.value = true
        await nextTick()
        syncCommonPresetTableSelection()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message || '加载字段列表失败')
      }
    }

    const handleCommonPresetSelectionChange = (rows) => {
      if (syncingPresetSelection.value) return
      commonFieldSelectedKeys.value = (rows || []).map((r) => r.key)
    }

    const selectAllAddableCommonFields = async () => {
      commonFieldSelectedKeys.value = commonPresetRows.value
        .filter((r) => r.selectable)
        .map((r) => r.key)
      await nextTick()
      syncCommonPresetTableSelection()
    }

    const clearCommonFieldSelection = async () => {
      commonFieldSelectedKeys.value = []
      const table = commonPresetTableRef.value
      if (!table) return
      syncingPresetSelection.value = true
      await nextTick()
      table.clearSelection()
      await nextTick()
      syncingPresetSelection.value = false
    }

    const handleCommonFieldDialogClosed = () => {
      commonPresetRows.value = []
      commonFieldSelectedKeys.value = []
    }

    const handleCommonFieldSubmit = async () => {
      const toAdd = commonPresetRows.value
        .filter((r) => r.selectable && commonFieldSelectedKeys.value.includes(r.key))
        .sort((a, b) => a.sortStep - b.sortStep)
      if (toAdd.length === 0) {
        ElMessage.warning('请勾选需要添加的字段')
        return
      }
      try {
        await ElMessageBox.confirm(
          `将为表「${selectedTableCode.value}」添加 ${toAdd.length} 个字段，并执行 ALTER TABLE。是否继续？`,
          '确认',
          { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
        )
      } catch {
        return
      }

      const sorts = (allFieldsForTable.value || [])
        .map((f) => Number(f.sort) || 0)
        .filter((n) => !Number.isNaN(n))
      let nextSort = sorts.length > 0 ? Math.max(...sorts) + 1 : 1

      commonFieldSubmitting.value = true
      const added = []
      const failed = []
      try {
        for (const preset of toAdd) {
          const payload = toAddFieldPayload(
            selectedTableCode.value,
            currentTableBusinessCode.value,
            preset,
            nextSort
          )
          try {
            const res = await addField(payload)
            if (res.code === 200) {
              added.push(preset.fieldName)
              nextSort += 1
              allFieldsForTable.value.push(payload)
            } else {
              failed.push(`${preset.fieldName}: ${res.message || '失败'}`)
            }
          } catch (e) {
            failed.push(
              `${preset.fieldName}: ${e.response?.data?.message || e.message || '失败'}`
            )
          }
        }
        if (added.length > 0) {
          ElMessage.success(`已添加 ${added.length} 个字段：${added.join('、')}`)
          await loadFields()
        }
        if (failed.length > 0) {
          ElMessage.warning(`部分失败：${failed.join('；')}`)
        }
        if (added.length > 0 && failed.length === 0) {
          commonFieldDialogVisible.value = false
        } else if (added.length > 0) {
          allFieldsForTable.value = await fetchAllFieldsForTable()
          let physicalColumns = []
          try {
            const phyRes = await getPhysicalColumnNames(
              selectedTableCode.value,
              currentTableBusinessCode.value
            )
            if (phyRes.code === 200 && Array.isArray(phyRes.data)) {
              physicalColumns = phyRes.data
            }
          } catch {
            physicalColumns = []
          }
          commonPresetRows.value = buildCommonFieldPresetRows(
            selectedTableCode.value,
            allFieldsForTable.value,
            physicalColumns
          )
          commonFieldSelectedKeys.value = []
          await nextTick()
          syncCommonPresetTableSelection()
        }
      } finally {
        commonFieldSubmitting.value = false
      }
    }

    const resetMigrateForm = () => {
      migrateForm.targetTableCode = ''
      migrateForm.migrateData = true
      migrateForm.removeFromSource = true
      migrateForm.joinSourceField = resolveTablePrimaryKeyFieldName(fieldData.value)
      migrateForm.joinTargetField = ''
      migrateTargetFields.value = []
    }

    const onMigrateDialogClosed = () => {
      migrateBatchMode.value = false
      migrateSourceRow.value = null
      migrateSourceRows.value = []
    }

    const openMigrateDialog = (row) => {
      if (isPrimaryKey(row)) {
        ElMessage.warning('主键字段不能迁移')
        return
      }
      migrateBatchMode.value = false
      migrateSourceRows.value = []
      migrateSourceRow.value = row
      resetMigrateForm()
      migrateDialogVisible.value = true
    }

    const openBatchMigrateDialog = () => {
      const rows = batchMigratableRows.value
      if (!rows.length) {
        const pkCount = (selectedRows.value || []).filter((row) => isPrimaryKey(row)).length
        if (pkCount > 0) {
          ElMessage.warning('所选字段均为主键，无法迁移')
        } else {
          ElMessage.warning('请先勾选要迁移的字段')
        }
        return
      }
      const pkSkipped = (selectedRows.value || []).length - rows.length
      if (pkSkipped > 0) {
        ElMessage.info(`已跳过 ${pkSkipped} 个主键字段`)
      }
      migrateBatchMode.value = true
      migrateSourceRow.value = null
      migrateSourceRows.value = [...rows]
      resetMigrateForm()
      migrateDialogVisible.value = true
    }

    const buildMigratePayload = (fieldId) => ({
      fieldId,
      targetTableCode: migrateForm.targetTableCode,
      businessCode: currentTableBusinessCode.value || undefined,
      migrateData: migrateForm.migrateData,
      removeFromSource: migrateForm.removeFromSource,
      joinSourceField: migrateForm.joinSourceField || 'id',
      joinTargetField: migrateForm.joinTargetField || undefined
    })

    const handleMigrateSubmit = async () => {
      if (migrateBatchMode.value) {
        await handleBatchMigrateSubmit()
        return
      }
      if (!migrateSourceRow.value?.id) {
        return
      }
      if (!migrateForm.targetTableCode) {
        ElMessage.warning('请选择目标表')
        return
      }
      if (migrateForm.migrateData && !migrateForm.joinTargetField) {
        ElMessage.warning('请选择目标表行对齐键')
        return
      }
      const src = migrateSourceRow.value
      const targetLabel =
        migrateTargetTableOptions.value.find((t) => t.tableCode === migrateForm.targetTableCode)?.tableName
        || migrateForm.targetTableCode
      let confirmMsg = `将字段「${src.label || src.fieldName}」迁移到表「${targetLabel}」？\n`
      if (migrateForm.migrateData) {
        confirmMsg += '· 将在目标表 ADD COLUMN（若缺列），并按关联列拷贝物理数据\n'
      } else {
        confirmMsg += '· 不拷贝物理数据\n'
      }
      if (migrateForm.removeFromSource) {
        confirmMsg += '· 迁移成功后从当前表删除该字段（DROP COLUMN）'
      } else {
        confirmMsg += '· 保留当前表字段'
      }
      try {
        await ElMessageBox.confirm(confirmMsg, '确认迁移', {
          type: 'warning',
          confirmButtonText: '开始迁移',
          cancelButtonText: '取消'
        })
      } catch {
        return
      }
      migrateSubmitting.value = true
      try {
        const res = await migrateFieldToTable(buildMigratePayload(src.id))
        if (res.code === 200) {
          const d = res.data || {}
          let msg = `已迁移到 ${d.targetTableCode}`
          if (d.dataMigrated) {
            msg += '，已拷贝数据'
          }
          if (d.removedFromSource) {
            msg += '，已从源表删除'
          }
          ElMessage.success(msg)
          if (d.warnings?.length) {
            ElMessage.warning(d.warnings.join('；'))
          }
          migrateDialogVisible.value = false
          await loadFields()
        }
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message || '迁移失败')
      } finally {
        migrateSubmitting.value = false
      }
    }

    const handleBatchMigrateSubmit = async () => {
      if (!migrateSourceRows.value.length) {
        return
      }
      if (!migrateForm.targetTableCode) {
        ElMessage.warning('请选择目标表')
        return
      }
      if (migrateForm.migrateData && !migrateForm.joinTargetField) {
        ElMessage.warning('请选择目标表行对齐键')
        return
      }
      const targetLabel =
        migrateTargetTableOptions.value.find((t) => t.tableCode === migrateForm.targetTableCode)?.tableName
        || migrateForm.targetTableCode
      const names = migrateSourceRows.value
        .map((r) => r.label || r.fieldName || r.fieldCode)
        .join('、')
      let confirmMsg = `将 ${migrateSourceRows.value.length} 个字段迁移到表「${targetLabel}」？\n字段：${names}\n`
      if (migrateForm.migrateData) {
        confirmMsg += '· 各字段在目标表 ADD COLUMN（若缺列），并按关联列拷贝数据\n'
      } else {
        confirmMsg += '· 不拷贝物理数据\n'
      }
      confirmMsg += migrateForm.removeFromSource
        ? '· 成功后从当前表删除已迁字段'
        : '· 保留当前表字段'
      try {
        await ElMessageBox.confirm(confirmMsg, '确认批量迁移', {
          type: 'warning',
          confirmButtonText: '开始批量迁移',
          cancelButtonText: '取消'
        })
      } catch {
        return
      }
      migrateSubmitting.value = true
      try {
        const res = await migrateFieldBatchToTable({
          fieldIds: migrateSourceRows.value.map((r) => r.id),
          targetTableCode: migrateForm.targetTableCode,
          businessCode: currentTableBusinessCode.value || undefined,
          migrateData: migrateForm.migrateData,
          removeFromSource: migrateForm.removeFromSource,
          joinSourceField: migrateForm.joinSourceField || 'id',
          joinTargetField: migrateForm.joinTargetField || undefined
        })
        if (res.code !== 200 || !res.data) {
          ElMessage.error(res.message || '批量迁移失败')
          return
        }
        const d = res.data
        const successCount = d.successCount ?? 0
        const failCount = d.failCount ?? 0
        const failedItems = (d.items || []).filter((it) => !it.success)
        if (failCount === 0) {
          ElMessage.success(`已成功迁移 ${successCount} 个字段`)
          migrateDialogVisible.value = false
        } else if (successCount === 0) {
          const detail = failedItems
            .map((it) => `${it.fieldCode || it.fieldName || it.fieldId}：${it.message || '迁移失败'}`)
            .join('\n')
          await ElMessageBox.alert(detail, '批量迁移失败', { type: 'error' })
        } else {
          const detail = failedItems
            .map((it) => `${it.fieldCode || it.fieldName || it.fieldId}：${it.message || '迁移失败'}`)
            .join('\n')
          await ElMessageBox.alert(
            `成功 ${successCount} 个，失败 ${failCount} 个：\n${detail}`,
            '批量迁移部分完成',
            { type: 'warning' }
          )
          migrateDialogVisible.value = false
        }
        selectedRows.value = []
        tableRef.value?.clearSelection?.()
        await loadFields()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message || '批量迁移失败')
      } finally {
        migrateSubmitting.value = false
      }
    }

    const handleSyncMissingFieldsFromPhysical = async () => {
      if (!selectedTableCode.value) {
        ElMessage.warning('请先选择表')
        return
      }
      getCurrentTableBusinessCode()
      try {
        await ElMessageBox.confirm(
          `将从业务库读取表「${selectedTableCode.value}」的物理列，把元数据中尚未登记的列补写入（不修改物理表、不删除已有元数据字段）。是否继续？`,
          '同步缺失字段',
          { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
        )
      } catch {
        return
      }
      syncMissingFieldsSubmitting.value = true
      try {
        const res = await syncMissingFieldsFromPhysical(
          selectedTableCode.value,
          currentTableBusinessCode.value
        )
        if (res.code === 200 && res.data) {
          const added = res.data.added || []
          const skipped = res.data.skipped || []
          if (added.length > 0) {
            ElMessage.success(`已补登记 ${added.length} 个字段：${added.join('、')}`)
            await loadFields()
          } else {
            ElMessage.info('没有需要补登记的字段' + (skipped.length ? `（物理列均已登记：${skipped.length} 个）` : ''))
          }
        }
      } catch (e) {
        ElMessage.error(e.response?.data?.message || e.message || '同步失败')
      } finally {
        syncMissingFieldsSubmitting.value = false
      }
    }

    const handleAdd = () => {
      dialogTitle.value = '新增字段'

      // 默认为非主键字段的排序号
      let newSort = 1
      if (fieldData.value && fieldData.value.length > 0) {
        // 找出当前所有字段中的最大排序号
        const existingSorts = fieldData.value
            .map(field => Number(field.sort) || 0)
            .filter(sort => !isNaN(sort))

        if (existingSorts.length > 0) {
          newSort = Math.max(...existingSorts) + 1
        }
      }

      Object.assign(form, {
        id: null,
        fieldCode: '',
        tableCode: selectedTableCode.value,
        fieldName: '',
        fieldType: '',
        baseFieldType: '',
        label: '',
        isRequired: 0,
        formComponent: 'input',
        validateRule: '',
        sort: newSort, // 默认为计算的排序号
        isEnabled: 1,
        inForm: 1,
        businessCode: currentTableBusinessCode.value // 默认为当前表的业务系统
      })

      // 重置类型参数
      Object.assign(typeParams, {
        length: 50,
        precision: 10,
        scale: 2,
        enumValues: '',
        statusValues: ''
      })

      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑字段'

      // 先保存校验规则，避免被watch清空
      let savedValidateRule = row.validateRule
      // 确保校验规则是字符串类型
      if (savedValidateRule === null || savedValidateRule === undefined) {
        savedValidateRule = ''
      } else if (typeof savedValidateRule !== 'string') {
        // 如果是对象类型，转换为JSON字符串
        savedValidateRule = JSON.stringify(savedValidateRule)
      }

      Object.assign(form, {
        id: row.id,
        fieldCode: row.fieldCode,
        tableCode: row.tableCode,
        fieldName: row.fieldName,
        fieldType: row.fieldType,
        baseFieldType: '', // 会通过watch自动解析
        label: row.label,
        isRequired: row.isRequired,
        formComponent: row.formComponent,
        inForm: row.inForm != null ? row.inForm : 1,
        validateRule: savedValidateRule, // 使用保存的校验规则
        sort: row.sort,
        businessCode: row.businessCode || '' // 设置当前字段的业务系统
      })

      // 先重置typeParams，避免残留上一个字段的值
      Object.assign(typeParams, {
        length: 50,
        precision: 10,
        scale: 2,
        enumValues: '',
        statusValues: ''
      })

      // 解析字段类型，自动填充baseFieldType和typeParams
      // 使用 nextTick 确保 watch 不会在设置 baseFieldType 时清空校验规则
      if (row.fieldType) {
        const {baseType, length, precision, scale, enumValues} = parseFieldType(row.fieldType)
        // 先设置其他参数
        typeParams.length = length
        typeParams.precision = precision
        typeParams.scale = scale

        // 如果是ENUM类型，先处理枚举值
        if (baseType === 'ENUM') {
          // 优先从校验规则中提取枚举值
          if (savedValidateRule && savedValidateRule.trim() !== '' && savedValidateRule !== '{}') {
            const enumValuesStr = extractEnumValuesFromValidateRule(savedValidateRule)
            if (enumValuesStr !== null && enumValuesStr !== '') {
              typeParams.enumValues = enumValuesStr
            } else {
              // 如果校验规则中没有枚举值，使用从字段类型中解析的值
              typeParams.enumValues = enumValues || ''
            }
          } else {
            // 如果没有校验规则，使用从字段类型中解析的值
            typeParams.enumValues = enumValues || ''
          }
        }

        // 最后设置 baseFieldType，这会触发 watch，但由于我们已经处理了校验规则，不会影响枚举值
        form.baseFieldType = baseType

        // 已按「状态字段（含 options）」保存但误选 ENUM 时，编辑页自动纠正为 TINYINT
        if (baseType === 'ENUM' && isStatusStyleValidateRule(savedValidateRule)) {
          applyStatusFieldTypePreset(true)
        }

        if (isStatusIntegerFieldType(form.baseFieldType)) {
          typeParams.statusValues = formatStatusEntriesFromValidateRule(savedValidateRule) || ''
        }
      }

      // 对于主键字段（字段名为id或uuid），确保设置为必填且设置默认表单组件
      if (row.fieldName === 'id' || row.fieldName === 'uuid') {
        form.isRequired = 1 // 主键字段强制设置为必填
        if (!row.formComponent) {
          form.formComponent = 'primary_key' // 主键字段使用primary_key表单组件
        }
      }

      dialogVisible.value = true
    }

    // 同步枚举值到校验规则
    const syncEnumToValidateRule = () => {
      if (form.baseFieldType !== 'ENUM') {
        ElMessage.warning('当前字段类型不是ENUM，无法同步枚举值')
        return
      }

      // 将中文逗号转换为英文逗号
      let enumValuesStr = typeParams.enumValues || ''
      enumValuesStr = enumValuesStr.replace(/，/g, ',')

      // 去除空格并分割
      const values = enumValuesStr
          .split(',')
          .map(v => v.trim())
          .filter(v => v.length > 0)

      if (values.length === 0) {
        ElMessage.warning('请先填写枚举值')
        return
      }

      // 构建校验规则对象
      let validateRuleObj = {}
      try {
        // 如果已有校验规则，先解析
        if (form.validateRule && form.validateRule.trim() !== '' && form.validateRule !== '{}') {
          validateRuleObj = JSON.parse(form.validateRule)
        }
      } catch (e) {
        // 解析失败，使用空对象
        validateRuleObj = {}
      }

      // 更新校验规则（保留已有 options 的 label）
      validateRuleObj.operator = 'IN'
      validateRuleObj.values = values
      validateRuleObj.options = mergeOptionsWithValues(values, validateRuleObj.options || [])
      validateRuleObj.message = validateRuleObj.message || '请选择有效值'
      validateRuleObj.trigger = validateRuleObj.trigger || 'blur'

      if ('value' in validateRuleObj) {
        delete validateRuleObj.value
      }

      form.validateRule = JSON.stringify(validateRuleObj, null, 2)
      ElMessage.success('枚举值已同步到校验规则')
    }

    // 从校验规则中提取枚举值（辅助函数）
    const extractEnumValuesFromValidateRule = (validateRuleStr) => {
      if (!validateRuleStr || validateRuleStr.trim() === '' || validateRuleStr === '{}') {
        return null
      }

      try {
        let validateRuleObj = null

        // 先尝试直接解析
        try {
          validateRuleObj = JSON.parse(validateRuleStr.trim())
        } catch (parseError) {
          // 如果解析失败，尝试修复trailing comma后再解析
          // 移除数组和对象中的trailing comma
          let fixedStr = validateRuleStr.trim()
              .replace(/,(\s*[}\]])/g, '$1') // 移除数组和对象末尾的逗号

          try {
            validateRuleObj = JSON.parse(fixedStr)
          } catch (retryError) {
            return null // 如果修复后仍然失败，返回null
          }
        }

        if (validateRuleObj && validateRuleObj.operator === 'IN' && validateRuleObj.values && Array.isArray(validateRuleObj.values)) {
          // 过滤掉空值（空字符串、null、undefined）和字符集前缀，然后转换为逗号分隔的字符串
          const filteredValues = validateRuleObj.values
              .filter(val => val !== null && val !== undefined && val !== '')
              .map(val => String(val).trim())
              .filter(val => val.length > 0 && !val.startsWith('_utf8mb4') && !val.startsWith('_gbk') && !val.startsWith('_'))

          // 将过滤后的values数组转换为逗号分隔的字符串，并去除末尾逗号
          let enumValuesStr = filteredValues.join(',')
          // 确保去除末尾的逗号（防止意外情况）
          enumValuesStr = enumValuesStr.replace(/,$/, '')

          return enumValuesStr
        }

        return null
      } catch (e) {
        return null
      }
    }

    // 同步校验规则到枚举值
    const syncValidateRuleToEnum = () => {
      if (form.baseFieldType !== 'ENUM') {
        ElMessage.warning('当前字段类型不是ENUM，无法同步')
        return
      }

      if (!form.validateRule || form.validateRule.trim() === '' || form.validateRule === '{}') {
        ElMessage.warning('校验规则为空，无法同步')
        return
      }

      const enumValuesStr = extractEnumValuesFromValidateRule(form.validateRule)

      if (enumValuesStr !== null) {
        typeParams.enumValues = enumValuesStr
        ElMessage.success('校验规则已同步到枚举值')
      } else {
        ElMessage.warning('校验规则中没有找到IN操作符的values数组')
      }
    }

    const handleSubmit = async () => {
      normalizeFormCodes(form, [
        { key: 'fieldCode', mode: 'code' },
        { key: 'fieldName', mode: 'column' }
      ])
      // 检查是否设置了主键字段（仅基于formComponent判断）
      const isSettingPrimaryKey = form.formComponent === 'primary_key'

      // 检查当前是否正在将主键字段修改为非主键字段
      if (form.id) {
        // 获取当前字段的原始数据
        const originalField = fieldData.value.find(field => field.id === form.id)
        const wasPrimaryKey = originalField && originalField.formComponent === 'primary_key'

        // 如果是将主键字段修改为非主键字段，检查是否还有其他主键字段
        if (wasPrimaryKey && !isSettingPrimaryKey) {
          // 检查是否还有其他主键字段
          const hasOtherPrimaryKey = fieldData.value.some(field =>
              field.id !== form.id && // 排除当前字段
              field.formComponent === 'primary_key'
          )

          if (!hasOtherPrimaryKey) {
            ElMessage.error('当前表必须有且只有一个主键字段，无法将唯一的主键字段修改为非主键字段')
            return
          }
        }

        // 检查业务系统是否被修改
        if (originalField && originalField.businessCode !== form.businessCode) {
          // 添加业务系统修改提示
          try {
            await ElMessageBox.confirm(
                '修改字段的业务系统可能会影响关联数据，确定要继续吗？',
                '提示',
                {
                  confirmButtonText: '确定',
                  cancelButtonText: '取消',
                  type: 'warning'
                }
            )
          } catch (error) {
            return
          }
        }
      }

      // 如果是设置主键字段，检查当前表是否已经存在主键字段
      if (isSettingPrimaryKey) {
        // 查找当前表中已有的主键字段
        const existingPrimaryKey = fieldData.value.find(field =>
            field.formComponent === 'primary_key' &&
            field.id !== form.id // 排除当前正在编辑的字段
        )

        if (existingPrimaryKey) {
          ElMessage.error('当前表已经存在主键字段，每个表只能有一个主键字段')
          return
        }
      }

      // 表单验证
      let valid = true
      if (form.fieldName === 'id') {
        // 主键字段简化验证，跳过表单组件验证
        if (!form.fieldCode) {
          valid = false
          ElMessage.error('请输入字段编码')
        } else if (!form.fieldName) {
          valid = false
          ElMessage.error('请输入字段名称')
        } else if (!form.fieldType) {
          valid = false
          ElMessage.error('请输入字段类型')
        } else if (!form.label) {
          valid = false
          ElMessage.error('请输入显示名')
        }
      } else {
        // 非主键字段使用正常的表单验证
        try {
          await formRef.value.validate()
        } catch (error) {
          valid = false
          // 表单验证失败，Element Plus会自动显示错误信息
        }
      }

      if (!valid) {
        return
      }

      // 状态模板 + ENUM 组合会在库中生成 ENUM(...)，与「状态字段（含 options）」设计不符
      if (
        form.baseFieldType === 'ENUM' &&
        isStatusStyleValidateRule(form.validateRule)
      ) {
        applyStatusFieldTypePreset(true)
      }

      if (isStatusFieldContext.value && typeParams.statusValues?.trim()) {
        syncStatusToValidateRule(false)
      }

      // 统一保存逻辑
      try {
        // 确保主键字段的排序号为0
        const submitForm = {...form}
        if (form.fieldName === 'id') {
          submitForm.sort = 0
        }

        // 如果是ENUM类型，处理枚举值：优先从校验规则中提取，如果校验规则无效才使用枚举值文本框的值
        if (form.baseFieldType === 'ENUM') {
          // 优先尝试从校验规则中提取枚举值
          let enumValuesStr = null
          let validateRuleObj = null

          if (submitForm.validateRule && submitForm.validateRule.trim() !== '' && submitForm.validateRule !== '{}') {
            // 尝试从校验规则中提取枚举值
            enumValuesStr = extractEnumValuesFromValidateRule(submitForm.validateRule)

            if (enumValuesStr !== null) {
              // 成功从校验规则中提取到枚举值，使用这些值更新typeParams.enumValues
              // 这样computedFieldType会使用正确的枚举值来构建字段类型
              typeParams.enumValues = enumValuesStr
              // 确保字段类型也被更新（触发computed重新计算）
              submitForm.fieldType = computedFieldType.value

              // 重新解析校验规则对象，确保格式正确
              try {
                let validateRuleStr = submitForm.validateRule.trim()
                try {
                  validateRuleObj = JSON.parse(validateRuleStr)
                } catch (parseError) {
                  // 修复trailing comma后重新解析
                  validateRuleStr = validateRuleStr.replace(/,(\s*[}\]])/g, '$1')
                  validateRuleObj = JSON.parse(validateRuleStr)
                }

                // 确保校验规则中的values数组是正确的
                if (validateRuleObj && validateRuleObj.operator === 'IN' && validateRuleObj.values) {
                  validateRuleObj.values = validateRuleObj.values
                      .filter(val => val !== null && val !== undefined && val !== '')
                      .map(val => String(val).trim())
                      .filter(val => val.length > 0 && !val.startsWith('_utf8mb4') && !val.startsWith('_gbk') && !val.startsWith('_'))
                  validateRuleObj.options = mergeOptionsWithValues(
                    validateRuleObj.values,
                    validateRuleObj.options || []
                  )
                  validateRuleObj.message = validateRuleObj.message || '请选择有效值'
                  validateRuleObj.trigger = validateRuleObj.trigger || 'blur'

                  if ('value' in validateRuleObj) {
                    delete validateRuleObj.value
                  }

                  submitForm.validateRule = JSON.stringify(validateRuleObj, null, 2)
                }
              } catch (e) {
                const values = enumValuesStr.split(',').map(v => v.trim()).filter(v => v.length > 0)
                if (values.length > 0) {
                  let prevRule = {}
                  try {
                    if (submitForm.validateRule?.trim() && submitForm.validateRule !== '{}') {
                      prevRule = JSON.parse(submitForm.validateRule) || {}
                    }
                  } catch {
                    prevRule = {}
                  }
                  validateRuleObj = {
                    operator: 'IN',
                    values: values,
                    options: mergeOptionsWithValues(values, prevRule.options || []),
                    message: '请选择有效值',
                    trigger: 'blur'
                  }
                  submitForm.validateRule = JSON.stringify(validateRuleObj, null, 2)
                }
              }
            }
          }

          // 如果无法从校验规则中提取枚举值，使用枚举值文本框中的值
          if (enumValuesStr === null && typeParams.enumValues) {
            // 将中文逗号转换为英文逗号
            enumValuesStr = typeParams.enumValues.replace(/，/g, ',')

            // 分割并处理枚举值，过滤掉空值
            const values = enumValuesStr
                .split(',')
                .map(v => v.trim())
                .filter(v => v.length > 0)

            if (values.length > 0) {
              // 更新typeParams.enumValues为处理后的值（去除末尾逗号等）
              typeParams.enumValues = values.join(',')
              // 确保字段类型也被更新
              submitForm.fieldType = computedFieldType.value

              // 构建校验规则对象（确保不包含value字段）
              let prevRule = {}
              try {
                if (submitForm.validateRule && submitForm.validateRule.trim() && submitForm.validateRule !== '{}') {
                  prevRule = JSON.parse(submitForm.validateRule) || {}
                }
              } catch {
                prevRule = {}
              }
              validateRuleObj = {
                operator: 'IN',
                values: values,
                options: mergeOptionsWithValues(values, prevRule.options || []),
                message: '请选择有效值',
                trigger: 'blur'
              }
              if ('value' in validateRuleObj) {
                delete validateRuleObj.value
              }
              submitForm.validateRule = JSON.stringify(validateRuleObj, null, 2)
            }
          }
        }

        // 处理校验规则：如果是{}，转换为null
        if (submitForm.validateRule === '{}' || submitForm.validateRule === '{\n}') {
          submitForm.validateRule = null
        }

        if (submitForm.inForm === 0) {
          submitForm.formComponent = 'none'
        } else if (submitForm.inForm === 1 && submitForm.formComponent === 'none' && submitForm.fieldName !== 'id' && submitForm.fieldName !== 'uuid') {
          submitForm.formComponent = getRecommendedFormComponent(
            submitForm.baseFieldType || 'VARCHAR',
            submitForm.fieldName
          )
        }

        if (form.id) {
          await updateField(submitForm)
        } else {
          await addField(submitForm)
        }
        ElMessage.success('操作成功')
        formGuard.markClean()
        dialogVisible.value = false
        loadFields()
      } catch (error) {
        // 显示后端返回的具体错误消息，适配多种错误格式
        const errorMsg = error.response?.data?.message || error.data?.message || error.message || '操作失败'
        ElMessage.error(errorMsg)
      }
    }

    const handleDelete = (row) => {
      // 增强主键字段判断：检查formComponent或字段名为id/uuid
      const isPrimaryKey = row.formComponent === 'primary_key' || row.fieldName === 'id' || row.fieldName === 'uuid'

      // 检查是否为表中最后一个字段
      const isLastField = pagination.total <= 1

      if (isPrimaryKey) {
        ElMessage.error('主键字段不允许删除')
        return
      }

      if (isLastField) {
        ElMessage.error('不能删除表中最后一个字段')
        return
      }

      ElMessageBox.confirm('确定要删除该字段吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteField(row.id)
          ElMessage.success('删除成功')
          loadFields()
        } catch (error) {
          // 显示后端返回的具体错误消息，适配多种错误格式
          const errorMsg = error.response?.data?.message || error.data?.message || error.message || '删除失败'
          ElMessage.error(errorMsg)
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleToggleEnable = (row) => {
      const newStatus = row.isEnabled === 1 ? 0 : 1
      const statusText = newStatus === 1 ? '启用' : '禁用'

      // 增强主键字段判断：防止主键字段被禁用
      if (newStatus === 0 && isPrimaryKey(row)) {
        ElMessage.error('主键字段不允许禁用')
        return
      }

      ElMessageBox.confirm(`确定要${statusText}该字段吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await updateField({
            ...row,
            isEnabled: newStatus
          })
          ElMessage.success(`${statusText}成功`)
          loadFields()
        } catch (error) {
          // 显示后端返回的具体错误消息，适配多种错误格式
          const errorMsg = error.response?.data?.message || error.data?.message || error.message || `${statusText}失败`
          ElMessage.error(errorMsg)
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    // 处理表格选择变化
    const handleSelectionChange = (selection) => {
      selectedRows.value = selection
    }

    // 判断字段是否为主键
    const isPrimaryKey = (row) => {
      return row.formComponent === 'primary_key' || row.fieldName === 'id' || row.fieldName === 'uuid'
    }

    // 处理批量删除
    const handleBatchDelete = async () => {
      if (!selectedRows.value || selectedRows.value.length === 0) {
        ElMessage.warning('请选择要删除的字段')
        return
      }

      // 增强主键字段判断：检查formComponent或字段名为id/uuid
      const hasPrimaryKey = selectedRows.value.some(row => isPrimaryKey(row))

      // 检查删除后是否会导致表中字段数量为0
      const remainingFieldsCount = pagination.total - selectedRows.value.length
      if (remainingFieldsCount <= 0) {
        ElMessage.error('不能删除表中所有字段')
        return
      }

      if (hasPrimaryKey) {
        ElMessage.error('选中的字段中包含主键字段，主键字段不允许删除')
        return
      }

      ElMessageBox.confirm('确定要删除选中的字段吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = selectedRows.value.map(row => row.id)
          await batchDeleteField({ids})
          ElMessage.success('批量删除成功')
          loadFields()
          selectedRows.value = []
        } catch (error) {
          const errorMsg = error.response?.data?.message || error.data?.message || error.message || '批量删除失败'
          ElMessage.error(errorMsg)
        }
      }).catch(() => {
        // 处理用户取消操作
      })
    }

    // 批量启用/禁用
    const handleBatchToggleEnable = async (status) => {
      if (!selectedRows.value || selectedRows.value.length === 0) {
        ElMessage.warning('请选择要操作的字段')
        return
      }

      // 检查是否包含主键字段且要禁用
      if (status === 0) {
        const hasPrimaryKey = selectedRows.value.some(row => isPrimaryKey(row))
        if (hasPrimaryKey) {
          ElMessage.error('选中的字段中包含主键字段，主键字段不允许禁用')
          return
        }
      }

      const actionText = status === 1 ? '启用' : '禁用'

      try {
        await ElMessageBox.confirm(`确定要${actionText}选中的 ${selectedRows.value.length} 个字段吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const ids = selectedRows.value.map(row => row.id)
        await batchUpdateFieldStatus(ids, status)
        ElMessage.success(`批量${actionText}成功`)
        loadFields()
        // 清空选中状态
        selectedRows.value = []
      } catch (error) {
        if (error.name === 'cancel' || error.toString().includes('取消')) {
          // 处理用户取消操作，不做任何处理
          return
        }
        ElMessage.error(error.response?.data?.message || error.data?.message || error.message || `批量${actionText}失败`)
      }
    }

    // 解码十六进制编码的中文字符
    const decodeHexChinese = (str) => {
      if (!str) return str;

      // 匹配 _utf8mb4'...' 格式的字符串
      return str.replace(/_utf8mb4\\'([^']+)\\'/g, (match, hexStr) => {
        // 处理 UTF-8 编码的乱码字符串
        try {
          // 方法1：通过 encodeURIComponent 和 decodeURIComponent 转换
          const decoded = decodeURIComponent(escape(hexStr));
          return `'${decoded}'`;
        } catch (e) {
          try {
            // 方法2：手动转换 UTF-8 字节序列
            const bytes = new Uint8Array(hexStr.length);
            for (let i = 0; i < hexStr.length; i++) {
              bytes[i] = hexStr.charCodeAt(i);
            }
            const decoded = new TextDecoder('utf-8').decode(bytes);
            return `'${decoded}'`;
          } catch (e2) {
            // 如果所有解码方法都失败，返回原始字符串
            return match;
          }
        }
      });
    };

    // 约束相关方法
    const handleViewConstraints = () => {
      constraintDialogVisible.value = true
    }

    const loadConstraints = async (tableCode) => {
      // 使用传入的tableCode或默认使用selectedTableCode.value
      const currentTableCode = tableCode || selectedTableCode.value
      if (!currentTableCode) return
      constraintLoading.value = true
      try {
        const res = await getConstraintList(currentTableCode)
        if (res.code === 200) {
          // 对约束内容进行解码处理
          constraints.value = (res.data || []).map(constraint => ({
            ...constraint,
            constraintContent: decodeHexChinese(constraint.constraintContent)
          }))
        }
      } catch (error) {
        ElMessage.error('加载约束列表失败')
        constraints.value = []
      } finally {
        constraintLoading.value = false
      }
    }

    const handleDeleteConstraint = (row) => {
      ElMessageBox.confirm('确定要删除该约束吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteConstraint(row)
          ElMessage.success('删除成功')
          // 传递row.tableCode给loadConstraints，确保使用正确的表编码刷新约束列表
          loadConstraints(row.tableCode)
          // 同时刷新字段列表，确保编辑字段时校验规则是最新的
          loadFields()
        } catch (error) {
          const errorMsg = error.response?.data?.message || error.data?.message || error.message || '删除失败'
          ElMessage.error(errorMsg)
        }
      }).catch(() => {
        // 处理用户取消操作
      })
    }

    onMounted(() => {
      loadTables()
      loadBusinessSystems()
    })

    const parseFieldOptionItems = (row) => resolveFieldOptionItems(row)

    const showOptionCode = (opt) =>
      opt.value != null && String(opt.label) !== String(opt.value)

    const optionTagType = (opt, index) => {
      const text = `${opt.label ?? ''} ${opt.value ?? ''}`.toLowerCase()
      if (/草稿|draft|待审|pending/.test(text)) return 'info'
      if (/生效|确认|confirm|启用|enable|通过|success|正常/.test(text)) return 'success'
      if (/作废|取消|cancel|关闭|禁用|disable|驳回|reject|删除/.test(text)) return 'danger'
      if (/警告|warning|暂停|pause/.test(text)) return 'warning'
      const cycle = ['', 'success', 'warning', 'danger', 'info']
      return cycle[index % cycle.length]
    }

    // 页面激活时重新加载表列表，确保获取最新的表名
    onActivated(() => {
      loadTables()
      loadBusinessSystems()
    })

    return {
      tables,
      fieldData,
      businessSystems,
      selectedBusinessCode,
      selectedTableCode,
      currentTableBusinessCode,
      loading,
      dialogVisible,
      dialogTitle,
      formRef,
      tableRef,
      selectedRows,
      batchEnableToggle,
      pagination,
      form,
      isEditingPrimaryKey,
      fieldRules,
      formGuard,
      applyIdentifierBlur,
      baseFieldTypes,
      typeParams,
      // 约束相关
      constraintDialogVisible,
      constraints,
      constraintLoading,
      // 方法
      loadFields,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleToggleEnable,
      handleDelete,
      handleDialogClose,
      handleSelectionChange,
      handleBatchDelete,
      handleBatchToggleEnable,
      handleTableChange,
      // 约束相关方法
      handleViewConstraints,
      loadConstraints,
      handleDeleteConstraint,
      // ENUM相关方法
      syncEnumToValidateRule,
      syncValidateRuleToEnum,
      syncStatusToValidateRule,
      syncValidateRuleToStatus,
      isStatusFieldContext,
      onValidateRuleExampleApplied,
      applyStatusFieldTypePreset,
      // 辅助函数
      isPrimaryKey,
      commonFieldDialogVisible,
      commonPresetRows,
      commonFieldSelectedKeys,
      commonFieldSubmitting,
      commonPresetTableRef,
      commonFieldCodeExample,
      commonPresetStatusTagType,
      addableCommonFieldCount,
      openCommonFieldDialog,
      handleCommonPresetSelectionChange,
      selectAllAddableCommonFields,
      clearCommonFieldSelection,
      handleCommonFieldDialogClosed,
      handleCommonFieldSubmit,
      syncMissingFieldsSubmitting,
      migrateDialogVisible,
      migrateSubmitting,
      migrateBatchMode,
      migrateSourceRow,
      migrateSourceRows,
      migrateForm,
      migrateTargetTableOptions,
      migrateJoinExample,
      migrateTargetJoinOptions,
      migrateTargetFieldsLoading,
      loadMigrateTargetJoinFields,
      batchMigratableCount,
      openMigrateDialog,
      openBatchMigrateDialog,
      onMigrateDialogClosed,
      handleMigrateSubmit,
      handleSyncMissingFieldsFromPhysical,
      parseFieldOptionItems,
      showOptionCode,
      optionTagType,
      isDiscreteStatusFieldName
    }
  }
}
</script>

<style scoped>
.field-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.precision-scale-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 确保精度和小数位数表单项在同一行 */
.precision-scale-form-item {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.precision-scale-form-item .el-form-item__label {
  margin-right: 10px;
  margin-bottom: 0;
  white-space: nowrap;
}

.precision-scale-form-item .el-form-item__content {
  flex: 1;
  margin-left: 0 !important;
}

.pk-field-alert {
  margin-bottom: 16px;
}

.common-field-intro {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.common-field-intro code {
  font-size: 12px;
  padding: 0 4px;
  background: #f4f4f5;
  border-radius: 2px;
}

.common-field-toolbar {
  margin-bottom: 8px;
}

.migrate-hints-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.migrate-hints-list {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  line-height: 1.65;
  color: #606266;
}

.migrate-hints-list li {
  margin-bottom: 4px;
}

.migrate-hints-list code {
  font-size: 11px;
  padding: 0 3px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 2px;
}

.migrate-batch-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.migrate-batch-tag {
  max-width: 100%;
}

.migrate-join-hint {
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 8px;
  padding: 8px 10px;
  background: #ecf5ff;
  border-radius: 4px;
  border: 1px solid #d9ecff;
}

.migrate-join-sql {
  font-size: 11px;
  font-family: ui-monospace, Consolas, monospace;
  color: #606266;
  margin: 0 0 12px;
  padding: 6px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  word-break: break-all;
}

.migrate-field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}

.migrate-field-tip code {
  font-size: 11px;
}

.field-options-empty {
  color: #c0c4cc;
}

.field-options-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  line-height: 1.4;
  padding: 2px 0;
}

.field-opt-tag {
  border-radius: 4px;
  max-width: 100%;
}

.field-opt-tag :deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.field-opt-code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.06);
}

.field-opt-label {
  font-size: 12px;
}
</style>
