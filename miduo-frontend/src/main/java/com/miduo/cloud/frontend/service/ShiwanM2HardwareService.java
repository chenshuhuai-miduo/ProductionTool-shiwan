package com.miduo.cloud.frontend.service;

import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;

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

    /** 上传成功灯自动恢复正常的延时（秒），文档规定 1 分钟 */
    private static final long UPLOAD_SUCCESS_REVERT_SECONDS = 60;

    // ==================== 状态机 ====================

    /** 设备整体基础状态枚举 */
    public enum BaseState {
        /** 正常运行（工控机灯+龙门架灯亮） */
        NORMAL,
        /** 上传成功（三灯亮，60秒后自动切回 NORMAL） */
        UPLOAD_SUCCESS,
        /** 上传失败报警（红灯+蜂鸣，持续到手动关闭） */
        UPLOAD_FAIL
    }

    /** 当前基础状态 */
    private volatile BaseState currentBaseState = BaseState.NORMAL;
    /** 剔除是否激活（激活时发送含剔除位的组合命令） */
    private volatile boolean rejectActive = false;

    /** 上传成功灯自动恢复计时器 */
    private ScheduledFuture<?> uploadSuccessRevertFuture;
    /** 剔除自动收回计时器 */
    private ScheduledFuture<?> rejectRetractFuture;

    // ==================== 单例 ====================

    private static volatile ShiwanM2HardwareService INSTANCE;

    private final DeviceConnectionManager deviceManager;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hw-timer");
                t.setDaemon(true);
                return t;
            });

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

    // ==================== 状态机核心方法 ====================

    /**
     * 进入正常运行状态（工控机灯+龙门架灯亮）。
     * 适用场景：开始采集、收回剔除后、关闭报警恢复正常。
     */
    public void enterNormalState() {
        cancelUploadSuccessTimer();
        currentBaseState = BaseState.NORMAL;
        sendCurrentState("进入正常状态");
    }

    /**
     * 上传成功：发送上传成功命令（三灯亮），60秒后自动恢复正常状态。
     */
    public void onUploadSuccess() {
        cancelUploadSuccessTimer();
        currentBaseState = BaseState.UPLOAD_SUCCESS;
        sendCurrentState("上传成功");
        uploadSuccessRevertFuture = scheduler.schedule(() -> {
            if (currentBaseState == BaseState.UPLOAD_SUCCESS) {
                System.out.println("[硬件] 上传成功灯60秒自动恢复正常状态");
                currentBaseState = BaseState.NORMAL;
                sendCurrentState("上传成功60s自动恢复");
            }
        }, UPLOAD_SUCCESS_REVERT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 上传失败报警：发送上传失败命令（红灯+蜂鸣），持续直到手动调用 {@link #enterNormalState()}。
     */
    public void onUploadFail() {
        cancelUploadSuccessTimer();
        currentBaseState = BaseState.UPLOAD_FAIL;
        sendCurrentState("上传失败报警");
    }

    /**
     * 触发剔除装置。
     * <p>流程：等待 rejectTriggerDelayMs → 发送当前状态+剔除命令 →
     * 等待 rejectRetractTimeMs → 自动发送当前状态（无剔除）命令完成收回。
     */
    public void triggerRejection() {
        int delayMs   = getAlarmConfig().getRejectTriggerDelayMs();
        int retractMsRaw = getAlarmConfig().getRejectRetractTimeMs();
        final int retractMs = retractMsRaw <= 0 ? 2000 : retractMsRaw;

        Runnable doTrigger = () -> {
            rejectActive = true;
            sendCurrentState("触发剔除");
            cancelRejectRetractTimer();
            final int retract = retractMs;
            rejectRetractFuture = scheduler.schedule(() -> {
                System.out.println("[硬件] 剔除自动收回（" + retract + "ms）");
                rejectActive = false;
                sendCurrentState("剔除自动收回");
            }, retract, TimeUnit.MILLISECONDS);
        };

        if (delayMs > 0) {
            scheduler.schedule(doTrigger, delayMs, TimeUnit.MILLISECONDS);
        } else {
            doTrigger.run();
        }
    }

    /**
     * 手动收回剔除装置（取消自动收回计时，立即恢复当前基础状态）。
     */
    public void retractRejection() {
        cancelRejectRetractTimer();
        rejectActive = false;
        sendCurrentState("手动收回剔除");
    }

    /**
     * 全部关闭：发送全关命令，重置状态机，用于退出软件时调用。
     */
    public void allOff() {
        cancelUploadSuccessTimer();
        cancelRejectRetractTimer();
        rejectActive = false;
        currentBaseState = BaseState.NORMAL;
        byte[] bytes = hexStringToBytes(getSignalConfig().getCmdAllOff());
        if (bytes != null) {
            send(CATEGORY_ALARM, bytes, "全关");
        }
    }

    /**
     * 根据当前基础状态和剔除激活状态，选择并发送对应命令。
     */
    private void sendCurrentState(String desc) {
        ShiwanM2Settings.DeviceSignalConfig sig = getSignalConfig();
        String hex;
        switch (currentBaseState) {
            case UPLOAD_SUCCESS:
                hex = rejectActive ? sig.getCmdUploadSuccessWithReject() : sig.getCmdUploadSuccess();
                break;
            case UPLOAD_FAIL:
                hex = rejectActive ? sig.getCmdUploadFailWithReject() : sig.getCmdUploadFail();
                break;
            default: // NORMAL
                hex = rejectActive ? sig.getCmdNormalWithReject() : sig.getCmdNormalOn();
        }
        byte[] bytes = hexStringToBytes(hex);
        if (bytes != null) {
            send(CATEGORY_ALARM, bytes, desc + " [" + currentBaseState + " reject=" + rejectActive + "]");
        } else {
            System.err.printf("[硬件] [%s] 对应hex未配置，跳过发送%n", desc);
        }
    }

    // ==================== 旧接口（保持兼容，委托到状态机）====================

    /** @deprecated 委托到 {@link #onUploadSuccess()} */
    public void greenLightOn() {
        onUploadSuccess();
    }

    /** @deprecated 委托到 {@link #onUploadFail()} */
    public void redLightAndBuzzer() {
        onUploadFail();
    }

    /**
     * 黄灯告警（盒码校验失败等采集级告警，不改变整体状态）。
     * 当前场景待确认，暂不改变状态机状态。
     */
    public void yellowLightOn() {
        // TODO: 此场景信号待用户确认后映射到状态机，当前暂不发送串口命令
        System.out.println("[硬件] [黄灯告警] 盒码校验失败场景，信号待配置");
    }

    /** @deprecated 委托到 {@link #enterNormalState()} */
    public void allLightsOff() {
        enterNormalState();
    }

    // ==================== 手动按钮信号（读取配置中的手动信号字段）====================

    /** 主界面"打开报警"按钮：发送 alarmOpenHex 信号 */
    public void openAlarm() {
        sendConfigSignal(CATEGORY_ALARM, getSignalConfig().getAlarmOpenHex(), "手动打开报警");
    }

    /** 主界面"关闭报警"按钮：发送 alarmCloseHex 信号 */
    public void closeAlarm() {
        sendConfigSignal(CATEGORY_ALARM, getSignalConfig().getAlarmCloseHex(), "手动关闭报警");
    }

    /** 主界面"测试报警灯亮"按钮：发送 alarmLightOnHex 信号 */
    public void alarmLightOn() {
        sendConfigSignal(CATEGORY_ALARM, getSignalConfig().getAlarmLightOnHex(), "测试报警灯亮");
    }

    /** 主界面"测试报警灯灭"按钮：发送 alarmLightOffHex 信号 */
    public void alarmLightOff() {
        sendConfigSignal(CATEGORY_ALARM, getSignalConfig().getAlarmLightOffHex(), "测试报警灯灭");
    }

    /** 主界面"触发剔除"手动按钮：发送 rejectTriggerHex 信号 */
    public void triggerRejectCustom() {
        sendConfigSignal(CATEGORY_ALARM, getSignalConfig().getRejectTriggerHex(), "手动触发剔除");
    }

    /** 主界面"收回剔除"手动按钮：发送 rejectRetractHex 信号 */
    public void retractRejectCustom() {
        sendConfigSignal(CATEGORY_ALARM, getSignalConfig().getRejectRetractHex(), "手动收回剔除");
    }

    // ==================== 内部工具 ====================

    private ShiwanM2Settings.DeviceSignalConfig getSignalConfig() {
        ShiwanM2Settings.DeviceSignalConfig cfg = ShiwanM2SettingsStore.get().getDeviceSignal();
        return cfg != null ? cfg : new ShiwanM2Settings.DeviceSignalConfig();
    }

    private ShiwanM2Settings.AlarmConfig getAlarmConfig() {
        ShiwanM2Settings.AlarmConfig cfg = ShiwanM2SettingsStore.get().getAlarm();
        return cfg != null ? cfg : new ShiwanM2Settings.AlarmConfig();
    }

    private void sendConfigSignal(int categoryCode, String hexStr, String desc) {
        byte[] bytes = hexStringToBytes(hexStr);
        if (bytes == null) {
            System.err.printf("[硬件] [%s] 自定义信号未配置（hex为空），请在系统设置-设备信号中填写%n", desc);
            return;
        }
        send(categoryCode, bytes, desc);
    }

    private void cancelUploadSuccessTimer() {
        if (uploadSuccessRevertFuture != null && !uploadSuccessRevertFuture.isDone()) {
            uploadSuccessRevertFuture.cancel(false);
            uploadSuccessRevertFuture = null;
        }
    }

    private void cancelRejectRetractTimer() {
        if (rejectRetractFuture != null && !rejectRetractFuture.isDone()) {
            rejectRetractFuture.cancel(false);
            rejectRetractFuture = null;
        }
    }

    /**
     * 将16进制字符串解析为字节数组。
     * 支持带空格（"01 0A FF"）、无分隔符（"010AFF"）、冒号（"01:0A:FF"）等格式。
     *
     * @return 字节数组；字符串为空或null时返回null
     */
    public static byte[] hexStringToBytes(String hex) {
        if (hex == null) return null;
        hex = hex.replaceAll("[^0-9A-Fa-f]", "");
        if (hex.isEmpty()) return null;
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
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
