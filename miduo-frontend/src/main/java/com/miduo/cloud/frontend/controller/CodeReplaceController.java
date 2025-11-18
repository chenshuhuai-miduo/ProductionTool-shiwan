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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 码替换控制器
 * 功能：码替换操作管理
 * 
 * API路径已验证: POST /api/code/replace
 */
public class CodeReplaceController {
    
    // 表单控件
    @FXML private TextField originalCodeField;
    @FXML private TextField newCodeField;
    @FXML private TextArea reasonTextArea;
    
    // 底部状态栏
    @FXML private Label currentTimeLabel;
    
    private Timer timer;
    
    // 静态变量：保存当前打开的码替换页面控制器实例（用于接收扫码枪数据）
    private static CodeReplaceController currentInstance = null;
    
    /**
     * 获取当前打开的码替换页面控制器实例
     * @return 如果码替换页面已打开，返回控制器实例；否则返回null
     */
    public static CodeReplaceController getCurrentInstance() {
        return currentInstance;
    }
    
    /**
     * 处理扫码枪数据（由MainController调用）
     * 优先填充到原码输入框，如果原码已有内容，则填充到新码输入框
     * @param barcodeData 扫码枪扫描的条码数据
     */
    public static void handleBarcodeData(String barcodeData) {
        if (currentInstance != null) {
            Platform.runLater(() -> {
                // 检查原码输入框是否为空
                String originalCode = currentInstance.originalCodeField.getText().trim();
                
                if (originalCode.isEmpty()) {
                    // 原码为空，填充到原码输入框
                    currentInstance.originalCodeField.setText(barcodeData);
                    // 自动聚焦到新码输入框
                    currentInstance.newCodeField.requestFocus();
                    System.out.println("[码替换页面] 扫码数据已填入原码输入框: " + barcodeData);
                } else {
                    // 原码已有内容，填充到新码输入框
                    currentInstance.newCodeField.setText(barcodeData);
                    // 保持新码输入框的焦点
                    currentInstance.newCodeField.requestFocus();
                    System.out.println("[码替换页面] 扫码数据已填入新码输入框: " + barcodeData);
                }
            });
        }
    }
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("码替换界面初始化...");
        
        // 注册当前实例（用于接收扫码枪数据）
        currentInstance = this;
        
        // 启动实时时钟
        startRealtimeClock();
        
        // 延迟设置窗口关闭事件监听器（因为此时场景可能还未设置到窗口）
        Platform.runLater(() -> {
            if (originalCodeField.getScene() != null && originalCodeField.getScene().getWindow() != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) originalCodeField.getScene().getWindow();
                stage.setOnCloseRequest(event -> {
                    if (currentInstance == this) {
                        currentInstance = null;
                        System.out.println("[码替换页面] 窗口关闭，清除控制器引用");
                    }
                });
            }
        });
    }
    
    /**
     * 启动实时时钟
     */
    private void startRealtimeClock() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    updateTime();
                });
            }
        }, 0, 1000);
    }
    
    /**
     * 更新时间显示
     */
    private void updateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        currentTimeLabel.setText(LocalDateTime.now().format(formatter));
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
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (timer != null) {
            timer.cancel();
        }
    }
}

