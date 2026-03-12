package com.miduo.cloud.application.device;

import com.miduo.cloud.common.util.IniFileUtil;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IO设备管理Service实现类
 * 基于INI文件存储的设备配置管理
 */
@Service
public class DeviceApplicationService {

    /**
     * INI 文件路径（可通过配置覆盖）
     * - 默认：io_devices.ini（致美斋、通用环境）
     * - 石湾2号机：application-shiwan-m2.properties 中覆盖为 io_devices_shiwan_m2.ini
     */
    @Value("${io.device.ini:io_devices.ini}")
    private String iniFilePath;

    /**
     * 获取所有设备
     */
    public List<IoDeviceDTO> getAllDevices() {
        List<IoDeviceDTO> devices = new ArrayList<>();
        
        // 读取所有节（每个节代表一个设备）
        List<String> sections = IniFileUtil.readSections(iniFilePath);
        
        for (String section : sections) {
            Map<String, String> properties = IniFileUtil.readSection(iniFilePath, section);
            IoDeviceDTO device = mapToDevice(section, properties);
            devices.add(device);
        }
        
        return devices;
    }

    /**
     * 根据ID获取设备
     */
    public IoDeviceDTO getDeviceById(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        
        Map<String, String> properties = IniFileUtil.readSection(iniFilePath, id);
        
        if (properties.isEmpty()) {
            return null;
        }
        
        return mapToDevice(id, properties);
    }

    /**
     * 添加设备
     */
    public String addDevice(IoDeviceDTO device) {
        // 验证设备名称
        if (!StringUtils.hasText(device.getDeviceName())) {
            throw new RuntimeException("设备名称不能为空");
        }
        
        // 检查设备名称是否已存在
        IoDeviceDTO existingDevice = getDeviceById(device.getDeviceName());
        if (existingDevice != null) {
            throw new RuntimeException("设备名称已存在：" + device.getDeviceName());
        }
        
        // 构建键值对
        Map<String, String> properties = mapToProperties(device);
        
        // 写入INI文件
        IniFileUtil.writeSection(iniFilePath, device.getDeviceName(), properties);
        
        return device.getDeviceName();
    }

    /**
     * 更新设备
     */
    public Boolean updateDevice(IoDeviceDTO device) {
        // 验证设备ID
        if (!StringUtils.hasText(device.getId())) {
            throw new RuntimeException("设备ID不能为空");
        }
        
        // 检查设备是否存在
        IoDeviceDTO existingDevice = getDeviceById(device.getId());
        if (existingDevice == null) {
            throw new RuntimeException("设备不存在：" + device.getId());
        }
        
        // 如果设备名称发生变化，需要删除旧节并创建新节
        if (!device.getId().equals(device.getDeviceName())) {
            // 检查新设备名称是否已存在
            IoDeviceDTO newNameDevice = getDeviceById(device.getDeviceName());
            if (newNameDevice != null) {
                throw new RuntimeException("设备名称已存在：" + device.getDeviceName());
            }
            
            // 删除旧节
            IniFileUtil.deleteSection(iniFilePath, device.getId());
        }
        
        // 构建键值对
        Map<String, String> properties = mapToProperties(device);
        
        // 写入INI文件
        IniFileUtil.writeSection(iniFilePath, device.getDeviceName(), properties);
        
        return true;
    }

    /**
     * 删除设备
     */
    public Boolean deleteDevice(String id) {
        // 验证设备ID
        if (!StringUtils.hasText(id)) {
            throw new RuntimeException("设备ID不能为空");
        }
        
        // 检查设备是否存在
        IoDeviceDTO existingDevice = getDeviceById(id);
        if (existingDevice == null) {
            throw new RuntimeException("设备不存在：" + id);
        }
        
        // 删除INI文件中的节
        IniFileUtil.deleteSection(iniFilePath, id);
        
        return true;
    }

    /**
     * 将Map转换为IoDeviceDTO
     */
    private IoDeviceDTO mapToDevice(String section, Map<String, String> properties) {
        IoDeviceDTO device = new IoDeviceDTO();
        device.setId(section);
        device.setDeviceName(section);
        
        // 从数字转换为设备类别文本
        String categoryCode = properties.get("deviceCategory");
        device.setDeviceCategory(convertCategoryCodeToText(categoryCode));
        
        device.setConnectionType(properties.get("connectionType"));
        device.setProtocolType(properties.get("protocolType"));
        device.setAddress(properties.get("address"));
        device.setPort(properties.get("port"));
        
        try {
            device.setTimeout(Integer.parseInt(properties.getOrDefault("timeout", "5")));
        } catch (NumberFormatException e) {
            device.setTimeout(5);
        }
        
        try {
            device.setRetryCount(Integer.parseInt(properties.getOrDefault("retryCount", "3")));
        } catch (NumberFormatException e) {
            device.setRetryCount(3);
        }
        
        device.setEnabled("true".equals(properties.get("enabled")));
        device.setDescription(properties.get("description"));
        
        // 状态文本由前端根据实际连接状态设置，这里初始化为空
        device.setStatusText("");
        
        return device;
    }

    /**
     * 将IoDeviceDTO转换为Map
     */
    private Map<String, String> mapToProperties(IoDeviceDTO device) {
        Map<String, String> properties = new java.util.LinkedHashMap<>();
        
        // 将设备类别文本转换为数字
        properties.put("deviceCategory", convertCategoryTextToCode(device.getDeviceCategory()));
        
        properties.put("connectionType", device.getConnectionType() != null ? device.getConnectionType() : "");
        properties.put("protocolType", device.getProtocolType() != null ? device.getProtocolType() : "");
        properties.put("address", device.getAddress() != null ? device.getAddress() : "");
        properties.put("port", device.getPort() != null ? device.getPort() : "");
        properties.put("timeout", String.valueOf(device.getTimeout() != null ? device.getTimeout() : 5));
        properties.put("retryCount", String.valueOf(device.getRetryCount() != null ? device.getRetryCount() : 3));
        properties.put("enabled", String.valueOf(device.getEnabled() != null ? device.getEnabled() : true));
        properties.put("description", device.getDescription() != null ? device.getDescription() : "");
        return properties;
    }
    
    /**
     * 将设备类别文本转换为代码
     * 瓶码采集->1, 盒码采集->2, 箱码采集->3, 报警器->4, 剔除装置->5, 扫码枪->6
     */
    private String convertCategoryTextToCode(String categoryText) {
        if (categoryText == null) return "";
        switch (categoryText) {
            case "瓶码采集": return "1";
            case "盒码采集": return "2";
            case "箱码采集": return "3";
            case "报警器": return "4";
            case "剔除装置": return "5";
            case "扫码枪": return "6";
            default: return "";
        }
    }
    
    /**
     * 将设备类别代码转换为文本（2号机：瓶码采集、盒码采集、箱码采集、报警器、剔除装置、扫码枪）
     */
    private String convertCategoryCodeToText(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) return "";
        switch (categoryCode.trim()) {
            case "1": return "瓶码采集";
            case "2": return "盒码采集";
            case "3": return "箱码采集";
            case "4": return "报警器";
            case "5": return "剔除装置";
            case "6": return "扫码枪";
            case "7": return "扫码枪";
            default: return "";
        }
    }
}

