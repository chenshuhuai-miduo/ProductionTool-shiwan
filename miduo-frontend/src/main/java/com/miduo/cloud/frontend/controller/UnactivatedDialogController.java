package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.miduo.cloud.frontend.controller.OfflineActivationController;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.SpringContextUtil;
import com.miduo.cloud.frontend.util.StageIconUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 未激活提示对话框控制器
 */
public class UnactivatedDialogController {

    @FXML private Button activateButton;
    @FXML private Button exitButton;
    @FXML private Button closeButton;

    @FXML
    public void initialize() {
        // 初始化按钮事件
        activateButton.setOnAction(e -> onActivate());
        exitButton.setOnAction(e -> onExit());
        closeButton.setOnAction(e -> onClose());
    }

     /**
     * 激活按钮事件
     */
    @FXML
    private void onActivate() {
        try {
            // 获取设备ID
            DeviceInfoService deviceInfoService = new DeviceInfoService();
            String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());

            // 获取当前窗口作为 owner
            Stage ownerStage = (Stage) activateButton.getScene().getWindow();
            
            // 打开离线激活向导（使用 showAndWait 等待窗口关闭）
            OfflineActivationController.showOfflineActivationWizard(ownerStage, currentDeviceId);
            
            // 离线激活窗口关闭后，检查是否激活成功
            // 如果激活成功，关闭未激活弹窗
            try {
                LicenseService licenseService = new LicenseService(new LicenseValidationService());
                licenseService.init();
                LicenseStatusEnum status = licenseService.getCurrentLicenseStatus(currentDeviceId);
                
                if (status != LicenseStatusEnum.UNACTIVATED) {
                    // 已激活或试用中，关闭未激活弹窗
                    closeDialog();
                }
                // 如果还是未激活状态，保持弹窗显示
            } catch (Exception e) {
                // 检查状态失败，保持弹窗显示
                System.err.println("检查激活状态失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("打开离线激活向导失败: " + e.getMessage());
            e.printStackTrace();
        }
        // 移除 closeDialog() 调用，不要立即关闭弹窗
    }

    /**
     * 退出软件按钮事件（实际是关闭弹窗）
     */
    @FXML
    private void onExit() {
        // 只关闭弹窗，不退出应用
        closeDialog();
        System.exit(0);
    }

    /**
     * 关闭按钮事件（点击×按钮）
     */
    @FXML
    private void onClose() {
        // 只关闭弹窗，不退出应用
        closeDialog();
        System.exit(0);
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) activateButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 静态方法：显示未激活提示对话框
     */
    public static void showUnactivatedDialog() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                UnactivatedDialogController.class.getResource("/fxml/UnactivatedDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("米多赋码采集关联系统");
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                UnactivatedDialogController.class.getResource("/css/deviceVerification-style.css").toExternalForm());
            stage.setScene(scene);
            
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);  // 改为透明样式
            stage.setResizable(false);
            StageIconUtil.setStageIcon(stage);

            // 关键：拦截系统标题栏 X / Alt+F4 / ESC 导致的关闭
            stage.setOnCloseRequest(event -> {
                event.consume();
                Platform.exit();
                System.exit(0);
            });

            // 允许用户关闭弹窗（点击×按钮或ESC）
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

