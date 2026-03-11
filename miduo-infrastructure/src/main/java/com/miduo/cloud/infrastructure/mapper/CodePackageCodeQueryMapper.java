package com.miduo.cloud.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 码包全码查询 Mapper（基于 AllCodePackageCodes 视图）
 */
@Mapper
public interface CodePackageCodeQueryMapper {

    @Select({
            "<script>",
            "SELECT CodeValue FROM AllCodePackageCodes",
            "WHERE PackageType = #{packageType}",
            "AND CodeValue IN",
            "<foreach collection='codeValues' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "</script>"
    })
    List<String> selectExistingCodeValues(@Param("packageType") Integer packageType,
                                          @Param("codeValues") List<String> codeValues);
}
