package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产订单表实体类
 */
@Data
@TableName("ProductionOrder")
public class ProductionOrderPO {
    
    /**
     * 自增主键
     */
    @TableId(value = "Id", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 原订单表id
     */
    @TableField("SrcId")
    private Integer srcId;
    
    /**
     * 类型 (1: 有箱码, 2: 无箱码)
     */
    @TableField("Type")
    private Integer type;
    
    /**
     * 订单编号
     */
    @TableField("OrderNo")
    private String orderNo;
    
    /**
     * 生产总数量
     */
    @TableField("OrderSumCoun")
    private Integer orderSumCount;
    
    /**
     * 订单总数量
     */
    @TableField("ProductSumCount")
    private Integer productSumCount;
    
    /**
     * 制单日期
     */
    @TableField("DmakeDate")
    private LocalDateTime dmakeDate;
    
    /**
     * 订单状态
     * 0: 待生产
     * 1: 生产中（已启用）
     * 2: 已完成
     * 3: 未启用但有采集数据（显示为"生产中"但需要点击启用任务）
     * 5: 提前结单
     */
    @TableField("OrderStatus")
    private Integer orderStatus;
    
    /**
     * 产品来源类型 (0: 默认, 1: PDA, 2: 产线)
     */
    @TableField("ProductSourceType")
    private Integer productSourceType;
    
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
    
    /**
     * 所属生产线
     */
    @TableField("ProductionLine")
    private Integer productionLine;
}

