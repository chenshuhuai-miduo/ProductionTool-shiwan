package com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码关系表实体类（致美斋产线）
 */
@Data
@TableName("CodeRelationUpload")
public class CodeRelationPO {

    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;
    @TableField("BiggerSerialNumber")
    private String biggerSerialNumber;
    @TableField("BigSerialNumber")
    private String bigSerialNumber;
    @TableField("MediumSerialNumber")
    private String mediumSerialNumber;
    @TableField("SmallSerialNumber")
    private String smallSerialNumber;
    @TableField("DxCode")
    private String dxCode;
    @TableField("SalesCode")
    private String salesCode;
    @TableField("VirtualSerialNumber")
    private String virtualSerialNumber;
    @TableField("IsVirtual")
    private Integer isVirtual;
    @TableField("ProductNO")
    private String productNo;
    @TableField("OrderNo")
    private String orderNo;
    @TableField("Status")
    private Integer status;
    @TableField("TagNo")
    private String tagNo;
    @TableField("Qty")
    private Integer qty;
    @TableField("Type")
    private Integer type;
    @TableField("WarehouseNo")
    private String warehouseNo;
    @TableField("BatchNo")
    private String batchNo;
    @TableField("AddTime")
    private LocalDateTime addTime;
    @TableField("ErrCount")
    private Integer errCount;
    @TableField("Msg")
    private String msg;
    @TableField("UploadTime")
    private LocalDateTime uploadTime;
    @TableField("IsUpload")
    private Integer isUpload;
    @TableField("IsDel")
    private Integer isDel;
    @TableField("TeamName")
    private String teamName;
    @TableField(value = "LastModifyTime", exist = false)
    private byte[] lastModifyTime;
}
