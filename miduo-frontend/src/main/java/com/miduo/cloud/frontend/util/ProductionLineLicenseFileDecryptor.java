package com.miduo.cloud.frontend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
// com.miduo.cloud.common.exception.Exception;
//import com.miduo.cloud.service.productionline.config.ProductionLineLicenseCryptoProperties;
import com.miduo.cloud.frontend.config.ProductionLineLicenseCryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

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
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * 产线许可证文件（.lic）解密器：RSA2048验签 + AES256解密
 *
 * @author 米多团队
 */
@Slf4j
public class ProductionLineLicenseFileDecryptor {

    private final ObjectMapper objectMapper;
    private final ProductionLineLicenseCryptoProperties cryptoProperties;

    public ProductionLineLicenseFileDecryptor(ObjectMapper objectMapper, ProductionLineLicenseCryptoProperties cryptoProperties) {
        this.objectMapper = objectMapper;
        this.cryptoProperties = cryptoProperties;
    }

    /**
     * 解密许可证文件内容
     *
     * @param licenseFileBase64 许可证文件的Base64编码内容
     * @return 解密后的明文数据（Map格式）
     */
    public Map<String, Object> decryptLicenseFile(String licenseFileBase64) throws Exception {
        if (licenseFileBase64 == null || licenseFileBase64.trim().isEmpty()) {
            throw new Exception("许可证文件内容不能为空");
        }

        try {
            // 1. Base64解码许可证文件
            //byte[] fileBytes = Base64.getDecoder().decode(licenseFileBase64);
            @SuppressWarnings("unchecked")
            Map<String, Object> licenseFile = objectMapper.readValue(licenseFileBase64, Map.class);

            // 2. 提取header、payload、signature
            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>) licenseFile.get("header");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) licenseFile.get("payload");
            String signatureB64 = (String) licenseFile.get("signature");

            if (header == null || payload == null || signatureB64 == null) {
                throw new Exception("许可证文件格式错误：缺少必要字段");
            }

            // 3. 验证签名
            String ivB64 = (String) payload.get("iv");
            String cipherB64 = (String) payload.get("data");
            String headerJson = objectMapper.writeValueAsString(header);
            String signText = headerJson + "|" + ivB64 + "|" + cipherB64;

            boolean signatureValid = verifySignature(signText.getBytes(StandardCharsets.UTF_8), signatureB64);
            if (!signatureValid) {
                throw new Exception("许可证文件签名验证失败");
            }

            // 4. AES-GCM解密
            byte[] aesKey = decodeBase64Flexible(cryptoProperties.getAesKeyBase64());
            if (aesKey.length != 32) {
                throw new Exception("AES密钥长度错误，必须为32字节");
            }

            byte[] iv = Base64.getDecoder().decode(ivB64);
            byte[] cipherBytes = Base64.getDecoder().decode(cipherB64);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            // 5. 验证checksum
            String expectedChecksum = (String) payload.get("checksum");
            String actualChecksum = DigestUtils.md5Hex(plainBytes);
            if (!expectedChecksum.equals(actualChecksum)) {
                throw new Exception("许可证文件数据校验失败，可能已被篡改");
            }

            // 6. 解析明文数据
            String plainJson = new String(plainBytes, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> plainData = objectMapper.readValue(plainJson, Map.class);

            return plainData;
        } catch (Exception e) {
            log.error("解密许可证文件失败", e);
            throw new Exception("解密许可证文件失败：" + e.getMessage());
        }
    }

    /**
     * 验证RSA签名
     */
    private boolean verifySignature(byte[] data, String signatureB64) {
        try {
            // 使用配置的公钥验证签名
            String rsaPublicKeyPem = cryptoProperties.getRsaPublicKeyBase64();
            if (rsaPublicKeyPem == null || rsaPublicKeyPem.trim().isEmpty()) {
                throw new Exception("RSA公钥未配置，无法验证签名");
            }

            // 从PEM格式提取Base64并解码
            byte[] keyBytes = extractPublicKeyFromPem(rsaPublicKeyPem);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(keyBytes));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data);

            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            log.error("签名验证失败", e);
            return false;
        }
    }

    /**
     * 从PEM格式提取公钥Base64并解码
     * 支持带/不带头尾的PEM格式
     */
    private byte[] extractPublicKeyFromPem(String pem) throws Exception {
        String cleaned = pem;

        // 移除PEM头尾
        cleaned = cleaned.replaceAll("-----BEGIN PUBLIC KEY-----", "");
        cleaned = cleaned.replaceAll("-----END PUBLIC KEY-----", "");

        // 移除空白字符
        cleaned = cleaned.replaceAll("\\s+", "");

        // Base64解码
        return decodeBase64Flexible(cleaned);
    }


    private String normalizeBase64(String raw) {
        return (raw == null ? "" : raw).replaceAll("\\s+", "");
    }

    private byte[] decodeBase64Flexible(String raw) throws Exception {
        String s = normalizeBase64(raw);
        if (s.isEmpty()) throw new Exception("Base64字符串为空");
        s = s.replaceAll("=+$", "");
        int mod = s.length() % 4;
        if (mod == 1) throw new Exception("非法的Base64长度");
        int pad = (4 - mod) % 4;
        if (pad > 0) s = s + "===".substring(0, pad);
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            try {
                return Base64.getUrlDecoder().decode(s);
            } catch (IllegalArgumentException ex) {
                throw new Exception("Base64解码失败");
            }
        }
    }

    /**
     * 从私钥生成并获取公钥Base64（辅助方法，用于配置）
     */
    public static String generatePublicKeyFromPrivate(String rsaPrivateKeyPkcs8Base64) {
        try {
            // 解析私钥
            String normalized = rsaPrivateKeyPkcs8Base64;

            // 移除PEM头尾
            normalized = normalized.replaceAll("-----BEGIN RSA PRIVATE KEY-----", "");
            normalized = normalized.replaceAll("-----END RSA PRIVATE KEY-----", "");

            // 移除空白字符
            normalized = normalized.replaceAll("\\s+", "");
            normalized = normalized.replaceAll("\\s+", "").replaceAll("=+$", "");
            int mod = normalized.length() % 4;
            if (mod > 0) normalized += "===".substring(0, (4 - mod) % 4);
            byte[] der = Base64.getDecoder().decode(normalized);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey;
            try {
                privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(der));
            } catch (Exception e) {
                // PKCS#1转换
                byte[] pkcs8 = wrapPkcs1ToPkcs8(der);
                privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            }

            // 提取公钥
            RSAPublicKeySpec publicKeySpec = kf.getKeySpec(privateKey, RSAPublicKeySpec.class);
            PublicKey publicKey = kf.generatePublic(publicKeySpec);

            // 编码为X.509
            return Base64.getEncoder().encodeToString(publicKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("从私钥生成公钥失败", e);
        }
    }

    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1Der) throws Exception {
        if (pkcs1Der == null || pkcs1Der.length == 0) {
            throw new Exception("私钥格式错误");
        }
        byte[] rsaOid = new byte[] { 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01 };
        byte[] rsaNull = new byte[] { 0x05, 0x00 };
        byte[] algId = derSeq(concat(rsaOid, rsaNull));
        byte[] version = new byte[] { 0x02, 0x01, 0x00 };
        byte[] privateKeyOctet = derOctetString(pkcs1Der);
        return derSeq(concat(concat(version, algId), privateKeyOctet));
    }

    private static byte[] derSeq(byte[] content) { return derWrap((byte) 0x30, content); }
    private static byte[] derOctetString(byte[] content) { return derWrap((byte) 0x04, content); }

    private static byte[] derWrap(byte tag, byte[] content) {
        byte[] len = derLength(content == null ? 0 : content.length);
        byte[] body = content == null ? new byte[0] : content;
        byte[] out = new byte[1 + len.length + body.length];
        out[0] = tag;
        System.arraycopy(len, 0, out, 1, len.length);
        System.arraycopy(body, 0, out, 1 + len.length, body.length);
        return out;
    }

    private static byte[] derLength(int length) {
        if (length < 128) return new byte[] { (byte) length };
        int numBytes = 0;
        int tmp = length;
        while (tmp > 0) { numBytes++; tmp >>= 8; }
        byte[] out = new byte[1 + numBytes];
        out[0] = (byte) (0x80 | numBytes);
        for (int i = numBytes; i > 0; i--) {
            out[i] = (byte) (length & 0xFF);
            length >>= 8;
        }
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] aa = a == null ? new byte[0] : a;
        byte[] bb = b == null ? new byte[0] : b;
        byte[] out = new byte[aa.length + bb.length];
        System.arraycopy(aa, 0, out, 0, aa.length);
        System.arraycopy(bb, 0, out, aa.length, bb.length);
        return out;
    }
}





