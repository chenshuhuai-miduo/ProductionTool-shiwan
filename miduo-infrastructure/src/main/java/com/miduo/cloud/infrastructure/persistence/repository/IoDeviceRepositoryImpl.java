package com.miduo.cloud.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.miduo.cloud.domain.device.model.IoDevice;
import com.miduo.cloud.domain.device.repository.IoDeviceRepository;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.IoDeviceMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.IoDevicePO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * IO设备仓储实现类
 * 实现领域层定义的IoDeviceRepository接口
 */
@Repository
public class IoDeviceRepositoryImpl implements IoDeviceRepository {
    
    @Autowired
    private IoDeviceMapper ioDeviceMapper;
    
    @Override
    public Integer save(IoDevice device) {
        IoDevicePO po = convertToPO(device);
        ioDeviceMapper.insert(po);
        return po.getId();
    }
    
    @Override
    public Optional<IoDevice> findById(Integer id) {
        IoDevicePO po = ioDeviceMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToModel);
    }
    
    @Override
    public List<IoDevice> findAll() {
        List<IoDevicePO> poList = ioDeviceMapper.selectList(null);
        return poList.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<IoDevice> findByDeviceIndex(Integer deviceIndex) {
        QueryWrapper<IoDevicePO> wrapper = new QueryWrapper<>();
        wrapper.eq("DeviceIndex", deviceIndex);
        
        IoDevicePO po = ioDeviceMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::convertToModel);
    }
    
    @Override
    public List<IoDevice> findByType(String deviceType) {
        QueryWrapper<IoDevicePO> wrapper = new QueryWrapper<>();
        wrapper.eq("DeviceType", deviceType);
        
        List<IoDevicePO> poList = ioDeviceMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean update(IoDevice device) {
        if (device.getId() == null) {
            return false;
        }
        IoDevicePO po = convertToPO(device);
        return ioDeviceMapper.updateById(po) > 0;
    }
    
    @Override
    public boolean deleteById(Integer id) {
        return ioDeviceMapper.deleteById(id) > 0;
    }
    
    /**
     * 将领域模型转换为持久化对象
     */
    private IoDevicePO convertToPO(IoDevice model) {
        if (model == null) {
            return null;
        }
        IoDevicePO po = new IoDevicePO();
        BeanUtils.copyProperties(model, po);
        return po;
    }
    
    /**
     * 将持久化对象转换为领域模型
     */
    private IoDevice convertToModel(IoDevicePO po) {
        if (po == null) {
            return null;
        }
        IoDevice model = new IoDevice();
        BeanUtils.copyProperties(po, model);
        return model;
    }
}

