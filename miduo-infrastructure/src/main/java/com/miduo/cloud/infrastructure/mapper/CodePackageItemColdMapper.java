package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageItemColdPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 码包冷码 Mapper
 */
@Mapper
public interface CodePackageItemColdMapper extends BaseMapper<CodePackageItemColdPO> {

    @Select("SELECT COUNT(1) FROM CodePackageItemCold WHERE ImportId = #{importId}")
    Long countByImportId(@Param("importId") Long importId);
}
