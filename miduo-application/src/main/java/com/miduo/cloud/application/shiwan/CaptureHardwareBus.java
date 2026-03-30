package com.miduo.cloud.application.shiwan;

/**
 * 采集硬件动作总线（发布-订阅解耦）。
 * <p>
 * 与 {@link UploadLogBus} 模式一致：后端采集服务（ShiwanM2TcpCaptureService）在
 * 检测到关联失败 / 码校验失败时，通过本类 <b>立即</b> 通知前端执行串口硬件指令，
 * 彻底绕过每 1 秒一次的 HTTP 轮询，消除 0-1s 的轮询延迟。
 * </p>
 *
 * <pre>
 * 触发时机：
 *   ASSOC_FAIL   → triggerRejection()  盒箱关联失败，触发剔除装置
 *   BOX_FAIL     → yellowLightOn()     码校验失败，黄灯告警（实物由后续箱级剔除处理）
 * </pre>
 */
public final class CaptureHardwareBus {

    /** 硬件动作枚举 */
    public enum HardwareAction {
        /** 关联失败 → 触发剔除装置 + 红灯 + 蜂鸣 */
        TRIGGER_REJECTION,
        /** 码校验失败 → 黄灯告警 */
        YELLOW_LIGHT_ON
    }

    /** 硬件动作监听器（由前端 ShiwanM2HardwareService 实现并注册） */
    @FunctionalInterface
    public interface HardwareListener {
        void onHardwareAction(HardwareAction action);
    }

    private static volatile HardwareListener listener;

    private CaptureHardwareBus() {}

    /**
     * 注册监听器（前端 initialize 时调用）。
     * 可重复调用覆盖旧监听器；传 null 则清除监听器。
     */
    public static void register(HardwareListener l) {
        listener = l;
    }

    /**
     * 关联失败时调用：立即通知前端触发剔除。
     * 在 addEvent("ASSOC_FAIL", ...) 的同一位置调用，不等待 HTTP 轮询。
     */
    public static void triggerRejection() {
        fire(HardwareAction.TRIGGER_REJECTION);
    }

    /**
     * 码校验失败时调用：立即通知前端黄灯告警。
     * 在 addEvent("BOX_FAIL", ...) 的同一位置调用，不等待 HTTP 轮询。
     */
    public static void yellowLightOn() {
        fire(HardwareAction.YELLOW_LIGHT_ON);
    }

    private static void fire(HardwareAction action) {
        HardwareListener l = listener;
        if (l != null) {
            try {
                l.onHardwareAction(action);
            } catch (Throwable t) {
                // 硬件调用不影响主业务流程
            }
        }
    }
}
