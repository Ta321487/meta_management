package ${packageName}.controller;

import ${packageName}.common.Result;
import ${packageName}.common.PageRequest;
import ${packageName}.common.PageResult;
import ${packageName}.entity.${className};
import ${packageName}.service.${className}Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ${table.tableName}控制器
 */
@RestController
@RequestMapping("/api/${entityName}")
public class ${className}Controller {

    @Autowired
    private ${className}Service ${entityName}Service;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody ${className} ${entityName}) {
        try {
            ${entityName}Service.add(${entityName});
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody ${className} ${entityName}) {
        try {
            ${entityName}Service.update(${entityName});
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            ${entityName}Service.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除
     */
    @PostMapping("/batchDelete")
    public Result<?> batchDelete(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) params.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的记录");
            }
            ${entityName}Service.batchDelete(ids);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<${className}> getById(@PathVariable Long id) {
        ${className} ${entityName} = ${entityName}Service.getById(id);
        return Result.success(${entityName});
    }

    /**
     * 查询列表（支持分页、条件查询、排序）
     * @param current 当前页码，从1开始
     * @param size 每页大小
     * @param orderBy 排序字段（如：id, createTime）
     * @param orderDirection 排序方式（ASC/DESC）
     * @param conditions 查询条件（JSON格式，通过@RequestBody接收）
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer current,
                         @RequestParam(required = false) Integer size,
                         @RequestParam(required = false) String orderBy,
                         @RequestParam(required = false) String orderDirection) {
        try {
            // 如果传入了分页参数，使用分页查询
            if (current != null && size != null) {
                PageRequest pageRequest = new PageRequest();
                pageRequest.setCurrent(current);
                pageRequest.setSize(size);
                if (orderBy != null && !orderBy.trim().isEmpty()) {
                    pageRequest.setOrderBy(orderBy);
                }
                if (orderDirection != null && !orderDirection.trim().isEmpty()) {
                    pageRequest.setOrderDirection(orderDirection);
                }
                PageResult<${className}> pageResult = ${entityName}Service.page(pageRequest);
                return Result.success(pageResult);
            }
            // 否则使用非分页查询（兼容旧接口）
            List<${className}> list = ${entityName}Service.list();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询列表（支持条件查询和排序）
     * 使用POST方式，支持复杂的查询条件
     */
    @PostMapping("/page")
    public Result<PageResult<${className}>> page(@RequestBody PageRequest pageRequest) {
        try {
            PageResult<${className}> pageResult = ${entityName}Service.page(pageRequest);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("分页查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 条件查询列表（不分页）
     */
    @PostMapping("/listByCondition")
    public Result<List<${className}>> listByCondition(@RequestBody PageRequest pageRequest) {
        try {
            List<${className}> list = ${entityName}Service.listByCondition(pageRequest);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}

