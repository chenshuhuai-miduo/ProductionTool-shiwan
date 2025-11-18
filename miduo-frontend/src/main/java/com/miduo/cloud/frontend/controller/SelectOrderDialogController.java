package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.task.TaskQueryRequest;
import com.miduo.cloud.entity.dto.task.TaskVO;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 选择生产订单对话框控制器
 */
public class SelectOrderDialogController {
    
    @FXML private TextField orderSearchField;
    @FXML private TextField productSearchField;
    @FXML private DatePicker productTimeStartPicker;
    @FXML private DatePicker productTimeEndPicker;
    
    @FXML private TableView<TaskVO> taskTableView;
    @FXML private TableColumn<TaskVO, String> orderColumn;
    @FXML private TableColumn<TaskVO, String> productCodeColumn;
    @FXML private TableColumn<TaskVO, String> productNameColumn;
    @FXML private TableColumn<TaskVO, String> productSpecColumn;
    @FXML private TableColumn<TaskVO, Integer> plannedQuantityColumn;
    @FXML private TableColumn<TaskVO, Integer> completedQuantityColumn;
    @FXML private TableColumn<TaskVO, LocalDateTime> productionDateColumn;
    @FXML private TableColumn<TaskVO, String> statusColumn;
    
    @FXML private Label totalCountLabel;
    @FXML private TextField currentPageField;
    @FXML private Label totalPagesLabel;
    
    private ObservableList<TaskVO> taskList = FXCollections.observableArrayList();
    private int totalPages = 0;
    private int pageSize = 20; // 固定每页20条
    
    private TaskVO selectedTask = null;
    private boolean confirmed = false;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("选择订单对话框初始化...");
        
        // 设置表格列宽自适应策略
        taskTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 禁止列重新排序
        taskTableView.getColumns().forEach(column -> column.setReorderable(false));
        
        // 禁止列排序
        taskTableView.getColumns().forEach(column -> column.setSortable(false));
        
        initializeTableColumns();
        
        // 设置当前页输入框的回车事件
        currentPageField.setOnAction(event -> onCurrentPageEnter());
        
        // 设置搜索框的回车事件
        orderSearchField.setOnAction(event -> onSearch());
        productSearchField.setOnAction(event -> onSearch());
        
        // 自动加载第一页数据
        loadTaskData();
    }
    
    /**
     * 初始化表格列
     */
    private void initializeTableColumns() {
        orderColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        productCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productNo"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productSpecColumn.setCellValueFactory(new PropertyValueFactory<>("productFormatName"));
        plannedQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("productCount"));
        completedQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("orderCount"));
        
        // 添加Tooltip支持的文本列
        addTooltipToColumn(orderColumn);
        addTooltipToColumn(productCodeColumn);
        addTooltipToColumn(productNameColumn);
        addTooltipToColumn(productSpecColumn);
        addTooltipToColumn(statusColumn);
        
        // 数字列也添加Tooltip
        addTooltipToNumberColumn(plannedQuantityColumn);
        addTooltipToNumberColumn(completedQuantityColumn);
        
        // 生产日期格式化
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        productionDateColumn.setCellValueFactory(new PropertyValueFactory<>("productTime"));
        productionDateColumn.setCellFactory(column -> new TableCell<TaskVO, LocalDateTime>() {
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
        
        // 状态列
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("orderStatusText"));
        
        // 设置表格选择模式为单选
        taskTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
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
     */
    private void loadTaskData() {
        new Thread(() -> {
            try {
                // 构建查询请求
                TaskQueryRequest request = new TaskQueryRequest();
                request.setPageNum(Integer.parseInt(currentPageField.getText()));
                request.setPageSize(pageSize);
                
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
                
                System.out.println("查询参数 - 页码: " + request.getPageNum() + ", 每页: " + request.getPageSize());
                
                // 调用接口 - 使用新的可选择任务接口V2（在数据库层面直接过滤状态，只查询待生产和生产中）
                String responseJson = HttpUtil.doPost("/api/task/page/selectable/v2", request);
                ApiResult<PageOutput<TaskVO>> result = HttpUtil.parseJson(responseJson, 
                    new TypeReference<ApiResult<PageOutput<TaskVO>>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null) {
                        PageOutput<TaskVO> pageResult = result.getData();
                        
                        // 更新表格数据
                        taskList.clear();
                        taskList.addAll(pageResult.getRecords());
                        
                        // 更新分页信息
                        totalCountLabel.setText("总数: " + pageResult.getTotal());
                        totalPages = pageResult.getPages().intValue();
                        totalPagesLabel.setText("/ " + totalPages + " 页");
                        
                        System.out.println("加载成功 - 共" + pageResult.getTotal() + "条，" + totalPages + "页");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "错误", "加载任务失败: " + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "加载任务异常: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 搜索按钮
     */
    @FXML
    private void onSearch() {
        System.out.println("执行搜索");
        currentPageField.setText("1"); // 重置到第一页
        loadTaskData();
    }
    
    /**
     * 清空搜索条件
     */
    @FXML
    private void onClearSearch() {
        System.out.println("清空搜索条件");
        orderSearchField.clear();
        productSearchField.clear();
        productTimeStartPicker.setValue(null);
        productTimeEndPicker.setValue(null);
        currentPageField.setText("1");
        loadTaskData();
    }
    
    /**
     * 首页
     */
    @FXML
    private void onFirstPage() {
        currentPageField.setText("1");
        loadTaskData();
    }
    
    /**
     * 上一页
     */
    @FXML
    private void onPreviousPage() {
        int currentPage = Integer.parseInt(currentPageField.getText());
        if (currentPage > 1) {
            currentPageField.setText(String.valueOf(currentPage - 1));
            loadTaskData();
        }
    }
    
    /**
     * 下一页
     */
    @FXML
    private void onNextPage() {
        int currentPage = Integer.parseInt(currentPageField.getText());
        if (currentPage < totalPages) {
            currentPageField.setText(String.valueOf(currentPage + 1));
            loadTaskData();
        }
    }
    
    /**
     * 末页
     */
    @FXML
    private void onLastPage() {
        currentPageField.setText(String.valueOf(totalPages));
        loadTaskData();
    }
    
    /**
     * 当前页输入框回车事件
     */
    private void onCurrentPageEnter() {
        try {
            int page = Integer.parseInt(currentPageField.getText());
            if (page >= 1 && page <= totalPages) {
                loadTaskData();
            } else {
                showAlert(Alert.AlertType.WARNING, "提示", "页码超出范围（1-" + totalPages + "）");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入有效的页码");
        }
    }
    
    /**
     * 确认选择
     */
    @FXML
    private void onConfirm() {
        System.out.println("确认选择订单");
        
        // 获取选中的任务
        selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        
        if (selectedTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择一个订单");
            return;
        }
        
        confirmed = true;
        closeDialog();
    }
    
    /**
     * 取消
     */
    @FXML
    private void onCancel() {
        System.out.println("取消选择订单");
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) taskTableView.getScene().getWindow();
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
     * 是否确认选择
     */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    /**
     * 获取选中的任务
     */
    public TaskVO getSelectedTask() {
        return selectedTask;
    }
}

