package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageItemHotPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 码包热码 Mapper
 */
@Mapper
public interface CodePackageItemHotMapper extends BaseMapper<CodePackageItemHotPO> {

    @Insert({
            "<script>",
            "INSERT INTO CodePackageItemHot (ImportId, PackageType, CodeValue, CreateTime) VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.importId}, #{item.packageType}, #{item.codeValue}, #{item.createTime})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<CodePackageItemHotPO> list);

    @Select("SELECT CodeValue FROM CodePackageItemHot WHERE ImportId = #{importId}")
    List<String> selectCodeValuesByImportId(@Param("importId") Long importId);

    @Delete("DELETE FROM CodePackageItemHot WHERE ImportId = #{importId}")
    int deleteByImportId(@Param("importId") Long importId);
}
