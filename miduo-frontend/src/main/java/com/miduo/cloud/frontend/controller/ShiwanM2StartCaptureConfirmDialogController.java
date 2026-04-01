package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * 开始采集确认弹窗控制器
 */
public class ShiwanM2StartCaptureConfirmDialogController {

    @FXML
    private HBox titleBar;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label productNoLabel;

    @FXML
    private Label orderNoLabel;

    @FXML
    private Label palletBoxesLabel;

    @FXML
    private Label boxUnitsLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button confirmButton;

    private boolean confirmed = false;

    @FXML
    private void initialize() {
        closeBtn_hoverEffect();
    }

    private void closeBtn_hoverEffect() {
        // closeBtn 通过 FXML fx:id 直接设置，此处无需额外操作
    }

    /**
     * 设置弹窗中要显示的生产信息和采集规格
     */
    public void setInfo(String orderNo, String productNo, String product, int m, int n) {
        productNameLabel.setText(product);
        productNoLabel.setText(productNo);
        orderNoLabel.setText(orderNo);
        palletBoxesLabel.setText(String.valueOf(m));
        boxUnitsLabel.setText(String.valueOf(n));
    }

    @FXML
    private void onClose() {
        confirmed = false;
        closeDialog();
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        closeDialog();
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
