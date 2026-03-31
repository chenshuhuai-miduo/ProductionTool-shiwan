package com.miduo.cloud.frontend.controller;//package com.example.app;

import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.StageIconUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.miduo.cloud.frontend.controller.OfflineActivationController.showOfflineActivationWizard;

/**
 * 许可证对话框控制器
 * 负责：
 * - 场景切换逻辑
 * - 按钮事件处理
 *
 * 不负责：
 * - 样式定义（所有样式在FXML中定义）
 * - 颜色、字体配置（在FXML样式中）
 */
public class TrialExpireDialogController {

    // 场景容器 - 与 FXML 中的 fx:id 匹配
    @FXML
    private VBox sceneLicense;      // 场景1：正式授权过期

    @FXML
    private VBox sceneTrial;        // 场景2：试用已过期（可续）

    @FXML
    private VBox sceneTrialLimit;   // 场景3：试用已过期（已达上限）

    // 过期时间显示标签
    @FXML
    private Label expireDateLabel;
    
    // 试用次数相关标签
    @FXML
    private Label trialCountLabel;
    @FXML
    private Label trialHintLabel;

    /**
     * 场景类型枚举
     */
    public enum SceneType {
        LICENSE_EXPIRED,      // 正式授权过期
        TRIAL_EXPIRED,        // 试用过期（可续）
        TRIAL_LIMIT           // 试用过期（已达上限）
    }

    @FXML
    public void initialize() {
        // 初始化时显示第一个场景
        showScene(SceneType.TRIAL_EXPIRED);
    }

    /**
     * 显示指定场景
     * @param sceneType 场景类型
     */
    public void showScene(SceneType sceneType) {
        // 隐藏所有场景容器
        if (sceneLicense != null) {
            sceneLicense.setVisible(false);
            sceneLicense.setManaged(false);
        }
        if (sceneTrial != null) {
            sceneTrial.setVisible(false);
            sceneTrial.setManaged(false);
        }
        if (sceneTrialLimit != null) {
            sceneTrialLimit.setVisible(false);
            sceneTrialLimit.setManaged(false);
        }

        // 显示对应场景
        switch (sceneType) {
            case LICENSE_EXPIRED:
                showLicenseExpiredScene();
                break;
            case TRIAL_EXPIRED:
                showTrialExpiredScene();
                break;
            case TRIAL_LIMIT:
                showTrialLimitScene();
                break;
        }
    }

    /**
     * 场景1：正式授权过期
     */
    private void showLicenseExpiredScene() {
        if (sceneLicense != null) {
            sceneLicense.setVisible(true);
            sceneLicense.setManaged(true);
        }
    }

    /**
     * 场景2：试用已过期（可续）
     */
    private void showTrialExpiredScene() {
        if (sceneTrial != null) {
            sceneTrial.setVisible(true);
            sceneTrial.setManaged(true);
        }
    }

    /**
     * 场景3：试用已过期（已达上限）
     */
    private void showTrialLimitScene() {
        if (sceneTrialLimit != null) {
            sceneTrialLimit.setVisible(true);
            sceneTrialLimit.setManaged(true);
        }
    }

    // ======================== 数据设置方法 ========================

    /**
     * 设置过期时间显示
     * @param expireDate 过期日期
     */
    public void setExpireDate(LocalDate expireDate) {
        if (expireDateLabel != null && expireDate != null) {
            String formattedDate = expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            expireDateLabel.setText("过期时间：" + formattedDate);
        }
    }
    
    /**
     * 设置试用次数信息
     * @param currentCount 当前次数
     * @param maxCount 最大次数
     */
    public void setTrialCount(int currentCount, int maxCount) {
        if (trialCountLabel != null) {
            trialCountLabel.setText("（当前第" + currentCount + "次/最多" + maxCount + "次）");
        }
        if (trialHintLabel != null) {
            int remaining = maxCount - currentCount;
            if (remaining > 0) {
                trialHintLabel.setText("可续试用(剩余" + remaining + "次)或购买正式许可");
            } else {
                trialHintLabel.setText("试用已达上限，如需继续使用请购买正式许可");
            }
        }
    }

    // ======================== 按钮事件处理 ========================

    /**
     * 获取当前窗口作为 owner
     */
    private Stage getOwnerStage() {
        if (sceneLicense != null && sceneLicense.getScene() != null) {
            return (Stage) sceneLicense.getScene().getWindow();
        } else if (sceneTrial != null && sceneTrial.getScene() != null) {
            return (Stage) sceneTrial.getScene().getWindow();
        } else if (sceneTrialLimit != null && sceneTrialLimit.getScene() != null) {
            return (Stage) sceneTrialLimit.getScene().getWindow();
        }
        return null;
    }

    /**
     * 续期按钮点击事件
     */
    @FXML
    private void onRenewClick() {
        Stage ownerStage = getOwnerStage();
        if (ownerStage != null) {
            OfflineActivationController.showOfflineActivationWizard(ownerStage);
        } else {
            showOfflineActivationWizard();
        }
    }

    /**
     * 续试用按钮点击事件
     */
    @FXML
    private void onTrialClick() {
        Stage ownerStage = getOwnerStage();
        if (ownerStage != null) {
            OfflineActivationController.showOfflineActivationWizard(ownerStage);
        } else {
            showOfflineActivationWizard();
        }
    }

    /**
     * 激活正式版按钮点击事件
     */
    @FXML
    private void onActivateClick() {
        Stage ownerStage = getOwnerStage();
        if (ownerStage != null) {
            OfflineActivationController.showOfflineActivationWizard(ownerStage);
        } else {
            showOfflineActivationWizard();
        }
    }

    /**
     * 退出软件按钮点击事件
     */
    @FXML
    private void onExitClick() {
        System.exit(0);
    }

    // ======================== 静态方法 ========================

    /**
     * 静态方法：显示试用/授权过期对话框
     * @param status 许可证状态
     */
    public static void showTrialExpireDialog(LicenseStatusEnum status) {
        try {
            FXMLLoader loader = new FXMLLoader(
                TrialExpireDialogController.class.getResource("/fxml/TrialExpireDialog.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置场景
            TrialExpireDialogController controller = loader.getController();

            // 读取许可证信息以判断场景
            try {
                DeviceInfoService deviceInfoService = new DeviceInfoService();
                LicenseService licenseService = new LicenseService(new LicenseValidationService());
                licenseService.init();
                
                String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());
                
                // 读取许可证验证结果以获取 payload
                LicenseValidationService.LicenseValidationResult validationResult = 
                    licenseService.readLicense(currentDeviceId);
                
                LicenseService.LicenseInfo licenseInfo = licenseService.getLicenseInfo(currentDeviceId);
                
                // 根据状态选择场景
                if (status == LicenseStatusEnum.EXPIRED) {
                    controller.showScene(SceneType.LICENSE_EXPIRED);
                } else if (status == LicenseStatusEnum.TRIAL_EXPIRED) {
                    // 检查是否为续期试用且已过期
                    if (validationResult.isValid() && validationResult.getPayload() != null) {
                        com.miduo.cloud.entity.dto.license.LicensePayload payload = validationResult.getPayload();
                        String licenseType = payload.getLicenseType();
                        String activationType = payload.getActivationType();
                        
                        // 如果是试用许可证且激活类型为续期（renewal），说明已达上限
                        if ("trial".equalsIgnoreCase(licenseType) && "renewal".equalsIgnoreCase(activationType)) {
                            controller.showScene(SceneType.TRIAL_LIMIT);
                        } else {
                            // 首次试用过期，可以续试用
                            controller.showScene(SceneType.TRIAL_EXPIRED);
                        }
                    } else {
                        // 无法获取 payload，默认显示可续试用场景
                        controller.showScene(SceneType.TRIAL_EXPIRED);
                    }
                }
                
                // 设置过期时间显示
                if (licenseInfo != null && licenseInfo.getExpireDate() != null) {
                    controller.setExpireDate(licenseInfo.getExpireDate());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // 异常情况下，根据状态默认显示场景
                if (status == LicenseStatusEnum.EXPIRED) {
                    controller.showScene(SceneType.LICENSE_EXPIRED);
                } else {
                    controller.showScene(SceneType.TRIAL_EXPIRED);
                }
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    TrialExpireDialogController.class.getResource("/css/expired-style.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("许可证提示");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);  // 改为透明样式
            stage.setResizable(false);
            StageIconUtil.setStageIcon(stage);

            // 拦截关闭请求：须以当前许可证状态为准（用户可能刚在向导中完成续期/激活）
            stage.setOnCloseRequest(e -> {
                e.consume();
                try {
                    DeviceInfoService deviceInfoService = new DeviceInfoService();
                    LicenseService licenseService = new LicenseService(new LicenseValidationService());
                    licenseService.init();
                    String deviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());
                    LicenseStatusEnum current = licenseService.getCurrentLicenseStatus(deviceId);
                    if (current == LicenseStatusEnum.ACTIVATED || current == LicenseStatusEnum.TRIAL_ACTIVE) {
                        stage.close();
                        javafx.stage.Window.getWindows().stream()
                            .filter(window -> window instanceof Stage)
                            .map(window -> (Stage) window)
                            .filter(s -> {
                                String title = s.getTitle();
                                return title != null && title.contains("米多赋码采集关联系统");
                            })
                            .findFirst()
                            .ifPresent(mainStage -> {
                                if (!mainStage.isShowing()) {
                                    mainStage.show();
                                }
                                mainStage.toFront();
                            });
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                Platform.exit();
                System.exit(0);
            });

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

