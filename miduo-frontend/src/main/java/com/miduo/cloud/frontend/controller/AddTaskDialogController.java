package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.dto.task.TaskAddRequest;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 添加任务对话框控制器
 */
public class AddTaskDialogController {
    
    @FXML private TextField orderNoField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField productNoField;
    @FXML private TextField productNameField;
    @FXML private TextField productSpecField;
    @FXML private TextField collectionSpecField;
    @FXML private TextField plannedQuantityField;
    @FXML private TextField batchNoField;
    @FXML private DatePicker productionDatePicker;
    @FXML private DatePicker completionDatePicker;
    
    private TaskAddRequest taskAddRequest;
    private boolean confirmed = false;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("添加任务对话框初始化...");
        
        // 初始化类型下拉框
        typeComboBox.getItems().addAll("有箱码", "无箱码");
        typeComboBox.getSelectionModel().selectFirst();
        
        // 设置默认日期为当前日期
        productionDatePicker.setValue(LocalDate.now());
    }
    
    /**
     * 确认按钮
     */
    @FXML
    private void onConfirm() {
        System.out.println("确认添加任务");
        
        // 验证表单
        if (!validateForm()) {
            return;
        }
        
        // 构建请求对象
        taskAddRequest = new TaskAddRequest();
        
        // 基本信息
        taskAddRequest.setOrderNo(orderNoField.getText().trim());
        taskAddRequest.setType(typeComboBox.getSelectionModel().getSelectedIndex() + 1); // 1=有箱码, 2=无箱码
        
        // 产品信息
        taskAddRequest.setProductNo(productNoField.getText().trim());
        taskAddRequest.setProductName(productNameField.getText().trim());
        taskAddRequest.setProductFormatName(productSpecField.getText().trim());
        
        // 计划数量
        try {
            int plannedQty = Integer.parseInt(plannedQuantityField.getText().trim());
            taskAddRequest.setProductCount(plannedQty);
            taskAddRequest.setOrderSumCount(plannedQty);
            taskAddRequest.setProductSumCount(plannedQty);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "计划数量必须是有效的数字");
            return;
        }
        
        // 生产批次
        taskAddRequest.setSyBatchNo(batchNoField.getText());
        
        // 采集规格（每垛箱数）
        try {
            if (collectionSpecField.getText() != null && !collectionSpecField.getText().trim().isEmpty()) {
                int ratio = Integer.parseInt(collectionSpecField.getText().trim());
                taskAddRequest.setRatio(ratio);
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "采集规格必须是有效的数字");
            return;
        }
        
        // 生产日期
        if (productionDatePicker.getValue() != null) {
            LocalDateTime productTime = LocalDateTime.of(productionDatePicker.getValue(), LocalTime.of(8, 0));
            taskAddRequest.setProductTime(productTime);
            taskAddRequest.setDmakeDate(productTime);
        }
        
        // 预计完工时间
        if (completionDatePicker.getValue() != null) {
            LocalDateTime completionTime = LocalDateTime.of(completionDatePicker.getValue(), LocalTime.of(18, 0));
            taskAddRequest.setTwillendTime(completionTime);
        }
        
        // 设置产品ID和规格ID（模拟数据）
        taskAddRequest.setProductId(1);
        taskAddRequest.setProductFormatId(1);
        
        confirmed = true;
        closeDialog();
    }
    
    /**
     * 取消按钮
     */
    @FXML
    private void onCancel() {
        System.out.println("取消添加任务");
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 验证表单
     */
    private boolean validateForm() {
        // 检查生产订单号
        if (orderNoField.getText() == null || orderNoField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "生产订单号不能为空");
            return false;
        }
        
        // 检查类型
        if (typeComboBox.getSelectionModel().getSelectedIndex() < 0) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择类型");
            return false;
        }
        
        // 检查产品编号
        if (productNoField.getText() == null || productNoField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "产品编号不能为空");
            return false;
        }
        
        // 检查产品名称
        if (productNameField.getText() == null || productNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "产品名称不能为空");
            return false;
        }
        
        // 检查产品规格
        if (productSpecField.getText() == null || productSpecField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "产品规格不能为空");
            return false;
        }
        
        // 检查采集规格
        if (collectionSpecField.getText() == null || collectionSpecField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "采集规格（每垛箱数）不能为空");
            return false;
        }
        
        try {
            int ratio = Integer.parseInt(collectionSpecField.getText().trim());
            if (ratio <= 0) {
                showAlert(Alert.AlertType.WARNING, "提示", "采集规格（每垛箱数）必须大于0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "采集规格必须是有效的数字");
            return false;
        }
        
        // 检查计划数量
        if (plannedQuantityField.getText() == null || plannedQuantityField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "计划数量不能为空");
            return false;
        }
        
        try {
            int qty = Integer.parseInt(plannedQuantityField.getText().trim());
            if (qty <= 0) {
                showAlert(Alert.AlertType.WARNING, "提示", "计划数量必须大于0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "计划数量必须是有效的数字");
            return false;
        }
        
        // 检查生产日期
        if (productionDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择生产日期");
            return false;
        }
        
        return true;
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) orderNoField.getScene().getWindow();
        stage.close();
    }
    
    /**
     * 显示提示对话框
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * 是否确认添加
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * 获取任务添加请求
     */
    public TaskAddRequest getTaskAddRequest() {
        return taskAddRequest;
    }
}

