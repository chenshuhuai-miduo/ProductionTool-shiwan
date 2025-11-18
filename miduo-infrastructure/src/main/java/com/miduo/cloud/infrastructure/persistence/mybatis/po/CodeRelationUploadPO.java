package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码关系上传表实体类
 */
@Data
@TableName("CodeRelationUpload")
public class CodeRelationUploadPO {
    
    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 跺标
     */
    @TableField("BiggerSerialNumber")
    private String biggerSerialNumber;
    
    /**
     * 大标（托盘码）
     */
    @TableField("BigSerialNumber")
    private String bigSerialNumber;
    
    /**
     * 中标
     */
    @TableField("MediumSerialNumber")
    private String mediumSerialNumber;
    
    /**
     * 小标（箱码）
     */
    @TableField("SmallSerialNumber")
    private String smallSerialNumber;
    
    /**
     * 动销码
     */
    @TableField("DxCode")
    private String dxCode;
    
    /**
     * 导购码
     */
    @TableField("SalesCode")
    private String salesCode;
    
    /**
     * 虚拟跺标
     */
    @TableField("VirtualSerialNumber")
    private String virtualSerialNumber;
    
    /**
     * 是否虚拟标签；0否；1是
     */
    @TableField("IsVirtual")
    private Integer isVirtual;
    
    /**
     * 产品编号
     */
    @TableField("ProductNO")
    private String productNo;
    
    /**
     * 订单编号
     */
    @TableField("OrderNo")
    private String orderNo;
    
    /**
     * 状态 0未完成 1已完成 2已解除
     */
    @TableField("Status")
    private Integer status;
    
    /**
     * 标签编号
     */
    @TableField("TagNo")
    private String tagNo;
    
    /**
     * 标签数量（托盘数量）
     */
    @TableField("Qty")
    private Integer qty;
    
    /**
     * 类型 1有箱码 2无箱码
     */
    @TableField("Type")
    private Integer type;
    
    /**
     * 仓库编号
     */
    @TableField("WarehouseNo")
    private String warehouseNo;
    
    /**
     * 批次号
     */
    @TableField("BatchNo")
    private String batchNo;
    
    /**
     * 添加时间
     */
    @TableField("AddTime")
    private LocalDateTime addTime;
    
    /**
     * 失败次数
     */
    @TableField("ErrCount")
    private Integer errCount;
    
    /**
     * 备注
     */
    @TableField("Msg")
    private String msg;
    
    /**
     * 上传时间
     */
    @TableField("UploadTime")
    private LocalDateTime uploadTime;
    
    /**
     * 是否同步（默认0）0同步 1不同步
     */
    @TableField("IsUpload")
    private Integer isUpload;
    
    /**
     * 是否删除
     */
    @TableField("IsDel")
    private Integer isDel;
    
    /**
     * 班组名称
     */
    @TableField("TeamName")
    private String teamName;
    
    /**
     * 最后修改时间戳（SQL Server timestamp/rowversion 类型，用于并发控制）
     * 该字段由数据库自动管理，使用 exist = false 完全排除该字段（不参与任何 SQL 操作）
     */
    @TableField(value = "LastModifyTime", exist = false)
    private byte[] lastModifyTime;

}

