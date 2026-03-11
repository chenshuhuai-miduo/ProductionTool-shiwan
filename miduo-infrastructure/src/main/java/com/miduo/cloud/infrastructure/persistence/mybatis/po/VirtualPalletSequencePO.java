package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 虚拟垛标序号表（石湾 2 号机成垛时按日期+产线号递增）。
 * 表名 PascalCase：VirtualPalletSequence
 */
@Data
@TableName("VirtualPalletSequence")
public class VirtualPalletSequencePO {

    @TableField("SeqDate")
    private LocalDate seqDate;

    @TableField("LineCode")
    private String lineCode;

    @TableField("CurrentSeq")
    private Integer currentSeq;

    @TableField("UpdateTime")
    private LocalDateTime updateTime;
}
