package com.miduo.cloud.frontend.util;

import com.miduo.cloud.frontend.service.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * 设备唯一码生成器
 * 基于硬件信息生成MD5哈希值作为设备唯一标识
 */
@Slf4j
public class DeviceUniqueIdGenerator {

    /**
     * 生成设备唯一码
     * 使用主板序列号 + CPU ID + 制造商 + BIOS UUID 四个参数组合进行MD5加密
     *
     * @param deviceInfo 设备信息
     * @return 32位MD5哈希值（小写，无分隔符）
     */
    public static String generateDeviceId(DeviceInfoService.DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }

        // 组合四个参数：主板序列号 + CPU ID + 制造商 + BIOS UUID
        String combinedInfo = deviceInfo.getBaseboardSerial() 
                + deviceInfo.getCpuId() 
                + deviceInfo.getManufacturer() 
                + deviceInfo.getBiosUuid();
        
        String deviceId = DigestUtils.md5Hex(combinedInfo);

        log.info("生成设备唯一码：{}（基于：主板={}, CPU={}, 制造商={}, BIOS UUID={}）",
                deviceId, deviceInfo.getBaseboardSerial(), deviceInfo.getCpuId(), 
                deviceInfo.getManufacturer(), deviceInfo.getBiosUuid());

        return deviceId;
    }

    /**
     * 生成设备唯一码（简化接口）
     *
     * @param baseboardSerial 主板序列号
     * @param cpuId CPU ID
     * @param manufacturer 制造商
     * @return 32位MD5哈希值
     */
    public static String generateDeviceId(String baseboardSerial, String cpuId, String manufacturer) {
        DeviceInfoService.DeviceInfo deviceInfo = DeviceInfoService.DeviceInfo.builder()
                .baseboardSerial(baseboardSerial)
                .cpuId(cpuId)
                .manufacturer(manufacturer)
                .biosUuid("UNKNOWN_BIOS_UUID")  // 简化接口无法获取BIOS UUID，使用默认值
                .build();

        return generateDeviceId(deviceInfo);
    }
}





















