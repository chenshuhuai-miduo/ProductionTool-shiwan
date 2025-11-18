package com.miduo.cloud.frontend.util;

import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.entity.po.OperateLog;

import java.time.LocalDateTime;

/**
 * 操作日志构建器
 * 使用Builder模式简化日志记录
 */
public class OperateLogBuilder {
    
    private final OperateLog operateLog;
    
    private OperateLogBuilder() {
        this.operateLog = new OperateLog();
        // 设置默认值
        this.operateLog.setOperatorName("系统");
        this.operateLog.setType(1); // 操作日志
        this.operateLog.setOperateResult("成功");
        this.operateLog.setOperateTime(LocalDateTime.now());
        this.operateLog.setOperatorIp("");
        this.operateLog.setDeviceInfo("JavaFX桌面应用");
    }
    
    /**
     * 创建日志构建器
     */
    public static OperateLogBuilder create() {
        return new OperateLogBuilder();
    }
    
    /**
     * 设置操作人
     */
    public OperateLogBuilder operator(String operatorName) {
        this.operateLog.setOperatorName(operatorName);
        return this;
    }
    
    /**
     * 设置操作人IP
     */
    public OperateLogBuilder ip(String operatorIp) {
        this.operateLog.setOperatorIp(operatorIp);
        return this;
    }
    
    /**
     * 设置操作模块（使用枚举）
     */
    public OperateLogBuilder module(ModuleNameEnum moduleNameEnum) {
        this.operateLog.setModuleName(moduleNameEnum.getDescription());
        return this;
    }
    
    /**
     * 设置操作模块（使用字符串）
     */
    public OperateLogBuilder module(String moduleName) {
        this.operateLog.setModuleName(moduleName);
        return this;
    }
    
    /**
     * 设置日志类型：1-操作日志  2-系统日志
     */
    public OperateLogBuilder type(int type) {
        this.operateLog.setType(type);
        return this;
    }
    
    /**
     * 设置操作类型（使用枚举）
     */
    public OperateLogBuilder operateType(OperateTypeEnum operateTypeEnum) {
        this.operateLog.setOperateType(operateTypeEnum.getDescription());
        return this;
    }
    
    /**
     * 设置操作类型（使用字符串）
     */
    public OperateLogBuilder operateType(String operateType) {
        this.operateLog.setOperateType(operateType);
        return this;
    }
    
    /**
     * 设置操作目标
     */
    public OperateLogBuilder target(String targetId, String targetName) {
        this.operateLog.setTargetId(targetId);
        this.operateLog.setTargetName(targetName);
        return this;
    }
    
    /**
     * 设置操作内容
     */
    public OperateLogBuilder content(String operateContent) {
        this.operateLog.setOperateContent(operateContent);
        return this;
    }
    
    /**
     * 设置操作前数据
     */
    public OperateLogBuilder beforeData(Object data) {
        try {
            if (data != null) {
                this.operateLog.setBeforeData(HttpUtil.getObjectMapper().writeValueAsString(data));
            }
        } catch (Exception e) {
            System.err.println("[操作日志] beforeData序列化失败: " + e.getMessage());
        }
        return this;
    }
    
    /**
     * 设置操作后数据
     */
    public OperateLogBuilder afterData(Object data) {
        try {
            if (data != null) {
                this.operateLog.setAfterData(HttpUtil.getObjectMapper().writeValueAsString(data));
            }
        } catch (Exception e) {
            System.err.println("[操作日志] afterData序列化失败: " + e.getMessage());
        }
        return this;
    }
    
    /**
     * 设置操作结果
     */
    public OperateLogBuilder result(String operateResult) {
        this.operateLog.setOperateResult(operateResult);
        return this;
    }
    
    /**
     * 设置失败原因
     */
    public OperateLogBuilder failReason(String failReason) {
        this.operateLog.setFailReason(failReason);
        this.operateLog.setOperateResult("失败");
        return this;
    }
    
    /**
     * 设置设备信息
     */
    public OperateLogBuilder deviceInfo(String deviceInfo) {
        this.operateLog.setDeviceInfo(deviceInfo);
        return this;
    }
    
    /**
     * 设置备注
     */
    public OperateLogBuilder remark(String remark) {
        this.operateLog.setRemark(remark);
        return this;
    }
    
    /**
     * 构建操作日志对象
     */
    public OperateLog build() {
        return this.operateLog;
    }
    
    /**
     * 保存日志（同步）
     */
    public void save() {
        try {
            // 通过HTTP调用后端保存日志（使用HttpUtil已配置好的ObjectMapper进行序列化）
            String json = HttpUtil.getObjectMapper().writeValueAsString(this.operateLog);
            HttpUtil.doPost("/api/log/operate", json);
        } catch (Exception e) {
            System.err.println("[操作日志] 保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步保存日志（不阻塞主线程）
     */
    public void saveAsync() {
        new Thread(this::save).start();
    }
}

