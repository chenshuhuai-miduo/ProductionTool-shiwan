package com.miduo.cloud.frontend.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 石湾2号机硬件控制服务
 *
 * <p><b>串口通信原理（jSerialComm）：</b><br>
 * Java 通过 jSerialComm 库打开 RS-232/RS-485 串口，调用 {@code writeBytes(byte[], int)} 将
 * 控制字节直接写入串口输出流，硬件设备（报警灯/剔除装置）收到字节后按其内置协议执行对应动作。
 * 整个过程无需额外网络层，属于点对点异步字节流通信。
 *
 * <p><b>三色声光报警灯协议（默认）：</b><br>
 * 大多数国内工业三色声光报警灯（RS-232 接口）采用单字节命令，每个比特位控制一路输出：
 * <pre>
 *   Bit0 = 绿灯      0x01 → 绿灯亮
 *   Bit1 = 红灯      0x02 → 红灯亮
 *   Bit2 = 黄灯      0x04 → 黄灯亮
 *   Bit3 = 蜂鸣器    0x08 → 蜂鸣
 *   组合：0x0A (0x02|0x08) → 红灯 + 蜂鸣
 *         0x00              → 全部关闭
 * </pre>
 * 若实际设备协议不同（如需要帧头/校验），只需修改下方 {@code CMD_*} 常量及 {@link #send} 方法。
 *
 * <p><b>剔除装置协议（默认）：</b><br>
 * 多数 PLC 控制剔除装置通过 RS-232 接收单字节触发信号：
 * <pre>
 *   0x01 → 触发剔除（装置动作推出问题箱）
 *   0x00 → 复位收回（若不支持自动复位时使用）
 * </pre>
 *
 * <p><b>使用方法：</b><br>
 * 获取单例后调用对应方法即可；绿灯会在 60 秒后自动熄灭。
 */
public class ShiwanM2HardwareService {

    // ==================== 报警灯串口命令字节 ====================

    /** 绿灯常亮：上传成功时使用，1 分钟后自动熄灭 */
    public static final byte[] CMD_GREEN_ON       = { 0x01 };
    /** 红灯常亮 + 蜂鸣：上传失败 / 关联失败时使用 */
    public static final byte[] CMD_RED_BUZZ       = { 0x0A }; // 0x02 | 0x08
    /** 黄灯常亮：采集警告（盒码失败等）时使用 */
    public static final byte[] CMD_YELLOW_ON      = { 0x04 };
    /** 全部关闭：手动关闭报警或定时熄灭时使用 */
    public static final byte[] CMD_ALL_OFF        = { 0x00 };

    // ==================== 剔除装置串口命令字节 ====================

    /** 触发剔除：有问题箱需要推出时发送 */
    public static final byte[] CMD_REJECT_TRIGGER = { 0x01 };
    /** 收回复位：剔除装置未自动复位时手动发送 */
    public static final byte[] CMD_REJECT_RESET   = { 0x00 };

    // ==================== 设备类别代码 ====================

    /** 报警器在 DeviceConnectionManager 中的类别代码 */
    private static final int CATEGORY_ALARM  = 5;
    /** 剔除装置在 DeviceConnectionManager 中的类别代码 */
    private static final int CATEGORY_REJECT = 6;

    /** 绿灯自动熄灭延时（秒），文档规定 1 分钟 */
    private static final long GREEN_AUTO_OFF_SECONDS = 60;

    // ==================== 单例 ====================

    private static volatile ShiwanM2HardwareService INSTANCE;

    private final DeviceConnectionManager deviceManager;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hw-timer");
                t.setDaemon(true);
                return t;
            });

    /** 正在运行的绿灯熄灭计时器（非 null 时取消旧计时再启新计时） */
    private ScheduledFuture<?> greenOffFuture;

    private ShiwanM2HardwareService() {
        this.deviceManager = DeviceConnectionManager.getInstance();
    }

    public static ShiwanM2HardwareService getInstance() {
        if (INSTANCE == null) {
            synchronized (ShiwanM2HardwareService.class) {
                if (INSTANCE == null) INSTANCE = new ShiwanM2HardwareService();
            }
        }
        return INSTANCE;
    }

    // ==================== 报警灯控制 ====================

    /**
     * 绿灯常亮（上传成功）。
     * 60 秒后自动调用 {@link #allLightsOff()} 熄灭。
     */
    public void greenLightOn() {
        cancelGreenOffTimer();
        boolean sent = send(CATEGORY_ALARM, CMD_GREEN_ON, "绿灯亮");
        if (sent) {
            greenOffFuture = scheduler.schedule(() -> {
                System.out.println("[硬件] 绿灯自动熄灭（60s）");
                send(CATEGORY_ALARM, CMD_ALL_OFF, "绿灯自动熄灭");
            }, GREEN_AUTO_OFF_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 红灯常亮 + 蜂鸣（上传失败 / 关联失败）。
     * 需要用户手动点击「关闭报警」按钮才能停止。
     */
    public void redLightAndBuzzer() {
        cancelGreenOffTimer();
        send(CATEGORY_ALARM, CMD_RED_BUZZ, "红灯+蜂鸣");
    }

    /**
     * 黄灯常亮（盒码校验失败等采集级告警）。
     */
    public void yellowLightOn() {
        cancelGreenOffTimer();
        send(CATEGORY_ALARM, CMD_YELLOW_ON, "黄灯亮");
    }

    /**
     * 关闭所有灯光及蜂鸣（「关闭报警」按钮/绿灯定时熄灭均调用此方法）。
     */
    public void allLightsOff() {
        cancelGreenOffTimer();
        send(CATEGORY_ALARM, CMD_ALL_OFF, "关闭所有灯光");
    }

    // ==================== 剔除装置控制 ====================

    /**
     * 触发剔除装置推出问题箱（盒箱关联失败时自动调用）。
     */
    public void triggerRejection() {
        send(CATEGORY_REJECT, CMD_REJECT_TRIGGER, "触发剔除");
    }

    /**
     * 复位/收回剔除装置（「收回剔除」按钮调用）。
     */
    public void retractRejection() {
        send(CATEGORY_REJECT, CMD_REJECT_RESET, "收回剔除");
    }

    // ==================== 内部工具 ====================

    /**
     * 通过 DeviceConnectionManager 向指定类别设备发送字节命令。
     *
     * @param categoryCode 设备类别代码
     * @param cmd          命令字节
     * @param desc         日志描述
     * @return 发送成功返回 true
     */
    private boolean send(int categoryCode, byte[] cmd, String desc) {
        boolean ok = deviceManager.sendBytesToDeviceByCategory(categoryCode, cmd);
        if (ok) {
            System.out.printf("[硬件] [%s] 指令已发送: %s%n", desc, bytesToHex(cmd));
        } else {
            System.err.printf("[硬件] [%s] 发送失败（设备未连接或串口异常）%n", desc);
        }
        return ok;
    }

    private void cancelGreenOffTimer() {
        if (greenOffFuture != null && !greenOffFuture.isDone()) {
            greenOffFuture.cancel(false);
            greenOffFuture = null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
