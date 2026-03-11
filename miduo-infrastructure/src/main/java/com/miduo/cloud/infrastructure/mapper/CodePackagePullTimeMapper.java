package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackagePullTimePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 码包拉取时间 Mapper
 */
@Mapper
public interface CodePackagePullTimeMapper extends BaseMapper<CodePackagePullTimePO> {
}
