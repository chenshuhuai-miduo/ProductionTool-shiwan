package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 2号机本地产品信息表
 */
@Data
@TableName("ProductInfo")
public class ProductInfoPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 远端产品id，可选 */
    @TableField("ProductId")
    private Integer productId;

    /** 产品名称 */
    @TableField("ProductName")
    private String productName;

    /** 产品编号（唯一） */
    @TableField("ProductNo")
    private String productNo;

    /** 条码，可选 */
    @TableField("Barcode")
    private String barcode;

    /** 规格/描述，可选 */
    @TableField("Spec")
    private String spec;

    @TableField("CreatedAt")
    private LocalDateTime createdAt;

    @TableField("UpdatedAt")
    private LocalDateTime updatedAt;
}
