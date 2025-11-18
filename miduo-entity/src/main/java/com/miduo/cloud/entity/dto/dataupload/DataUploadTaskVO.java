package com.miduo.cloud.entity.dto.dataupload;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据上传页面-生产任务数据VO
 */
@Data
public class DataUploadTaskVO {
    
    /**
     * 第一层(箱码) - SmallSerialNumber
     */
    private String layer1;
    
    /**
     * 箱码采集时间 - AddTime
     */
    private LocalDateTime addTime;
    
    /**
     * 第二层(中标) - MediumSerialNumber
     */
    private String layer2;
    
    /**
     * 第三层(托盘码) - BigSerialNumber
     */
    private String layer3;
    
    /**
     * 第四层(跺标) - BiggerSerialNumber
     */
    private String layer4;
}

