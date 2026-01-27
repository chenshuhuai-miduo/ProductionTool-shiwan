package com.miduo.cloud.entity.dto.license;

    public class DeviceInfo {

        /**
         * 设备指纹ID（可与外层 deviceId 一致或冗余）
         */
        private String deviceId;

        /**
         * 设备型号
         */
        private String deviceModel;

        /**
         * 主板序列号
         */
        private String baseboardSerial;

        /**
         * CPU ID
         */
        private String cpuId;

        /**
         * 设备厂商
         */
        private String manufacturer;

        /**
         * 操作系统版本
         */
        private String osVersion;

        /**
         * 应用版本
         */
        private String appVersion;

        // ===== getter / setter 省略 =====
    }
