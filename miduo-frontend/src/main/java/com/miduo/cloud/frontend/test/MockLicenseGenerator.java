package com.miduo.cloud.frontend.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;

/**
 * Mock License Generator for Testing
 * Generates test license files without requiring actual hardware info
 */
public class MockLicenseGenerator {

    // Mock device ID for testing
    private static final String TEST_DEVICE_ID = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";

    public static void main(String[] args) {
        try {
            System.out.println("=== Mock License Generator ===");
            System.out.println("Test Device ID: " + TEST_DEVICE_ID);

            // Generate trial license (15 days)
            generateTrialLicense(TEST_DEVICE_ID);

            // Generate standard license (365 days)
            generateStandardLicense(TEST_DEVICE_ID);

            // Generate device request file
            generateDeviceRequestFile(TEST_DEVICE_ID);

            System.out.println("\n=== Generated Files ===");
            System.out.println("trial_license.lic - Trial license (15 days)");
            System.out.println("standard_license.lic - Standard license (365 days)");
            System.out.println("test_device_request.devreq - Device request file");

        } catch (Exception e) {
            System.err.println("License generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate trial license
     */
    private static void generateTrialLicense(String deviceId) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate expireDate = today.plusDays(15);

        String payload = "{" +
                "\"licenseKey\":\"TRIAL-TEST-001\"," +
                "\"licenseType\":\"TRIAL\"," +
                "\"deviceId\":\"" + deviceId + "\"," +
                "\"activationDate\":\"" + today + "\"," +
                "\"expireDate\":\"" + expireDate + "\"," +
                "\"validDays\":15," +
                "\"features\":[\"all\"]" +
                "}";

        String header = "{" +
                "\"version\":\"1.0\"," +
                "\"type\":\"TRIAL\"," +
                "\"keyVersion\":\"2025\"," +
                "\"algorithm\":\"AES256+RSA2048\"" +
                "}";

        String licenseFile = "{" +
                "\"header\":" + header + "," +
                "\"payload\":\"" + Base64.getEncoder().encodeToString(payload.getBytes()) + "\"," +
                "\"signature\":\"TEST_SIGNATURE_TRIAL_" + System.currentTimeMillis() + "\"" +
                "}";

        String encodedLicense = Base64.getEncoder().encodeToString(licenseFile.getBytes());
        Files.write(Paths.get("trial_license.lic"), encodedLicense.getBytes());

        System.out.println("Trial license generated: expires " + expireDate);
    }

    /**
     * Generate standard license
     */
    private static void generateStandardLicense(String deviceId) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate expireDate = today.plusDays(365);

        String payload = "{" +
                "\"licenseKey\":\"STD-TEST-001\"," +
                "\"licenseType\":\"STANDARD\"," +
                "\"deviceId\":\"" + deviceId + "\"," +
                "\"activationDate\":\"" + today + "\"," +
                "\"expireDate\":\"" + expireDate + "\"," +
                "\"validDays\":365," +
                "\"features\":[\"all\"]" +
                "}";

        String header = "{" +
                "\"version\":\"1.0\"," +
                "\"type\":\"STANDARD\"," +
                "\"keyVersion\":\"2025\"," +
                "\"algorithm\":\"AES256+RSA2048\"" +
                "}";

        String licenseFile = "{" +
                "\"header\":" + header + "," +
                "\"payload\":\"" + Base64.getEncoder().encodeToString(payload.getBytes()) + "\"," +
                "\"signature\":\"TEST_SIGNATURE_STANDARD_" + System.currentTimeMillis() + "\"" +
                "}";

        String encodedLicense = Base64.getEncoder().encodeToString(licenseFile.getBytes());
        Files.write(Paths.get("standard_license.lic"), encodedLicense.getBytes());

        System.out.println("Standard license generated: expires " + expireDate);
    }

    /**
     * Generate device request file
     */
    private static void generateDeviceRequestFile(String deviceId) throws Exception {
        LocalDate today = LocalDate.now();

        String deviceRequest = "{" +
                "\"fileType\":\"DEVICE_ACTIVATION_REQUEST\"," +
                "\"version\":\"1.0\"," +
                "\"createTime\":\"" + today + "T10:00:00\"," +
                "\"deviceInfo\":{" +
                "\"deviceId\":\"" + deviceId + "\"," +
                "\"baseboardSerial\":\"TEST_BASEBOARD_123\"," +
                "\"cpuId\":\"TEST_CPU_ABC123\"," +
                "\"manufacturer\":\"TEST_MANUFACTURER\"," +
                "\"deviceModel\":\"TEST_MODEL_X1\"," +
                "\"osVersion\":\"Windows 10 Pro\"," +
                "\"appVersion\":\"1.0.0\"" +
                "}," +
                "\"requestType\":\"activation\"," +
                "\"checksum\":\"test_checksum_123\"" +
                "}";

        String encodedRequest = Base64.getEncoder().encodeToString(deviceRequest.getBytes());
        Files.write(Paths.get("test_device_request.devreq"), encodedRequest.getBytes());

        System.out.println("Device request file generated");
    }
}




























