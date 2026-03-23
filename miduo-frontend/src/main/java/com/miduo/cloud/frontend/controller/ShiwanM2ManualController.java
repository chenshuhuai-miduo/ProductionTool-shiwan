package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.FxDialog;
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
import java.util.List;
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
    @FXML private TextField codeInput;
    @FXML private Button    addBtn;
    @FXML private Label     promptCodeLabel;
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

        // 限制输入：1–999 正整数，禁止前导零（如 01）
        bottlesPerBoxField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (!newText.matches("[1-9]\\d{0,2}")) return null;
            int val2 = Integer.parseInt(newText);
            if (val2 < 1 || val2 > 999) return null;
            return change;
        }));

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
            promptCodeLabel.setText("瓶码");
            promptCodeLabel.getStyleClass().remove("sw2-prompt-code-red");
            if (!promptCodeLabel.getStyleClass().contains("sw2-prompt-code-active"))
                promptCodeLabel.getStyleClass().add("sw2-prompt-code-active");
            if (codeInput != null) codeInput.setPromptText("扫描或输入瓶码");
        } else {
            promptCodeLabel.setText("盒码");
            promptCodeLabel.getStyleClass().remove("sw2-prompt-code-active");
            if (!promptCodeLabel.getStyleClass().contains("sw2-prompt-code-red"))
                promptCodeLabel.getStyleClass().add("sw2-prompt-code-red");
            if (codeInput != null) codeInput.setPromptText("扫描或输入盒码");
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
        // 前端校验：每盒N瓶必须是正整数
        String bpbText = bottlesPerBoxField.getText();
        if (bpbText == null || bpbText.trim().isEmpty()) {
            FxDialog.warn(bottlesPerBoxField.getScene().getWindow(),
                    "采集规格错误", "请填写「每盒瓶数 N」（1盒N瓶），不能为空。");
            return;
        }
        int bpbCheck;
        try { bpbCheck = Integer.parseInt(bpbText.trim()); } catch (NumberFormatException e) {
            FxDialog.warn(bottlesPerBoxField.getScene().getWindow(),
                    "采集规格错误", "每盒瓶数 N 必须是数字。");
            return;
        }
        if (bpbCheck <= 0 || bpbCheck > 999) {
            FxDialog.warn(bottlesPerBoxField.getScene().getWindow(),
                    "采集规格错误", "每盒瓶数 N 必须在 1-999 之间。");
            return;
        }

        // 门禁：检查热表是否已导入小标和中标码包
        try {
            String checkJson = HttpUtil.doGet("/api/shiwan-m2/code-package/check-manual");
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(checkJson);
            boolean passed = root != null && root.has("data")
                    && root.get("data").has("passed")
                    && root.get("data").get("passed").asBoolean();
            if (!passed) {
                String msg = root != null && root.has("message") ? root.get("message").asText() : "请先导入码包";
                FxDialog.warn(bottlesPerBoxField.getScene().getWindow(), "码包未导入", msg);
                return;
            }
        } catch (Exception e) {
            log.warn("[手工采集] 码包检查失败: {}", e.getMessage());
            FxDialog.warn(bottlesPerBoxField.getScene().getWindow(),
                    "码包检查失败", "码包门禁检查失败，请确认后端服务正常：" + e.getMessage());
            return;
        }

        isRunning = true;
        startBtn.setText("停止采集");
        startBtn.getStyleClass().add("running");

        bottlesPerBoxField.setEditable(false);

        codeInput.setDisable(false);
        addBtn.setDisable(false);
        addBtn.setStyle(addBtn.getStyle()
                .replace("-fx-background-color: #D1D5DB", "-fx-background-color: #2563EB"));
        Platform.runLater(() -> codeInput.requestFocus());

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

        codeInput.setDisable(true);
        codeInput.clear();
        addBtn.setDisable(true);
        addBtn.setStyle(addBtn.getStyle()
                .replace("-fx-background-color: #2563EB", "-fx-background-color: #D1D5DB"));

        // 丢弃未完成关联的临时瓶码，重置计数和阶段
        if (currentBottles > 0) {
            addOpLog(now() + "  已丢弃未完成关联的 " + currentBottles + " 个瓶码，下次从瓶码重新开始",
                    ShiwanM2MainController.LogType.INFO);
        }
        currentBottles = 0;
        pendingBottleCodes.clear();
        waitingBottle = true;
        currentCountLabel.setText("0");
        refreshPrompt();

        addOpLog(now() + "  手工采集已停止", ShiwanM2MainController.LogType.INFO);
        OperateLogBuilder.create()
                .module(ModuleNameEnum.MANUAL_COLLECT)
                .operateType(OperateTypeEnum.STOP)
                .content("手工采集已停止，累计完成盒关联：" + totalBoxes + " 盒")
                .saveAsync();
    }

    /**
     * 码输入框按 Enter 键或点击「加入」按钮时触发。
     * 提交后：清空输入框、保持焦点，路由到 onScanCode 执行校验逻辑。
     */
    @FXML
    private void onCodeInputSubmit() {
        if (!isRunning) return;
        String code = codeInput.getText() != null ? codeInput.getText().trim() : "";
        codeInput.clear();
        codeInput.requestFocus();
        if (!code.isEmpty()) {
            onScanCode(code);
        }
    }

    @FXML
    private void onReset() {
        boolean ok = FxDialog.confirm(
                bottlesPerBoxField.getScene().getWindow(),
                "清零确认",
                "确认将生产总数和当前读数清零？\n\n此操作仅重置界面计数，不影响已关联数据。"
        );
        if (ok) {
            currentBottles = 0;
            totalBoxes     = 0;
            pendingBottleCodes.clear();
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

    /** 当前盒码关联的瓶码暂存列表，盒码扫描成功后提交并清空 */
    private final java.util.List<String> pendingBottleCodes = new java.util.ArrayList<>();

    /**
     * 收到一条扫码数据（由 ShiwanM2MainController 在扫码枪设备 category=7 数据到达时调用）。
     * 校验顺序：码位数（本地）→ 热表 → CodeRelationUpload 重码（后端 API）。
     *
     * @param code 扫描到的码值
     */
    public void onScanCode(String code) {
        log.debug("[手工采集] 扫码枪收到数据: {} | isRunning={}", code, isRunning);
        if (!isRunning) {
            log.debug("[手工采集] 当前未启动采集，忽略扫码: {}", code);
            return;
        }
        if (isValidating) {
            log.debug("[手工采集] 上一码正在校验中，忽略新码: {}", code);
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

            // 2. 本批次内瓶码重复检查（本地，快速，无需网络）
            if (packageType == 1 && pendingBottleCodes.contains(trimmedCode)) {
                log.warn("[手工采集] 瓶码重复 code={} 已在本批次中", trimmedCode);
                addDataLog(now + "  【瓶码重复】" + trimmedCode + " 已在本批次中，该码不计入，请重新扫码",
                        ShiwanM2MainController.LogType.ERROR);
                return;
            }

            // 3. 后端校验：热表 + CodeRelationUpload 重码
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
            pendingBottleCodes.add(code);
            currentCountLabel.setText(String.valueOf(currentBottles));
            log.debug("[手工采集] 瓶码 #{}: {} (当前已扫 {}/{})", currentBottles, code, currentBottles, bpb);
            addDataLog(now + "  瓶码采集：" + code, ShiwanM2MainController.LogType.DATA);
            if (currentBottles >= bpb) {
                waitingBottle = false;
                log.debug("[手工采集] 瓶码已满 {} 个，等待扫盒码", bpb);
                addDataLog(now + "  扫码完成，请扫盒码", ShiwanM2MainController.LogType.DATA);
            }
        } else {
            // 盒码：先写入关联表，再更新UI
            final String boxCode = code;
            final List<String> bottleCodesCopy = new java.util.ArrayList<>(pendingBottleCodes);
            final int currentTotalBoxes = totalBoxes + 1;
            log.debug("[手工采集] 盒码: {} ({}瓶→1盒，累计 {} 盒)", code, bpb, currentTotalBoxes);

            // 先重置计数和状态，避免重复扫码
            currentBottles = 0;
            pendingBottleCodes.clear();
            currentCountLabel.setText("0");
            waitingBottle = true;
            totalBoxes = currentTotalBoxes;
            totalCountLabel.setText(String.valueOf(totalBoxes));
            refreshPrompt();

            addDataLog(now + "  盒码关联成功：" + code + "（" + bpb + "瓶→1盒）",
                    ShiwanM2MainController.LogType.SUCCESS);

            // 异步写入码关系表（ProductNO/OrderNo 为空字符串）
            String reqBody = buildManualAssociateBody(boxCode, bottleCodesCopy);
            HttpUtil.asyncPost(
                    "/api/shiwan-m2/manual/associate",
                    reqBody,
                    json -> {
                        try {
                            com.fasterxml.jackson.databind.JsonNode node =
                                    HttpUtil.getObjectMapper().readTree(json);
                            int respCode = node.has("code") ? node.get("code").asInt() : -1;
                            if (respCode != 200) {
                                String msg = node.has("message") ? node.get("message").asText() : "写入失败";
                                log.warn("[手工采集] 码关系写入失败 boxCode={} msg={}", boxCode, msg);
                                Platform.runLater(() ->
                                        addDataLog(now() + "  【写入码关系失败】" + msg,
                                                ShiwanM2MainController.LogType.ERROR));
                            }
                        } catch (Exception e) {
                            log.error("[手工采集] 解析写入响应异常 boxCode={}", boxCode, e);
                        }
                    },
                    err -> log.error("[手工采集] 码关系写入请求失败 boxCode={}", boxCode, err)
            );

            OperateLogBuilder.create()
                    .module(ModuleNameEnum.MANUAL_COLLECT)
                    .operateType(OperateTypeEnum.COLLECT)
                    .target(code, code)
                    .content("手工采集盒码关联成功：盒码=" + code + "，已关联" + bpb + "瓶，累计完成" + totalBoxes + "盒")
                    .saveAsync();
            return;
        }
        refreshPrompt();
    }

    private String buildManualAssociateBody(String boxCode, List<String> bottleCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"boxCode\":\"").append(escapeJson(boxCode)).append("\",\"bottleCodes\":[");
        for (int i = 0; i < bottleCodes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(bottleCodes.get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
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
