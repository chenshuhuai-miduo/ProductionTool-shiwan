package com.miduo.cloud.controller;

import com.miduo.cloud.application.device.DeviceApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 石湾 2 号机必选设备校验接口。
 * 开始采集前校验：盒码采集相机(网口)、箱码采集相机(网口)、剔除装置(串口)、报警灯(报警器+串口) 是否已配置且网口设备可连接。
 */
@RestController
@RequestMapping("/api/shiwan-m2/device")
@CrossOrigin
public class ShiwanM2DeviceController {

    private static final int SOCKET_TIMEOUT_MS = 2000;

    @Autowired
    private DeviceApplicationService deviceApplicationService;

    /**
     * 校验 2 号机必选设备是否已配置且（网口设备）可连接。
     * GET /api/shiwan-m2/device/check-required
     * 返回 data.passed=true 表示通过；data.failedChecks 为未通过项说明。
     */
    @GetMapping("/check-required")
    public ApiResult<Map<String, Object>> checkRequiredDevices() {
        List<String> failedChecks = new ArrayList<>();
        List<IoDeviceDTO> all = deviceApplicationService.getAllDevices();
        List<IoDeviceDTO> enabled = all.stream()
            .filter(d -> d.getEnabled() != null && d.getEnabled())
            .collect(Collectors.toList());

        // 1. 盒码采集 + 网口，至少 1 台且可连接
        if (!checkCategoryWithNetwork(enabled, "盒码采集")) {
            failedChecks.add("盒码采集相机(网口)未配置或未连接");
        }
        // 2. 箱码采集 + 网口，至少 1 台且可连接
        if (!checkCategoryWithNetwork(enabled, "箱码采集")) {
            failedChecks.add("箱码采集相机(网口)未配置或未连接");
        }
        // 3. 剔除装置 + 串口，至少 1 台（串口不测连接，只检查已配置）
        if (!checkCategoryWithSerial(enabled, "剔除装置")) {
            failedChecks.add("剔除装置(串口)未配置");
        }
        // 4. 报警器 + 串口，至少 1 台（报警灯）
        if (!checkCategoryWithSerial(enabled, "报警器")) {
            failedChecks.add("报警灯(串口)未配置");
        }

        boolean passed = failedChecks.isEmpty();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("passed", passed);
        data.put("failedChecks", failedChecks);
        return ApiResult.success(passed ? "设备校验通过" : "设备未就绪", data);
    }

    /** 网口设备：要求至少一台该类别且连接方式为网口，并尝试 TCP 连接 */
    private boolean checkCategoryWithNetwork(List<IoDeviceDTO> enabled, String category) {
        List<IoDeviceDTO> candidates = enabled.stream()
            .filter(d -> category.equals(d.getDeviceCategory()) && "网口".equals(d.getConnectionType()))
            .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return false;
        }
        for (IoDeviceDTO d : candidates) {
            if (tryConnectTcp(d.getAddress(), d.getPort())) {
                return true;
            }
        }
        return false;
    }

    /** 串口设备：只要求至少一台该类别且连接方式为串口 */
    private boolean checkCategoryWithSerial(List<IoDeviceDTO> enabled, String category) {
        return enabled.stream()
            .anyMatch(d -> category.equals(d.getDeviceCategory()) && "串口".equals(d.getConnectionType()));
    }

    private boolean tryConnectTcp(String address, String portStr) {
        if (address == null || address.trim().isEmpty()) return false;
        int port;
        try {
            port = portStr != null && !portStr.trim().isEmpty() ? Integer.parseInt(portStr.trim()) : 502;
        } catch (NumberFormatException e) {
            port = 502;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(address.trim(), port), SOCKET_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
