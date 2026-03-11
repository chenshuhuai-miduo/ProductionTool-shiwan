package com.miduo.cloud.infrastructure.mapper;

import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeAssociationTimePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 码关联时间查询 Mapper
 */
@Mapper
public interface CodePackageRelationMapper {

    @Select({
            "<script>",
            "SELECT CodeValue, MIN(AddTime) AS AssociatedAt",
            "FROM (",
            "<choose>",
            "<when test='packageType == 1'>",
            "SELECT SmallSerialNumber AS CodeValue, AddTime",
            "FROM CodeRelationUpload",
            "WHERE IsDel = 0",
            "AND SmallSerialNumber IS NOT NULL AND SmallSerialNumber != ''",
            "AND SmallSerialNumber IN",
            "<foreach collection='codeValues' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "</when>",
            "<when test='packageType == 2'>",
            "SELECT MediumSerialNumber AS CodeValue, AddTime",
            "FROM CodeRelationUpload",
            "WHERE IsDel = 0",
            "AND MediumSerialNumber IS NOT NULL AND MediumSerialNumber != ''",
            "AND MediumSerialNumber IN",
            "<foreach collection='codeValues' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "</when>",
            "<otherwise>",
            "SELECT BigSerialNumber AS CodeValue, AddTime",
            "FROM CodeRelationUpload",
            "WHERE IsDel = 0",
            "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''",
            "AND BigSerialNumber IN",
            "<foreach collection='codeValues' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "UNION ALL",
            "SELECT BiggerSerialNumber AS CodeValue, AddTime",
            "FROM CodeRelationUpload",
            "WHERE IsDel = 0",
            "AND BiggerSerialNumber IS NOT NULL AND BiggerSerialNumber != ''",
            "AND BiggerSerialNumber IN",
            "<foreach collection='codeValues' item='code' open='(' separator=',' close=')'>",
            "#{code}",
            "</foreach>",
            "</otherwise>",
            "</choose>",
            ") T",
            "GROUP BY CodeValue",
            "</script>"
    })
    List<CodeAssociationTimePO> selectAssociationTimeByCodes(@Param("packageType") Integer packageType,
                                                             @Param("codeValues") List<String> codeValues);
}
