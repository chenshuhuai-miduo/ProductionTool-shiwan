package com.miduo.cloud.frontend.util;

import com.miduo.cloud.entity.enums.LicenseStatusEnum;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.controller.LicenseInfoController;
import com.miduo.cloud.frontend.service.LicenseValidationService;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;

/**
 * 许可证守卫类
 * 负责在关键功能入口处检查许可证状态，执行功能限制策略
 */
@Slf4j
public class LicenseGuard {

    private static DeviceInfoService deviceInfoService = new DeviceInfoService();
    private static LicenseService licenseService = new LicenseService(new LicenseValidationService());
    private static String currentDeviceId;

    /**
     * 初始化许可证守卫
     * 应该在应用程序启动时调用
     */
    public static void initialize() {
        try {
            if (deviceInfoService != null) {
                currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());
            }
            if (licenseService != null)
            {
                licenseService.init();
            }

            log.info("许可证守卫初始化完成");
        } catch (Exception e) {
            log.error("许可证守卫初始化失败", e);
        }
    }

    /**
     * 检查许可证状态是否允许功能使用
     *
     * @return true - 允许使用，false - 禁止使用
     */
    public static boolean checkFeatureAccess() {
        return checkFeatureAccess(null);
    }

    /**
     * 检查许可证状态是否允许功能使用
     *
     * @param featureName 功能名称（用于日志记录）
     * @return true - 允许使用，false - 禁止使用
     */
    public static boolean checkFeatureAccess(String featureName) {
        try {
            if (licenseService == null || currentDeviceId == null) {
                log.warn("许可证服务未初始化，允许功能使用（开发/测试环境）");
                return true; // 开发/测试环境允许使用
            }

            LicenseStatusEnum status = licenseService.getCurrentLicenseStatus(currentDeviceId);

            String feature = featureName != null ? featureName : "未知功能";
            log.debug("检查功能访问权限 - 功能: {}, 状态: {}", feature, status.getDescription());

            switch (status) {
                case ACTIVATED:
                case TRIAL_ACTIVE:
                    // 正式激活或试用期内，允许使用
                    return true;

                case UNACTIVATED:
                    // 未激活，显示提示并拒绝访问
                    showUnactivatedDialog(feature);
                    return false;

                case TRIAL_EXPIRED:
                    // 试用已过期，显示提示并拒绝访问
                    showTrialExpiredDialog(feature);
                    return false;

                case EXPIRED:
                    // 正式版已过期，显示提示并拒绝访问
                    showExpiredDialog(feature);
                    return false;

                default:
                    showUnactivatedDialog(feature);
                    return false;
            }

        } catch (Exception e) {
            log.error("许可证检查异常，允许功能使用", e);
            return true; // 异常情况下允许使用，避免阻塞正常功能
        }
    }

    /**
     * 检查任务启用权限
     */
    public static boolean checkTaskEnableAccess() {
        return checkFeatureAccess("任务启用");
    }

    /**
     * 检查码采集权限
     */
    public static boolean checkCodeCollectionAccess() {
        return checkFeatureAccess("码采集");
    }

    /**
     * 检查数据上传权限
     */
    public static boolean checkDataUploadAccess() {
        return checkFeatureAccess("数据上传");
    }

    /**
     * 检查产品管理权限
     */
    public static boolean checkProductManagementAccess() {
        return checkFeatureAccess("产品管理");
    }

    /**
     * 检查系统配置权限
     */
    public static boolean checkSystemConfigAccess() {
        return checkFeatureAccess("系统配置");
    }

    /**
     * 显示未激活提示对话框
     */
    private static void showUnactivatedDialog(String featureName) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("功能受限");
            alert.setHeaderText("软件未激活");
            alert.setContentText("功能 \"" + featureName + "\" 需要有效的许可证。\n\n请先激活软件后继续使用。");

            ButtonType activateButton = new ButtonType("立即激活");
            ButtonType cancelButton = new ButtonType("取消", ButtonType.CANCEL.getButtonData());
            alert.getButtonTypes().setAll(activateButton, cancelButton);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == activateButton) {
                    try {
                        LicenseInfoController.showLicenseInfo();
                    } catch (Exception e) {
                        log.error("打开许可证信息页面失败", e);
                    }
                }
            });
        });
    }

    /**
     * 显示试用过期提示对话框
     */
    private static void showTrialExpiredDialog(String featureName) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("功能受限");
            alert.setHeaderText("试用已过期");
            alert.setContentText("功能 \"" + featureName + "\" 的试用期已过期。\n\n请购买正式版许可证或联系米多专员续试用。");

            ButtonType activateButton = new ButtonType("获取许可证");
            ButtonType cancelButton = new ButtonType("取消", ButtonType.CANCEL.getButtonData());
            alert.getButtonTypes().setAll(activateButton, cancelButton);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == activateButton) {
                    try {
                        LicenseInfoController.showLicenseInfo();
                    } catch (Exception e) {
                        log.error("打开许可证信息页面失败", e);
                    }
                }
            });
        });
    }

    /**
     * 显示正式版过期提示对话框
     */
    private static void showExpiredDialog(String featureName) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("功能受限");
            alert.setHeaderText("许可证已过期");
            alert.setContentText("功能 \"" + featureName + "\" 的许可证已过期。\n\n请联系米多专员进行续期。");

            ButtonType renewButton = new ButtonType("联系续期");
            ButtonType cancelButton = new ButtonType("取消", ButtonType.CANCEL.getButtonData());
            alert.getButtonTypes().setAll(renewButton, cancelButton);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == renewButton) {
                    try {
                        LicenseInfoController.showLicenseInfo();
                    } catch (Exception e) {
                        log.error("打开许可证信息页面失败", e);
                    }
                }
            });
        });
    }

    /**
     * 检查许可证验证结果（包含设备不匹配信息）
     * @return 验证结果
     */
    public static LicenseValidationService.LicenseValidationResult checkLicenseValidation() {
        if (licenseService == null || currentDeviceId == null) {
            return LicenseValidationService.LicenseValidationResult.invalid("服务未初始化");
        }
        return licenseService.readLicense(currentDeviceId);
    }

    /**
     * 获取当前许可证状态
     */
    public static LicenseStatusEnum getCurrentLicenseStatus() {
        if (licenseService == null || currentDeviceId == null) {
            return LicenseStatusEnum.UNACTIVATED;
        }
        return licenseService.getCurrentLicenseStatus(currentDeviceId);
    }

    /**
     * 获取剩余天数
     */
    public static long getRemainingDays() {
        if (licenseService == null || currentDeviceId == null) {
            return -1;
        }
        return licenseService.getRemainingDays(currentDeviceId);
    }

    /**
     * 检查是否接近到期（剩余天数 <= threshold）
     */
    public static boolean isNearExpiry(int threshold) {
        long remainingDays = getRemainingDays();
        return remainingDays >= 0 && remainingDays <= threshold;
    }

    /**
     * 检查是否需要显示到期提醒
     */
    public static boolean shouldShowExpiryReminder() {
        LicenseStatusEnum status = getCurrentLicenseStatus();
        if (status == LicenseStatusEnum.ACTIVATED || status == LicenseStatusEnum.TRIAL_ACTIVE) {
            return isNearExpiry(30); // 提前30天提醒
        }
        return false;
    }
}






