package com.miduo.cloud.frontend.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * 石湾1号机（瓶盒关联软件）主界面控制器
 * <p>
 * 对应 FXML：ShiwanM1MainWindow.fxml
 * 需求文档：P01-01-主界面-瓶盒关联.md
 * </p>
 */
public class ShiwanM1MainController implements Initializable {

    // ==================== FXML 注入 ====================

    /** 每盒瓶数输入框（默认6） */
    @FXML private TextField bottlesPerBoxField;

    /** 合计瓶数统计标签（红色大字） */
    @FXML private Label totalBottlesLabel;

    /** 当前盒数统计标签（蓝色大字） */
    @FXML private Label totalBoxesLabel;

    /** 开始/停止采集按钮 */
    @FXML private Button startCaptureBtn;

    /** 数据接收区 ListView */
    @FXML private ListView<LogEntry> dataLogList;

    /** 操作日志 ListView */
    @FXML private ListView<LogEntry> opLogList;

    /** 右侧分隔面板 */
    @FXML private SplitPane rightSplitPane;

    /** 许可证状态容器 */
    @FXML private HBox licenseStatusBox;
    /** 许可证状态圆点 */
    @FXML private Region licenseStatusDot;
    /** 许可证状态文本 */
    @FXML private Label licenseStatusLabel;

    /** 状态栏：采集中运行指示标签 */
    @FXML private Label runningStatusLabel;

    /** 状态栏：当前时间标签 */
    @FXML private Label currentTimeLabel;

    // ==================== 内部状态 ====================

    private static final int MAX_LOG_ENTRIES = 1000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObservableList<LogEntry> dataLogItems = FXCollections.observableArrayList();
    private final ObservableList<LogEntry> opLogItems   = FXCollections.observableArrayList();

    private boolean isRunning = false;
    private int totalBottles  = 0;
    private int totalBoxes    = 0;

    /** 时钟定时器 */
    private Timeline clockTimeline;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListViews();
        setupClock();
        setupInitialLogs();
        initActivationStatus();
    }

    /** 配置 ListView 的 CellFactory，根据日志类型渲染不同样式 */
    private void setupListViews() {
        dataLogList.setItems(dataLogItems);
        dataLogList.setCellFactory(lv -> new LogCell());

        opLogList.setItems(opLogItems);
        opLogList.setCellFactory(lv -> new LogCell());
    }

    /** 启动实时时钟，每秒刷新一次 */
    private void setupClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            currentTimeLabel.setText(LocalDateTime.now().format(DATETIME_FMT));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
        currentTimeLabel.setText(LocalDateTime.now().format(DATETIME_FMT));
    }

    /** 初始化欢迎日志 */
    private void setupInitialLogs() {
        addOpLog("系统启动完成，等待设备连接...", LogType.NORMAL);
    }

    /** 初始化并更新许可证状态显示 */
    private void initActivationStatus() {
        updateLicenseStatus();
    }

    /** 点击许可证状态标签，打开许可证信息弹窗 */
    @FXML
    private void onLicenseStatusClick(javafx.scene.input.MouseEvent event) {
        try {
            LicenseInfoController.showLicenseInfo();
            updateLicenseStatus();
        } catch (Exception e) {
            showErrorAlert("打开许可证信息失败", e.getMessage());
        }
    }

    /** 从许可证服务读取状态并更新状态栏 UI */
    private void updateLicenseStatus() {
        try {
            com.miduo.cloud.frontend.service.DeviceInfoService deviceInfoService =
                new com.miduo.cloud.frontend.service.DeviceInfoService();
            com.miduo.cloud.frontend.service.LicenseService licenseService =
                new com.miduo.cloud.frontend.service.LicenseService(
                    new com.miduo.cloud.frontend.service.LicenseValidationService());
            licenseService.init();

            String currentDeviceId = com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator
                .generateDeviceId(deviceInfoService.getDeviceInfo());

            com.miduo.cloud.entity.enums.LicenseStatusEnum status =
                licenseService.getCurrentLicenseStatus(currentDeviceId);
            com.miduo.cloud.frontend.service.LicenseService.LicenseInfo licenseInfo =
                licenseService.getLicenseInfo(currentDeviceId);
            long remainingDays = licenseInfo.getRemainingDays();

            String statusType;
            String statusText;
            String tooltipText;
            String expiredDay = licenseInfo.getExpireDate() != null
                ? licenseInfo.getExpireDate().toString() : "未知";

            switch (status) {
                case ACTIVATED:
                    if (remainingDays > 30) {
                        statusType = "normal";
                        statusText = "授权剩余: " + remainingDays + "天";
                        tooltipText = "到期：" + expiredDay;
                    } else if (remainingDays >= 7) {
                        statusType = "warning";
                        statusText = "授权剩余: " + remainingDays + "天";
                        tooltipText = "到期：" + expiredDay + "\n⚠即将到期，请尽快续期";
                    } else if (remainingDays > 0) {
                        statusType = "urgent";
                        statusText = "授权剩余: " + remainingDays + "天";
                        tooltipText = "到期：" + expiredDay + "\n⚠紧急，请立即续期！";
                    } else {
                        statusType = "urgent";
                        statusText = "授权剩余: 不足1天";
                        tooltipText = "到期：" + expiredDay + "\n⚠紧急，请立即续期！";
                    }
                    break;
                case TRIAL_ACTIVE:
                    statusType = "trial";
                    statusText = remainingDays > 0
                        ? "试用剩余: " + remainingDays + "天"
                        : "试用剩余: 不足1天";
                    tooltipText = "试用模式\n到期: " + expiredDay + "\n💡 点击激活正式版";
                    break;
                case TRIAL_EXPIRED:
                    statusType = "expired";
                    statusText = "试用已过期";
                    tooltipText = "试用期已结束\n必须激活才能使用";
                    break;
                case EXPIRED:
                    statusType = "expired";
                    statusText = "已过期";
                    tooltipText = "过期时间：" + expiredDay + "\n❌已过期，必须续期";
                    break;
                case UNACTIVATED:
                default:
                    statusType = "expired";
                    statusText = "未激活";
                    tooltipText = "软件未激活\n💡 点击进行激活";
                    break;
            }

            Platform.runLater(() -> {
                licenseStatusBox.getStyleClass().removeAll(
                    "license-normal", "license-warning", "license-urgent",
                    "license-expired", "license-trial");
                licenseStatusBox.getStyleClass().add("license-" + statusType);
                licenseStatusLabel.setText(statusText);
                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
                tooltip.setShowDelay(javafx.util.Duration.millis(200));
                javafx.scene.control.Tooltip.install(licenseStatusBox, tooltip);
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                licenseStatusBox.getStyleClass().removeAll(
                    "license-normal", "license-warning", "license-urgent",
                    "license-expired", "license-trial");
                licenseStatusBox.getStyleClass().add("license-expired");
                licenseStatusLabel.setText("状态未知");
            });
        }
    }

    // ==================== 菜单事件 ====================

    /** 文件 → 退出软件 */
    @FXML
    private void onExit() {
        if (isRunning) {
            showInfoAlert("请先停止采集", "请先点击「停止采集」停止当前采集任务，再退出软件。");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("退出确认");
        confirm.setHeaderText(null);
        confirm.setContentText("确认退出米多赋码采集关联系统？");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                stopClock();
                Platform.exit();
            }
        });
    }

    /** 配置 → 系统设置 */
    @FXML
    private void onSystemConfig() {
        try {
            //SystemConfigController.showSystemConfig();
        } catch (Exception e) {
            showErrorAlert("打开系统设置失败", e.getMessage());
        }
    }

    /** 配置 → 许可证信息 */
    @FXML
    private void onLicenseInfo() {
        try {
            LicenseInfoController.showLicenseInfo();
        } catch (Exception e) {
            showErrorAlert("打开许可证信息失败", e.getMessage());
        }
    }

    /** 数据 → 操作日志 */
    @FXML
    private void onOperationLog() {
        try {
            //OperateLogController.showOperateLog();
        } catch (Exception e) {
            showErrorAlert("打开操作日志失败", e.getMessage());
        }
    }

    /** 帮助 → 操作帮助 */
    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("操作帮助");
        alert.setHeaderText("瓶盒关联模式 操作说明");
        alert.setContentText(
            "【日常操作流程】\n\n" +
            "1. 确认采集规格：左侧「采集规格设置」中确认「1盒 X瓶」。\n" +
            "2. 开始采集：点击「开始采集」按钮，系统检测设备后自动采集。\n" +
            "3. 监控采集：右侧「数据接收区」显示实时读码结果；绿色=成功，红色=异常。\n" +
            "4. 停止采集：点击「停止采集」按钮；未完成的盒数据将保留。\n\n" +
            "【报警说明】\n" +
            "• 短报警（重码/错码等单次异常）：自动关闭，无需干预。\n" +
            "• 长报警（设备未连接、连续重码3次等）：点击「关闭报警」重置。\n\n" +
            "如需更多帮助，请联系米多技术支持：15917372153"
        );
        alert.showAndWait();
    }

    /** 帮助 → 关于系统 */
    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于系统");
        alert.setHeaderText("米多赋码采集关联系统 v1.2");
        alert.setContentText(
            "关联模式：瓶盒关联\n" +
            "部署站点：石湾产线\n\n" +
            "版权所有 © 米多科技\n" +
            "技术支持：15917372153"
        );
        alert.showAndWait();
    }

    // ==================== 任务控制事件 ====================

    /**
     * 开始/停止采集按钮点击
     * <p>
     * 未运行时：检测设备 → 开始采集（按钮变红"停止采集"）<br>
     * 运行中时：停止采集（按钮变蓝"开始采集"）
     * </p>
     */
    @FXML
    private void onToggleCapture() {
        if (!isRunning) {
            startCapture();
        } else {
            stopCapture();
        }
    }

    private void startCapture() {
        isRunning = true;
        startCaptureBtn.setText("停止采集");
        startCaptureBtn.getStyleClass().add("running");
        bottlesPerBoxField.setDisable(true);

        runningStatusLabel.setVisible(true);
        runningStatusLabel.setManaged(true);

        addOpLog("开始采集", LogType.NORMAL);
        addOpLog("设备检测中...", LogType.NORMAL);

        // 模拟设备检测完成（实际应由 DeviceConnectionManager 回调）
        Platform.runLater(() -> {
            addOpLog("瓶1读码器 已连接", LogType.NORMAL);
            addOpLog("瓶2读码器 已连接", LogType.NORMAL);
            addOpLog("盒读码器 已连接", LogType.NORMAL);
        });
    }

    private void stopCapture() {
        isRunning = false;
        startCaptureBtn.setText("开始采集");
        startCaptureBtn.getStyleClass().remove("running");
        bottlesPerBoxField.setDisable(false);

        runningStatusLabel.setVisible(false);
        runningStatusLabel.setManaged(false);

        addOpLog("已停止采集", LogType.NORMAL);
    }

    /** 关闭报警按钮：重置长报警状态（停止声光报警器、蜂鸣器） */
    @FXML
    private void onCloseAlarm() {
        addOpLog("已手动关闭报警", LogType.NORMAL);
        // TODO: 对接 DeviceConnectionManager 发送关闭报警指令
    }

    /** 任务控制帮助图标（?） */
    @FXML
    private void onTaskHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("任务控制说明");
        alert.setHeaderText("任务控制");
        alert.setContentText(
            "【开始采集】\n" +
            "系统检测设备后开始采集；再次点击停止采集。\n\n" +
            "【关闭报警】\n" +
            "长报警时点击可重置报警状态（停止声光报警器、蜂鸣器）。\n" +
            "注意：短报警（重码/错码/超码/缺码等单次异常）自动关闭，无需手动操作。"
        );
        alert.showAndWait();
    }

    // ==================== 数据接收回调（供外部业务服务调用） ====================

    /**
     * 添加盒关联成功记录
     *
     * @param boxCode 盒读码
     */
    public void onBoxAssociationSuccess(String boxCode) {
        Platform.runLater(() -> {
            String ts = LocalDateTime.now().format(TIME_FMT);
            addDataLog(ts + "  盒读码：" + boxCode, LogType.SUCCESS);
            addDataLog(ts + "  盒码 " + boxCode + " 关联成功", LogType.SUCCESS);
            totalBoxes++;
            totalBoxesLabel.setText(String.valueOf(totalBoxes));
        });
    }

    /**
     * 添加瓶码读取记录
     *
     * @param scannerName 读码器名称，如"瓶1"、"瓶2"
     * @param codes       本次读到的码列表，逗号分隔
     */
    public void onBottleScanned(String scannerName, String codes) {
        Platform.runLater(() -> {
            String ts = LocalDateTime.now().format(TIME_FMT);
            addDataLog(ts + "  " + scannerName + "读码：" + codes, LogType.NORMAL);
        });
    }

    /**
     * 添加异常读码记录（重码/错码等）
     *
     * @param desc 异常描述，如"重码: xxxx（已标记剔除）"
     */
    public void onScanError(String desc) {
        Platform.runLater(() -> {
            String ts = LocalDateTime.now().format(TIME_FMT);
            addDataLog(ts + "  " + desc, LogType.ERROR);
        });
    }

    /**
     * 更新合计瓶数
     *
     * @param delta 本次新增有效瓶数
     */
    public void addBottleCount(int delta) {
        Platform.runLater(() -> {
            totalBottles += delta;
            totalBottlesLabel.setText(String.valueOf(totalBottles));
        });
    }

    // ==================== 日志工具 ====================

    /**
     * 向数据接收区写入一条日志（最新在顶部，超过 MAX_LOG_ENTRIES 时截断末尾）
     */
    private void addDataLog(String text, LogType type) {
        dataLogItems.add(0, new LogEntry(text, type));
        if (dataLogItems.size() > MAX_LOG_ENTRIES) {
            dataLogItems.remove(dataLogItems.size() - 1);
        }
    }

    /**
     * 向操作日志区写入一条日志（最新在顶部）
     */
    private void addOpLog(String text, LogType type) {
        String ts = LocalDateTime.now().format(TIME_FMT);
        opLogItems.add(0, new LogEntry(ts + "  " + text, type));
        if (opLogItems.size() > MAX_LOG_ENTRIES) {
            opLogItems.remove(opLogItems.size() - 1);
        }
    }

    // ==================== 工具方法 ====================

    private void showInfoAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void stopClock() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }

    // ==================== 内部类型 ====================

    /** 日志类型枚举 */
    public enum LogType {
        /** 成功（绿色） */
        SUCCESS,
        /** 错误/异常（红色） */
        ERROR,
        /** 普通信息（灰色） */
        NORMAL
    }

    /** 日志条目数据模型 */
    public static class LogEntry {
        private final String text;
        private final LogType type;

        public LogEntry(String text, LogType type) {
            this.text = text;
            this.type = type;
        }

        public String getText() { return text; }
        public LogType getType() { return type; }
    }

    /**
     * 自定义 ListCell：根据 LogType 应用不同的样式类，
     * 渲染带左侧彩色边框的日志条目标签。
     */
    private static class LogCell extends ListCell<LogEntry> {

        private final Label label = new Label();

        LogCell() {
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
            setGraphic(null);
            setPadding(new Insets(2, 4, 2, 4));
        }

        @Override
        protected void updateItem(LogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                label.getStyleClass().clear();
            } else {
                label.getStyleClass().clear();
                label.getStyleClass().add("log-cell-label");
                switch (item.getType()) {
                    case SUCCESS:
                        label.getStyleClass().add("log-cell-success");
                        break;
                    case ERROR:
                        label.getStyleClass().add("log-cell-error");
                        break;
                    default:
                        label.getStyleClass().add("log-cell-normal");
                        break;
                }
                label.setText(item.getText());
                setText(null);
                setGraphic(label);
            }
        }
    }
}
