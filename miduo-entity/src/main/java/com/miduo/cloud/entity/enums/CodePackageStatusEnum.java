package com.miduo.cloud.entity.enums;

/**
 * 码包导入记录状态枚举
 */
public enum CodePackageStatusEnum {

    NORMAL(1, "正常"),
    DELETED(-1, "已删除");

    private final int code;
    private final String desc;

    CodePackageStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CodePackageStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CodePackageStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
