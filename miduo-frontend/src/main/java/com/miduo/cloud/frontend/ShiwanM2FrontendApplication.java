package com.miduo.cloud.frontend;

import com.miduo.cloud.frontend.controller.DeviceMismatchDialogController;
import com.miduo.cloud.frontend.controller.ShiwanM2MainController;
import com.miduo.cloud.frontend.controller.TrialExpireDialogController;
import com.miduo.cloud.frontend.controller.TrialExpiringDialogController;
import com.miduo.cloud.frontend.controller.UnactivatedDialogController;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.OperateLogBatchManager;
import com.miduo.cloud.frontend.util.CssHotReloader;
import com.miduo.cloud.frontend.util.StageIconUtil;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 石湾2号机（盒箱垛关联软件）JavaFX 启动类
 * <p>
 * 加载 {@code ShiwanM2MainWindow.fxml} 作为主界面，
 * 窗口规格 1366×768（适配工控机显示器）。
 * </p>
 */
public class ShiwanM2FrontendApplication extends Application {

    private CssHotReloader cssHotReloader;
    /** 启动前产品同步任务（异步执行，供主界面在“产品选择”时判断是否完成）。 */
    private static volatile CompletableFuture<Boolean> startupProductSyncFuture = CompletableFuture.completedFuture(false);
    /** 前端启动起始时间（用于启动阶段耗时埋点）。 */
    private static volatile long frontendStartupBeginMs = 0L;
    /** 启动期许可证状态快照，供主界面复用，避免重复采集设备指纹。 */
    private static volatile LicenseStatusSnapshot startupLicenseStatusSnapshot;

    @Override
    public void start(Stage primaryStage) throws Exception {
        frontendStartupBeginMs = System.currentTimeMillis();
        System.out.println("========================================");
        System.out.println("石湾2号机（盒箱垛关联软件）正在启动...");
        System.out.println("========================================");

        OperateLogBatchManager.getInstance().start();

        // 从系统配置读取后端 API 地址（默认 http://localhost:8080），前端请求都发往该地址
        String backendUrl = ShiwanM2SettingsStore.getBackendBaseUrl();
        HttpUtil.setBaseUrl(backendUrl);
        System.out.println("  后端 API 地址: " + backendUrl + "（可在系统设置中修改「后端服务地址」）");

        long licenseBeginMs = System.currentTimeMillis();
        boolean licenseOk = performLicenseValidation();
        long licenseEndMs = System.currentTimeMillis();
        System.out.println("[启动耗时] 许可证校验耗时 " + (licenseEndMs - licenseBeginMs) + " ms");
        if (!licenseOk) {
            System.err.println("✗ 许可证验证失败，应用程序退出");
            Platform.exit();
            return;
        }
        // 启动前产品同步改为异步：不阻塞主界面显示。
        // 主界面点击“产品选择”时再根据该任务状态决定直接打开或显示加载提示。
        startStartupProductSyncAsync();

        try {
            long fxmlLoadBeginMs = System.currentTimeMillis();
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ShiwanM2MainWindow.fxml"));
            Parent root = loader.load();
            long fxmlLoadEndMs = System.currentTimeMillis();
            System.out.println("[启动耗时] 主界面FXML加载耗时 " + (fxmlLoadEndMs - fxmlLoadBeginMs) + " ms");

            ShiwanM2MainController mainController = loader.getController();

            // 动态适配屏幕分辨率：高度占屏幕 80%，宽度取适当值
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double windowHeight = screenBounds.getHeight() * 0.80;
            double windowWidth = Math.min(1400, screenBounds.getWidth() - 20);

            Scene scene = new Scene(root, windowWidth, windowHeight);

            primaryStage.setTitle("米多赋码采集关联系统");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(false);

            // 水平居中，Y 靠近屏幕顶部以确保标题栏（缩小/放大/关闭按钮）可见
            primaryStage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - windowWidth) / 2);
            primaryStage.setY(screenBounds.getMinY() + 20);

            StageIconUtil.setStageIcon(primaryStage);

            if (CssHotReloader.isDevMode()) {
                cssHotReloader = new CssHotReloader(scene, "/fxml/ShiwanM2MainWindow.fxml");
                cssHotReloader.start();
            }

            // 点 X 执行与菜单「退出软件」一致的退出逻辑（检查是否有正在进行的采集任务等）
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                mainController.requestExit();
            });

            primaryStage.show();
            long firstShowMs = System.currentTimeMillis();
            System.out.println("[启动耗时] 前端启动总耗时（到主界面显示） "
                    + (firstShowMs - frontendStartupBeginMs) + " ms");

            System.out.println("✓ 石湾 2 号机界面启动成功！窗口大小：" + (int) windowWidth + "x" + (int) windowHeight);

        } catch (Exception e) {
            System.err.println("✗ 启动失败！" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void startStartupProductSyncAsync() {
        final long productSyncBeginMs = System.currentTimeMillis();
        startupProductSyncFuture = CompletableFuture.supplyAsync(this::preSyncProductsBeforeMainWindow)
            .whenComplete((ok, ex) -> {
                long now = System.currentTimeMillis();
                long phaseMs = now - productSyncBeginMs;
                long sinceStartupMs = frontendStartupBeginMs > 0 ? now - frontendStartupBeginMs : phaseMs;
                if (ex == null) {
                    System.out.println("[启动耗时] 启动前产品异步同步完成，耗时 "
                            + phaseMs + " ms（启动后 " + sinceStartupMs + " ms）"
                            + "，结果=" + (Boolean.TRUE.equals(ok) ? "成功" : "失败"));
                } else {
                    System.out.println("[启动耗时] 启动前产品异步同步异常，耗时 "
                            + phaseMs + " ms（启动后 " + sinceStartupMs + " ms）");
                }
            })
            .exceptionally(ex -> {
                System.out.println("⚠ 启动前产品同步异常：" + ex.getMessage() + "（主界面产品选择时可继续操作）");
                return false;
            });
    }

    /** 供主界面判断“启动前产品同步”是否完成。 */
    public static CompletableFuture<Boolean> getStartupProductSyncFuture() {
        return startupProductSyncFuture;
    }

    public static LicenseStatusSnapshot getStartupLicenseStatusSnapshot() {
        return startupLicenseStatusSnapshot;
    }

    /**
     * 主界面打开前预同步产品到本地。
     * 失败不阻断启动，仅记录日志。
     */
    private boolean preSyncProductsBeforeMainWindow() {
        try {
            String resp = HttpUtil.doPost("/api/shiwan-m2/products/sync", "");
            JsonNode node = HttpUtil.getObjectMapper().readTree(resp);
            boolean ok = node != null && node.has("code") && node.get("code").asInt() == 200;
            if (ok) {
                System.out.println("✓ 启动前产品同步成功");
            } else {
                String msg = (node != null && node.has("message")) ? node.get("message").asText() : "未知错误";
                System.out.println("⚠ 启动前产品同步失败：" + msg + "（不影响主界面使用）");
            }
            return ok;
        } catch (Exception e) {
            System.out.println("⚠ 启动前产品同步异常：" + e.getMessage() + "（不影响主界面使用）");
            return false;
        }
    }

    @Override
    public void stop() throws Exception {
        if (cssHotReloader != null) {
            cssHotReloader.stop();
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
        // 「关于系统」弹窗展示内容，发版时在此修改，用 \n 分隔多行
        System.setProperty("app.about.text",
            "米多赋码采集关联系统 v1.0.0\n关联模式：盒箱垛关联\n部署站点：石湾产线\n版权所有 © 米多科技");
        launch(args);
    }
}
