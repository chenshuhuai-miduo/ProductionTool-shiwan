package com.miduo.cloud.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageImportPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 码包导入记录 Mapper
 */
@Mapper
public interface CodePackageImportMapper extends BaseMapper<CodePackageImportPO> {

    @Select("SELECT COUNT(1) FROM CodePackageImport " +
            "WHERE PackageType = #{packageType} AND FileName = #{fileName} AND Status = 1")
    Long countActiveByTypeAndFileName(@Param("packageType") Integer packageType,
                                      @Param("fileName") String fileName);

    @Select("SELECT MAX(CreateTime) FROM CodePackageImport " +
            "WHERE PackageType = #{packageType} AND ImportSource = 1 AND Status = 1")
    java.time.LocalDateTime selectLatestOnlineCreateTimeByType(@Param("packageType") Integer packageType);
}
