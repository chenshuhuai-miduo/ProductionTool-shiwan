package com.miduo.cloud.domain.device.model;

import lombok.Data;

/**
 * IO设备领域模型
 * 代表产线上的一个IO设备（串口、TCP、UDP）
 */
@Data
public class IoDevice {
    
    private Integer id;
    private Integer deviceIndex;         // 设备序号
    private String deviceName;           // 设备名称
    private String deviceType;           // 设备类型 (SERIAL/TCP_CLIENT/UDP)
    private String connectionParam;      // 连接参数（端口号或IP:Port）
    private Integer status;              // 状态 (0:未连接, 1:已连接)
    
    // ===== 业务方法 =====
    
    /**
     * 判断是否为串口设备
     */
    public boolean isSerialDevice() {
        return "SERIAL".equalsIgnoreCase(this.deviceType);
    }
    
    /**
     * 判断是否为TCP客户端
     */
    public boolean isTcpClient() {
        return "TCP_CLIENT".equalsIgnoreCase(this.deviceType);
    }
    
    /**
     * 判断是否为UDP设备
     */
    public boolean isUdpDevice() {
        return "UDP".equalsIgnoreCase(this.deviceType);
    }
    
    /**
     * 判断设备是否已连接
     */
    public boolean isConnected() {
        return Integer.valueOf(1).equals(this.status);
    }
    
    /**
     * 连接设备
     */
    public void connect() {
        this.status = 1;
    }
    
    /**
     * 断开设备
     */
    public void disconnect() {
        this.status = 0;
    }
    
    /**
     * 验证设备配置
     */
    public boolean validate() {
        return this.deviceName != null && !this.deviceName.isEmpty()
            && this.deviceType != null && !this.deviceType.isEmpty()
            && this.connectionParam != null && !this.connectionParam.isEmpty();
    }
}

