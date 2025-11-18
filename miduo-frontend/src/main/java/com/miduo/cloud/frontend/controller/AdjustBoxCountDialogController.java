package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * 指定当前箱数对话框控制器
 */
public class AdjustBoxCountDialogController {
    
    @FXML
    private Label currentOrderLabel;
    
    @FXML
    private Label currentCountLabel;
    
    @FXML
    private Label totalCountLabel;
    
    @FXML
    private TextField targetCountField;
    
    @FXML
    private Label hintLabel;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button confirmButton;
    
    // 数据字段
    private String orderNo;
    private Integer currentCount;
    private Integer totalCount;
    private Integer targetCount;
    private boolean confirmed = false;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        // 监听输入框变化
        targetCountField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateHintLabel();
        });
    }
    
    /**
     * 设置当前垛信息
     */
    public void setCurrentPalletInfo(String orderNo, Integer currentCount, Integer totalCount) {
        this.orderNo = orderNo;
        this.currentCount = currentCount;
        this.totalCount = totalCount;
        
        // 更新UI显示
        currentOrderLabel.setText(orderNo);
        currentCountLabel.setText(String.valueOf(currentCount));
        totalCountLabel.setText(String.valueOf(totalCount));
    }
    
    /**
     * 更新提示标签
     */
    private void updateHintLabel() {
        String inputText = targetCountField.getText().trim();
        
        if (inputText.isEmpty()) {
            hintLabel.setText("请输入目标箱数");
            hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #999999; -fx-padding: 8px; -fx-background-color: #fafafa; -fx-background-radius: 4px;");
            return;
        }
        
        try {
            int target = Integer.parseInt(inputText);
            
            if (target < 0) {
                hintLabel.setText("⚠ 目标箱数不能为负数");
                hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff4d4f; -fx-padding: 8px; -fx-background-color: #fff1f0; -fx-background-radius: 4px;");
                return;
            }
            
            if (target == currentCount) {
                hintLabel.setText("✓ 无需调整");
                hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #52c41a; -fx-padding: 8px; -fx-background-color: #f6ffed; -fx-background-radius: 4px;");
            } else if (target < currentCount) {
                int diff = currentCount - target;
                hintLabel.setText("⚠ 将删除 " + diff + " 个最新采集的箱码");
                hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff4d4f; -fx-padding: 8px; -fx-background-color: #fff1f0; -fx-background-radius: 4px;");
            } else {
                int diff = target - currentCount;
                hintLabel.setText("ℹ 将自动生成 " + diff + " 个虚拟箱码");
                hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1890ff; -fx-padding: 8px; -fx-background-color: #e6f7ff; -fx-background-radius: 4px;");
            }
            
        } catch (NumberFormatException e) {
            hintLabel.setText("⚠ 请输入有效的数字");
            hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff4d4f; -fx-padding: 8px; -fx-background-color: #fff1f0; -fx-background-radius: 4px;");
        }
    }
    
    /**
     * 取消按钮点击
     */
    @FXML
    private void onCancel() {
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 确认按钮点击
     */
    @FXML
    private void onConfirm() {
        String inputText = targetCountField.getText().trim();
        
        if (inputText.isEmpty()) {
            hintLabel.setText("⚠ 请输入目标箱数");
            hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff4d4f; -fx-padding: 8px; -fx-background-color: #fff1f0; -fx-background-radius: 4px;");
            return;
        }
        
        try {
            targetCount = Integer.parseInt(inputText);
            
            if (targetCount < 0) {
                hintLabel.setText("⚠ 目标箱数不能为负数");
                hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff4d4f; -fx-padding: 8px; -fx-background-color: #fff1f0; -fx-background-radius: 4px;");
                return;
            }
            
            confirmed = true;
            closeDialog();
            
        } catch (NumberFormatException e) {
            hintLabel.setText("⚠ 请输入有效的数字");
            hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ff4d4f; -fx-padding: 8px; -fx-background-color: #fff1f0; -fx-background-radius: 4px;");
        }
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * 是否确认
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * 获取目标箱数
     */
    public Integer getTargetCount() {
        return targetCount;
    }
}

