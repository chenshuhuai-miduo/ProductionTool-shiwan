package com.miduo.cloud.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 码关系表Mapper接口
 */
@Mapper
public interface CodeRelationMapper extends BaseMapper<CodeRelationPO> {
    
    /**
     * 批量插入码关联记录
     * @param list 码关联列表
     * @return 插入的记录数
     */
    int insertBatch(@Param("list") List<CodeRelationPO> list);
    
    /**
     * 批量更新托盘码
     * @param ids 记录ID列表
     * @param palletCode 托盘码
     * @return 更新的记录数
     */
    int batchUpdatePalletCode(@Param("ids") List<Integer> ids, @Param("palletCode") String palletCode);
}

