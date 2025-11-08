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
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<${className}> getById(@PathVariable Long id) {
        ${className} ${entityName} = ${entityName}Service.getById(id);
        return Result.success(${entityName});
    }

    /**
     * 查询列表（支持分页）
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer current,
                         @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<${className}> pageResult = ${entityName}Service.page(pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<${className}> list = ${entityName}Service.list();
        return Result.success(list);
    }

    /**
     * 分页查询列表
     */
    @GetMapping("/page")
    public Result<PageResult<${className}>> page(PageRequest pageRequest) {
        PageResult<${className}> pageResult = ${entityName}Service.page(pageRequest);
        return Result.success(pageResult);
    }
}

