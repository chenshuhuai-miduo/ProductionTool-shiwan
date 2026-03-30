package com.miduo.cloud.frontend;

import com.miduo.cloud.frontend.controller.DeviceMismatchDialogController;
import com.miduo.cloud.frontend.controller.ShiwanM2MainController;
import com.miduo.cloud.frontend.controller.TrialExpireDialogController;
import com.miduo.cloud.frontend.controller.TrialExpiringDialogController;
import com.miduo.cloud.frontend.controller.UnactivatedDialogController;
import com.miduo.cloud.frontend.service.DeviceConnectionManager;
import com.miduo.cloud.frontend.service.ShiwanM2HardwareService;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.OperateLogBatchManager;
import com.miduo.cloud.frontend.util.CssHotReloader;
import com.miduo.cloud.frontend.util.StageIconUtil;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.FileLogManager;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.time.LocalDate;

/**
 * 石湾2号机（盒箱垛关联软件）JavaFX 启动类
 * <p>
 * 加载 {@code ShiwanM2MainWindow.fxml} 作为主界面，
 * 窗口规格 1366×768（适配工控机显示器）。
 * </p>
 */
public class ShiwanM2FrontendApplication extends Application {

    private CssHotReloader cssHotReloader;
    /** 启动等待小窗：尽早显示，避免用户误以为未响应而重复双击 exe */
    private Stage startupSplashStage;
    /** 前端启动起始时间（用于启动阶段耗时埋点）。 */
    private static volatile long frontendStartupBeginMs = 0L;
    /**
     * 进程入口时间：统一启动器 {@code main} 或仅前端 {@code ShiwanM2FrontendApplication.main} 第一条语句，
     * 用于日志中「从双击 exe 起算」的耗时。
     */
    private static volatile long processEntryEpochMs = 0L;
    /** 启动期许可证状态快照，供主界面复用，避免重复采集设备指纹。 */
    private static volatile LicenseStatusSnapshot startupLicenseStatusSnapshot;

    /**
     * 后端就绪信号门（初始 count=1）。
     * 同 JVM 启动时由 {@code ShiwanM2ApplicationLauncher} 调用 {@link #signalBackendReady()} 释放；
     * 单独启动前端时将在超时后自动释放，让控制器正常尝试连接（后端已在外部启动）。
     */
    private static final java.util.concurrent.CountDownLatch BACKEND_READY_LATCH =
            new java.util.concurrent.CountDownLatch(1);

    /**
     * 由启动器在后端 Spring Boot 就绪后调用，通知前端控制器可以开始 DB 检测和 IO 连接。
     * 幂等：重复调用无副作用。
     */
    public static void signalBackendReady() {
        BACKEND_READY_LATCH.countDown();
    }

    /**
     * 由统一启动器在 {@code main} 入口尽早调用，便于日志从「进程启动」连续计时。
     */
    public static void setProcessEntryEpochMs(long epochMs) {
        if (epochMs > 0L) {
            processEntryEpochMs = epochMs;
        }
    }

    /**
     * 启动阶段埋点：写入当日文件日志（模块「启动追踪」）并打印控制台，便于排查「多久才看见界面」。
     */
    public static void traceStartup(String phase) {
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder(phase);
        if (processEntryEpochMs > 0L) {
            sb.append(" | +").append(now - processEntryEpochMs).append("ms(进程入口)");
        }
        if (frontendStartupBeginMs > 0L) {
            sb.append(" | +").append(now - frontendStartupBeginMs).append("ms(JavaFX.start)");
        }
        String msg = sb.toString();
        try {
            FileLogManager.getInstance().logInfo("启动追踪", msg);
        } catch (Throwable ignored) {
            // 文件日志未启动或异常时不影响启动
        }
        System.out.println("[启动追踪] " + msg);
    }

    /**
     * 后端就绪（或超时）后在后台线程执行 {@code callback}，完成后通过 {@code Platform.runLater} 切回 FX 线程。
     * 超时后仍会执行（后端可能已在外部启动），由调用方处理接口不可用的情况。
     *
     * @param callback     需在 FX 线程执行的初始化逻辑
     * @param timeoutSecs  最长等待秒数（建议 60）
     */
    public static void runAfterBackendReady(Runnable callback, int timeoutSecs) {
        Thread waiter = new Thread(() -> {
            try {
                BACKEND_READY_LATCH.await(timeoutSecs, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(callback);
        }, "ShiwanM2-BackendReady-Waiter");
        waiter.setDaemon(true);
        waiter.start();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        frontendStartupBeginMs = System.currentTimeMillis();
        FileLogManager.getInstance().start();
        traceStartup("JavaFX Application.start 入口");
        // ① 最先出窗：避免在读配置/许可证阻塞 FX 线程前长时间无任何界面（用户易重复双击 exe）
        showStartupSplash();
        traceStartup("启动等待小窗已显示");

        // ② 短延迟后再做读盘与许可证：慢机/Win7 上需留出时间让启动窗真正合成到屏幕
        PauseTransition splashPaintDelay = new PauseTransition(Duration.millis(280));
        splashPaintDelay.setOnFinished(ev -> {
            try {
                traceStartup("首帧延迟结束，开始初始化(日志批量器/读配置/许可证)");
                System.out.println("========================================");
                System.out.println("石湾2号机（盒箱垛关联软件）正在启动...");
                System.out.println("========================================");

                OperateLogBatchManager.getInstance().start();

                String backendUrl = ShiwanM2SettingsStore.getBackendBaseUrl();
                HttpUtil.setBaseUrl(backendUrl);
                System.out.println("  后端 API 地址: " + backendUrl + "（可在系统设置中修改「后端服务地址」）");
                traceStartup("后端 API 基址已配置: " + backendUrl);

                long licenseBeginMs = System.currentTimeMillis();
                boolean licenseOk = performLicenseValidation();
                long licenseEndMs = System.currentTimeMillis();
                long licenseCost = licenseEndMs - licenseBeginMs;
                System.out.println("[启动耗时] 许可证校验耗时 " + licenseCost + " ms");
                traceStartup("许可证校验结束，耗时 " + licenseCost + "ms，结果=" + (licenseOk ? "通过" : "未通过"));
                if (!licenseOk) {
                    System.err.println("✗ 许可证验证失败，应用程序退出");
                    traceStartup("许可证未通过，退出(启动小窗将关闭)");
                    closeStartupSplash();
                    Platform.exit();
                    return;
                }

                traceStartup("即将进入主界面 FXML 加载前短延迟(200ms)");
                // ③ 再留一截时间加载主 FXML（FXML 很重），避免小窗与主窗切换看起来像「没提示」
                PauseTransition beforeFxml = new PauseTransition(Duration.millis(200));
                beforeFxml.setOnFinished(e2 -> loadMainWindowAfterSplash(primaryStage));
                beforeFxml.play();
            } catch (Exception e) {
                closeStartupSplash();
                traceStartup("启动初始化异常: " + e.getMessage());
                System.err.println("✗ 启动失败！" + e.getMessage());
                e.printStackTrace();
                Platform.exit();
            }
        });
        splashPaintDelay.play();
    }

    private void loadMainWindowAfterSplash(Stage primaryStage) {
        try {
            traceStartup("开始加载主界面 FXML(ShiwanM2MainWindow.fxml)");
            long fxmlLoadBeginMs = System.currentTimeMillis();
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ShiwanM2MainWindow.fxml"));
            Parent root = loader.load();
            long fxmlLoadEndMs = System.currentTimeMillis();
            long fxmlCost = fxmlLoadEndMs - fxmlLoadBeginMs;
            System.out.println("[启动耗时] 主界面FXML加载耗时 " + fxmlCost + " ms");
            traceStartup("主界面 FXML 加载完成，耗时 " + fxmlCost + "ms");

            ShiwanM2MainController mainController = loader.getController();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenW = screenBounds.getWidth();
            double screenH = screenBounds.getHeight();

            // 最小窗口高度：右侧面板（统计卡+任务控制）最小可用高度约 650px，
            // 低分辨率（Win7 768p）或 DPI 缩放场景下以此兜底，避免内容被挤压。
            final double MIN_WIN_H = 650;
            final double MIN_WIN_W = 1100;

            // 目标高度：屏幕可用区域的 90%，下限 MIN_WIN_H，上限屏幕高度-8px 边距
            double windowHeight = Math.max(MIN_WIN_H, screenH * 0.90);
            windowHeight = Math.min(windowHeight, screenH - 8);

            // 目标宽度：[MIN_WIN_W, 1400]，左右各留 10px 边距
            double windowWidth = Math.max(MIN_WIN_W, Math.min(1400, screenW - 20));

            Scene scene = new Scene(root, windowWidth, windowHeight);

            primaryStage.setTitle("米多赋码采集关联系统");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(false);
            // 设置最小尺寸，防止用户手动缩小窗口后布局再次挤压
            primaryStage.setMinWidth(MIN_WIN_W);
            primaryStage.setMinHeight(MIN_WIN_H);

            // 水平 + 垂直居中（相对屏幕可用区域）
            primaryStage.setX(screenBounds.getMinX() + (screenW - windowWidth) / 2.0);
            primaryStage.setY(screenBounds.getMinY() + (screenH - windowHeight) / 2.0);

            StageIconUtil.setStageIcon(primaryStage);

            if (CssHotReloader.isDevMode()) {
                cssHotReloader = new CssHotReloader(scene, "/fxml/ShiwanM2MainWindow.fxml");
                cssHotReloader.start();
            }

            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                mainController.requestExit();
            });

            closeStartupSplash();
            primaryStage.show();
            long firstShowMs = System.currentTimeMillis();
            long totalFx = firstShowMs - frontendStartupBeginMs;
            System.out.println("[启动耗时] 前端启动总耗时（到主界面显示） "
                    + totalFx + " ms");

            System.out.println("✓ 石湾 2 号机界面启动成功！窗口大小：" + (int) windowWidth + "x" + (int) windowHeight);
            traceStartup("主界面窗口已显示(可见)，JavaFX.start 起累计 " + totalFx + "ms — 启动到此阶段结束");

        } catch (Exception e) {
            closeStartupSplash();
            traceStartup("主界面加载失败: " + e.getMessage());
            System.err.println("✗ 启动失败！" + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    private void showStartupSplash() {
        // 若 exe4j/JVM 配置了 -splash:xxx.png，此处关闭原生闪屏，改由 JavaFX 小窗承接
        try {
            java.awt.SplashScreen ss = java.awt.SplashScreen.getSplashScreen();
            if (ss != null && ss.isVisible()) {
                ss.close();
            }
        } catch (Throwable ignored) {
            // 无桌面环境或未配置 -splash 时忽略
        }

        startupSplashStage = new Stage();
        // TRANSPARENT + 透明 Scene：去掉系统矩形窗体外沿，圆角与内容一致（UNDECORATED 仍会有直角底板）
        startupSplashStage.initStyle(StageStyle.TRANSPARENT);
        // 不使用 alwaysOnTop：避免压住许可证等模态对话框

        final double splashW = 440;
        final double splashH = 248;
        final double shadowPad = 28;

        Label title = new Label("米多赋码采集关联系统");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        title.setMaxWidth(splashW - 48);
        title.setAlignment(Pos.CENTER);

        Label sub = new Label("程序已启动，正在加载…");
        sub.setStyle("-fx-font-size: 15px; -fx-text-fill: #475569;");
        sub.setMaxWidth(splashW - 48);
        sub.setAlignment(Pos.CENTER);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(56, 56);

        Label warn = new Label("请勿重复双击桌面图标\n多开会占用端口或导致异常");
        warn.setWrapText(true);
        warn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #b45309; "
                + "-fx-background-color: #fffbeb; -fx-padding: 10 14 10 14; -fx-background-radius: 6;");
        warn.setMaxWidth(splashW - 40);
        warn.setAlignment(Pos.CENTER);

        VBox box = new VBox(16, title, sub, pi, warn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32, 28, 32, 28));
        box.setPrefSize(splashW, splashH);
        box.setMinSize(splashW, splashH);
        box.setMaxSize(splashW, splashH);
        // 仅圆角底，无描边（描边与 -fx-background-radius 在部分系统上易错位）
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;");
        box.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.color(0, 0, 0, 0.22), 18, 0, 0, 3));

        StackPane root = new StackPane(box);
        root.setPadding(new Insets(shadowPad));
        root.setStyle("-fx-background-color: transparent;");

        double sceneW = splashW + shadowPad * 2;
        double sceneH = splashH + shadowPad * 2;
        Scene splashScene = new Scene(root, sceneW, sceneH);
        splashScene.setFill(Color.TRANSPARENT);
        startupSplashStage.setScene(splashScene);
        startupSplashStage.setTitle("米多赋码采集关联系统");

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        startupSplashStage.setX(bounds.getMinX() + (bounds.getWidth() - sceneW) / 2);
        startupSplashStage.setY(bounds.getMinY() + (bounds.getHeight() - sceneH) / 2);

        try {
            StageIconUtil.setStageIcon(startupSplashStage);
        } catch (Exception ignored) {
            // 与主窗一致图标，缺失资源时不影响启动
        }
        startupSplashStage.show();
        startupSplashStage.toFront();
        startupSplashStage.requestFocus();
    }

    private void closeStartupSplash() {
        if (startupSplashStage != null) {
            startupSplashStage.close();
            startupSplashStage = null;
        }
    }

    public static LicenseStatusSnapshot getStartupLicenseStatusSnapshot() {
        return startupLicenseStatusSnapshot;
    }

    @Override
    public void stop() throws Exception {
        closeStartupSplash();
        if (cssHotReloader != null) {
            cssHotReloader.stop();
        }
        // 退出时兜底释放所有IO连接，避免串口/网口占用残留
        try {
            ShiwanM2HardwareService.getInstance().allOff();
        } catch (Exception e) {
            System.err.println("[退出清理] 发送设备全关信号失败: " + e.getMessage());
        }
        try {
            DeviceConnectionManager.getInstance().stopAllConnections();
        } catch (Exception e) {
            System.err.println("[退出清理] 断开设备连接失败: " + e.getMessage());
        }
        OperateLogBatchManager.getInstance().stop();
        super.stop();
        System.out.println("✓ 石湾2号机应用已安全退出");
    }

    private boolean performLicenseValidation() {
        try {
            LicenseService licenseService = new LicenseService(new LicenseValidationService());
            licenseService.init();

            // 优先读取缓存设备ID，跳过 OSHI 硬件采集（~1.5s）；缓存缺失时惰性采集。
            String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceIdCached(
                () -> new DeviceInfoService().getDeviceInfo());

            if (!licenseService.licenseExists()) {
                UnactivatedDialogController.showUnactivatedDialog();
                return true;
            }

            LicenseValidationService.LicenseValidationResult validationResult =
                licenseService.readLicense(currentDeviceId);

            if (validationResult.isDeviceMismatch()) {
                // 设备不匹配时清除缓存，避免下次启动仍用旧设备ID
                DeviceUniqueIdGenerator.clearDeviceIdCache();
                DeviceMismatchDialogController.showDeviceMismatchDialog(
                    validationResult.getMismatchedDeviceId());
                return false;
            }

            LicenseStatusEnum status = licenseService.getCurrentLicenseStatus(currentDeviceId);
            LicenseService.LicenseInfo licenseInfo = licenseService.getLicenseInfo(currentDeviceId);
            long remainingDays = licenseInfo != null ? licenseInfo.getRemainingDays() : -1;
            LocalDate expireDate = licenseInfo != null ? licenseInfo.getExpireDate() : null;
            startupLicenseStatusSnapshot = new LicenseStatusSnapshot(status, remainingDays, expireDate);

            if (status == LicenseStatusEnum.UNACTIVATED) {
                UnactivatedDialogController.showUnactivatedDialog();
            } else if (status == LicenseStatusEnum.TRIAL_EXPIRED
                    || status == LicenseStatusEnum.EXPIRED) {
                TrialExpireDialogController.showTrialExpireDialog(status);
            } else if (status == LicenseStatusEnum.TRIAL_ACTIVE) {
                if (remainingDays >= 0 && remainingDays <= 3) {
                    TrialExpiringDialogController.showTrialExpiringDialog(remainingDays);
                }
            }
            return true;

        } catch (Exception e) {
            System.err.println("许可证验证异常: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("许可证验证失败");
                alert.setHeaderText("无法验证许可证状态");
                alert.setContentText("请联系技术支持：15917372153");
                ShiwanM2AlertUtil.applyStyle(alert);
                alert.showAndWait();
                Platform.exit();
            });
            return false;
        }
    }

    public static final class LicenseStatusSnapshot {
        private final LicenseStatusEnum status;
        private final long remainingDays;
        private final LocalDate expireDate;

        public LicenseStatusSnapshot(LicenseStatusEnum status, long remainingDays, LocalDate expireDate) {
            this.status = status;
            this.remainingDays = remainingDays;
            this.expireDate = expireDate;
        }

        public LicenseStatusEnum getStatus() {
            return status;
        }

        public long getRemainingDays() {
            return remainingDays;
        }

        public LocalDate getExpireDate() {
            return expireDate;
        }
    }

    public static void main(String[] args) {
        if (processEntryEpochMs == 0L) {
            processEntryEpochMs = System.currentTimeMillis();
        }
        // 「关于系统」弹窗展示内容，发版时在此修改，用 \n 分隔多行
        System.setProperty("app.about.text",
            "米多赋码采集关联系统 v1.0.0\n关联模式：盒箱垛关联\n部署站点：石湾产线\n版权所有 © 米多科技");
        launch(args);
    }
}
