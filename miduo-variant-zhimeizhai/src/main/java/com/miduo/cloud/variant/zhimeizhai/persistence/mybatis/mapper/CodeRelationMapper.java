package com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.CodeRelationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 码关系表 Mapper（致美斋产线）
 */
@Mapper
public interface CodeRelationMapper extends BaseMapper<CodeRelationPO> {

    int insertBatch(@Param("list") List<CodeRelationPO> list);

    int batchUpdatePalletCode(@Param("ids") List<Integer> ids, @Param("palletCode") String palletCode);

    @Select("SELECT COUNT(DISTINCT TagNo) FROM CodeRelationUpload " +
            "WHERE OrderNo = #{orderNo} AND ProductNO = #{productNo} " +
            "AND IsDel = 0 AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''")
    Long countDistinctTagNoByOrderAndProduct(@Param("orderNo") String orderNo, @Param("productNo") String productNo);

    @Select("SELECT TOP 1 * FROM CodeRelationUpload " +
            "WHERE OrderNo = #{orderNo} AND ProductNO = #{productNo} AND IsDel = 0 " +
            "ORDER BY AddTime DESC")
    CodeRelationPO selectLatestByOrderAndProduct(@Param("orderNo") String orderNo, @Param("productNo") String productNo);
}
