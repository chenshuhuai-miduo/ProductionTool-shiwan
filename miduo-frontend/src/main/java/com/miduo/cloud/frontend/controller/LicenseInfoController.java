package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.SpringContextUtil;
import com.miduo.cloud.frontend.util.StageIconUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import static com.miduo.cloud.frontend.controller.OfflineActivationController.showOfflineActivationWizard;

/**
 * 许可证信息页面控制器
 */
@Slf4j
public class LicenseInfoController {

    // 原有的控制
    @FXML private Label statusLabel;
    @FXML private Label deviceIdLabel;
    @FXML private Label deviceModelLabel;
    @FXML private Label osVersionLabel;
    @FXML private Label appVersionLabel;
    @FXML private Label licenseTypeLabel;
    @FXML private Label activationDateLabel;
    @FXML private Label expireDateLabel;
    @FXML private Label remainingDaysLabel;
    @FXML private Label licenseKeyLabel;
    @FXML private Button activateButton;
    @FXML private Button renewButton;
    @FXML private Button refreshButton;

    // 新增的控件
    @FXML private Label statusHintLabel;        // 状态提示标签（显示剩余天数）
    @FXML private Region statusDot;             // 状态指示点
    @FXML private VBox licenseInfoCard;         // 授权信息卡片容器

    // 各个场景面板
    @FXML private VBox sceneUnactivated;
    @FXML private VBox sceneTrial;
    @FXML private VBox sceneTrialExpired;
    @FXML private VBox sceneActivated;
    @FXML private VBox sceneExpired;

    // 未激活场景的Label
    @FXML private Label deviceIdLabelUnactivated;
    @FXML private Label deviceModelLabelUnactivated;
    @FXML private Label manufacturerLabelUnactivated;
    @FXML private Label osVersionLabelUnactivated;

    // 试用期内场景的Label
    @FXML private Label trialStatusLabel;
    @FXML private Label deviceIdLabelTrial;
    @FXML private Label deviceModelLabelTrial;
    @FXML private Label manufacturerLabelTrial;
    @FXML private Label osVersionLabelTrial;

    // 试用已过期场景的Label
    @FXML private Label deviceIdLabelTrialExpired;
    @FXML private Label deviceModelLabelTrialExpired;
    @FXML private Label manufacturerLabelTrialExpired;
    @FXML private Label osVersionLabelTrialExpired;

    // 已激活场景的Label
    @FXML private Label activatedStatusLabel;
    @FXML private Label activationDateLabelActivated;
    @FXML private Label expireDateLabelActivated;
    @FXML private Label validDaysLabelActivated;
    @FXML private Label deviceIdLabelActivated;
    @FXML private Label deviceModelLabelActivated;
    @FXML private Label manufacturerLabelActivated;
    @FXML private Label osVersionLabelActivated;

    // 已过期场景的Label
    @FXML private Label activationDateLabelExpired;
    @FXML private Label expireDateLabelExpired;
    @FXML private Label expiredDaysLabelExpired;
    @FXML private Label deviceIdLabelExpired;
    @FXML private Label deviceModelLabelExpired;
    @FXML private Label manufacturerLabelExpired;
    @FXML private Label osVersionLabelExpired;

    private DeviceInfoService deviceInfoService = new DeviceInfoService();
    private LicenseService licenseService = new LicenseService(new LicenseValidationService());
    private String currentDeviceId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 颜色常量
    private static final String COLOR_INFO = "#2196F3";      // 信息色（蓝色）- 试用中
    private static final String COLOR_SUCCESS = "#4CAF50";   // 成功色（绿色）- 已激活
    private static final String COLOR_WARNING = "#FF9800";   // 警告色（橙色）- 即将过期
    private static final String COLOR_ERROR = "#F44336";     // 错误色（红色）- 已过期
    private static final String COLOR_TEXT_PRIMARY = "#212121";
    private static final String COLOR_TEXT_MUTED = "#9E9E9E";

    @FXML
    public void initialize() {
        log.info("许可证信息页面初始化");

        if (deviceInfoService != null) {
            // 获取当前设备ID
            currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());
        }
        if (licenseService != null) {
            licenseService.init();
        }
        // 初始化新增控件（如果存在）
        initializeNewControls();

        // 加载许可证信息
        loadLicenseInfo();
    }

    /**
     * 初始化新增的UI控件
     */
    private void initializeNewControls() {
        // 如果新控件不存在（使用旧版FXML），则不需要初始化
        if (statusHintLabel != null) {
            statusHintLabel.setText("");
        }

        if (licenseInfoCard != null) {
            // 默认隐藏授权信息卡片
            licenseInfoCard.setVisible(false);
            licenseInfoCard.setManaged(false);
        }
    }

    /**
     * 加载许可证信息
     */
    private void loadLicenseInfo() {
        if (deviceInfoService == null || licenseService == null) {
            showError("服务初始化失败");
            return;
        }

        try {
            // 获取设备信息
            DeviceInfoService.DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo();

            // 获取许可证信息
            LicenseService.LicenseInfo licenseInfo = licenseService.getLicenseInfo(currentDeviceId);

            // 显示设备信息和许可证信息（兼容旧版Label）
            Platform.runLater(() -> {
                // 更新旧版Label（如果存在，用于向后兼容）
                if (deviceIdLabel != null) deviceIdLabel.setText(formatDeviceId(currentDeviceId));
                if (deviceModelLabel != null) deviceModelLabel.setText(deviceInfo.getManufacturer() + " " + deviceInfo.getDeviceModel());
                if (osVersionLabel != null) osVersionLabel.setText(deviceInfo.getOsVersion());
                if (appVersionLabel != null) appVersionLabel.setText(deviceInfo.getAppVersion());

                // 更新许可证信息显示（包括新版场景数据）
                updateLicenseDisplay(licenseInfo);
            });

        } catch (Exception e) {
            log.error("加载许可证信息失败", e);
            showError("加载许可证信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新许可证信息显示
     */
    private void updateLicenseDisplay(LicenseService.LicenseInfo licenseInfo) {
        LicenseStatusEnum status = licenseInfo.getStatus();

        // 状态标签
        if (statusLabel != null) {
            statusLabel.setText(status.getDescription());

            // 兼容旧版样式类
            statusLabel.getStyleClass().removeAll("license-status-normal", "license-status-warning", "license-status-error");
            switch (status) {
                case ACTIVATED:
                    statusLabel.getStyleClass().add("license-status-normal");
                    break;
                case TRIAL_ACTIVE:
                    statusLabel.getStyleClass().add("license-status-normal");
                    break;
                case TRIAL_EXPIRED:
                case EXPIRED:
                    statusLabel.getStyleClass().add("license-status-error");
                    break;
                case UNACTIVATED:
                default:
                    statusLabel.getStyleClass().add("license-status-error");
                    break;
            }
        }

        // 更新新版UI元素（状态指示点和提示）
        updateStatusIndicator(status, licenseInfo);

        // 根据状态显示对应场景
        showScene(status);
        
        // 获取设备信息并更新场景数据
        DeviceInfoService.DeviceInfo deviceInfo = deviceInfoService.getDeviceInfo();
        updateSceneData(status, deviceInfo, licenseInfo);

        // 许可证信息
        boolean hasLicense = licenseInfo.isLicenseExists() && licenseInfo.getLicenseType() != null;

        if (hasLicense) {
            licenseTypeLabel.setText(licenseInfo.getLicenseType().equals("TRIAL") ? "试用版" : "正式版");
            licenseKeyLabel.setText(licenseInfo.getLicenseKey() != null ? licenseInfo.getLicenseKey() : "无");

            if (licenseInfo.getActivationDate() != null) {
                activationDateLabel.setText(licenseInfo.getActivationDate().format(DATE_TIME_FORMATTER));
            } else {
                activationDateLabel.setText("无");
            }

            if (licenseInfo.getExpireDate() != null) {
                expireDateLabel.setText(licenseInfo.getExpireDate().format(DATE_FORMATTER));

                // 更新剩余天数显示
                updateRemainingDaysDisplay(licenseInfo.getRemainingDays(), status);
            } else {
                expireDateLabel.setText("无");
                remainingDaysLabel.setText("无");
            }
        } else {
            // 无许可证
            licenseTypeLabel.setText("无");
            licenseKeyLabel.setText("无");
            activationDateLabel.setText("无");
            expireDateLabel.setText("无");
            remainingDaysLabel.setText("无");
        }

        // 控制授权信息卡片的显示
        updateLicenseInfoCardVisibility(status, hasLicense);

        // 更新按钮状态
        updateButtonStates(status);
    }

    /**
     * 更新场景数据（设备信息和授权信息）
     */
    private void updateSceneData(LicenseStatusEnum status, DeviceInfoService.DeviceInfo deviceInfo, 
                                  LicenseService.LicenseInfo licenseInfo) {
        // 更新设备信息（所有场景都需要）
        updateDeviceInfoForScene(status, deviceInfo);
        
        // 更新授权信息和状态提示（根据场景不同）
        updateLicenseInfoForScene(status, licenseInfo);
    }

    /**
     * 更新指定场景的设备信息
     */
    private void updateDeviceInfoForScene(LicenseStatusEnum status, DeviceInfoService.DeviceInfo deviceInfo) {
        String formattedDeviceId = formatDeviceId(currentDeviceId);
        String deviceModel = deviceInfo.getManufacturer() + " " + deviceInfo.getDeviceModel();
        String manufacturer = deviceInfo.getManufacturer();
        String osVersion = deviceInfo.getOsVersion();

        switch (status) {
            case UNACTIVATED:
                if (deviceIdLabelUnactivated != null) deviceIdLabelUnactivated.setText(formattedDeviceId);
                if (deviceModelLabelUnactivated != null) deviceModelLabelUnactivated.setText(deviceModel);
                if (manufacturerLabelUnactivated != null) manufacturerLabelUnactivated.setText(manufacturer);
                if (osVersionLabelUnactivated != null) osVersionLabelUnactivated.setText(osVersion);
                break;
            case TRIAL_ACTIVE:
                if (deviceIdLabelTrial != null) deviceIdLabelTrial.setText(formattedDeviceId);
                if (deviceModelLabelTrial != null) deviceModelLabelTrial.setText(deviceModel);
                if (manufacturerLabelTrial != null) manufacturerLabelTrial.setText(manufacturer);
                if (osVersionLabelTrial != null) osVersionLabelTrial.setText(osVersion);
                break;
            case TRIAL_EXPIRED:
                if (deviceIdLabelTrialExpired != null) deviceIdLabelTrialExpired.setText(formattedDeviceId);
                if (deviceModelLabelTrialExpired != null) deviceModelLabelTrialExpired.setText(deviceModel);
                if (manufacturerLabelTrialExpired != null) manufacturerLabelTrialExpired.setText(manufacturer);
                if (osVersionLabelTrialExpired != null) osVersionLabelTrialExpired.setText(osVersion);
                break;
            case ACTIVATED:
                if (deviceIdLabelActivated != null) deviceIdLabelActivated.setText(formattedDeviceId);
                if (deviceModelLabelActivated != null) deviceModelLabelActivated.setText(deviceModel);
                if (manufacturerLabelActivated != null) manufacturerLabelActivated.setText(manufacturer);
                if (osVersionLabelActivated != null) osVersionLabelActivated.setText(osVersion);
                break;
            case EXPIRED:
                if (deviceIdLabelExpired != null) deviceIdLabelExpired.setText(formattedDeviceId);
                if (deviceModelLabelExpired != null) deviceModelLabelExpired.setText(deviceModel);
                if (manufacturerLabelExpired != null) manufacturerLabelExpired.setText(manufacturer);
                if (osVersionLabelExpired != null) osVersionLabelExpired.setText(osVersion);
                break;
        }
    }

    /**
     * 更新指定场景的授权信息
     */
    private void updateLicenseInfoForScene(LicenseStatusEnum status, LicenseService.LicenseInfo licenseInfo) {
        switch (status) {
            case UNACTIVATED:
                // 未激活场景不需要显示授权信息
                break;
            case TRIAL_ACTIVE:
                long remainingDays = licenseInfo.getRemainingDays();
                // 更新试用状态标题
                if (trialStatusLabel != null) {
                    if (remainingDays > 0) {
                        trialStatusLabel.setText("试用中，试用剩余: " + remainingDays + "天");
                    } else {
                        trialStatusLabel.setText("试用中，试用剩余: 不足1天");
                    }
                }
                break;
            case TRIAL_EXPIRED:
                // 试用已过期场景不需要显示授权信息
                break;
            case ACTIVATED:
                // 更新已激活场景的授权信息
                updateActivatedLicenseInfo(licenseInfo);
                break;
            case EXPIRED:
                // 更新已过期场景的授权信息
                updateExpiredLicenseInfo(licenseInfo);
                break;
        }
    }

    /**
     * 更新已激活场景的授权信息
     */
    private void updateActivatedLicenseInfo(LicenseService.LicenseInfo licenseInfo) {
        // 更新已激活状态标题
        if (activatedStatusLabel != null) {
            long remainingDays = licenseInfo.getRemainingDays();
            if (remainingDays > 0) {
                activatedStatusLabel.setText("已激活，剩余" + remainingDays + "天");
            } else if (remainingDays == 0) {
                activatedStatusLabel.setText("已激活，剩余不足1天");
            } else {
                activatedStatusLabel.setText("已激活");
            }
        }

        // 更新激活时间
        if (activationDateLabelActivated != null) {
            if (licenseInfo.getActivationDate() != null) {
                activationDateLabelActivated.setText(licenseInfo.getActivationDate().format(DATE_TIME_FORMATTER));
            } else {
                activationDateLabelActivated.setText("无");
            }
        }

        // 更新到期时间
        if (expireDateLabelActivated != null) {
            if (licenseInfo.getExpireDate() != null) {
                expireDateLabelActivated.setText(licenseInfo.getExpireDate().format(DATE_FORMATTER));
            } else {
                expireDateLabelActivated.setText("无");
            }
        }

        // 更新有效期
        if (validDaysLabelActivated != null) {
            if (licenseInfo.getValidDays() != null) {
                validDaysLabelActivated.setText(licenseInfo.getValidDays() + "天");
            } else if (licenseInfo.getExpireDate() != null && licenseInfo.getActivationDate() != null) {
                // 如果没有有效天数，计算激活日期到到期日期的天数
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    licenseInfo.getActivationDate().toLocalDate(), 
                    licenseInfo.getExpireDate()
                );
                validDaysLabelActivated.setText(days + "天");
            } else {
                validDaysLabelActivated.setText("无");
            }
        }
    }

    /**
     * 更新已过期场景的授权信息
     */
    private void updateExpiredLicenseInfo(LicenseService.LicenseInfo licenseInfo) {
        // 更新激活时间
        if (activationDateLabelExpired != null) {
            if (licenseInfo.getActivationDate() != null) {
                activationDateLabelExpired.setText(licenseInfo.getActivationDate().format(DATE_TIME_FORMATTER));
            } else {
                activationDateLabelExpired.setText("无");
            }
        }

        // 更新到期时间
        if (expireDateLabelExpired != null) {
            if (licenseInfo.getExpireDate() != null) {
                expireDateLabelExpired.setText(licenseInfo.getExpireDate().format(DATE_FORMATTER));
            } else {
                expireDateLabelExpired.setText("无");
            }
        }

        // 更新过期天数
        if (expiredDaysLabelExpired != null) {
            long remainingDays = licenseInfo.getRemainingDays();
            if (remainingDays < 0) {
                expiredDaysLabelExpired.setText("已过期" + Math.abs(remainingDays) + "天");
            } else {
                expiredDaysLabelExpired.setText("已过期");
            }
        }
    }

    /**
     * 更新状态指示点和状态提示
     */
    private void updateStatusIndicator(LicenseStatusEnum status, LicenseService.LicenseInfo licenseInfo) {
        if (statusDot == null || statusHintLabel == null) {
            return; // 如果是旧版FXML，跳过
        }

        String dotColor;
        String hintText = "";
        String hintColor = COLOR_TEXT_MUTED;

        switch (status) {
            case TRIAL_ACTIVE:
                // 试用中 - 蓝色
                dotColor = COLOR_INFO;
                long trialRemainingDays = licenseInfo.getRemainingDays();
                if (trialRemainingDays > 0) {
                    hintText = "试用剩余: " + trialRemainingDays + "天";
                    hintColor = COLOR_INFO;
                } else if (trialRemainingDays == 0) {
                    hintText = "试用剩余: 不足1天";
                    hintColor = COLOR_INFO;
                }
                break;

            case ACTIVATED:
                // 已激活 - 绿色/橙色/红色（根据剩余天数）
                long remainingDays = licenseInfo.getRemainingDays();
                if (remainingDays > 30) {
                    dotColor = COLOR_SUCCESS;
                    hintText = "剩余" + remainingDays + "天";
                    hintColor = COLOR_SUCCESS;
                } else if (remainingDays > 7) {
                    dotColor = COLOR_SUCCESS;
                    hintText = "剩余" + remainingDays + "天";
                    hintColor = COLOR_WARNING;
                } else if (remainingDays > 0) {
                    dotColor = COLOR_WARNING;
                    hintText = "剩余" + remainingDays + "天";
                    hintColor = COLOR_ERROR;
                } else {
                    // remainingDays == 0
                    dotColor = COLOR_WARNING;
                    hintText = "剩余不足1天";
                    hintColor = COLOR_ERROR;
                }
                break;

            case TRIAL_EXPIRED:
            case EXPIRED:
                // 已过期 - 红色
                dotColor = COLOR_ERROR;
                hintText = "";
                break;

            case UNACTIVATED:
            default:
                // 未激活 - 红色
                dotColor = COLOR_ERROR;
                hintText = "";
                break;
        }

        // 设置状态指示点颜色
        updateStatusDotColor(dotColor);

        // 设置状态提示文字和颜色
        statusHintLabel.setText(hintText);
        statusHintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + hintColor + ";");
    }

    /**
     * 更新状态指示点颜色
     */
    private void updateStatusDotColor(String color) {
        if (statusDot != null) {
            statusDot.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-background-radius: 5; " +
                            "-fx-min-width: 10; " +
                            "-fx-max-width: 10; " +
                            "-fx-min-height: 10; " +
                            "-fx-max-height: 10;"
            );
        }
    }

    /**
     * 更新剩余天数显示（带颜色）
     */
    private void updateRemainingDaysDisplay(long remainingDays, LicenseStatusEnum status) {
        if (remainingDays >= 0) {
            // 未过期
            // 如果剩余天数为0，显示"不足1天"
            if (remainingDays == 0) {
                remainingDaysLabel.setText("不足1天");
            } else {
                remainingDaysLabel.setText(remainingDays + "天");
            }

            // 根据剩余天数设置颜色
            String color;
            if (remainingDays > 30) {
                color = COLOR_TEXT_PRIMARY;
            } else if (remainingDays > 7) {
                color = COLOR_WARNING;
            } else {
                color = COLOR_ERROR;
            }
            remainingDaysLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");

            // 到期日期使用普通颜色
            expireDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT_PRIMARY + ";");

        } else {
            // 已过期
            remainingDaysLabel.setText("已过期" + Math.abs(remainingDays) + "天");
            remainingDaysLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_ERROR + "; -fx-font-weight: 500;");

            // 到期日期标红
            expireDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_ERROR + ";");
        }
    }

    /**
     * 控制授权信息卡片的显示/隐藏
     */
    private void updateLicenseInfoCardVisibility(LicenseStatusEnum status, boolean hasLicense) {
        if (licenseInfoCard == null) {
            return; // 如果是旧版FXML，跳过
        }

        // 只在已激活或已过期状态显示授权信息卡片
        boolean shouldShow = hasLicense && (
                status == LicenseStatusEnum.ACTIVATED ||
                        status == LicenseStatusEnum.EXPIRED
        );

        licenseInfoCard.setVisible(shouldShow);
        licenseInfoCard.setManaged(shouldShow);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates(LicenseStatusEnum status) {
        switch (status) {
            case UNACTIVATED:
                activateButton.setVisible(true);
                activateButton.setManaged(true);
                activateButton.setText("离线激活");
                renewButton.setVisible(false);
                renewButton.setManaged(false);
                break;

            case TRIAL_ACTIVE:
                activateButton.setVisible(true);
                activateButton.setManaged(true);
                activateButton.setText("升级正式版");
                renewButton.setVisible(false);
                renewButton.setManaged(false);
                break;

            case ACTIVATED:
                activateButton.setVisible(false);
                activateButton.setManaged(false);
                renewButton.setVisible(true);
                renewButton.setManaged(true);
                renewButton.setText("续期");
                break;

            case TRIAL_EXPIRED:
                activateButton.setVisible(true);
                activateButton.setManaged(true);
                activateButton.setText("续试用");
                renewButton.setVisible(true);
                renewButton.setManaged(true);
                renewButton.setText("升级正式版");
                break;

            case EXPIRED:
                activateButton.setVisible(true);
                activateButton.setManaged(true);
                activateButton.setText("续期");
                renewButton.setVisible(false);
                renewButton.setManaged(false);
                break;
        }
    }

    /**
     * 激活按钮点击事件
     */
    @FXML
    private void onActivate() {
        try {
            // 打开离线激活向导
            openOfflineActivationWizard();
        } catch (Exception e) {
            log.error("打开激活向导失败", e);
            showError("打开激活向导失败: " + e.getMessage());
        }
    }

    /**
     * 续期按钮点击事件
     */
    @FXML
    private void onRenew() {
        try {
            // 打开离线激活向导（续期流程）
            openOfflineActivationWizard();
        } catch (Exception e) {
            log.error("打开续期向导失败", e);
            showError("打开续期向导失败: " + e.getMessage());
        }
    }

    /**
     * 刷新按钮点击事件
     */
    @FXML
    private void onRefresh() {
        loadLicenseInfo();
    }

    /**
     * 关闭按钮点击事件
     */
    @FXML
    private void onClose(ActionEvent event) {
        // 通过事件源获取按钮，再获取 Stage
        Button button = (Button) event.getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }

    /**
     * 打开离线激活向导
     */
    private void openOfflineActivationWizard() {
         showOfflineActivationWizard();
    }

    /**
     * 通知主界面更新许可证状态
     */
    private void updateMainWindowLicenseStatus() {
        try {
            MainController mainController = MainController.getCurrentInstance();
            if (mainController != null) {
                mainController.updateLicenseStatus();
            }
        } catch (Exception e) {
            // 处理异常
        }
    }

    /**
     * 格式化设备ID显示（部分隐藏）
     */
    private String formatDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() < 8) {
            return deviceId;
        }
        return deviceId.substring(0, 8) + "****" + deviceId.substring(deviceId.length() - 4);
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("操作失败");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 静态方法：打开许可证信息页面
     */
    public static void showLicenseInfo() {
        try {
            FXMLLoader loader = new FXMLLoader(LicenseInfoController.class.getResource("/fxml/LicenseInfo.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("许可证信息");
            Scene scene = new Scene(root, 800, 900);

            // 加载CSS样式（如果使用CSS版本）
            // 注释掉这行则使用内联样式版本
            try {
                scene.getStylesheets().add(
                        LicenseInfoController.class.getResource("/css/liceseInfo-style.css").toExternalForm()
                );
            } catch (Exception e) {
                log.warn("CSS文件未找到，使用内联样式");
            }

            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            StageIconUtil.setStageIcon(stage);
            stage.showAndWait();

        } catch (IOException e) {
            log.error("打开许可证信息页面失败", e);
        }
    }

    /**
     * 外部调用的公共方法 - 根据状态切换场景
     * @param status 许可证状态
     */
    public void showScene(LicenseStatusEnum status) {
        // 隐藏所有场景
        hideAllScenes();

        // 根据状态显示对应场景
        switch (status) {
            case UNACTIVATED:
                sceneUnactivated.setVisible(true);
                sceneUnactivated.setManaged(true);
                break;
            case TRIAL_ACTIVE:
                sceneTrial.setVisible(true);
                sceneTrial.setManaged(true);
                break;
            case TRIAL_EXPIRED:
                sceneTrialExpired.setVisible(true);
                sceneTrialExpired.setManaged(true);
                break;
            case ACTIVATED:
                sceneActivated.setVisible(true);
                sceneActivated.setManaged(true);
                break;
            case EXPIRED:
                sceneExpired.setVisible(true);
                sceneExpired.setManaged(true);
                break;
        }
    }

    /**
     * 隐藏所有场景
     */
    private void hideAllScenes() {
        sceneUnactivated.setVisible(false);
        sceneUnactivated.setManaged(false);

        sceneTrial.setVisible(false);
        sceneTrial.setManaged(false);

        sceneTrialExpired.setVisible(false);
        sceneTrialExpired.setManaged(false);

        sceneActivated.setVisible(false);
        sceneActivated.setManaged(false);

        sceneExpired.setVisible(false);
        sceneExpired.setManaged(false);
    }
}