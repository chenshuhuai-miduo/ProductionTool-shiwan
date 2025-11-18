package com.miduo.cloud.entity.dto.operatelog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO - 用于页面展示
 */
@Data
public class OperateLogVO {
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 操作人姓名
     */
    private String operatorName;
    
    /**
     * 操作模块
     */
    private String moduleName;
    
    /**
     * 操作类型（如：新增、修改、删除、查询等）
     */
    private String operateType;
    
    /**
     * 操作内容
     */
    private String operateContent;
    
    /**
     * 操作结果（成功/失败）
     */
    private String operateResult;
    
    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
    
    /**
     * 备注信息
     */
    private String remark;
}

