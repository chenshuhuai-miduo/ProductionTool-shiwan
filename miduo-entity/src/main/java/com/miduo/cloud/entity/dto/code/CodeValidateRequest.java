package com.miduo.cloud.entity.dto.code;

/**
 * 码校验请求
 * 第一台设备：串口通信，只做校验，不存数据库
 */
public class CodeValidateRequest {
    
    /**
     * 设备ID
     */
    private Integer deviceId;
    
    /**
     * 接收到的码
     */
    private String code;
    
    public CodeValidateRequest() {
    }
    
    public CodeValidateRequest(Integer deviceId, String code) {
        this.deviceId = deviceId;
        this.code = code;
    }
    
    public Integer getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    @Override
    public String toString() {
        return "CodeValidateRequest{" +
                "deviceId=" + deviceId +
                ", code='" + code + '\'' +
                '}';
    }
}

