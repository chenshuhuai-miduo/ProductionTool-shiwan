package com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产订单明细表实体类（致美斋产线）
 */
@Data
@TableName("ProductionOrderDetail")
public class ProductionOrderDetailPO {

    @TableId(value = "Id", type = IdType.AUTO)
    private Integer id;
    @TableField("OrderCount")
    private Integer orderCount;
    @TableField("ProductCount")
    private Integer productCount;
    @TableField("ProductID")
    private Integer productId;
    @TableField("ProductName")
    private String productName;
    @TableField("ProductNO")
    private String productNo;
    @TableField("ProductForMatID")
    private Integer productFormatId;
    @TableField("ProductForMatName")
    private String productFormatName;
    @TableField("OrderNo")
    private String orderNo;
    @TableField("OrderId")
    private Integer orderId;
    @TableField("SyBatchNo")
    private String syBatchNo;
    @TableField("Type")
    private Integer type;
    @TableField("Ratio")
    private Integer ratio;
    @TableField("IsDel")
    private Integer isDel;
    @TableField("AddTime")
    private LocalDateTime addTime;
    @TableField("ProductTime")
    private LocalDateTime productTime;
    @TableField("TwillendTime")
    private LocalDateTime twillendTime;
    @TableField("CreateTime")
    private LocalDateTime createTime;
    @TableField(value = "LastModifyTime", exist = false)
    private byte[] lastModifyTime;
}
