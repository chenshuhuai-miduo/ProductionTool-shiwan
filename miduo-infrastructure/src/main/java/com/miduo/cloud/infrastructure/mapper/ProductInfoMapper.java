package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductInfoPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductInfoMapper extends BaseMapper<ProductInfoPO> {

    /**
     * 批量 upsert：INSERT ... ON DUPLICATE KEY UPDATE（依赖 ProductNo 唯一键）
     */
    int batchUpsert(List<ProductInfoPO> list);
}
