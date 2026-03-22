package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * 取消关联确认弹窗控制器
 */
public class ShiwanM2CancelConfirmDialogController {

    @FXML private HBox          titleBar;
    @FXML private Label         infoScopeLabel;
    @FXML private Label         infoExecLabel;
    @FXML private Label         infoSkipLabel;
    @FXML private VBox          cloudWarningBox;
    @FXML private VBox          cloudPalletList;
    @FXML private PasswordField passwordField;
    @FXML private TextField     plainField;
    @FXML private Label         eyeIcon;
    @FXML private Label         pwdErrorLabel;

    private double  dragOffsetX;
    private double  dragOffsetY;
    private boolean confirmed   = false;
    private String  correctPassword = "123456";

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
        plainField.setOnAction(e -> onConfirm());
    }

    /**
     * 填充弹窗数据。
     *
     * @param modeAll         是否「全部解除」模式
     * @param execCount       可解除项数量
     * @param totalRelations  涉及关联条数
     * @param skipCount       不可解除项数量
     * @param uploadedCodes   已上传云端的垛码列表（可为空）
     * @param password        正确密码
     */
    public void setInfo(boolean modeAll, int execCount, int totalRelations,
                        int skipCount, List<String> uploadedCodes, String password) {
        this.correctPassword = password;

        infoScopeLabel.setText("取消范围：" + (modeAll ? "全部解除" : "只解一层"));
        infoExecLabel.setText("本次将执行 " + execCount + " 个可解除项，共涉及 " + totalRelations + " 条关联");

        if (skipCount > 0) {
            infoSkipLabel.setText(skipCount + " 个不可解除项将被跳过");
            infoSkipLabel.setVisible(true);
            infoSkipLabel.setManaged(true);
        }

        if (uploadedCodes != null && !uploadedCodes.isEmpty()) {
            cloudPalletList.getChildren().clear();
            for (String code : uploadedCodes) {
                Label l = new Label("· 垛码 " + code);
                l.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #92400E;" +
                           " -fx-font-family: 'Microsoft YaHei';");
                cloudPalletList.getChildren().add(l);
            }
            cloudWarningBox.setVisible(true);
            cloudWarningBox.setManaged(true);
        }
    }

    @FXML
    private void onToggleVisible() {
        boolean showing = plainField.isVisible();
        if (showing) {
            passwordField.setText(plainField.getText());
            passwordField.setVisible(true);  passwordField.setManaged(true);
            plainField.setVisible(false);    plainField.setManaged(false);
            eyeIcon.setText("\uD83D\uDC41");
        } else {
            plainField.setText(passwordField.getText());
            plainField.setVisible(true);     plainField.setManaged(true);
            passwordField.setVisible(false); passwordField.setManaged(false);
            eyeIcon.setText("\uD83D\uDD12");
        }
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        String input = plainField.isVisible() ? plainField.getText() : passwordField.getText();
        if (!correctPassword.equals(input)) {
            pwdErrorLabel.setText("密码错误，请重新输入");
            pwdErrorLabel.setVisible(true);
            pwdErrorLabel.setManaged(true);
            passwordField.clear();
            plainField.clear();
            return;
        }
        confirmed = true;
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
