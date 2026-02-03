package com.miduo.cloud.frontend.controller;

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
     * 处理确定按钮点击 - 关闭弹窗
     */
    @FXML
    private void handleConfirm() {
        closeDialog();
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
     */
    private void setupButtonHoverEffects() {
        if (confirmButton != null) {
            confirmButton.setOnMouseEntered(e -> 
                confirmButton.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white;")
            );
            confirmButton.setOnMouseExited(e -> 
                confirmButton.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;")
            );
        }
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
                dialogStage.initOwner(owner);
            } else {
                dialogStage.initModality(Modality.APPLICATION_MODAL);
            }
            
            //dialogStage.initStyle(StageStyle.UNDECORATED);

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

