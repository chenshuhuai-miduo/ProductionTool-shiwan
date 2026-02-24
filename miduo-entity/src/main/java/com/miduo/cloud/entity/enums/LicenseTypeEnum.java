package com.miduo.cloud.entity.enums;

/**
 * 许可证类型枚举
 */
public enum LicenseTypeEnum {

    /**
     * 试用许可证
     */
    TRIAL("TRIAL", "试用版"),

    /**
     * 正式许可证
     */
    STANDARD("STANDARD", "正式版");

    private final String code;
    private final String description;

    LicenseTypeEnum(String code, String description) {
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
    public static LicenseTypeEnum fromCode(String code) {
        for (LicenseTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的许可证类型代码: " + code);
    }
}






















