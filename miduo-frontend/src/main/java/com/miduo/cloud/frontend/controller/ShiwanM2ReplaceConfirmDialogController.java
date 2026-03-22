package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ShiwanM2ReplaceConfirmDialogController {

    @FXML private HBox          titleBar;
    @FXML private Label         origCodeLabel;
    @FXML private Label         newCodeLabel;
    @FXML private Label         reasonLabel;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private double dragOffsetX, dragOffsetY;
    private boolean confirmed = false;
    private String  expectedPassword = "";

    @FXML
    private void initialize() {
        titleBar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
        passwordField.setOnAction(e -> onConfirm());
    }

    /** 由外部调用初始化替换信息和预期密码 */
    public void setReplaceInfo(String orig, String newCode, String reason, String password) {
        origCodeLabel.setText("原码值：" + orig);
        newCodeLabel.setText("新码值：" + newCode);
        if (reason == null || reason.isEmpty()) {
            reasonLabel.setVisible(false);
            reasonLabel.setManaged(false);
        } else {
            reasonLabel.setText("替换原因：" + reason);
        }
        this.expectedPassword = password;
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        closeStage();
    }

    @FXML
    private void onConfirm() {
        String pwd = passwordField.getText();
        if (pwd == null || pwd.isEmpty()) {
            showError("请输入密码");
            return;
        }
        if (!expectedPassword.equals(pwd)) {
            showError("密码不正确，请重新输入");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }
        confirmed = true;
        closeStage();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void closeStage() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }
}
