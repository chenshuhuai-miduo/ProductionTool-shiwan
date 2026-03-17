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
 * <p><b>继电器板协议（8字节帧）：</b><br>
 * 帧格式：{@code 55 01 01 R1 R2 R3 R4 SUM}，其中 Rx 值含义：
 * <pre>
 *   0 = 保持原状态
 *   1 = 断开（关闭）
 *   2 = 吸合（开启）
 *   SUM = 前7字节之和的低8位
 * </pre>
 * 默认继电器接线约定：
 * <pre>
 *   继电器1 → 报警红灯（长亮）
 *   继电器2 → 蜂鸣器
 *   继电器3 → 黄灯
 *   继电器4 → 绿灯
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
    /** 全部关闭：手动关闭报警或定时熄灭时使用（21 0F 00 00 00 08 01 69 3C A3） */
    public static final byte[] CMD_ALL_OFF        = {
        0x21, 0x0F, 0x00, 0x00, 0x00, 0x08, 0x01, 0x69, 0x3C, (byte) 0xA3
    };

    // ==================== 剔除装置串口命令字节 ====================

    /** 触发剔除：有问题箱需要推出时发送 */
    public static final byte[] CMD_REJECT_TRIGGER = { 0x01 };
    /** 收回复位：剔除装置未自动复位时手动发送 */
    public static final byte[] CMD_REJECT_RESET   = { 0x00 };

    // ==================== 继电器板命令帧（8字节，55 01 01 R1 R2 R3 R4 SUM） ====================

    /** 继电器值：保持原状态 */
    private static final int RELAY_KEEP = 0;
    /** 继电器值：断开（关闭） */
    private static final int RELAY_OFF  = 1;
    /** 继电器值：吸合（开启） */
    private static final int RELAY_ON   = 2;

    /**
     * 红灯长亮 + 蜂鸣：继电器1（红灯）吸合 + 继电器2（蜂鸣器）吸合，
     * 用于上传失败 / 关联失败报警场景。
     */
    public static final byte[] RELAY_CMD_ALARM_ON  = buildRelayFrame(RELAY_ON,  RELAY_ON,  RELAY_KEEP, RELAY_KEEP);

    /**
     * 断开所有继电器：关闭红灯、蜂鸣器、黄灯、绿灯，
     * 用于「关闭报警」或事件解除场景。
     */
    public static final byte[] RELAY_CMD_ALARM_OFF = buildRelayFrame(RELAY_OFF, RELAY_OFF, RELAY_OFF,  RELAY_OFF);

    /**
     * 黄灯长亮：继电器3（黄灯）吸合，其余保持，
     * 用于盒码校验失败等采集级告警。
     */
    public static final byte[] RELAY_CMD_YELLOW_ON = buildRelayFrame(RELAY_KEEP, RELAY_KEEP, RELAY_ON,  RELAY_KEEP);

    /**
     * 绿灯长亮：继电器4（绿灯）吸合，其余保持，
     * 用于垛上传成功提示。
     */
    public static final byte[] RELAY_CMD_GREEN_ON  = buildRelayFrame(RELAY_KEEP, RELAY_KEEP, RELAY_KEEP, RELAY_ON);

    // ==================== 设备类别代码 ====================

    /** 报警器在 DeviceConnectionManager 中的类别代码 */
    private static final int CATEGORY_ALARM        = 5;
    /** 剔除装置在 DeviceConnectionManager 中的类别代码 */
    private static final int CATEGORY_REJECT       = 6;
    /** 继电器板在 DeviceConnectionManager 中的类别代码 */
    private static final int CATEGORY_RELAY_BOARD  = 8;

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
        send(CATEGORY_RELAY_BOARD, RELAY_CMD_GREEN_ON, "继电器板绿灯亮");
        if (sent) {
            greenOffFuture = scheduler.schedule(() -> {
                System.out.println("[硬件] 绿灯自动熄灭（60s）");
                send(CATEGORY_ALARM, CMD_ALL_OFF, "绿灯自动熄灭");
                send(CATEGORY_RELAY_BOARD, RELAY_CMD_ALARM_OFF, "继电器板绿灯自动熄灭");
            }, GREEN_AUTO_OFF_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 红灯常亮 + 蜂鸣（上传失败 / 关联失败）。
     * 同时向继电器板发送吸合指令（继电器1=红灯长亮，继电器2=蜂鸣器）。
     * 需要用户手动点击「关闭报警」按钮才能停止。
     */
    public void redLightAndBuzzer() {
        cancelGreenOffTimer();
        send(CATEGORY_ALARM, CMD_RED_BUZZ, "红灯+蜂鸣");
        send(CATEGORY_RELAY_BOARD, RELAY_CMD_ALARM_ON, "继电器板报警吸合（红灯+蜂鸣）");
    }

    /**
     * 黄灯常亮（盒码校验失败等采集级告警）。
     * 同时向继电器板发送黄灯吸合指令（继电器3）。
     */
    public void yellowLightOn() {
        cancelGreenOffTimer();
        send(CATEGORY_ALARM, CMD_YELLOW_ON, "黄灯亮");
        send(CATEGORY_RELAY_BOARD, RELAY_CMD_YELLOW_ON, "继电器板黄灯亮");
    }

    /**
     * 关闭所有灯光及蜂鸣（「关闭报警」按钮/绿灯定时熄灭均调用此方法）。
     * 同时向继电器板发送断开所有继电器指令，确保报警灯和蜂鸣器完全停止。
     */
    public void allLightsOff() {
        cancelGreenOffTimer();
        send(CATEGORY_ALARM, CMD_ALL_OFF, "关闭所有灯光");
        send(CATEGORY_RELAY_BOARD, RELAY_CMD_ALARM_OFF, "继电器板断开所有（关闭报警）");
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

    /**
     * 构建继电器板 8 字节控制帧。
     * <p>帧格式：{@code 55 01 01 R1 R2 R3 R4 SUM}，SUM = 前 7 字节之和的低 8 位。
     *
     * @param r1 第1路继电器（0=保持, 1=断开, 2=吸合）
     * @param r2 第2路继电器
     * @param r3 第3路继电器
     * @param r4 第4路继电器
     * @return 8 字节命令帧
     */
    public static byte[] buildRelayFrame(int r1, int r2, int r3, int r4) {
        byte[] cmd = new byte[8];
        cmd[0] = (byte) 0x55;
        cmd[1] = 0x01;
        cmd[2] = 0x01;
        cmd[3] = (byte) r1;
        cmd[4] = (byte) r2;
        cmd[5] = (byte) r3;
        cmd[6] = (byte) r4;
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (cmd[i] & 0xFF);
        }
        cmd[7] = (byte) (sum & 0xFF);
        return cmd;
    }
}
