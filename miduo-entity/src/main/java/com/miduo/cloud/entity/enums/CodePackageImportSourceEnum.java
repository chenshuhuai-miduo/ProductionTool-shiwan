package com.miduo.cloud.entity.enums;

/**
 * 码包导入方式枚举
 */
public enum CodePackageImportSourceEnum {

    ONLINE(1, "在线"),
    LOCAL(2, "本地");

    private final int code;
    private final String desc;

    CodePackageImportSourceEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CodePackageImportSourceEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CodePackageImportSourceEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
