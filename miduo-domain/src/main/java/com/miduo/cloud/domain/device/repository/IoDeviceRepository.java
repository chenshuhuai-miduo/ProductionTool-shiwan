package com.miduo.cloud.domain.device.repository;

import com.miduo.cloud.domain.device.model.IoDevice;

import java.util.List;
import java.util.Optional;

/**
 * IO设备仓储接口
 */
public interface IoDeviceRepository {
    
    /**
     * 保存设备
     */
    Integer save(IoDevice device);
    
    /**
     * 根据ID查询设备
     */
    Optional<IoDevice> findById(Integer id);
    
    /**
     * 查询所有设备
     */
    List<IoDevice> findAll();
    
    /**
     * 根据设备序号查询设备
     */
    Optional<IoDevice> findByDeviceIndex(Integer deviceIndex);
    
    /**
     * 根据类型查询设备
     */
    List<IoDevice> findByType(String deviceType);
    
    /**
     * 更新设备
     */
    boolean update(IoDevice device);
    
    /**
     * 删除设备
     */
    boolean deleteById(Integer id);
}

