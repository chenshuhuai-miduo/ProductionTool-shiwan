package com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产订单表 Mapper（致美斋产线）
 */
@Mapper
public interface ProductionOrderMapper extends BaseMapper<ProductionOrderPO> {
}
