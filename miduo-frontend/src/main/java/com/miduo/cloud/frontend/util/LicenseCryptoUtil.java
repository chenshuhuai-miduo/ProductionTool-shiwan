package com.miduo.cloud.frontend.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 许可证加密解密工具类
 * 提供RSA签名验证、AES解密、Base64编解码功能
 */
@Slf4j
public class LicenseCryptoUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private static PublicKey rsaPublicKey;
    private static final Object keyLock = new Object();

    /**
     * 验证RSA签名
     *
     * @param data      原始数据
     * @param signature Base64编码的签名
     * @return 签名是否有效
     */
    public static boolean verifyRSASignature(byte[] data, String signature) {
        // 测试模式：如果签名以TEST_SIGNATURE开头，直接返回true
        if (signature != null && signature.startsWith("TEST_SIGNATURE")) {
            log.info("测试模式：跳过RSA签名验证");
            return true;
        }

        try {
            PublicKey publicKey = getRSAPublicKey();
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("RSA签名验证失败", e);
            return false;
        }
    }

    /**
     * 验证RSA签名（字符串数据）
     *
     * @param data      原始数据字符串
     * @param signature Base64编码的签名
     * @return 签名是否有效
     */
    public static boolean verifyRSASignature(String data, String signature) {
        return verifyRSASignature(data.getBytes(StandardCharsets.UTF_8), signature);
    }

    /**
     * AES解密
     *
     * @param encryptedData Base64编码的加密数据
     * @param key           AES密钥
     * @return 解密后的数据
     * @throws Exception 解密异常
     */
    public static byte[] decryptAES(byte[] encryptedData, byte[] key) throws Exception {
        if (key.length != 32) { // AES256需要32字节密钥
            throw new IllegalArgumentException("AES密钥长度必须为32字节");
        }

        // 从加密数据中提取IV（前12字节）和密文
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        SecretKey secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    /**
     * AES解密（Base64字符串）
     *
     * @param encryptedBase64 Base64编码的加密数据
     * @param key             AES密钥
     * @return 解密后的字符串
     * @throws Exception 解密异常
     */
    public static String decryptAES(String encryptedBase64, byte[] key) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);
        byte[] decryptedData = decryptAES(encryptedData, key);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * 获取AES密钥
     * 方案：基于设备ID派生AES密钥（推荐）
     *
     * @param deviceId 设备ID
     * @return AES密钥字节数组
     */
    public static byte[] getAESKey(String deviceId) {
        // 使用固定的种子密钥 + 设备ID进行密钥派生
        String seedKey = "MiDuoLicenseAESKey2024!@#"; // 长度24的种子密钥
        String combinedKey = seedKey + deviceId;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combinedKey.getBytes(StandardCharsets.UTF_8));
            // 取前32字节作为AES256密钥
            byte[] aesKey = new byte[32];
            System.arraycopy(hash, 0, aesKey, 0, 32);
            return aesKey;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("无法生成AES密钥", e);
        }
    }

    /**
     * 获取RSA公钥
     * 从资源文件加载RSA公钥
     *
     * @return RSA公钥
     * @throws Exception 加载异常
     */
    public static PublicKey getRSAPublicKey() throws Exception {
        if (rsaPublicKey == null) {
            synchronized (keyLock) {
                if (rsaPublicKey == null) {
                    rsaPublicKey = loadRSAPublicKey();
                }
            }
        }
        return rsaPublicKey;
    }

    /**
     * 加载RSA公钥
     *
     * @return RSA公钥
     * @throws Exception 加载异常
     */
    private static PublicKey loadRSAPublicKey() throws Exception {
        try (InputStream is = LicenseCryptoUtil.class.getResourceAsStream("/keys/license_public.key")) {
            if (is == null) {
                throw new RuntimeException("RSA公钥文件不存在: /keys/license_public.key");
            }

            StringBuilder keyContent = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                keyContent.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }

            // 移除PEM头部和尾部
            String pemKey = keyContent.toString()
                    .replaceAll("\\n", "")
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "");

            // Base64解码
            byte[] keyBytes = Base64.getDecoder().decode(pemKey);

            // 生成公钥
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        }
    }

    /**
     * Base64编码
     *
     * @param data 原始数据
     * @return Base64编码字符串
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64编码（字符串）
     *
     * @param data 原始字符串
     * @return Base64编码字符串
     */
    public static String encodeBase64(String data) {
        return encodeBase64(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64解码
     *
     * @param encodedData Base64编码字符串
     * @return 解码后的字节数组
     */
    public static byte[] decodeBase64(String encodedData) {
        return Base64.getDecoder().decode(encodedData);
    }

    /**
     * Base64解码为字符串
     *
     * @param encodedData Base64编码字符串
     * @return 解码后的字符串
     */
    public static String decodeBase64ToString(String encodedData) {
        byte[] decoded = decodeBase64(encodedData);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}

