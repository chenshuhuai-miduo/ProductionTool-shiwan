package com.miduo.cloud.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    
    /**
     * 统计指定订单和产品的不同TagNo数量（已生产垛数）
     * 使用COUNT(DISTINCT)在数据库层面直接计数，性能优化
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @return 不同TagNo的数量
     */
    @Select("SELECT COUNT(DISTINCT TagNo) FROM CodeRelationUpload " +
            "WHERE OrderNo = #{orderNo} AND ProductNO = #{productNo} " +
            "AND IsDel = 0 AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''")
    Long countDistinctTagNoByOrderAndProduct(@Param("orderNo") String orderNo, @Param("productNo") String productNo);
    
    /**
     * 查询指定订单和产品的最新一条记录（按AddTime降序）
     * 使用TOP 1确保只查询一条数据，性能最优
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @return 最新的一条记录，如果没有则返回null
     */
    @Select("SELECT TOP 1 * FROM CodeRelationUpload " +
            "WHERE OrderNo = #{orderNo} AND ProductNO = #{productNo} AND IsDel = 0 " +
            "ORDER BY AddTime DESC")
    CodeRelationPO selectLatestByOrderAndProduct(@Param("orderNo") String orderNo, @Param("productNo") String productNo);
}

