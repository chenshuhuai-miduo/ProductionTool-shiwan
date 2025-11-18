package com.miduo.cloud.entity.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
public class OperateLog {
    
    /**
     * 日志唯一标识（自增主键）
     */
    private Long id;
    
    /**
     * 操作人姓名
     */
    private String operatorName;
    
    /**
     * 操作人IP地址
     */
    private String operatorIp;
    
    /**
     * 操作模块
     */
    private String moduleName;
    
    /**
     * 类型：1-操作日志  2-系统日志
     */
    private Integer type;
    
    /**
     * 操作类型（如：新增、修改、删除、查询等）
     */
    private String operateType;
    
    /**
     * 操作对象ID
     */
    private String targetId;
    
    /**
     * 操作对象名称
     */
    private String targetName;
    
    /**
     * 操作内容
     */
    private String operateContent;
    
    /**
     * 操作前数据（JSON格式）
     */
    private String beforeData;
    
    /**
     * 操作后数据（JSON格式）
     */
    private String afterData;
    
    /**
     * 操作结果（成功/失败）
     */
    private String operateResult;
    
    /**
     * 失败原因
     */
    private String failReason;
    
    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
    
    /**
     * 操作设备
     */
    private String deviceInfo;
    
    /**
     * 备注信息
     */
    private String remark;
}

