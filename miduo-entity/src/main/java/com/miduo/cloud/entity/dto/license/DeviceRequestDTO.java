package com.miduo.cloud.entity.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备请求DTO
 * 用于生成设备请求文件(.devreq)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequestDTO {

    /**
     * 文件类型标识
     */
    private String fileType = "DEVICE_ACTIVATION_REQUEST";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 设备信息
     */
    private DeviceInfo deviceInfo;

    /**
     * 请求类型
     */
    private String requestType = "activation";

    /**
     * MD5校验和
     */
    private String checksum;

    /**
     * 设备信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        /**
         * 设备唯一ID（MD5哈希值）
         */
        private String deviceId;

        /**
         * 主板序列号
         */
        private String baseboardSerial;

        /**
         * CPU ID
         */
        private String cpuId;

        /**
         * 制造商
         */
        private String manufacturer;

        /**
         * 设备型号
         */
        private String deviceModel;

        /**
         * 操作系统版本
         */
        private String osVersion;

        /**
         * 应用版本
         */
        private String appVersion;

        /**
         * BIOS UUID
         */
        private String biosUuid;
    }
}





















