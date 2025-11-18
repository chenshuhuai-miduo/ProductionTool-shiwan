package com.miduo.cloud.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.IoDevicePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IO设备Mapper接口
 */
@Mapper
public interface IoDeviceMapper extends BaseMapper<IoDevicePO> {
    
}

