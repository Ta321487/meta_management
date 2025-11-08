package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataModuleType;
import com.metadata.service.MetadataModuleTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块类型控制器
 */
@RestController
@RequestMapping("/api/moduleType")
public class MetadataModuleTypeController {

    @Autowired
    private MetadataModuleTypeService typeService;

    @GetMapping("/list")
    public Result<List<MetadataModuleType>> listAll() {
        List<MetadataModuleType> list = typeService.listAll();
        return Result.success(list);
    }
}

