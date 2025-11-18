package com.miduo.cloud.domain.repository;

import com.miduo.cloud.entity.po.OperateLog;

/**
 * 操作日志仓储接口
 */
public interface OperateLogRepository {
    
    /**
     * 插入操作日志
     * 
     * @param operateLog 操作日志对象
     * @return 插入成功返回1，失败返回0
     */
    int insert(OperateLog operateLog);
}

