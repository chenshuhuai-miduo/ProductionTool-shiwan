package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.util.FxToast;
import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ShiwanM2ReplaceConfirmDialogController {

    @FXML private HBox  titleBar;
    @FXML private javafx.scene.control.Label origCodeValue;
    @FXML private javafx.scene.control.Label newCodeValue;
    @FXML private HBox  reasonRow;
    @FXML private javafx.scene.control.Label reasonValue;
    @FXML private PasswordField passwordField;
    @FXML private TextField     plainPasswordField;
    @FXML private Button        eyeToggleBtn;
    @FXML private StackPane     headerWarnIconPane;
    @FXML private StackPane     footerWarnIconPane;
    @FXML private StackPane     replaceEyeIconPane;

    private double  dragOffsetX, dragOffsetY;
    private boolean confirmed        = false;
    private boolean passwordVisible  = false;
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
        plainPasswordField.setOnAction(e -> onConfirm());

        // 明文框与密码框保持同步
        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!passwordVisible) plainPasswordField.setText(n);
        });
        plainPasswordField.textProperty().addListener((obs, o, n) -> {
            if (passwordVisible) passwordField.setText(n);
        });

        SvgIconLoader.loadInto(headerWarnIconPane, SvgIconLoader.ICON_WARN, 20, Color.web("#DC2626"));
        SvgIconLoader.loadInto(footerWarnIconPane, SvgIconLoader.ICON_WARN, 18, Color.web("#DC2626"));
        refreshReplaceEyeIcon();
    }

    private void refreshReplaceEyeIcon() {
        if (replaceEyeIconPane == null) {
            return;
        }
        String path = passwordVisible ? SvgIconLoader.ICON_EYE_HIDE : SvgIconLoader.ICON_EYE_SHOW;
        SvgIconLoader.loadInto(replaceEyeIconPane, path, 22, Color.web("#9CA3AF"));
    }

    /** 由外部调用初始化替换信息和预期密码 */
    public void setReplaceInfo(String orig, String newCode, String reason, String password) {
        origCodeValue.setText(orig);
        newCodeValue.setText(newCode);
        if (reason == null || reason.isEmpty()) {
            reasonRow.setVisible(false);
            reasonRow.setManaged(false);
        } else {
            reasonValue.setText(reason);
        }
        this.expectedPassword = password;
    }

    @FXML
    private void onTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            plainPasswordField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            plainPasswordField.setVisible(true);
            plainPasswordField.setManaged(true);
            plainPasswordField.requestFocus();
            plainPasswordField.positionCaret(plainPasswordField.getText().length());
            refreshReplaceEyeIcon();
        } else {
            passwordField.setText(plainPasswordField.getText());
            plainPasswordField.setVisible(false);
            plainPasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            refreshReplaceEyeIcon();
        }
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        closeStage();
    }

    @FXML
    private void onConfirm() {
        String pwd = passwordVisible ? plainPasswordField.getText() : passwordField.getText();
        if (pwd == null || pwd.isEmpty()) {
            showError("请输入密码");
            return;
        }
        if (!expectedPassword.equals(pwd)) {
            showError("密码错误，请重新输入");
            passwordField.clear();
            plainPasswordField.clear();
            if (passwordVisible) {
                plainPasswordField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }
        confirmed = true;
        closeStage();
    }

    private void showError(String msg) {
        Window window = titleBar.getScene().getWindow();
        FxToast.error(window, msg);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void closeStage() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }
}
