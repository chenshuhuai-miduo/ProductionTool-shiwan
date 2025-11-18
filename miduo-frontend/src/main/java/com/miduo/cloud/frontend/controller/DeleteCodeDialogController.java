package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * 清除上一个箱码对话框控制器
 */
public class DeleteCodeDialogController {

    @FXML
    private Label orderNoLabel;
    
    @FXML
    private Label currentCountLabel;
    
    @FXML
    private Label afterCountLabel;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button confirmButton;

    private boolean confirmed = false;

    /**
     * 设置当前垛信息
     */
    public void setPalletInfo(String orderNo, Integer currentCount) {
        // 更新基本信息
        orderNoLabel.setText(orderNo);
        currentCountLabel.setText(String.valueOf(currentCount));
        
        // 计算删除后的箱数
        int afterCount = currentCount - 1;
        afterCountLabel.setText(String.valueOf(afterCount));
    }

    /**
     * 确定按钮事件
     */
    @FXML
    private void onConfirm() {
        System.out.println("确认清除上一个箱码");
        confirmed = true;
        closeDialog();
    }

    /**
     * 取消按钮事件
     */
    @FXML
    private void onCancel() {
        System.out.println("取消清除箱码");
        confirmed = false;
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

