package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码包导入记录
 */
@Data
@TableName("CodePackageImport")
public class CodePackageImportPO {

    @TableId(value = "Id", type = IdType.AUTO)
    private Long id;

    @TableField("PackageType")
    private Integer packageType;

    @TableField("PackageName")
    private String packageName;

    @TableField("ImportTime")
    private LocalDateTime importTime;

    @TableField("ImportSource")
    private Integer importSource;

    @TableField("CodeCount")
    private Integer codeCount;

    @TableField("Status")
    private Integer status;

    @TableField("Remark")
    private String remark;

    @TableField("FileName")
    private String fileName;

    @TableField("CreateTime")
    private LocalDateTime createTime;

    @TableField("UpdateTime")
    private LocalDateTime updateTime;
}
