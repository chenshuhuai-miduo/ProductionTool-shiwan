package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * 码删除确认对话框控制器
 */
public class CodeDeleteDialogController {
    
    @FXML
    private TextField boxCodeField;
    
    private boolean confirmed = false;
    private String boxCode;
    
    /**
     * 确认按钮点击事件
     */
    @FXML
    private void onConfirm() {
        boxCode = boxCodeField.getText().trim();
        
        if (boxCode.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING
            );
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("请输入箱码！");
            alert.showAndWait();
            return;
        }
        
        confirmed = true;
        closeDialog();
    }
    
    /**
     * 取消按钮点击事件
     */
    @FXML
    private void onCancel() {
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) boxCodeField.getScene().getWindow();
        stage.close();
    }
    
    /**
     * 是否确认删除
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * 获取输入的箱码
     */
    public String getBoxCode() {
        return boxCode;
    }
}

