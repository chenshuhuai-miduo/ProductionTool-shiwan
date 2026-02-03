package com.miduo.cloud.frontend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miduo.cloud.entity.dto.license.DeviceRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备请求文件工具类
 * 负责生成和解析设备请求文件(.devreq)
 */
@Slf4j
public class DeviceRequestFileUtil {

    private static final String FILE_TYPE = "DEVICE_ACTIVATION_REQUEST";
    private static final String VERSION = "1.0";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 生成设备请求文件
     *
     * @param deviceInfo 设备信息
     * @param outputPath 输出文件路径
     * @throws IOException 文件写入异常
     */
    public static void generateDeviceRequestFile(
            com.miduo.cloud.frontend.service.DeviceInfoService.DeviceInfo deviceInfo,
            String outputPath) throws IOException {

        // 生成设备唯一码
        String deviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfo);

        // 构建设备信息对象
        DeviceRequestDTO.DeviceInfo deviceInfoDto = DeviceRequestDTO.DeviceInfo.builder()
                .deviceId(deviceId)
                .baseboardSerial(deviceInfo.getBaseboardSerial())
                .cpuId(deviceInfo.getCpuId())
                .manufacturer(deviceInfo.getManufacturer())
                .deviceModel(deviceInfo.getDeviceModel())
                .osVersion(deviceInfo.getOsVersion())
                .appVersion(deviceInfo.getAppVersion())
                .build();

        // 构建不包含checksum的JSON对象（用于计算checksum）
        Map<String, Object> rootWithoutChecksum = new LinkedHashMap<>();
        rootWithoutChecksum.put("fileType", FILE_TYPE);
        rootWithoutChecksum.put("version", VERSION);

        // 创建时间使用字符串格式，与解密端保持一致
        String createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        rootWithoutChecksum.put("createTime", createTime);

        // 设备信息：必须使用deviceInfoDto，不能使用原始的deviceInfo
        Map<String, Object> deviceInfoMap = new LinkedHashMap<>();
        deviceInfoMap.put("deviceId", deviceInfoDto.getDeviceId());
        deviceInfoMap.put("baseboardSerial", deviceInfoDto.getBaseboardSerial());
        deviceInfoMap.put("cpuId", deviceInfoDto.getCpuId());
        deviceInfoMap.put("manufacturer", deviceInfoDto.getManufacturer());
        deviceInfoMap.put("deviceModel", deviceInfoDto.getDeviceModel());
        deviceInfoMap.put("osVersion", deviceInfoDto.getOsVersion());
        deviceInfoMap.put("appVersion", deviceInfoDto.getAppVersion());
        rootWithoutChecksum.put("deviceInfo", deviceInfoMap);

        rootWithoutChecksum.put("requestType", "activation");

        // 计算校验和：不包含checksum字段的JSON的MD5
        byte[] jsonBytes = objectMapper.writeValueAsBytes(rootWithoutChecksum);
        String checksum = DigestUtils.md5Hex(jsonBytes);

        // 添加checksum到root
        rootWithoutChecksum.put("checksum", checksum);

        // 最终的JSON字符串（包含checksum）
        String jsonContent = objectMapper.writeValueAsString(rootWithoutChecksum);

        // Base64编码
        String encodedContent = Base64.getEncoder().encodeToString(jsonContent.getBytes("UTF-8"));

        // 写入文件
        Path outputFilePath = Paths.get(outputPath);
        Files.createDirectories(outputFilePath.getParent());
        Files.write(outputFilePath, encodedContent.getBytes("UTF-8"));

        log.info("设备请求文件生成成功：{}", outputPath);
    }

    /**
     * 解析设备请求文件
     *
     * @param filePath 文件路径
     * @return 设备请求DTO
     * @throws IOException 文件读取或解析异常
     */
    public static DeviceRequestDTO parseDeviceRequestFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("设备请求文件不存在: " + filePath);
        }

        // 读取文件内容
        byte[] fileContent = Files.readAllBytes(path);
        String encodedContent = new String(fileContent, "UTF-8");

        // Base64解码
        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
        String jsonContent = new String(decodedBytes, "UTF-8");

        // 解析JSON
        DeviceRequestDTO requestDTO = objectMapper.readValue(jsonContent, DeviceRequestDTO.class);

        // 验证文件类型
        if (!FILE_TYPE.equals(requestDTO.getFileType())) {
            throw new IOException("无效的文件类型: " + requestDTO.getFileType());
        }

        log.info("设备请求文件解析成功：{}", filePath);
        return requestDTO;
    }

    /**
     * 验证设备请求文件的完整性
     *
     * @param requestDTO 设备请求DTO
     * @return 校验是否通过
     */
    public static boolean validateChecksum(DeviceRequestDTO requestDTO) {
        if (requestDTO.getDeviceInfo() == null || requestDTO.getChecksum() == null) {
            return false;
        }

        DeviceRequestDTO.DeviceInfo deviceInfo = requestDTO.getDeviceInfo();
        String checksumData = deviceInfo.getDeviceId() + "|" +
                             deviceInfo.getBaseboardSerial() + "|" +
                             deviceInfo.getCpuId() + "|" +
                             deviceInfo.getManufacturer();

        String expectedChecksum = DigestUtils.md5Hex(checksumData);
        boolean isValid = expectedChecksum.equals(requestDTO.getChecksum());

        if (!isValid) {
            log.warn("设备请求文件校验和验证失败");
        }

        return isValid;
    }

    /**
     * 生成默认的文件名
     *
     * @param deviceId 设备ID
     * @return 文件名（格式：device_request_{deviceId}.devreq）
     */
    public static String generateDefaultFileName(String deviceId) {
        return String.format("device_request_%s.devreq", deviceId.substring(0, 8));
    }
}





















