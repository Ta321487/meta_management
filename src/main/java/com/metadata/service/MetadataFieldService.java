package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataField;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 字段服务
 */
@Service
public class MetadataFieldService {

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private OperationLogService logService;

    @Autowired
    @Lazy
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private SqlExecuteService sqlExecuteService;

    /**
     * 新增字段
     */
    @Transactional
    public void add(MetadataField field) {
        if (!CodeValidator.isValidCode(field.getFieldCode())) {
            throw new RuntimeException("字段编码格式不正确");
        }
        if (fieldMapper.countByCode(field.getTableCode(), field.getFieldCode()) > 0) {
            throw new RuntimeException("字段编码已存在");
        }
        fieldMapper.insert(field);
        logService.logSuccess("admin", "ADD", "新增字段：" + JSON.toJSONString(field));
        
        // 生成并执行ALTER TABLE ADD COLUMN语句
        try {
            String alterSql = codeGeneratorService.generateAlterTableAddColumnSQL(field.getTableCode(), field);
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, true);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("执行ALTER TABLE ADD COLUMN失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "ALTER_TABLE_ADD_COLUMN", "执行ALTER TABLE ADD COLUMN成功: " + alterSql.substring(0, Math.min(100, alterSql.length())));
        } catch (Exception e) {
            logService.logError("admin", "ALTER_TABLE_ADD_COLUMN", "执行ALTER TABLE ADD COLUMN失败", e.getMessage());
            throw new RuntimeException("执行ALTER TABLE ADD COLUMN失败: " + e.getMessage());
        }
    }

    /**
     * 更新字段
     */
    @Transactional
    public void update(MetadataField field) {
        MetadataField existing = fieldMapper.selectByCode(field.getTableCode(), field.getFieldCode());
        if (existing == null) {
            throw new RuntimeException("字段不存在");
        }
        field.setId(existing.getId());
        field.setFieldCode(existing.getFieldCode()); // 编码不可修改
        field.setTableCode(existing.getTableCode()); // 表编码不可修改
        fieldMapper.update(field);
        logService.logSuccess("admin", "EDIT", "更新字段：" + JSON.toJSONString(field));
        
        // 生成并执行ALTER TABLE MODIFY COLUMN语句
        try {
            String alterSql = codeGeneratorService.generateAlterTableModifyColumnSQL(field.getTableCode(), field);
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, true);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("执行ALTER TABLE MODIFY COLUMN失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "ALTER_TABLE_MODIFY_COLUMN", "执行ALTER TABLE MODIFY COLUMN成功: " + alterSql.substring(0, Math.min(100, alterSql.length())));
        } catch (Exception e) {
            logService.logError("admin", "ALTER_TABLE_MODIFY_COLUMN", "执行ALTER TABLE MODIFY COLUMN失败", e.getMessage());
            throw new RuntimeException("执行ALTER TABLE MODIFY COLUMN失败: " + e.getMessage());
        }
    }

    /**
     * 删除字段
     */
    @Transactional
    public void delete(Long id) {
        MetadataField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段不存在");
        }
        
        // 增强主键字段判断：检查formComponent或字段名为id/uuid
        boolean isPrimaryKey = "primary_key".equals(field.getFormComponent()) || "id".equals(field.getFieldName()) || "uuid".equals(field.getFieldName());
        if (isPrimaryKey) {
            throw new RuntimeException("主键字段不允许删除");
        }
        
        // 检查表中字段数量，不能删除最后一个字段
        Long fieldCount = fieldMapper.countByTableCode(field.getTableCode());
        if (fieldCount <= 1) {
            throw new RuntimeException("不能删除表中最后一个字段");
        }
        
        // 生成并执行ALTER TABLE DROP COLUMN语句
        try {
            String alterSql = codeGeneratorService.generateAlterTableDropColumnSQL(field.getTableCode(), field.getFieldName());
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, true);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("执行ALTER TABLE DROP COLUMN失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "ALTER_TABLE_DROP_COLUMN", "执行ALTER TABLE DROP COLUMN成功: " + alterSql.substring(0, Math.min(100, alterSql.length())));
        } catch (Exception e) {
            logService.logError("admin", "ALTER_TABLE_DROP_COLUMN", "执行ALTER TABLE DROP COLUMN失败", e.getMessage());
            throw new RuntimeException("执行ALTER TABLE DROP COLUMN失败: " + e.getMessage());
        }
        
        fieldMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除字段：" + JSON.toJSONString(field));
    }

    /**
     * 批量删除字段
     */
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("删除ID列表不能为空");
        }
        // 为每个ID调用单个删除方法，确保物理结构删除和日志记录正确
        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 查询表的所有字段
     */
    public List<MetadataField> listByTableCode(String tableCode) {
        return fieldMapper.selectByTableCode(tableCode);
    }

    /**
     * 分页查询表的字段
     */
    public PageResult<MetadataField> pageByTableCode(String tableCode, PageRequest pageRequest) {
        Long total = fieldMapper.countByTableCode(tableCode);
        List<MetadataField> records = fieldMapper.selectPageByTableCode(tableCode, pageRequest);
        return new PageResult<>(total, records);
    }
}

