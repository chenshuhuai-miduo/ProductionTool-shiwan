package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码包热数据
 */
@Data
@TableName("CodePackageItemHot")
public class CodePackageItemHotPO {

    @TableId(value = "Id", type = IdType.AUTO)
    private Long id;

    @TableField("ImportId")
    private Long importId;

    @TableField("PackageType")
    private Integer packageType;

    @TableField("CodeValue")
    private String codeValue;

    @TableField("CreateTime")
    private LocalDateTime createTime;
}
