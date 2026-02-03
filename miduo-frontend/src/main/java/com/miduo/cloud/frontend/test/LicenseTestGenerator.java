package com.miduo.cloud.frontend.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miduo.cloud.entity.dto.license.LicenseFile;
import com.miduo.cloud.entity.dto.license.LicenseHeader;
import com.miduo.cloud.entity.dto.license.LicensePayload;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.util.DeviceRequestFileUtil;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.LicenseCryptoUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;

/**
 * 许可证测试生成器
 * 用于生成测试用的许可证文件
 */
public class LicenseTestGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static void main(String[] args) {
        try {
            System.out.println("=== 许可证测试生成器 ===");

            // 1. 获取设备信息
            System.out.println("\n1. 获取设备信息...");
            DeviceInfoService deviceInfoService = new DeviceInfoService();
            DeviceInfoService.DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo();

            System.out.println("设备信息:");
            System.out.println("  主板序列号: " + deviceInfo.getBaseboardSerial());
            System.out.println("  CPU ID: " + deviceInfo.getCpuId());
            System.out.println("  制造商: " + deviceInfo.getManufacturer());
            System.out.println("  设备型号: " + deviceInfo.getDeviceModel());
            System.out.println("  操作系统: " + deviceInfo.getOsVersion());
            System.out.println("  应用版本: " + deviceInfo.getAppVersion());

            // 2. 生成设备唯一码
            System.out.println("\n2. 生成设备唯一码...");
            String deviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfo);
            System.out.println("设备ID: " + deviceId);

            // 3. 生成设备请求文件
            System.out.println("\n3. 生成设备请求文件...");
            String requestFilePath = "test_device_request.devreq";
            DeviceRequestFileUtil.generateDeviceRequestFile(deviceInfo, requestFilePath);
            System.out.println("设备请求文件已生成: " + requestFilePath);

            // 读取并显示请求文件内容
            String content = new String(Files.readAllBytes(Paths.get(requestFilePath)));
            System.out.println("请求文件内容: " + content.substring(0, Math.min(100, content.length())) + "...");

            // 4. 生成试用许可证
            System.out.println("\n4. 生成试用许可证...");
            generateTrialLicense(deviceId);

            // 5. 生成正式许可证
            System.out.println("\n5. 生成正式许可证...");
            generateStandardLicense(deviceId);

            System.out.println("\n=== 测试文件生成完成 ===");
            System.out.println("生成的文件:");
            System.out.println("  - " + requestFilePath);
            System.out.println("  - trial_license.lic");
            System.out.println("  - standard_license.lic");

        } catch (Exception e) {
            System.err.println("生成测试文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成试用许可证
     */
    private static void generateTrialLicense(String deviceId) throws Exception {
        // 试用期15天
        LocalDate activationDate = LocalDate.now();
        LocalDate expireDate = activationDate.plusDays(15);

        LicensePayload payload = LicensePayload.builder()
                .licenseKey("TRIAL-TEST-001")
                .licenseType("TRIAL")
                .deviceId(deviceId)
                //.activationDate(activationDate)
                .expireDate(expireDate)
                .validDays(15)
                //.features(Arrays.asList("all"))
                .build();

        LicenseHeader header = LicenseHeader.builder()
                .version("1.0")
                .type("TRIAL")
                .keyVersion("2025")
                .algorithm("AES256+RSA2048")
                .build();

        generateLicenseFile(header, payload, "trial_license.lic");
    }

    /**
     * 生成正式许可证
     */
    private static void generateStandardLicense(String deviceId) throws Exception {
        // 正式版365天
        LocalDate activationDate = LocalDate.now();
        LocalDate expireDate = activationDate.plusDays(365);

        LicensePayload payload = LicensePayload.builder()
                .licenseKey("STD-TEST-001")
                .licenseType("STANDARD")
                .deviceId(deviceId)
                //.activationDate(activationDate)
                .expireDate(expireDate)
                .validDays(365)
                //.features(Arrays.asList("all"))
                .build();

        LicenseHeader header = LicenseHeader.builder()
                .version("1.0")
                .type("STANDARD")
                .keyVersion("2025")
                .algorithm("AES256+RSA2048")
                .build();

        generateLicenseFile(header, payload, "standard_license.lic");
    }

    /**
     * 生成许可证文件
     */
    private static void generateLicenseFile(LicenseHeader header, LicensePayload payload, String fileName) throws Exception {
        // 1. 将payload转换为JSON字符串
        String payloadJson = objectMapper.writeValueAsString(payload);

        // 2. 获取AES密钥并加密payload
        byte[] aesKey = LicenseCryptoUtil.getAESKey(payload.getDeviceId());
        byte[] encryptedPayload = encryptAES(payloadJson.getBytes(), aesKey);

        // 3. 创建许可证文件对象
        LicenseFile licenseFile = LicenseFile.builder()
                .header(header)
                .payload(Base64.getEncoder().encodeToString(encryptedPayload))
                .signature("TEST_SIGNATURE_" + System.currentTimeMillis()) // 测试签名
                .build();

        // 4. 转换为JSON并Base64编码
        String licenseJson = objectMapper.writeValueAsString(licenseFile);
        String encodedLicense = Base64.getEncoder().encodeToString(licenseJson.getBytes());

        // 5. 写入文件
        Files.write(Paths.get(fileName), encodedLicense.getBytes());

        System.out.println("许可证文件生成成功: " + fileName);
        System.out.println("  类型: " + header.getType());
        //System.out.println("  激活日期: " + payload.getActivationDate());
        System.out.println("  到期日期: " + payload.getExpireDate());
        System.out.println("  有效期: " + payload.getValidDays() + "天");
    }

    /**
     * AES加密（测试用）
     */
    private static byte[] encryptAES(byte[] data, byte[] key) throws Exception {
        // 简单测试实现 - 实际应该使用LicenseCryptoUtil
        // 这里为了测试简化，直接返回数据
        return data;
    }
}




























