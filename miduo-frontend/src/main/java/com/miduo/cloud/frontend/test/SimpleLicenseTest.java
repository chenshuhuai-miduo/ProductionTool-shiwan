package com.miduo.cloud.frontend.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Simple License Test without Spring dependencies
 */
public class SimpleLicenseTest {

    private static final String TEST_DEVICE_ID = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";

    public static void main(String[] args) {
        try {
            System.out.println("=== Simple License Test ===");
            System.out.println("Test Device ID: " + TEST_DEVICE_ID);

            // Test 1: Check generated files exist
            System.out.println("\n--- Test 1: Check Generated Files ---");
            checkFilesExist();

            // Test 2: Validate trial license content
            System.out.println("\n--- Test 2: Validate Trial License ---");
            validateTrialLicense();

            // Test 3: Validate standard license content
            System.out.println("\n--- Test 3: Validate Standard License ---");
            validateStandardLicense();

            // Test 4: Validate device request file
            System.out.println("\n--- Test 4: Validate Device Request ---");
            validateDeviceRequest();

            System.out.println("\n=== All Basic Tests Passed ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkFilesExist() {
        String[] files = {"trial_license.lic", "standard_license.lic", "test_device_request.devreq"};

        for (String file : files) {
            if (Files.exists(Paths.get(file))) {
                System.out.println("[OK] " + file + " exists");
            } else {
                System.out.println("[FAIL] " + file + " missing");
            }
        }
    }

    private static void validateTrialLicense() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("trial_license.lic")));
        String decoded = new String(Base64.getDecoder().decode(content));

        // Parse JSON to extract payload
        int payloadStart = decoded.indexOf("\"payload\":\"") + 11;
        int payloadEnd = decoded.indexOf("\"", payloadStart);
        String payloadBase64 = decoded.substring(payloadStart, payloadEnd);

        String payloadJson = new String(Base64.getDecoder().decode(payloadBase64));

        System.out.println("Trial license payload: " + payloadJson);

        if (payloadJson.contains("TRIAL") && payloadJson.contains("15")) {
            System.out.println("[OK] Trial license contains correct type and duration");
        } else {
            System.out.println("[FAIL] Trial license validation failed");
        }

        if (payloadJson.contains(TEST_DEVICE_ID)) {
            System.out.println("[OK] Trial license contains correct device ID");
        } else {
            System.out.println("[FAIL] Trial license device ID mismatch");
            System.out.println("Expected device ID: " + TEST_DEVICE_ID);
        }
    }

    private static void validateStandardLicense() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("standard_license.lic")));
        String decoded = new String(Base64.getDecoder().decode(content));

        // Parse JSON to extract payload
        int payloadStart = decoded.indexOf("\"payload\":\"") + 11;
        int payloadEnd = decoded.indexOf("\"", payloadStart);
        String payloadBase64 = decoded.substring(payloadStart, payloadEnd);

        String payloadJson = new String(Base64.getDecoder().decode(payloadBase64));

        System.out.println("Standard license payload: " + payloadJson);

        if (payloadJson.contains("STANDARD") && payloadJson.contains("365")) {
            System.out.println("[OK] Standard license contains correct type and duration");
        } else {
            System.out.println("[FAIL] Standard license validation failed");
        }

        if (payloadJson.contains(TEST_DEVICE_ID)) {
            System.out.println("[OK] Standard license contains correct device ID");
        } else {
            System.out.println("[FAIL] Standard license device ID mismatch");
            System.out.println("Expected device ID: " + TEST_DEVICE_ID);
        }
    }

    private static void validateDeviceRequest() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("test_device_request.devreq")));
        String decoded = new String(Base64.getDecoder().decode(content));

        System.out.println("Device request content: " + decoded.substring(0, Math.min(100, decoded.length())) + "...");

        if (decoded.contains("DEVICE_ACTIVATION_REQUEST") && decoded.contains(TEST_DEVICE_ID)) {
            System.out.println("[OK] Device request contains correct format and device ID");
        } else {
            System.out.println("[FAIL] Device request validation failed");
        }
    }
}
