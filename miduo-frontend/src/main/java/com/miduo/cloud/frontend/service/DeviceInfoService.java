package com.miduo.cloud.frontend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * 设备信息采集服务
 * 使用OSHI库获取硬件信息，用于生成设备唯一码
 */
@Slf4j
@Service
public class DeviceInfoService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;

    public DeviceInfoService() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
    }

    /**
     * 获取主板序列号
     */
    public String getBaseboardSerial() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            if (computerSystem != null) {
                Baseboard baseboard = computerSystem.getBaseboard();
                if (baseboard != null) {
                    String serial = baseboard.getSerialNumber();
                    if (serial != null && !serial.trim().isEmpty()) {
                        return serial.trim();
                    }
                }
            }
            log.warn("无法获取主板序列号，返回默认值");
            return "UNKNOWN_BASEBOARD";
        } catch (Exception e) {
            log.error("获取主板序列号失败", e);
            return "UNKNOWN_BASEBOARD";
        }
    }

    /**
     * 获取CPU ID
     */
    public String getCpuId() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            if (processor != null) {
                String processorId = processor.getProcessorIdentifier().getProcessorID();
                if (processorId != null && !processorId.trim().isEmpty()) {
                    return processorId.trim();
                }
            }
            log.warn("无法获取CPU ID，返回默认值");
            return "UNKNOWN_CPU";
        } catch (Exception e) {
            log.error("获取CPU ID失败", e);
            return "UNKNOWN_CPU";
        }
    }

    /**
     * 获取制造商信息
     */
    public String getManufacturer() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            if (computerSystem != null) {
                String manufacturer = computerSystem.getManufacturer();
                if (manufacturer != null && !manufacturer.trim().isEmpty()) {
                    return manufacturer.trim();
                }
            }
            log.warn("无法获取制造商信息，返回默认值");
            return "UNKNOWN_MANUFACTURER";
        } catch (Exception e) {
            log.error("获取制造商信息失败", e);
            return "UNKNOWN_MANUFACTURER";
        }
    }

    /**
     * 获取设备型号
     */
    public String getDeviceModel() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            if (computerSystem != null) {
                String model = computerSystem.getModel();
                if (model != null && !model.trim().isEmpty()) {
                    return model.trim();
                }
            }
            log.warn("无法获取设备型号，返回默认值");
            return "UNKNOWN_MODEL";
        } catch (Exception e) {
            log.error("获取设备型号失败", e);
            return "UNKNOWN_MODEL";
        }
    }

    /**
     * 获取操作系统版本
     */
    public String getOsVersion() {
        try {
            String osVersion = System.getProperty("os.name") + " " + System.getProperty("os.version");
            return osVersion.trim();
        } catch (Exception e) {
            log.error("获取操作系统版本失败", e);
            return "UNKNOWN_OS";
        }
    }

    /**
     * 获取应用版本
     */
    public String getAppVersion() {
        try {
            // 从JAR清单或配置文件获取版本信息
            Package pkg = this.getClass().getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null) {
                return pkg.getImplementationVersion();
            }
            // 默认版本号
            return "1.0.0";
        } catch (Exception e) {
            log.error("获取应用版本失败", e);
            return "1.0.0";
        }
    }

    /**
     * 获取BIOS UUID
     */
    public String getBiosUuid() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            if (computerSystem != null) {
                String uuid = computerSystem.getHardwareUUID();
                if (uuid != null && !uuid.trim().isEmpty()) {
                    return uuid.trim();
                }
            }
            log.warn("无法获取BIOS UUID，返回默认值");
            return "UNKNOWN_BIOS_UUID";
        } catch (Exception e) {
            log.error("获取BIOS UUID失败", e);
            return "UNKNOWN_BIOS_UUID";
        }
    }

    /**
     * 获取完整的设备信息
     */
    public DeviceInfo getDeviceInfo() {
        return DeviceInfo.builder()
                .baseboardSerial(getBaseboardSerial())
                .cpuId(getCpuId())
                .manufacturer(getManufacturer())
                .deviceModel(getDeviceModel())
                .osVersion(getOsVersion())
                .appVersion(getAppVersion())
                .biosUuid(getBiosUuid())
                .build();
    }

    /**
     * 设备信息数据传输对象
     */
    public static class DeviceInfo {
        private final String baseboardSerial;
        private final String cpuId;
        private final String manufacturer;
        private final String deviceModel;
        private final String osVersion;
        private final String appVersion;
        private final String biosUuid;

        private DeviceInfo(Builder builder) {
            this.baseboardSerial = builder.baseboardSerial;
            this.cpuId = builder.cpuId;
            this.manufacturer = builder.manufacturer;
            this.deviceModel = builder.deviceModel;
            this.osVersion = builder.osVersion;
            this.appVersion = builder.appVersion;
            this.biosUuid = builder.biosUuid;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getBaseboardSerial() { return baseboardSerial; }
        public String getCpuId() { return cpuId; }
        public String getManufacturer() { return manufacturer; }
        public String getDeviceModel() { return deviceModel; }
        public String getOsVersion() { return osVersion; }
        public String getAppVersion() { return appVersion; }
        public String getBiosUuid() { return biosUuid; }

        public static class Builder {
            private String baseboardSerial;
            private String cpuId;
            private String manufacturer;
            private String deviceModel;
            private String osVersion;
            private String appVersion;
            private String biosUuid;

            public Builder baseboardSerial(String baseboardSerial) {
                this.baseboardSerial = baseboardSerial;
                return this;
            }

            public Builder cpuId(String cpuId) {
                this.cpuId = cpuId;
                return this;
            }

            public Builder manufacturer(String manufacturer) {
                this.manufacturer = manufacturer;
                return this;
            }

            public Builder deviceModel(String deviceModel) {
                this.deviceModel = deviceModel;
                return this;
            }

            public Builder osVersion(String osVersion) {
                this.osVersion = osVersion;
                return this;
            }

            public Builder appVersion(String appVersion) {
                this.appVersion = appVersion;
                return this;
            }

            public Builder biosUuid(String biosUuid) {
                this.biosUuid = biosUuid;
                return this;
            }

            public DeviceInfo build() {
                return new DeviceInfo(this);
            }
        }
    }
}





















