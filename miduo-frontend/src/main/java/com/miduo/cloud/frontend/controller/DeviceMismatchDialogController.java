package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 设备不匹配提示对话框控制器
 */
public class DeviceMismatchDialogController {

    @FXML private Label deviceIdLabel;
    @FXML private Label deviceModelLabel;
    @FXML private Button exitButton;

    private String mismatchedDeviceId;

    @FXML
    public void initialize() {
        // 更新设备信息显示
        updateDeviceInfo();
    }

    /**
     * 设置不匹配的设备ID
     */
    public void setMismatchedDeviceId(String deviceId) {
        this.mismatchedDeviceId = deviceId;
        updateDeviceInfo();
    }

    /**
     * 更新设备信息显示
     */
    private void updateDeviceInfo() {
        Platform.runLater(() -> {
            try {
                DeviceInfoService deviceInfoService = new DeviceInfoService();
                if (deviceInfoService != null) {
                    DeviceInfoService.DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo();
                    String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfo);

                    // 更新各个标签
                    if (deviceIdLabel != null) {
                        deviceIdLabel.setText(formatDeviceId(currentDeviceId));
                    }
                    if (deviceModelLabel != null) {
                        deviceModelLabel.setText(deviceInfo.getDeviceModel());
                    }
                }
            } catch (Exception e) {
                System.err.println("无法获取设备信息：" + e.getMessage());
            }
        });
    }

    /**
     * 重新激活按钮事件
     */
    @FXML
    private void onReactivate() {
        // 打开许可证信息页面进行重新激活
        Platform.runLater(() -> {
            try {
                LicenseInfoController.showLicenseInfo();
            } catch (Exception e) {
                // 处理异常
            }
        });

        closeDialog();
    }

    /**
     * 联系客服按钮事件
     */
    @FXML
    private void onContact() {
        // 显示联系信息对话框
        Platform.runLater(this::showContactDialog);
    }

    /**
     * 退出软件按钮事件
     */
    @FXML
    private void onExit() {
        // 退出应用程序
        System.exit(0);
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 显示联系方式对话框
     */
    private void showContactDialog() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("联系我们");
        alert.setHeaderText("米多专员联系方式");
        alert.setContentText("请通过以下方式联系米多专员获取支持：\n\n" +
                           "• 企业微信：搜索米多科技\n" +
                           "• 电话：15917372153\n" +
                           "• 邮箱：support@miduo.com\n\n" +
                           "请提供上述设备信息以便快速处理。\n\n" +
                           "工作时间：周一至周五 9:00-18:00");

        // 添加确定按钮
        alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * 格式化设备ID显示
     */
    private String formatDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() < 8) {
            return deviceId;
        }
        return deviceId.substring(0, 8) + "****" + deviceId.substring(deviceId.length() - 4);
    }

    /**
     * 静态方法：显示设备不匹配提示对话框
     */
    public static void showDeviceMismatchDialog(String mismatchedDeviceId) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                DeviceMismatchDialogController.class.getResource("/fxml/DeviceMismatchDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            // 设置控制器参数
            DeviceMismatchDialogController controller = loader.getController();
            controller.setMismatchedDeviceId(mismatchedDeviceId);

            Stage stage = new Stage();
            stage.setTitle("设备验证失败");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    DeviceMismatchDialogController.class.getResource("/css/deviceVerification-style.css").toExternalForm());

            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);

            // 设置为不可关闭（用户必须选择操作）
            stage.setOnCloseRequest(e -> System.exit(0));

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}










