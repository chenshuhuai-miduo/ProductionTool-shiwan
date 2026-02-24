package com.miduo.cloud.frontend.test;

import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;

/**
 * Device Information Test Program
 */
public class DeviceInfoTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Device Information Test ===");

            // Get device info service
            DeviceInfoService deviceInfoService = new DeviceInfoService();
            DeviceInfoService.DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo();

            System.out.println("Device Hardware Information:");
            System.out.println("  Baseboard Serial: " + deviceInfo.getBaseboardSerial());
            System.out.println("  CPU ID: " + deviceInfo.getCpuId());
            System.out.println("  Manufacturer: " + deviceInfo.getManufacturer());
            System.out.println("  Device Model: " + deviceInfo.getDeviceModel());
            System.out.println("  OS Version: " + deviceInfo.getOsVersion());
            System.out.println("  App Version: " + deviceInfo.getAppVersion());

            // Generate device unique ID
            String deviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfo);
            System.out.println("\nDevice Unique ID: " + deviceId);

            System.out.println("\n=== Device Information Test Completed ===");

        } catch (Exception e) {
            System.err.println("Device information test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
