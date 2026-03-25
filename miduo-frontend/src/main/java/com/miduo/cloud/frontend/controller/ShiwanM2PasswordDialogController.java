package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * 系统密码输入弹窗控制器（对应 ShiwanM2PasswordDialog.fxml）
 */
public class ShiwanM2PasswordDialogController {

    @FXML private VBox          rootBox;
    @FXML private PasswordField passwordField;
    @FXML private TextField     plainField;
    @FXML private StackPane     titleLockIconPane;
    @FXML private StackPane     eyeIconPane;
    @FXML private Label         errorLabel;

    private boolean showingPlain = false;
    private boolean confirmed    = false;

    /** 拖拽偏移量 */
    private double dragOffsetX, dragOffsetY;

    @FXML
    private void initialize() {
        // 整卡片可拖拽
        rootBox.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        rootBox.setOnMouseDragged(e -> {
            Stage stage = (Stage) rootBox.getScene().getWindow();
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        // 输入框聚焦时边框变蓝
        String focusedBorder = "-fx-border-color: #2563EB; -fx-border-width: 2;";
        String normalBorder  = "-fx-border-color: #D1D5DB; -fx-border-width: 1.5;";
        passwordField.focusedProperty().addListener((o, old, focused) -> {
            String base = getPasswordFieldBaseStyle();
            passwordField.setStyle(base + (focused ? focusedBorder : normalBorder));
        });
        plainField.focusedProperty().addListener((o, old, focused) -> {
            String base = getPlainFieldBaseStyle();
            plainField.setStyle(base + (focused ? focusedBorder : normalBorder));
        });

        // Enter 键确认
        passwordField.setOnAction(e -> onConfirm());
        plainField.setOnAction(e -> onConfirm());

        SvgIconLoader.loadInto(titleLockIconPane, SvgIconLoader.ICON_LOCK, 26, Color.web("#374151"));
        refreshEyeIcon();
    }

    private void refreshEyeIcon() {
        if (eyeIconPane == null) {
            return;
        }
        String path = showingPlain ? SvgIconLoader.ICON_EYE_HIDE : SvgIconLoader.ICON_EYE_SHOW;
        SvgIconLoader.loadInto(eyeIconPane, path, 22, Color.web("#9CA3AF"));
    }

    /** 切换显示/隐藏密码 */
    @FXML
    private void onToggleVisible() {
        showingPlain = !showingPlain;
        if (showingPlain) {
            plainField.setText(passwordField.getText());
            plainField.setVisible(true);
            plainField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            refreshEyeIcon();
            plainField.requestFocus();
            plainField.positionCaret(plainField.getText().length());
        } else {
            passwordField.setText(plainField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            plainField.setVisible(false);
            plainField.setManaged(false);
            refreshEyeIcon();
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        getStage().close();
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        getStage().close();
    }

    /** 显示错误提示（密码错误时从外部调用）*/
    public void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public String getPassword() {
        return showingPlain ? plainField.getText() : passwordField.getText();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private Stage getStage() {
        return (Stage) rootBox.getScene().getWindow();
    }

    private String getPasswordFieldBaseStyle() {
        return "-fx-min-height: 52px; -fx-pref-height: 52px;" +
               "-fx-font-size: 18px; -fx-text-fill: #374151;" +
               "-fx-background-color: white;" +
               "-fx-background-radius: 10;" +
               "-fx-border-radius: 10;" +
               "-fx-padding: 0 48 0 14;" +
               "-fx-font-family: 'Microsoft YaHei';";
    }

    private String getPlainFieldBaseStyle() {
        return "-fx-min-height: 52px; -fx-pref-height: 52px;" +
               "-fx-font-size: 18px; -fx-text-fill: #374151;" +
               "-fx-background-color: white;" +
               "-fx-background-radius: 10;" +
               "-fx-border-radius: 10;" +
               "-fx-padding: 0 48 0 14;" +
               "-fx-font-family: 'Microsoft YaHei';";
    }
}
