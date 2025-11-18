package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.entity.po.OperateLog;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.OperateLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface OperateLogMapper extends BaseMapper<OperateLogPO> {
    
    /**
     * 插入操作日志
     * 
     * @param operateLog 操作日志对象
     * @return 插入成功返回1，失败返回0
     */
    int insert(OperateLog operateLog);
}

