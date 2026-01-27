package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.dataupload.DataUploadOrderVO;
import com.miduo.cloud.entity.dto.dataupload.DataUploadTaskVO;
import com.miduo.cloud.frontend.util.HttpUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 数据上传页面控制器（分页版本）
 */
public class DataUploadController {
    
    // 顶部标签
    @FXML private Label statusLabel;
    @FXML private Label currentTimeLabel;
    
    // 左侧：生产订单 - 搜索
    @FXML private TextField orderSearchField;
    @FXML private TextField productSearchField;
    
    // 左侧：生产订单 - 表格
    @FXML private Label orderStatsLabel;
    @FXML private TableView<DataUploadOrderVO> orderTableView;
    @FXML private TableColumn<DataUploadOrderVO, String> orderNoColumn;
    @FXML private TableColumn<DataUploadOrderVO, String> productNameColumn;
    @FXML private TableColumn<DataUploadOrderVO, Integer> productCountColumn;
    @FXML private TableColumn<DataUploadOrderVO, Integer> completedCountColumn;
    @FXML private TableColumn<DataUploadOrderVO, LocalDateTime> productTimeColumn;
    @FXML private TableColumn<DataUploadOrderVO, Integer> productionStatusColumn;
    @FXML private TableColumn<DataUploadOrderVO, String> uploadStatusColumn;
    
    // 左侧：生产订单 - 分页
    @FXML private Label orderTotalCountLabel;
    @FXML private ComboBox<String> orderPageSizeComboBox;
    @FXML private TextField orderCurrentPageField;
    @FXML private Label orderPageInfoLabel;
    
    // 右侧：生产任务数据 - 搜索框
    @FXML private TextField codeSearchField;
    
    // 右侧：生产任务数据 - 表格
    @FXML private Label taskStatsLabel;
    @FXML private TableView<DataUploadTaskVO> taskTableView;
    @FXML private TableColumn<DataUploadTaskVO, String> layer1Column;
    @FXML private TableColumn<DataUploadTaskVO, LocalDateTime> addTimeColumn;
    @FXML private TableColumn<DataUploadTaskVO, String> layer2Column;
    @FXML private TableColumn<DataUploadTaskVO, String> layer3Column;
    @FXML private TableColumn<DataUploadTaskVO, String> layer4Column;
    
    // 右侧：生产任务数据 - 分页
    @FXML private Label taskTotalCountLabel;
    @FXML private ComboBox<String> taskPageSizeComboBox;
    @FXML private TextField taskCurrentPageField;
    @FXML private Label taskPageInfoLabel;
    
    private Timer timer;
    
    // 数据列表
    private ObservableList<DataUploadOrderVO> orderList = FXCollections.observableArrayList();
    private ObservableList<DataUploadTaskVO> taskList = FXCollections.observableArrayList();
    
    // 分页信息 - 订单
    private long orderTotalRecords = 0;
    private long orderTotalPages = 0;
    
    // 分页信息 - 任务
    private long taskTotalRecords = 0;
    private long taskTotalPages = 0;
    private String currentSelectedOrderNo = null;
    private String currentSelectedProductNo = null;  // 保存选中的产品编号
    
    // 码搜索相关
    private String currentSearchCode = null;  // 当前搜索的码
    private String searchedProductNo = null;  // 搜索到的码所属的ProductNO
    private String searchedVirtualSerialNumber = null;  // 搜索到的码的VirtualSerialNumber
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("数据上传界面初始化...");
        
        // 初始化分页下拉框
        orderPageSizeComboBox.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        orderPageSizeComboBox.getSelectionModel().select("20");
        orderCurrentPageField.setText("1");
        
        taskPageSizeComboBox.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        taskPageSizeComboBox.getSelectionModel().select("20");
        taskCurrentPageField.setText("1");
        
        // 初始化表格
        initializeOrderTable();
        initializeTaskTable();
        
        // 禁止列重新排序
        orderTableView.getColumns().forEach(column -> column.setReorderable(false));
        taskTableView.getColumns().forEach(column -> column.setReorderable(false));
        
        // 禁止列排序
        orderTableView.getColumns().forEach(column -> column.setSortable(false));
        taskTableView.getColumns().forEach(column -> column.setSortable(false));
        
        // 设置搜索框的回车事件
        orderSearchField.setOnAction(event -> onSearchOrders());
        productSearchField.setOnAction(event -> onSearchOrders());
        codeSearchField.setOnAction(event -> onSearchCode());
        
        // 加载生产订单数据
        loadProductionOrders();
        
        // 启动实时时钟
        startRealtimeClock();
    }
    
    /**
     * 初始化生产订单表格
     */
    private void initializeOrderTable() {
        // 订单号列 - 添加Tooltip
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        addTooltipToOrderColumn(orderNoColumn);
        
        // 产品名称列 - 添加Tooltip
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        addTooltipToOrderColumn(productNameColumn);
        
        // 计划数量列 - 添加Tooltip
        productCountColumn.setCellValueFactory(new PropertyValueFactory<>("productCount"));
        addTooltipToOrderNumberColumn(productCountColumn);
        
        // 完成数量列 - 添加Tooltip
        completedCountColumn.setCellValueFactory(new PropertyValueFactory<>("completedCount"));
        addTooltipToOrderNumberColumn(completedCountColumn);
        
        // 生产日期列格式化 - 添加Tooltip
        productTimeColumn.setCellValueFactory(new PropertyValueFactory<>("productTime"));
        productTimeColumn.setCellFactory(column -> new TableCell<DataUploadOrderVO, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
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
        
        // 生产状态列自定义渲染 - 添加Tooltip
        productionStatusColumn.setCellValueFactory(new PropertyValueFactory<>("productionStatus"));
        productionStatusColumn.setCellFactory(column -> new TableCell<DataUploadOrderVO, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    // 0:待生产, 1:生产中, 2:已完成, 3:生产中（未启用但有采集数据）
                    String text;
                    switch (item) {
                        case 0: text = "待生产"; break;
                        case 1: text = "生产中"; break;
                        case 2: text = "已完成"; break;
                        case 3: text = "生产中"; break; // 未启用但有采集数据，显示为"生产中"但需要点击启用任务
                        default: text = "未知";
                    }
                    setText(text);
                    // 添加Tooltip
                    Tooltip tooltip = new Tooltip(text);
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    setTooltip(tooltip);
                }
            }
        });
        
        // 上传状态列 - 添加Tooltip
        uploadStatusColumn.setCellValueFactory(new PropertyValueFactory<>("uploadStatus"));
        addTooltipToOrderColumn(uploadStatusColumn);
        
        // 设置表格自适应列宽
        orderTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 绑定数据
        orderTableView.setItems(orderList);
        
        // 设置表格行样式 - 标注搜索到的ProductNO对应的整行
        orderTableView.setRowFactory(tv -> {
            TableRow<DataUploadOrderVO> row = new TableRow<DataUploadOrderVO>() {
                @Override
                protected void updateItem(DataUploadOrderVO item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // 判断是否是搜索到的ProductNO
                        if (searchedProductNo != null && searchedProductNo.equals(item.getProductNo())) {
                            // 标注整行（使用黄色背景）
                            setStyle("-fx-background-color: #fff9c4;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
            
            // 点击行时不改变样式（去除选中效果）
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    // 重新应用当前样式，不被选中样式覆盖
                    DataUploadOrderVO item = row.getItem();
                    if (item != null && searchedProductNo != null && searchedProductNo.equals(item.getProductNo())) {
                        row.setStyle("-fx-background-color: #fff9c4;");
                    } else {
                        row.setStyle("");
                    }
                }
            });
            
            return row;
        });
        
        // 去除选中时的焦点样式
        orderTableView.setFocusTraversable(false);
        
        // 添加行选择监听器
        orderTableView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    currentSelectedOrderNo = newValue.getOrderNo();
                    currentSelectedProductNo = newValue.getProductNo();  // 保存选中的产品编号
                    taskCurrentPageField.setText("1");
                    loadProductionTasks();
                }
            }
        );
    }
    
    /**
     * 初始化生产任务表格
     */
    private void initializeTaskTable() {
        // 第一层列 - 添加Tooltip和搜索高亮（搜索SmallSerialNumber时红色显示）
        layer1Column.setCellValueFactory(new PropertyValueFactory<>("layer1"));
        layer1Column.setCellFactory(column -> new TableCell<DataUploadTaskVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(item);
                    // 判断是否是搜索的SmallSerialNumber（第一层）
                    if (currentSearchCode != null && currentSearchCode.equals(item)) {
                        // 搜索的SmallSerialNumber用红色字体显示
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                    // 添加Tooltip
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    setTooltip(tooltip);
                }
            }
        });
        
        // 箱码采集时间列格式化 - 添加Tooltip
        addTimeColumn.setCellValueFactory(new PropertyValueFactory<>("addTime"));
        addTimeColumn.setCellFactory(column -> new TableCell<DataUploadTaskVO, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
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
        
        // 第二层列 - 添加Tooltip
        layer2Column.setCellValueFactory(new PropertyValueFactory<>("layer2"));
        addTooltipToTaskColumn(layer2Column);
        
        // 第三层列 - 添加Tooltip
        layer3Column.setCellValueFactory(new PropertyValueFactory<>("layer3"));
        addTooltipToTaskColumn(layer3Column);
        
        // 第四层列 - 添加Tooltip
        layer4Column.setCellValueFactory(new PropertyValueFactory<>("layer4"));
        addTooltipToTaskColumn(layer4Column);
        
        // 设置表格自适应列宽
        taskTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 绑定数据
        taskTableView.setItems(taskList);
        
        // 设置表格行样式 - 去除选中时文字变白的效果
        taskTableView.setRowFactory(tv -> {
            TableRow<DataUploadTaskVO> row = new TableRow<>();
            
            // 点击行时不改变样式（去除选中效果）
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle(""); // 保持无样式
                }
            });
            
            return row;
        });
        
        // 去除选中时的焦点样式
        taskTableView.setFocusTraversable(false);
    }
    
    /**
     * 加载生产订单数据（分页）
     */
    private void loadProductionOrders() {
        new Thread(() -> {
            try {
                int pageNum = Integer.parseInt(orderCurrentPageField.getText());
                int pageSize = Integer.parseInt(orderPageSizeComboBox.getValue());
                String orderNo = orderSearchField.getText();
                String productName = productSearchField.getText();
                
                // 构建请求URL
                StringBuilder url = new StringBuilder("/api/dataupload/orders/page?pageNum=" + pageNum + "&pageSize=" + pageSize);
                if (orderNo != null && !orderNo.trim().isEmpty()) {
                    url.append("&orderNo=").append(orderNo.trim());
                }
                if (productName != null && !productName.trim().isEmpty()) {
                    url.append("&productName=").append(productName.trim());
                }
                
                String responseJson = HttpUtil.doGet(url.toString());
                ApiResult<PageOutput<DataUploadOrderVO>> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<PageOutput<DataUploadOrderVO>>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null) {
                        PageOutput<DataUploadOrderVO> pageResult = result.getData();
                        orderList.clear();
                        orderList.addAll(pageResult.getRecords());
                        
                        // 更新分页信息
                        orderTotalRecords = pageResult.getTotal();
                        orderTotalPages = pageResult.getPages();
                        updateOrderPaginationInfo();
                        
                        System.out.println("加载生产订单成功：" + orderTotalRecords + "条");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "错误", "加载订单失败：" + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "错误", "加载订单异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 加载生产任务数据（分页）
     * 根据订单号和产品编号查询CodeRelationUpload表数据
     */
    private void loadProductionTasks() {
        if (currentSelectedOrderNo == null || currentSelectedOrderNo.isEmpty()) {
            return;
        }
        
        new Thread(() -> {
            try {
                int pageNum = Integer.parseInt(taskCurrentPageField.getText());
                int pageSize = Integer.parseInt(taskPageSizeComboBox.getValue());
                
                // 构建URL，添加productNo和virtualSerialNumber参数
                StringBuilder urlBuilder = new StringBuilder("/api/dataupload/tasks/")
                    .append(currentSelectedOrderNo)
                    .append("/page?pageNum=").append(pageNum)
                    .append("&pageSize=").append(pageSize);
                
                // 添加productNo参数（用于区分同一订单下的不同产品）
                if (currentSelectedProductNo != null && !currentSelectedProductNo.isEmpty()) {
                    urlBuilder.append("&productNo=").append(currentSelectedProductNo);
                }
                
                // 添加virtualSerialNumber参数（用于搜索码后只显示相同VirtualSerialNumber的数据）
                // 需要进行URL编码，因为VirtualSerialNumber可能包含特殊字符（如#）
                if (searchedVirtualSerialNumber != null && !searchedVirtualSerialNumber.isEmpty()) {
                    try {
                        String encodedVirtualSerialNumber = java.net.URLEncoder.encode(searchedVirtualSerialNumber, "UTF-8");
                        urlBuilder.append("&virtualSerialNumber=").append(encodedVirtualSerialNumber);
                    } catch (java.io.UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                
                String url = urlBuilder.toString();
                String responseJson = HttpUtil.doGet(url);
                ApiResult<PageOutput<DataUploadTaskVO>> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<PageOutput<DataUploadTaskVO>>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null) {
                        PageOutput<DataUploadTaskVO> pageResult = result.getData();
                        taskList.clear();
                        taskList.addAll(pageResult.getRecords());
                        
                        // 更新分页信息
                        taskTotalRecords = pageResult.getTotal();
                        taskTotalPages = pageResult.getPages();
                        updateTaskPaginationInfo();
                        
                        taskStatsLabel.setText("订单 " + currentSelectedOrderNo + " 共" + taskTotalRecords + "条任务数据");
                        System.out.println("加载生产任务成功：" + taskTotalRecords + "条");
                    } else {
                        taskList.clear();
                        taskStatsLabel.setText("加载失败");
                        showAlert(Alert.AlertType.ERROR, "错误", "加载任务数据失败：" + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    taskList.clear();
                    taskStatsLabel.setText("加载异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "加载任务数据异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    // ==================== 订单搜索事件 ====================
    
    @FXML
    private void onSearchOrders() {
        System.out.println("搜索订单");
        orderCurrentPageField.setText("1");
        loadProductionOrders();
    }
    
    @FXML
    private void onClearOrderSearch() {
        System.out.println("清空订单搜索");
        orderSearchField.clear();
        productSearchField.clear();
        orderCurrentPageField.setText("1");
        loadProductionOrders();
    }
    
    // ==================== 订单分页事件 ====================
    
    @FXML
    private void onOrderFirstPage() {
        orderCurrentPageField.setText("1");
        loadProductionOrders();
    }
    
    @FXML
    private void onOrderPreviousPage() {
        int currentPage = Integer.parseInt(orderCurrentPageField.getText());
        if (currentPage > 1) {
            orderCurrentPageField.setText(String.valueOf(currentPage - 1));
            loadProductionOrders();
        }
    }
    
    @FXML
    private void onOrderNextPage() {
        int currentPage = Integer.parseInt(orderCurrentPageField.getText());
        if (currentPage < orderTotalPages) {
            orderCurrentPageField.setText(String.valueOf(currentPage + 1));
            loadProductionOrders();
        }
    }
    
    @FXML
    private void onOrderLastPage() {
        if (orderTotalPages > 0) {
            orderCurrentPageField.setText(String.valueOf(orderTotalPages));
            loadProductionOrders();
        }
    }
    
    @FXML
    private void onOrderPageSizeChange() {
        orderCurrentPageField.setText("1");
        loadProductionOrders();
    }
    
    // ==================== 任务分页事件 ====================
    
    @FXML
    private void onTaskFirstPage() {
        taskCurrentPageField.setText("1");
        loadProductionTasks();
    }
    
    @FXML
    private void onTaskPreviousPage() {
        int currentPage = Integer.parseInt(taskCurrentPageField.getText());
        if (currentPage > 1) {
            taskCurrentPageField.setText(String.valueOf(currentPage - 1));
            loadProductionTasks();
        }
    }
    
    @FXML
    private void onTaskNextPage() {
        int currentPage = Integer.parseInt(taskCurrentPageField.getText());
        if (currentPage < taskTotalPages) {
            taskCurrentPageField.setText(String.valueOf(currentPage + 1));
            loadProductionTasks();
        }
    }
    
    @FXML
    private void onTaskLastPage() {
        if (taskTotalPages > 0) {
            taskCurrentPageField.setText(String.valueOf(taskTotalPages));
            loadProductionTasks();
        }
    }
    
    @FXML
    private void onTaskPageSizeChange() {
        taskCurrentPageField.setText("1");
        loadProductionTasks();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 更新订单分页信息显示
     */
    private void updateOrderPaginationInfo() {
        orderTotalCountLabel.setText("共 " + orderTotalRecords + " 条");
        orderPageInfoLabel.setText(orderCurrentPageField.getText() + " / " + (orderTotalPages > 0 ? orderTotalPages : 1));
        orderStatsLabel.setText("共" + orderTotalRecords + "条订单");
    }
    
    /**
     * 更新任务分页信息显示
     */
    private void updateTaskPaginationInfo() {
        taskTotalCountLabel.setText("共 " + taskTotalRecords + " 条");
        taskPageInfoLabel.setText(taskCurrentPageField.getText() + " / " + (taskTotalPages > 0 ? taskTotalPages : 1));
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
    
    /**
     * 在左侧订单列表中查找并选中指定的订单产品
     * @param orderNo 订单号
     * @param productNo 产品编号
     */
    private void selectOrderInTable(String orderNo, String productNo) {
        try {
            // 1. 首先在当前页面查找
            DataUploadOrderVO targetOrder = null;
            int targetIndex = -1;
            
            for (int i = 0; i < orderList.size(); i++) {
                DataUploadOrderVO order = orderList.get(i);
                if (order.getOrderNo().equals(orderNo) && order.getProductNo().equals(productNo)) {
                    targetOrder = order;
                    targetIndex = i;
                    break;
                }
            }
            
            // 2. 如果在当前页找到，直接选中
            if (targetOrder != null && targetIndex >= 0) {
                orderTableView.getSelectionModel().select(targetIndex);
                orderTableView.scrollTo(targetIndex);
                System.out.println("[选中订单] 在当前页找到并选中: " + orderNo + " - " + productNo);
                return;
            }
            
            // 3. 如果在当前页未找到，需要重新搜索并加载对应页面
            System.out.println("[选中订单] 当前页未找到，开始搜索并跳转到对应页面...");
            
            // 清空搜索条件，设置订单号为搜索条件
            orderSearchField.setText(orderNo);
            productSearchField.clear();
            
            // 重新加载第一页订单数据
            orderCurrentPageField.setText("1");
            
            // 异步加载订单数据
            new Thread(() -> {
                try {
                    // 调用loadProductionOrders的逻辑
                    int pageNum = 1;
                    int pageSize = Integer.parseInt(orderPageSizeComboBox.getValue());
                    
                    // 构建请求URL（使用正确的路径）
                    StringBuilder urlBuilder = new StringBuilder("/api/dataupload/orders/page?pageNum=" + pageNum + "&pageSize=" + pageSize);
                    
                    // 添加 OrderNo 筛选条件
                    if (orderNo != null && !orderNo.isEmpty()) {
                        urlBuilder.append("&orderNo=").append(orderNo);
                    }
                    
                    String url = urlBuilder.toString();
                    System.out.println("[选中订单] 请求URL: " + url);
                    
                    String responseJson = HttpUtil.doGet(url);
                    ApiResult<PageOutput<DataUploadOrderVO>> result = HttpUtil.parseJson(responseJson,
                        new TypeReference<ApiResult<PageOutput<DataUploadOrderVO>>>() {});
                    
                    if (result.getCode() == 200 && result.getData() != null) {
                        PageOutput<DataUploadOrderVO> pageData = result.getData();
                        
                        Platform.runLater(() -> {
                            orderList.clear();
                            if (pageData.getRecords() != null) {
                                orderList.addAll(pageData.getRecords());
                            }
                            
                            orderTotalRecords = pageData.getTotal();
                            orderTotalPages = pageData.getPages();
                            
                            orderTotalCountLabel.setText("共 " + orderTotalRecords + " 条");
                            orderPageInfoLabel.setText(pageData.getCurrent() + " / " + orderTotalPages);
                            orderStatsLabel.setText("共" + orderList.size() + "条订单");
                            
                            // 在新加载的数据中查找并选中（需要同时匹配 OrderNo 和 ProductNo）
                            for (int i = 0; i < orderList.size(); i++) {
                                DataUploadOrderVO order = orderList.get(i);
                                if (order.getOrderNo().equals(orderNo) && order.getProductNo().equals(productNo)) {
                                    int finalIndex = i;
                                    Platform.runLater(() -> {
                                        orderTableView.getSelectionModel().select(finalIndex);
                                        orderTableView.scrollTo(finalIndex);
                                        System.out.println("[选中订单] 搜索后找到并选中: OrderNo=" + orderNo + ", ProductNo=" + productNo);
                                    });
                                    break;
                                }
                            }
                        });
                    } else {
                        System.err.println("[选中订单] 查询订单失败: " + (result != null ? result.getMessage() : "result is null"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("[选中订单] 搜索订单异常: " + e.getMessage());
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[选中订单] 异常: " + e.getMessage());
        }
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
     * 数据上传操作
     */
    private void onDataUpload(DataUploadOrderVO order) {
        System.out.println("点击数据上传：订单编号=" + order.getOrderNo());
        // TODO: 实现数据上传功能
        showAlert(Alert.AlertType.INFORMATION, "提示", "数据上传功能待实现\n订单编号: " + order.getOrderNo());
    }
    
    // ==================== 码搜索功能 ====================
    
    /**
     * 搜索码（支持BigSerialNumber和SmallSerialNumber）
     * 调用后端码查询接口，查询同一ProductNO下的所有码关联关系
     */
    @FXML
    private void onSearchCode() {
        String code = codeSearchField.getText().trim();
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入要搜索的码");
            return;
        }
        
        System.out.println("搜索码: " + code);
        statusLabel.setText("正在搜索...");
        
        // 保存当前搜索的码
        currentSearchCode = code;
        
        // 调用后端码查询接口
        new Thread(() -> {
            try {
                String responseJson = HttpUtil.doGet("/api/code/query/" + code);
                ApiResult<java.util.List<com.miduo.cloud.entity.dto.code.CodeQueryVO>> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<java.util.List<com.miduo.cloud.entity.dto.code.CodeQueryVO>>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                        // 获取第一条记录的ProductNO、OrderNo和VirtualSerialNumber
                        com.miduo.cloud.entity.dto.code.CodeQueryVO firstRecord = result.getData().get(0);
                        searchedProductNo = firstRecord.getProductNo();
                        searchedVirtualSerialNumber = firstRecord.getVirtualSerialNumber();
                        String orderNo = firstRecord.getOrderNo();
                        
                        System.out.println("搜索成功 - ProductNO: " + searchedProductNo + ", OrderNo: " + orderNo + ", VirtualSerialNumber: " + searchedVirtualSerialNumber);
                        
                        // 设置当前选中的订单号和产品号
                        currentSelectedOrderNo = orderNo;
                        currentSelectedProductNo = searchedProductNo;
                        
                        // 在左侧订单列表中查找并选中该订单产品
                        selectOrderInTable(orderNo, searchedProductNo);
                        
                        // 重新加载任务数据（会根据VirtualSerialNumber筛选，只显示相同VirtualSerialNumber的数据）
                        taskCurrentPageField.setText("1");
                        loadProductionTasks();
                        
                        // 刷新订单表格以高亮显示相同ProductNO的行
                        orderTableView.refresh();
                        
                        statusLabel.setText("找到码：" + code + " (产品编号: " + searchedProductNo + ")");
                    } else {
                        statusLabel.setText("未找到码: " + code);
                        showAlert(Alert.AlertType.INFORMATION, "搜索结果", "未找到该码：" + code);
                        currentSearchCode = null;
                        searchedProductNo = null;
                        searchedVirtualSerialNumber = null;
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("搜索失败");
                    showAlert(Alert.AlertType.ERROR, "错误", "搜索码异常：" + e.getMessage());
                    currentSearchCode = null;
                    searchedProductNo = null;
                    searchedVirtualSerialNumber = null;
                });
            }
        }).start();
    }
    
    /**
     * 清空码搜索
     */
    @FXML
    private void onClearCodeSearch() {
        codeSearchField.clear();
        currentSearchCode = null;
        searchedProductNo = null;
        searchedVirtualSerialNumber = null;
        
        // 重新加载任务数据（不带VirtualSerialNumber筛选）
        if (currentSelectedOrderNo != null) {
            taskCurrentPageField.setText("1");
            loadProductionTasks();
        }
        
        // 刷新表格以移除高亮
        taskTableView.refresh();
        orderTableView.refresh();
        
        statusLabel.setText("准备就绪");
        System.out.println("清空码搜索");
    }
    
    /**
     * 打开码替换弹窗
     */
    @FXML
    private void onCodeReplace() {
        try {
            // 获取当前窗口作为 owner
            Stage ownerStage = null;
            if (statusLabel != null && statusLabel.getScene() != null) {
                ownerStage = (Stage) statusLabel.getScene().getWindow();
            }
            
            // 加载码替换弹窗FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CodeReplaceDialog.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            CodeReplaceDialogController controller = loader.getController();
            
            // 创建新的Stage作为弹窗
            Stage dialogStage = new Stage();
            dialogStage.setTitle("码替换");
            
            // 如果有 owner，使用 WINDOW_MODAL，否则使用 APPLICATION_MODAL
            if (ownerStage != null) {
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(ownerStage);
            } else {
                dialogStage.initModality(Modality.APPLICATION_MODAL);
            }
            
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            com.miduo.cloud.frontend.util.StageIconUtil.setStageIcon(dialogStage);
            
            // 将Stage传递给控制器
            controller.setDialogStage(dialogStage);
            
            // 显示弹窗
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开码替换窗口：" + e.getMessage());
        }
    }
    
    /**
     * 打开码删除确认弹窗
     */
    @FXML
    private void onCodeDelete() {
        try {
            // 加载码删除弹窗FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CodeDeleteDialog.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            CodeDeleteDialogController controller = loader.getController();
            
            // 创建新的Stage作为弹窗
            Stage dialogStage = new Stage();
            dialogStage.setTitle("码删除");
            dialogStage.initModality(Modality.APPLICATION_MODAL); // 设置为模态窗口
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            com.miduo.cloud.frontend.util.StageIconUtil.setStageIcon(dialogStage);
            
            // 显示弹窗并等待用户操作
            dialogStage.showAndWait();
            
            // 如果用户确认删除
            if (controller.isConfirmed()) {
                String boxCode = controller.getBoxCode();
                executeCodeDelete(boxCode);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开码删除窗口：" + e.getMessage());
        }
    }
    
    /**
     * 执行码删除操作
     */
    private void executeCodeDelete(String boxCode) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("正在删除码..."));
                
                // 调用后端删除接口
                String responseJson = HttpUtil.doDelete("/api/code/delete-by-box-code/" + boxCode);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson, 
                    new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData()) {
                        statusLabel.setText("码删除成功");
                        showAlert(Alert.AlertType.INFORMATION, "成功", "码删除成功！");
                        
                        // 刷新当前任务数据（如果有选中的订单）
                        if (currentSelectedOrderNo != null && currentSelectedProductNo != null) {
                            loadProductionTasks();
                        }
                        
                        // 刷新订单列表（更新完成数量）
                        loadProductionOrders();
                        
                    } else {
                        statusLabel.setText("码删除失败");
                        showAlert(Alert.AlertType.ERROR, "失败", 
                            result.getMessage() != null ? result.getMessage() : "码删除失败！");
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("码删除异常");
                    showAlert(Alert.AlertType.ERROR, "异常", "码删除异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 为订单表格的文本列添加Tooltip支持
     */
    private void addTooltipToOrderColumn(TableColumn<DataUploadOrderVO, String> column) {
        column.setCellFactory(col -> new TableCell<DataUploadOrderVO, String>() {
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
     * 为订单表格的数字列添加Tooltip支持
     */
    private void addTooltipToOrderNumberColumn(TableColumn<DataUploadOrderVO, Integer> column) {
        column.setCellFactory(col -> new TableCell<DataUploadOrderVO, Integer>() {
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
     * 为任务表格的文本列添加Tooltip支持
     */
    private void addTooltipToTaskColumn(TableColumn<DataUploadTaskVO, String> column) {
        column.setCellFactory(col -> new TableCell<DataUploadTaskVO, String>() {
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
     * 清理资源
     */
    public void cleanup() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
