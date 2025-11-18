package com.miduo.cloud.infrastructure.repository;

import com.miduo.cloud.domain.repository.OperateLogRepository;
import com.miduo.cloud.entity.po.OperateLog;
import com.miduo.cloud.infrastructure.mapper.OperateLogMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * 操作日志仓储实现类
 */
@Repository
public class OperateLogRepositoryImpl implements OperateLogRepository {
    
    @Resource
    private OperateLogMapper operateLogMapper;
    
    @Override
    public int insert(OperateLog operateLog) {
        return operateLogMapper.insert(operateLog);
    }
}

