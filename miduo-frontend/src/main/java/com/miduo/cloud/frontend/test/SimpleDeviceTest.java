package com.miduo.cloud.frontend.test;

import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

import java.security.MessageDigest;

/**
 * Simple Device Information Test
 */
public class SimpleDeviceTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Simple Device Information Test ===");

            // Get hardware info using OSHI
            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();

            // Get baseboard serial
            ComputerSystem computerSystem = hardware.getComputerSystem();
            String baseboardSerial = "UNKNOWN";
            if (computerSystem != null) {
                Baseboard baseboard = computerSystem.getBaseboard();
                if (baseboard != null) {
                    baseboardSerial = baseboard.getSerialNumber();
                    if (baseboardSerial == null || baseboardSerial.trim().isEmpty() || "Default string".equals(baseboardSerial)) {
                        baseboardSerial = "UNKNOWN_BASEBOARD";
                    }
                }
            }

            // Get CPU ID
            CentralProcessor processor = hardware.getProcessor();
            String cpuId = "UNKNOWN";
            if (processor != null) {
                cpuId = processor.getProcessorIdentifier().getProcessorID();
                if (cpuId == null || cpuId.trim().isEmpty()) {
                    cpuId = "UNKNOWN_CPU";
                }
            }

            // Get manufacturer
            String manufacturer = "UNKNOWN";
            if (computerSystem != null) {
                manufacturer = computerSystem.getManufacturer();
                if (manufacturer == null || manufacturer.trim().isEmpty()) {
                    manufacturer = "UNKNOWN_MANUFACTURER";
                }
            }

            // Get device model
            String deviceModel = "UNKNOWN";
            if (computerSystem != null) {
                deviceModel = computerSystem.getModel();
                if (deviceModel == null || deviceModel.trim().isEmpty()) {
                    deviceModel = "UNKNOWN_MODEL";
                }
            }

            // Get OS version
            String osVersion = System.getProperty("os.name") + " " + System.getProperty("os.version");

            System.out.println("Hardware Information:");
            System.out.println("  Baseboard Serial: " + baseboardSerial);
            System.out.println("  CPU ID: " + cpuId);
            System.out.println("  Manufacturer: " + manufacturer);
            System.out.println("  Device Model: " + deviceModel);
            System.out.println("  OS Version: " + osVersion);

            // Generate device ID
            String combinedInfo = baseboardSerial + "|" + cpuId + "|" + manufacturer;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(combinedInfo.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String deviceId = sb.toString();

            System.out.println("\nGenerated Device ID: " + deviceId);

            // Save device info to file
            String deviceInfo = "{\n" +
                    "  \"deviceId\": \"" + deviceId + "\",\n" +
                    "  \"baseboardSerial\": \"" + baseboardSerial + "\",\n" +
                    "  \"cpuId\": \"" + cpuId + "\",\n" +
                    "  \"manufacturer\": \"" + manufacturer + "\",\n" +
                    "  \"deviceModel\": \"" + deviceModel + "\",\n" +
                    "  \"osVersion\": \"" + osVersion + "\",\n" +
                    "  \"appVersion\": \"1.0.0\"\n" +
                    "}";

            java.nio.file.Files.write(java.nio.file.Paths.get("device_info.json"), deviceInfo.getBytes("UTF-8"));
            System.out.println("\nDevice info saved to device_info.json");

            System.out.println("\n=== Test Completed Successfully ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}





















