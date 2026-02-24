package com.miduo.cloud.entity.enums;

/**
 * 许可证状态枚举
 */
public enum LicenseStatusEnum {

    /**
     * 未激活 - 没有找到许可证文件或许可证无效
     */
    UNACTIVATED("unactivated", "未激活"),

    /**
     * 试用期内 - 有试用许可证且未过期
     */
    TRIAL_ACTIVE("trial_active", "试用期内"),

    /**
     * 试用已过期 - 试用许可证已过期
     */
    TRIAL_EXPIRED("trial_expired", "试用已过期"),

    /**
     * 已激活 - 有正式许可证且未过期
     */
    ACTIVATED("activated", "已激活"),

    /**
     * 已过期 - 正式许可证已过期
     */
    EXPIRED("expired", "已过期");

    private final String code;
    private final String description;

    LicenseStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举值
     */
    public static LicenseStatusEnum fromCode(String code) {
        for (LicenseStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的许可证状态代码: " + code);
    }

    /**
     * 判断是否为激活状态（试用期内或已激活）
     */
    public boolean isActive() {
        return this == TRIAL_ACTIVE || this == ACTIVATED;
    }

    /**
     * 判断是否为过期状态
     */
    public boolean isExpired() {
        return this == TRIAL_EXPIRED || this == EXPIRED;
    }

    /**
     * 判断是否为试用状态
     */
    public boolean isTrial() {
        return this == TRIAL_ACTIVE || this == TRIAL_EXPIRED;
    }
}






















