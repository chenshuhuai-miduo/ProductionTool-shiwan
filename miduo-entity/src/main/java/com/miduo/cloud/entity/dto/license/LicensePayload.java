package com.miduo.cloud.entity.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 许可证载荷信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicensePayload {

    /**
     * 设备唯一ID（设备指纹 MD5）
     */
    private String deviceId;

    /**
     * 商户编号
     */
    private String merchantId;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 许可证激活码（20位，仅用于展示 / 追溯）
     */
    private String licenseKey;

    /**
     * 许可证类型：trial | official
     */
    private String licenseType;

    /**
     * 激活类型：first | renewal
     */
    private String activationType;

    /**
     * 激活时间
     */
    private String activationTime;

    /**
     * 到期日期
     */
    private LocalDate expireDate;

    /**
     * 有效天数
     */
    private Integer validDays;

    /**
     * 设备信息（用于强绑定）
     */
    private Object deviceInfo;

    /**
     * 完整性校验值
     * MD5(licenseKey + deviceId + expireDate)
     */
    private String checksum;

    // ===== getter / setter 省略 =====
}






























