package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产订单明细表实体类
 */
@Data
@TableName("ProductionOrderDetail")
public class ProductionOrderDetailPO {
    
    /**
     * 自增主键
     */
    @TableId(value = "Id", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 已生产数量
     */
    @TableField("OrderCount")
    private Integer orderCount;
    
    /**
     * 计划订单数量
     */
    @TableField("ProductCount")
    private Integer productCount;
    
    /**
     * 产品ID
     */
    @TableField("ProductID")
    private Integer productId;
    
    /**
     * 产品名称
     */
    @TableField("ProductName")
    private String productName;
    
    /**
     * 产品编号
     */
    @TableField("ProductNO")
    private String productNo;
    
    /**
     * 规格ID
     */
    @TableField("ProductForMatID")
    private Integer productFormatId;
    
    /**
     * 规格名称
     */
    @TableField("ProductForMatName")
    private String productFormatName;
    
    /**
     * 订单编号
     */
    @TableField("OrderNo")
    private String orderNo;
    
    /**
     * 订单ID
     */
    @TableField("OrderId")
    private Integer orderId;
    
    /**
     * 批次信息
     */
    @TableField("SyBatchNo")
    private String syBatchNo;
    
    /**
     * 类型 1 有箱码 2 无箱码
     */
    @TableField("Type")
    private Integer type;
    
    /**
     * 拖箱比例
     */
    @TableField("Ratio")
    private Integer ratio;
    
    /**
     * 是否删除 1删除
     */
    @TableField("IsDel")
    private Integer isDel;
    
    /**
     * 添加时间
     */
    @TableField("AddTime")
    private LocalDateTime addTime;
    
    /**
     * 生产时间 (产线设置)
     */
    @TableField("ProductTime")
    private LocalDateTime productTime;
    
    /**
     * 预计完工时间 (产线设置)
     */
    @TableField("TwillendTime")
    private LocalDateTime twillendTime;
    
    /**
     * 创建日期
     */
    @TableField("CreateTime")
    private LocalDateTime createTime;
    
    /**
     * 最后修改时间（SQL Server timestamp 类型，由数据库自动管理）
     * 使用 exist = false 完全排除该字段（不参与任何 SQL 操作）
     */
    @TableField(value = "LastModifyTime", exist = false)
    private byte[] lastModifyTime;

}

