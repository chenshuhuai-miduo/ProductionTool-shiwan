package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.dto.task.TaskUpdateRequest;
import com.miduo.cloud.entity.dto.task.TaskVO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 编辑任务对话框控制器
 */
public class EditTaskDialogController {
    
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
    
    private TaskVO originalTask; // 原始任务数据
    private TaskUpdateRequest taskUpdateRequest;
    private boolean confirmed = false;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("编辑任务对话框初始化...");
        
        // 初始化类型下拉框
        typeComboBox.getItems().addAll("有箱码", "无箱码");
    }
    
    /**
     * 设置要编辑的任务数据
     */
    public void setTask(TaskVO task) {
        this.originalTask = task;
        
        // 填充表单
        orderNoField.setText(task.getOrderNo());
        typeComboBox.getSelectionModel().select(task.getType() == 1 ? 0 : 1);
        productNoField.setText(task.getProductNo());
        productNameField.setText(task.getProductName());
        productSpecField.setText(task.getProductFormatName());
        collectionSpecField.setText(task.getRatio() != null ? task.getRatio().toString() : "");
        plannedQuantityField.setText(task.getProductCount() != null ? task.getProductCount().toString() : "");
        batchNoField.setText(task.getSyBatchNo());
        
        // 设置日期
        if (task.getProductTime() != null) {
            productionDatePicker.setValue(task.getProductTime().toLocalDate());
        }
        if (task.getTwillendTime() != null) {
            completionDatePicker.setValue(task.getTwillendTime().toLocalDate());
        }
    }
    
    /**
     * 确认按钮
     */
    @FXML
    private void onConfirm() {
        System.out.println("确认修改任务");
        
        // 验证表单
        if (!validateForm()) {
            return;
        }
        
        // 构建请求对象
        taskUpdateRequest = new TaskUpdateRequest();
        
        // 设置ID（必须）
        taskUpdateRequest.setId(originalTask.getId());
        
        // 基本信息
        taskUpdateRequest.setOrderNo(orderNoField.getText());
        taskUpdateRequest.setType(typeComboBox.getSelectionModel().getSelectedIndex() + 1); // 1=有箱码, 2=无箱码
        
        // 产品信息
        taskUpdateRequest.setProductNo(productNoField.getText());
        taskUpdateRequest.setProductName(productNameField.getText());
        taskUpdateRequest.setProductFormatName(productSpecField.getText());
        
        // 计划数量
        try {
            int plannedQty = Integer.parseInt(plannedQuantityField.getText().trim());
            taskUpdateRequest.setProductCount(plannedQty);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "计划数量必须是有效的数字");
            return;
        }
        
        // 生产批次
        taskUpdateRequest.setSyBatchNo(batchNoField.getText());
        
        // 采集规格（拖箱比例）
        try {
            if (collectionSpecField.getText() != null && !collectionSpecField.getText().trim().isEmpty()) {
                int ratio = Integer.parseInt(collectionSpecField.getText().trim());
                taskUpdateRequest.setRatio(ratio);
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "采集规格必须是有效的数字");
            return;
        }
        
        // 生产日期
        if (productionDatePicker.getValue() != null) {
            LocalDateTime productTime = LocalDateTime.of(productionDatePicker.getValue(), LocalTime.of(8, 0));
            taskUpdateRequest.setProductTime(productTime);
        }
        
        // 预计完工时间
        if (completionDatePicker.getValue() != null) {
            LocalDateTime completionTime = LocalDateTime.of(completionDatePicker.getValue(), LocalTime.of(18, 0));
            taskUpdateRequest.setTwillendTime(completionTime);
        }
        
        // 设置产品ID和规格ID（从原始数据获取）
        taskUpdateRequest.setProductId(originalTask.getOrderId());
        taskUpdateRequest.setProductFormatId(1); // TODO: 从原始数据获取
        
        confirmed = true;
        closeDialog();
    }
    
    /**
     * 取消按钮
     */
    @FXML
    private void onCancel() {
        System.out.println("取消修改任务");
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 验证表单
     */
    private boolean validateForm() {
        // 检查类型
        if (typeComboBox.getSelectionModel().getSelectedIndex() < 0) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择类型");
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
     * 是否确认修改
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * 获取任务修改请求
     */
    public TaskUpdateRequest getTaskUpdateRequest() {
        return taskUpdateRequest;
    }
}

