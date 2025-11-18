package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.dto.code.CodeAssociationRow;
import com.miduo.cloud.entity.dto.code.CodeQueryVO;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 查询码控制器
 * 功能：码查询和关联信息展示
 * 
 * API路径已验证: GET /api/code/query/{code}
 */
public class CodeQueryController {
    
    // 操作按钮栏控件
    @FXML private TextField codeInputField;
    @FXML private Label queryStatusLabel;
    
    // 左侧：码关联信息
    @FXML private Label associationStatsLabel;
    @FXML private TableView<CodeAssociationRow> codeAssociationTableView;
    @FXML private TableColumn<CodeAssociationRow, String> layer1Column;
    @FXML private TableColumn<CodeAssociationRow, LocalDateTime> addTimeColumn;
    @FXML private TableColumn<CodeAssociationRow, String> layer2Column;
    @FXML private TableColumn<CodeAssociationRow, String> layer3Column;
    @FXML private TableColumn<CodeAssociationRow, String> layer4Column;
    
    // 右侧：具体信息区
    @FXML private VBox detailInfoContainer;
    
    // 底部状态栏
    @FXML private Label currentTimeLabel;
    
    private Timer timer;
    
    // 当前查询的码值（用于高亮显示）
    private String currentQueryCode;
    
    // 静态变量：保存当前打开的查询码页面控制器实例（用于接收扫码枪数据）
    private static CodeQueryController currentInstance = null;
    
    /**
     * 获取当前打开的查询码页面控制器实例
     * @return 如果查询码页面已打开，返回控制器实例；否则返回null
     */
    public static CodeQueryController getCurrentInstance() {
        return currentInstance;
    }
    
    /**
     * 处理扫码枪数据（由MainController调用）
     * @param barcodeData 扫码枪扫描的条码数据
     */
    public static void handleBarcodeData(String barcodeData) {
        if (currentInstance != null) {
            Platform.runLater(() -> {
                // 将扫码数据填入输入框
                currentInstance.codeInputField.setText(barcodeData);
                // 自动触发查询
                currentInstance.onQuery();
            });
        }
    }
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("查询码界面初始化...");
        
        // 注册当前实例（用于接收扫码枪数据）
        currentInstance = this;
        
        // 初始化表格
        initializeTableColumns();
        
        // 禁止列重新排序
        codeAssociationTableView.getColumns().forEach(column -> column.setReorderable(false));
        
        // 禁止列排序
        codeAssociationTableView.getColumns().forEach(column -> column.setSortable(false));
        
        // 设置搜索框的回车事件
        codeInputField.setOnAction(event -> onQuery());
        
        // 启动实时时钟
        startRealtimeClock();
        
        // 延迟设置窗口关闭事件监听器（因为此时场景可能还未设置到窗口）
        Platform.runLater(() -> {
            if (codeInputField.getScene() != null && codeInputField.getScene().getWindow() != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) codeInputField.getScene().getWindow();
                stage.setOnCloseRequest(event -> {
                    if (currentInstance == this) {
                        currentInstance = null;
                        System.out.println("[查询码页面] 窗口关闭，清除控制器引用");
                    }
                });
            }
        });
    }
    
    /**
     * 初始化表格列
     */
    private void initializeTableColumns() {
        // 绑定表格列到数据模型
        layer1Column.setCellValueFactory(new PropertyValueFactory<>("layer1"));
        
        // 第一层（箱码）列自定义渲染 - 查询的箱码显示红色
        layer1Column.setCellFactory(column -> new TableCell<CodeAssociationRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // 判断是否是查询的箱码
                    if (currentQueryCode != null && currentQueryCode.equals(item)) {
                        // 查询的箱码用红色字体显示
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        layer2Column.setCellValueFactory(new PropertyValueFactory<>("layer2"));
        layer3Column.setCellValueFactory(new PropertyValueFactory<>("layer3"));
        layer4Column.setCellValueFactory(new PropertyValueFactory<>("layer4"));
        
        // 采集时间列需要格式化显示
        addTimeColumn.setCellValueFactory(new PropertyValueFactory<>("addTime"));
        addTimeColumn.setCellFactory(column -> new TableCell<CodeAssociationRow, LocalDateTime>() {
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
        codeAssociationTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 添加行选择监听器 - 点击时显示详细信息
        codeAssociationTableView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displayDetailInfo(newValue.getFullData());
                }
            }
        );
        
        // 去除行选中样式
        codeAssociationTableView.setRowFactory(tv -> {
            TableRow<CodeAssociationRow> row = new TableRow<>();
            
            // 点击行时不改变样式（去除选中效果）
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle(""); // 保持无样式
                }
            });
            
            return row;
        });
        
        codeAssociationTableView.setPlaceholder(new Label("请输入码进行查询"));
        
        // 去除选中时的焦点样式
        codeAssociationTableView.setFocusTraversable(false);
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
    
    // ==================== 操作按钮栏事件 ====================
    
    /**
     * 查询码
     * API: GET /api/code/query/{code}
     */
    @FXML
    public void onQuery() {
        String code = codeInputField.getText().trim();
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入要查询的码值");
            return;
        }
        
        // 保存当前查询的码值
        currentQueryCode = code;
        
        System.out.println("查询码: " + code);
        queryStatusLabel.setText("正在查询...");
        
        // 调用后端查询接口
        queryCodeFromServer(code);
    }
    
    /**
     * 调用后端接口查询码信息
     * API: GET /api/code/query/{code}
     */
    private void queryCodeFromServer(String code) {
        new Thread(() -> {
            try {
                // 调用重构后的接口：GET /api/code/query/{code}
                String responseJson = HttpUtil.doGet("/api/code/query/" + code);
                ApiResult<java.util.List<CodeQueryVO>> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<java.util.List<CodeQueryVO>>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                        queryStatusLabel.setText("查询成功");
                        // 显示查询结果
                        displayQueryResult(result.getData());
                        
                        // 记录查询成功日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_QUERY)
                            .operateType(OperateTypeEnum.QUERY)
                            .target(code, "")
                            .content("查询码: " + code + ", 结果数=" + result.getData().size())
                            .saveAsync();
                    } else {
                        queryStatusLabel.setText("查询失败");
                        showAlert(Alert.AlertType.ERROR, "查询失败", result.getMessage());
                        // 清空显示
                        codeInputField.clear();
                        queryStatusLabel.setText("请输入码进行查询");
                        associationStatsLabel.setText("未查询");
                        currentQueryCode = null;
                        codeAssociationTableView.getItems().clear();
                        detailInfoContainer.getChildren().clear();
                        detailInfoContainer.getChildren().add(new Label("请点击左侧记录查看详细信息"));
                        
                        // 记录查询失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_QUERY)
                            .operateType(OperateTypeEnum.QUERY)
                            .target(code, "")
                            .content("查询码失败: " + code)
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    queryStatusLabel.setText("查询异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "查询异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 显示查询结果
     */
    private void displayQueryResult(java.util.List<CodeQueryVO> dataList) {
        // 更新统计信息
        associationStatsLabel.setText("已查询到 " + dataList.size() + " 条码信息");
        
        // 将数据添加到表格
        codeAssociationTableView.getItems().clear();
        int targetIndex = -1; // 要选中的行索引
        int index = 0;
        for (CodeQueryVO data : dataList) {
            CodeAssociationRow row = new CodeAssociationRow(data);
            codeAssociationTableView.getItems().add(row);
            
            // 如果查询的是箱码，找到对应的行
            if (currentQueryCode != null && currentQueryCode.equals(data.getSmallSerialNumber())) {
                targetIndex = index;
            }
            index++;
        }
        
        // 选中目标行：如果查询的是箱码且找到了对应行，则选中该行；否则选中第一行
        // 注意：选中行后，行选择监听器会自动调用 displayDetailInfo 显示详细信息
        if (targetIndex >= 0) {
            codeAssociationTableView.getSelectionModel().select(targetIndex);
        } else {
            // 如果没找到对应的箱码行，默认选中第一行
            codeAssociationTableView.getSelectionModel().selectFirst();
        }
        
        System.out.println("显示查询结果：找到 " + dataList.size() + " 条记录");
    }
    
    /**
     * 显示具体信息
     */
    private void displayDetailInfo(CodeQueryVO data) {
        detailInfoContainer.getChildren().clear();
        
        GridPane gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(15));
        gridPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1px;");
        
        int row = 0;
        
        // 标题
        Label titleLabel = new Label("码详细信息");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        GridPane.setColumnSpan(titleLabel, 2);
        gridPane.add(titleLabel, 0, row++);
        
        // 分隔线
        Separator separator1 = new Separator();
        GridPane.setColumnSpan(separator1, 2);
        gridPane.add(separator1, 0, row++);
        
        // 码信息（只显示：箱码、托盘码、中标、是否关联）
        addInfoRow(gridPane, row++, "箱码:", data.getSmallSerialNumber());
        addInfoRow(gridPane, row++, "托盘码:", data.getBigSerialNumber());
        addInfoRow(gridPane, row++, "中标:", data.getMediumSerialNumber());
        addInfoRow(gridPane, row++, "是否关联:", data.getIsVirtual() != null ? (data.getIsVirtual() == 1 ? "已关联" : "未关联") : "-");
        
        // 分隔线
        Separator separator2 = new Separator();
        GridPane.setColumnSpan(separator2, 2);
        gridPane.add(separator2, 0, row++);
        
        // 产品信息
        Label productTitle = new Label("产品信息");
        productTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        GridPane.setColumnSpan(productTitle, 2);
        gridPane.add(productTitle, 0, row++);
        
        addInfoRow(gridPane, row++, "产品编号:", data.getProductNo());
        addInfoRow(gridPane, row++, "产品名称:", data.getProductName());
        addInfoRow(gridPane, row++, "产品规格:", data.getProductFormatName());
        addInfoRow(gridPane, row++, "订单编号:", data.getOrderNo());
        addInfoRow(gridPane, row++, "生产批次:", data.getBatchNo());
        addInfoRow(gridPane, row++, "类型:", data.getType() != null ? (data.getType() == 1 ? "有箱码" : "无箱码") : "");
        addInfoRow(gridPane, row++, "采集规格:", data.getRatio() != null ? ("1:" + data.getRatio().toString()) : "");
        
        detailInfoContainer.getChildren().add(gridPane);
    }
    
    /**
     * 添加信息行
     */
    private void addInfoRow(GridPane gridPane, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        
        Label valueNode = new Label(value != null && !value.isEmpty() ? value : "-");
        valueNode.setStyle("-fx-text-fill: #333;");
        valueNode.setWrapText(true);
        
        gridPane.add(labelNode, 0, row);
        gridPane.add(valueNode, 1, row);
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

