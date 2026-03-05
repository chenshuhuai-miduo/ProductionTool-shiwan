package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.controller.MainController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 续期成功弹窗控制器
 * 
 * 显示场景：当许可证文件的 payload 中 activationType 为 "renewal"，且校验均无误时显示
 */
public class RenewalSuccessController {

    @FXML
    private Label newExpirationLabel;

    @FXML
    private Label extensionDaysLabel;

    @FXML
    private Button confirmButton;

    private Stage dialogStage;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        // 添加按钮悬停效果
        setupButtonHoverEffects();
    }

    /**
     * 设置对话框 Stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * 设置续期信息
     * 
     * @param newExpiration  新到期时间
     * @param extensionDays  延长天数
     */
    public void setRenewalInfo(LocalDate newExpiration, long extensionDays) {
        newExpirationLabel.setText(newExpiration.format(DATE_FORMATTER));
        extensionDaysLabel.setText("+" + extensionDays + "天");
    }

    /**
     * 设置续期信息（字符串格式）
     * 
     * @param newExpiration  新到期时间（yyyy-MM-dd格式）
     * @param extensionDays  延长天数
     */
    public void setRenewalInfo(String newExpiration, long extensionDays) {
        LocalDate newDate = LocalDate.parse(newExpiration, DATE_FORMATTER);
        setRenewalInfo(newDate, extensionDays);
    }

    /**
     * 处理确定按钮点击 - 关闭所有相关窗口（续期成功对话框、激活向导窗口、过期对话框）
     */
    @FXML
    private void handleConfirm() {
        // 关闭续期成功对话框
        if (dialogStage != null) {
            // 获取窗口层级关系：
            // renewalStage (当前窗口) -> activationStage (owner) -> expireStage 或 licenseInfoStage (owner的owner)
            Stage activationStage = null;
            Stage parentStage = null;
            
            if (dialogStage.getOwner() != null && dialogStage.getOwner() instanceof Stage) {
                activationStage = (Stage) dialogStage.getOwner();
                
                // 获取激活向导窗口的 owner（可能是过期对话框窗口或许可证信息窗口）
                if (activationStage.getOwner() != null && activationStage.getOwner() instanceof Stage) {
                    parentStage = (Stage) activationStage.getOwner();
                }
            }
            
            // 检查是否从许可证信息界面打开的（通过检查parentStage的标题）
            boolean openedFromLicenseInfo = false;
            if (parentStage != null) {
                String parentTitle = parentStage.getTitle();
                if (parentTitle != null && parentTitle.contains("许可证信息")) {
                    openedFromLicenseInfo = true;
                }
            }
            
            // 关闭续期成功对话框
            dialogStage.close();
            
            // 关闭激活向导窗口
            if (activationStage != null) {
                activationStage.close();
            }
            
            // 如果不是从许可证信息界面打开的，才关闭过期对话框并将主界面显示到前面
            if (parentStage != null && !openedFromLicenseInfo) {
                // 关闭过期对话框窗口（这样才能显示主界面）
                parentStage.close();
                
                // 关闭过期对话框后，确保主界面窗口被激活并显示在前台
                Platform.runLater(() -> {
                    try {
                        MainController mainController = MainController.getCurrentInstance();
                        if (mainController != null) {
                            // 通过主界面控制器获取窗口（使用许可证状态控件的场景）
                            // 由于 MainController 没有直接的 getRoot 方法，我们通过查找所有 JavaFX 窗口来找到主界面
                            javafx.stage.Window.getWindows().stream()
                                .filter(window -> window instanceof Stage)
                                .map(window -> (Stage) window)
                                .filter(stage -> {
                                    // 查找主界面窗口（标题为"米多赋码采集关联系统"）
                                    String title = stage.getTitle();
                                    return title != null && title.contains("米多赋码采集关联系统");
                                })
                                .findFirst()
                                .ifPresent(mainStage -> {
                                    // 激活主界面窗口
                                    mainStage.requestFocus();
                                    mainStage.toFront();
                                    // 如果窗口被最小化，则恢复显示
                                    if (mainStage.isIconified()) {
                                        mainStage.setIconified(false);
                                    }
                                });
                        }
                    } catch (Exception e) {
                        // 静默处理，不影响正常流程
                        System.err.println("激活主界面窗口失败: " + e.getMessage());
                    }
                });
            }
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
     * 设置按钮悬停效果
     * 按钮样式已在CSS中定义（btn-primary），无需额外设置
     */
    private void setupButtonHoverEffects() {
        // 悬停效果由CSS样式 btn-primary:hover 控制
    }

        /**
     * 静态方法：显示续期成功弹窗（带 owner 窗口）
     * 
     * @param owner 父窗口
     * @param newExpiration  新到期时间
     * @param extensionDays  延长天数
     */
    public static void showRenewalSuccessDialog(Stage owner, LocalDate newExpiration, long extensionDays) {
        try {
            FXMLLoader loader = new FXMLLoader(
                RenewalSuccessController.class.getResource("/fxml/RenewalSuccess.fxml")
            );
            Parent root = loader.load();

            RenewalSuccessController controller = loader.getController();
            controller.setRenewalInfo(newExpiration, extensionDays);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("续期成功");
            
            // 如果有 owner，使用 WINDOW_MODAL，否则使用 APPLICATION_MODAL
            if (owner != null) {
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initStyle(StageStyle.TRANSPARENT);
                dialogStage.initOwner(owner);
            } else {
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initStyle(StageStyle.TRANSPARENT);
            }

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.sizeToScene();
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 静态方法：显示续期成功弹窗
     * 
     * @param newExpiration  新到期时间
     * @param extensionDays  延长天数
     */
    public static void showRenewalSuccessDialog(LocalDate newExpiration, long extensionDays) {
        showRenewalSuccessDialog(null, newExpiration, extensionDays);
    }
}

