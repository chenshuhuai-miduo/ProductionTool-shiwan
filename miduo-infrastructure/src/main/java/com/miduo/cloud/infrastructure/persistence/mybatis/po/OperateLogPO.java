package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表实体类
 */
@Data
@TableName("OperateLog")
public class OperateLogPO {
    
    /**
     * 日志唯一标识（自增主键）
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作人姓名
     */
    @TableField("OperatorName")
    private String operatorName;
    
    /**
     * 操作人IP地址
     */
    @TableField("OperatorIp")
    private String operatorIp;
    
    /**
     * 操作模块
     */
    @TableField("ModuleName")
    private String moduleName;
    
    /**
     * 类型：1-操作日志  2-系统日志
     */
    @TableField("Type")
    private Integer type;
    
    /**
     * 操作类型（如：新增、修改、删除、查询等）
     */
    @TableField("OperateType")
    private String operateType;
    
    /**
     * 操作对象ID
     */
    @TableField("TargetId")
    private String targetId;
    
    /**
     * 操作对象名称
     */
    @TableField("TargetName")
    private String targetName;
    
    /**
     * 操作内容
     */
    @TableField("OperateContent")
    private String operateContent;
    
    /**
     * 操作前数据（JSON格式）
     */
    @TableField("BeforeData")
    private String beforeData;
    
    /**
     * 操作后数据（JSON格式）
     */
    @TableField("AfterData")
    private String afterData;
    
    /**
     * 操作结果（成功/失败）
     */
    @TableField("OperateResult")
    private String operateResult;
    
    /**
     * 失败原因
     */
    @TableField("FailReason")
    private String failReason;
    
    /**
     * 操作时间
     */
    @TableField("OperateTime")
    private LocalDateTime operateTime;
    
    /**
     * 操作设备
     */
    @TableField("DeviceInfo")
    private String deviceInfo;
    
    /**
     * 备注信息
     */
    @TableField("Remark")
    private String remark;
}

