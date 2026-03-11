package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码包上次拉取时间
 */
@Data
@TableName("CodePackagePullTime")
public class CodePackagePullTimePO {

    @TableId(value = "Id", type = IdType.AUTO)
    private Long id;

    @TableField("PackageType")
    private Integer packageType;

    @TableField("LastPullTime")
    private LocalDateTime lastPullTime;

    @TableField("UpdateTime")
    private LocalDateTime updateTime;
}
