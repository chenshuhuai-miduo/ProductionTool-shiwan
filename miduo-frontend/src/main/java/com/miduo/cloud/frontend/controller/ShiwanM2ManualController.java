package com.miduo.cloud.frontend.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 手工采集 Tab 控制器
 * <p>
 * 1号机相机损坏时手工完成瓶盒关联采集，支持扫码枪依次扫瓶码和盒码。
 * </p>
 */
public class ShiwanM2ManualController implements Initializable {

    // ==================== FXML 注入 ====================

    @FXML private TextField bottlesPerBoxField;
    @FXML private Label     promptCode1;
    @FXML private Label     promptCode2;
    @FXML private ListView<ShiwanM2MainController.LogEntry> dataLogList;
    @FXML private ListView<ShiwanM2MainController.LogEntry> opLogList;
    @FXML private Label     currentCountLabel;
    @FXML private Label     perBoxCountLabel;
    @FXML private Label     totalCountLabel;
    @FXML private Button    startBtn;

    // ==================== 内部状态 ====================

    private static final int MAX_LOG = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<ShiwanM2MainController.LogEntry> dataLogItems = FXCollections.observableArrayList();
    private final ObservableList<ShiwanM2MainController.LogEntry> opLogItems   = FXCollections.observableArrayList();

    private boolean isRunning   = false;
    private int currentBottles  = 0;
    private int totalBoxes      = 0;

    /** 当前采集阶段：true = 等待扫瓶码，false = 等待扫盒码 */
    private boolean waitingBottle = true;

    private Timeline specSyncTimeline;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dataLogList.setItems(dataLogItems);
        dataLogList.setCellFactory(lv -> new ShiwanM2MainController.LogCell());

        opLogList.setItems(opLogItems);
        opLogList.setCellFactory(lv -> new ShiwanM2MainController.LogCell());

        bottlesPerBoxField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                perBoxCountLabel.setText(val.trim());
            }
        });

        String now = now();
        addOpLog(now + "  进入手工采集模式 - 关联类型：瓶盒关联", ShiwanM2MainController.LogType.INFO);
        addOpLog(now + "  设备状态：扫码枪已就绪，等待扫码", ShiwanM2MainController.LogType.INFO);

        refreshPrompt();
    }

    // ==================== 提示区 ====================

    private void refreshPrompt() {
        if (waitingBottle) {
            promptCode1.getStyleClass().remove("sw2-prompt-code-wait");
            promptCode1.getStyleClass().add("sw2-prompt-code-active");
            promptCode2.getStyleClass().remove("sw2-prompt-code-active");
            promptCode2.getStyleClass().add("sw2-prompt-code-wait");
            promptCode1.setText("瓶码");
            promptCode2.setText("盒码");
        } else {
            promptCode1.getStyleClass().remove("sw2-prompt-code-active");
            promptCode1.getStyleClass().add("sw2-prompt-code-wait");
            promptCode2.getStyleClass().remove("sw2-prompt-code-wait");
            promptCode2.getStyleClass().add("sw2-prompt-code-active");
            promptCode1.setText("瓶码");
            promptCode2.setText("盒码");
        }
    }

    // ==================== 任务控制 ====================

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
        startBtn.setText("停止采集");
        startBtn.getStyleClass().removeAll("sw2-btn-primary");
        startBtn.getStyleClass().add("shiwan-m2-ctrl-btn-primary");
        startBtn.getStyleClass().add("running");

        bottlesPerBoxField.setEditable(false);

        String now = now();
        addOpLog(now + "  手工采集已启动，请按提示扫码", ShiwanM2MainController.LogType.INFO);
        addDataLog(now + "  系统就绪，请扫描第1个瓶码", ShiwanM2MainController.LogType.INFO);
    }

    private void stopCapture() {
        isRunning = false;
        startBtn.setText("开始采集");
        startBtn.getStyleClass().remove("running");
        startBtn.getStyleClass().remove("shiwan-m2-ctrl-btn-primary");
        startBtn.getStyleClass().add("sw2-btn-primary");

        bottlesPerBoxField.setEditable(true);
        addOpLog(now() + "  手工采集已停止", ShiwanM2MainController.LogType.INFO);
    }

    @FXML
    private void onReset() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("清零确认");
        confirm.setHeaderText("确认将生产总数和当前读数清零？");
        confirm.setContentText("此操作仅重置界面计数，不影响已关联数据。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            currentBottles = 0;
            totalBoxes     = 0;
            currentCountLabel.setText("0");
            totalCountLabel.setText("0");
            waitingBottle = true;
            refreshPrompt();
            addOpLog(now() + "  计数已清零", ShiwanM2MainController.LogType.INFO);
        }
    }

    // ==================== 外部调用（扫码枪回调）====================

    /**
     * 收到一条扫码数据（由上层服务调用）
     *
     * @param code 扫描到的码值
     */
    public void onScanCode(String code) {
        if (!isRunning) return;
        Platform.runLater(() -> {
            int bpb = parseBottlesPerBox();
            String now = now();
            if (waitingBottle) {
                currentBottles++;
                currentCountLabel.setText(String.valueOf(currentBottles));
                addDataLog(now + "  瓶码采集：" + code, ShiwanM2MainController.LogType.DATA);
                if (currentBottles >= bpb) {
                    waitingBottle = false;
                    addDataLog(now + "  扫码完成，请扫盒码", ShiwanM2MainController.LogType.DATA);
                }
            } else {
                addDataLog(now + "  盒码关联成功：" + code + "（" + bpb + "瓶→1盒）",
                        ShiwanM2MainController.LogType.SUCCESS);
                totalBoxes++;
                totalCountLabel.setText(String.valueOf(totalBoxes));
                currentBottles = 0;
                currentCountLabel.setText("0");
                waitingBottle = true;
            }
            refreshPrompt();
        });
    }

    // ==================== 日志 ====================

    public void addDataLog(String msg, ShiwanM2MainController.LogType type) {
        Platform.runLater(() -> {
            while (dataLogItems.size() >= MAX_LOG) dataLogItems.remove(dataLogItems.size() - 1);
            dataLogItems.add(0, new ShiwanM2MainController.LogEntry(msg, type));
        });
    }

    public void addOpLog(String msg, ShiwanM2MainController.LogType type) {
        Platform.runLater(() -> {
            while (opLogItems.size() >= MAX_LOG) opLogItems.remove(opLogItems.size() - 1);
            opLogItems.add(0, new ShiwanM2MainController.LogEntry(msg, type));
        });
    }

    // ==================== 工具 ====================

    private String now() {
        return LocalDateTime.now().format(TIME_FMT);
    }

    private int parseBottlesPerBox() {
        try {
            int v = Integer.parseInt(bottlesPerBoxField.getText().trim());
            return v > 0 ? v : 6;
        } catch (NumberFormatException e) {
            return 6;
        }
    }
}
