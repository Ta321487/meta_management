package com.metadata.service;

import com.metadata.entity.MetadataPhysicalDatabase;

import java.util.List;

public interface MetadataPhysicalDatabaseService {

    List<MetadataPhysicalDatabase> listAll(Boolean includeDisabled);

    default List<MetadataPhysicalDatabase> listAll() {
        return listAll(false);
    }

    void add(MetadataPhysicalDatabase row);

    void update(MetadataPhysicalDatabase row);

    void delete(Long id, boolean dropOnInstance);

    /** 对已登记库在实例上执行 CREATE IF NOT EXISTS */
    void syncToInstance(Long id);
}
