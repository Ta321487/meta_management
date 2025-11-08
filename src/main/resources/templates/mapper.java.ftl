package ${packageName}.mapper;

import ${packageName}.entity.${className};
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ${table.tableName}Mapper接口
 */
@Mapper
public interface ${className}Mapper {

    /**
     * 新增
     */
    int insert(${className} ${entityName});

    /**
     * 更新
     */
    int update(${className} ${entityName});

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 根据ID查询
     */
    ${className} selectById(Long id);

    /**
     * 查询所有
     */
    List<${className}> selectAll();
}

