package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.dto.task.*;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.miduo.cloud.frontend.util.StageIconUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 任务管理控制器
 * 功能：任务列表管理界面
 * 
 * API路径已验证与重构后端一致:
 * - POST /api/task/add
 * - PUT /api/task/update/{id}
 * - DELETE /api/task/delete/{id}
 * - POST /api/task/page
 * - GET /api/task/get/{orderNo}
 */
public class TaskManagementController {
    
    // 筛选控件
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TextField orderSearchField;
    @FXML private TextField productSearchField;
    @FXML private DatePicker productTimeStartPicker;
    @FXML private DatePicker productTimeEndPicker;
    
    // 表格控件
    @FXML private TableView<TaskVO> taskTableView;
    @FXML private TableColumn<TaskVO, String> orderColumn;
    @FXML private TableColumn<TaskVO, String> productCodeColumn;
    @FXML private TableColumn<TaskVO, String> productNameColumn;
    @FXML private TableColumn<TaskVO, String> productSpecColumn;
    @FXML private TableColumn<TaskVO, Integer> plannedQuantityColumn;
    @FXML private TableColumn<TaskVO, Integer> completedQuantityColumn;
    @FXML private TableColumn<TaskVO, String> batchColumn;
    @FXML private TableColumn<TaskVO, LocalDateTime> expectedCompletionColumn;
    @FXML private TableColumn<TaskVO, LocalDateTime> orderDateColumn;
    @FXML private TableColumn<TaskVO, String> statusColumn;
    
    // 分页控件
    @FXML private Label totalCountLabel;
    @FXML private ComboBox<String> pageSizeComboBox;
    @FXML private TextField currentPageField;
    @FXML private Label pageInfoLabel;
    
    // 数据列表
    private ObservableList<TaskVO> taskList = FXCollections.observableArrayList();
    
    // 分页信息
    private long totalRecords = 0;
    private long totalPages = 0;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("任务管理界面初始化...");
        
        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部状态", "待生产", "生产中", "已完成"
        ));
        statusFilterComboBox.getSelectionModel().selectFirst();
        
        // 初始化每页显示数量下拉框
        pageSizeComboBox.setItems(FXCollections.observableArrayList(
            "10", "20", "50", "100"
        ));
        pageSizeComboBox.getSelectionModel().select("20"); // 默认每页20条
        
        currentPageField.setText("1");
        
        // 设置表格列宽自适应策略
        taskTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 禁止列重新排序
        taskTableView.getColumns().forEach(column -> column.setReorderable(false));
        
        // 禁止列排序
        taskTableView.getColumns().forEach(column -> column.setSortable(false));
        
        // 初始化表格列
        initializeTableColumns();
        
        // 初始化分页信息
        updatePaginationInfo();
        
        // 设置搜索框的回车事件
        orderSearchField.setOnAction(event -> onSearch());
        productSearchField.setOnAction(event -> onSearch());
        
        // 加载数据
        loadTaskData();
    }
    
    /**
     * 初始化表格列
     */
    private void initializeTableColumns() {
        // 设置列的数据绑定
        orderColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        productCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productNo"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productSpecColumn.setCellValueFactory(new PropertyValueFactory<>("productFormatName"));
        plannedQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("productCount"));
        completedQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("orderCount"));
        batchColumn.setCellValueFactory(new PropertyValueFactory<>("syBatchNo"));
        
        // 添加Tooltip支持的文本列
        addTooltipToColumn(orderColumn);
        addTooltipToColumn(productCodeColumn);
        addTooltipToColumn(productNameColumn);
        addTooltipToColumn(productSpecColumn);
        addTooltipToColumn(batchColumn);
        
        // 数字列也添加Tooltip
        addTooltipToNumberColumn(plannedQuantityColumn);
        addTooltipToNumberColumn(completedQuantityColumn);
        
        // 时间列格式化
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        expectedCompletionColumn.setCellValueFactory(new PropertyValueFactory<>("productTime"));
        expectedCompletionColumn.setCellFactory(column -> new TableCell<TaskVO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String text = item.format(formatter);
                    setText(text);
                    // 添加Tooltip
                    Tooltip tooltip = new Tooltip(text);
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    setTooltip(tooltip);
                }
            }
        });
        
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        orderDateColumn.setCellFactory(column -> new TableCell<TaskVO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String text = item.format(formatter);
                    setText(text);
                    // 添加Tooltip
                    Tooltip tooltip = new Tooltip(text);
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    setTooltip(tooltip);
                }
            }
        });
        
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("orderStatusText"));
        addTooltipToColumn(statusColumn);
        
        // 设置表格行样式 - 去除选中时文字变白的效果
        taskTableView.setRowFactory(tv -> {
            TableRow<TaskVO> row = new TableRow<>();
            
            // 点击行时不改变样式（去除选中效果）
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle(""); // 保持无样式
                }
            });
            
            return row;
        });
        
        // 绑定数据
        taskTableView.setItems(taskList);
        
        // 去除选中时的焦点样式
        taskTableView.setFocusTraversable(false);
        
        System.out.println("表格列初始化完成");
    }
    
    /**
     * 为文本列添加Tooltip支持
     */
    private void addTooltipToColumn(TableColumn<TaskVO, String> column) {
        column.setCellFactory(col -> new TableCell<TaskVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    // 添加Tooltip，显示完整内容
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    setTooltip(tooltip);
                }
            }
        });
    }
    
    /**
     * 为数字列添加Tooltip支持
     */
    private void addTooltipToNumberColumn(TableColumn<TaskVO, Integer> column) {
        column.setCellFactory(col -> new TableCell<TaskVO, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String text = String.valueOf(item);
                    setText(text);
                    // 添加Tooltip
                    Tooltip tooltip = new Tooltip(text);
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    setTooltip(tooltip);
                }
            }
        });
    }
    
    /**
     * 加载任务数据
     * API: POST /api/task/page
     */
    private void loadTaskData() {
        new Thread(() -> {
            try {
                // 构建查询请求
                TaskQueryRequest request = new TaskQueryRequest();
                request.setPageNum(Integer.parseInt(currentPageField.getText()));
                request.setPageSize(Integer.parseInt(pageSizeComboBox.getValue()));
                
                // 设置搜索条件
                String orderNo = orderSearchField.getText();
                if (orderNo != null && !orderNo.trim().isEmpty()) {
                    request.setOrderNo(orderNo.trim());
                }
                
                String productName = productSearchField.getText();
                if (productName != null && !productName.trim().isEmpty()) {
                    request.setProductName(productName.trim());
                }
                
                // 预计生产时间范围
                if (productTimeStartPicker.getValue() != null) {
                    LocalDate startDate = productTimeStartPicker.getValue();
                    request.setProductTimeStart(LocalDateTime.of(startDate, LocalTime.MIN));
                }
                if (productTimeEndPicker.getValue() != null) {
                    LocalDate endDate = productTimeEndPicker.getValue();
                    request.setProductTimeEnd(LocalDateTime.of(endDate, LocalTime.MAX));
                }
                
                // 状态筛选
                String status = statusFilterComboBox.getValue();
                if (!"全部状态".equals(status)) {
                    if ("待生产".equals(status)) {
                        request.setOrderStatus(0);
                    } else if ("生产中".equals(status)) {
                        request.setOrderStatus(1);
                    } else if ("已完成".equals(status)) {
                        request.setOrderStatus(2);
                    }
                }
                
                // 调用重构后的接口: POST /api/task/page
                String responseJson = HttpUtil.doPost("/api/task/page", request);
                
                // 解析响应 (Result → ApiResult, PageResult → PageOutput)
                ApiResult<PageOutput<TaskVO>> result = HttpUtil.parseJson(responseJson, 
                    new TypeReference<ApiResult<PageOutput<TaskVO>>>() {});
                
                // 在JavaFX线程更新UI
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null) {
                        PageOutput<TaskVO> pageResult = result.getData();
                        taskList.clear();
                        taskList.addAll(pageResult.getRecords());
                        
                        // 更新分页信息
                        totalRecords = pageResult.getTotal();
                        totalPages = pageResult.getPages();
                        updatePaginationInfo();
                        
                        System.out.println("加载任务数据成功，共" + totalRecords + "条");
                        
                        // 记录查询操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(OperateTypeEnum.QUERY)
                            .content("查询任务列表: 页码=" + request.getPageNum() + 
                                    ", 总记录数=" + totalRecords +
                                    (request.getOrderNo() != null ? ", 订单号=" + request.getOrderNo() : "") +
                                    (request.getSyBatchNo() != null ? ", 批次号=" + request.getSyBatchNo() : "") +
                                    (request.getProductName() != null ? ", 产品=" + request.getProductName() : ""))
                            .saveAsync();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "错误", "加载数据失败：" + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "加载数据异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    // ==================== 操作按钮事件 ====================
    
    @FXML
    private void onAddTask() {
        System.out.println("添加任务");
        
        try {
            // 加载添加任务对话框FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/AddTaskDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // 获取控制器
            AddTaskDialogController dialogController = loader.getController();
            
            // 创建对话框Stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("添加任务");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(root));
            StageIconUtil.setStageIcon(dialogStage);
            dialogStage.showAndWait();
            
            // 检查是否确认添加
            if (dialogController.isConfirmed()) {
                TaskAddRequest request = dialogController.getTaskAddRequest();
                
                // 调用接口: POST /api/task/add
                addTaskToServer(request);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "打开添加任务对话框失败：" + e.getMessage());
        }
    }
    
    /**
     * 调用后端接口添加任务
     * API: POST /api/task/add
     */
    private void addTaskToServer(TaskAddRequest request) {
        new Thread(() -> {
            try {
                System.out.println("正在添加任务：" + request.getOrderNo());
                
                // 调用重构后的接口
                String responseJson = HttpUtil.doPost("/api/task/add", request);
                ApiResult<String> result = HttpUtil.parseJson(responseJson, 
                    new TypeReference<ApiResult<String>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", 
                            "任务添加成功！\n订单编号：" + result.getData());
                        // 刷新列表
                        loadTaskData();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "错误", 
                            "任务添加失败：" + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", 
                        "添加任务异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void onSyncTasks() {
        System.out.println("自动同步任务");
        showAlert(Alert.AlertType.INFORMATION, "提示", "自动同步功能（待实现）");
    }
    
    /**
     * 修改任务
     * API: PUT /api/task/update/{id}
     */
    private void onEditTask(TaskVO task) {
        System.out.println("修改任务：" + task.getOrderNo());
        
        try {
            // 加载编辑任务对话框FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/EditTaskDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // 获取控制器并设置任务数据
            EditTaskDialogController dialogController = loader.getController();
            dialogController.setTask(task);
            
            // 创建对话框Stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("修改任务");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(root));
            StageIconUtil.setStageIcon(dialogStage);
            dialogStage.showAndWait();
            
            // 检查是否确认修改
            if (dialogController.isConfirmed()) {
                TaskUpdateRequest request = dialogController.getTaskUpdateRequest();
                
                // 调用接口: PUT /api/task/update/{id}
                updateTaskToServer(request);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "打开修改任务对话框失败：" + e.getMessage());
        }
    }
    
    /**
     * 调用后端接口修改任务
     * API: PUT /api/task/update/{id}
     */
    private void updateTaskToServer(TaskUpdateRequest request) {
        new Thread(() -> {
            try {
                System.out.println("正在修改任务，ID：" + request.getId());
                
                // 调用重构后的接口：PUT /api/task/update/{id}
                String responseJson = HttpUtil.doPut("/api/task/update/" + request.getId(), request);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson, 
                    new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", "任务修改成功！");
                        // 刷新列表
                        loadTaskData();
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(OperateTypeEnum.UPDATE)
                            .target(request.getId().toString(), request.getOrderNo())
                            .content("更新任务: " + request.getOrderNo())
                            .afterData(request)
                            .saveAsync();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "错误", "任务修改失败：" + result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(OperateTypeEnum.UPDATE)
                            .target(request.getId().toString(), request.getOrderNo())
                            .content("更新任务失败: " + request.getOrderNo())
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "修改任务异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 删除任务
     * API: DELETE /api/task/delete/{id}
     */
    private void onDeleteTask(TaskVO task) {
        System.out.println("删除任务，ID：" + task.getId() + "，订单编号：" + task.getOrderNo());
        
        // 确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("确定要删除该任务吗？");
        confirmAlert.setContentText("订单编号：" + task.getOrderNo() + "\n产品名称：" + task.getProductName());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        System.out.println("正在删除任务，ID：" + task.getId());
                        
                        // 调用重构后的接口：DELETE /api/task/delete/{id}
                        String responseJson = HttpUtil.doDelete("/api/task/delete/" + task.getId());
                        ApiResult<Boolean> result = HttpUtil.parseJson(responseJson, 
                            new TypeReference<ApiResult<Boolean>>() {});
                        
                        Platform.runLater(() -> {
                            if (result.getCode() == 200) {
                                showAlert(Alert.AlertType.INFORMATION, "成功", "任务删除成功！");
                                // 刷新列表
                                loadTaskData();
                                
                                // 记录操作日志
                                OperateLogBuilder.create()
                                    .module(ModuleNameEnum.TASK_MANAGEMENT)
                                    .operateType(OperateTypeEnum.DELETE)
                                    .target(task.getId().toString(), task.getOrderNo())
                                    .content("删除任务: " + task.getOrderNo() + " - " + task.getProductName())
                                    .saveAsync();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "错误", "任务删除失败：" + result.getMessage());
                                
                                // 记录失败日志
                                OperateLogBuilder.create()
                                    .module(ModuleNameEnum.TASK_MANAGEMENT)
                                    .operateType(OperateTypeEnum.DELETE)
                                    .target(task.getId().toString(), task.getOrderNo())
                                    .content("删除任务失败: " + task.getOrderNo())
                                    .failReason(result.getMessage())
                                    .saveAsync();
                            }
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "错误", "删除任务异常：" + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    // ==================== 筛选事件 ====================
    
    @FXML
    private void onSearch() {
        System.out.println("执行搜索");
        currentPageField.setText("1"); // 重置到第一页
        loadTaskData();
    }
    
    @FXML
    private void onClearSearch() {
        System.out.println("清空搜索条件");
        
        // 清空所有搜索框
        statusFilterComboBox.getSelectionModel().selectFirst();
        orderSearchField.clear();
        productSearchField.clear();
        productTimeStartPicker.setValue(null);
        productTimeEndPicker.setValue(null);
        
        // 重新加载数据
        currentPageField.setText("1");
        loadTaskData();
    }
    
    // ==================== 分页事件 ====================
    
    @FXML
    private void onFirstPage() {
        System.out.println("跳转到首页");
        currentPageField.setText("1");
        loadTaskData();
        updatePaginationInfo();
    }
    
    @FXML
    private void onPreviousPage() {
        System.out.println("上一页");
        int currentPage = Integer.parseInt(currentPageField.getText());
        if (currentPage > 1) {
            currentPageField.setText(String.valueOf(currentPage - 1));
            loadTaskData();
            updatePaginationInfo();
        }
    }
    
    @FXML
    private void onNextPage() {
        System.out.println("下一页");
        int currentPage = Integer.parseInt(currentPageField.getText());
        if (currentPage < totalPages) {
            currentPageField.setText(String.valueOf(currentPage + 1));
            loadTaskData();
            updatePaginationInfo();
        }
    }
    
    @FXML
    private void onLastPage() {
        System.out.println("跳转到末页");
        if (totalPages > 0) {
            currentPageField.setText(String.valueOf(totalPages));
            loadTaskData();
            updatePaginationInfo();
        }
    }
    
    @FXML
    private void onPageSizeChange() {
        System.out.println("每页显示数量改变：" + pageSizeComboBox.getValue());
        currentPageField.setText("1"); // 重置到第一页
        loadTaskData();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 更新分页信息显示
     */
    private void updatePaginationInfo() {
        totalCountLabel.setText("共 " + totalRecords + " 条");
        pageInfoLabel.setText(currentPageField.getText() + " / " + (totalPages > 0 ? totalPages : 1));
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
}

