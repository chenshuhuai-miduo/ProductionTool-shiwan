package com.miduo.cloud.entity.enums;

/**
 * 操作类型枚举
 */
public enum OperateTypeEnum {
    
    // 基础操作
    ADD("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    QUERY("查询"),
    
    // 任务操作
    START("启用"),
    STOP("停用"),
    COMPLETE("完成"),
    
    // 业务操作
    VALIDATE("校验"),
    COLLECT("采集"),
    ASSOCIATE("关联"),
    REPLACE("替换"),
    CLEAR("清除"),
    SPECIFY("指定"),
    FORCE("强制"),
    
    // 设备操作
    CONNECT("连接"),
    DISCONNECT("断开"),
    TEST("测试"),
    
    // 系统操作
    LOGIN("登录"),
    LOGOUT("退出"),
    EXPORT("导出"),
    IMPORT("导入");
    
    private final String description;
    
    OperateTypeEnum(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

