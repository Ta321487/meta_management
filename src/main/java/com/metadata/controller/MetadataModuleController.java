package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataModule;
import com.metadata.service.MetadataModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模块控制器
 */
@RestController
@RequestMapping("/api/module")
public class MetadataModuleController {

    @Autowired
    private MetadataModuleService moduleService;

    /**
     * 新增模块
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, Object> params) {
        try {
            MetadataModule module = new MetadataModule();
            module.setModuleCode((String) params.get("moduleCode"));
            module.setModuleName((String) params.get("moduleName"));
            module.setModuleType((String) params.get("moduleType"));
            module.setDescription((String) params.get("description"));
            // 处理新增字段
            if (params.containsKey("sort")) {
                module.setSort(params.get("sort") != null ? Integer.valueOf(params.get("sort").toString()) : 1);
            }
            module.setIcon((String) params.get("icon"));
            module.setRoutePath((String) params.get("routePath"));
            module.setComponentPath((String) params.get("componentPath"));
            @SuppressWarnings("unchecked")
            List<String> tableCodes = (List<String>) params.get("tableCodes");
            moduleService.add(module, tableCodes);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新模块
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody Map<String, Object> params) {
        try {
            MetadataModule module = new MetadataModule();
            module.setId(Long.valueOf(params.get("id").toString()));
            module.setModuleName((String) params.get("moduleName"));
            module.setModuleType((String) params.get("moduleType"));
            module.setDescription((String) params.get("description"));
            // 处理新增字段
            if (params.containsKey("sort")) {
                module.setSort(params.get("sort") != null ? Integer.valueOf(params.get("sort").toString()) : 1);
            }
            module.setIcon((String) params.get("icon"));
            module.setRoutePath((String) params.get("routePath"));
            module.setComponentPath((String) params.get("componentPath"));
            @SuppressWarnings("unchecked")
            List<String> tableCodes = (List<String>) params.get("tableCodes");
            moduleService.update(module, tableCodes);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除模块
     */
    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            moduleService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询模块详情
     */
    @GetMapping("/{moduleCode}")
    public Result<MetadataModule> getByCode(@PathVariable String moduleCode) {
        MetadataModule module = moduleService.getByCode(moduleCode);
        return Result.success(module);
    }

    /**
     * 查询所有模块
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String moduleName,
                          @RequestParam(required = false) String moduleType,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Integer current,
                          @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataModule> pageResult = moduleService.page(moduleName, moduleType, status, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataModule> list = moduleService.list(moduleName, moduleType, status);
        return Result.success(list);
    }

    /**
     * 更新模块状态
     */
    @PostMapping("/updateStatus")
    public Result<?> updateStatus(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            Integer status = Integer.valueOf(params.get("status").toString());
            moduleService.updateStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

