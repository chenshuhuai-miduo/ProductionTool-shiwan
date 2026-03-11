package com.miduo.cloud.entity.enums;

/**
 * 码包类型枚举
 */
public enum CodePackageTypeEnum {

    SMALL(1, "盖外码小标"),
    MEDIUM(2, "盒外码中标"),
    BIG(3, "箱外码大标");

    private final int code;
    private final String desc;

    CodePackageTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CodePackageTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CodePackageTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
