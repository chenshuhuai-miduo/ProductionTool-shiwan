package com.miduo.cloud.entity.dto.operatelog;

import com.miduo.cloud.common.dto.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志查询条件DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperateLogQueryDTO extends PageInput {
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 操作人姓名（模糊查询）
     */
    private String operatorName;
    
    /**
     * 操作模块（模糊查询）
     */
    private String moduleName;
    
    /**
     * 操作类型
     */
    private String operateType;
}

