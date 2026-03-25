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

/**
 * 石湾2号机（盒箱垛关联软件）JavaFX 启动类
 * <p>
 * 加载 {@code ShiwanM2MainWindow.fxml} 作为主界面，
 * 窗口规格 1366×768（适配工控机显示器）。
 * </p>
 */
public class ShiwanM2FrontendApplication extends Application {

    private CssHotReloader cssHotReloader;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("========================================");
        System.out.println("石湾2号机（盒箱垛关联软件）正在启动...");
        System.out.println("========================================");

        OperateLogBatchManager.getInstance().start();

        // 从系统配置读取后端 API 地址（默认 http://localhost:8080），前端请求都发往该地址
        String backendUrl = ShiwanM2SettingsStore.getBackendBaseUrl();
        HttpUtil.setBaseUrl(backendUrl);
        System.out.println("  后端 API 地址: " + backendUrl + "（可在系统设置中修改「后端服务地址」）");

        if (!performLicenseValidation()) {
            System.err.println("✗ 许可证验证失败，应用程序退出");
            Platform.exit();
            return;
        }
        // 主界面打开前先尝试一次云端产品同步：
        // 成功：主界面点“产品选择”时无需再触发远端同步；
        // 失败：主界面打开后在首次产品选择时补偿重试一次。
        boolean preSyncOk = preSyncProductsBeforeMainWindow();
        ShiwanM2MainController.reportStartupProductSyncResult(preSyncOk);

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ShiwanM2MainWindow.fxml"));
            Parent root = loader.load();

            com.miduo.cloud.frontend.controller.ShiwanM2MainController mainController = loader.getController();

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

            System.out.println("✓ 石湾 2 号机界面启动成功！窗口大小：" + (int) windowWidth + "x" + (int) windowHeight);

        } catch (Exception e) {
            System.err.println("✗ 启动失败！" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 主界面打开前预同步产品到本地。
     * 失败不阻断启动，仅记录日志并交由主界面首次产品选择时补偿重试。
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
                System.out.println("[警告] 启动前产品同步失败：" + msg + "（主界面首次产品选择时将重试）");
            }
            return ok;
        } catch (Exception e) {
            System.out.println("[警告] 启动前产品同步异常：" + e.getMessage() + "（主界面首次产品选择时将重试）");
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
            DeviceInfoService deviceInfoService = new DeviceInfoService();
            LicenseService licenseService = new LicenseService(new LicenseValidationService());
            licenseService.init();

            String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(
                deviceInfoService.getDeviceInfo());

            if (!licenseService.licenseExists()) {
                UnactivatedDialogController.showUnactivatedDialog();
                return true;
            }

            LicenseValidationService.LicenseValidationResult validationResult =
                licenseService.readLicense(currentDeviceId);

            if (validationResult.isDeviceMismatch()) {
                DeviceMismatchDialogController.showDeviceMismatchDialog(
                    validationResult.getMismatchedDeviceId());
                return false;
            }

            LicenseStatusEnum status = licenseService.getCurrentLicenseStatus(currentDeviceId);

            if (status == LicenseStatusEnum.UNACTIVATED) {
                UnactivatedDialogController.showUnactivatedDialog();
            } else if (status == LicenseStatusEnum.TRIAL_EXPIRED
                    || status == LicenseStatusEnum.EXPIRED) {
                TrialExpireDialogController.showTrialExpireDialog(status);
            } else if (status == LicenseStatusEnum.TRIAL_ACTIVE) {
                long remainingDays = licenseService.getRemainingDays(currentDeviceId);
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

    public static void main(String[] args) {
        // 「关于系统」弹窗展示内容，发版时在此修改，用 \n 分隔多行
        System.setProperty("app.about.text",
            "米多赋码采集关联系统 v1.0.0\n关联模式：盒箱垛关联\n部署站点：石湾产线\n版权所有 © 米多科技");
        launch(args);
    }
}
