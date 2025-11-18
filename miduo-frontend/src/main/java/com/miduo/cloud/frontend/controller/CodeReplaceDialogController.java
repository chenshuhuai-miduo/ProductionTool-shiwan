package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.dto.code.CodeReplaceRequest;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * 码替换弹窗控制器
 * 用于数据上传页面的码替换弹窗
 */
public class CodeReplaceDialogController {
    
    // 表单控件
    @FXML private TextField originalCodeField;
    @FXML private TextField newCodeField;
    @FXML private TextArea reasonTextArea;
    
    private Stage dialogStage;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("码替换弹窗初始化...");
    }
    
    /**
     * 设置弹窗Stage（从外部调用）
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    // ==================== 操作事件 ====================
    
    /**
     * 清空
     */
    @FXML
    private void onClear() {
        originalCodeField.clear();
        newCodeField.clear();
        reasonTextArea.clear();
        System.out.println("清空表单");
    }
    
    /**
     * 确认替换
     * API: POST /api/code/replace
     */
    @FXML
    private void onConfirmReplace() {
        // 验证输入
        String originalCode = originalCodeField.getText().trim();
        String newCode = newCodeField.getText().trim();
        String reason = reasonTextArea.getText().trim();
        
        if (originalCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入原码");
            return;
        }
        
        if (newCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入新码");
            return;
        }
        
        if (originalCode.equals(newCode)) {
            showAlert(Alert.AlertType.WARNING, "提示", "原码和新码不能相同");
            return;
        }
        
        // 显示确认对话框
        if (showConfirmDialog(originalCode, newCode, reason)) {
            // 调用后端接口进行替换
            replaceCodeToServer(originalCode, newCode, reason);
        }
    }
    
    /**
     * 调用后端接口替换码
     * API: POST /api/code/replace
     */
    private void replaceCodeToServer(String oldCode, String newCode, String reason) {
        new Thread(() -> {
            try {
                // 构建请求对象
                CodeReplaceRequest request = new CodeReplaceRequest();
                request.setOldCode(oldCode);
                request.setNewCode(newCode);
                request.setReason(reason);
                
                // 调用重构后的接口：POST /api/code/replace
                String responseJson = HttpUtil.doPost("/api/code/replace", request);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData()) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", "码替换成功！");
                        // 清空表单
                        onClear();
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_REPLACE)
                            .operateType(OperateTypeEnum.REPLACE)
                            .target(oldCode, newCode)
                            .content("码替换: " + oldCode + " → " + newCode + 
                                    (reason != null && !reason.isEmpty() ? ", 原因: " + reason : ""))
                            .beforeData(oldCode)
                            .afterData(newCode)
                            .remark(reason)
                            .saveAsync();
                        
                        // 2秒后关闭弹窗
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> {
                                    if (dialogStage != null) {
                                        dialogStage.close();
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "失败", result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_REPLACE)
                            .operateType(OperateTypeEnum.REPLACE)
                            .target(oldCode, newCode)
                            .content("码替换失败: " + oldCode + " → " + newCode)
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "码替换异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 显示确认对话框
     */
    private boolean showConfirmDialog(String originalCode, String newCode, String reason) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认码替换");
        confirmDialog.setHeaderText("请确认替换信息：");
        
        StringBuilder content = new StringBuilder();
        content.append("原码值：").append(originalCode).append("\n");
        content.append("新码值：").append(newCode).append("\n");
        if (!reason.isEmpty()) {
            content.append("替换原因：").append(reason).append("\n");
        }
        content.append("\n此操作不可恢复，请仔细核对后确认！");
        
        confirmDialog.setContentText(content.toString());
        
        // 修改按钮文本
        ButtonType confirmButton = new ButtonType("确认替换", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(confirmButton, cancelButton);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }
    
    /**
     * 显示提示对话框
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

