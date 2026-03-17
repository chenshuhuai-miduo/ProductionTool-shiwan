package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2ManualController.class);

    /** 静态实例引用，供 ShiwanM2MainController 分发扫码枪数据时调用 */
    private static volatile ShiwanM2ManualController instance;

    public static ShiwanM2ManualController getInstance() {
        return instance;
    }

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
        instance = this;

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
        startBtn.getStyleClass().add("running");

        bottlesPerBoxField.setEditable(false);

        String now = now();
        addOpLog(now + "  手工采集已启动，请按提示扫码", ShiwanM2MainController.LogType.INFO);
        addDataLog(now + "  系统就绪，请扫描第1个瓶码", ShiwanM2MainController.LogType.INFO);
        OperateLogBuilder.create()
                .module(ModuleNameEnum.MANUAL_COLLECT)
                .operateType(OperateTypeEnum.START)
                .content("手工采集已启动，规格：每盒" + parseBottlesPerBox() + "瓶")
                .saveAsync();
    }

    private void stopCapture() {
        isRunning = false;
        startBtn.setText("开始采集");
        startBtn.getStyleClass().remove("running");

        bottlesPerBoxField.setEditable(true);
        addOpLog(now() + "  手工采集已停止", ShiwanM2MainController.LogType.INFO);
        OperateLogBuilder.create()
                .module(ModuleNameEnum.MANUAL_COLLECT)
                .operateType(OperateTypeEnum.STOP)
                .content("手工采集已停止，累计完成盒关联：" + totalBoxes + " 盒")
                .saveAsync();
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

    /** 防止并发扫码时多次触发校验 */
    private volatile boolean isValidating = false;

    /**
     * 收到一条扫码数据（由 ShiwanM2MainController 在扫码枪设备 category=7 数据到达时调用）。
     * 校验顺序：码位数（本地）→ 热表 → CodeRelationUpload 重码（后端 API）。
     *
     * @param code 扫描到的码值
     */
    public void onScanCode(String code) {
        log.info("[手工采集] 扫码枪收到数据: {} | isRunning={}", code, isRunning);
        if (!isRunning) {
            log.info("[手工采集] 当前未启动采集，忽略扫码: {}", code);
            return;
        }
        if (isValidating) {
            log.info("[手工采集] 上一码正在校验中，忽略新码: {}", code);
            return;
        }
        final String trimmedCode = code != null ? code.trim() : "";
        if (trimmedCode.isEmpty()) return;

        Platform.runLater(() -> {
            final int bpb = parseBottlesPerBox();
            // 当前阶段：瓶码=1，盒码=2
            final int packageType = waitingBottle ? 1 : 2;
            final String codeTypeName = waitingBottle ? "瓶码" : "盒码";
            final String now = now();

            // 1. 码位数校验（本地读取系统设置）
            ShiwanM2Settings settings = ShiwanM2SettingsStore.get();
            if (settings != null && settings.getCodeDigits() != null) {
                ShiwanM2Settings.CodeDigitsConfig digitsConfig = settings.getCodeDigits();
                int expectedDigits = packageType == 1
                        ? digitsConfig.getSmallCodeDigits()
                        : digitsConfig.getMediumCodeDigits();
                if (expectedDigits > 0 && trimmedCode.length() != expectedDigits) {
                    log.warn("[手工采集] {}位数不符 code={} expected={} actual={}",
                            codeTypeName, trimmedCode, expectedDigits, trimmedCode.length());
                    addDataLog(now + "  【" + codeTypeName + "位数不符】需 " + expectedDigits
                            + " 位，实 " + trimmedCode.length() + " 位：" + trimmedCode,
                            ShiwanM2MainController.LogType.ERROR);
                    return;
                }
            }

            // 2. 后端校验：热表 + CodeRelationUpload 重码
            isValidating = true;
            String reqBody = "{\"code\":\"" + trimmedCode + "\",\"packageType\":" + packageType + "}";
            HttpUtil.asyncPost(
                    "/api/shiwan-m2/manual/validate-code",
                    reqBody,
                    json -> {
                        isValidating = false;
                        try {
                            JsonNode node = HttpUtil.getObjectMapper().readTree(json);
                            int respCode = node.has("code") ? node.get("code").asInt() : -1;
                            String nowInner = now();
                            if (respCode != 200) {
                                String msg = node.has("message") ? node.get("message").asText() : "校验失败";
                                log.warn("[手工采集] {}校验不通过 code={} msg={}", codeTypeName, trimmedCode, msg);
                                addDataLog(nowInner + "  【" + codeTypeName + "校验不通过】" + msg,
                                        ShiwanM2MainController.LogType.ERROR);
                                OperateLogBuilder.create()
                                        .module(ModuleNameEnum.MANUAL_COLLECT)
                                        .operateType(OperateTypeEnum.VALIDATE)
                                        .target(trimmedCode, codeTypeName)
                                        .content("手工采集" + codeTypeName + "校验不通过：" + trimmedCode)
                                        .failReason(msg)
                                        .saveAsync();
                                return;
                            }
                            // 校验通过，执行采集逻辑
                            processValidatedCode(trimmedCode, packageType, bpb);
                        } catch (Exception e) {
                            isValidating = false;
                            log.error("[手工采集] 解析校验响应异常 code={}", trimmedCode, e);
                            addDataLog(now() + "  【" + codeTypeName + "校验响应异常】" + e.getMessage(),
                                    ShiwanM2MainController.LogType.ERROR);
                        }
                    },
                    err -> {
                        isValidating = false;
                        log.error("[手工采集] 校验请求失败 code={}", trimmedCode, err);
                        addDataLog(now() + "  【" + codeTypeName + "校验请求失败】" + err.getMessage(),
                                ShiwanM2MainController.LogType.ERROR);
                    }
            );
        });
    }

    /** 校验通过后执行采集入账逻辑（在 FX 线程执行） */
    private void processValidatedCode(String code, int packageType, int bpb) {
        String now = now();
        if (packageType == 1) {
            // 瓶码
            currentBottles++;
            currentCountLabel.setText(String.valueOf(currentBottles));
            log.info("[手工采集] 瓶码 #{}: {} (当前已扫 {}/{})", currentBottles, code, currentBottles, bpb);
            addDataLog(now + "  瓶码采集：" + code, ShiwanM2MainController.LogType.DATA);
            if (currentBottles >= bpb) {
                waitingBottle = false;
                log.info("[手工采集] 瓶码已满 {} 个，等待扫盒码", bpb);
                addDataLog(now + "  扫码完成，请扫盒码", ShiwanM2MainController.LogType.DATA);
            }
        } else {
            // 盒码
            log.info("[手工采集] 盒码: {} ({}瓶→1盒，累计 {} 盒)", code, bpb, totalBoxes + 1);
            addDataLog(now + "  盒码关联成功：" + code + "（" + bpb + "瓶→1盒）",
                    ShiwanM2MainController.LogType.SUCCESS);
            totalBoxes++;
            totalCountLabel.setText(String.valueOf(totalBoxes));
            OperateLogBuilder.create()
                    .module(ModuleNameEnum.MANUAL_COLLECT)
                    .operateType(OperateTypeEnum.COLLECT)
                    .target(code, code)
                    .content("手工采集盒码关联成功：盒码=" + code + "，已关联" + bpb + "瓶，累计完成" + totalBoxes + "盒")
                    .saveAsync();
            currentBottles = 0;
            currentCountLabel.setText("0");
            waitingBottle = true;
        }
        refreshPrompt();
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
