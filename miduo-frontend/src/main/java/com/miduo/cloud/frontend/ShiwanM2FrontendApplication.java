package com.miduo.cloud.frontend;

import com.miduo.cloud.frontend.controller.DeviceMismatchDialogController;
import com.miduo.cloud.frontend.controller.TrialExpireDialogController;
import com.miduo.cloud.frontend.controller.TrialExpiringDialogController;
import com.miduo.cloud.frontend.controller.UnactivatedDialogController;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.OperateLogBatchManager;
import com.miduo.cloud.frontend.util.StageIconUtil;
import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * 石湾2号机（盒箱垛关联软件）JavaFX 启动类
 * <p>
 * 加载 {@code ShiwanM2MainWindow.fxml} 作为主界面，
 * 窗口规格 1366×768（适配工控机显示器）。
 * </p>
 */
public class ShiwanM2FrontendApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("========================================");
        System.out.println("石湾2号机（盒箱垛关联软件）正在启动...");
        System.out.println("========================================");

        OperateLogBatchManager.getInstance().start();

        if (!performLicenseValidation()) {
            System.err.println("✗ 许可证验证失败，应用程序退出");
            Platform.exit();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ShiwanM2MainWindow.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1366, 768);

            primaryStage.setTitle("盒箱垛关联采集系统 - 石湾2号机");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(false);

            StageIconUtil.setStageIcon(primaryStage);

            // 点 X 最小化而非退出（工控机常驻程序）
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                primaryStage.setIconified(true);
            });

            primaryStage.show();

            System.out.println("✓ 石湾2号机界面启动成功！窗口大小: 1366x768");

        } catch (Exception e) {
            System.err.println("✗ 启动失败！" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
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
                alert.showAndWait();
                Platform.exit();
            });
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
