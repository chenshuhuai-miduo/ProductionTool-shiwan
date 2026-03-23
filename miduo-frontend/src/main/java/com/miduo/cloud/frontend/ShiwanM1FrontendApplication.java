package com.miduo.cloud.frontend;

import com.miduo.cloud.frontend.controller.DeviceMismatchDialogController;
import com.miduo.cloud.frontend.controller.LicenseInfoController;
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
import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * 石湾1号机（瓶盒关联软件）JavaFX 启动类
 * <p>
 * 加载 {@code ShiwanM1MainWindow.fxml} 作为主界面，
 * 窗口规格 1366×768（适配工控机显示器）。
 * </p>
 */
public class ShiwanM1FrontendApplication extends Application {

    private CssHotReloader cssHotReloader;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("========================================");
        System.out.println("石湾1号机（瓶盒关联软件）正在启动...");
        System.out.println("========================================");

        OperateLogBatchManager.getInstance().start();

        if (!performLicenseValidation()) {
            System.err.println("✗ 许可证验证失败，应用程序退出");
            Platform.exit();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ShiwanM1MainWindow.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1400, 900);

            primaryStage.setTitle("米多赋码采集关联系统");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(false);

            StageIconUtil.setStageIcon(primaryStage);

            if (CssHotReloader.isDevMode()) {
                cssHotReloader = new CssHotReloader(scene, "/fxml/ShiwanM1MainWindow.fxml");
                cssHotReloader.start();
            }

            // 点 X 最小化而非退出（工控机常驻程序）
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                primaryStage.setIconified(true);
            });

            primaryStage.show();

            System.out.println("✓ 石湾 1 号机界面启动成功！窗口大小：1400x900");

        } catch (Exception e) {
            System.err.println("✗ 启动失败！" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        if (cssHotReloader != null) {
            cssHotReloader.stop();
        }
        OperateLogBatchManager.getInstance().stop();
        super.stop();
        System.out.println("✓ 石湾1号机应用已安全退出");
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
                alert.showAndWait();
                Platform.exit();
            });
            return false;
        }
    }

    public static void main(String[] args) {
        // 「关于系统」弹窗展示内容，发版时在此修改，用 \n 分隔多行
        System.setProperty("app.about.text",
            "米多赋码采集关联系统 v1.0.0\n关联模式：瓶盒关联\n部署站点：石湾产线\n版权所有 © 米多科技");
        launch(args);
    }
}
