package com.miduo.cloud.entity.dto.device;

import lombok.Data;

/**
 * IO设备DTO
 */
@Data
public class IoDeviceDTO {
    
    /**
     * 设备ID（使用设备名称作为唯一标识）
     */
    private String id;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 设备类别（码校验、箱码采集、托盘码关联、箱码关联、报警器）
     */
    private String deviceCategory;
    
    /**
     * 连接方式（网口/串口）
     */
    private String connectionType;
    
    /**
     * 协议类型（TCP/UDP，仅网口有效）
     */
    private String protocolType;
    
    /**
     * IP地址或串口号
     */
    private String address;
    
    /**
     * 端口号或波特率
     */
    private String port;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 设备描述
     */
    private String description;
    
    /**
     * 设备状态文本
     */
    private String statusText;
}

