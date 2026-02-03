package com.miduo.cloud.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miduo.cloud.entity.dto.license.LicenseFile;
import com.miduo.cloud.entity.dto.license.LicensePayload;
import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.miduo.cloud.frontend.util.LicenseCryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.security.spec.X509EncodedKeySpec;
import com.miduo.cloud.frontend.util.ProductionLineLicenseFileDecryptor;
import com.miduo.cloud.frontend.config.ProductionLineLicenseCryptoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.logging.log4j.message.MapMessage.MapFormat.JSON;

/**
 * 许可证验证服务
 * 负责验证许可证文件的完整性、签名、有效期等
 */
@Slf4j
@Service
public class LicenseValidationService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    // 添加解密器实例（可以延迟初始化或作为字段）
    private ProductionLineLicenseFileDecryptor licenseFileDecryptor;
    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

 /**
     * 获取许可证文件解密器实例（懒加载）
     */
    private ProductionLineLicenseFileDecryptor getLicenseFileDecryptor() {
        if (licenseFileDecryptor == null) {
            ProductionLineLicenseCryptoProperties cryptoProperties = new ProductionLineLicenseCryptoProperties();
            licenseFileDecryptor = new ProductionLineLicenseFileDecryptor(objectMapper, cryptoProperties);
        }
        return licenseFileDecryptor;
    }

    /**
     * 验证许可证文件
     *
     * @param licenseFilePath 许可证文件路径
     * @param currentDeviceId 当前设备ID
     * @return 验证结果
     */
    public LicenseValidationResult validateLicense(String licenseFilePath, String currentDeviceId) {
        try {
            // 1. 检查文件是否存在
            Path path = Paths.get(licenseFilePath);
            if (!Files.exists(path)) {
                log.warn("许可证文件不存在: {}", licenseFilePath);
                return LicenseValidationResult.invalid("许可证文件不存在");
            }

            // 2. 读取许可证文件内容
            String fileContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            // 3. 使用 ProductionLineLicenseFileDecryptor 解密许可证文件
            Map<String, Object> decryptedData;
            try {
                decryptedData = getLicenseFileDecryptor().decryptLicenseFile(fileContent);
            } catch (Exception e) {
                log.warn("许可证文件解密失败: {}", e.getMessage());
                return LicenseValidationResult.invalid("许可证文件解密失败: " + e.getMessage());
            }

            // 4. 将解密后的 Map 转换为 LicensePayload 对象
            LicensePayload payload;
            try {
                payload = objectMapper.convertValue(decryptedData, LicensePayload.class);
            } catch (Exception e) {
                log.error("许可证载荷转换失败", e);
                return LicenseValidationResult.invalid("许可证载荷解析失败: " + e.getMessage());
            }

            if (payload == null) {
                return LicenseValidationResult.invalid("许可证载荷解析失败");
            }

            //currentDeviceId = "112222333";//测试试用
            // 5. 验证设备ID匹配
            if (!currentDeviceId.equals(payload.getDeviceId())) {
                log.warn("设备ID不匹配 - 期望: {}, 实际: {}", payload.getDeviceId(), currentDeviceId);
                return LicenseValidationResult.deviceMismatch(payload.getDeviceId());
            }

            // 6. 验证有效期
            LicenseStatusEnum status = determineLicenseStatus(payload);

            log.info("许可证验证成功 - 类型: {}, 状态: {}, 剩余天数: {}",
                    payload.getLicenseType(), status.getDescription(),
                    calculateDaysRemaining(payload.getExpireDate()));

            return LicenseValidationResult.valid(payload, status);

        } catch (Exception e) {
            log.error("许可证验证异常", e);
            return LicenseValidationResult.invalid("许可证验证异常: " + e.getMessage());
        }
    }

    /**
     * 确定许可证状态
     */
    private LicenseStatusEnum determineLicenseStatus(LicensePayload payload) {
        // 临时测试：使用固定日期
        //LocalDate now = LocalDate.of(2026, 1, 20);  // 模拟2026年1月20日
        LocalDate now = LocalDate.now();  // 正式代码
        LocalDate expireDate = payload.getExpireDate();

        if (expireDate.isBefore(now)) {
            // 已过期
            return "trial".equals(payload.getLicenseType()) ?
                    LicenseStatusEnum.TRIAL_EXPIRED : LicenseStatusEnum.EXPIRED;
        } else {
            // 未过期
            return "trial".equals(payload.getLicenseType()) ?
                    LicenseStatusEnum.TRIAL_ACTIVE : LicenseStatusEnum.ACTIVATED;
        }
    }

    /**
     * 计算剩余天数
     */
    public static long calculateDaysRemaining(LocalDate expireDate) {
        if (expireDate == null) {
            return 0;
        }
        // 使用 ChronoUnit.DAYS.between 计算两个日期之间的总天数差
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
    }

    /**
     * 验证结果类
     */
    public static class LicenseValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final LicensePayload payload;
        private final LicenseStatusEnum status;
        private final String mismatchedDeviceId;

        private LicenseValidationResult(boolean valid, String errorMessage,
                                       LicensePayload payload, LicenseStatusEnum status,
                                       String mismatchedDeviceId) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.payload = payload;
            this.status = status;
            this.mismatchedDeviceId = mismatchedDeviceId;
        }

        public static LicenseValidationResult valid(LicensePayload payload, LicenseStatusEnum status) {
            return new LicenseValidationResult(true, null, payload, status, null);
        }

        public static LicenseValidationResult invalid(String errorMessage) {
            return new LicenseValidationResult(false, errorMessage, null, null, null);
        }

        public static LicenseValidationResult deviceMismatch(String mismatchedDeviceId) {
            return new LicenseValidationResult(false, "设备ID不匹配", null, null, mismatchedDeviceId);
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public LicensePayload getPayload() { return payload; }
        public LicenseStatusEnum getStatus() { return status; }
        public String getMismatchedDeviceId() { return mismatchedDeviceId; }
        public boolean isDeviceMismatch() { return mismatchedDeviceId != null; }
    }
}

