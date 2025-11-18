package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.operatelog.OperateLogQueryDTO;
import com.miduo.cloud.entity.dto.operatelog.OperateLogVO;
import com.miduo.cloud.frontend.util.HttpUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 操作日志页面控制器
 */
public class OperateLogController {
    
    // 顶部筛选控件
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button searchButton;
    @FXML private Button resetButton;
    
    // 统计信息
    @FXML private Label statsLabel;
    
    // 表格控件
    @FXML private TableView<OperateLogVO> logTableView;
    @FXML private TableColumn<OperateLogVO, Long> idColumn;
    @FXML private TableColumn<OperateLogVO, String> operatorNameColumn;
    @FXML private TableColumn<OperateLogVO, String> moduleNameColumn;
    @FXML private TableColumn<OperateLogVO, String> operateTypeColumn;
    @FXML private TableColumn<OperateLogVO, String> operateContentColumn;
    @FXML private TableColumn<OperateLogVO, String> operateResultColumn;
    @FXML private TableColumn<OperateLogVO, LocalDateTime> operateTimeColumn;
    @FXML private TableColumn<OperateLogVO, String> remarkColumn;
    
    // 分页控件
    @FXML private Button firstPageButton;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button lastPageButton;
    @FXML private Label currentPageLabel;
    @FXML private Label totalPageLabel;
    @FXML private ComboBox<String> pageSizeComboBox;
    @FXML private Label totalRecordsLabel;
    @FXML private Label currentTimeLabel;
    
    // 分页状态
    private long currentPage = 1;
    private long totalPages = 1;
    private long pageSize = 20;
    private long totalRecords = 0;
    
    private Timer timer;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("操作日志页面初始化...");
        
        // 初始化表格
        initializeTable();
        
        // 禁止列重新排序
        logTableView.getColumns().forEach(column -> column.setReorderable(false));
        
        // 禁止列排序
        logTableView.getColumns().forEach(column -> column.setSortable(false));
        
        // 初始化分页控件
        initializePagination();
        
        // 加载第一页数据
        loadData();
        
        // 启动实时时钟
        startRealtimeClock();
    }
    
    /**
     * 初始化表格
     */
    private void initializeTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        operatorNameColumn.setCellValueFactory(new PropertyValueFactory<>("operatorName"));
        moduleNameColumn.setCellValueFactory(new PropertyValueFactory<>("moduleName"));
        operateTypeColumn.setCellValueFactory(new PropertyValueFactory<>("operateType"));
        operateContentColumn.setCellValueFactory(new PropertyValueFactory<>("operateContent"));
        operateResultColumn.setCellValueFactory(new PropertyValueFactory<>("operateResult"));
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));
        
        // 操作时间列格式化
        operateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("operateTime"));
        operateTimeColumn.setCellFactory(column -> new TableCell<OperateLogVO, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(formatter));
                }
            }
        });
        
        // 设置表格自适应列宽
        logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 添加Tooltip和去除选中样式
        addRowFactoryWithTooltip();
    }
    
    /**
     * 为表格添加Tooltip和去除选中样式
     */
    private void addRowFactoryWithTooltip() {
        logTableView.setRowFactory(tv -> {
            TableRow<OperateLogVO> row = new TableRow<OperateLogVO>() {
                private Tooltip tooltip = new Tooltip();
                
                {
                    // 配置Tooltip样式
                    tooltip.setShowDelay(Duration.millis(300));
                    tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #f5f5f5; -fx-text-fill: black;");
                }
                
                @Override
                protected void updateItem(OperateLogVO item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setTooltip(null);
                    } else {
                        // 构建详细信息字符串
                        StringBuilder sb = new StringBuilder();
                        sb.append("【详细信息】\n\n");
                        sb.append("ID: ").append(item.getId() != null ? item.getId() : "-").append("\n");
                        sb.append("操作人: ").append(item.getOperatorName() != null ? item.getOperatorName() : "-").append("\n");
                        sb.append("操作模块: ").append(item.getModuleName() != null ? item.getModuleName() : "-").append("\n");
                        sb.append("操作类型: ").append(item.getOperateType() != null ? item.getOperateType() : "-").append("\n");
                        sb.append("操作内容: ").append(item.getOperateContent() != null ? item.getOperateContent() : "-").append("\n");
                        sb.append("操作结果: ").append(item.getOperateResult() != null ? item.getOperateResult() : "-").append("\n");
                        
                        if (item.getOperateTime() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            sb.append("操作时间: ").append(item.getOperateTime().format(formatter)).append("\n");
                        }
                        
                        sb.append("备注: ").append(item.getRemark() != null && !item.getRemark().isEmpty() ? item.getRemark() : "-");
                        
                        tooltip.setText(sb.toString());
                        setTooltip(tooltip);
                    }
                }
            };
            
            // 去除行选中样式（保持字体颜色不变白）
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle(""); // 保持无样式
                }
            });
            
            return row;
        });
        
        // 去除选中时的焦点样式
        logTableView.setFocusTraversable(false);
    }
    
    /**
     * 初始化分页控件
     */
    private void initializePagination() {
        // 设置每页大小下拉框选项
        pageSizeComboBox.getItems().addAll("10", "20", "50", "100");
        pageSizeComboBox.setValue(String.valueOf(pageSize));
        pageSizeComboBox.setOnAction(event -> {
            String selected = pageSizeComboBox.getValue();
            if (selected != null) {
                pageSize = Integer.parseInt(selected);
                currentPage = 1; // 重置到第一页
                loadData();
            }
        });
        
        updatePaginationControls();
    }
    
    /**
     * 查询按钮点击事件
     */
    @FXML
    private void onSearch() {
        currentPage = 1;
        loadData();
    }
    
    /**
     * 重置按钮点击事件
     */
    @FXML
    private void onReset() {
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        currentPage = 1;
        loadData();
    }
    
    /**
     * 首页按钮点击事件
     */
    @FXML
    private void onFirstPage() {
        if (currentPage > 1) {
            currentPage = 1;
            loadData();
        }
    }
    
    /**
     * 上一页按钮点击事件
     */
    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadData();
        }
    }
    
    /**
     * 下一页按钮点击事件
     */
    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadData();
        }
    }
    
    /**
     * 末页按钮点击事件
     */
    @FXML
    private void onLastPage() {
        if (currentPage < totalPages) {
            currentPage = totalPages;
            loadData();
        }
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        new Thread(() -> {
            try {
                // 构建查询条件
                OperateLogQueryDTO queryDTO = new OperateLogQueryDTO();
                queryDTO.setCurrent(currentPage);
                queryDTO.setSize(pageSize);
                
                // 日期筛选
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                
                if (startDate != null) {
                    queryDTO.setStartTime(LocalDateTime.of(startDate, LocalTime.MIN));
                }
                if (endDate != null) {
                    queryDTO.setEndTime(LocalDateTime.of(endDate, LocalTime.MAX));
                }
                
                // 调用后端接口
                String responseJson = HttpUtil.doPost("/api/operateLog/query", queryDTO);
                ApiResult<PageOutput<OperateLogVO>> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<PageOutput<OperateLogVO>>>() {});
                
                if (result != null && result.getCode() == 200) {
                    PageOutput<OperateLogVO> pageOutput = result.getData();
                    
                    Platform.runLater(() -> {
                        // 更新表格数据
                        logTableView.getItems().clear();
                        if (pageOutput.getRecords() != null) {
                            logTableView.getItems().addAll(pageOutput.getRecords());
                        }
                        
                        // 更新分页信息
                        totalPages = pageOutput.getPages();
                        totalRecords = pageOutput.getTotal();
                        
                        updatePaginationControls();
                        updateStatsLabel();
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "错误", 
                            result != null ? result.getMessage() : "查询失败");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "查询失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 更新分页控件状态
     */
    private void updatePaginationControls() {
        currentPageLabel.setText(String.valueOf(currentPage));
        totalPageLabel.setText(String.valueOf(totalPages));
        totalRecordsLabel.setText(String.valueOf(totalRecords));
        
        // 更新按钮状态
        firstPageButton.setDisable(currentPage <= 1);
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
        lastPageButton.setDisable(currentPage >= totalPages);
    }
    
    /**
     * 更新统计标签
     */
    private void updateStatsLabel() {
        statsLabel.setText(String.format("共%d条记录", (int)totalRecords));
    }
    
    /**
     * 启动实时时钟
     */
    private void startRealtimeClock() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    currentTimeLabel.setText(LocalDateTime.now().format(formatter));
                });
            }
        }, 0, 1000);
    }
    
    /**
     * 显示提示框
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

