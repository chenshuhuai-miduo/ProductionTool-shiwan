package com.miduo.cloud.entity.dto.upload;

import lombok.Data;

/**
 * 实时上传区数据VO
 */
@Data
public class RealTimeUploadVO {
    
    /**
     * 生产订单号
     */
    private String orderNo;
    
    /**
     * 箱数（BigSerialNumber有值的记录数）
     */
    private Integer boxCount;
    
    /**
     * 上传状态
     */
    private String uploadStatus;
    
    /**
     * 是否为当前选中订单
     */
    private Boolean isSelected;
}

