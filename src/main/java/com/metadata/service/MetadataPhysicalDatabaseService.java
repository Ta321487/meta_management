package com.metadata.service;

import com.metadata.entity.MetadataPhysicalDatabase;

import java.util.List;

public interface MetadataPhysicalDatabaseService {

    List<MetadataPhysicalDatabase> listAll();

    void add(MetadataPhysicalDatabase row);

    void update(MetadataPhysicalDatabase row);

    void delete(Long id, boolean dropOnInstance);

    /** 对已登记库在实例上执行 CREATE IF NOT EXISTS */
    void syncToInstance(Long id);
}
