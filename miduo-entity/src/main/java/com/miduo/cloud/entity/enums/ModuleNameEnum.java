package com.miduo.cloud.entity.enums;

/**
 * 模块名称枚举
 */
public enum ModuleNameEnum {
    
    // 业务模块
    CODE_VALIDATE("码校验"),
    CODE_COLLECT("箱码采集"),
    CODE_ASSOCIATE_PALLET("托盘码关联"),
    CODE_ASSOCIATE_BOX("箱码关联"),
    CODE_QUERY("码查询"),
    CODE_REPLACE("码替换"),
    BARCODE_SCAN("扫码枪"),
    
    // 任务管理
    TASK_MANAGEMENT("任务管理"),
    ORDER_MANAGEMENT("订单管理"),
    
    // 系统配置
    SYSTEM_CONFIG("系统配置"),
    DEVICE_MANAGEMENT("设备管理"),
    
    // 其他
    DATA_STATISTICS("数据统计"),
    SYSTEM_LOG("系统日志");
    
    private final String description;
    
    ModuleNameEnum(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

