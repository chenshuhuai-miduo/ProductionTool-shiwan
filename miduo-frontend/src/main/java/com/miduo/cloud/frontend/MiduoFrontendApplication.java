package com.miduo.cloud.frontend;

import com.miduo.cloud.frontend.controller.*;
import com.miduo.cloud.frontend.service.DeviceInfoService;
import com.miduo.cloud.frontend.service.LicenseService;
import com.miduo.cloud.frontend.util.DeviceUniqueIdGenerator;
import com.miduo.cloud.frontend.util.OperateLogBatchManager;
import com.miduo.cloud.frontend.service.LicenseValidationService;
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
 * 米多赋码采集关联系统 - JavaFX应用程序类
 * 前端独立启动类（不依赖Spring Boot）
 */
public class MiduoFrontendApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("========================================");
        System.out.println("米多赋码采集关联系统正在启动...");
        System.out.println("========================================");
        
        // 启动操作日志批量管理器
        OperateLogBatchManager.getInstance().start();

        // 执行许可证验证
        if (!performLicenseValidation()) {
            // 许可证验证失败，退出应用程序
            System.err.println("========================================");
            System.err.println("✗ 许可证验证失败，应用程序退出");
            System.err.println("========================================");
            Platform.exit();
            return;
        }

        try {
            // 加载主界面FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            
            // 创建场景
            Scene scene = new Scene(root, 1400, 900);
            
            // 设置舞台属性
            primaryStage.setTitle("米多赋码采集关联系统");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true); // 允许调整窗口大小
            primaryStage.setMaximized(false); // 启动时不最大化（可根据需要改为true）
            
            // 设置窗口图标
            StageIconUtil.setStageIcon(primaryStage);
            
            // 窗口关闭事件处理 - 改为最小化
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("========================================");
                System.out.println("用户点击关闭按钮（X）- 最小化窗口");
                System.out.println("========================================");
                
                // 消费事件，防止窗口关闭
                event.consume();
                
                // 最小化窗口
                primaryStage.setIconified(true);
            });
            
            // 显示窗口
            primaryStage.show();
            
            System.out.println("========================================");
            System.out.println("✓ 米多赋码采集关联系统启动成功！");
            System.out.println("  主界面已加载");
            System.out.println("  窗口大小: 1400x900");
            System.out.println("  后端地址: http://localhost:8080");
            System.out.println("========================================");
            System.out.println("提示:");
            System.out.println("  1. 请确保后端服务已启动（端口8080）");
            System.out.println("  2. 如需修改后端地址，请编辑 HttpUtil.java");
            System.out.println("  3. 设备连接功能需要相应硬件设备");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("✗ 启动失败！");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("========================================");
            System.err.println("可能的原因:");
            System.err.println("  1. FXML文件路径错误");
            System.err.println("  2. Controller类初始化失败");
            System.err.println("  3. 资源文件缺失");
            System.err.println("========================================");
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public void stop() throws Exception {
        System.out.println("========================================");
        System.out.println("米多赋码采集关联系统正在关闭...");
        System.out.println("========================================");
        
        // 停止操作日志批量管理器（会先保存所有待保存的日志）
        OperateLogBatchManager.getInstance().stop();
        
        super.stop();
        System.out.println("✓ 应用程序已安全退出");
        System.out.println("========================================");
    }

    /**
     * 执行许可证验证
     * @return 验证是否通过
     */
    private boolean performLicenseValidation() {
        try {
            System.out.println("正在验证许可证...");

            DeviceInfoService deviceInfoService = new DeviceInfoService();
            LicenseService licenseService = new LicenseService(new LicenseValidationService());
            licenseService.init();

            // 获取当前设备ID
            String currentDeviceId = DeviceUniqueIdGenerator.generateDeviceId(deviceInfoService.getDeviceInfo());

            // 先检查许可证文件是否存在
            if (!licenseService.licenseExists()) {
                System.out.println("检测到许可证文件不存在，显示未激活弹窗");
                return handleUnactivatedStatus();
            }

            // 获取详细验证结果（包含设备不匹配信息）
            LicenseValidationService.LicenseValidationResult validationResult = 
                licenseService.readLicense(currentDeviceId);
        
            // 检查设备不匹配
            if (validationResult.isDeviceMismatch()) {
                DeviceMismatchDialogController.showDeviceMismatchDialog(
                    validationResult.getMismatchedDeviceId());
                return false; // 返回true让应用继续，但功能会被限制
            }

            // 读取许可证状态
            LicenseStatusEnum status = licenseService.getCurrentLicenseStatus(currentDeviceId);

            System.out.println("许可证状态: " + status.getDescription());

            if (status == LicenseStatusEnum.UNACTIVATED) {
                // 未激活，弹出未激活对话框
                UnactivatedDialogController.showUnactivatedDialog();
            } else if (status == LicenseStatusEnum.TRIAL_EXPIRED ||
                    status == LicenseStatusEnum.EXPIRED) {
                // 试用过期或正式授权过期，弹出过期对话框
                TrialExpireDialogController.showTrialExpireDialog(status);
            } else if (status == LicenseStatusEnum.TRIAL_ACTIVE) {
                // 试用期内，检查是否需要显示即将到期提醒
                long remainingDays = licenseService.getRemainingDays(currentDeviceId);
                if (remainingDays >= 0 && remainingDays <= 3) {
                    // 剩余天数 <= 3 天，显示即将到期提醒
                    TrialExpiringDialogController.showTrialExpiringDialog(remainingDays);
                }
            }

            return  true;

        } catch (Exception e) {
            System.err.println("许可证验证异常: " + e.getMessage());
            e.printStackTrace();
            return handleValidationError();
        }
    }

    /**
     * 处理未激活状态
     */
    private boolean handleUnactivatedStatus() {
        System.out.println("检测到未激活状态，显示未激活提示");

        try {
            // 使用CompletableFuture等待弹窗结果
           UnactivatedDialogController.showUnactivatedDialog();
           return true;

        } catch (Exception e) {
            System.err.println("处理未激活状态异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 处理试用过期状态
     */
    private boolean handleTrialExpiredStatus() {

        LicenseInfoController.showLicenseInfo();

        return false;
    }

    /**
     * 处理正式版过期状态
     */
    private boolean handleExpiredStatus() {
        System.out.println("检测到正式版已过期状态，显示续期提示");

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("许可证已过期");
            alert.setHeaderText("您的许可证已过期");
            alert.setContentText("正式版许可证已过期，无法继续使用。\n\n请联系米多专员进行续期。");

            ButtonType renewButton = new ButtonType("联系续期");
            ButtonType exitButton = new ButtonType("退出软件");
            alert.getButtonTypes().setAll(renewButton, exitButton);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == renewButton) {
                    Platform.runLater(() -> LicenseInfoController.showLicenseInfo());
                }
            });

            Platform.exit();
        });

        return false;
    }

    /**
     * 处理验证异常
     */
    private boolean handleValidationError() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("许可证验证失败");
            alert.setHeaderText("无法验证许可证状态");
            alert.setContentText("许可证验证过程中发生错误。\n\n为了安全起见，应用程序将退出。\n\n请联系技术支持获取帮助。");
            alert.showAndWait();
            Platform.exit();
        });

        return false;
    }

    /**
     * JavaFX应用程序启动入口
     * 可以直接运行此方法启动前端
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        launch(args);
    }
}

