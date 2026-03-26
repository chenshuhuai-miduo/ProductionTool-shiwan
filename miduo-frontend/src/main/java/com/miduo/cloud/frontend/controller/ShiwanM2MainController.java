package com.miduo.cloud.frontend.controller;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.application.shiwan.UploadLogBus;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.frontend.ShiwanM2FrontendApplication;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.service.DeviceConnectionManager;
import com.miduo.cloud.frontend.service.ShiwanM2HardwareService;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.util.FxDialog;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import com.miduo.cloud.frontend.util.ShiwanM2ScannerConnectHelper;
import com.miduo.cloud.frontend.util.SvgIconLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
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

    // --- Tab 引用（全部在运行时懒加载内容，勿用 fx:include） ---
    @FXML private Tab manualTab;
    @FXML private Tab queryTab;
    @FXML private Tab dataReplaceTab;
    @FXML private Tab statsTab;
    @FXML private Tab packageTab;
    @FXML private Tab cancelTab;
    @FXML private Tab uploadTab;
    /** 生产统计 Tab 控制器（懒加载后赋值） */
    private ShiwanM2StatsController statsController;
    /** 码包管理 Tab 控制器（懒加载后赋值） */
    private ShiwanM2PackageController pkgController;

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

    /** 无需采集码切换按钮 */
    @FXML private Button noCodeNeededBtn;

    /** 打开/关闭报警切换按钮（初始文字：打开报警） */
    @FXML private Button alarmToggleBtn;

    /** 触发/收回剔除切换按钮（初始文字：触发剔除） */
    @FXML private Button rejectToggleBtn;

    /** 强制满垛按钮（采集停止时置灰） */
    @FXML private Button forcePalletBtn;

    /** 提取工单未成垛按钮（采集进行中时置灰，停止后恢复） */
    @FXML private Button extractUnfinishedBtn;

    /** 任务控制区帮助按钮 */
    @FXML private Button taskHelpButton;

    /** 测试报警灯亮/灭按钮（初始文字：测试报警灯亮） */
    @FXML private Button alarmLightTestBtn;

    /** 报警是否处于打开状态（false=未打开, true=已打开） */
    private boolean alarmOpen = false;

    /** 剔除是否处于触发状态（false=未触发, true=已触发） */
    private boolean rejectTriggered = false;

    /** 测试报警灯当前是否处于亮状态 */
    private boolean alarmLightOn = false;

    /** 无需采集码模式是否已激活 */
    private boolean noCodeNeededMode = false;

    // --- 状态栏 ---

    /** IO 设备连接汇总：已启用设备均由 DeviceConnectionManager 管理（含启动自动连接的盒码/箱码采集） */
    @FXML private Label deviceStatusLabel;

    /** 许可证状态容器 */
    @FXML private HBox licenseStatusBox;
    /** 许可证状态圆点 */
    @FXML private Region licenseStatusDot;
    /** 许可证状态文本 */
    @FXML private Label licenseStatusLabel;

    /** 自动采集（产线）状态条 */
    @FXML private HBox runningStatusBox;

    /** 自动采集脉冲环 */
    @FXML private Region runningPulseRing;

    /** 自动采集内点 */
    @FXML private Region runningDot;

    /** 自动采集脉冲动画 */
    private Animation runningPulseAnim;

    /** 手工采集状态条 */
    @FXML private HBox manualRunningStatusBox;

    /** 手工采集扫描线视口 */
    @FXML private StackPane manualScanViewport;

    /** 手工采集扫描亮线（横向平移） */
    @FXML private Region manualScanLine;

    /** 手工采集扫描线循环动画 */
    private Animation manualScanLineAnim;

    /** 供手工采集 Tab 回写状态栏（主窗口单例） */
    private static volatile ShiwanM2MainController mainWindowControllerRef;

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
    /** 最近一次收到“盒码批次（BOX_RECV）”后是否已经按批次去重数量写入 currentBoxes。 */
    private boolean boxRecvCountSet = false;
    private int palletCount     = 0;
    private int totalRejectCount = 0;
    /** 当前采集参数，用于"无需采集码"关闭时重启TCP采集 */
    private String currentOrderNo = "";
    private String currentProductNo = "";
    private int currentBoxesPerCase = 0;
    private int currentBoxesPerPallet = 0;

    private Timeline clockTimeline;
    private Timeline statsRefreshTimeline;
    /** 采集事件轮询 Timeline（每 1 秒触发一次调度，HTTP 调用在后台线程执行） */
    private Timeline captureEventsTimeline;
    /** 上次已处理的事件 seq，用于增量拉取 */
    private long lastCaptureEventSeq = 0;
    /** 防止采集事件并发拉取 */
    private final AtomicBoolean processingCaptureEvents = new AtomicBoolean(false);
    /** M1 同步结果事件轮询 Timeline（每 4 秒触发一次） */
    private Timeline m1SyncEventsTimeline;
    /** 上次已处理的 M1 同步事件 seq */
    private long lastM1SyncEventSeq = 0;
    /** 防止 M1 同步事件并发拉取 */
    private final AtomicBoolean processingM1SyncEvents = new AtomicBoolean(false);

    // ==================== 初始化 ====================

    /** 页面 id 与 Tab 索引对应（与 FXML 中 8 个 Tab 顺序一致） */
    private static final String[] PAGE_IDS = {
        "dataCollection", "manual", "query", "replace", "stats", "package", "cancel", "upload"
    };

    /** 保存初始化时 FXML 中全部 8 个 Tab 的原始引用，供 applyPageConfig() 多次调用时使用 */
    private List<Tab> allOriginalTabs;

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2MainController.class);
    /** 扫码枪设备类别代码（与 DeviceConnectionManager.convertCategoryTextToCode 保持一致） */
    private static final int CATEGORY_SCANNER = 7;
    /** “提取工单未成垛”弹窗打开期间的扫码输入回调（关闭弹窗时置空）。 */
    private volatile Consumer<String> extractUnfinishedScannerConsumer;
    private static final int TAB_IDX_MANUAL = 1;
    private static final int TAB_IDX_QUERY = 2;
    private static final int TAB_IDX_REPLACE = 3;
    private static final int TAB_IDX_CANCEL = 6;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainWindowControllerRef = this;
        setupManualScanViewportClip();
        setupProductComboBox();
        setupListViews();
        setupSpecListeners();
        setupClock();
        // 初始未采集：强制满垛/收回剔除置灰，提取工单未成垛可用
        forcePalletBtn.setDisable(true);
        rejectToggleBtn.setDisable(true);
        extractUnfinishedBtn.setDisable(false);
        SvgIconLoader.installHelpButtonGraphic(taskHelpButton);
        // 首屏最小化：仅保留首屏必要初始化，其余逻辑在首帧后执行，减少启动阻塞。
        Platform.runLater(() -> {
            setupInitialLogs();
            initActivationStatus();
            applyPageConfig();
            registerPalletEventListener();
            registerDeviceDataHandler();
            DeviceConnectionManager.getInstance().setDeviceStatusChangeHandler(this::updateDeviceStatusBar);
            setupTabLazyLoad();
            setupScannerWarmupOnRelevantTabs();
            initOrderNumFieldFormatter();
            loadSpecFromSettings();
        });

        // 依赖后端 HTTP 接口的初始化：等后端就绪信号后再执行，避免后端未启动完时报连接错误。
        ShiwanM2FrontendApplication.runAfterBackendReady(() -> {
            requestSuggestedOrderNo();
            checkDbConnectionOnStartup();
            checkUnfinishedOnStartup();
            autoConnectConfiguredDevicesOnStartup();
            updateDeviceStatusBar();
        }, 60);
    }

    /**
     * 主界面打开后按系统设置自动连接已启用 IO 设备：
     * - 非相机设备（扫码枪、报警灯等）由前端 DeviceConnectionManager 直连；
     * - 盒码/箱码采集相机由后端 TCP 采集服务统一管理持久连接（调用 /capture/connect-cameras），
     *   开始采集时直接复用该连接，避免双重 TCP 端口。
     */
    private void autoConnectConfiguredDevicesOnStartup() {
        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  正在按系统设置自动连接IO设备...", LogType.INFO);
        connectAllDevices(true, false);   // 非相机设备
        connectCamerasPersistently();     // 相机设备（后端持久连接）
    }

    /** 通知后端建立相机持久 TCP 连接，开始采集时复用，不再创建新端口。 */
    private void connectCamerasPersistently() {
        productExecutor.submit(() -> {
            try {
                HttpUtil.doPost("/api/shiwan-m2/capture/connect-cameras", "");
                Platform.runLater(() -> addOpLog(
                        LocalDateTime.now().format(TIME_FMT) + "  相机TCP持久连接已触发", LogType.INFO));
            } catch (Exception e) {
                Platform.runLater(() -> addOpLog(
                        LocalDateTime.now().format(TIME_FMT) + "  相机TCP预连接失败（可在开始采集时重试）：" + e.getMessage(),
                        LogType.WARN));
            }
        });
    }

    /** 状态栏统计：仅统计由前端连接管理器维护的设备（不含盒码/箱码采集相机）。 */
    private static boolean isDeviceCountedForStatusBar(IoDeviceDTO device) {
        return device != null
                && Boolean.TRUE.equals(device.getEnabled())
                && !isCameraCaptureDevice(device);
    }

    /** 盒码/箱码采集相机设备识别。 */
    private static boolean isCameraCaptureDevice(IoDeviceDTO device) {
        if (device == null) {
            return false;
        }
        String cat = device.getDeviceCategory();
        return "盒码采集".equals(cat) || "箱码采集".equals(cat);
    }

    /**
     * 刷新状态栏「设备状态：…」（后台请求列表，避免阻塞 UI；连接变化由 DeviceConnectionManager 回调）。
     */
    private void updateDeviceStatusBar() {
        if (deviceStatusLabel == null) {
            return;
        }
        productExecutor.submit(() -> {
            try {
                String responseJson = HttpUtil.doGet("/api/device/list");
                ApiResult<List<IoDeviceDTO>> result = HttpUtil.parseJson(
                        responseJson, new TypeReference<ApiResult<List<IoDeviceDTO>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    Platform.runLater(() -> deviceStatusLabel.setText("设备状态：状态未知"));
                    return;
                }
                int enabledCount = 0;
                int connectedCount = 0;
                for (IoDeviceDTO device : result.getData()) {
                    if (!isDeviceCountedForStatusBar(device)) {
                        continue;
                    }
                    enabledCount++;
                    if (DeviceConnectionManager.getInstance().isConnected(device.getId())) {
                        connectedCount++;
                    }
                }
                final int e = enabledCount;
                final int c = connectedCount;
                Platform.runLater(() -> {
                    String status;
                    if (e == 0) {
                        status = "设备状态：无启用设备";
                    } else if (c == 0) {
                        status = "设备状态：全部未连接";
                    } else if (c == e) {
                        status = "设备状态：全部已连接";
                    } else {
                        status = "设备状态：部分未连接";
                    }
                    deviceStatusLabel.setText(status);
                });
            } catch (Exception ex) {
                log.debug("[设备状态栏] 更新失败: {}", ex.getMessage());
                Platform.runLater(() -> deviceStatusLabel.setText("设备状态：状态未知"));
            }
        });
    }

    /**
     * Tab 2-8 懒加载：FXML 在首次切换到该 Tab 时才解析并加载，减少启动期 FXML 解析开销。
     * 生产统计 / 码包管理 Tab 额外在加载后触发 onFirstShow()。
     */
    private void setupTabLazyLoad() {
        setupTabFxmlLazy(manualTab,      "ShiwanM2ManualTab.fxml",           null);
        setupTabFxmlLazy(queryTab,       "ShiwanM2QueryTab.fxml",            null);
        setupTabFxmlLazy(dataReplaceTab, "ShiwanM2DataReplacePane.fxml",     null);
        setupTabFxmlLazy(statsTab,       "ShiwanM2StatsTab.fxml", ctrl -> {
            statsController = (ShiwanM2StatsController) ctrl;
            if (statsController != null) statsController.onFirstShow();
        });
        setupTabFxmlLazy(packageTab,     "ShiwanM2PackageTab.fxml", ctrl -> {
            pkgController = (ShiwanM2PackageController) ctrl;
            if (pkgController != null) pkgController.onFirstShow();
        });
        setupTabFxmlLazy(cancelTab,      "ShiwanM2CancelAssociationPane.fxml", null);
        setupTabFxmlLazy(uploadTab,      "ShiwanM2UploadTab.fxml",           null);
    }

    /**
     * 为指定 Tab 注册首次激活时的 FXML 懒加载监听器。
     * Tab 内容在第一次被选中后才通过 FXMLLoader 加载，之后不再重复加载。
     *
     * @param tab       目标 Tab（为 null 则跳过）
     * @param fxmlName  FXML 文件名（位于 /fxml/ 目录下）
     * @param onLoaded  加载完成后的回调，参数为子控制器实例（可为 null）
     */
    private void setupTabFxmlLazy(Tab tab, String fxmlName, Consumer<Object> onLoaded) {
        if (tab == null) return;
        AtomicBoolean loaded = new AtomicBoolean(false);
        tab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected && loaded.compareAndSet(false, true)) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/" + fxmlName));
                    Parent content = loader.load();
                    tab.setContent(content);
                    if (onLoaded != null) {
                        onLoaded.accept(loader.getController());
                    }
                } catch (Exception e) {
                    log.error("懒加载 Tab FXML 失败: {}", fxmlName, e);
                }
            }
        });
    }

    /**
     * 切换到依赖扫码枪的 Tab 时后台尝试重连（与手工采集「开始采集」、系统设置-设备逻辑一致）。
     */
    private void setupScannerWarmupOnRelevantTabs() {
        if (mainTabPane == null) {
            return;
        }
        mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx == null) {
                return;
            }
            int idx = newIdx.intValue();
            if (idx == TAB_IDX_MANUAL || idx == TAB_IDX_QUERY || idx == TAB_IDX_REPLACE || idx == TAB_IDX_CANCEL) {
                ShiwanM2ScannerConnectHelper.tryReconnectScannersAsync();
            }
        });
    }

    private void setupManualScanViewportClip() {
        if (manualScanViewport == null) {
            return;
        }
        Rectangle clip = new Rectangle(30, 14);
        clip.setArcWidth(4);
        clip.setArcHeight(4);
        manualScanViewport.setClip(clip);
    }

    private void ensureManualScanLineAnim() {
        if (manualScanLine == null || manualScanLineAnim != null) {
            return;
        }
        manualScanLine.setOpacity(0);
        manualScanLine.setTranslateX(-2);
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(manualScanLine.translateXProperty(), -2),
                        new KeyValue(manualScanLine.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(60),
                        new KeyValue(manualScanLine.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(420),
                        new KeyValue(manualScanLine.translateXProperty(), 24, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(540),
                        new KeyValue(manualScanLine.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(2200),
                        new KeyValue(manualScanLine.translateXProperty(), -2),
                        new KeyValue(manualScanLine.opacityProperty(), 0.0)));
        t.setCycleCount(Animation.INDEFINITE);
        manualScanLineAnim = t;
    }

    /** 手工采集 Tab 调用：显示/隐藏「手工采集中」与扫描线动画 */
    public void setManualCaptureStatusBarRunning(boolean running) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setManualCaptureStatusBarRunning(running));
            return;
        }
        if (manualRunningStatusBox == null) {
            return;
        }
        manualRunningStatusBox.setVisible(running);
        manualRunningStatusBox.setManaged(running);
        if (running) {
            ensureManualScanLineAnim();
            if (manualScanLineAnim != null) {
                manualScanLineAnim.playFromStart();
            }
        } else {
            if (manualScanLineAnim != null) {
                manualScanLineAnim.stop();
            }
            if (manualScanLine != null) {
                manualScanLine.setTranslateX(-2);
                manualScanLine.setOpacity(0);
            }
        }
    }

    public static void notifyManualCaptureRunning(boolean running) {
        ShiwanM2MainController c = mainWindowControllerRef;
        if (c != null) {
            c.setManualCaptureStatusBarRunning(running);
        }
    }

    /**
     * 注册设备数据分发处理器：
     * 非扫码枪设备数据写入数据接收区；
     * category=7（扫码枪）按当前场景路由到对应输入框，不写入数据接收区。
     */
    private void registerDeviceDataHandler() {
        DeviceConnectionManager.getInstance().setDataReceiveHandlerWithOrder((categoryCode, data) -> {
            log.debug("[设备数据分发] category={} data={}", categoryCode, data);

            // 扫码枪数据不写数据接收区，直接路由到各页面输入框
            if (categoryCode == CATEGORY_SCANNER) {
                dispatchScannerCode(data);
                return;
            }

            // 查询设备名称，写入数据接收区
            IoDeviceDTO device = DeviceConnectionManager.getInstance().getDeviceByCategory(categoryCode);
            String deviceLabel = device != null ? device.getDeviceName() : ("类别" + categoryCode);
            String now = LocalDateTime.now().format(TIME_FMT);
            addDataLog(now + "  [" + deviceLabel + "] 收到: " + data, LogType.DATA);
        });
    }

    /**
     * 扫码枪数据路由：
     * 1) 若“提取工单未成垛”弹窗打开，优先填入该弹窗输入框并触发查询；
     * 2) 否则按当前活动窗口中的主界面当前页分发到手工采集/数据查询/数据替换/取消关联。
     */
    private void dispatchScannerCode(String raw) {
        final String code = raw == null ? "" : raw.trim();
        if (code.isEmpty()) return;

        // 弹窗场景优先：提取工单未成垛
        Consumer<String> popupConsumer = extractUnfinishedScannerConsumer;
        if (popupConsumer != null) {
            popupConsumer.accept(code);
            log.debug("[扫码枪路由] 提取未成垛弹窗接收 code={}", code);
            return;
        }

        if (mainTabPane == null || mainTabPane.getScene() == null) return;
        javafx.stage.Window mainWindow = mainTabPane.getScene().getWindow();
        javafx.stage.Window focusedWindow = javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isFocused)
                .findFirst().orElse(null);
        // 非主界面激活时，不抢占扫码数据（提取未成垛弹窗由 popupConsumer 优先处理）
        if (focusedWindow != null && focusedWindow != mainWindow) {
            log.debug("[扫码枪路由] 当前焦点非主界面窗口，忽略 code={}", code);
            return;
        }

        int selectedIdx = mainTabPane.getSelectionModel().getSelectedIndex();
        if (selectedIdx == TAB_IDX_MANUAL) {
            ShiwanM2ManualController manualCtrl = ShiwanM2ManualController.getInstance();
            if (manualCtrl != null) {
                manualCtrl.onScanCode(code);
                log.debug("[扫码枪路由] 手工采集页接收 code={}", code);
            }
            return;
        }
        if (selectedIdx == TAB_IDX_QUERY) {
            ShiwanM2QueryController queryCtrl = ShiwanM2QueryController.getInstance();
            if (queryCtrl != null) {
                queryCtrl.onScanCode(code);
                log.debug("[扫码枪路由] 数据查询页接收 code={}", code);
            }
            return;
        }
        if (selectedIdx == TAB_IDX_REPLACE) {
            ShiwanM2DataReplaceController replaceCtrl = ShiwanM2DataReplaceController.getInstance();
            if (replaceCtrl != null) {
                replaceCtrl.onScanCode(code);
                log.debug("[扫码枪路由] 数据替换页接收 code={}", code);
            }
            return;
        }
        if (selectedIdx == TAB_IDX_CANCEL) {
            ShiwanM2CancelAssociationController cancelCtrl = ShiwanM2CancelAssociationController.getInstance();
            if (cancelCtrl != null) {
                cancelCtrl.onScanCode(code);
                log.debug("[扫码枪路由] 取消关联页接收 code={}", code);
            }
        }
    }

    /**
     * 注册 UploadLogBus 垛状态事件监听：
     * 每个垛码在实时上传区仅一行，状态在待上传/上传中/已上传/失败间就地变更；
     * UPLOADING 时若已有该行则只改状态与箱数（不挪位置），无行时再在顶部插入；
     * SUCCESS/FAILED → 同上更新状态 + 数据接收区追加结果日志。
     */
    private void registerPalletEventListener() {
        UploadLogBus.registerPalletEventListener((palletCode, boxCount, status, errorMsg) ->
            Platform.runLater(() -> {
                String time = LocalDateTime.now().format(TIME_FMT);
                ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
                switch (status) {
                    case UPLOADING: {
                        String bc = boxCount + "箱";
                        UploadItem keep = null;
                        int minIdx = Integer.MAX_VALUE;
                        for (int i = 0; i < uploadItems.size(); i++) {
                            UploadItem it = uploadItems.get(i);
                            if (!matchesPalletCode(it.palletCode, palletCode)) continue;
                            it.status = UploadStatus.UPLOADING;
                            it.boxCount = bc;
                            if (i < minIdx) {
                                minIdx = i;
                                keep = it;
                            }
                        }
                        if (keep == null) {
                            String disp = formatPalletDisplayForCard(palletCode);
                            if (disp.isEmpty() && palletCode != null && !palletCode.trim().isEmpty()) {
                                disp = "垛 " + normalizePalletKey(palletCode);
                            }
                            if (!disp.isEmpty()) {
                                uploadItems.add(0, new UploadItem(disp, bc, UploadStatus.UPLOADING));
                            }
                        } else {
                            // 同一垛仅保留一行：去掉重复项，不在列表内改顺序（只变状态）
                            final UploadItem keepRef = keep;
                            uploadItems.removeIf(it -> it != keepRef && matchesPalletCode(it.palletCode, palletCode));
                        }
                        uploadDataList.refresh();
                        String base = time + " 垛码 " + palletCode + "，箱数 " + boxCount + "，";
                        addDataLog(base + "开始上传", LogType.INFO);
                        addDataLog(base + "上传中…", LogType.UPLOAD_BLUE);
                        break;
                    }
                    case SUCCESS:
                        updateUploadItemStatus(palletCode, UploadStatus.DONE);
                        addDataLog(time + " 垛码 " + palletCode + "，箱数 " + boxCount + "，上传成功", LogType.SUCCESS);
                        // 文档：上传成功 → 绿灯常亮，1 分钟后自动熄灭
                        hw.greenLightOn();
                        break;
                    case FAILED:
                        updateUploadItemStatus(palletCode, UploadStatus.FAILED);
                        String reason = (errorMsg != null && !errorMsg.isEmpty()) ? errorMsg : "未知错误";
                        addDataLog(time + " 垛码 " + palletCode + "，箱数 " + boxCount + "，上传失败（" + reason + "）", LogType.ERROR);
                        addAlarmLog(time + "  上传失败：" + palletCode + "（" + reason + "）", LogType.ERROR);
                        // 文档：上传失败 → 红灯常亮 + 蜂鸣
                        hw.redLightAndBuzzer();
                        break;
                    default:
                        break;
                }
            })
        );
    }

    /** 按垛码更新状态，并合并同一垛码的重复行（只保留一条） */
    private void updateUploadItemStatus(String palletCode, UploadStatus newStatus) {
        UploadItem keep = null;
        int minIdx = Integer.MAX_VALUE;
        for (int i = 0; i < uploadItems.size(); i++) {
            UploadItem item = uploadItems.get(i);
            if (!matchesPalletCode(item.palletCode, palletCode)) continue;
            item.status = newStatus;
            if (i < minIdx) {
                minIdx = i;
                keep = item;
            }
        }
        if (keep != null) {
            final UploadItem keepRef = keep;
            uploadItems.removeIf(it -> it != keepRef && matchesPalletCode(it.palletCode, palletCode));
        }
        uploadDataList.refresh();
    }

    /**
     * 实时上传数据区卡片第一行：统一展示为「垛」+ 垛码值（去掉已带的垛/垛码前缀，避免重复）。
     */
    private static String formatPalletDisplayForCard(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replaceFirst("^垛\\s*码?\\s*", "");
        return s.isEmpty() ? "" : "垛 " + s;
    }

    /** 去掉「垛/垛码」前缀后的垛码，用于列表去重与事件匹配（避免同一垛多条记录） */
    private static String normalizePalletKey(String storedOrRaw) {
        if (storedOrRaw == null) return "";
        String s = storedOrRaw.trim().replaceFirst("^垛\\s*码?\\s*", "");
        return s.trim();
    }

    /** 列表中存储的垛显示串与事件里的垛码是否同一垛 */
    private static boolean matchesPalletCode(String storedDisplay, String palletCodeFromEvent) {
        if (palletCodeFromEvent == null) return false;
        String k1 = normalizePalletKey(palletCodeFromEvent);
        String k2 = normalizePalletKey(storedDisplay);
        if (!k1.isEmpty() && k1.equals(k2)) return true;
        return palletCodeFromEvent.equals(storedDisplay)
                || ("垛 " + palletCodeFromEvent).equals(storedDisplay)
                || palletCodeFromEvent.equals(storedDisplay.replaceFirst("^垛 ", ""));
    }

    /**
     * 满垛/强制满垛时在顶部插入「待上传」行；同一归一化垛码先删旧再插，保证实时上传区每条垛码仅一行。
     */
    private void addPendingUploadRowAtTop(String palletCodeRaw, String boxCountLabel) {
        String key = normalizePalletKey(palletCodeRaw);
        if (!key.isEmpty()) {
            uploadItems.removeIf(it -> key.equals(normalizePalletKey(it.palletCode)));
        }
        String disp = formatPalletDisplayForCard(palletCodeRaw);
        if (disp.isEmpty() && key != null && !key.isEmpty()) {
            disp = "垛 " + key;
        }
        if (disp.isEmpty()) {
            return;
        }
        uploadItems.add(0, new UploadItem(disp, boxCountLabel, UploadStatus.PENDING));
    }

    /** 根据系统设置中的页面配置，调整主界面 Tab 的显示与顺序（可多次调用，实时生效） */
    private void applyPageConfig() {
        if (mainTabPane == null) return;
        // 首次调用时保存原始 8 个 Tab 的引用，后续调用直接从原始列表取
        if (allOriginalTabs == null || allOriginalTabs.isEmpty()) {
            allOriginalTabs = new ArrayList<>(mainTabPane.getTabs());
        }
        var settings = ShiwanM2SettingsStore.get();
        List<String> order = settings.getPageTabOrder() != null ? settings.getPageTabOrder() : ShiwanM2Settings.defaultPageTabOrder();
        var visible = settings.getPageVisible() != null ? settings.getPageVisible() : ShiwanM2Settings.defaultPageVisible();
        List<Tab> newOrder = new ArrayList<>();
        for (String id : order) {
            if (!Boolean.TRUE.equals(visible.get(id))) continue;
            int idx = indexOfPageId(id);
            if (idx >= 0 && idx < allOriginalTabs.size()) newOrder.add(allOriginalTabs.get(idx));
        }
        if (newOrder.isEmpty()) return;
        mainTabPane.getTabs().setAll(newOrder);
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
                if (item == null) return "";
                String n = item.getName();
                return n != null ? n : "";
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

        // 启动时仅从本地加载产品列表。
        // 远端产品同步由 Application 在主界面打开前先尝试一次；
        // 若该次失败，则在主界面打开后（首次点产品选择）再重试一次。
        productExecutor.submit(() -> fetchProducts("", 50));
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

    /** 初始化生产单号输入框格式：仅允许字母/数字，最大 16 位。 */
    private void initOrderNumFieldFormatter() {
        orderNumField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (!newText.matches("[a-zA-Z0-9]{0,16}")) return null;
            return change;
        }));
    }

    /**
     * 从后端获取当天建议生产单号（YYYYMMDD + 自增序号）并回填到输入框。
     * 仅在当前输入框为空时写入，避免覆盖用户手工输入或“未成垛恢复”回填值。
     */
    private void requestSuggestedOrderNo() {
        // 异步获取建议单号（当天日期+首个未用序号）
        String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        HttpUtil.asyncGet("/api/shiwan-m2/current-task/suggest-order-no?prefix=" + prefix,
                json -> {
                    try {
                        JsonNode node = JSON.readTree(json);
                        if (node != null && node.has("data") && !node.get("data").isNull()) {
                            String suggested = node.get("data").asText();
                            Platform.runLater(() -> {
                                if (orderNumField.getText() == null || orderNumField.getText().isEmpty()) {
                                    orderNumField.setText(suggested);
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                },
                ignored -> {});
    }

    /** 为输入框添加数字过滤器：1–999 正整数，禁止前导零（如 01、002） */
    private void applyNumericFilter(TextField field, int min, int max) {
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            // 首位须为 1-9，总长 1–3 位，避免 01、00 等形式
            if (!newText.matches("[1-9]\\d{0,2}")) return null;
            int value = Integer.parseInt(newText);
            if (value < min || value > max) return null;
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
            ShiwanM2FrontendApplication.LicenseStatusSnapshot startupSnapshot =
                ShiwanM2FrontendApplication.getStartupLicenseStatusSnapshot();
            if (startupSnapshot != null) {
                applyLicenseStatusUi(startupSnapshot.getStatus(),
                        startupSnapshot.getRemainingDays(),
                        startupSnapshot.getExpireDate());
                return;
            }

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
            LocalDate expireDate = licenseInfo.getExpireDate();
            applyLicenseStatusUi(status, remainingDays, expireDate);

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

    private void applyLicenseStatusUi(com.miduo.cloud.entity.enums.LicenseStatusEnum status,
                                      long remainingDays,
                                      LocalDate expireDate) {
        String statusType;
        String statusText;
        String tooltipText;
        String expiredDay = expireDate != null ? expireDate.toString() : "未知";

        switch (status) {
            case ACTIVATED:
                if (remainingDays > 30) {
                    statusType = "normal";
                    statusText = "授权剩余: " + remainingDays + "天";
                    tooltipText = "到期：" + expiredDay;
                } else if (remainingDays >= 7) {
                    statusType = "warning";
                    statusText = "授权剩余: " + remainingDays + "天";
                    tooltipText = "到期：" + expiredDay + "\n注意：即将到期，请尽快续期";
                } else if (remainingDays > 0) {
                    statusType = "urgent";
                    statusText = "授权剩余: " + remainingDays + "天";
                    tooltipText = "到期：" + expiredDay + "\n注意：紧急，请立即续期！";
                } else {
                    statusType = "urgent";
                    statusText = "授权剩余: 不足1天";
                    tooltipText = "到期：" + expiredDay + "\n注意：紧急，请立即续期！";
                }
                break;
            case TRIAL_ACTIVE:
                statusType = "trial";
                statusText = remainingDays > 0
                    ? "试用剩余: " + remainingDays + "天"
                    : "试用剩余: 不足1天";
                tooltipText = "试用模式\n到期: " + expiredDay + "\n提示：点击激活正式版";
                break;
            case TRIAL_EXPIRED:
                statusType = "expired";
                statusText = "试用已过期";
                tooltipText = "试用期已结束\n必须激活才能使用";
                break;
            case EXPIRED:
                statusType = "expired";
                statusText = "已过期";
                tooltipText = "过期时间：" + expiredDay + "\n错误：已过期，必须续期";
                break;
            case UNACTIVATED:
            default:
                statusType = "expired";
                statusText = "未激活";
                tooltipText = "软件未激活\n提示：点击进行激活";
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
    }

    // ==================== 菜单事件处理 ====================

    @FXML
    private void onExit() {
        requestExit();
    }

    public void requestExit() {
        if (isRunning) {
            FxDialog.warn(
                    mainTabPane.getScene().getWindow(),
                    "无法退出",
                    "系统当前正在采集中，请先停止采集后再退出。"
            );
            return;
        }
        if (currentCases > 0) {
            int idx = FxDialog.choice(
                    mainTabPane.getScene().getWindow(),
                    "退出确认",
                    "存在未满垛数据（" + currentCases + "箱），是否退出？\n暂存后可通过「提取工单未成垛」继续生产。",
                    FxDialog.BtnDef.cancel("取消"),
                    FxDialog.BtnDef.primary("强制满垛"),
                    FxDialog.BtnDef.warn("暂存退出")
            );
            if (idx <= 0) return; // 取消或关闭窗口
            String orderNo = orderNumField.getText() != null ? orderNumField.getText().trim() : "";
            if (idx == 1) {
                forceFullPalletBackend(orderNo, currentCases);
                doExit();
                return;
            } else if (idx == 2) {
                markUnfinishedAndExit(orderNo);
                return;
            }
        } else {
            boolean ok = FxDialog.confirm(
                    mainTabPane.getScene().getWindow(),
                    "退出确认",
                    "确认退出米多赋码采集关联系统？"
            );
            if (!ok) return;
        }
        doExit();
    }

    private void doExit() {
        if (clockTimeline != null) clockTimeline.stop();
        if (statsRefreshTimeline != null) statsRefreshTimeline.stop();
        if (captureEventsTimeline != null) captureEventsTimeline.stop();
        // 无论是否处于采集状态，退出时都停止TCP采集和1号机同步，释放连接和串口
        stopTcpCapture();
        stopM1Sync();
        DeviceConnectionManager.getInstance().stopAllConnections();
        productExecutor.shutdownNow();
        palletCount = 0;
        totalRejectCount = 0;
        Platform.exit();
    }

    @FXML
    private void onSystemConfig() {
        try {
            FXMLLoader pwdLoader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2PasswordDialog.fxml"));
            Parent pwdRoot = pwdLoader.load();
            ShiwanM2PasswordDialogController pwdCtrl = pwdLoader.getController();

            Stage pwdStage = new Stage();
            pwdStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            pwdStage.initModality(Modality.APPLICATION_MODAL);
            Scene pwdScene = new Scene(pwdRoot);
            pwdScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            pwdStage.setScene(pwdScene);
            pwdStage.setResizable(false);
            pwdStage.showAndWait();

            if (!pwdCtrl.isConfirmed()) return;

            String expectedPwd = ShiwanM2SettingsStore.get().getSystemSettingsPassword();
            if (expectedPwd == null) expectedPwd = "123456";
            if (!expectedPwd.equals(pwdCtrl.getPassword())) {
                // 密码错误：重新弹窗并显示错误提示
                FXMLLoader errLoader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2PasswordDialog.fxml"));
                Parent errRoot = errLoader.load();
                ShiwanM2PasswordDialogController errCtrl = errLoader.getController();
                errCtrl.showError("密码错误，请重新输入");

                Stage errStage = new Stage();
                errStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                errStage.initModality(Modality.APPLICATION_MODAL);
                Scene errScene = new Scene(errRoot);
                errScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                errStage.setScene(errScene);
                errStage.setResizable(false);
                errStage.showAndWait();

                if (!errCtrl.isConfirmed()) return;
                if (!expectedPwd.equals(errCtrl.getPassword())) {
                    showWarn("密码错误", "密码错误，已取消操作。");
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showWarn("弹窗错误", "无法打开密码验证弹窗：" + e.getMessage());
            return;
        }

        // 密码验证通过，打开系统设置弹窗
        openSystemSettingsDialog();
    }

    /** 打开系统设置弹窗（自定义无装饰窗口，标题栏在 FXML 内） */
    private void openSystemSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ShiwanM2SystemSettingsDialog.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.initStyle(StageStyle.UNDECORATED);
            settingsStage.initModality(Modality.WINDOW_MODAL);
            settingsStage.initOwner(currentTimeLabel.getScene().getWindow());
            settingsStage.setScene(new Scene(root));
            settingsStage.setResizable(false);
            settingsStage.showAndWait();
            // 设置保存后立即刷新 Tab 显示（响应页面配置开关变更）
            applyPageConfig();
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
            helpStage.initStyle(StageStyle.TRANSPARENT);
            helpStage.initModality(Modality.WINDOW_MODAL);
            helpStage.initOwner(currentTimeLabel.getScene().getWindow());
            helpStage.setResizable(false);

            ShiwanM2HelpController ctrl = loader.getController();
            ctrl.initDrag(helpStage);

            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.web("#000000", 0.15));
            shadow.setRadius(24);
            shadow.setOffsetY(8);
            root.setEffect(shadow);

            StackPane wrapper = new StackPane(root);
            wrapper.setStyle("-fx-background-color: transparent;");
            wrapper.setPadding(new Insets(20));

            Scene scene = new Scene(wrapper);
            scene.setFill(Color.TRANSPARENT);
            helpStage.setScene(scene);

            helpStage.setOnShown(e -> {
                javafx.stage.Window owner = currentTimeLabel.getScene().getWindow();
                helpStage.setX(owner.getX() + (owner.getWidth()  - helpStage.getWidth())  / 2.0);
                helpStage.setY(owner.getY() + (owner.getHeight() - helpStage.getHeight()) / 2.0);
            });
            helpStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("操作帮助", "无法打开操作帮助界面，请联系技术人员。\n错误信息：" + e.getMessage());
        }
    }

    @FXML
    private void onAbout() {
        FxDialog.alert(mainTabPane.getScene().getWindow(), "关于系统",
                com.miduo.cloud.common.config.AppVersion.readAboutText());
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
        final ProductItem originalValue = productComboBox != null ? productComboBox.getValue() : null;
        final String originalProductCode = productCodeLabel != null ? productCodeLabel.getText() : "";
        final boolean originalCodeRowVisible = productCodeRow != null && productCodeRow.isVisible();
        final boolean originalCodeRowManaged = productCodeRow != null && productCodeRow.isManaged();

        CompletableFuture<Boolean> syncFuture = ShiwanM2FrontendApplication.getStartupProductSyncFuture();
        if (syncFuture == null || syncFuture.isDone()) {
            doOpenProductSelectDialog(originalValue, originalProductCode, originalCodeRowVisible, originalCodeRowManaged);
            return;
        }

        // 启动前异步同步尚未完成：显示加载提示，完成后再打开产品选择弹窗。
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.initStyle(javafx.stage.StageStyle.UNDECORATED);

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(48, 48);
        Label loadingLabel = new Label("正在同步产品数据，请稍候...");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-font-family: 'Microsoft YaHei';");

        VBox loadingBox = new VBox(12);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(24));
        loadingBox.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1;");
        loadingBox.getChildren().addAll(spinner, loadingLabel);

        loadingStage.setScene(new Scene(loadingBox, 260, 120));
        loadingStage.show();

        syncFuture.whenComplete((ok, ex) -> Platform.runLater(() -> {
            if (loadingStage.isShowing()) {
                loadingStage.close();
            }
            doOpenProductSelectDialog(originalValue, originalProductCode, originalCodeRowVisible, originalCodeRowManaged);
        }));
    }

    private void doOpenProductSelectDialog(ProductItem originalValue,
                                           String originalProductCode,
                                           boolean originalCodeRowVisible,
                                           boolean originalCodeRowManaged) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/fxml/ShiwanM2ProductSelectDialog.fxml");
            if (fxmlUrl == null) return;
            FXMLLoader loader = new FXMLLoader(fxmlUrl); // 必须传 URL，否则 FXML 中的 @css 相对路径无法解析
            Parent root = loader.load();
            ShiwanM2ProductSelectDialogController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
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
            } else {
                // 弹窗取消：恢复弹窗打开前的产品显示。
                // fetchProducts 刷新列表时会用新对象替换旧引用（setAll），
                // 必须从当前列表中找到同编号的新对象来恢复，否则 setValue 设的是不在列表中
                // 的旧引用，JavaFX 按钮单元格不刷新，导致下拉框显示空白。
                if (productComboBox != null) {
                    ProductItem restoreItem = originalValue;
                    if (originalValue != null
                            && originalValue.getNo() != null
                            && !originalValue.getNo().isEmpty()) {
                        final String targetNo = originalValue.getNo();
                        restoreItem = productItems.stream()
                                .filter(p -> targetNo.equals(p.getNo()))
                                .findFirst()
                                .orElse(originalValue);
                    }
                    // setValue(null) → setValue(target) 强制按钮单元格重新渲染
                    final ProductItem finalRestore = restoreItem;
                    productComboBox.setValue(null);
                    productComboBox.setValue(finalRestore);
                }
                if (productCodeLabel != null) {
                    productCodeLabel.setText(originalProductCode != null ? originalProductCode : "");
                }
                if (productCodeRow != null) {
                    productCodeRow.setVisible(originalCodeRowVisible);
                    productCodeRow.setManaged(originalCodeRowManaged);
                }
            }
        } catch (IOException e) {
            showWarn("打开产品选择失败", e.getMessage());
        }
    }

    /** 标志：是否正在恢复上次未完成任务，开始采集后需调用 restore-queue */
    private volatile String pendingRestoreOrderNo = null;

    /** 启动时检查是否存在未成垛数据，若有则静默自动填入界面（不弹窗提示） */
    private void checkUnfinishedOnStartup() {
        HttpUtil.asyncGet("/api/shiwan-m2/current-task/unfinished", json -> {
            try {
                JsonNode root = JSON.readTree(json);
                if (root == null || root.get("code").asInt() != 200 || !root.has("data")) return;
                JsonNode list = root.get("data");
                if (list == null || !list.isArray() || list.size() == 0) return;
                JsonNode first = list.get(0);
                String orderNo      = getTextNode(first, "OrderNo", "orderNo");
                String productNo    = getTextNode(first, "productNo", "ProductNO");
                String productName  = getTextNode(first, "productName", "ProductName");
                long   caseCount    = getLongNode(first, "currentCaseCount", "currentcasecount");
                long   boxCount     = getLongNode(first, "pendingBoxCount",  "pendingboxcount");
                if (orderNo.isEmpty()) return;

                Platform.runLater(() -> {
                    // 静默回填界面字段，不弹窗
                    orderNumField.setText(orderNo);
                    currentCases = (int) caseCount;
                    curCasesLabel.setText(String.valueOf(currentCases));
                    currentBoxes = (int) boxCount;
                    curBoxesLabel.setText(String.valueOf(currentBoxes));

                    // 匹配产品（优先按编号匹配列表，否则用接口返回的名称构造）
                    if (!productNo.isEmpty()) {
                        boolean matched = false;
                        for (ProductItem item : productItems) {
                            if (productNo.equals(item.getNo())) {
                                suppressProductSearch = true;
                                productComboBox.setValue(item);
                                productCodeLabel.setText(item.getNo());
                                productCodeRow.setVisible(true);
                                productCodeRow.setManaged(true);
                                suppressProductSearch = false;
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            String displayName = productName.isEmpty() ? productNo : productName;
                            ProductItem fallback = new ProductItem(displayName, productNo, "");
                            suppressProductSearch = true;
                            productItems.add(0, fallback);
                            productComboBox.setValue(fallback);
                            productCodeLabel.setText(productNo);
                            productCodeRow.setVisible(true);
                            productCodeRow.setManaged(true);
                            suppressProductSearch = false;
                        }
                    }

                    pendingRestoreOrderNo = orderNo;

                    String logTime = LocalDateTime.now().format(TIME_FMT);
                    String productDisplay = productName.isEmpty()
                            ? (productNo.isEmpty() ? "" : "（" + productNo + "）")
                            : productName + "（" + productNo + "）";
                    addOpLog(logTime + "  检测到未成垛数据已自动恢复：订单=" + orderNo
                            + productDisplay + "，已有" + caseCount + "箱，点击「开始采集」继续", LogType.INFO);
                });
            } catch (Exception ignored) {}
        }, null);
    }

    private static String getTextNode(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) return node.get(key).asText("");
        }
        return "";
    }

    private static long getLongNode(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) return node.get(key).asLong(0);
        }
        return 0;
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
        if (!orderNo.matches("[a-zA-Z0-9]{4,16}")) {
            showWarn("生产单号格式错误", "生产单号只能包含字母和数字，长度 4-16 位。");
            return;
        }
        final String productNo = productItem != null ? productItem.getNo()
                : (productCodeLabel.getText() != null ? productCodeLabel.getText().trim() : "");
        if (productNo.isEmpty()) {
            showWarn("产品编号为空", "请通过「选择」按钮重新选择产品以带出产品编号。");
            return;
        }

        // === 规格校验与解析（FX 线程，无 I/O）===
        String msText = casesPerPalletField.getText();
        String nsText = boxesPerCaseField.getText();
        if (msText == null || msText.trim().isEmpty()) {
            showWarn("采集规格错误", "请填写「每垛箱数 M」（1垛M箱），不能为空。");
            return;
        }
        if (nsText == null || nsText.trim().isEmpty()) {
            showWarn("采集规格错误", "请填写「每箱盒数 N」（1箱N盒），不能为空。");
            return;
        }
        int m, n;
        try { m = Integer.parseInt(msText.trim()); } catch (NumberFormatException e) {
            showWarn("采集规格错误", "每垛箱数 M 必须是数字。"); return;
        }
        try { n = Integer.parseInt(nsText.trim()); } catch (NumberFormatException e) {
            showWarn("采集规格错误", "每箱盒数 N 必须是数字。"); return;
        }
        if (m <= 0) { showWarn("采集规格错误", "每垛箱数 M 必须大于0。"); return; }
        if (n <= 0) { showWarn("采集规格错误", "每箱盒数 N 必须大于0。"); return; }

        // 采集规格持久化变更提示（FX 线程，Alert 弹窗）
        ShiwanM2Settings settings = ShiwanM2SettingsStore.get();
        int savedM = settings != null && settings.getBoxesPerPallet() != null ? settings.getBoxesPerPallet() : m;
        int savedN = settings != null && settings.getBoxesPerCase()   != null ? settings.getBoxesPerCase()   : n;
        if (m != savedM || n != savedN) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/ShiwanM2SpecChangeDialog.fxml"));
                Parent specRoot = loader.load();
                ShiwanM2SpecChangeDialogController specCtrl = loader.getController();
                specCtrl.setSpec(
                        "1垛 " + savedM + " 箱，1箱 " + savedN + " 盒",
                        "1垛 " + m + " 箱，1箱 " + n + " 盒");

                javafx.stage.Stage specStage = new javafx.stage.Stage();
                specStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
                specStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                specStage.setScene(new Scene(specRoot));
                specStage.setResizable(false);
                specStage.showAndWait();

                if (!specCtrl.isConfirmed()) {
                    m = savedM; n = savedN;
                    casesPerPalletField.setText(String.valueOf(m));
                    boxesPerCaseField.setText(String.valueOf(n));
                } else {
                    persistSpec(m, n);
                }
            } catch (Exception ex) {
                boolean specOk = FxDialog.confirm(
                        mainTabPane.getScene().getWindow(),
                        "采集规格变更确认",
                        "已保存：1垛 " + savedM + " 箱 / 1箱 " + savedN + " 盒\n"
                        + "当前：1垛 " + m + " 箱 / 1箱 " + n + " 盒\n\n是否覆盖保存为当前规格？"
                );
                if (!specOk) {
                    m = savedM; n = savedN;
                    casesPerPalletField.setText(String.valueOf(m));
                    boxesPerCaseField.setText(String.valueOf(n));
                } else {
                    persistSpec(m, n);
                }
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

        // 检查生产单号是否已存在于 ProductionOrder 表
        boolean orderExists = false;
        try {
            String existsJson = HttpUtil.doGet("/api/shiwan-m2/current-task/exists?orderNo="
                    + URLEncoder.encode(orderNo, StandardCharsets.UTF_8.name()));
            JsonNode existsNode = JSON.readTree(existsJson);
            if (existsNode != null && existsNode.has("data") && existsNode.get("data").asBoolean()) {
                orderExists = true;
            }
        } catch (Exception ignored) {}

        if (orderExists) {
            final String finalOrderNo = orderNo;
            Platform.runLater(() -> {
                boolean go = FxDialog.confirm(
                        mainTabPane.getScene().getWindow(),
                        "生产单号已存在",
                        "生产单号 [" + finalOrderNo + "] 已存在生产记录\n\n是否继续使用？",
                        "继 续"
                );
                if (go) {
                    showConfirmAndStartCapture(orderNo, productNo, product, m, n);
                } else {
                    resetStartCaptureBtn();
                }
            });
        } else {
            // 全部通过 → 回到 FX 线程显示确认弹窗
            Platform.runLater(() -> showConfirmAndStartCapture(orderNo, productNo, product, m, n));
        }
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
            log.warn("[开始采集-设备校验] 组装设备连接状态失败: {}", e.getMessage());
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2StartCaptureConfirmDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2StartCaptureConfirmDialogController dialogCtrl = loader.getController();
            dialogCtrl.setInfo(orderNo, productNo, product, m, n);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (!dialogCtrl.isConfirmed()) return;
        } catch (Exception e) {
            e.printStackTrace();
            showWarn("弹窗错误", "无法打开开始采集确认弹窗：" + e.getMessage());
            return;
        }

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
        // 保存当前采集参数，供"无需采集码"关闭时重启TCP采集使用
        this.currentOrderNo = orderNo;
        this.currentProductNo = productNo;
        this.currentBoxesPerCase = n;
        this.currentBoxesPerPallet = m;
        startCaptureBtn.setDisable(false);
        startCaptureBtn.setText("停止采集");
        startCaptureBtn.getStyleClass().add("running");
        casesPerPalletField.setEditable(false);
        boxesPerCaseField.setEditable(false);
        productComboBox.setDisable(true);
        orderNumField.setEditable(false);
        // 采集进行中：提取工单未成垛 置灰不可点击；强制满垛 和 收回剔除 恢复可用
        extractUnfinishedBtn.setDisable(true);
        forcePalletBtn.setDisable(false);
        rejectToggleBtn.setDisable(false);

        runningStatusBox.setVisible(true);
        runningStatusBox.setManaged(true);
        // 启动脉冲动画（单 Timeline 精确控制：雷达扩散环 + 内点心跳弹跳）
        if (runningPulseAnim == null) {
            Timeline t = new Timeline(
                // t=0ms：环归位（小 + 可见），点归位（正常大小）
                new KeyFrame(javafx.util.Duration.ZERO,
                    new KeyValue(runningPulseRing.scaleXProperty(), 0.3),
                    new KeyValue(runningPulseRing.scaleYProperty(), 0.3),
                    new KeyValue(runningPulseRing.opacityProperty(), 0.85),
                    new KeyValue(runningDot.scaleXProperty(), 1.0),
                    new KeyValue(runningDot.scaleYProperty(), 1.0)
                ),
                // t=130ms：点轻微弹跳（模拟心跳）
                new KeyFrame(javafx.util.Duration.millis(130),
                    new KeyValue(runningDot.scaleXProperty(), 1.35, Interpolator.EASE_OUT),
                    new KeyValue(runningDot.scaleYProperty(), 1.35, Interpolator.EASE_OUT)
                ),
                // t=380ms：点回弹恢复正常，环已扩散到中途
                new KeyFrame(javafx.util.Duration.millis(380),
                    new KeyValue(runningDot.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                    new KeyValue(runningDot.scaleYProperty(), 1.0, Interpolator.EASE_IN)
                ),
                // t=1100ms：环完全扩散（2.5x）并完全消隐
                new KeyFrame(javafx.util.Duration.millis(1100),
                    new KeyValue(runningPulseRing.scaleXProperty(), 2.5, Interpolator.EASE_OUT),
                    new KeyValue(runningPulseRing.scaleYProperty(), 2.5, Interpolator.EASE_OUT),
                    new KeyValue(runningPulseRing.opacityProperty(), 0.0, Interpolator.EASE_IN)
                ),
                // t=1800ms：停顿等待，保持透明（周期结束后 INDEFINITE 重置到 t=0 开启下一轮）
                new KeyFrame(javafx.util.Duration.millis(1800),
                    new KeyValue(runningPulseRing.opacityProperty(), 0.0)
                )
            );
            t.setCycleCount(Animation.INDEFINITE);
            runningPulseAnim = t;
        }
        runningPulseAnim.play();

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
        // 非恢复模式：重置计数
        if (pendingRestoreOrderNo == null || !pendingRestoreOrderNo.equals(orderNo)) {
            currentCases = 0;
            currentBoxes = 0;
            boxRecvCountSet = false;
            curCasesLabel.setText("0");
            curBoxesLabel.setText("0");
        }
        // 注意：不在此处调用 refreshRejectCount，避免覆盖用户手动清零的效果

        // 先启动 1号机 T_Code 轮询同步（后台线程，不阻塞 UI）
        startM1Sync();
        // 启动 TCP 相机采集（n=每箱盒数, m=每垛箱数）
        startTcpCapture(orderNo, productNo, n, m);
        // 最后连接前端托管设备（不含相机，避免与后端 TCP 采集服务形成双连接）
        connectAllDevices(false, false);
        // 开始采集时：按数据库实时箱数检查是否已达到成垛阈值，达到则直接强制满垛（并触发上传流程）
        checkAndForceFullPalletOnStart(orderNo, m);
        // 如果是恢复上次未完成任务，启动TCP后从数据库恢复待关联盒码到内存队列
        if (pendingRestoreOrderNo != null && pendingRestoreOrderNo.equals(orderNo)) {
            final String restoreOrderNo = pendingRestoreOrderNo;
            pendingRestoreOrderNo = null;
            productExecutor.submit(() -> {
                try {
                    Thread.sleep(1000); // 等TCP采集服务完全启动
                    String body = "{\"orderNo\":\"" + escapeJson(restoreOrderNo) + "\"}";
                    String resp = HttpUtil.doPost("/api/shiwan-m2/capture/restore-queue", body);
                    JsonNode node = JSON.readTree(resp);
                    if (node != null && node.has("code") && node.get("code").asInt() == 200) {
                        int restored = node.has("data") && node.get("data").has("restoredCount")
                                ? node.get("data").get("restoredCount").asInt() : 0;
                        if (restored > 0) {
                            Platform.runLater(() -> addOpLog(
                                    LocalDateTime.now().format(TIME_FMT) + "  已从数据库恢复 " + restored + " 个盒码到采集队列", LogType.INFO));
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    /**
     * 开始采集后按数据库实时箱数检查：若当前未成垛箱数已达到每垛箱数阈值，则直接强制满垛。
     * 场景：提取工单未成垛后，已有箱数已满足成垛标准。
     */
    private void checkAndForceFullPalletOnStart(String orderNo, int boxesPerPallet) {
        if (orderNo == null || orderNo.trim().isEmpty() || boxesPerPallet <= 0) return;
        HttpUtil.asyncGet("/api/shiwan-m2/box-case/current-cases?orderNo="
                        + java.net.URLEncoder.encode(orderNo.trim(), java.nio.charset.StandardCharsets.UTF_8),
                json -> {
                    try {
                        JsonNode node = JSON.readTree(json);
                        if (node == null || !node.has("code") || node.get("code").asInt() != 200 || !node.has("data")) {
                            return;
                        }
                        int realCases = node.get("data").asInt(0);
                        currentCases = realCases;
                        curCasesLabel.setText(String.valueOf(realCases));
                        if (realCases >= boxesPerPallet) {
                            addOpLog(LocalDateTime.now().format(TIME_FMT)
                                    + "  启动检测到未成垛箱数已达阈值（" + realCases + "/" + boxesPerPallet + "），自动执行满垛", LogType.INFO);
                            forceFullPalletBackend(orderNo, realCases, false);
                        }
                    } catch (Exception ignored) {}
                },
                ignored -> {});
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
        // 不重置事件游标，避免停止后再次开始时重复消费历史事件（导致日志重复和统计回涨）。
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
     * 开始采集时在后台线程连接所有已启用的IO设备（网口/串口）。
     * 先尝试连接所有设备，等待片刻后再用 isConnected 统计实际连接数，最后打印结果日志。
     */
    private void connectAllDevices(boolean logDeviceDetails, boolean includeCameraDevices) {
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doGet("/api/device/list");
                ApiResult<List<IoDeviceDTO>> result = HttpUtil.parseJson(
                        resp, new TypeReference<ApiResult<List<IoDeviceDTO>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    Platform.runLater(() -> {
                        addOpLog(
                                LocalDateTime.now().format(TIME_FMT) + "  获取设备列表失败，跳过IO设备连接",
                                LogType.WARN);
                        updateDeviceStatusBar();
                    });
                    return;
                }
                // 收集需要连接的设备（是否包含相机由调用方控制）
                List<IoDeviceDTO> targetDevices = new ArrayList<>();
                for (IoDeviceDTO device : result.getData()) {
                    if (!Boolean.TRUE.equals(device.getEnabled())) continue;
                    if (!includeCameraDevices && isCameraCaptureDevice(device)) {
                        continue;
                    }
                    targetDevices.add(device);
                }
                // 逐个发起连接
                for (IoDeviceDTO device : targetDevices) {
                    try {
                        // 已连接设备保持现状，避免重复重连导致短暂抖动。
                        if (DeviceConnectionManager.getInstance().isConnected(device.getId())) {
                            continue;
                        }
                        DeviceConnectionManager.getInstance().startConnection(device);
                    } catch (Exception e) {
                        final String errMsg = device.getDeviceName() + " 连接失败：" + e.getMessage();
                        Platform.runLater(() -> addOpLog(
                                LocalDateTime.now().format(TIME_FMT) + "  [设备] " + errMsg, LogType.WARN));
                    }
                }
                // 轮询等待所有设备连接完成，每 500ms 检查一次，最多等待 8 秒
                if (!targetDevices.isEmpty()) {
                    long deadline = System.currentTimeMillis() + 8000;
                    while (System.currentTimeMillis() < deadline) {
                        try { Thread.sleep(500); } catch (InterruptedException ignored) { break; }
                        long connected = targetDevices.stream()
                                .filter(d -> DeviceConnectionManager.getInstance().isConnected(d.getId()))
                                .count();
                        if (connected >= targetDevices.size()) break;
                    }
                }
                int connectedCount = 0;
                for (IoDeviceDTO device : targetDevices) {
                    if (DeviceConnectionManager.getInstance().isConnected(device.getId())) {
                        connectedCount++;
                    }
                }
                if (logDeviceDetails) {
                    for (IoDeviceDTO device : targetDevices) {
                        boolean connected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                        String connType = device.getConnectionType() == null ? "" : " (" + device.getConnectionType() + ")";
                        String msg = LocalDateTime.now().format(TIME_FMT) + "  设备连接"
                                + (connected ? "成功: " : "失败: ")
                                + device.getDeviceName() + connType;
                        LogType type = connected ? LogType.SUCCESS : LogType.WARN;
                        Platform.runLater(() -> addOpLog(msg, type));
                    }
                }
                final int cnt = connectedCount;
                final int total = targetDevices.size();
                Platform.runLater(() -> {
                    addOpLog(
                            LocalDateTime.now().format(TIME_FMT) + "  设备初始化完成，已连接 " + cnt + " / " + total + " 台",
                            LogType.INFO);
                    updateDeviceStatusBar();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addOpLog(
                            LocalDateTime.now().format(TIME_FMT) + "  IO设备连接异常：" + e.getMessage(), LogType.WARN);
                    updateDeviceStatusBar();
                });
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

    /**
     * 仅停止相机端 TCP 采集（HTTP），不停事件轮询 Timeline。
     * 用于"无需采集码"模式：Timeline 继续运行，consumeEventSeqOnly 持续推进 seq，
     * 后端旧事件被静默消化，关闭模式后不会有积压事件涌入。
     */
    private void stopCameraOnly() {
        productExecutor.submit(() -> {
            try { HttpUtil.doPost("/api/shiwan-m2/capture/stop", ""); }
            catch (Exception ignored) {}
        });
    }

    /**
     * 仅重启相机端 TCP 采集（HTTP），不重置 lastCaptureEventSeq，不重启 Timeline。
     * 用于关闭"无需采集码"模式：Timeline 已在运行，seq 已与后端保持同步，直接恢复即可。
     */
    private void restartCameraOnly(String orderNo, String productNo, int boxesPerCase, int boxesPerPallet) {
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
                    logMsg = LocalDateTime.now().format(TIME_FMT) + "  TCP相机采集已恢复";
                    logType = LogType.INFO;
                } else {
                    String errMsg = node != null && node.has("message") ? node.get("message").asText() : "未知错误";
                    logMsg = LocalDateTime.now().format(TIME_FMT) + "  TCP相机采集恢复失败：" + errMsg;
                    logType = LogType.WARN;
                }
            } catch (Exception e) {
                logMsg = LocalDateTime.now().format(TIME_FMT) + "  TCP相机采集恢复异常：" + e.getMessage();
                logType = LogType.WARN;
            }
            final String finalMsg = logMsg;
            final LogType finalType = logType;
            Platform.runLater(() -> addOpLog(finalMsg, finalType));
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
        // 捕获当前"无需采集码"状态快照，供后台线程安全读取
        final boolean skipProcessing = noCodeNeededMode;
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doGet("/api/shiwan-m2/capture/events?lastSeq=" + seqSnapshot);
                JsonNode root = JSON.readTree(resp);
                if (root == null || !root.has("code") || root.get("code").asInt() != 200) return;
                JsonNode dataArr = root.get("data");
                if (dataArr == null || !dataArr.isArray() || dataArr.size() == 0) return;
                if (skipProcessing) {
                    // 无需采集码模式：仍消费事件序号，防止关闭模式后积压事件涌入，但不做任何业务处理
                    Platform.runLater(() -> consumeEventSeqOnly(dataArr));
                } else {
                    Platform.runLater(() -> processCaptureEventsData(dataArr));
                }
            } catch (Exception ignored) {
            } finally {
                processingCaptureEvents.set(false);
            }
        });
    }

    /**
     * 无需采集码模式下：只推进事件序号游标，不处理任何事件内容。
     */
    private void consumeEventSeqOnly(JsonNode dataArr) {
        for (JsonNode evt : dataArr) {
            long seq = evt.has("seq") ? evt.get("seq").asLong() : 0;
            if (seq > lastCaptureEventSeq) lastCaptureEventSeq = seq;
        }
    }

    /**
     * 在 FX 线程上处理从后端拉取的采集事件，更新数据日志和 UI 计数。
     */
    private void processCaptureEventsData(JsonNode dataArr) {
        int bpc = parseSpecValue(boxesPerCaseField.getText(), 4);
        int cpp = parseSpecValue(casesPerPalletField.getText(), 70);
        String orderNo = orderNumField.getText() != null ? orderNumField.getText().trim() : "";

        List<JsonNode> events = new ArrayList<>();
        dataArr.forEach(events::add);
        // 数据接收区按毫秒时间排序；同毫秒再按 seq 排序，保证“最新在上”稳定。
        events.sort((a, b) -> {
            long ta = eventTimeMs(a);
            long tb = eventTimeMs(b);
            if (ta != tb) return Long.compare(ta, tb);
            long sa = a.has("seq") ? a.get("seq").asLong() : 0L;
            long sb = b.has("seq") ? b.get("seq").asLong() : 0L;
            return Long.compare(sa, sb);
        });

        for (JsonNode evt : events) {
            long   seq  = evt.has("seq")     ? evt.get("seq").asLong()   : 0;
            String type = evt.has("type")    ? evt.get("type").asText()  : "";
            String msg  = evt.has("message") ? evt.get("message").asText(): "";
            String time = evt.has("time")    ? evt.get("time").asText()  : LocalDateTime.now().format(TIME_FMT);
            JsonNode evtData = evt.get("data");

            if (seq > lastCaptureEventSeq) lastCaptureEventSeq = seq;

            switch (type) {
                case "BOX_RECV":
                    addDataLog(time + "  " + msg, LogType.DATA);
                    // 统计：当前盒数 = 盒相机本批去重后的盒码数量
                    int dedupCount = parseBoxRecvDedupCount(msg);
                    if (dedupCount >= 0) {
                        currentBoxes = dedupCount;
                        curBoxesLabel.setText(String.valueOf(currentBoxes));
                        boxRecvCountSet = true;
                    }
                    break;

                case "CASE_RECV":
                    addDataLog(time + "  " + msg, LogType.DATA);
                    break;

                case "BOX_CODE":
                    // 兼容其它采集模式：仅当 BOX_RECV 尚未设置时才累加
                    if (!boxRecvCountSet) {
                        currentBoxes++;
                        curBoxesLabel.setText(String.valueOf(currentBoxes));
                    }
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
                    boxRecvCountSet = false;
                    currentCases = cases;
                    curBoxesLabel.setText("0");
                    curCasesLabel.setText(String.valueOf(currentCases));

                    if (fullPallet) {
                        palletCount++;
                        palletCountLabel.setText(String.valueOf(palletCount));
                        String pc = palletCode != null ? palletCode : ("P" + orderNo + String.format("%03d", palletCount));
                        boolean autoUploadOn = ShiwanM2SettingsStore.get().getUpload() == null
                                || ShiwanM2SettingsStore.get().getUpload().isAutoUpload();
                        String uploadSuffix = autoUploadOn ? "，正在上传..." : "";
                        addDataLog(time + "  满垛完成 - 垛码：" + pc + " 共 " + cpp + " 箱" + uploadSuffix, LogType.SUCCESS);
                        addOpLog(time + "  满垛完成，已生产垛数：" + palletCount, LogType.INFO);
                        OperateLogBuilder.create()
                                .module(ModuleNameEnum.PALLET_MANAGE)
                                .operateType(OperateTypeEnum.COMPLETE)
                                .target(pc, orderNo)
                                .content("满垛完成 - 垛码：" + pc + " 共 " + cpp + " 箱，生产单：" + orderNo
                                        + "，已生产垛数：" + palletCount)
                                .saveAsync();
                        addPendingUploadRowAtTop(pc, cpp + "箱");
                        currentCases = 0;
                        curCasesLabel.setText("0");
                    }
                    break;
                }

                case "ASSOC_FAIL": {
                    String reason = evtData != null && evtData.has("reason") ? evtData.get("reason").asText() : msg;
                    addAlarmLog(time + "  关联失败：" + reason, LogType.ERROR);
                    addDataLog(time + "  " + msg, LogType.ERROR);
                    OperateLogBuilder.create()
                            .module(ModuleNameEnum.BOX_CASE_ASSOCIATE)
                            .operateType(OperateTypeEnum.ASSOCIATE)
                            .target("", orderNo)
                            .content("盒箱关联失败，生产单：" + orderNo)
                            .failReason(reason)
                            .saveAsync();
                    // 盒箱关联失败的盒码已写入剔除记录，从当前盒数中减去（不计入计数）
                    if (evtData != null && evtData.has("boxCodes") && evtData.get("boxCodes").isArray()) {
                        // 整批剔除：本批去重后的盒码不再待关联，清零显示
                        currentBoxes = 0;
                        boxRecvCountSet = false;
                        curBoxesLabel.setText("0");
                    }
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

    /**
     * 事件排序时间（毫秒）。优先使用后端 timeMs，缺失时回退到 seq，保证老版本数据也可稳定排序。
     */
    private static long eventTimeMs(JsonNode evt) {
        if (evt != null && evt.has("timeMs")) {
            return evt.get("timeMs").asLong(0L);
        }
        return evt != null && evt.has("seq") ? evt.get("seq").asLong(0L) : 0L;
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

    /**
     * 解析 BOX_RECV 消息中的“本批去重后盒码数量”。
     * 当前后端拼接格式示例：
     *   [盒码相机] 批1 接收到 4 个盒码：[...]
     */
    private static int parseBoxRecvDedupCount(String msg) {
        if (msg == null || msg.isEmpty()) return -1;
        // 取 “接收到 X 个盒码” 里的 X
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("接收到\\s*(\\d+)\\s*个盒码")
                .matcher(msg);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {
                return -1;
            }
        }
        return -1;
    }

    /** 通知后端启动 1 号机 T_Code 同步（后台线程执行，不阻塞 UI） */
    private void startM1Sync() {
        lastM1SyncEventSeq = 0;
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doPost("/api/shiwan-m2/m1-sync/start", "");
                JsonNode n = JSON.readTree(resp);
                if (n != null && n.has("code") && n.get("code").asInt() == 200) {
                    Platform.runLater(() -> {
                        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  1号机数据同步已启动", LogType.INFO);
                        // 启动 M1 同步结果轮询
                        if (m1SyncEventsTimeline != null) m1SyncEventsTimeline.stop();
                        m1SyncEventsTimeline = new Timeline(new KeyFrame(Duration.seconds(4),
                                ev -> scheduleM1SyncEventsRefresh()));
                        m1SyncEventsTimeline.setCycleCount(Animation.INDEFINITE);
                        m1SyncEventsTimeline.play();
                    });
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
        if (m1SyncEventsTimeline != null) {
            m1SyncEventsTimeline.stop();
            m1SyncEventsTimeline = null;
        }
        productExecutor.submit(() -> {
            try {
                HttpUtil.doPost("/api/shiwan-m2/m1-sync/stop", "");
            } catch (Exception ignored) {}
        });
    }

    /** 每 4 秒触发一次 M1 同步事件拉取（FX 线程调度，HTTP 在后台线程） */
    private void scheduleM1SyncEventsRefresh() {
        if (!processingM1SyncEvents.compareAndSet(false, true)) return;
        final long seqSnapshot = lastM1SyncEventSeq;
        productExecutor.submit(() -> {
            try {
                String resp = HttpUtil.doGet("/api/shiwan-m2/m1-sync/events?lastSeq=" + seqSnapshot);
                JsonNode root = JSON.readTree(resp);
                if (root == null || !root.has("code") || root.get("code").asInt() != 200) return;
                JsonNode data = root.get("data");
                if (data == null || !data.isArray() || data.size() == 0) return;
                long maxSeq = seqSnapshot;
                List<String[]> msgs = new ArrayList<>();
                for (JsonNode evt : data) {
                    long seq = evt.has("seq") ? evt.get("seq").asLong() : 0;
                    if (seq > maxSeq) maxSeq = seq;
                    String type = evt.has("type") ? evt.get("type").asText() : "";
                    String msg = evt.has("message") ? evt.get("message").asText() : "";
                    String time = evt.has("time") ? evt.get("time").asText() : LocalDateTime.now().format(TIME_FMT);
                    msgs.add(new String[]{time, type, msg});
                }
                final long finalSeq = maxSeq;
                Platform.runLater(() -> {
                    lastM1SyncEventSeq = finalSeq;
                    for (String[] m : msgs) {
                        LogType lt = "SYNC_ERROR".equals(m[1]) ? LogType.ERROR : LogType.SUCCESS;
                        addDataLog(m[0] + "  [1号机同步] " + m[2], lt);
                    }
                });
            } catch (Exception ignored) {
            } finally {
                processingM1SyncEvents.set(false);
            }
        });
    }

    /** 同步产品到本地（后台线程调用） */
    private void syncProducts() {
        try {
            String resp = HttpUtil.doPost("/api/shiwan-m2/products/sync", "");
            JsonNode n = JSON.readTree(resp);
            if (n == null || !n.has("code") || n.get("code").asInt() != 200) {
                log.warn("[产品同步] 失败：{}", (n != null && n.has("message") ? n.get("message").asText() : "未知错误"));
            }
        } catch (Exception e) {
            log.warn("[产品同步] 异常：{}", e.getMessage());
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
                // setAll 后 value 引用可能仍非 null 但已不在新列表中（fallback 对象），
                // 所以不能只判断 getValue()==null，需检查 value 是否真正在新列表中
                if (currentValue != null) {
                    String currentNo = currentValue.getNo();
                    ProductItem matched = null;
                    if (currentNo != null && !currentNo.isEmpty()) {
                        for (ProductItem item : list) {
                            if (currentNo.equals(item.getNo())) {
                                matched = item;
                                break;
                            }
                        }
                    }
                    if (matched != null) {
                        // 用新列表中的真实对象替换（避免 fallback 游离导致显示空白）
                        productComboBox.setValue(matched);
                        productCodeLabel.setText(matched.getNo());
                        productCodeRow.setVisible(true);
                        productCodeRow.setManaged(true);
                    } else if (productComboBox.getValue() == null) {
                        // 新列表中无匹配项且 value 已被清空，保留原 fallback 对象
                        productComboBox.setValue(currentValue);
                    }
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
        // 停止采集：提取工单未成垛 恢复可用；强制满垛 和 收回剔除 置灰（仅运行时有意义）
        extractUnfinishedBtn.setDisable(false);
        forcePalletBtn.setDisable(true);
        rejectToggleBtn.setDisable(true);
        // 停止采集时若"无需采集码"模式开启，重置其状态
        if (noCodeNeededMode) {
            noCodeNeededMode = false;
            noCodeNeededBtn.setText("无需采集码");
            noCodeNeededBtn.getStyleClass().removeAll("shiwan-m2-ctrl-btn-red");
            noCodeNeededBtn.getStyleClass().add("shiwan-m2-ctrl-btn-outline-blue");
        }

        runningStatusBox.setVisible(false);
        runningStatusBox.setManaged(false);
        if (runningPulseAnim != null) runningPulseAnim.stop();

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
        noCodeNeededMode = !noCodeNeededMode;
        if (noCodeNeededMode) {
            noCodeNeededBtn.setText("关闭无需采集码");
            noCodeNeededBtn.getStyleClass().removeAll("shiwan-m2-ctrl-btn-outline-blue");
            noCodeNeededBtn.getStyleClass().add("shiwan-m2-ctrl-btn-red");
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  无需采集码模式已开启", LogType.INFO);
            // 采集中：仅停止相机，不停 Timeline（seq 继续推进，后端旧事件静默消化）；同时停止1号机同步
            if (isRunning) {
                stopCameraOnly();
                stopM1Sync();
            }
        } else {
            noCodeNeededBtn.setText("无需采集码");
            noCodeNeededBtn.getStyleClass().removeAll("shiwan-m2-ctrl-btn-red");
            noCodeNeededBtn.getStyleClass().add("shiwan-m2-ctrl-btn-outline-blue");
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  无需采集码模式已关闭", LogType.INFO);
            // 采集中：仅重启相机（不重置 seq、不重启 Timeline），同时恢复1号机同步
            if (isRunning) {
                restartCameraOnly(currentOrderNo, currentProductNo, currentBoxesPerCase, currentBoxesPerPallet);
                startM1Sync();
            }
        }
    }

    /** 保留供内部调用（自动关闭报警，如业务触发的关闭）。 */
    private void closeAlarmInternal() {
        ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
        hw.allLightsOff();
        String now = LocalDateTime.now().format(TIME_FMT);
        addOpLog(now + "  [报警器] 已发送关闭报警指令（内部）", LogType.INFO);
    }

    /**
     * 关闭报警按钮：每次点击均发送关闭报警信号，按钮文字不变。
     */
    @FXML
    private void onToggleAlarm() {
        ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
        hw.closeAlarm();
        alarmOpen = false;
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [报警器] 已发送关闭报警信号", LogType.INFO);
    }

    /**
     * 收回剔除按钮：每次点击均发送收回剔除信号，按钮文字不变。
     */
    @FXML
    private void onToggleReject() {
        ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
        hw.retractRejectCustom();
        rejectTriggered = false;
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [剔除] 已发送收回剔除信号", LogType.INFO);
    }

    /**
     * 测试报警灯亮/灭按钮：
     * 初始文字"测试报警灯亮"，点击发送报警灯亮信号，按钮变为"测试报警灯灭"；
     * 再次点击发送报警灯灭信号，按钮恢复为"测试报警灯亮"。
     */
    @FXML
    private void onToggleAlarmLight() {
        ShiwanM2HardwareService hw = ShiwanM2HardwareService.getInstance();
        String now = LocalDateTime.now().format(TIME_FMT);
        if (!alarmLightOn) {
            hw.alarmLightOn();
            alarmLightOn = true;
            if (alarmLightTestBtn != null) alarmLightTestBtn.setText("测试报警灯灭");
            addOpLog(now + "  [报警灯] 已发送报警灯亮信号", LogType.INFO);
        } else {
            hw.alarmLightOff();
            alarmLightOn = false;
            if (alarmLightTestBtn != null) alarmLightTestBtn.setText("测试报警灯亮");
            addOpLog(now + "  [报警灯] 已发送报警灯灭信号", LogType.INFO);
        }
    }

    @FXML
    private void onForcePallet() {
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [操作] 点击强制满垛", LogType.INFO);
        if (currentCases == 0) {
            showWarn("强制满垛", "当前没有待处理的箱数，无需强制满垛。");
            return;
        }
        boolean ok = FxDialog.confirm(
                mainTabPane.getScene().getWindow(),
                "强制满垛确认",
                "当前箱数：" + currentCases + " 箱\n\n强制满垛将生成虚拟垛标并重置计数，是否继续？"
        );
        if (ok) {
            forceFullPalletBackend(orderNumField.getText(), currentCases);
        }
    }

    private void forceFullPalletBackend(String orderNo, int currentCaseCount) {
        forceFullPalletBackend(orderNo, currentCaseCount, true);
    }

    /**
     * 调用后端满垛接口。
     * @param showAsForce true=按“强制满垛”文案展示，false=按“满垛”文案展示（自动达标场景）
     */
    private void forceFullPalletBackend(String orderNo, int currentCaseCount, boolean showAsForce) {
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
                    // 强制满垛失败时，从数据库刷新真实箱数，防止前端计数与实际不符
                    refreshCurrentCasesAsync(orderNo);
                    showWarn("强制满垛失败", errMsg);
                    return;
                }
                JsonNode data = root.get("data");
                String palletCode = data != null && data.has("palletCode") ? data.get("palletCode").asText() : null;
                if (palletCode == null) {
                    fetchNextVirtualSerialNumberAsync(code -> finishForcePallet(code, currentCaseCount, showAsForce));
                } else {
                    finishForcePallet(palletCode, currentCaseCount, showAsForce);
                }
            } catch (Exception e) {
                showWarn("强制满垛失败", e.getMessage());
            }
        }, e -> showWarn("强制满垛失败", e.getMessage()));
    }

    /** 强制满垛后端成功后更新 UI（FX 线程回调） */
    private void finishForcePallet(String palletCode, int caseCount, boolean showAsForce) {
        palletCount++;
        palletCountLabel.setText(String.valueOf(palletCount));
        String now = LocalDateTime.now().format(TIME_FMT);
        boolean autoUploadOn = ShiwanM2SettingsStore.get().getUpload() == null
                || ShiwanM2SettingsStore.get().getUpload().isAutoUpload();
        String uploadSuffix = autoUploadOn ? "，正在上传..." : "";
        String actionText = showAsForce ? "强制满垛" : "满垛";
        addDataLog(now + "  " + actionText + " - 垛码：" + palletCode + " 已生成，共" + caseCount + "箱" + uploadSuffix, LogType.SUCCESS);
        addOpLog(now + "  强制满垛操作完成，已生产垛数：" + palletCount, LogType.INFO);
        final String forceOrderNo = orderNumField.getText();
        OperateLogBuilder.create()
                .module(ModuleNameEnum.PALLET_MANAGE)
                .operateType(OperateTypeEnum.FORCE)
                .target(palletCode, forceOrderNo)
                .content("强制满垛 - 垛码：" + palletCode + " 已生成，共" + caseCount + "箱，生产单：" + forceOrderNo
                        + "，已生产垛数：" + palletCount)
                .saveAsync();
        addPendingUploadRowAtTop(palletCode, caseCount + "箱");
        currentCases = 0;
        currentBoxes = 0;
        boxRecvCountSet = false;
        curCasesLabel.setText("0");
        curBoxesLabel.setText("0");
    }

    /**
     * 从数据库异步刷新当前真实箱数，并同步更新前端计数与标签。
     * 用于强制满垛失败后修正可能因取消关联等操作导致的前端计数偏差。
     */
    private void refreshCurrentCasesAsync(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return;
        HttpUtil.asyncGet("/api/shiwan-m2/box-case/current-cases?orderNo="
                + java.net.URLEncoder.encode(orderNo.trim(), java.nio.charset.StandardCharsets.UTF_8),
                json -> {
                    try {
                        JsonNode node = JSON.readTree(json);
                        if (node != null && node.has("code") && node.get("code").asInt() == 200
                                && node.has("data") && !node.get("data").isNull()) {
                            int realCases = node.get("data").asInt(0);
                            Platform.runLater(() -> {
                                currentCases = realCases;
                                if (curCasesLabel != null) curCasesLabel.setText(String.valueOf(realCases));
                                addOpLog(LocalDateTime.now().format(TIME_FMT)
                                        + "  [刷新] 当前真实箱数已从数据库同步：" + realCases + " 箱", LogType.INFO);
                            });
                        }
                    } catch (Exception ignored) {}
                }, ignored -> {});
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

    /** 停止定时刷新统计 */
    private void stopStatsRefresh() {
        if (statsRefreshTimeline != null) {
            statsRefreshTimeline.stop();
            statsRefreshTimeline = null;
        }
    }

    @FXML
    private void onExtractUnfinished() {
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [操作] 点击提取工单未成垛", LogType.INFO);
        Stage dialog = new Stage();
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(orderNumField.getScene().getWindow());
        dialog.setResizable(false);

        // ── 说明文字 ─────────────────────────────────────────────
        Label descLbl = new Label("请输入或扫描未完成垛中的任意一个箱码，系统将查出对应的生产订单数据。");
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size:14px;-fx-text-fill:#6B7280;-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;-fx-padding:12 16;");

        // ── 箱码输入行 ───────────────────────────────────────────
        Label boxCodeLbl = new Label("箱码");
        boxCodeLbl.setStyle("-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:#374151;");
        TextField boxCodeInput = new TextField();
        boxCodeInput.setPromptText("输入或扫描未完成垛中的任意一个箱码");
        boxCodeInput.setPrefHeight(40);
        boxCodeInput.setStyle("-fx-font-size:15px;-fx-border-color:#D1D5DB;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:0 12;");
        Button queryBtn = new Button("查询");
        queryBtn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-font-size:15px;-fx-font-weight:600;-fx-border-width:0;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:0 20;-fx-min-height:40;");
        HBox inputRow = new HBox(8, boxCodeInput, queryBtn);
        HBox.setHgrow(boxCodeInput, Priority.ALWAYS);
        VBox boxCodeSection = new VBox(8, boxCodeLbl, inputRow);

        // ── 查询结果区（初始隐藏）────────────────────────────────
        Label resultHeader = new Label("查询结果");
        resultHeader.setMaxWidth(Double.MAX_VALUE);
        resultHeader.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:transparent transparent #E5E7EB transparent;-fx-border-width:0 0 1 0;-fx-padding:10 16;-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:#374151;-fx-font-family:'Microsoft YaHei';");
        VBox resultInfo = new VBox(12);
        resultInfo.setStyle("-fx-padding:16;");
        VBox resultArea = new VBox(resultHeader, resultInfo);
        resultArea.setStyle("-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-background-color:white;");
        resultArea.setVisible(false);
        resultArea.setManaged(false);

        // ── 未找到提示（初始隐藏）───────────────────────────────
        Label errorLbl = new Label("未找到对应的未成垛记录，请确认箱码是否正确。");
        errorLbl.setMaxWidth(Double.MAX_VALUE);
        errorLbl.setAlignment(Pos.CENTER);
        errorLbl.setStyle("-fx-text-fill:#EF4444;-fx-font-size:14px;-fx-padding:8 0;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        // ── 内容区 ───────────────────────────────────────────────
        VBox content = new VBox(16, descLbl, boxCodeSection, resultArea, errorLbl);
        content.setStyle("-fx-padding:24;");

        // ── 底部按钮 ─────────────────────────────────────────────
        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color:#F3F4F6;-fx-border-width:0;-fx-background-radius:8;-fx-font-size:16px;-fx-font-weight:600;-fx-text-fill:#374151;-fx-cursor:hand;-fx-min-height:40;-fx-min-width:90;");
        cancelBtn.setOnAction(e -> dialog.close());
        Button confirmBtn = new Button("确认");
        confirmBtn.setDisable(true);
        confirmBtn.setStyle("-fx-background-color:#D1D5DB;-fx-border-width:0;-fx-background-radius:8;-fx-font-size:16px;-fx-font-weight:600;-fx-text-fill:white;-fx-cursor:not-allowed;-fx-min-height:40;-fx-min-width:90;");
        HBox bottomBar = new HBox(12, cancelBtn, confirmBtn);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB transparent transparent transparent;-fx-border-width:1 0 0 0;-fx-padding:16 24;-fx-min-height:72;");

        // ── 查询结果暂存（供确认按钮使用）───────────────────────
        final String[] foundOrderNo        = {null};
        final String[] foundProductNo      = {null};
        final String[] foundProductName    = {null};
        final int[]    foundBoxCount       = {0};
        final int[]    foundPendingBoxes   = {0};
        final int[]    foundCasesPerPallet = {0};
        final int[]    foundBoxesPerCase   = {0};

        // ── 查询逻辑 ─────────────────────────────────────────────
        Runnable doQuery = () -> {
            String code = boxCodeInput.getText() != null ? boxCodeInput.getText().trim() : "";
            if (code.isEmpty()) return;
            resultArea.setVisible(false);
            resultArea.setManaged(false);
            errorLbl.setVisible(false);
            errorLbl.setManaged(false);
            confirmBtn.setDisable(true);
            confirmBtn.setStyle("-fx-background-color:#D1D5DB;-fx-border-width:0;-fx-background-radius:8;-fx-font-size:16px;-fx-font-weight:600;-fx-text-fill:white;-fx-cursor:not-allowed;-fx-min-height:40;-fx-min-width:90;");

            String url = "/api/shiwan-m2/current-task/unfinished-by-box-code?boxCode="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8);
            HttpUtil.asyncGet(url, json -> {
                try {
                    JsonNode root = JSON.readTree(json);
                    int respCode = root != null && root.has("code") ? root.get("code").asInt() : -1;
                    if (respCode == 200 && root.has("data") && !root.get("data").isNull()) {
                        JsonNode data = root.get("data");
                        String orderNo         = data.has("orderNo")         ? data.get("orderNo").asText("")         : "";
                        String productNo       = data.has("productNo")       ? data.get("productNo").asText("")       : "";
                        String productName     = data.has("productName")     ? data.get("productName").asText("")     : "";
                        int    boxCount        = data.has("currentCaseCount") ? data.get("currentCaseCount").asInt(0) : 0;
                        int    pendingBoxes    = data.has("pendingBoxCount")  ? data.get("pendingBoxCount").asInt(0)  : 0;
                        int    casesPerPallet  = data.has("casesPerPallet")  ? data.get("casesPerPallet").asInt(0)   : 0;
                        int    boxesPerCase    = data.has("boxesPerCase")    ? data.get("boxesPerCase").asInt(0)     : 0;
                        foundOrderNo[0]        = orderNo;
                        foundProductNo[0]      = productNo;
                        foundProductName[0]    = productName;
                        foundBoxCount[0]       = boxCount;
                        foundPendingBoxes[0]   = pendingBoxes;
                        foundCasesPerPallet[0] = casesPerPallet;
                        foundBoxesPerCase[0]   = boxesPerCase;
                        Platform.runLater(() -> {
                            // 生产信息 小节标题
                            Label prodInfoHeader = new Label("生产信息");
                            prodInfoHeader.setStyle("-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:#2563EB;-fx-font-family:'Microsoft YaHei';");

                            // 分隔线（生产信息与采集规格之间）
                            javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
                            divider.setMaxWidth(Double.MAX_VALUE);
                            divider.setPrefHeight(1);
                            divider.setMaxHeight(1);
                            divider.setStyle("-fx-background-color:#E5E7EB;");

                            // 采集规格 小节标题
                            Label specHeader = new Label("采集规格");
                            specHeader.setStyle("-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:#2563EB;-fx-font-family:'Microsoft YaHei';");

                            // 采集规格文案：1垛 m 箱，1箱 n 盒（数字加粗 16px）
                            String mStr = casesPerPallet > 0 ? String.valueOf(casesPerPallet) : "—";
                            String nStr = boxesPerCase   > 0 ? String.valueOf(boxesPerCase)   : "—";
                            Label s1 = new Label("1垛");
                            Label s2 = new Label(mStr);
                            Label s3 = new Label("箱，1箱");
                            Label s4 = new Label(nStr);
                            Label s5 = new Label("盒");
                            String normalSpec = "-fx-font-size:16px;-fx-font-weight:normal;-fx-text-fill:#1F2937;-fx-font-family:'Microsoft YaHei';";
                            String boldSpec   = "-fx-font-size:16px;-fx-font-weight:600;-fx-text-fill:#1F2937;-fx-font-family:'Microsoft YaHei';";
                            s1.setStyle(normalSpec); s2.setStyle(boldSpec);
                            s3.setStyle(normalSpec); s4.setStyle(boldSpec); s5.setStyle(normalSpec);
                            HBox specRow = new HBox(4, s1, s2, s3, s4, s5);
                            specRow.setAlignment(Pos.CENTER_LEFT);

                            // 已关联箱数
                            Label linkedLabel = new Label("已关联箱数：");
                            linkedLabel.setStyle("-fx-font-size:15px;-fx-font-weight:600;-fx-text-fill:#6B7280;-fx-font-family:'Microsoft YaHei';");
                            Label linkedVal = new Label(boxCount + " 箱");
                            linkedVal.setStyle("-fx-font-size:15px;-fx-font-weight:600;-fx-text-fill:#2563EB;-fx-font-family:'Microsoft YaHei';");
                            HBox linkedRow = new HBox(8, linkedLabel, linkedVal);
                            linkedRow.setAlignment(Pos.CENTER_LEFT);

                            // 扁平结构填入 resultInfo（gap 已在创建时设为 12）
                            resultInfo.getChildren().setAll(
                                    prodInfoHeader,
                                    buildResultRow("产品名称", productName.isEmpty() ? "—" : productName),
                                    buildResultRow("产品编号", productNo.isEmpty() ? "—" : productNo),
                                    buildResultRow("生产单号", orderNo.isEmpty() ? "—" : orderNo),
                                    divider,
                                    specHeader,
                                    specRow,
                                    linkedRow
                            );
                            resultArea.setVisible(true);
                            resultArea.setManaged(true);
                            confirmBtn.setDisable(false);
                            confirmBtn.setStyle("-fx-background-color:#2563EB;-fx-border-width:0;-fx-background-radius:8;-fx-font-size:16px;-fx-font-weight:600;-fx-text-fill:white;-fx-font-family:'Microsoft YaHei';-fx-cursor:hand;-fx-min-height:40;-fx-min-width:90;");
                            dialog.sizeToScene();
                        });
                    } else {
                        Platform.runLater(() -> {
                            errorLbl.setVisible(true);
                            errorLbl.setManaged(true);
                            dialog.sizeToScene();
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        errorLbl.setVisible(true);
                        errorLbl.setManaged(true);
                        dialog.sizeToScene();
                    });
                }
            }, ex -> Platform.runLater(() -> {
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                dialog.sizeToScene();
            }));
        };

        // 弹窗打开期间：扫码枪读到的码直接填入箱码输入框并触发查询
        extractUnfinishedScannerConsumer = scannedCode -> Platform.runLater(() -> {
            if (!dialog.isShowing()) return;
            boxCodeInput.setText(scannedCode);
            doQuery.run();
        });
        dialog.setOnHidden(e -> extractUnfinishedScannerConsumer = null);

        queryBtn.setOnAction(e -> doQuery.run());
        boxCodeInput.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) doQuery.run();
        });

        // ── 确认逻辑：将查询结果回填主界面 ─────────────────────
        confirmBtn.setOnAction(e -> {
            if (foundOrderNo[0] == null) return;
            // 回填订单号
            orderNumField.setText(foundOrderNo[0]);
            // 回填产品（先按编号匹配已有列表，找不到则用接口返回的名称兜底）
            if (foundProductNo[0] != null && !foundProductNo[0].isEmpty()) {
                final String targetNo   = foundProductNo[0];
                final String targetName = foundProductName[0] != null ? foundProductName[0] : "";
                boolean matched = false;
                for (ProductItem item : productItems) {
                    if (targetNo.equals(item.getNo())) {
                        suppressProductSearch = true;
                        productComboBox.setValue(item);
                        productCodeLabel.setText(item.getNo());
                        productCodeRow.setVisible(true);
                        productCodeRow.setManaged(true);
                        suppressProductSearch = false;
                        matched = true;
                        break;
                    }
                }
                // 列表中未找到时，用接口返回的名称 + 编号构造 ProductItem 插入列表再填入
                if (!matched) {
                    ProductItem fallback = new ProductItem(
                            targetName.isEmpty() ? targetNo : targetName, targetNo, "");
                    suppressProductSearch = true;
                    // ComboBox 非可编辑模式下，值必须在 items 列表中才能正常显示
                    productItems.add(0, fallback);
                    productComboBox.setValue(fallback);
                    productCodeLabel.setText(targetNo);
                    productCodeRow.setVisible(true);
                    productCodeRow.setManaged(true);
                    suppressProductSearch = false;
                }
            }
            // 回填未成垛箱数
            currentCases = foundBoxCount[0];
            curCasesLabel.setText(String.valueOf(currentCases));
            // 回填采集规格
            if (foundCasesPerPallet[0] > 0) {
                casesPerPalletField.setText(String.valueOf(foundCasesPerPallet[0]));
                casesPerPalletDisplayLabel.setText(String.valueOf(foundCasesPerPallet[0]));
            }
            if (foundBoxesPerCase[0] > 0) {
                boxesPerCaseField.setText(String.valueOf(foundBoxesPerCase[0]));
                boxesPerCaseDisplayLabel.setText(String.valueOf(foundBoxesPerCase[0]));
            }
            // 标记需要在开始采集时调用 restore-queue，恢复关联关系入队列
            pendingRestoreOrderNo = foundOrderNo[0];
            addOpLog(LocalDateTime.now().format(TIME_FMT)
                    + "  提取工单未成垛成功，订单号：" + foundOrderNo[0]
                    + "，产品：" + (foundProductName[0] != null && !foundProductName[0].isEmpty()
                            ? foundProductName[0] + "（" + foundProductNo[0] + "）"
                            : foundProductNo[0])
                    + "，已有箱数：" + foundBoxCount[0]
                    + (foundCasesPerPallet[0] > 0 ? "，规格：1垛" + foundCasesPerPallet[0] + "箱/1箱" + foundBoxesPerCase[0] + "盒" : "")
                    + (foundPendingBoxes[0] > 0 ? "，待恢复盒码：" + foundPendingBoxes[0] + " 个" : ""),
                    LogType.INFO);
            dialog.close();
        });

        // ── 自定义标题栏（可拖拽）────────────────────────────────
        Label titleLbl = new Label("提取工单未成垛");
        titleLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1F2937;-fx-font-family:'Microsoft YaHei';");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Button titleCloseBtn = new Button("×");
        titleCloseBtn.getStyleClass().add("sw2-dialog-title-close-btn");
        titleCloseBtn.setOnAction(e -> dialog.close());
        HBox titleBar = new HBox(8, titleLbl, titleSpacer, titleCloseBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-background-color:#F5F7FA;-fx-border-color:transparent transparent #E5E7EB transparent;-fx-border-width:0 0 1 0;-fx-padding:0 8 0 24;-fx-min-height:56;-fx-pref-height:56;");

        // 拖拽支持
        final double[] dragOffset = {0, 0};
        titleBar.setOnMousePressed(e -> { dragOffset[0] = e.getSceneX(); dragOffset[1] = e.getSceneY(); });
        titleBar.setOnMouseDragged(e -> { dialog.setX(e.getScreenX() - dragOffset[0]); dialog.setY(e.getScreenY() - dragOffset[1]); });

        // ── 组装场景 ─────────────────────────────────────────────
        VBox root = new VBox(titleBar, content, bottomBar);
        root.setStyle("-fx-background-color:white;-fx-border-color:#D9E1EC;-fx-border-width:1;");
        Scene scene = new Scene(root, 560, -1);
        scene.getStylesheets().addAll(
                ShiwanM2MainController.class.getResource("/css/base-styles.css").toExternalForm(),
                ShiwanM2MainController.class.getResource("/css/shiwan-m2-styles.css").toExternalForm());
        dialog.setScene(scene);
        ShiwanM2ScannerConnectHelper.tryReconnectScannersAsync();
        dialog.show();
        boxCodeInput.requestFocus();
    }

    /** 构建查询结果行（标签 + 值） */
    private HBox buildResultRow(String label, String value) {
        Label lbl = new Label(label + "：");
        lbl.setStyle("-fx-font-size:15px;-fx-font-weight:600;-fx-text-fill:#6B7280;-fx-font-family:'Microsoft YaHei';");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:15px;-fx-font-weight:normal;-fx-text-fill:#1F2937;-fx-font-family:'Microsoft YaHei';");
        HBox row = new HBox(8, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @FXML
    private void onRetractReject() {
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [操作] 点击收回剔除", LogType.INFO);
        boolean ok = FxDialog.confirm(
                mainTabPane.getScene().getWindow(),
                "收回剔除",
                "确认向剔除装置发送收回指令？\n\n将通过串口向剔除装置发送收回动作指令。"
        );
        if (ok) {
            ShiwanM2HardwareService.getInstance().retractRejection();
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [剔除装置] 已发送收回指令", LogType.INFO);
        }
    }

    @FXML
    private void onResetRejectCount() {
        addOpLog(LocalDateTime.now().format(TIME_FMT) + "  [操作] 点击剔除数清零", LogType.INFO);
        boolean ok = FxDialog.confirm(
                mainTabPane.getScene().getWindow(),
                "剔除数清零",
                "确认将总剔除数重置为0？\n\n此操作仅重置界面计数，不影响数据库记录。"
        );
        if (ok) {
            totalRejectCount = 0;
            rejectCountLabel.setText("0");
            addOpLog(LocalDateTime.now().format(TIME_FMT) + "  总剔除数已清零", LogType.INFO);
        }
    }

    @FXML
    private void onTaskHelp() {
        FxHelpDialog.show(
                currentTimeLabel.getScene().getWindow(),
                "任务控制 - 功能说明",
                "- **开始/停止采集**：点击开始后系统检测硬件设备，检测通过后开始采集工作；再次点击停止采集，未满垛数据会保留",
                "- **无需采集码**：当天不生产五码合一产品时开启，所有读码设备不工作，不采集任何数据",
                "- **关闭报警**：当自动上传异常触发报警时，点击此按钮可重置三色声光报警灯（停止灯光和蜂鸣）",
                "- **强制满垛**：未达到设定箱数也强制结束当前垛，生成虚拟垛标并重置计数",
                "- **提取工单未成垛**：停止采集时可用。弹窗输入或扫描未完成垛中的任一箱码，查出生产信息、采集规格和当前箱数后确认，回显主页面并自动开始采集",
                "- **收回剔除**：当剔除设备进行剔除动作后没有自动收回时，点击此按钮让剔除设备收回当前剔除动作",
                "- **剔除数清零**：将总剔除数重置为0（总剔除数显示在单位实时统计区域）"
        );
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
                    boxRecvCountSet = false;
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
            boolean autoUploadOn = ShiwanM2SettingsStore.get().getUpload() == null
                    || ShiwanM2SettingsStore.get().getUpload().isAutoUpload();
            String uploadSuffix = autoUploadOn ? "，正在上传..." : "";
            addDataLog(now + "  满垛提示 - 垛码：" + palletCode + " 已生成，共" + cpp + "箱" + uploadSuffix, LogType.SUCCESS);
            addOpLog(now + "  满垛完成，已生产垛数：" + palletCount, LogType.INFO);

            addPendingUploadRowAtTop(palletCode, cpp + "箱");

            currentCases = 0;
            currentBoxes = 0;
            boxRecvCountSet = false;
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
        String line = softWrapLongToken(message);
        Platform.runLater(() -> {
            trimList(dataLogItems);
            dataLogItems.add(0, new LogEntry(line, type));
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

    /** 为超长连续串插入零宽断点，便于 Label 换行展示完整内容、不出现末尾省略号。 */
    private static String softWrapLongToken(String text) {
        if (text == null || text.isEmpty()) return text;
        final int chunk = 12;
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            sb.append(ch);
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':') {
                run++;
                if (run >= chunk) {
                    sb.append('\u200B');
                    run = 0;
                }
            } else {
                run = 0;
            }
        }
        return sb.toString();
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
        FxDialog.alert(mainTabPane.getScene().getWindow(), title, content);
    }

    private void showWarn(String title, String content) {
        FxDialog.warn(mainTabPane.getScene().getWindow(), title, content);
    }

    // ==================== 枚举与数据模型 ====================

    /** 日志类型 */
    public enum LogType {
        DATA,    // 普通数据（黑字灰边）
        SUCCESS, // 成功（绿字绿边）
        WARN,    // 警告（黄字黄边）
        ERROR,   // 错误/报警（红字红边）
        INFO,    // 操作日志（灰字灰边）
        UPLOAD_BLUE // 上传中（蓝字，与需求文档一致）
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
        public       String       boxCount;
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
            label.setMaxHeight(Double.MAX_VALUE);
            label.setTextOverrun(OverrunStyle.CLIP);
            label.getStyleClass().add("log-cell-label");
            // prefWidth=0 使 cell 自动跟随 ListView 宽度分配，避免 cell 撑出横向滚动条
            setPrefWidth(0);
            setMaxWidth(Double.MAX_VALUE);
            // 将 label 宽度绑定到 ListView 宽度（减去左右 padding 16 + 竖向滚动条约 14px）
            listViewProperty().addListener((obs, oldLv, newLv) -> {
                if (newLv != null) {
                    label.prefWidthProperty().bind(newLv.widthProperty().subtract(30));
                }
            });
        }

        @Override
        protected double computePrefHeight(double width) {
            if (isEmpty() || getItem() == null) {
                return super.computePrefHeight(width);
            }
            double pad = snapSizeX(getInsets().getLeft() + getInsets().getRight());
            double lw = width > 0 ? width - pad : -1;
            if (lw <= 0 && getListView() != null) {
                lw = Math.max(0, getListView().getWidth() - 30);
            }
            if (lw <= 0) {
                lw = 280;
            }
            label.setMaxWidth(lw);
            return snapSizeY(getInsets().getTop() + getInsets().getBottom() + label.prefHeight(lw));
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
                    "log-cell-warn", "log-cell-error", "log-cell-info", "log-cell-upload-blue"
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
                case UPLOAD_BLUE:
                    label.getStyleClass().add("log-cell-upload-blue");
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

        private final VBox  root        = new VBox(4);
        private final HBox  bottomRow   = new HBox();
        private final Label palletLbl   = new Label();
        private final Label statusBadge = new Label();
        private final Label boxCountLbl = new Label();

        UploadItemCell() {
            // 垛码独占第一行，超长时截断，不撑宽
            palletLbl.getStyleClass().add("shiwan-m2-upload-pallet-code");
            palletLbl.setMaxWidth(Double.MAX_VALUE);
            palletLbl.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

            // 第二行：N箱（左） + 状态标签（右），两端对齐
            boxCountLbl.getStyleClass().add("shiwan-m2-upload-box-count");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            bottomRow.setAlignment(Pos.CENTER_LEFT);
            bottomRow.setMaxWidth(Double.MAX_VALUE);
            bottomRow.getChildren().addAll(boxCountLbl, spacer, statusBadge);

            root.getChildren().addAll(palletLbl, bottomRow);
            root.getStyleClass().add("shiwan-m2-upload-item");
            root.setPadding(new Insets(10, 12, 10, 12));
            root.setMaxWidth(Double.MAX_VALUE);

            // 绑定 cell 根节点宽度 = ListView 宽度 - 垂直滚动条预留，防止水平溢出
            listViewProperty().addListener((obs, old, lv) -> {
                if (lv != null) {
                    root.prefWidthProperty().bind(lv.widthProperty().subtract(20));
                }
            });
        }

        @Override
        protected void updateItem(UploadItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            palletLbl.setText(formatPalletDisplayForCard(item.palletCode));
            boxCountLbl.setText(item.boxCount);

            statusBadge.getStyleClass().removeAll(
                    "shiwan-m2-badge-pending", "shiwan-m2-badge-done",
                    "shiwan-m2-badge-uploading", "shiwan-m2-badge-failed"
            );
            root.getStyleClass().removeAll("shiwan-m2-upload-item-failed");
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
                    statusBadge.getStyleClass().add("shiwan-m2-badge-failed");
                    root.getStyleClass().add("shiwan-m2-upload-item-failed");
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
