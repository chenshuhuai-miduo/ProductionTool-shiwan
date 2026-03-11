package com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产订单表实体类（致美斋产线）
 */
@Data
@TableName("ProductionOrder")
public class ProductionOrderPO {

    @TableId(value = "Id", type = IdType.AUTO)
    private Integer id;
    @TableField("SrcId")
    private Integer srcId;
    @TableField("Type")
    private Integer type;
    @TableField("OrderNo")
    private String orderNo;
    @TableField("OrderSumCoun")
    private Integer orderSumCount;
    @TableField("ProductSumCount")
    private Integer productSumCount;
    @TableField("DmakeDate")
    private LocalDateTime dmakeDate;
    @TableField("OrderStatus")
    private Integer orderStatus;
    @TableField("ProductSourceType")
    private Integer productSourceType;
    @TableField("CreateTime")
    private LocalDateTime createTime;
    @TableField(value = "LastModifyTime", exist = false)
    private byte[] lastModifyTime;
    @TableField("ProductionLine")
    private Integer productionLine;
}
