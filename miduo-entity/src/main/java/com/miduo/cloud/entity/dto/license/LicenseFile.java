package com.miduo.cloud.entity.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 许可证文件结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseFile {

    /**
     * 许可证头部
     */
    private LicenseHeader header;

    /**
     * 许可证载荷（JSON字符串）
     */
    private String payload;

    /**
     * RSA签名
     */
    private String signature;
}





























