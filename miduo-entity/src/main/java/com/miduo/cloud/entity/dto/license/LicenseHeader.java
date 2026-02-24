package com.miduo.cloud.entity.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 许可证头部信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseHeader {

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 许可证类型
     */
    private String type;

    /**
     * 密钥版本
     */
    private String keyVersion;

    /**
     * 算法标识
     */
    private String algorithm = "AES256+RSA2048";
}






















