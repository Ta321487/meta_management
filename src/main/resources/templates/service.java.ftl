package ${packageName}.service;

import ${packageName}.common.PageRequest;
import ${packageName}.common.PageResult;
import ${packageName}.entity.${className};
import ${packageName}.mapper.${className}Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ${table.tableName}服务类
 */
@Service
public class ${className}Service {

    @Autowired
    private ${className}Mapper ${entityName}Mapper;

    /**
     * 新增
     */
    public void add(${className} ${entityName}) {
        ${entityName}Mapper.insert(${entityName});
    }

    /**
     * 更新
     */
    public void update(${className} ${entityName}) {
        ${entityName}Mapper.update(${entityName});
    }

    /**
     * 删除
     */
    public void delete(Long id) {
        ${entityName}Mapper.deleteById(id);
    }

    /**
     * 根据ID查询
     */
    public ${className} getById(Long id) {
        return ${entityName}Mapper.selectById(id);
    }

    /**
     * 查询列表
     */
    public List<${className}> list() {
        return ${entityName}Mapper.selectAll();
    }

    /**
     * 分页查询列表
     */
    public PageResult<${className}> page(PageRequest pageRequest) {
        Long total = ${entityName}Mapper.count();
        List<${className}> records = ${entityName}Mapper.selectPage(pageRequest);
        return new PageResult<>(total, records);
    }
}

