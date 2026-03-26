package com.miduo.cloud.frontend.util;

import com.miduo.cloud.frontend.service.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * 设备唯一码生成器
 * 基于硬件信息生成MD5哈希值作为设备唯一标识
 */
@Slf4j
public class DeviceUniqueIdGenerator {

    /** 缓存文件路径：与许可证文件同目录，避免额外目录权限问题。 */
    private static final String CACHE_FILE_PATH;

    static {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home");
        }
        CACHE_FILE_PATH = appData + File.separator + "miduo"
                + File.separator + "license" + File.separator + "device_id.cache";
    }

    /**
     * 带文件缓存的设备唯一码获取。
     * <p>
     * 缓存命中时直接返回，跳过 OSHI 硬件采集（~1.5s）；
     * 缓存缺失时调用 {@code deviceInfoSupplier} 采集硬件信息，计算后写入缓存。
     * </p>
     *
     * @param deviceInfoSupplier 惰性设备信息提供者，缓存命中时不会被调用
     * @return 32位MD5设备唯一码
     */
    public static String generateDeviceIdCached(Supplier<DeviceInfoService.DeviceInfo> deviceInfoSupplier) {
        String cached = readCachedDeviceId();
        if (cached != null) {
            log.info("使用缓存设备唯一码：{}", cached);
            return cached;
        }

        log.info("缓存未命中，执行 OSHI 硬件采集...");
        String deviceId = generateDeviceId(deviceInfoSupplier.get());
        saveCachedDeviceId(deviceId);
        return deviceId;
    }

    /**
     * 清除设备 ID 缓存文件。
     * 设备硬件更换或许可证迁移后调用，下次启动将重新采集硬件信息。
     */
    public static void clearDeviceIdCache() {
        try {
            Path cachePath = Paths.get(CACHE_FILE_PATH);
            if (Files.exists(cachePath)) {
                Files.delete(cachePath);
                log.info("设备ID缓存已清除：{}", CACHE_FILE_PATH);
            }
        } catch (Exception e) {
            log.warn("清除设备ID缓存失败：{}", e.getMessage());
        }
    }

    /**
     * 生成设备唯一码（原始方法，直接使用传入的硬件信息，不读写缓存）。
     */
    public static String generateDeviceId(DeviceInfoService.DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }

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
     */
    public static String generateDeviceId(String baseboardSerial, String cpuId, String manufacturer) {
        DeviceInfoService.DeviceInfo deviceInfo = DeviceInfoService.DeviceInfo.builder()
                .baseboardSerial(baseboardSerial)
                .cpuId(cpuId)
                .manufacturer(manufacturer)
                .biosUuid("UNKNOWN_BIOS_UUID")
                .build();

        return generateDeviceId(deviceInfo);
    }

    private static String readCachedDeviceId() {
        try {
            Path cachePath = Paths.get(CACHE_FILE_PATH);
            if (!Files.exists(cachePath)) {
                return null;
            }
            String content = new String(Files.readAllBytes(cachePath), StandardCharsets.UTF_8).trim();
            if (content.matches("[0-9a-f]{32}")) {
                return content;
            }
            log.warn("设备ID缓存内容格式异常，将重新采集");
            return null;
        } catch (Exception e) {
            log.warn("读取设备ID缓存失败：{}", e.getMessage());
            return null;
        }
    }

    private static void saveCachedDeviceId(String deviceId) {
        try {
            Path cachePath = Paths.get(CACHE_FILE_PATH);
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, deviceId.getBytes(StandardCharsets.UTF_8));
            log.info("设备ID已缓存至：{}", CACHE_FILE_PATH);
        } catch (Exception e) {
            log.warn("保存设备ID缓存失败（不影响功能）：{}", e.getMessage());
        }
    }
}
