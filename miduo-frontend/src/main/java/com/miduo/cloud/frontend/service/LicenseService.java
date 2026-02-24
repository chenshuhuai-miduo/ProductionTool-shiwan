package com.miduo.cloud.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.entity.dto.license.LicensePayload;
import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.miduo.cloud.frontend.config.ProductionLineLicenseCryptoProperties;
import com.miduo.cloud.frontend.util.LicenseCryptoUtil;
import com.miduo.cloud.frontend.util.ProductionLineLicenseFileDecryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

/**
 * 许可证管理服务
 * 负责许可证文件的读取、保存和管理
 */
@Slf4j
@Service
public class LicenseService {

    private static final String LICENSE_FILE_NAME = "production_line.lic";
    private static final String TIME_RECORD_FILE_NAME = "license_time.dat";

    @Value("${app.license.storage.path:#{system.getenv('APPDATA') + '/miduo/license'}}")
    private String licenseStoragePath;

    private Path licenseFilePath;
    private Path timeRecordFilePath;
    private LicenseValidationService validationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LicenseService(LicenseValidationService validationService) {
        this.validationService = validationService;
    }

    @PostConstruct
    public void init() {
        try {
            // 关键：非 Spring 场景下，@Value 不生效，需要兜底默认路径
            if (licenseStoragePath == null || licenseStoragePath.isBlank()) {
                String appData = System.getenv("APPDATA");
                if (appData == null || appData.isBlank()) {
                    appData = System.getProperty("user.home");
                }
                licenseStoragePath = appData + "/miduo/license";
            }
            // 确保许可证存储目录存在
            Path storageDir = Paths.get(licenseStoragePath);
            Files.createDirectories(storageDir);

            // 设置文件路径
            licenseFilePath = storageDir.resolve(LICENSE_FILE_NAME);
            timeRecordFilePath = storageDir.resolve(TIME_RECORD_FILE_NAME);

            log.info("许可证存储路径: {}", licenseFilePath);
            log.info("时间记录文件路径: {}", timeRecordFilePath);

        } catch (IOException e) {
            log.error("初始化许可证存储路径失败", e);
            throw new RuntimeException("无法初始化许可证存储路径", e);
        }
    }

    /**
     * 读取许可证文件
     *
     * @return 许可证验证结果
     */
    public LicenseValidationService.LicenseValidationResult readLicense(String currentDeviceId) {
        if (!Files.exists(licenseFilePath)) {
            log.info("许可证文件不存在");
            return LicenseValidationService.LicenseValidationResult.invalid("许可证文件不存在");
        }

        // 检查时间防篡改
//       if (!checkTimeIntegrity()) {
//           log.warn("检测到时间防篡改，许可证验证失败");
//           return LicenseValidationService.LicenseValidationResult.invalid("系统时间异常");
//       }

        LicenseValidationService.LicenseValidationResult result =
                validationService.validateLicense(licenseFilePath.toString(), currentDeviceId);
        // 如果验证成功，更新时间记录
//        if (result.isValid()) {
//            updateTimeRecord();
//        }

        return result;
    }

/**
 * 保存许可证文件（接受File对象）
 *
 * @param sourceFile 源许可证文件
 * @param currentDeviceId 当前设备ID
 * @return 保存结果
 */
public boolean saveLicense(File sourceFile, String currentDeviceId) {
    if (sourceFile == null || !sourceFile.exists()) {
        log.error("源许可证文件不存在");
        return false;
    }
    
    try {
        // 读取文件内容
        String fileContent = new String(Files.readAllBytes(sourceFile.toPath()), "UTF-8");
        
//        // 使用文件内容验证许可证
//        LicenseValidationService.LicenseValidationResult validationResult =
//                validationService.validateLicenseFromContent(fileContent, currentDeviceId);
//
//        if (!validationResult.isValid()) {
//            log.error("许可证文件验证失败: {}", validationResult.getErrorMessage());
//            return false;
//        }

        ObjectMapper objectMapper = new ObjectMapper();
        ProductionLineLicenseCryptoProperties cryptoProperties = new ProductionLineLicenseCryptoProperties();
        ProductionLineLicenseFileDecryptor decryptor = new ProductionLineLicenseFileDecryptor(objectMapper, cryptoProperties);

        Map<String, Object> map = decryptor.decryptLicenseFile(fileContent);

        // 使用文件内容验证许可证
        //boolean validationResult =
//                validationService.validateLicenseFromContent(fileContent, currentDeviceId);

//        if (!validationResult) {
//            log.error("许可证文件验证失败: {}");
//            return false;
//        }

        // 复制文件到存储目录
        Files.copy(sourceFile.toPath(), licenseFilePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("许可证文件保存成功: {}", licenseFilePath);
        return true;

    } catch (IOException e) {
        log.error("保存许可证文件失败", e);
        return false;
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

    /**
     * 保存许可证文件
     *
     * @param sourceFilePath 源许可证文件路径
     * @param currentDeviceId 当前设备ID
     * @return 保存结果
     */
    public boolean saveLicense(String sourceFilePath, String currentDeviceId) {
        try {
            Path sourcePath = Paths.get(sourceFilePath);
            if (!Files.exists(sourcePath)) {
                log.error("源许可证文件不存在: {}", sourceFilePath);
                return false;
            }

            // 先验证许可证文件
            LicenseValidationService.LicenseValidationResult validationResult =
                    validationService.validateLicense(sourceFilePath, currentDeviceId);

            if (!validationResult.isValid()) {
                log.error("许可证文件验证失败: {}", validationResult.getErrorMessage());
                return false;
            }

            // 复制文件到存储目录
            Files.copy(sourcePath, licenseFilePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("许可证文件保存成功: {}", licenseFilePath);
            return true;

        } catch (IOException e) {
            log.error("保存许可证文件失败", e);
            return false;
        }
    }

    /**
     * 许可证导入结果
     */
    public static class LicenseImportResult {
        private final ResultType type;
        private final String message;
        private final RenewalInfo renewalInfo; // 续期信息（仅当type为RENEW_SUCCESS时有效）
        private final ActivationInfo activationInfo; // 激活信息（仅当type为SUCCESS时有效）

        private LicenseImportResult(ResultType type, String message, RenewalInfo renewalInfo, ActivationInfo activationInfo) {
            this.type = type;
            this.message = message;
            this.renewalInfo = renewalInfo;
            this.activationInfo = activationInfo;
        }

        public ResultType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public RenewalInfo getRenewalInfo() {
            return renewalInfo;
        }

        public ActivationInfo getActivationInfo() {
            return activationInfo;
        }

        public boolean isSuccess() {
            return type == ResultType.SUCCESS || type == ResultType.RENEW_SUCCESS;
        }

        public boolean isRenewSuccess() {
            return type == ResultType.RENEW_SUCCESS;
        }

        // 静态工厂方法
       public static LicenseImportResult success(ActivationInfo activationInfo) {
            return new LicenseImportResult(ResultType.SUCCESS, "导入成功", null, activationInfo);
        }

        public static LicenseImportResult renewSuccess(LocalDate newExpiration, long extensionDays) {
            return new LicenseImportResult(ResultType.RENEW_SUCCESS, "续期成功", 
                new RenewalInfo(newExpiration, extensionDays), null);
        }

        public static LicenseImportResult fileNotFound() {
            return new LicenseImportResult(ResultType.FILE_NOT_FOUND, "许可证文件不存在", null, null);
        }

        public static LicenseImportResult invalidFormat() {
            return new LicenseImportResult(ResultType.INVALID_FORMAT, 
                "许可证文件格式不正确，请确认文件来源", null, null);
        }

        public static LicenseImportResult fileCorrupted() {
            return new LicenseImportResult(ResultType.FILE_CORRUPTED, 
                "许可证文件损坏，需重新激活", null, null);
        }

        public static LicenseImportResult deviceMismatch() {
            return new LicenseImportResult(ResultType.DEVICE_MISMATCH, 
                "许可证与当前设备不匹配，请联系米多专员", null, null);
        }

        public static LicenseImportResult signatureInvalid() {
            return new LicenseImportResult(ResultType.SIGNATURE_INVALID, 
                "许可证被篡改，需重新激活", null, null);
        }

        public static LicenseImportResult licenseExpired() {
            return new LicenseImportResult(ResultType.LICENSE_EXPIRED, "许可证已过期", null, null);
        }

        public static LicenseImportResult alreadyImported() {
            return new LicenseImportResult(ResultType.ALREADY_IMPORTED, 
                "许可证文件已导入，无需重复激活", null, null);
        }

        public static LicenseImportResult unknownError() {
            return new LicenseImportResult(ResultType.UNKNOWN_ERROR, "未知错误", null, null);
        }

        /**
         * 结果类型枚举
         */
        public enum ResultType {
            SUCCESS,
            RENEW_SUCCESS,
            FILE_NOT_FOUND,
            INVALID_FORMAT,
            FILE_CORRUPTED,
            DEVICE_MISMATCH,
            SIGNATURE_INVALID,
            LICENSE_EXPIRED,
            ALREADY_IMPORTED,
            UNKNOWN_ERROR
        }

        /**
         * 续期信息
         */
        public static class RenewalInfo {
            private final LocalDate newExpiration;
            private final long extensionDays;

            public RenewalInfo(LocalDate newExpiration, long extensionDays) {
                this.newExpiration = newExpiration;
                this.extensionDays = extensionDays;
            }

            public LocalDate getNewExpiration() {
                return newExpiration;
            }

            public long getExtensionDays() {
                return extensionDays;
            }
        }

        /**
         * 激活信息
         */
        public static class ActivationInfo {
            private final LocalDate activationDate;
            private final LocalDate expirationDate;
            private final Integer validDays;

            public ActivationInfo(LocalDate activationDate, LocalDate expirationDate, Integer validDays) {
                this.activationDate = activationDate;
                this.expirationDate = expirationDate;
                this.validDays = validDays;
            }

            public LocalDate getActivationDate() {
                return activationDate;
            }

            public LocalDate getExpirationDate() {
                return expirationDate;
            }

            public Integer getValidDays() {
                return validDays;
            }
        }
    }

    /**
     * 保存许可证文件（带详细结果）
     *
     * @param sourceFile 源许可证文件
     * @param currentDeviceId 当前设备ID
     * @return 导入结果
     */
    public LicenseImportResult saveLicenseWithResult(File sourceFile, String currentDeviceId) {
        if (sourceFile == null || !sourceFile.exists()) {
            log.error("源许可证文件不存在");
            return LicenseImportResult.fileNotFound();
        }

        // 检查文件格式
        if (!sourceFile.getName().toLowerCase().endsWith(".lic")) {
            log.error("许可证文件格式不正确");
            return LicenseImportResult.invalidFormat();
        }

        try {
            // 读取文件内容
            String fileContent = new String(Files.readAllBytes(sourceFile.toPath()), "UTF-8");

            // 检查是否为空或格式错误
            if (fileContent == null || fileContent.trim().isEmpty()) {
                return LicenseImportResult.fileCorrupted();
            }

            // 解密并验证许可证
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            ProductionLineLicenseCryptoProperties cryptoProperties = new ProductionLineLicenseCryptoProperties();
            ProductionLineLicenseFileDecryptor decryptor = new ProductionLineLicenseFileDecryptor(objectMapper, cryptoProperties);

            Map<String, Object> decryptedData;
            try {
                decryptedData = decryptor.decryptLicenseFile(fileContent);
            } catch (SecurityException e) {
                // 签名验证失败
                log.error("许可证签名验证失败: {}", e.getMessage());
                return LicenseImportResult.signatureInvalid();
            } catch (Exception e) {
                // 解密失败，文件损坏
                log.error("许可证文件解密失败: {}", e.getMessage());
                return LicenseImportResult.fileCorrupted();
            }

            // 转换为 LicensePayload
            LicensePayload payload;
            try {
                payload = objectMapper.convertValue(decryptedData, LicensePayload.class);
            } catch (Exception e) {
                log.error("许可证载荷转换失败: {}", e.getMessage());
                return LicenseImportResult.fileCorrupted();
            }

            // 验证设备ID
            if (payload.getDeviceId() != null && !payload.getDeviceId().equals(currentDeviceId)) {
                log.error("设备ID不匹配 - 期望: {}, 实际: {}", payload.getDeviceId(), currentDeviceId);
                return LicenseImportResult.deviceMismatch();
            }

            // 验证有效期
            if (payload.getExpireDate() != null && payload.getExpireDate().isBefore(LocalDate.now())) {
                log.error("许可证已过期: {}", payload.getExpireDate());
                return LicenseImportResult.licenseExpired();
            }

            // 复制文件到存储目录
            Files.copy(sourceFile.toPath(), licenseFilePath, StandardCopyOption.REPLACE_EXISTING);

            // 强制刷新文件系统，确保文件写入完成
            try {
                java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                    licenseFilePath, 
                    java.nio.file.StandardOpenOption.WRITE
                );
                channel.force(true);
                channel.close();
            } catch (IOException e) {
                log.warn("强制刷新文件系统失败，但不影响功能: {}", e.getMessage());
            }

            log.info("许可证文件保存成功: {}", licenseFilePath);
            String activationType = payload.getActivationType();
            if ("renewal".equals(activationType)) {
                // 续期成功，使用新许可证的 validDays 作为延长天数
                long extensionDays = payload.getValidDays() != null ? payload.getValidDays() : 0;
                return LicenseImportResult.renewSuccess(payload.getExpireDate(), extensionDays);
            } else {
                // 激活成功，提取激活信息
                LocalDate activationDate = null;
                if (payload.getActivationTime() != null && !payload.getActivationTime().isEmpty()) {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(payload.getActivationTime(), TIME_FORMATTER);
                        activationDate = dateTime.toLocalDate();
                    } catch (Exception e) {
                        log.warn("解析激活时间失败，使用当前日期: {}", e.getMessage());
                        activationDate = LocalDate.now();
                    }
                } else {
                    activationDate = LocalDate.now();
                }
                
                LocalDate expirationDate = payload.getExpireDate();
                Integer validDays = payload.getValidDays();
                
                LicenseImportResult.ActivationInfo activationInfo = 
                    new LicenseImportResult.ActivationInfo(activationDate, expirationDate, validDays);
                
                return LicenseImportResult.success(activationInfo);
            }

        } catch (IOException e) {
            log.error("保存许可证文件失败", e);
            return LicenseImportResult.fileCorrupted();
        } catch (Exception e) {
            log.error("导入许可证时发生未知错误", e);
            return LicenseImportResult.unknownError();
        }
    }

    /**
     * 删除许可证文件
     *
     * @return 删除是否成功
     */
    public boolean deleteLicense() {
        try {
            if (Files.exists(licenseFilePath)) {
                Files.delete(licenseFilePath);
                log.info("许可证文件已删除: {}", licenseFilePath);
                return true;
            }
            return true; // 文件不存在也算成功
        } catch (IOException e) {
            log.error("删除许可证文件失败", e);
            return false;
        }
    }

    /**
     * 获取当前许可证状态
     *
     * @param currentDeviceId 当前设备ID
     * @return 许可证状态
     */
    public LicenseStatusEnum getCurrentLicenseStatus(String currentDeviceId) {
        LicenseValidationService.LicenseValidationResult result = readLicense(currentDeviceId);
        return result.isValid() ? result.getStatus() : LicenseStatusEnum.UNACTIVATED;
    }

    /**
     * 获取剩余天数
     *
     * @param currentDeviceId 当前设备ID
     * @return 剩余天数（-1表示无许可证）
     */
    public long getRemainingDays(String currentDeviceId) {
        LicenseValidationService.LicenseValidationResult result = readLicense(currentDeviceId);
        if (!result.isValid()) {
            return -1;
        }

        LicensePayload payload = result.getPayload();
        if (payload == null || payload.getExpireDate() == null) {
            return -1;
        }

        return LicenseValidationService.calculateDaysRemaining(payload.getExpireDate());
    }

    /**
     * 检查许可证是否存在
     *
     * @return 许可证文件是否存在
     */
    public boolean licenseExists() {
        return Files.exists(licenseFilePath);
    }

    /**
     * 获取许可证文件路径
     *
     * @return 许可证文件路径
     */
    public String getLicenseFilePath() {
        return licenseFilePath.toString();
    }

    /**
     * 获取许可证存储目录
     *
     * @return 存储目录路径
     */
    public String getLicenseStoragePath() {
        return licenseStoragePath;
    }

    /**
     * 获取许可证信息
     *
     * @param currentDeviceId 当前设备ID
     * @return 许可证信息DTO
     */
    public LicenseInfo getLicenseInfo(String currentDeviceId) {
        LicenseValidationService.LicenseValidationResult result = readLicense(currentDeviceId);

        LicenseInfo info = new LicenseInfo();
        info.setLicenseExists(licenseExists());
        info.setStatus(result.isValid() ? result.getStatus() : LicenseStatusEnum.UNACTIVATED);

        if (result.isValid() && result.getPayload() != null) {
            LicensePayload payload = result.getPayload();
            info.setLicenseType(payload.getLicenseType());
            info.setLicenseKey(payload.getLicenseKey());
            // 从 activationTime 字符串中提取日期部分并转换为 LocalDate
            if (payload.getActivationTime() != null && !payload.getActivationTime().isEmpty()) {
                LocalDateTime dateTime = LocalDateTime.parse(payload.getActivationTime(), TIME_FORMATTER);
                info.setActivationDate(dateTime);
            }
            info.setExpireDate(payload.getExpireDate());
            info.setValidDays(payload.getValidDays());
            info.setRemainingDays(LicenseValidationService.calculateDaysRemaining(payload.getExpireDate()));
        }

        return info;
    }

    /**
     * 检查时间完整性（防篡改）
     *
     * @return 时间是否正常
     */
    private boolean checkTimeIntegrity() {
        try {
            if (!Files.exists(timeRecordFilePath)) {
                // 首次运行，创建时间记录
                updateTimeRecord();
                return true;
            }

            // 读取上次记录的时间
            String lastTimeStr = new String(Files.readAllBytes(timeRecordFilePath)).trim();
            LocalDateTime lastTime = LocalDateTime.parse(lastTimeStr, TIME_FORMATTER);
            LocalDateTime currentTime = LocalDateTime.now();

            // 检查时间是否被回拨
            if (currentTime.isBefore(lastTime.minusHours(1))) {
                log.warn("检测到系统时间被回拨 - 上次记录时间: {}, 当前时间: {}", lastTime, currentTime);
                return false;
            }

            // 检查时间跳跃是否过大（24小时内不允许跳跃超过12小时）
            if (currentTime.isAfter(lastTime.plusHours(12))) {
                log.warn("检测到系统时间大幅跳跃 - 上次记录时间: {}, 当前时间: {}", lastTime, currentTime);
                // 这里可以选择警告但允许，或者严格拒绝
                // 为了兼容性，这里选择警告但允许
            }

            return true;

        } catch (Exception e) {
            log.warn("时间完整性检查失败，将重置时间记录", e);
            // 如果检查失败，重置时间记录
            try {
                updateTimeRecord();
            } catch (Exception ex) {
                log.error("重置时间记录失败", ex);
            }
            return true;
        }
    }

    /**
     * 更新时间记录
     */
    private void updateTimeRecord() {
        try {
            String currentTimeStr = LocalDateTime.now().format(TIME_FORMATTER);
            Files.write(timeRecordFilePath, currentTimeStr.getBytes());
            log.debug("时间记录已更新: {}", currentTimeStr);
        } catch (IOException e) {
            log.error("更新时间记录失败", e);
        }
    }

    /**
     * 获取上次验证时间
     *
     * @return 上次验证时间，如果不存在则返回null
     */
    public LocalDateTime getLastVerificationTime() {
        try {
            if (!Files.exists(timeRecordFilePath)) {
                return null;
            }
            String lastTimeStr = new String(Files.readAllBytes(timeRecordFilePath)).trim();
            return LocalDateTime.parse(lastTimeStr, TIME_FORMATTER);
        } catch (Exception e) {
            log.error("获取上次验证时间失败", e);
            return null;
        }
    }

    /**
     * 许可证信息DTO
     */
    public static class LicenseInfo {
        private boolean licenseExists;
        private LicenseStatusEnum status;
        private String licenseType;
        private String licenseKey;
        private LocalDateTime activationDate;
        private LocalDate expireDate;
        private Integer validDays;
        private long remainingDays;
        private java.util.List<String> features;

        // Getters and setters
        public boolean isLicenseExists() { return licenseExists; }
        public void setLicenseExists(boolean licenseExists) { this.licenseExists = licenseExists; }

        public LicenseStatusEnum getStatus() { return status; }
        public void setStatus(LicenseStatusEnum status) { this.status = status; }

        public String getLicenseType() { return licenseType; }
        public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

        public String getLicenseKey() { return licenseKey; }
        public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

        public LocalDateTime getActivationDate() { return activationDate; }
        public void setActivationDate(LocalDateTime activationDate) { this.activationDate = activationDate; }

        public LocalDate getExpireDate() { return expireDate; }
        public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }

        public Integer getValidDays() { return validDays; }
        public void setValidDays(Integer validDays) { this.validDays = validDays; }

        public long getRemainingDays() { return remainingDays; }
        public void setRemainingDays(long remainingDays) { this.remainingDays = remainingDays; }

        public java.util.List<String> getFeatures() { return features; }
        public void setFeatures(java.util.List<String> features) { this.features = features; }
    }
}
