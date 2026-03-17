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
import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.application.shiwan.UploadLogBus;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.service.DeviceConnectionManager;
import com.miduo.cloud.frontend.service.ShiwanM2HardwareService;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
    @FXML private ComboBox<ProductItem> productComboBox;

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

    /** 许可证状态容器 */
    @FXML private HBox licenseStatusBox;
    /** 许可证状态圆点 */
    @FXML private Region licenseStatusDot;
    /** 许可证状态文本 */
    @FXML private Label licenseStatusLabel;

    /** 采集运行状态标签 */
    @FXML private Label runningStatusLabel;

    /** 当前时间标签 */
    @FXML private Label currentTimeLabel;

    // ==================== 内部状态 ====================

    private static final int MAX_LOG_ENTRIES       = 1000;
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private final ObservableList<LogEntry>  dataLogItems   = FXCollections.observableArrayList();
    private final ObservableList<LogEntry>  opLogItems     = FXCollections.observableArrayList();
    private final ObservableList<LogEntry>  alarmLogItems  = FXCollections.observableArrayList();
    private final ObservableList<UploadItem> uploadItems   = FXCollections.observableArrayList();
    private final ObservableList<ProductItem> productItems = FXCollections.observableArrayList();

    private final ExecutorService productExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "capture-executor");
        t.setDaemon(true);
        return t;
    });
    /** 为 true 时屏蔽编辑框文字变化触发的搜索（程序设值时用） */
    private volatile boolean suppressProductSearch = false;

    private boolean isRunning   = false;
    private int currentCases    = 0;
    private int currentBoxes    = 0;
    private int palletCount     = 0;
    private int totalRejectCount = 0;

    private Timeline clockTimeline;
    private Timeline statsRefreshTimeline;
    /** 采集事件轮询 Timeline（每 1 秒触发一次调度，HTTP 调用在后台线程执行） */
    private Timeline captureEventsTimeline;
    /** 上次已处理的事件 seq，用于增量拉取 */
    private long lastCaptureEventSeq = 0;
    /** 防止采集事件并发拉取 */
    private final AtomicBoolean processingCaptureEvents = new AtomicBoolean(false);

    // ==================== 初始化 ====================

    /** 页面 id 与 Tab 索引对应（与 FXML 中 8 个 Tab 顺序一致） */
    private static final String[] PAGE_IDS = {
        "dataCollection", "manual", "query", "replace", "stats", "package", "cancel", "upload"
    };

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2MainController.class);

    /** 扫码枪设备类别代码（与 DeviceConnectionManager.convertCategoryTextToCode 保持一致） */
    private static final int CATEGORY_SCANNER = 7;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupProductComboBox();
        setupListViews();
        setupSpecListeners();
        setupClock();
        setupInitialLogs();
        initActivationStatus();
        applyPageConfig();
        registerPalletEventListener();
        registerDeviceDataHandler();
        Platform.runLater(() -> {
            checkDbConnectionOnStartup();
            checkUnfinishedOnStartup();
        });
        loadSpecFromSettings();
    }

    /**
     * 注册设备数据分发处理器：
     * 所有设备收到的数据都写入数据接收区；
     * category=7（扫码枪）→ 额外转发给手工采集控制器 {@link ShiwanM2ManualController#onScanCode}。
     */
    private void registerDeviceDataHandler() {
        DeviceConnectionManager.getInstance().setDataReceiveHandlerWithOrder((categoryCode, data) -> {
            log.info("[设备数据分发] category={} data={}", categoryCode, data);

            // 查询设备名称，写入数据接收区
            IoDeviceDTO device = DeviceConnectionManager.getInstance().getDeviceByCategory(categoryCode);
            String deviceLabel = device != null ? device.getDeviceName() : ("类别" + categoryCode);
            String now = LocalDateTime.now().format(TIME_FMT);
            addDataLog(now + "  [" + deviceLabel + "] 收到: " + data, LogType.DATA);

            if (categoryCode == CATEGORY_SCANNER) {
                ShiwanM2ManualController manualCtrl = ShiwanM2ManualController.getInstance();
                if (manualCtrl != null) {
                    log.info("[设备数据分发] 扫码枪数据 → 手工采集控制器: {}", data);
                    manualCtrl.onScanCode(data);
                } else {
                    log.warn("[设备数据分发] 扫码枪数据到达但手工采集控制器未初始化: {}", data);
                }
            }
        });
    }

    /**
     * 注册 UploadLogBus 垛状态事件监听：
     * UPLOADING → 实时上传数据区新增条目；
     * SUCCESS/FAILED → 更新条目状态 + 数据接收区追加结果日志。
     */
    private void registerPalletEventListener() {
        UploadLogBus.registerPalletEventListener((palletCode, boxCount, status, errorMsg) ->
            Platform.runLater(() -> {
                String time = LocalDateTime.now().format(TIME_FMT);
                ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
                switch (status) {
                    case UPLOADING:
                        uploadItems.add(0, new UploadItem(palletCode, boxCount + "箱", UploadStatus.UPLOADING));
                        break;
                    case SUCCESS:
                        updateUploadItemStatus(palletCode, UploadStatus.DONE);
                        addDataLog(time + "  垛码上传成功：" + palletCode + " " + boxCount + "箱", LogType.SUCCESS);
                        // 文档：上传成功 → 绿灯常亮，1 分钟后自动熄灭
                        hw.greenLightOn();
                        break;
                    case FAILED:
                        updateUploadItemStatus(palletCode, UploadStatus.FAILED);
                        String errInfo = (errorMsg != null && !errorMsg.isEmpty()) ? "：" + errorMsg : "";
                        addDataLog(time + "  垛码上传失败：" + palletCode + " " + boxCount + "箱" + errInfo, LogType.ERROR);
                        addAlarmLog(time + "  上传失败：" + palletCode + (errInfo.isEmpty() ? "" : errInfo), LogType.ERROR);
                        // 文档：上传失败 → 红灯常亮 + 蜂鸣
                        hw.redLightAndBuzzer();
                        break;
                    default:
                        break;
                }
            })
        );
    }

    /** 按垛码查找 uploadItems 中对应条目并更新状态，更新后刷新列表 */
    private void updateUploadItemStatus(String palletCode, UploadStatus newStatus) {
        for (UploadItem item : uploadItems) {
            if (palletCode.equals(item.palletCode)) {
                item.status = newStatus;
                break;
            }
        }
        uploadDataList.refresh();
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

    /** 初始化产品下拉框：可输入搜索、滚动、高度增加 */
    private void setupProductComboBox() {
        productComboBox.setItems(productItems);
        productComboBox.setEditable(false);

        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductItem item) {
                return item != null ? item.getDisplay() : "";
            }

            @Override
            public ProductItem fromString(String string) {
                // 仅在下拉选中“有名字”的项或弹窗选择时改值；失焦时编辑框为空或未匹配则保持当前选中
                return null;
            }
        });

        // 拦截下拉展开，改为弹出产品选择弹窗
        productComboBox.setOnShowing(e -> {
            Platform.runLater(() -> {
                productComboBox.hide();
                onOpenProductSelectDialog();
            });
        });

        // 选中后同步产品编号
        productComboBox.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null && nv.getNo() != null) {
                productCodeLabel.setText(nv.getNo());
            }
        });

        // 启动时先同步一次再加载
        productExecutor.submit(() -> {
            syncProducts();
            fetchProducts("", 50);
        });
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

    /** 监听采集规格输入框变化，同步到统计卡片；限制仅数字输入 1-999 */
    private void setupSpecListeners() {
        applyNumericFilter(casesPerPalletField, 1, 999);
        applyNumericFilter(boxesPerCaseField, 1, 999);

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

    /** 为输入框添加数字过滤器，限制范围 min-max */
    private void applyNumericFilter(TextField field, int min, int max) {
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (!newText.matches("\\d{1,3}")) return null;
            int value = Integer.parseInt(newText);
            if (value < 0 || value > max) return null;
            return change;
        }));
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
        addOpLog(now + "  系统启动完成，正在初始化...", LogType.INFO);
    }

    /** 主界面打开后异步检测本机数据库连接，结果写入操作日志 */
    private void checkDbConnectionOnStartup() {
        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  正在检测本机数据库连接...", LogType.INFO);
        HttpUtil.asyncGet("/api/shiwan-m2/settings/check-db-connection", json -> {
            try {
                JsonNode root = JSON.readTree(json);
                String time = LocalDateTime.now().format(TIME_FMT);
                if (root != null && root.has("code") && root.get("code").asInt() == 200) {
                    String msg = root.has("message") ? root.get("message").asText() : "连接正常";
                    addOpLog(time + "  数据库连接成功：" + msg, LogType.SUCCESS);
                } else {
                    String msg = root != null && root.has("message") ? root.get("message").asText() : "未知错误";
                    addOpLog(time + "  数据库连接失败：" + msg, LogType.ERROR);
                }
            } catch (Exception e) {
                String time = LocalDateTime.now().format(TIME_FMT);
                addOpLog(time + "  数据库连接检测异常：" + e.getMessage(), LogType.ERROR);
            }
        }, e -> {
            String time = LocalDateTime.now().format(TIME_FMT);
            addOpLog(time + "  数据库连接检测失败：" + (e != null ? e.getMessage() : "网络错误"), LogType.ERROR);
        });
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
            showInfo("打开许可证信息失败", e.getMessage());
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
                doExit();
                return;
            } else if (result.get().getText().equals("暂存退出")) {
                markUnfinishedAndExit(orderNo);
                return;
            }
        }
        doExit();
    }

    private void doExit() {
        if (clockTimeline != null) clockTimeline.stop();
        if (statsRefreshTimeline != null) statsRefreshTimeline.stop();
        if (captureEventsTimeline != null) captureEventsTimeline.stop();
        DeviceConnectionManager.getInstance().stopAllConnections();
        productExecutor.shutdownNow();
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
        try {
            LicenseInfoController.showLicenseInfo();
        } catch (Exception e) {
            showInfo("打开许可证信息失败", e.getMessage());
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OperateLog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("操作日志");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(currentTimeLabel.getScene().getWindow());
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("操作日志", "无法打开操作日志界面，请联系技术人员。\n错误信息：" + e.getMessage());
        }
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
        showInfo("关于系统", "米多赋码采集关联系统 v1.2\n关联模式：盒箱垛关联\n部署站点：石湾产线\n版权所有 © 米多科技");
    }

    // ==================== 产品选择 ====================

    @FXML
    private void onProductSelect() {
        ProductItem selected = productComboBox.getValue();
        if (selected != null) {
            productCodeRow.setVisible(true);
            productCodeRow.setManaged(true);
            productCodeLabel.setText(selected.getNo());
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  产品已选择：" + selected.getDisplay(), LogType.INFO);
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
                if (name != null) {
                    ProductItem item = productItems.stream()
                            .filter(p -> name.equals(p.getName())
                                    || (pronumber != null && pronumber.equals(p.getNo())))
                            .findFirst()
                            .orElse(new ProductItem(name, pronumber, ""));
                    productComboBox.setValue(item);
                }
                if (pronumber != null) productCodeLabel.setText(pronumber);
                productCodeRow.setVisible(true);
                productCodeRow.setManaged(true);
                addOpLog(LocalDateTime.now().format(TIME_FMT) + "  产品已选择：" + name + "（" + pronumber + "）", LogType.INFO);
            }
        } catch (IOException e) {
            showWarn("打开产品选择失败", e.getMessage());
        }
    }

    /** 启动时检查是否存在未成垛数据，若有则弹窗提示（异步 HTTP） */
    private void checkUnfinishedOnStartup() {
        HttpUtil.asyncGet("/api/shiwan-m2/current-task/unfinished", json -> {
            try {
                JsonNode root = JSON.readTree(json);
                if (root == null || root.get("code").asInt() != 200 || !root.has("data")) return;
                JsonNode list = root.get("data");
                if (list == null || !list.isArray() || list.size() == 0) return;
                JsonNode first = list.get(0);
                String orderNo = first.has("OrderNo") ? first.get("OrderNo").asText()
                        : (first.has("orderNo") ? first.get("orderNo").asText() : "");
                long currentCaseCount = first.has("currentCaseCount") ? first.get("currentCaseCount").asLong()
                        : (first.has("currentcasecount") ? first.get("currentcasecount").asLong() : 0);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("未成垛数据恢复");
                alert.setHeaderText("检测到上次退出软件时有未满垛数据，默认继续使用该垛数据继续采集。");
                alert.setContentText("订单号：" + orderNo + "\n未成垛箱数：" + currentCaseCount + " 箱");
                alert.showAndWait();
            } catch (Exception ignored) {}
        }, null);
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

    /**
     * 开始采集入口（FX 线程）。
     * 快速 UI 校验和规格弹窗在 FX 线程完成；所有 I/O（HTTP 检查、任务启动）
     * 提交到 productExecutor 后台线程，避免阻塞 FX/UI 线程。
     */
    private void startCapture() {
        // === 快速 FX 线程校验（无 I/O）===
        ProductItem productItem = productComboBox.getValue();
        String product = null;
        if (productItem != null) {
            String name = productItem.getName();
            String no   = productItem.getNo();
            product = (name != null && !name.isEmpty()) ? name : no;
        }
        if (product == null || product.isEmpty()) {
            showWarn("请先选择产品", "开始采集前需要选择生产产品。");
            return;
        }
        final String orderNo = orderNumField.getText() != null ? orderNumField.getText().trim() : "";
        if (orderNo.isEmpty()) {
            showWarn("请输入生产单号", "开始采集前需要填写生产单号。");
            return;
        }
        final String productNo = productItem != null ? productItem.getNo()
                : (productCodeLabel.getText() != null ? productCodeLabel.getText().trim() : "");
        if (productNo.isEmpty()) {
            showWarn("产品编号为空", "请通过「选择」按钮重新选择产品以带出产品编号。");
            return;
        }

        // === 规格解析（FX 线程，无 I/O）===
        int m = 70, n = 4;
        try {
            String ms = casesPerPalletField.getText();
            String ns = boxesPerCaseField.getText();
            if (ms != null && !ms.trim().isEmpty()) m = Integer.parseInt(ms.trim());
            if (ns != null && !ns.trim().isEmpty()) n = Integer.parseInt(ns.trim());
        } catch (NumberFormatException ignored) {}

        // 采集规格持久化变更提示（FX 线程，Alert 弹窗）
        ShiwanM2Settings settings = ShiwanM2SettingsStore.get();
        int savedM = settings != null && settings.getBoxesPerPallet() != null ? settings.getBoxesPerPallet() : m;
        int savedN = settings != null && settings.getBoxesPerCase()   != null ? settings.getBoxesPerCase()   : n;
        if (m != savedM || n != savedN) {
            Alert specAlert = new Alert(Alert.AlertType.CONFIRMATION);
            specAlert.setTitle("采集规格变更确认");
            specAlert.setHeaderText("检测到已保存的采集规格与当前输入不一致");
            specAlert.setContentText("已保存：1垛 " + savedM + " 箱 / 1箱 " + savedN + " 盒\n"
                    + "当前：1垛 " + m + " 箱 / 1箱 " + n + " 盒\n是否覆盖保存为当前规格？");
            Optional<ButtonType> specResult = specAlert.showAndWait();
            if (specResult.isEmpty() || specResult.get() != ButtonType.OK) {
                m = savedM; n = savedN;
                casesPerPalletField.setText(String.valueOf(m));
                boxesPerCaseField.setText(String.valueOf(n));
            } else {
                persistSpec(m, n);
            }
        }

        // === 切换按钮为"检查中..."，后台执行所有 I/O 检查 ===
        final int mFinal = m, nFinal = n;
        final String productFinal = product;
        startCaptureBtn.setDisable(true);
        startCaptureBtn.setText("检查中...");

        productExecutor.submit(() -> runPreCaptureChecks(orderNo, productNo, productFinal, mFinal, nFinal));
    }

    /**
     * 在后台线程执行所有开始采集前的 I/O 检查。
     * 任何门禁不通过时用 Platform.runLater 恢复按钮并弹窗。
     * 全部通过后回到 FX 线程显示确认弹窗。
     */
    private void runPreCaptureChecks(String orderNo, String productNo, String product, int m, int n) {
        // 门禁1：码包检查
        try {
            String json = HttpUtil.doGet("/api/shiwan-m2/code-package/check");
            JsonNode cp = JSON.readTree(json);
            boolean cpFail = (cp != null && cp.has("code") && cp.get("code").asInt() != 200)
                    || (cp != null && cp.has("data") && cp.get("data").has("passed")
                        && !cp.get("data").get("passed").asBoolean());
            if (cpFail) {
                String msg = cp.has("message") ? cp.get("message").asText() : "请先导入小标、中标、大标码包。";
                Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("码包门禁未通过", msg); });
                return;
            }
        } catch (Exception e) {
            Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("码包门禁检查失败", e.getMessage()); });
            return;
        }

        // 门禁2：1号机连接检查
//        try {
//            String json = HttpUtil.doGet("/api/shiwan-m2/settings/check-m1-connection");
//            JsonNode m1 = JSON.readTree(json);
//            if (m1 == null || !m1.has("code") || m1.get("code").asInt() != 200) {
//                String msg = m1 != null && m1.has("message") ? m1.get("message").asText()
//                        : "请在系统设置中配置并确保1号机 SQL Server 可连接。";
//                Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("1号机连接未通过", msg); });
//                return;
//            }
//        } catch (Exception e) {
//            Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("1号机连接检查失败", e.getMessage()); });
//            return;
//        }

        // 门禁3：2号机本机数据库可访问
        try {
            ShiwanM2Settings s = ShiwanM2SettingsStore.load();
            ShiwanM2Settings.DbConnectionConfig db = s != null ? s.getDbConnection() : null;
            if (db == null || db.getHost() == null || db.getHost().isEmpty()
                    || db.getDatabase() == null || db.getDatabase().isEmpty()
                    || db.getUsername() == null || db.getUsername().isEmpty()) {
                Platform.runLater(() -> { resetStartCaptureBtn();
                    showWarn("数据库未配置", "请在系统设置→连接→数据库连接中填写2号机本机库信息。"); });
                return;
            }
            String dbBody = String.format(
                    "{\"host\":\"%s\",\"port\":\"%s\",\"database\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}",
                    escapeJson(db.getHost()), escapeJson(db.getPort() != null ? db.getPort() : "3306"),
                    escapeJson(db.getDatabase()), escapeJson(db.getUsername()),
                    escapeJson(db.getPassword() != null ? db.getPassword() : ""));
            String dbJson = HttpUtil.doPost("/api/shiwan-m2/settings/test-db-connection", dbBody);
            JsonNode dbTest = JSON.readTree(dbJson);
            if (dbTest == null || !dbTest.has("code") || dbTest.get("code").asInt() != 200) {
                String msg = dbTest != null && dbTest.has("message") ? dbTest.get("message").asText()
                        : "请在系统设置中配置并确保2号机本机数据库可访问。";
                Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("数据库连接未通过", msg); });
                return;
            }
        } catch (Exception e) {
            Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("数据库连接检查失败", e.getMessage()); });
            return;
        }

        // 门禁4：必选设备检查（仅校验配置和TCP网口可达性，不检查前端连接状态；IO设备将在采集启动时统一连接）
        try {
            String devJson = HttpUtil.doGet("/api/shiwan-m2/device/check-required");
            JsonNode dev = JSON.readTree(devJson);
            if (dev != null && dev.has("data") && dev.get("data").has("passed")
                    && !dev.get("data").get("passed").asBoolean()) {
                StringBuilder sb = new StringBuilder("设备未就绪，无法开始采集：\n\n");
                if (dev.get("data").has("failedChecks") && dev.get("data").get("failedChecks").isArray()) {
                    dev.get("data").get("failedChecks").forEach(item ->
                            sb.append("• ").append(item.asText()).append("\n"));
                }
                sb.append("\n请在「系统设置→设备→IO设备管理」中配置上述设备。");
                final String devMsg = sb.toString();
                Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("设备未就绪", devMsg); });
                return;
            }
        } catch (Exception e) {
            Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("设备检查失败", e.getMessage()); });
            return;
        }

        // 全部通过 → 回到 FX 线程显示确认弹窗
        Platform.runLater(() -> showConfirmAndStartCapture(orderNo, productNo, product, m, n));
    }

    /**
     * 组装设备连接状态JSON并URL编码（与致美斋启用任务校验策略一致）。
     * 格式示例：{"设备ID1":true,"设备ID2":false}
     */
    private String buildDeviceConnectionsJsonEncoded() {
        try {
            Map<String, Boolean> deviceConnections = new HashMap<>();
            String deviceResponseJson = HttpUtil.doGet("/api/device/list");
            ApiResult<List<IoDeviceDTO>> deviceResult = HttpUtil.parseJson(
                    deviceResponseJson,
                    new TypeReference<ApiResult<List<IoDeviceDTO>>>() {}
            );
            if (deviceResult != null && deviceResult.getCode() == 200 && deviceResult.getData() != null) {
                for (IoDeviceDTO device : deviceResult.getData()) {
                    boolean isConnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                    deviceConnections.put(device.getId(), isConnected);
                }
            }
            if (deviceConnections.isEmpty()) {
                return null;
            }
            String rawJson = JSON.writeValueAsString(deviceConnections);
            return URLEncoder.encode(rawJson, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            System.err.println("[开始采集-设备校验] 组装设备连接状态失败: " + e.getMessage());
            return null;
        }
    }

    /** 恢复开始采集按钮到可点击状态（FX 线程）。 */
    private void resetStartCaptureBtn() {
        startCaptureBtn.setDisable(false);
        startCaptureBtn.setText("开始采集");
    }

    /**
     * 所有门禁通过后在 FX 线程显示最终确认弹窗，确认后提交任务启动到后台线程。
     */
    private void showConfirmAndStartCapture(String orderNo, String productNo, String product, int m, int n) {
        resetStartCaptureBtn();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("开始采集确认");
        confirm.setHeaderText("确认开始生产？");
        confirm.setContentText("产品：" + product + "\n生产单号：" + orderNo
                + "\n1垛 " + m + " 箱  |  1箱 " + n + " 盒");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        startCaptureBtn.setDisable(true);
        startCaptureBtn.setText("启动中...");

        // 后台提交任务启动
        productExecutor.submit(() -> {
            try {
                String body = "{\"orderNo\":\"" + escapeJson(orderNo) + "\",\"productNo\":\""
                        + escapeJson(productNo) + "\",\"productName\":\"" + escapeJson(product)
                        + "\",\"boxesPerPallet\":" + m + ",\"boxesPerCase\":" + n + "}";
                String json = HttpUtil.doPost("/api/shiwan-m2/current-task/start", body);
                JsonNode start = JSON.readTree(json);
                if (start == null || !start.has("code") || start.get("code").asInt() != 200) {
                    String msg = start != null && start.has("message") ? start.get("message").asText() : "后端返回异常";
                    Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("开始采集失败", msg); });
                    return;
                }
            } catch (Exception e) {
                Platform.runLater(() -> { resetStartCaptureBtn(); showWarn("开始采集失败", e.getMessage()); });
                return;
            }
            // 任务启动成功 → 回 FX 线程更新 UI 并启动采集
            Platform.runLater(() -> finishStartCapture(orderNo, productNo, product, m, n));
        });
    }

    /**
     * 任务后端启动成功后在 FX 线程执行：更新 UI 状态、启动 M1 同步、启动 TCP 采集。
     */
    private void finishStartCapture(String orderNo, String productNo, String product, int m, int n) {
        persistSpec(m, n);
        isRunning = true;
        startCaptureBtn.setDisable(false);
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
        OperateLogBuilder.create()
                .module(ModuleNameEnum.BOX_CASE_ASSOCIATE)
                .operateType(OperateTypeEnum.START)
                .target(orderNo, product)
                .content("开始采集 - 产品：" + product + "，生产单：" + orderNo
                        + "，规格：每箱" + n + "盒/每垛" + m + "箱")
                .saveAsync();
        refreshProducedPalletCount(orderNo);
        refreshRejectCount(orderNo);

        // 连接所有IO设备（网口/串口），采集期间才占用端口
        connectAllDevices();
        // 启动 1号机 T_Code 轮询同步（后台线程，不阻塞 UI）
        startM1Sync();
        // 启动 TCP 相机采集（n=每箱盒数, m=每垛箱数）
        startTcpCapture(orderNo, productNo, n, m);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // ================================================================
    //  TCP 相机采集：启停 + 事件轮询
    // ================================================================

    /**
     * 启动 TCP 相机采集。HTTP 调用在后台线程执行，避免阻塞 FX 线程。
     * Timeline 事件轮询在 HTTP 响应返回后才启动。
     */
    private void startTcpCapture(String orderNo, String productNo, int boxesPerCase, int boxesPerPallet) {
        lastCaptureEventSeq = 0;
        processingCaptureEvents.set(false);
        stopCaptureEventsTimeline();

        final String body = String.format(
                "{\"orderNo\":\"%s\",\"productNo\":\"%s\",\"boxesPerCase\":%d,\"boxesPerPallet\":%d}",
                escapeJson(orderNo), escapeJson(productNo), boxesPerCase, boxesPerPallet);

        productExecutor.submit(() -> {
            String logMsg;
            LogType logType;
            try {
                String resp = HttpUtil.doPost("/api/shiwan-m2/capture/start", body);
                JsonNode node = JSON.readTree(resp);
                if (node != null && node.has("code") && node.get("code").asInt() == 200) {
                    logMsg = LocalDateTime.now().format(TIME_FMT) + "  TCP相机采集已启动";
                    logType = LogType.INFO;
                } else {
                    String errMsg = node != null && node.has("message") ? node.get("message").asText() : "未知错误";
                    logMsg = LocalDateTime.now().format(TIME_FMT) + "  TCP相机采集启动失败：" + errMsg;
                    logType = LogType.WARN;
                }
            } catch (Exception e) {
                logMsg = LocalDateTime.now().format(TIME_FMT) + "  TCP相机采集启动异常：" + e.getMessage();
                logType = LogType.WARN;
            }
            final String finalMsg = logMsg;
            final LogType finalType = logType;
            Platform.runLater(() -> {
                addOpLog(finalMsg, finalType);
                // 在 FX 线程上启动事件轮询 Timeline
                captureEventsTimeline = new Timeline(new KeyFrame(Duration.seconds(1),
                        ev -> scheduleCaptureEventsRefresh()));
                captureEventsTimeline.setCycleCount(Animation.INDEFINITE);
                captureEventsTimeline.play();
            });
        });
    }

    /**
     * 开始采集时在后台线程连接所有已启用的IO设备（网口/串口），
     * 采集期间才占用端口，停止采集时统一释放。
     */
    private void connectAllDevices() {
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doGet("/api/device/list");
                ApiResult<List<IoDeviceDTO>> result = HttpUtil.parseJson(
                        resp, new TypeReference<ApiResult<List<IoDeviceDTO>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    Platform.runLater(() -> addOpLog(
                            LocalDateTime.now().format(TIME_FMT) + "  获取设备列表失败，跳过IO设备连接", LogType.WARN));
                    return;
                }
                int connectedCount = 0;
                for (IoDeviceDTO device : result.getData()) {
                    if (!Boolean.TRUE.equals(device.getEnabled())) continue;
                    try {
                        DeviceConnectionManager.getInstance().startConnection(device);
                        connectedCount++;
                    } catch (Exception e) {
                        final String errMsg = device.getDeviceName() + " 连接失败：" + e.getMessage();
                        Platform.runLater(() -> addOpLog(
                                LocalDateTime.now().format(TIME_FMT) + "  [设备] " + errMsg, LogType.WARN));
                    }
                }
                final int cnt = connectedCount;
                Platform.runLater(() -> addOpLog(
                        LocalDateTime.now().format(TIME_FMT) + "  IO设备连接完成，已连接 " + cnt + " 台", LogType.INFO));
            } catch (Exception e) {
                Platform.runLater(() -> addOpLog(
                        LocalDateTime.now().format(TIME_FMT) + "  IO设备连接异常：" + e.getMessage(), LogType.WARN));
            }
        });
    }

    /** 停止 TCP 相机采集并关闭事件轮询。 */
    private void stopTcpCapture() {
        stopCaptureEventsTimeline();
        productExecutor.submit(() -> {
            try { HttpUtil.doPost("/api/shiwan-m2/capture/stop", ""); }
            catch (Exception ignored) {}
        });
    }

    private void stopCaptureEventsTimeline() {
        if (captureEventsTimeline != null) {
            captureEventsTimeline.stop();
            captureEventsTimeline = null;
        }
    }

    /**
     * 由 Timeline 每秒触发（FX 线程），将实际 HTTP 调用提交到后台线程执行，
     * 避免阻塞 FX 线程造成 UI 卡顿。使用 AtomicBoolean 防止并发拉取。
     */
    private void scheduleCaptureEventsRefresh() {
        if (!processingCaptureEvents.compareAndSet(false, true)) return;
        final long seqSnapshot = lastCaptureEventSeq;
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doGet("/api/shiwan-m2/capture/events?lastSeq=" + seqSnapshot);
                JsonNode root = JSON.readTree(resp);
                if (root == null || !root.has("code") || root.get("code").asInt() != 200) return;
                JsonNode dataArr = root.get("data");
                if (dataArr == null || !dataArr.isArray() || dataArr.size() == 0) return;
                Platform.runLater(() -> processCaptureEventsData(dataArr));
            } catch (Exception ignored) {
            } finally {
                processingCaptureEvents.set(false);
            }
        });
    }

    /**
     * 在 FX 线程上处理从后端拉取的采集事件，更新数据日志和 UI 计数。
     */
    private void processCaptureEventsData(JsonNode dataArr) {
        int bpc = parseSpecValue(boxesPerCaseField.getText(), 4);
        int cpp = parseSpecValue(casesPerPalletField.getText(), 70);
        String orderNo = orderNumField.getText() != null ? orderNumField.getText().trim() : "";

        for (JsonNode evt : dataArr) {
            long   seq  = evt.has("seq")     ? evt.get("seq").asLong()   : 0;
            String type = evt.has("type")    ? evt.get("type").asText()  : "";
            String msg  = evt.has("message") ? evt.get("message").asText(): "";
            String time = evt.has("time")    ? evt.get("time").asText()  : LocalDateTime.now().format(TIME_FMT);
            JsonNode evtData = evt.get("data");

            if (seq > lastCaptureEventSeq) lastCaptureEventSeq = seq;

            switch (type) {
                case "BOX_RECV":
                    addDataLog(time + "  " + msg, LogType.DATA);
                    break;

                case "CASE_RECV":
                    addDataLog(time + "  " + msg, LogType.DATA);
                    break;

                case "BOX_CODE":
                    currentBoxes++;
                    curBoxesLabel.setText(String.valueOf(currentBoxes));
                    addDataLog(time + "  " + msg, LogType.SUCCESS);
                    break;

                case "BOX_FAIL":
                    addDataLog(time + "  " + msg, LogType.ERROR);
                    addAlarmLog(time + "  " + msg, LogType.WARN);
                    // 盒码校验失败：黄灯告警（实物待后续在箱级剔除，不立即触发剔除装置）
                    ShiwanM2HardwareService.getInstance().yellowLightOn();
                    break;

                case "CASE_PENDING":
                    addDataLog(time + "  " + msg, LogType.WARN);
                    break;

                case "CASE_CODE":
                    addDataLog(time + "  收到箱码: " + msg.replace("收到箱码: ", ""), LogType.DATA);
                    break;

                case "ASSOCIATED": {
                    String caseCode = evtData != null && evtData.has("caseCode") ? evtData.get("caseCode").asText() : "";
                    int    cases    = evtData != null && evtData.has("currentCaseCount")
                            ? evtData.get("currentCaseCount").asInt() : (currentCases + 1);
                    boolean fullPallet = evtData != null && evtData.path("fullPallet").asBoolean(false);
                    String  palletCode = evtData != null && evtData.has("palletCode") ? evtData.get("palletCode").asText() : null;

                    if (evtData != null && evtData.has("boxCodes") && evtData.get("boxCodes").isArray()) {
                        List<String> bcs = new ArrayList<>();
                        evtData.get("boxCodes").forEach(b -> bcs.add(b.asText()));
                        for (String bc : bcs) {
                            addDataLog(time + "  盒箱关联 - 盒码：" + bc + " → 箱码：" + caseCode, LogType.DATA);
                        }
                    }
                    addDataLog(time + "  装箱完成 - 箱码：" + caseCode + " 已关联 " + bpc + " 盒", LogType.SUCCESS);
                    OperateLogBuilder.create()
                            .module(ModuleNameEnum.BOX_CASE_ASSOCIATE)
                            .operateType(OperateTypeEnum.ASSOCIATE)
                            .target(caseCode, orderNo)
                            .content("盒箱关联成功 - 箱码：" + caseCode + " 已关联 " + bpc + " 盒，生产单：" + orderNo)
                            .saveAsync();

                    currentBoxes = 0;
                    currentCases = cases;
                    curBoxesLabel.setText("0");
                    curCasesLabel.setText(String.valueOf(currentCases));

                    if (fullPallet) {
                        palletCount++;
                        palletCountLabel.setText(String.valueOf(palletCount));
                        String pc = palletCode != null ? palletCode : ("P" + orderNo + String.format("%03d", palletCount));
                        addDataLog(time + "  满垛完成 - 垛码：" + pc + " 共 " + cpp + " 箱，正在上传...", LogType.SUCCESS);
                        addOpLog(time + "  满垛完成，已生产垛数：" + palletCount, LogType.INFO);
                        OperateLogBuilder.create()
                                .module(ModuleNameEnum.PALLET_MANAGE)
                                .operateType(OperateTypeEnum.COMPLETE)
                                .target(pc, orderNo)
                                .content("满垛完成 - 垛码：" + pc + " 共 " + cpp + " 箱，生产单：" + orderNo
                                        + "，已生产垛数：" + palletCount)
                                .saveAsync();
                        uploadItems.add(0, new UploadItem("垛 " + pc, cpp + "箱", UploadStatus.PENDING));
                        currentCases = 0;
                        curCasesLabel.setText("0");
                    }
                    break;
                }

                case "ASSOC_FAIL": {
                    String reason = evtData != null && evtData.has("reason") ? evtData.get("reason").asText() : msg;
                    addAlarmLog(time + "  关联失败：" + reason, LogType.ERROR);
                    OperateLogBuilder.create()
                            .module(ModuleNameEnum.BOX_CASE_ASSOCIATE)
                            .operateType(OperateTypeEnum.ASSOCIATE)
                            .target("", orderNo)
                            .content("盒箱关联失败，生产单：" + orderNo)
                            .failReason(reason)
                            .saveAsync();
                    // 文档：盒箱关联失败 → 触发剔除装置 + 红灯+蜂鸣
                    ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
                    hw.triggerRejection();
                    hw.redLightAndBuzzer();
                    totalRejectCount++;
                    if (rejectCountLabel != null) rejectCountLabel.setText(String.valueOf(totalRejectCount));
                    break;
                }


                case "BOX_CONNECTED":
                    addOpLog(time + "  " + msg, LogType.SUCCESS);
                    break;
                case "CASE_CONNECTED":
                    addOpLog(time + "  " + msg, LogType.SUCCESS);
                    break;
                case "BOX_DISCONNECTED":
                case "CASE_DISCONNECTED":
                    addAlarmLog(time + "  " + msg, LogType.WARN);
                    break;
                case "BOX_ERROR":
                case "CASE_ERROR":
                    addAlarmLog(time + "  " + msg, LogType.ERROR);
                    break;
                case "STARTED":
                case "STOPPED":
                    break;
                default:
                    break;
            }
        }
    }

    /** 从事件 data 中安全提取文本值，fallback 到 defaultVal。 */
    private static String getEvtText(JsonNode data, String field, String defaultVal) {
        if (data == null || !data.has(field)) return defaultVal;
        JsonNode v = data.get(field);
        if (v.isArray()) {
            List<String> list = new ArrayList<>();
            v.forEach(n -> list.add(n.asText()));
            return String.join(", ", list);
        }
        return v.asText(defaultVal);
    }

    /** 通知后端启动 1 号机 T_Code 同步（后台线程执行，不阻塞 UI） */
    private void startM1Sync() {
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doPost("/api/shiwan-m2/m1-sync/start", "");
                JsonNode n = JSON.readTree(resp);
                if (n != null && n.has("code") && n.get("code").asInt() == 200) {
                    Platform.runLater(() -> addOpLog(
                            LocalDateTime.now().format(TIME_FMT) + "  1号机数据同步已启动", LogType.INFO));
                } else {
                    String msg = n != null && n.has("message") ? n.get("message").asText() : "未知错误";
                    Platform.runLater(() -> addOpLog(
                            LocalDateTime.now().format(TIME_FMT) + "  1号机数据同步启动失败：" + msg, LogType.WARN));
                }
            } catch (Exception e) {
                Platform.runLater(() -> addOpLog(
                        LocalDateTime.now().format(TIME_FMT) + "  1号机数据同步启动异常：" + e.getMessage(), LogType.WARN));
            }
        });
    }

    /** 通知后端停止 1 号机 T_Code 同步（后台线程执行，不阻塞 UI） */
    private void stopM1Sync() {
        productExecutor.submit(() -> {
            try {
                HttpUtil.doPost("/api/shiwan-m2/m1-sync/stop", "");
            } catch (Exception ignored) {}
        });
    }

    /** 同步产品到本地（后台线程调用） */
    private void syncProducts() {
        try {
            String resp = HttpUtil.doPost("/api/shiwan-m2/products/sync", "");
            JsonNode n = JSON.readTree(resp);
            if (n == null || !n.has("code") || n.get("code").asInt() != 200) {
                System.out.println("[产品同步] 失败：" + (n != null && n.has("message") ? n.get("message").asText() : "未知错误"));
            }
        } catch (Exception e) {
            System.out.println("[产品同步] 异常：" + e.getMessage());
        }
    }

    /** 模糊搜索产品并刷新下拉（后台线程调用） */
    private void fetchProducts(String keyword, int size) {
        try {
            StringBuilder url = new StringBuilder("/api/shiwan-m2/products/search?size=").append(size);
            if (keyword != null && !keyword.trim().isEmpty()) {
                url.append("&keyword=").append(URLEncoder.encode(keyword.trim(), "UTF-8"));
            }
            String resp = HttpUtil.doGet(url.toString());
            JsonNode n = JSON.readTree(resp);
            if (n == null || !n.has("code") || n.get("code").asInt() != 200) return;
            JsonNode data = n.get("data");
            if (data == null || !data.isArray()) return;
            List<ProductItem> list = new ArrayList<>();
            for (JsonNode item : data) {
                String name = item.has("productName") ? item.get("productName").asText() : "";
                String no   = item.has("productNo") ? item.get("productNo").asText() : "";
                String spec = item.has("spec") ? item.get("spec").asText() : "";
                if (no == null || no.isEmpty()) continue;
                list.add(new ProductItem(name, no, spec));
            }
            Platform.runLater(() -> {
                // 刷新条目前记住当前选中，防止 setAll 因引用变化清空 value
                ProductItem currentValue = productComboBox.getValue();
                suppressProductSearch = true;
                productItems.setAll(list);
                if (currentValue != null && productComboBox.getValue() == null) {
                    productComboBox.setValue(currentValue);
                }
                suppressProductSearch = false;
            });
        } catch (Exception ignored) {}
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
        final String stopOrderNo = orderNumField.getText();
        OperateLogBuilder.create()
                .module(ModuleNameEnum.BOX_CASE_ASSOCIATE)
                .operateType(OperateTypeEnum.STOP)
                .target(stopOrderNo, stopOrderNo)
                .content("停止采集，未满垛数据已保留（" + currentCases + "箱），生产单：" + stopOrderNo)
                .saveAsync();
        stopStatsRefresh();

        // 停止 1 号机 T_Code 轮询同步
        stopM1Sync();

        // 停止 TCP 相机采集
        stopTcpCapture();

        // 断开所有IO设备连接（释放网口和串口占用）
        productExecutor.submit(() -> {
            DeviceConnectionManager.getInstance().stopAllConnections();
            Platform.runLater(() -> addOpLog(
                    LocalDateTime.now().format(TIME_FMT) + "  所有IO设备连接已断开", LogType.INFO));
        });
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
        ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
        hw.allLightsOff();
        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  [报警器] 已发送关闭报警指令", LogType.INFO);
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
        String body = "{\"orderNo\":\"" + escapeJson(orderNo) + "\",\"currentCaseCount\":" + currentCaseCount + "}";
        HttpUtil.asyncPost("/api/shiwan-m2/box-case/force-full-pallet", body, json -> {
            try {
                JsonNode root = JSON.readTree(json);
                if (root == null || !root.has("code") || root.get("code").asInt() != 200) {
                    String errMsg = root != null && root.has("message") ? root.get("message").asText() : "后端返回异常";
                    OperateLogBuilder.create()
                            .module(ModuleNameEnum.PALLET_MANAGE)
                            .operateType(OperateTypeEnum.FORCE)
                            .target("", orderNo)
                            .content("强制满垛失败，生产单：" + orderNo + "，当前箱数：" + currentCaseCount)
                            .failReason(errMsg)
                            .saveAsync();
                    showWarn("强制满垛失败", errMsg);
                    return;
                }
                JsonNode data = root.get("data");
                String palletCode = data != null && data.has("palletCode") ? data.get("palletCode").asText() : null;
                if (palletCode == null) {
                    fetchNextVirtualSerialNumberAsync(code -> finishForcePallet(code, currentCaseCount));
                } else {
                    finishForcePallet(palletCode, currentCaseCount);
                }
            } catch (Exception e) {
                showWarn("强制满垛失败", e.getMessage());
            }
        }, e -> showWarn("强制满垛失败", e.getMessage()));
    }

    /** 强制满垛后端成功后更新 UI（FX 线程回调） */
    private void finishForcePallet(String palletCode, int caseCount) {
        palletCount++;
        palletCountLabel.setText(String.valueOf(palletCount));
        String now = LocalDateTime.now().format(TIME_FMT);
        addDataLog(now + "  强制满垛 - 垛码：" + palletCode + " 已生成，共" + caseCount + "箱", LogType.SUCCESS);
        addOpLog(now + "  强制满垛操作完成，已生产垛数：" + palletCount, LogType.INFO);
        final String forceOrderNo = orderNumField.getText();
        OperateLogBuilder.create()
                .module(ModuleNameEnum.PALLET_MANAGE)
                .operateType(OperateTypeEnum.FORCE)
                .target(palletCode, forceOrderNo)
                .content("强制满垛 - 垛码：" + palletCode + " 已生成，共" + caseCount + "箱，生产单：" + forceOrderNo
                        + "，已生产垛数：" + palletCount)
                .saveAsync();
        uploadItems.add(0, new UploadItem("垛 " + palletCode, caseCount + "箱", UploadStatus.PENDING));
        currentCases = 0;
        currentBoxes = 0;
        curCasesLabel.setText("0");
        curBoxesLabel.setText("0");
        refreshProducedPalletCount(orderNumField.getText());
    }

    /** 暂存未成垛数据并退出（异步 HTTP，回调里再 Platform.exit） */
    private void markUnfinishedAndExit(String orderNo) {
        String body = "{\"orderNo\":\"" + escapeJson(orderNo) + "\"}";
        HttpUtil.asyncPost("/api/shiwan-m2/current-task/mark-unfinished", body, json -> {
            try {
                JsonNode root = JSON.readTree(json);
                if (root == null || !root.has("code") || root.get("code").asInt() != 200) {
                    showWarn("暂存失败", root != null && root.has("message") ? root.get("message").asText() : "后端返回异常");
                    return;
                }
            } catch (Exception e) {
                showWarn("暂存失败", e.getMessage());
                return;
            }
            stopStatsRefresh();
            doExit();
        }, e -> {
            showWarn("暂存失败", e.getMessage());
        });
    }

    /** 刷新已生产垛数（异步：HTTP 在后台线程，UI 更新回 FX 线程） */
    private void refreshProducedPalletCount(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) return;
        try {
            String encodedOrder = java.net.URLEncoder.encode(orderNo, "UTF-8");
            HttpUtil.asyncGet("/api/shiwan-m2/stats/produced-pallet-count?orderNo=" + encodedOrder, json -> {
                try {
                    JsonNode root = JSON.readTree(json);
                    if (root != null && root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                        int count = root.get("data").asInt();
                        palletCount = count;
                        palletCountLabel.setText(String.valueOf(count));
                    }
                } catch (Exception ignored) {}
            }, null);
        } catch (Exception ignored) {}
    }

    /** 刷新总剔除数（异步：HTTP 在后台线程，UI 更新回 FX 线程） */
    private void refreshRejectCount(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) return;
        try {
            String encodedOrder = java.net.URLEncoder.encode(orderNo, "UTF-8");
            HttpUtil.asyncGet("/api/shiwan-m2/stats/reject-count?orderNo=" + encodedOrder, json -> {
                try {
                    JsonNode root = JSON.readTree(json);
                    if (root != null && root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                        int count = root.get("data").asInt();
                        totalRejectCount = count;
                        rejectCountLabel.setText(String.valueOf(count));
                    }
                } catch (Exception ignored) {}
            }, null);
        } catch (Exception ignored) {}
    }

    /** 开始定时刷新统计（垛数/剔除数）—— Timeline 仅做调度，HTTP 在后台执行 */
    private void startStatsRefresh(String orderNo) {
        stopStatsRefresh();
        if (orderNo == null || orderNo.isEmpty()) return;
        statsRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            refreshProducedPalletCount(orderNo);
            refreshRejectCount(orderNo);
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

    @FXML
    private void onExtractUnfinished() {
        showInfo("提取工单未成垛", "请输入或扫描垛内任意一箱的箱码，\n系统将查出对应生产订单并显示，\n确认后可回显到主页面继续生产。\n\n（功能开发中，请联系技术人员）");
    }

    @FXML
    private void onRetractReject() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("收回剔除");
        confirm.setHeaderText("确认向剔除装置发送收回指令？");
        confirm.setContentText("将通过串口向剔除装置发送收回动作指令。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ShiwanM2HardwareService.getInstance().retractRejection();
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [剔除装置] 已发送收回指令", LogType.INFO);
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

        fetchNextVirtualSerialNumberAsync(palletCode -> {
            if (palletCode == null || palletCode.isEmpty()) {
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
        });
    }

    /**
     * 异步获取下一个虚拟垛标，结果通过 callback 返回到 FX 线程。
     * 失败时 callback 收到 "V-" + timestamp 的 fallback 值。
     */
    private void fetchNextVirtualSerialNumberAsync(Consumer<String> callback) {
        HttpUtil.asyncGet("/api/shiwan-m2/pallet/next-virtual-serial-number", json -> {
            try {
                JsonNode node = JSON.readTree(json);
                if (node.has("code") && node.get("code").asInt(500) == 200
                        && node.has("data") && !node.get("data").isNull()) {
                    callback.accept(node.get("data").asText());
                    return;
                }
            } catch (Exception ignored) {}
            callback.accept("V-" + System.currentTimeMillis());
        }, e -> callback.accept("V-" + System.currentTimeMillis()));
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
        PENDING,   // 待上传
        UPLOADING, // 上传中
        DONE,      // 已上传
        FAILED     // 上传失败
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
                    "shiwan-m2-badge-pending", "shiwan-m2-badge-done", "shiwan-m2-badge-uploading"
            );
            switch (item.status) {
                case DONE:
                    statusBadge.setText("已上传");
                    statusBadge.getStyleClass().add("shiwan-m2-badge-done");
                    break;
                case UPLOADING:
                    statusBadge.setText("上传中");
                    statusBadge.getStyleClass().add("shiwan-m2-badge-uploading");
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

    /**
     * 前端产品模型（下拉选择）
     */
    public static class ProductItem {
        private final String name;
        private final String no;
        private final String spec;

        public ProductItem(String name, String no, String spec) {
            this.name = name;
            this.no = no;
            this.spec = spec;
        }

        public String getName() { return name; }
        public String getNo()   { return no; }
        public String getSpec() { return spec; }

        public String getDisplay() {
            String left = name != null ? name : "";
            String right = (no != null && !no.isEmpty()) ? ("（" + no + "）") : "";
            return left + right;
        }

        @Override
        public String toString() {
            return getDisplay();
        }
    }
}
