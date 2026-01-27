package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceRequestFileUtil;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 离线激活控制器
 * 对应原HTML文件: offline-activation.html
 */
public class OfflineActivationController {

    // 场景标签
    @FXML private Button tab1;
    @FXML private Button tab2;
    @FXML private Button tab3;
    @FXML private HBox sceneTabs;

    // 场景容器
    @FXML private VBox step1Scene;
    @FXML private VBox step2Scene;
    @FXML private VBox successScene;

    // 步骤1控件
    @FXML private Button closeButton1;
    @FXML private Label deviceIdLabel;
    @FXML private Button exportButton;
    @FXML private Label filePathLabel;

    // 步骤2控件
    @FXML private Button closeButton2;
    @FXML private VBox fileDropZone;
    @FXML private HBox fileSelected;
    @FXML private Label selectedFileName;
    @FXML private Button selectFileButton;
    @FXML private Button confirmButton;

    // 步骤3控件
    @FXML private Label activationDateLabel;
    @FXML private Label expirationDateLabel;
    @FXML private Label validityLabel;

    private Stage dialogStage;
    private String deviceId;
    private File selectedLicenseFile;
    private String exportedFilePath;
    private Runnable onActivationComplete;
    private DeviceInfoService deviceInfoService;
    private String exportedRequestFilePath;

    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        // 添加按钮悬停效果
        setupButtonHoverEffects();

        // 设置文件拖放区域点击事件
        if (fileDropZone != null) {
            fileDropZone.setOnMouseClicked(e -> handleSelectFile());
        }
    }

    /**
     * 设置对话框Stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * 显示步骤1
     */
    @FXML
    private void showStep1() {
        switchScene(step1Scene);
        updateTabStyles(tab1);
    }

    /**
     * 显示步骤2
     */
    @FXML
    private void showStep2() {
        switchScene(step2Scene);
        updateTabStyles(tab2);
    }

    /**
     * 显示成功页面
     */
    @FXML
    private void showSuccess() {
        switchScene(successScene);
        updateTabStyles(tab3);

        // 设置激活信息
        LocalDate activationDate = LocalDate.now();
        LocalDate expirationDate = activationDate.plusYears(1);

        activationDateLabel.setText(activationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        expirationDateLabel.setText(expirationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        validityLabel.setText("365天");
    }

    /**
     * 处理导出设备请求文件
     */
    @FXML
    private void handleExport() {
        try {
            if  (deviceInfoService == null) {
                this.deviceInfoService = new DeviceInfoService();
            }

            // 获取设备信息
            DeviceInfoService.DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo();

            // 选择保存位置
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择设备请求文件保存位置");
            fileChooser.setInitialFileName(DeviceRequestFileUtil.generateDefaultFileName(deviceId));
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("设备请求文件", "*.devreq")
            );

            File selectedFile = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }

            // 导出文件
            DeviceRequestFileUtil.generateDeviceRequestFile(deviceInfo, selectedFile.getAbsolutePath());
            exportedRequestFilePath = selectedFile.getAbsolutePath();

            // 显示成功消息
            showAlert("导出成功",
                    "设备请求文件已导出至:\n" + exportedRequestFilePath + "\n\n请将此文件发送给米多客服获取许可证。",
                    Alert.AlertType.INFORMATION);

            showStep2();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("导出失败", "无法导出设备请求文件: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * 处理选择许可证文件
     */
    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择许可证文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("许可证文件 (*.lic)", "*.lic")
        );

        // 设置初始目录为桌面
        String desktopPath = System.getProperty("user.home") + "/Desktop";
        File desktop = new File(desktopPath);
        if (desktop.exists()) {
            fileChooser.setInitialDirectory(desktop);
        }

        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectLicenseFile(file);
        }
    }

    /**
     * 处理拖放悬停
     */
    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != fileDropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            // 改变拖放区域样式
            fileDropZone.setStyle(
                    fileDropZone.getStyle() +
                            "-fx-border-color: #1976d2; -fx-background-color: rgba(25, 118, 210, 0.05);"
            );
        }
        event.consume();
    }

    /**
     * 处理文件拖放
     */
    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            if (file.getName().endsWith(".lic")) {
                selectLicenseFile(file);
                success = true;
            } else {
                showAlert("文件格式错误", "请选择 .lic 格式的许可证文件", Alert.AlertType.WARNING);
            }
        }

        // 恢复拖放区域样式
        fileDropZone.setStyle(
                "-fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-border-style: dashed; " +
                        "-fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-padding: 40; -fx-cursor: hand;"
        );

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * 处理激活操作
     */
    @FXML
    private void handleActivate() {
        if (selectedLicenseFile == null) {
            showAlert("未选择文件", "请先选择许可证文件", Alert.AlertType.WARNING);
            return;
        }

        // 检查文件格式
        if (!selectedLicenseFile.getName().toLowerCase().endsWith(".lic")) {
            showAlert("文件格式错误", "许可证文件格式不正确，请确认文件来源", Alert.AlertType.ERROR);
            return;
        }

        try {
            // 验证并保存许可证文件
            LicenseService licenseService = new LicenseService(new LicenseValidationService());
            licenseService.init();
            
            LicenseService.LicenseImportResult result = licenseService.saveLicenseWithResult(selectedLicenseFile, deviceId);

            switch (result.getType()) {
                case SUCCESS:
                    // 显示成功场景
                    showSuccess();
                    break;
                    
                case RENEW_SUCCESS:
                    // 显示续期成功场景
                    // 从结果中获取续期信息
                    LicenseService.LicenseImportResult.RenewalInfo renewalInfo = result.getRenewalInfo();
                    RenewalSuccessController.showRenewalSuccessDialog(
                            dialogStage,  // 传递当前窗口作为 owner
                            renewalInfo.getNewExpiration(),
                            renewalInfo.getExtensionDays()
                    );
                    break;
                    
                case FILE_NOT_FOUND:
                    showAlert("导入失败", result.getMessage(), Alert.AlertType.ERROR);
                    break;
                    
                case INVALID_FORMAT:
                    showAlert("文件格式错误", result.getMessage(), Alert.AlertType.ERROR);
                    break;
                    
                case FILE_CORRUPTED:
                    showAlert("文件损坏", result.getMessage(), Alert.AlertType.ERROR);
                    break;
                    
                case DEVICE_MISMATCH:
                    showAlert("设备不匹配", result.getMessage(), Alert.AlertType.ERROR);
                    break;
                    
                case SIGNATURE_INVALID:
                    showAlert("签名无效", result.getMessage(), Alert.AlertType.ERROR);
                    break;
                    
                case LICENSE_EXPIRED:
                    showAlert("许可证已过期", result.getMessage(), Alert.AlertType.ERROR);
                    break;
                    
                case ALREADY_IMPORTED:
                    showAlert("重复导入", result.getMessage(), Alert.AlertType.INFORMATION);
                    break;
                    
                case UNKNOWN_ERROR:
                default:
                    showAlert("激活失败", result.getMessage(), Alert.AlertType.ERROR);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("激活失败", "无法处理许可证文件: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * 处理取消操作
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }

    /**
     * 处理关闭操作
     */
    @FXML
    private void handleClose() {
        closeDialog();
    }

    /**
     * 处理开始使用
     */
    @FXML
    private void handleStartUsing() {
        closeDialog();
    }

    /**
     * 选择许可证文件
     */
    private void selectLicenseFile(File file) {
        // 检查文件格式
        if (!file.getName().toLowerCase().endsWith(".lic")) {
            showAlert("文件格式错误", "许可证文件格式不正确，请确认文件来源", Alert.AlertType.WARNING);
            return;
        }
        
        selectedLicenseFile = file;
        selectedFileName.setText(file.getName());
        fileSelected.setVisible(true);
        fileSelected.setManaged(true);
        confirmButton.setDisable(false);
    }

    /**
     * 切换场景
     */
    private void switchScene(VBox targetScene) {
        step1Scene.setVisible(false);
        step1Scene.setManaged(false);
        step2Scene.setVisible(false);
        step2Scene.setManaged(false);
        successScene.setVisible(false);
        successScene.setManaged(false);

        targetScene.setVisible(true);
        targetScene.setManaged(true);

        // 切换场景后重新调整窗口大小以适应新内容
        if (dialogStage != null) {
            dialogStage.sizeToScene();
        }
    }

    /**
     * 更新标签样式
     */
    private void updateTabStyles(Button activeTab) {
        String inactiveStyle = "-fx-background-color: #f5f5f5; -fx-text-fill: #333333; " +
                "-fx-padding: 8 16; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 13;";
        String activeStyle = "-fx-background-color: #1976d2; -fx-text-fill: white; " +
                "-fx-padding: 8 16; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 13;";

        tab1.setStyle(inactiveStyle);
        tab2.setStyle(inactiveStyle);
        tab3.setStyle(inactiveStyle);
        activeTab.setStyle(activeStyle);
    }

    /**
     * 设置按钮悬停效果
     */
    private void setupButtonHoverEffects() {
        // 为所有按钮添加悬停效果
        addPrimaryButtonHover(exportButton);
        addPrimaryButtonHover(confirmButton);
        addSecondaryButtonHover(selectFileButton);
    }

    /**
     * 添加主按钮悬停效果
     */
    private void addPrimaryButtonHover(Button button) {
        if (button == null) return;

        String originalStyle = button.getStyle();
        button.setOnMouseEntered(e ->
                button.setStyle(originalStyle.replace("#1976d2", "#1565c0"))
        );
        button.setOnMouseExited(e ->
                button.setStyle(originalStyle)
        );
    }

    /**
     * 添加次要按钮悬停效果
     */
    private void addSecondaryButtonHover(Button button) {
        if (button == null) return;

        String originalStyle = button.getStyle();
        button.setOnMouseEntered(e ->
                button.setStyle(originalStyle.replace("#f5f5f5", "#e0e0e0"))
        );
        button.setOnMouseExited(e ->
                button.setStyle(originalStyle)
        );
    }

    /**
     * 设置设备ID
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        deviceIdLabel.setText(maskDeviceId(deviceId));
    }
    /**
     * 遮罩设备ID
     */
    private String maskDeviceId(String deviceId) {
        if (deviceId.length() <= 8) return deviceId;
        return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
    }

    /**
     * 生成设备请求文件内容
     */
    private String generateDeviceRequestContent() {
        StringBuilder content = new StringBuilder();
        content.append("=== 设备激活请求文件 ===\n");
        content.append("设备ID: ").append(deviceId).append("\n");
        content.append("生成时间: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("操作系统: ").append(System.getProperty("os.name")).append("\n");
        content.append("Java版本: ").append(System.getProperty("java.version")).append("\n");
        content.append("\n请将此文件发送给米多客服以获取许可证文件。");
        return content.toString();
    }

    /**
     * 验证许可证文件
     */
    private boolean validateLicenseFile(File file) {
        try {
            // 这里添加实际的许可证验证逻辑
            // 目前简单验证文件是否存在且可读
            return file.exists() && file.canRead() && file.length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 打开文件所在位置
     */
    private void openFileLocation(File directory) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("explorer.exe /select," + directory.getAbsolutePath());
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                Runtime.getRuntime().exec("open " + directory.getAbsolutePath());
            } else {
                Runtime.getRuntime().exec("xdg-open " + directory.getAbsolutePath());
            }
        } catch (IOException e) {
            // 如果无法打开,静默失败
            e.printStackTrace();
        }
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * 显示提示对话框
     */
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 静态方法:显示离线激活对话框
     */
    public static void showOfflineActivationWizard(String deviceId) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    OfflineActivationController.class.getResource("/fxml/OfflineActivation.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // 设置控制器参数
            OfflineActivationController controller = loader.getController();
            if (controller != null && deviceId != null) {
                controller.setDeviceId(deviceId);
            }

            Stage dialogStage = new Stage();
            dialogStage.setTitle("离线激活向导");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            dialogStage.setScene(scene);
            dialogStage.sizeToScene();
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 静态方法:显示离线激活对话框
     */
    public static void showOfflineActivationWizard() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    OfflineActivationController.class.getResource("/fxml/OfflineActivation.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // 获取设备ID
            DeviceInfoService deviceInfoService = new DeviceInfoService();
            String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());

            // 设置控制器参数
            OfflineActivationController controller = loader.getController();
            if (controller != null && currentDeviceId != null) {
                controller.setDeviceId(currentDeviceId);
            }

            Stage dialogStage = new Stage();
            dialogStage.setTitle("离线激活向导");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            dialogStage.setScene(scene);
            dialogStage.sizeToScene();
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置激活完成回调
     */
    public void setOnActivationComplete(Runnable callback) {
        this.onActivationComplete = callback;
    }
}
