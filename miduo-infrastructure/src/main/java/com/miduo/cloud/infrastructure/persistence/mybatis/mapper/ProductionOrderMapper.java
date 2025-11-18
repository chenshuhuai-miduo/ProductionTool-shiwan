package com.miduo.cloud.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产订单表Mapper接口
 */
@Mapper
public interface ProductionOrderMapper extends BaseMapper<ProductionOrderPO> {
    
}

