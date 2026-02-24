package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.StageIconUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static com.miduo.cloud.frontend.controller.OfflineActivationController.showOfflineActivationWizard;

/**
 * 试用即将到期提醒对话框控制器
 * 
 * 功能说明：
 * - 剩余2-3天时显示警告样式（橙色）
 * - 剩余1天时显示紧急样式（红色）
 * - 每次启动时检测并显示
 * - "立即激活"打开离线激活向导
 * - "稍后提醒"关闭弹窗，下次启动再提醒
 */
public class TrialExpiringDialogController {

    // 场景1：警告样式（剩余2-3天）
    @FXML
    private VBox sceneWarning;
    @FXML
    private Label countdownBadgeWarning;
    @FXML
    private Button closeButtonWarning;

    // 场景2：紧急样式（剩余1天）
    @FXML
    private VBox sceneUrgent;
    @FXML
    private Label countdownBadgeUrgent;
    @FXML
    private Button closeButtonUrgent;
    @FXML
    private StackPane urgentWarningIconPane;

    private long remainingDays = 3;

    /**
     * 场景类型枚举
     */
    public enum SceneType {
        WARNING,    // 警告样式（剩余2-3天）
        URGENT      // 紧急样式（剩余1天）
    }

    @FXML
    public void initialize() {
        // 初始化时默认显示警告场景
        showScene(SceneType.WARNING);
        // 加载紧急警告图标
        loadUrgentWarningIcon();
    }
    
    /**
     * 加载紧急警告SVG图标
     */
    private void loadUrgentWarningIcon() {
        Platform.runLater(() -> {
            if (urgentWarningIconPane != null) {
                loadSvgIconToPane(urgentWarningIconPane, "/icons/试用期明天到期icon.svg", Color.web("#F44336"), 14, 12);
            }
        });
    }
    
    /**
     * 加载SVG图标到StackPane
     */
    private void loadSvgIconToPane(StackPane iconPane, String svgPath, Color defaultColor, double targetWidth, double targetHeight) {
        try {
            if (iconPane == null) {
                return;
            }
            
            // 读取SVG文件内容
            String svgContent;
            try (java.io.InputStream svgStream = getClass().getResourceAsStream(svgPath)) {
                if (svgStream == null) {
                    System.err.println("[图标加载] 无法找到SVG文件: " + svgPath);
                    return;
                }
                
                try (java.util.Scanner scanner = new java.util.Scanner(svgStream, "UTF-8")) {
                    svgContent = scanner.useDelimiter("\\A").next();
                }
            }
            
            // 使用正则表达式提取path的d属性
            java.util.regex.Pattern pathPattern = java.util.regex.Pattern.compile(
                "<path[^>]*d=\"([^\"]+)\"[^>]*>", 
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher matcher = pathPattern.matcher(svgContent);
            
            if (matcher.find()) {
                String pathData = matcher.group(1);
                
                // 创建SVGPath节点
                SVGPath svgPathNode = new SVGPath();
                svgPathNode.setContent(pathData);
                svgPathNode.setFill(defaultColor);
                svgPathNode.setStrokeWidth(0);
                
                // 计算缩放比例（SVG viewBox是14x12）
                double scaleX = targetWidth / 14.0;
                double scaleY = targetHeight / 12.182;
                double scale = Math.min(scaleX, scaleY);
                
                // 将SVGPath放在Group中，并应用缩放
                Group iconGroup = new Group(svgPathNode);
                iconGroup.setScaleX(scale);
                iconGroup.setScaleY(scale);
                
                // 清空容器并添加图标
                iconPane.getChildren().clear();
                iconPane.getChildren().add(iconGroup);
            }
        } catch (Exception e) {
            System.err.println("[图标加载] 加载SVG图标失败: " + svgPath + ", " + e.getMessage());
        }
    }

    /**
     * 设置剩余天数并自动选择对应场景
     * @param days 剩余天数
     */
    public void setRemainingDays(long days) {
        this.remainingDays = days;
        
        if (days <= 1) {
            // 剩余1天，显示紧急样式
            showScene(SceneType.URGENT);
            if (countdownBadgeUrgent != null) {
                countdownBadgeUrgent.setText(String.valueOf(days));
            }
        } else {
            // 剩余2-3天，显示警告样式
            showScene(SceneType.WARNING);
            if (countdownBadgeWarning != null) {
                countdownBadgeWarning.setText(String.valueOf(days));
            }
        }
    }

    /**
     * 显示指定场景
     * @param sceneType 场景类型
     */
    public void showScene(SceneType sceneType) {
        // 隐藏所有场景
        if (sceneWarning != null) {
            sceneWarning.setVisible(false);
            sceneWarning.setManaged(false);
        }
        if (sceneUrgent != null) {
            sceneUrgent.setVisible(false);
            sceneUrgent.setManaged(false);
        }

        // 显示对应场景
        switch (sceneType) {
            case WARNING:
                if (sceneWarning != null) {
                    sceneWarning.setVisible(true);
                    sceneWarning.setManaged(true);
                }
                break;
            case URGENT:
                if (sceneUrgent != null) {
                    sceneUrgent.setVisible(true);
                    sceneUrgent.setManaged(true);
                }
                break;
        }
    }

    /**
     * 立即激活按钮点击事件
     */
    @FXML
    private void onActivateClick() {
        // 关闭当前对话框
        //closeDialog();
        
        // 获取当前窗口作为 owner
        Stage ownerStage = null;
        if (sceneWarning != null && sceneWarning.getScene() != null) {
            ownerStage = (Stage) sceneWarning.getScene().getWindow();
        } else if (sceneUrgent != null && sceneUrgent.getScene() != null) {
            ownerStage = (Stage) sceneUrgent.getScene().getWindow();
        }
        
        // 打开离线激活向导
        try {
            if (ownerStage != null) {
                OfflineActivationController.showOfflineActivationWizard(ownerStage);
            } else {
                showOfflineActivationWizard();
            }
            closeDialog();
        } catch (Exception e) {
            System.err.println("打开离线激活向导失败: " + e.getMessage());
            e.printStackTrace();
            // 尝试无参数调用
            showOfflineActivationWizard();
        }
    }

    /**
     * 稍后提醒按钮点击事件（关闭按钮也调用此方法）
     */
    @FXML
    private void onLaterRemind() {
        // 只关闭弹窗，下次启动再提醒
        closeDialog();
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = null;
        if (sceneWarning != null && sceneWarning.getScene() != null) {
            stage = (Stage) sceneWarning.getScene().getWindow();
        } else if (sceneUrgent != null && sceneUrgent.getScene() != null) {
            stage = (Stage) sceneUrgent.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    // ======================== 静态方法 ========================

    /**
     * 检查是否需要显示试用即将到期提醒
     * @param currentDeviceId 当前设备ID
     * @return 是否需要显示（试用类型且剩余天数 <= 3）
     */
    public static boolean shouldShowExpiringReminder(String currentDeviceId) {
        try {
            LicenseService licenseService = new LicenseService(new LicenseValidationService());
            licenseService.init();
            
            LicenseService.LicenseInfo licenseInfo = licenseService.getLicenseInfo(currentDeviceId);
            
            if (licenseInfo == null || !licenseInfo.isLicenseExists()) {
                return false;
            }
            
            // 检查是否为试用类型
            String licenseType = licenseInfo.getLicenseType();
            if (!"trial".equalsIgnoreCase(licenseType)) {
                return false;
            }
            
            // 检查剩余天数
            long remainingDays = licenseInfo.getRemainingDays();
            return remainingDays > 0 && remainingDays <= 3;
            
        } catch (Exception e) {
            System.err.println("检查试用到期提醒失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取剩余天数
     * @param currentDeviceId 当前设备ID
     * @return 剩余天数
     */
    public static long getRemainingDays(String currentDeviceId) {
        try {
            LicenseService licenseService = new LicenseService(new LicenseValidationService());
            licenseService.init();
            return licenseService.getRemainingDays(currentDeviceId);
        } catch (Exception e) {
            System.err.println("获取剩余天数失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 静态方法：显示试用即将到期提醒对话框
     * @param remainingDays 剩余天数
     */
    public static void showTrialExpiringDialog(long remainingDays) {
        try {
            FXMLLoader loader = new FXMLLoader(
                TrialExpiringDialogController.class.getResource("/fxml/TrialExpiringDialog.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置剩余天数
            TrialExpiringDialogController controller = loader.getController();
            controller.setRemainingDays(remainingDays);

            Scene scene = new Scene(root);
            //scene.setFill(Color.TRANSPARENT);// 设置场景背景为透明
            scene.getStylesheets().add(
                TrialExpiringDialogController.class.getResource("/css/expired-style.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("试用即将到期");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.initStyle(StageStyle.TRANSPARENT);  // 改为透明样式
            StageIconUtil.setStageIcon(stage);

            // 允许关闭（等同于"稍后提醒"）
            stage.setOnCloseRequest(e -> {
                // 不阻止关闭，让用户可以关闭弹窗继续使用
            });

            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("显示试用即将到期提醒失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}



