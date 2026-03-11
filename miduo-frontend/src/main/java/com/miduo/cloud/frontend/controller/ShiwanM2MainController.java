package com.miduo.cloud.frontend.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.HttpUtil;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 石湾2号机（盒箱垛关联软件）主界面控制器
 * <p>
 * 对应 FXML：ShiwanM2MainWindow.fxml
 * 需求文档：P02-01-主界面-盒箱垛关联.md
 * </p>
 */
public class ShiwanM2MainController implements Initializable {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ==================== FXML 注入 ====================

    /** 主 TabPane */
    @FXML private TabPane mainTabPane;

    // --- 左侧面板 ---

    /** 产品选择下拉框 */
    @FXML private ComboBox<String> productComboBox;

    /** 产品编号行（选中产品后显示） */
    @FXML private HBox productCodeRow;

    /** 产品编号标签 */
    @FXML private Label productCodeLabel;

    /** 生产单号输入框 */
    @FXML private TextField orderNumField;

    /** 每垛箱数输入框（采集规格设置） */
    @FXML private TextField casesPerPalletField;

    /** 每箱盒数输入框（采集规格设置） */
    @FXML private TextField boxesPerCaseField;

    /** 实时上传数据区 ListView */
    @FXML private ListView<UploadItem> uploadDataList;

    // --- 中间面板 ---

    /** 数据接收区 ListView */
    @FXML private ListView<LogEntry> dataLogList;

    /** 操作日志 ListView */
    @FXML private ListView<LogEntry> opLogList;

    /** 报警信息 ListView */
    @FXML private ListView<LogEntry> alarmLogList;

    // --- 右侧面板 - 统计 ---

    /** 当前箱数（红色大字） */
    @FXML private Label curCasesLabel;

    /** 每垛箱数显示（蓝色大字） */
    @FXML private Label casesPerPalletDisplayLabel;

    /** 当前盒数 */
    @FXML private Label curBoxesLabel;

    /** 每箱盒数显示 */
    @FXML private Label boxesPerCaseDisplayLabel;

    /** 已生产垛数 */
    @FXML private Label palletCountLabel;

    /** 总剔除数（红色大字） */
    @FXML private Label rejectCountLabel;

    // --- 右侧面板 - 任务控制 ---

    /** 开始/停止采集按钮 */
    @FXML private Button startCaptureBtn;

    // --- 状态栏 ---

    /** 激活状态标签 */
    @FXML private Label activationStatusLabel;

    /** 采集运行状态标签 */
    @FXML private Label runningStatusLabel;

    /** 当前时间标签 */
    @FXML private Label currentTimeLabel;

    // ==================== 内部状态 ====================

    private static final int MAX_LOG_ENTRIES       = 1000;
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private final ObservableList<LogEntry>  dataLogItems  = FXCollections.observableArrayList();
    private final ObservableList<LogEntry>  opLogItems    = FXCollections.observableArrayList();
    private final ObservableList<LogEntry>  alarmLogItems = FXCollections.observableArrayList();
    private final ObservableList<UploadItem> uploadItems  = FXCollections.observableArrayList();

    private boolean isRunning   = false;
    private int currentCases    = 0;
    private int currentBoxes    = 0;
    private int palletCount     = 0;
    private int totalRejectCount = 0;

    private Timeline clockTimeline;
    private Timeline statsRefreshTimeline;

    // ==================== 初始化 ====================

    /** 页面 id 与 Tab 索引对应（与 FXML 中 8 个 Tab 顺序一致） */
    private static final String[] PAGE_IDS = {
        "dataCollection", "manual", "query", "replace", "stats", "package", "cancel", "upload"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupProductComboBox();
        setupListViews();
        setupSpecListeners();
        setupClock();
        setupInitialLogs();
        initActivationStatus();
        applyPageConfig();
        Platform.runLater(() -> checkUnfinishedOnStartup());
        loadSpecFromSettings();
    }

    /** 根据系统设置中的页面配置，调整主界面 Tab 的显示与顺序（重启后生效） */
    private void applyPageConfig() {
        if (mainTabPane == null) return;
        var settings = ShiwanM2SettingsStore.get();
        List<String> order = settings.getPageTabOrder() != null ? settings.getPageTabOrder() : ShiwanM2Settings.defaultPageTabOrder();
        var visible = settings.getPageVisible() != null ? settings.getPageVisible() : ShiwanM2Settings.defaultPageVisible();
        var tabs = mainTabPane.getTabs();
        if (tabs.size() < 8) return;
        List<Tab> newOrder = new ArrayList<>();
        for (String id : order) {
            if (!Boolean.TRUE.equals(visible.get(id))) continue;
            int idx = indexOfPageId(id);
            if (idx >= 0 && idx < tabs.size()) newOrder.add(tabs.get(idx));
        }
        if (newOrder.isEmpty()) return;
        tabs.clear();
        tabs.addAll(newOrder);
    }

    private int indexOfPageId(String id) {
        for (int i = 0; i < PAGE_IDS.length; i++) {
            if (PAGE_IDS[i].equals(id)) return i;
        }
        return -1;
    }

    /** 初始化产品下拉框示例数据 */
    private void setupProductComboBox() {
        productComboBox.getItems().addAll(
                "石湾酒 52度 500ml",
                "石湾酒 38度 500ml",
                "石湾酒 42度 250ml",
                "石湾酒 52度 250ml"
        );
    }

    /** 配置所有 ListView 的 CellFactory */
    private void setupListViews() {
        dataLogList.setItems(dataLogItems);
        dataLogList.setCellFactory(lv -> new LogCell());

        opLogList.setItems(opLogItems);
        opLogList.setCellFactory(lv -> new LogCell());

        alarmLogList.setItems(alarmLogItems);
        alarmLogList.setCellFactory(lv -> new LogCell());

        uploadDataList.setItems(uploadItems);
        uploadDataList.setCellFactory(lv -> new UploadItemCell());
    }

    /** 监听采集规格输入框变化，同步到统计卡片 */
    private void setupSpecListeners() {
        casesPerPalletField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                casesPerPalletDisplayLabel.setText(val.trim());
            }
        });
        boxesPerCaseField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                boxesPerCaseDisplayLabel.setText(val.trim());
            }
        });
    }

    /** 启动实时时钟 */
    private void setupClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            currentTimeLabel.setText(LocalDateTime.now().format(DATETIME_FMT));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
        currentTimeLabel.setText(LocalDateTime.now().format(DATETIME_FMT));
    }

    /** 写入初始欢迎日志 */
    private void setupInitialLogs() {
        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  系统启动完成", LogType.INFO);
        addOpLog(now + "  等待设备连接...", LogType.INFO);
    }

    /** 初始化激活状态显示 */
    private void initActivationStatus() {
        activationStatusLabel.setText("● 已激活");
    }

    // ==================== 菜单事件处理 ====================

    @FXML
    private void onExit() {
        if (currentCases > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("退出确认");
            confirm.setHeaderText("存在未满垛数据（" + currentCases + "箱），是否退出？");
            confirm.setContentText("暂存后可通过\"提取工单未成垛\"继续生产。");
            confirm.getButtonTypes().setAll(
                    new ButtonType("取消"),
                    new ButtonType("强制满垛"),
                    new ButtonType("暂存退出")
            );
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get().getText().equals("取消")) return;
            String orderNo = orderNumField.getText() != null ? orderNumField.getText().trim() : "";
            if (result.get().getText().equals("强制满垛")) {
                forceFullPalletBackend(orderNo, currentCases);
            } else if (result.get().getText().equals("暂存退出")) {
                markUnfinished(orderNo);
            }
        }
        if (clockTimeline != null) clockTimeline.stop();
        Platform.exit();
    }

    @FXML
    private void onSystemConfig() {
        // 密码验证：每次打开均需重新输入
        TextInputDialog pwdDialog = new TextInputDialog();
        pwdDialog.setTitle("系统密码验证");
        pwdDialog.setHeaderText("请输入系统密码以访问系统设置");
        pwdDialog.setContentText("密码：");
        pwdDialog.getEditor().setPromptText("请输入密码");

        // 将密码框设置为隐藏输入
        javafx.scene.control.PasswordField pwdField = new javafx.scene.control.PasswordField();
        pwdField.setPromptText("请输入密码");
        pwdDialog.getDialogPane().setContent(pwdField);
        pwdDialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
                .bind(pwdField.textProperty().isEmpty());

        // 点击确认时取密码框值
        pwdDialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) return pwdField.getText();
            return null;
        });

        Optional<String> result = pwdDialog.showAndWait();
        if (result.isEmpty()) return;

        String expectedPwd = ShiwanM2SettingsStore.get().getSystemSettingsPassword();
        if (expectedPwd == null) expectedPwd = "123456";
        if (!expectedPwd.equals(result.get())) {
            Alert errAlert = new Alert(Alert.AlertType.ERROR);
            errAlert.setTitle("密码错误");
            errAlert.setHeaderText(null);
            errAlert.setContentText("密码错误，请重新输入。");
            errAlert.showAndWait();
            return;
        }

        // 密码验证通过，打开系统设置弹窗
        openSystemSettingsDialog();
    }

    /** 打开系统设置弹窗 */
    private void openSystemSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ShiwanM2SystemSettingsDialog.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("系统设置（2号机）");
            settingsStage.setScene(new Scene(root));
            settingsStage.initModality(Modality.WINDOW_MODAL);
            settingsStage.initOwner(currentTimeLabel.getScene().getWindow());
            settingsStage.setResizable(true);
            settingsStage.setMinWidth(760);
            settingsStage.setMinHeight(500);
            settingsStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("系统设置", "无法打开系统设置界面，请联系技术人员。\n错误信息：" + e.getMessage());
        }
    }

    @FXML
    private void onLicenseInfo() {
        showInfo("许可证信息", "当前许可证状态：已激活\n到期日期：长期有效");
    }

    @FXML
    private void onCodePackage() {
        switchToTab(5);
    }

    @FXML
    private void onDataQuery() {
        switchToTab(2);
    }

    @FXML
    private void onDataReplace() {
        switchToTab(3);
    }

    @FXML
    private void onCancelAssociation() {
        switchToTab(6);
    }

    @FXML
    private void onProductionStats() {
        switchToTab(4);
    }

    @FXML
    private void onDataUpload() {
        switchToTab(7);
    }

    @FXML
    private void onOperationLog() {
        showInfo("操作日志", "操作日志功能开发中...");
    }

    @FXML
    private void onHelp() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ShiwanM2HelpDialog.fxml"));
            Parent root = loader.load();

            Stage helpStage = new Stage();
            helpStage.setTitle("操作帮助");
            helpStage.setScene(new Scene(root));
            helpStage.initModality(Modality.WINDOW_MODAL);
            helpStage.initOwner(currentTimeLabel.getScene().getWindow());
            helpStage.setResizable(true);
            helpStage.setMinWidth(700);
            helpStage.setMinHeight(500);
            helpStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("操作帮助", "无法打开操作帮助界面，请联系技术人员。\n错误信息：" + e.getMessage());
        }
    }

    @FXML
    private void onAbout() {
        showInfo("关于系统", "盒箱垛关联采集系统 v1.2\n石湾产线 2号机\n版权所有 © 米多科技");
    }

    // ==================== 产品选择 ====================

    @FXML
    private void onProductSelect() {
        String selected = productComboBox.getValue();
        if (selected != null && !selected.isEmpty()) {
            productCodeRow.setVisible(true);
            productCodeRow.setManaged(true);
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  产品已选择：" + selected, LogType.INFO);
        }
    }

    @FXML
    private void onOpenProductSelectDialog() {
        try (InputStream is = getClass().getResourceAsStream("/fxml/ShiwanM2ProductSelectDialog.fxml")) {
            if (is == null) return;
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(is);
            ShiwanM2ProductSelectDialogController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("选择产品");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            java.util.Map<String, String> selected = ctrl.getSelectedProduct();
            if (selected != null) {
                String name = selected.get("name");
                String pronumber = selected.get("pronumber");
                if (name != null) productComboBox.setValue(name);
                if (pronumber != null) productCodeLabel.setText(pronumber);
                productCodeRow.setVisible(true);
                productCodeRow.setManaged(true);
                addOpLog(LocalDateTime.now().format(TIME_FMT) + "  产品已选择：" + name + "（" + pronumber + "）", LogType.INFO);
            }
        } catch (IOException e) {
            showWarn("打开产品选择失败", e.getMessage());
        }
    }

    /** 启动时检查是否存在未成垛数据，若有则弹窗提示 */
    private void checkUnfinishedOnStartup() {
        try {
            String json = HttpUtil.doGet("/api/shiwan-m2/current-task/unfinished");
            JsonNode root = JSON.readTree(json);
            if (root == null || root.get("code").asInt() != 200 || !root.has("data")) return;
            JsonNode list = root.get("data");
            if (list == null || !list.isArray() || list.size() == 0) return;
            JsonNode first = list.get(0);
            String orderNo = first.has("OrderNo") ? first.get("OrderNo").asText() : (first.has("orderNo") ? first.get("orderNo").asText() : "");
            long currentCaseCount = first.has("currentCaseCount") ? first.get("currentCaseCount").asLong() : (first.has("currentcasecount") ? first.get("currentcasecount").asLong() : 0);
            String productNo = first.has("productNo") ? first.get("productNo").asText() : (first.has("ProductNo") ? first.get("ProductNo").asText() : "");
            String productName = first.has("productName") ? first.get("productName").asText() : (first.has("ProductName") ? first.get("ProductName").asText() : "");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("未成垛数据恢复");
            alert.setHeaderText("检测到上次退出软件时有未满垛数据，默认继续使用该垛数据继续采集。");
            alert.setContentText("订单号：" + orderNo + "\n未成垛箱数：" + currentCaseCount + " 箱");
            alert.showAndWait();
            if (orderNo != null && !orderNo.isEmpty()) {
                orderNumField.setText(orderNo);
                currentCases = (int) currentCaseCount;
                currentBoxes = 0;
                curCasesLabel.setText(String.valueOf(currentCases));
                curBoxesLabel.setText("0");
                if (productNo != null && !productNo.isEmpty()) {
                    productCodeLabel.setText(productNo);
                    productCodeRow.setVisible(true);
                    productCodeRow.setManaged(true);
                }
                if (productName != null && !productName.isEmpty()) {
                    if (!productComboBox.getItems().contains(productName)) {
                        productComboBox.getItems().add(productName);
                    }
                    productComboBox.setValue(productName);
                }
                refreshProducedPalletCount(orderNo);
                refreshRejectCount(orderNo);
                refreshUploadList(orderNo);
                addOpLog(LocalDateTime.now().format(TIME_FMT) + "  已恢复未成垛数据，订单：" + orderNo + "，当前箱数：" + currentCaseCount, LogType.INFO);
            }
        } catch (Exception ignored) {}
    }

    /** 从本地配置加载采集规格并回显 */
    private void loadSpecFromSettings() {
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s != null) {
            if (s.getBoxesPerPallet() != null && s.getBoxesPerPallet() > 0) {
                casesPerPalletField.setText(String.valueOf(s.getBoxesPerPallet()));
                casesPerPalletDisplayLabel.setText(String.valueOf(s.getBoxesPerPallet()));
            }
            if (s.getBoxesPerCase() != null && s.getBoxesPerCase() > 0) {
                boxesPerCaseField.setText(String.valueOf(s.getBoxesPerCase()));
                boxesPerCaseDisplayLabel.setText(String.valueOf(s.getBoxesPerCase()));
            }
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
        String product = productComboBox.getValue();
        if (product == null || product.isEmpty()) {
            showWarn("请先选择产品", "开始采集前需要选择生产产品。");
            return;
        }
        String orderNo = orderNumField.getText() != null ? orderNumField.getText().trim() : "";
        if (orderNo.isEmpty()) {
            showWarn("请输入生产单号", "开始采集前需要填写生产单号。");
            return;
        }
        String productNo = productCodeLabel.getText() != null ? productCodeLabel.getText().trim() : "";
        if (productNo.isEmpty()) {
            showWarn("产品编号为空", "请通过「选择」按钮重新选择产品以带出产品编号。");
            return;
        }

        int m = 70;
        int n = 4;
        try {
            String ms = casesPerPalletField.getText();
            String ns = boxesPerCaseField.getText();
            if (ms != null && !ms.trim().isEmpty()) m = Integer.parseInt(ms.trim());
            if (ns != null && !ns.trim().isEmpty()) n = Integer.parseInt(ns.trim());
        } catch (NumberFormatException ignored) {}

        // 采集规格持久化变更提示
        ShiwanM2Settings settings = ShiwanM2SettingsStore.get();
        int savedM = settings != null && settings.getBoxesPerPallet() != null ? settings.getBoxesPerPallet() : m;
        int savedN = settings != null && settings.getBoxesPerCase() != null ? settings.getBoxesPerCase() : n;
        if (m != savedM || n != savedN) {
            Alert specAlert = new Alert(Alert.AlertType.CONFIRMATION);
            specAlert.setTitle("采集规格变更确认");
            specAlert.setHeaderText("检测到已保存的采集规格与当前输入不一致");
            specAlert.setContentText("已保存：1垛 " + savedM + " 箱 / 1箱 " + savedN + " 盒\n当前：1垛 " + m + " 箱 / 1箱 " + n + " 盒\n是否覆盖保存为当前规格？");
            Optional<ButtonType> specResult = specAlert.showAndWait();
            if (specResult.isEmpty() || specResult.get() != ButtonType.OK) {
                // 使用已保存规格继续
                m = savedM;
                n = savedN;
                casesPerPalletField.setText(String.valueOf(m));
                boxesPerCaseField.setText(String.valueOf(n));
            } else {
                persistSpec(m, n);
            }
        }

        // 门禁1：码包检查
        try {
            String codePackageJson = HttpUtil.doGet("/api/shiwan-m2/code-package/check");
            JsonNode cp = JSON.readTree(codePackageJson);
            if (cp != null && cp.has("code") && cp.get("code").asInt() != 200) {
                showWarn("码包门禁未通过", cp.has("message") ? cp.get("message").asText() : "请先导入小标、中标、大标码包。");
                return;
            }
            if (cp != null && cp.has("data") && cp.get("data").has("passed") && !cp.get("data").get("passed").asBoolean()) {
                showWarn("码包门禁未通过", cp.has("message") ? cp.get("message").asText() : "请先导入小标、中标、大标码包。");
                return;
            }
        } catch (Exception e) {
            showWarn("码包门禁检查失败", e.getMessage());
            return;
        }

        // 门禁2：1号机连接检查
        try {
            String m1Json = HttpUtil.doGet("/api/shiwan-m2/settings/check-m1-connection");
            JsonNode m1 = JSON.readTree(m1Json);
            if (m1 == null || !m1.has("code") || m1.get("code").asInt() != 200) {
                showWarn("1号机连接未通过", m1 != null && m1.has("message") ? m1.get("message").asText() : "请在系统设置中配置并确保1号机 SQL Server 可连接。");
                return;
            }
        } catch (Exception e) {
            showWarn("1号机连接检查失败", e.getMessage());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("开始采集确认");
        confirm.setHeaderText("确认开始生产？");
        confirm.setContentText(
                "产品：" + product + "\n" +
                "生产单号：" + orderNo + "\n" +
                "1垛 " + m + " 箱  |  1箱 " + n + " 盒"
        );
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // 调用后端写当前任务并开始采集
        try {
            String body = "{\"orderNo\":\"" + escapeJson(orderNo) + "\",\"productNo\":\"" + escapeJson(productNo) + "\",\"productName\":\"" + escapeJson(product) + "\",\"boxesPerPallet\":" + m + ",\"boxesPerCase\":" + n + "}";
            String startJson = HttpUtil.doPost("/api/shiwan-m2/current-task/start", body);
            JsonNode start = JSON.readTree(startJson);
            if (start == null || !start.has("code") || start.get("code").asInt() != 200) {
                showWarn("开始采集失败", start != null && start.has("message") ? start.get("message").asText() : "后端返回异常");
                return;
            }
        } catch (Exception e) {
            showWarn("开始采集失败", e.getMessage());
            return;
        }

        persistSpec(m, n);
        isRunning = true;
        startCaptureBtn.setText("停止采集");
        startCaptureBtn.getStyleClass().add("running");
        casesPerPalletField.setEditable(false);
        boxesPerCaseField.setEditable(false);
        productComboBox.setDisable(true);
        orderNumField.setEditable(false);
        startStatsRefresh(orderNo);

        runningStatusLabel.setVisible(true);
        runningStatusLabel.setManaged(true);
        runningStatusLabel.setText("采集中");

        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  开始采集 - 产品：" + product + "，生产单：" + orderNo, LogType.INFO);
        addDataLog(now + "  设备初始化完成，开始接收数据", LogType.SUCCESS);
        refreshProducedPalletCount(orderNo);
        refreshRejectCount(orderNo);
        refreshUploadList(orderNo);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void persistSpec(int m, int n) {
        try {
            ShiwanM2Settings settings = ShiwanM2SettingsStore.get();
            if (settings == null) settings = new ShiwanM2Settings();
            settings.setBoxesPerPallet(m);
            settings.setBoxesPerCase(n);
            ShiwanM2SettingsStore.save(settings);
        } catch (IOException e) {
            showWarn("保存采集规格失败", e.getMessage());
        }
    }

    private void stopCapture() {
        isRunning = false;
        startCaptureBtn.setText("开始采集");
        startCaptureBtn.getStyleClass().remove("running");
        casesPerPalletField.setEditable(true);
        boxesPerCaseField.setEditable(true);
        productComboBox.setDisable(false);
        orderNumField.setEditable(true);

        runningStatusLabel.setVisible(false);
        runningStatusLabel.setManaged(false);

        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  停止采集，未满垛数据已保留（" + currentCases + "箱）", LogType.INFO);
        stopStatsRefresh();
    }

    @FXML
    private void onNoCodeNeeded() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("无需采集码");
        confirm.setHeaderText("确认开启\"无需采集码\"模式？");
        confirm.setContentText("开启后，所有读码设备将停止工作，不采集任何数据。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  无需采集码模式已开启", LogType.INFO);
        }
    }

    @FXML
    private void onCloseAlarm() {
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  报警已关闭（声光报警器已重置）", LogType.INFO);
    }

    @FXML
    private void onForcePallet() {
        if (currentCases == 0) {
            showWarn("强制满垛", "当前没有待处理的箱数，无需强制满垛。");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("强制满垛确认");
        confirm.setHeaderText("确认强制满垛？");
        confirm.setContentText("当前箱数：" + currentCases + "箱\n强制满垛将生成虚拟垛标并重置计数。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            forceFullPalletBackend(orderNumField.getText(), currentCases);
        }
    }

    private void forceFullPalletBackend(String orderNo, int currentCaseCount) {
        try {
            String body = "{\"orderNo\":\"" + escapeJson(orderNo) + "\",\"currentCaseCount\":" + currentCaseCount + "}";
            String json = HttpUtil.doPost("/api/shiwan-m2/box-case/force-full-pallet", body);
            JsonNode root = JSON.readTree(json);
            if (root == null || !root.has("code") || root.get("code").asInt() != 200) {
                showWarn("强制满垛失败", root != null && root.has("message") ? root.get("message").asText() : "后端返回异常");
                return;
            }
            JsonNode data = root.get("data");
            String palletCode = data != null && data.has("palletCode") ? data.get("palletCode").asText() : fetchNextVirtualSerialNumber();
            palletCount++;
            palletCountLabel.setText(String.valueOf(palletCount));

            String now = LocalDateTime.now().format(TIME_FMT);
            addDataLog(now + "  强制满垛 - 垛码：" + palletCode + " 已生成，共" + currentCaseCount + "箱", LogType.SUCCESS);
            if (data != null && data.has("uploadTriggered") && data.get("uploadTriggered").asBoolean(false)) {
                String uploadStatus = data.has("uploadStatus") ? data.get("uploadStatus").asText() : "PENDING";
                String uploadMsg = data.has("uploadMessage") ? data.get("uploadMessage").asText() : "";
                if ("DONE".equalsIgnoreCase(uploadStatus)) {
                    addDataLog(now + "  垛码 " + palletCode + " 自动上传成功", LogType.SUCCESS);
                } else {
                    addDataLog(now + "  垛码 " + palletCode + " 自动上传失败：" + uploadMsg, LogType.WARN);
                    addAlarmLog(now + "  上传失败：" + palletCode + "，原因：" + uploadMsg, LogType.ERROR);
                }
            }
            addOpLog(now + "  强制满垛操作完成，已生产垛数：" + palletCount, LogType.INFO);

            currentCases = 0;
            currentBoxes = 0;
            curCasesLabel.setText("0");
            curBoxesLabel.setText("0");
            refreshProducedPalletCount(orderNumField.getText());
            refreshUploadList(orderNumField.getText());
        } catch (Exception e) {
            showWarn("强制满垛失败", e.getMessage());
        }
    }

    private void markUnfinished(String orderNo) {
        try {
            String body = "{\"orderNo\":\"" + escapeJson(orderNo) + "\"}";
            String json = HttpUtil.doPost("/api/shiwan-m2/current-task/mark-unfinished", body);
            JsonNode root = JSON.readTree(json);
            if (root == null || !root.has("code") || root.get("code").asInt() != 200) {
                showWarn("暂存失败", root != null && root.has("message") ? root.get("message").asText() : "后端返回异常");
            }
            stopStatsRefresh();
        } catch (Exception e) {
            showWarn("暂存失败", e.getMessage());
        }
    }

    /** 刷新已生产垛数 */
    private void refreshProducedPalletCount(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) return;
        try {
            String json = HttpUtil.doGet("/api/shiwan-m2/stats/produced-pallet-count?orderNo=" + java.net.URLEncoder.encode(orderNo, "UTF-8"));
            JsonNode root = JSON.readTree(json);
            if (root != null && root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                int count = root.get("data").asInt();
                palletCount = count;
                palletCountLabel.setText(String.valueOf(count));
            }
        } catch (Exception ignored) {}
    }

    /** 刷新总剔除数 */
    private void refreshRejectCount(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) return;
        try {
            String json = HttpUtil.doGet("/api/shiwan-m2/stats/reject-count?orderNo=" + java.net.URLEncoder.encode(orderNo, "UTF-8"));
            JsonNode root = JSON.readTree(json);
            if (root != null && root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                int count = root.get("data").asInt();
                totalRejectCount = count;
                rejectCountLabel.setText(String.valueOf(count));
            }
        } catch (Exception ignored) {}
    }

    /** 开始定时刷新统计（垛数/剔除数） */
    private void startStatsRefresh(String orderNo) {
        stopStatsRefresh();
        if (orderNo == null || orderNo.isEmpty()) return;
        statsRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            refreshProducedPalletCount(orderNo);
            refreshRejectCount(orderNo);
            refreshUploadList(orderNo);
        }));
        statsRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        statsRefreshTimeline.play();
    }

    /** 停止定时刷新统计 */
    private void stopStatsRefresh() {
        if (statsRefreshTimeline != null) {
            statsRefreshTimeline.stop();
            statsRefreshTimeline = null;
        }
    }

    /** 刷新实时上传数据区（后端聚合列表）。 */
    private void refreshUploadList(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) return;
        try {
            String json = HttpUtil.doGet("/api/shiwan-m2/upload-list?orderNo=" + java.net.URLEncoder.encode(orderNo, "UTF-8"));
            JsonNode root = JSON.readTree(json);
            if (root == null || !root.has("code") || root.get("code").asInt() != 200 || !root.has("data")) return;
            JsonNode list = root.get("data");
            if (!list.isArray()) return;
            List<UploadItem> refreshed = new ArrayList<>();
            for (JsonNode item : list) {
                String palletCode = item.has("palletCode") ? item.get("palletCode").asText() : "";
                String boxCount = item.has("boxCount") ? item.get("boxCount").asText() : "0";
                String status = item.has("status") ? item.get("status").asText() : "PENDING";
                UploadStatus st;
                if ("DONE".equalsIgnoreCase(status)) {
                    st = UploadStatus.DONE;
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    st = UploadStatus.FAILED;
                } else {
                    st = UploadStatus.PENDING;
                }
                refreshed.add(new UploadItem("垛 " + palletCode, boxCount + "箱", st));
            }
            uploadItems.setAll(refreshed);
        } catch (Exception ignored) {}
    }

    @FXML
    private void onExtractUnfinished() {
        showInfo("提取工单未成垛", "请输入或扫描垛内任意一箱的箱码，\n系统将查出对应生产订单并显示，\n确认后可回显到主页面继续生产。\n\n（功能开发中，请联系技术人员）");
    }

    @FXML
    private void onRetractReject() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("收回剔除");
        confirm.setHeaderText("确认向PLC发送收回指令？");
        confirm.setContentText("将向剔除设备发送收回动作指令。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  已发送收回指令到PLC", LogType.INFO);
        }
    }

    @FXML
    private void onResetRejectCount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("剔除数清零");
        confirm.setHeaderText("确认将总剔除数重置为0？");
        confirm.setContentText("此操作仅重置界面计数，不影响数据库记录。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            totalRejectCount = 0;
            rejectCountLabel.setText("0");
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  总剔除数已清零", LogType.INFO);
        }
    }

    @FXML
    private void onTaskHelp() {
        showInfo("任务控制 - 功能说明",
                "● 开始/停止采集：系统检测硬件后开始采集；再次点击停止，未满垛数据保留。\n" +
                "● 无需采集码：当天不生产五码合一产品时开启，所有读码设备不工作。\n" +
                "● 关闭报警：重置报警状态，停止声光报警器/蜂鸣器等。\n" +
                "● 强制满垛：未达到设定箱数也强制结束当前垛，生成虚拟垛标并重置计数。\n" +
                "● 提取工单未成垛：弹窗输入/扫描垛内任一箱码，查出对应生产订单继续生产。\n" +
                "● 收回剔除：剔除设备未自动收回时，点击收回当前剔除动作（PLC控制）。\n" +
                "● 剔除数清零：将总剔除数重置为0。");
    }

    // ==================== 外部调用接口（业务服务调用）====================

    /**
     * 盒箱关联成功回调
     * @param boxCode 盒码
     * @param caseCode 箱码
     */
    public void onBoxCaseAssociationSuccess(String boxCode, String caseCode) {
        Platform.runLater(() -> {
            currentBoxes++;
            curBoxesLabel.setText(String.valueOf(currentBoxes));

            int bpc = parseSpecValue(boxesPerCaseField.getText(), 4);
            if (currentBoxes >= bpc) {
                currentCases++;
                currentBoxes = 0;
                curCasesLabel.setText(String.valueOf(currentCases));
                curBoxesLabel.setText("0");

                String now = LocalDateTime.now().format(TIME_FMT);
                addDataLog(now + "  装箱完成 - 箱码：" + caseCode + " 已关联" + bpc + "盒", LogType.SUCCESS);

                int cpp = parseSpecValue(casesPerPalletField.getText(), 70);
                if (currentCases >= cpp) {
                    onPalletFull();
                }
            }
            String now = LocalDateTime.now().format(TIME_FMT);
            addDataLog(now + "  盒箱关联 - 盒码：" + boxCode + " → 箱码：" + caseCode, LogType.DATA);
        });
    }

    /**
     * 扫描错误/报警回调
     * @param message 错误描述
     */
    public void onScanError(String message) {
        Platform.runLater(() -> {
            totalRejectCount++;
            rejectCountLabel.setText(String.valueOf(totalRejectCount));
            String now = LocalDateTime.now().format(TIME_FMT);
            addAlarmLog(now + "  " + message, LogType.ERROR);
        });
    }

    /** 满垛处理（箱数达到每垛箱数时调用，使用 VirtualPalletSequence 取号生成虚拟垛标） */
    private void onPalletFull() {
        palletCount++;
        palletCountLabel.setText(String.valueOf(palletCount));

        String palletCode = fetchNextVirtualSerialNumber();
        if (palletCode == null) {
            palletCode = "P" + orderNumField.getText() + String.format("%03d", palletCount);
        }
        int cpp = parseSpecValue(casesPerPalletField.getText(), 70);

        String now = LocalDateTime.now().format(TIME_FMT);
        addDataLog(now + "  满垛提示 - 垛码：" + palletCode + " 已生成，共" + cpp + "箱，正在上传...", LogType.SUCCESS);
        addOpLog(now + "  满垛完成，已生产垛数：" + palletCount, LogType.INFO);

        uploadItems.add(0, new UploadItem("垛 " + palletCode, cpp + "箱", UploadStatus.PENDING));

        currentCases = 0;
        currentBoxes = 0;
        curCasesLabel.setText("0");
        curBoxesLabel.setText("0");
        refreshUploadList(orderNumField.getText());
    }

    /**
     * 从后端获取下一个虚拟垛标（VirtualPalletSequence + 系统设置前缀/产线号）。
     * 失败时返回 null，调用方可用本地 fallback。
     */
    private String fetchNextVirtualSerialNumber() {
        try {
            String response = HttpUtil.doGet("/api/shiwan-m2/pallet/next-virtual-serial-number");
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
            if (node.has("code") && node.get("code").asInt(500) == 200 && node.has("data") && !node.get("data").isNull()) {
                return node.get("data").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ==================== 日志管理 ====================

    /**
     * 向数据接收区添加日志条目
     */
    public void addDataLog(String message, LogType type) {
        Platform.runLater(() -> {
            trimList(dataLogItems);
            dataLogItems.add(0, new LogEntry(message, type));
        });
    }

    /**
     * 向操作日志添加条目
     */
    public void addOpLog(String message, LogType type) {
        Platform.runLater(() -> {
            trimList(opLogItems);
            opLogItems.add(0, new LogEntry(message, type));
        });
    }

    /**
     * 向报警信息添加条目
     */
    public void addAlarmLog(String message, LogType type) {
        Platform.runLater(() -> {
            trimList(alarmLogItems);
            alarmLogItems.add(0, new LogEntry(message, type));
        });
    }

    private void trimList(ObservableList<LogEntry> list) {
        while (list.size() >= MAX_LOG_ENTRIES) {
            list.remove(list.size() - 1);
        }
    }

    // ==================== 工具方法 ====================

    private void switchToTab(int index) {
        if (mainTabPane != null && index < mainTabPane.getTabs().size()) {
            mainTabPane.getSelectionModel().select(index);
        }
    }

    private int parseSpecValue(String text, int defaultValue) {
        try {
            int v = Integer.parseInt(text.trim());
            return v > 0 ? v : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarn(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==================== 枚举与数据模型 ====================

    /** 日志类型 */
    public enum LogType {
        DATA,    // 普通数据（黑字灰边）
        SUCCESS, // 成功（绿字绿边）
        WARN,    // 警告（黄字黄边）
        ERROR,   // 错误/报警（红字红边）
        INFO     // 操作日志（灰字灰边）
    }

    /** 日志条目数据模型 */
    public static class LogEntry {
        public final String  text;
        public final LogType type;

        public LogEntry(String text, LogType type) {
            this.text = text;
            this.type = type;
        }
    }

    /** 上传状态枚举 */
    public enum UploadStatus {
        PENDING, // 待上传
        DONE,    // 已上传
        FAILED   // 上传失败
    }

    /** 上传条目数据模型 */
    public static class UploadItem {
        public final String       palletCode;
        public final String       boxCount;
        public       UploadStatus status;

        public UploadItem(String palletCode, String boxCount, UploadStatus status) {
            this.palletCode = palletCode;
            this.boxCount   = boxCount;
            this.status     = status;
        }
    }

    // ==================== 自定义 ListCell ====================

    /**
     * 日志区 ListCell：根据 LogType 渲染不同样式的带左边框标签
     */
    static class LogCell extends ListCell<LogEntry> {

        private final Label label = new Label();

        LogCell() {
            label.setMaxWidth(Double.MAX_VALUE);
            label.setWrapText(true);
            label.getStyleClass().add("log-cell-label");
            HBox.setHgrow(label, Priority.ALWAYS);
        }

        @Override
        protected void updateItem(LogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            label.getStyleClass().removeAll(
                    "log-cell-data", "log-cell-success",
                    "log-cell-warn", "log-cell-error", "log-cell-info"
            );

            switch (item.type) {
                case SUCCESS:
                    label.getStyleClass().add("log-cell-success");
                    break;
                case WARN:
                    label.getStyleClass().add("log-cell-warn");
                    break;
                case ERROR:
                    label.getStyleClass().add("log-cell-error");
                    break;
                case INFO:
                    label.getStyleClass().add("log-cell-info");
                    break;
                default:
                    label.getStyleClass().add("log-cell-data");
                    break;
            }

            label.setText(item.text);
            setGraphic(label);
            setText(null);
        }
    }

    /**
     * 实时上传数据区 ListCell：显示垛码、状态徽标、箱数
     */
    static class UploadItemCell extends ListCell<UploadItem> {

        private final VBox  root       = new VBox(4);
        private final HBox  topRow     = new HBox();
        private final Label palletLbl  = new Label();
        private final Label statusBadge = new Label();
        private final Label boxCountLbl = new Label();

        UploadItemCell() {
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.setSpacing(8);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            topRow.getChildren().addAll(palletLbl, spacer, statusBadge);

            palletLbl.getStyleClass().add("shiwan-m2-upload-pallet-code");
            boxCountLbl.getStyleClass().add("shiwan-m2-upload-box-count");

            root.getChildren().addAll(topRow, boxCountLbl);
            root.getStyleClass().add("shiwan-m2-upload-item");
            root.setPadding(new Insets(10, 12, 10, 12));
            root.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(UploadItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            palletLbl.setText(item.palletCode);
            boxCountLbl.setText(item.boxCount);

            statusBadge.getStyleClass().removeAll(
                    "shiwan-m2-badge-pending", "shiwan-m2-badge-done"
            );
            switch (item.status) {
                case DONE:
                    statusBadge.setText("已上传");
                    statusBadge.getStyleClass().add("shiwan-m2-badge-done");
                    break;
                case FAILED:
                    statusBadge.setText("上传失败");
                    statusBadge.getStyleClass().add("shiwan-m2-badge-pending");
                    break;
                default:
                    statusBadge.setText("待上传");
                    statusBadge.getStyleClass().add("shiwan-m2-badge-pending");
                    break;
            }

            setGraphic(root);
            setText(null);
        }
    }
}
