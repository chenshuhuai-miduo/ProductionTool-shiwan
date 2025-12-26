package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.task.TaskVO;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.service.DeviceConnectionManager;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.miduo.cloud.frontend.util.SpringContextUtil;
import com.miduo.cloud.frontend.util.StageIconUtil;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.entity.dto.code.*;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主界面控制器
 * 功能：任务执行控制界面
 */
public class MainController {
    
    // 左侧面板 - 当前生产信息
    @FXML private TextField productionOrderField;
    @FXML private Label productCodeLabel;
    @FXML private Label productNameLabel;
    @FXML private Label productSpecLabel;
    @FXML private TextField collectionSpecField;
    @FXML private Label plannedQuantityLabel;
    @FXML private TextField productionBatchField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ListView<VBox> uploadDataList;
    
    // 中间面板 - 数据接收和日志
    @FXML private ListView<javafx.scene.text.TextFlow> dataReceiveList;
    @FXML private TextArea operationLogArea;
    @FXML private TextArea alarmInfoArea;
    
    // 右侧面板 - 统计和控制
    @FXML private Label currentBoxCountLabel;
    @FXML private Label boxesPerPalletLabel;
    @FXML private Label producedPalletCountLabel;
    @FXML private Label completionRateLabel;
    @FXML private Button enableTaskButton;
    @FXML private Button completeOrderButton;
    @FXML private Button codeRejectionButton;
    @FXML private Button addCodeButton;
    @FXML private Button clearCodeButton;
    @FXML private Button specifyBoxCountButton;
    @FXML private Button forcePalletButton;
    @FXML private Button deleteEmptyCodesButton;
    @FXML private Button closeAlarmButton;
    
    // 工具栏按钮图标容器
    @FXML private VBox taskManagementIconBox;
    @FXML private VBox dataUploadIconBox;
    @FXML private VBox codeQueryIconBox;
    @FXML private VBox codeReplaceIconBox;
    @FXML private VBox clearScreenIconBox;
    
    // 状态栏
    @FXML private Label currentTimeLabel;
    @FXML private Label deviceStatusLabel;
    @FXML private Label workStatusLabel;
    
    // 当前选中的任务
    private TaskVO currentTask = null;
    
    // 统计数据
    private int currentBoxCount = 0;        // 当前箱数
    private int boxesPerPallet = 0;         // 每垛箱数
    private int producedPalletCount = 0;    // 已生产垛数
    
    // 托盘码关联设备的触发箱码（用于托盘码关联）
    private String triggerBoxCode = null;
    
    // 读码剔除状态（true-开启，false-关闭）
    private boolean isRejectModeEnabled = false;
    
    // 报警器状态（true-开启，false-关闭）
    private boolean isAlarmEnabled = true;
    
    // 报警器是否正在报警（true-报警中，false-正常）
    private volatile boolean isAlarming = false;
    
    // 实时上传区已显示的托盘码列表（用于累加显示，LinkedHashMap保持插入顺序）
    // Key: 托盘码, Value: PalletUploadVO (托盘码 + 箱数 + 上传状态)
    private java.util.LinkedHashMap<String, PalletUploadVO> displayedPallets = new java.util.LinkedHashMap<>();
    
    /**
     * 托盘上传信息内部类
     */
    private static class PalletUploadVO {
        private String palletCode;
        private int boxCount;
        private String uploadStatus;
        
        public PalletUploadVO(String palletCode, int boxCount, String uploadStatus) {
            this.palletCode = palletCode;
            this.boxCount = boxCount;
            this.uploadStatus = uploadStatus;
        }
        
        public String getPalletCode() {
            return palletCode;
        }
        
        public void setPalletCode(String palletCode) {
            this.palletCode = palletCode;
        }
        
        public int getBoxCount() {
            return boxCount;
        }
        
        public void setBoxCount(int boxCount) {
            this.boxCount = boxCount;
        }
        
        public String getUploadStatus() {
            return uploadStatus;
        }
        
        public void setUploadStatus(String uploadStatus) {
            this.uploadStatus = uploadStatus;
        }
    }
    
    // 操作日志服务（注意：原始代码中使用的是OperateLogService，这里需要通过HttpUtil调用后端API）
    // private OperateLogService operateLogService;  // 暂时注释，后续通过HTTP调用
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("主界面初始化...");
        
        // 初始化类型下拉框
        typeComboBox.setItems(FXCollections.observableArrayList("有箱码", "无箱码"));
        typeComboBox.getSelectionModel().selectFirst();
        
        // 限制采集规格输入框只能输入大于0的正整数
        collectionSpecField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                // 不是数字，恢复旧值
                collectionSpecField.setText(oldValue);
            } else if (!newValue.isEmpty()) {
                try {
                    int value = Integer.parseInt(newValue);
                    if (value <= 0) {
                        // 小于等于0，恢复旧值
                        collectionSpecField.setText(oldValue);
                    }
                } catch (NumberFormatException e) {
                    // 数字过大，恢复旧值
                    collectionSpecField.setText(oldValue);
                }
            }
        });
        
        // 初始化数据接收列表
        dataReceiveList.setItems(FXCollections.observableArrayList());
        
        // 初始化实时上传数据区（延迟执行，确保FXML完全加载）
        Platform.runLater(() -> {
            if (uploadDataList != null) {
                uploadDataList.setItems(FXCollections.observableArrayList());
                // 设置单元格工厂，直接显示VBox内容
                uploadDataList.setCellFactory(listView -> new javafx.scene.control.ListCell<VBox>() {
                    @Override
                    protected void updateItem(VBox item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            setGraphic(item);
                        }
                    }
                });
            }
        });
        
        // 初始化时间显示
        updateTime();
        
        // 初始化统计显示
        updateStatisticsDisplay();
        
        // 初始化按钮状态
        updateButtonStates();
        
        // 添加初始日志
        appendTextToTop(operationLogArea,getCurrentTime() + " 系统初始化完成 - 成功\n");
        
        // 设置设备连接管理器的数据接收处理器（新版本，携带设备顺序）
        DeviceConnectionManager.getInstance().setDataReceiveHandlerWithOrder(this::handleDeviceDataWithOrder);
        
        // 设置设备连接管理器的操作日志处理器（用于设备重试日志）
        DeviceConnectionManager.getInstance().setOperationLogHandler(message -> {
            appendTextToTop(operationLogArea,message + "\n");
        });
        
        // 设置设备状态变化处理器（设备连接/断开时自动更新状态显示）
        DeviceConnectionManager.getInstance().setDeviceStatusChangeHandler(this::updateDeviceStatus);
        
        // 初始化时加载并启动所有启用的设备连接
        loadAndStartDeviceConnections();
        
        // 加载工具栏图标
        loadToolbarIcons();
    }
    
    /**
     * 加载工具栏图标
     */
    private void loadToolbarIcons() {
        // 延迟加载，确保FXML完全加载后再设置图标
        Platform.runLater(() -> {
            try {
                System.out.println("[图标加载] 开始加载工具栏图标...");
                // 加载SVG图标（使用SVGPath节点）
                loadSvgIconToVBox(taskManagementIconBox, "/icons/任务管理.svg");
                loadSvgIconToVBox(dataUploadIconBox, "/icons/数据上传.svg");
                loadSvgIconToVBox(codeQueryIconBox, "/icons/查询码.svg");
                loadSvgIconToVBox(codeReplaceIconBox, "/icons/码替换.svg");
                loadSvgIconToVBox(clearScreenIconBox, "/icons/清屏.svg");
                System.out.println("[图标加载] 工具栏图标加载完成");
            } catch (Exception e) {
                System.err.println("加载工具栏图标失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 加载SVG图标到VBox（使用SVGPath节点）
     */
    private void loadSvgIconToVBox(VBox iconBox, String svgPath) {
        try {
            if (iconBox == null) {
                System.err.println("[图标加载] VBox为null: " + svgPath);
                return;
            }
            
            System.out.println("[图标加载] 加载图标: " + svgPath);
            
            // 读取SVG文件内容
            String svgContent;
            try (java.io.InputStream svgStream = getClass().getResourceAsStream(svgPath)) {
                if (svgStream == null) {
                    System.err.println("[图标加载] 无法找到SVG文件: " + svgPath);
                    return;
                }
                
                // 解析SVG文件，提取path数据
                try (java.util.Scanner scanner = new java.util.Scanner(svgStream, "UTF-8")) {
                    svgContent = scanner.useDelimiter("\\A").next();
                }
            }
            
            // 使用正则表达式提取path的d属性和transform属性
            java.util.regex.Pattern pathPattern = java.util.regex.Pattern.compile(
                "<path[^>]*d=\"([^\"]+)\"[^>]*(?:transform=\"([^\"]+)\")?[^>]*>", 
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher matcher = pathPattern.matcher(svgContent);
            
            if (matcher.find()) {
                String pathData = matcher.group(1);
                String transform = matcher.group(2); // 可能为null
                
                System.out.println("[图标加载] 提取path数据成功: " + svgPath + ", transform=" + transform);
                
                // 创建SVGPath节点
                SVGPath svgPathNode = new SVGPath();
                svgPathNode.setContent(pathData);
                svgPathNode.setFill(Color.web("#323232"));
                svgPathNode.setStrokeWidth(0);
                
                // 如果有transform属性，解析并应用
                Group iconGroup = new Group(svgPathNode);
                if (transform != null && transform.trim().startsWith("translate")) {
                    // 解析translate(x, y)
                    java.util.regex.Pattern translatePattern = java.util.regex.Pattern.compile(
                        "translate\\(([-\\d.]+)\\s+([-\\d.]+)\\)"
                    );
                    java.util.regex.Matcher translateMatcher = translatePattern.matcher(transform);
                    if (translateMatcher.find()) {
                        double translateX = Double.parseDouble(translateMatcher.group(1));
                        double translateY = Double.parseDouble(translateMatcher.group(2));
                        // SVG的transform是相对于原始坐标的，需要应用相同的transform
                        iconGroup.setTranslateX(translateX);
                        iconGroup.setTranslateY(translateY);
                        System.out.println("[图标加载] 应用transform: translate(" + translateX + ", " + translateY + ")");
                    }
                }
                
                // 使用StackPane包装以控制大小（24x24像素），并添加裁剪
                StackPane iconPane = new StackPane();
                iconPane.setMaxWidth(24);
                iconPane.setMaxHeight(24);
                iconPane.setMinWidth(24);
                iconPane.setMinHeight(24);
                // 添加矩形裁剪，确保图标在24x24范围内
                Rectangle clip = new Rectangle(0, 0, 24, 24);
                iconPane.setClip(clip);
                iconPane.getChildren().add(iconGroup);
                
                // 替换VBox中的第一个子节点（ImageView）为SVGPath StackPane
                if (iconBox.getChildren().size() > 0) {
                    // 找到ImageView并替换
                    boolean replaced = false;
                    for (int i = 0; i < iconBox.getChildren().size(); i++) {
                        javafx.scene.Node node = iconBox.getChildren().get(i);
                        if (node instanceof javafx.scene.image.ImageView) {
                            iconBox.getChildren().set(i, iconPane);
                            replaced = true;
                            System.out.println("[图标加载] 成功替换ImageView: " + svgPath);
                            break;
                        }
                    }
                    if (!replaced) {
                        System.err.println("[图标加载] 未找到ImageView节点，VBox子节点数: " + iconBox.getChildren().size() + ", " + svgPath);
                        // 如果没找到ImageView，直接添加到VBox的第一个位置
                        if (iconBox.getChildren().size() > 0) {
                            iconBox.getChildren().set(0, iconPane);
                            System.out.println("[图标加载] 直接替换第一个节点: " + svgPath);
                        } else {
                            iconBox.getChildren().add(0, iconPane);
                            System.out.println("[图标加载] 添加到VBox: " + svgPath);
                        }
                    }
                } else {
                    System.err.println("[图标加载] VBox为空，直接添加图标: " + svgPath);
                    iconBox.getChildren().add(iconPane);
                }
            } else {
                System.err.println("[图标加载] 无法从SVG文件中提取path数据: " + svgPath);
            }
        } catch (Exception e) {
            System.err.println("[图标加载] 加载SVG图标失败: " + svgPath + ", " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 添加带颜色的文本到数据接收区
     * @param text 要显示的文本
     * @param statusColor 状态颜色（"green", "red", "orange", "gray", null）
     */
    private void addColoredTextToDataReceive(String text, String statusColor) {
        javafx.scene.text.TextFlow textFlow = new javafx.scene.text.TextFlow();
        
        // 分割文本：时间+设备+码 和 状态
        String[] parts = text.split(" (合格|重码|错码|无码|系统生成|成功|失败|不合格)");
        
        if (parts.length > 0) {
            // 第一部分：时间+设备+码（黑色）
            javafx.scene.text.Text normalText = new javafx.scene.text.Text(parts[0]);
            normalText.setStyle("-fx-fill: black;");
            textFlow.getChildren().add(normalText);
            
            // 第二部分：状态文字（带颜色）
            if (text.contains("合格")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 合格");
                statusText.setStyle("-fx-fill: #28a745; -fx-font-weight: bold;"); // 绿色
                textFlow.getChildren().add(statusText);
            } else if (text.contains("系统生成")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 系统生成");
                statusText.setStyle("-fx-fill: #28a745; -fx-font-weight: bold;"); // 绿色
                textFlow.getChildren().add(statusText);
            } else if (text.contains("成功")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 成功");
                statusText.setStyle("-fx-fill: #28a745; -fx-font-weight: bold;"); // 绿色（与合格一致）
                textFlow.getChildren().add(statusText);
            } else if (text.contains("重码")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 重码");
                statusText.setStyle("-fx-fill: #dc3545; -fx-font-weight: bold;"); // 红色
                textFlow.getChildren().add(statusText);
            } else if (text.contains("失败")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 失败");
                statusText.setStyle("-fx-fill: #dc3545; -fx-font-weight: bold;"); // 红色（与重码一致）
                textFlow.getChildren().add(statusText);
            } else if (text.contains("不合格")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 不合格");
                statusText.setStyle("-fx-fill: #dc3545; -fx-font-weight: bold;"); // 红色（与重码一致）
                textFlow.getChildren().add(statusText);
            } else if (text.contains("错码")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 错码");
                statusText.setStyle("-fx-fill: #fd7e14; -fx-font-weight: bold;"); // 橙色
                textFlow.getChildren().add(statusText);
            } else if (text.contains("无码")) {
                javafx.scene.text.Text statusText = new javafx.scene.text.Text(" 无码");
                statusText.setStyle("-fx-fill: #6c757d; -fx-font-weight: bold;"); // 灰色
                textFlow.getChildren().add(statusText);
            }
        } else {
            // 如果分割失败，全部显示为黑色
            javafx.scene.text.Text normalText = new javafx.scene.text.Text(text);
            normalText.setStyle("-fx-fill: black;");
            textFlow.getChildren().add(normalText);
        }
        
        // 将最新数据添加到列表顶部（索引0）
        dataReceiveList.getItems().add(0, textFlow);
        
        // 限制数据接收区最多显示1000条数据，超过则删除最旧的数据（列表末尾）
        final int MAX_DATA_COUNT = 1000;
        if (dataReceiveList.getItems().size() > MAX_DATA_COUNT) {
            // 删除列表末尾最旧的数据
            dataReceiveList.getItems().remove(MAX_DATA_COUNT, dataReceiveList.getItems().size());
        }
        
        // 滚动到顶部显示最新数据
        dataReceiveList.scrollTo(0);
    }
    
    /**
     * 加载并启动所有启用的设备连接
     */
    private void loadAndStartDeviceConnections() {
        new Thread(() -> {
            try {
                appendTextToTop(operationLogArea,getCurrentTime() + " 正在加载设备配置...\n");
                
                // 调用接口获取所有设备（API路径已更新为重构后的）
                String responseJson = HttpUtil.doGet("/api/device/list");
                ApiResult<java.util.List<IoDeviceDTO>> result = 
                    HttpUtil.parseJson(responseJson,
                        new TypeReference<ApiResult<java.util.List<IoDeviceDTO>>>() {});
                
                if (result.getCode() == 200 && result.getData() != null) {
                    int connectedCount = 0;
                    for (IoDeviceDTO device : result.getData()) {
                        if (device.getEnabled()) {
                            try {
                                DeviceConnectionManager.getInstance().startConnection(device);
                                connectedCount++;
                                Platform.runLater(() -> {
                                    appendTextToTop(operationLogArea,getCurrentTime() + " 设备连接成功: " + 
                                        device.getDeviceName() + " (" + device.getConnectionType() + ")\n");
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> {
                                    appendTextToTop(operationLogArea,getCurrentTime() + " 设备连接失败: " + 
                                        device.getDeviceName() + " - " + e.getMessage() + "\n");
                                });
                            }
                        }
                    }
                    
                    final int finalCount = connectedCount;
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " 设备初始化完成，已连接" + 
                            finalCount + "个设备\n");
                        // 更新设备状态显示
                        updateDeviceStatus();
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " 设备初始化异常: " + e.getMessage() + "\n");
                    // 更新设备状态显示
                    updateDeviceStatus();
                });
            }
        }).start();
    }
    
    /**
     * 更新主界面的设备状态显示
     * 规则：
     * - 全部已连接：所有已启用的设备都已连接
     * - 部分未连接：部分已启用的设备未连接
     * - 全部未连接：所有已启用的设备都未连接，或没有启用的设备
     */
    private void updateDeviceStatus() {
        try {
            // 调用接口获取所有设备
            String responseJson = HttpUtil.doGet("/api/device/list");
            ApiResult<java.util.List<IoDeviceDTO>> result = 
                HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<java.util.List<IoDeviceDTO>>>() {});
            
            if (result.getCode() == 200 && result.getData() != null) {
                java.util.List<IoDeviceDTO> allDevices = result.getData();
                int enabledCount = 0; // 已启用的设备数量
                int connectedCount = 0; // 已连接的设备数量
                
                for (IoDeviceDTO device : allDevices) {
                    if (device.getEnabled()) {
                        enabledCount++;
                        if (DeviceConnectionManager.getInstance().isConnected(device.getId())) {
                            connectedCount++;
                        }
                    }
                }
                
                final int finalEnabledCount = enabledCount;
                final int finalConnectedCount = connectedCount;
                
                Platform.runLater(() -> {
                    String status;
                    if (finalEnabledCount == 0) {
                        status = "设备状态：全部未连接";
                    } else if (finalConnectedCount == 0) {
                        status = "设备状态：全部未连接";
                    } else if (finalConnectedCount == finalEnabledCount) {
                        status = "设备状态：全部已连接";
                    } else {
                        status = "设备状态：部分未连接";
                    }
                    deviceStatusLabel.setText(status);
                });
            }
        } catch (Exception e) {
            System.err.println("[设备状态] 更新设备状态失败: " + e.getMessage());
            Platform.runLater(() -> {
                deviceStatusLabel.setText("设备状态：状态未知");
            });
        }
    }
    
    /**
     * 处理设备接收到的数据（新版本，携带设备类别信息）
     * 
     * @param categoryCode 设备类别代码（1=码校验,2=箱码采集,3=托盘码关联,4=箱码关联,5=报警器,6=剔除设备）
     * @param data 接收到的数据
     */
    private void handleDeviceDataWithOrder(int categoryCode, String data) {
        Platform.runLater(() -> {
            // 箱码采集设备和码校验设备的数据不在此处显示，延迟到获取状态后显示
            if (categoryCode != 2 && categoryCode != 1) {
                // 其他设备：添加时间戳和设备标识到数据接收区（使用ListView）
                String deviceName = getCategoryName(categoryCode);
                String timestampedData = getCurrentTime() + " [" + deviceName + "] " + data;
                addColoredTextToDataReceive(timestampedData, null);
            }
        });
        
        // 异步处理设备数据，避免阻塞UI
        processDeviceDataByOrder(categoryCode, data);
    }
    
    /**
     * 根据类别代码获取设备名称
     * 优先从IO设备配置中获取设备名称，如果未配置则返回默认名称
     */
    private String getCategoryName(int categoryCode) {
        // 从设备连接管理器获取设备配置
        DeviceConnectionManager manager = DeviceConnectionManager.getInstance();
        IoDeviceDTO device = manager.getDeviceByCategory(categoryCode);
        
        // 如果找到设备配置且设备名称不为空，返回配置的设备名称
        if (device != null && device.getDeviceName() != null && !device.getDeviceName().isEmpty()) {
            return device.getDeviceName();
        }
        
        // 否则返回默认名称
        switch (categoryCode) {
            case 1: return "码校验设备";
            case 2: return "箱码采集设备";
            case 3: return "托盘码关联设备";
            case 4: return "箱码关联设备";
            case 5: return "报警器";
            case 6: return "剔除设备";
            case 7: return "扫码枪";
            default: return "未知设备(" + categoryCode + ")";
        }
    }
    
    /**
     * 根据设备类别代码处理数据
     * 
     * @param deviceOrder 设备顺序索引（1/2/3/4/7）
     * @param data 接收到的数据
     */
    private void processDeviceDataByOrder(int deviceOrder, String data) {
        new Thread(() -> {
            try {
                // 扫码枪（设备类别7）不需要检查订单和任务状态，直接处理
                if (deviceOrder == 7) {
                    // 检查data是否为空
                    if (data == null || data.trim().isEmpty()) {
                        // 扫码枪收到空数据时不显示在操作日志中
                        return;
                    }
                    // 扫码枪：不显示在数据接收区和操作日志中，仅自动填入查询码页面或码替换页面
                    handleBarcodeScannerData(data);
                    return;
                }
                
                // 其他设备需要检查订单和任务状态
                // 1. 检查是否选择了订单
                if (currentTask == null) {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " 警告：未选择生产订单，忽略设备数据\n");
                    });
                    return;
                }
                
                // 2. 检查任务是否已启用（状态为1表示生产中）
                Integer taskStatus = currentTask.getOrderStatus();
                if (taskStatus == null || taskStatus != 1) {
                    String statusText = getStatusText(taskStatus);
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " 警告：任务未启用（当前状态：" + statusText + "），忽略设备数据\n");
                    });
                    return;
                }
                
                // 检查data是否为空
                if (data == null || data.trim().isEmpty()) {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(deviceOrder) + "] 收到空数据\n");
                    });
                    return;
                }
                
                // 根据设备顺序调用相应的处理方法
                switch (deviceOrder) {
                    case 1:
                        // 码校验设备：码校验（串口通信）
                        handleFirstDeviceData(data);
                        break;
                    case 2:
                        // 箱码采集设备：箱码采集（网口通信）
                        handleSecondDeviceData(data);
                        break;
                    case 3:
                        // 托盘码关联设备：箱码触发（预留）
                        handleThirdDeviceData(data);
                        break;
                    case 4:
                        // 箱码关联设备：托盘码（网口通信）
                        handleFourthDeviceData(data);
                        break;
                    default:
                        Platform.runLater(() -> {
                            appendTextToTop(operationLogArea,getCurrentTime() + " 未知设备类别: " + deviceOrder + "\n");
                        });
                        break;
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(deviceOrder) + "] 处理异常: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }
    
    /**
     * 获取任务状态文本
     * 状态值说明：
     * 0: 待生产
     * 1: 生产中（已启用）
     * 2: 已完成
     * 3: 生产中（未启用但有采集数据，显示为"生产中"但需要点击启用任务）
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待生产";
            case 1: return "生产中";
            case 2: return "已完成";
            case 3: return "生产中"; // 未启用但有采集数据，显示为"生产中"但需要点击启用任务
            default: return "未知(" + status + ")";
        }
    }
    
    /**
     * 处理码校验设备数据（码校验）
     * 串口通信，判断码是否合格
     */
    private void handleFirstDeviceData(String code) {
        try {
            // 使用按钮状态判断是否启用读码剔除模式
            if (isRejectModeEnabled) {
                // 读码剔除模式
                handleFirstDeviceDataRejectMode(code);
            } else {
                // 普通校验模式
                handleFirstDeviceDataNormalMode(code);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(1) + "] 处理失败: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 码校验设备-普通校验模式
     */
    private void handleFirstDeviceDataNormalMode(String code) {
        try {
            // 码校验信息不再在操作日志区显示，只在数据接收区显示
            
            // 调用校验接口
            CodeValidateRequest request = new CodeValidateRequest(1, code);
            
            String responseJson = HttpUtil.doPost("/api/code/validate", request);
            ApiResult<String> result = HttpUtil.parseJson(responseJson,
                new TypeReference<ApiResult<String>>() {});
            
            if (result.getCode() == 200) {
                String validateResult = result.getData(); // 01 or 02
                
                // 在数据接收区显示校验结果
                Platform.runLater(() -> {
                    String statusText = "01".equals(validateResult) ? "合格" : "不合格";
                    String displayText = getCurrentTime() + " [" + getCategoryName(1) + "] " + code + " " + statusText;
                    addColoredTextToDataReceive(displayText, null);
                });
                
                // 向剔除设备（类别代码6）发送校验结果（01=放行，02=剔除）
                DeviceConnectionManager.getInstance().sendToDeviceByCategory(6, validateResult);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(1) + "-普通模式] 处理失败: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 码校验设备-读码剔除模式
     */
    private void handleFirstDeviceDataRejectMode(String code) {
        try {
            // 检查是否为"null"字符串（无码）
            boolean isNullString = "null".equals(code);
            
            // 获取当前订单号
            String orderNo = productionOrderField.getText();
            
            // 调用读码剔除校验接口
            CodeRejectRequest request = new CodeRejectRequest(1, code, orderNo);
            
            String responseJson = HttpUtil.doPost("/api/code/validate-reject", request);
            ApiResult<CodeRejectResult> result = 
                HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<CodeRejectResult>>() {});
            
            if (result.getCode() == 200) {
                CodeRejectResult rejectResult = result.getData();
                String validateResult = rejectResult.getResult(); // 01-放行 or 02-剔除
                String rejectReason = rejectResult.getRejectReason();
                String message = rejectResult.getMessage();
                
                Platform.runLater(() -> {
                    // 在数据接收区显示校验结果（与箱码采集格式一致）
                    String statusText = "";
                    String displayCode = isNullString ? "未读到码none" : code;
                    
                    if ("01".equals(validateResult)) {
                        // 合格，放行
                        statusText = "合格";
                    } else {
                        // 剔除，根据rejectReason确定状态
                        if ("NO_CODE".equals(rejectReason)) {
                            statusText = "无码";
                            displayCode = "未读到码none";
                        } else if ("DUPLICATE".equals(rejectReason)) {
                            statusText = "重码";
                        } else if ("INVALID_FORMAT".equals(rejectReason) || "ALL_LETTERS".equals(rejectReason)) {
                            statusText = "错码";
                        } else {
                            statusText = "不合格";
                        }
                    }
                    
                    String displayText = getCurrentTime() + " [" + getCategoryName(1) + "] " + displayCode + " " + statusText;
                    addColoredTextToDataReceive(displayText, null);
                    
                    if ("01".equals(validateResult)) {
                        // 记录放行日志到OperateLog表
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_VALIDATE)
                            .operateType(OperateTypeEnum.VALIDATE)
                            .target(code, orderNo)
                            .content("读码剔除-放行: " + (isNullString ? "null(无码)" : code) + " - " + message)
                            .afterData(rejectResult)
                            .saveAsync();
                    } else {
                        // 剔除，触发短报警（只有成功发送报警信号02时才显示报警信息）
                        // 触发短报警（错码、无码、重码），只有成功发送报警信号（02）时才显示报警信息
                        triggerShortAlarm(getCategoryName(1) + "-" + message);
                        
                        // 记录剔除日志到OperateLog表（重码、无码、错码等）
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_VALIDATE)
                            .operateType(OperateTypeEnum.VALIDATE)
                            .target(code, orderNo)
                            .content("读码剔除-剔除: " + (isNullString ? "null(无码)" : code) + " - " + message)
                            .failReason(rejectReason)
                            .afterData(rejectResult)
                            .saveAsync();
                    }
                });
                
                // 通过已连接的串口向剔除设备（类别代码6）发送剔除指令
                // 无码、重码发送02（剔除），其他情况发送01（放行）
                String rejectCommand = "02".equals(validateResult) ? "02" : "01";
                System.out.println("[读码剔除] 准备向剔除设备发送指令: " + rejectCommand + " (校验结果=" + validateResult + ")");
                
                boolean rejectSent = DeviceConnectionManager.getInstance().sendToDeviceByCategory(6, rejectCommand);
                System.out.println("[读码剔除] 向剔除设备发送指令: " + rejectCommand + ", 发送状态: " + rejectSent);
                
                // 如果校验不通过（02-剔除），也向报警器（类别代码5）发送信号（需检查报警器是否开启）
                boolean alarmSent = false;
                if ("02".equals(rejectCommand)) {
                    if (isAlarmEnabled) {
                        System.out.println("[读码剔除] 准备向报警器发送信号: " + rejectCommand);
                        alarmSent = DeviceConnectionManager.getInstance().sendToDeviceByCategory(5, rejectCommand);
                        System.out.println("[读码剔除] 向报警器发送信号: " + rejectCommand + ", 发送状态: " + alarmSent);
                    } else {
                        System.out.println("[读码剔除] 报警器已关闭，不发送信号");
                    }
                }
                
                // 码校验相关的剔除设备和报警器信息不再在操作日志区显示
                // final String finalRejectCommand = rejectCommand;
                // final boolean finalRejectSent = rejectSent;
                // final boolean finalAlarmSent = alarmSent;
                // final boolean finalAlarmEnabled = isAlarmEnabled;
                // Platform.runLater(() -> {
                //     if (finalRejectSent) {
                //         appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(6) + "] 已发送指令: " + finalRejectCommand + 
                //             ("02".equals(finalRejectCommand) ? " (剔除)" : " (放行)") + "\n");
                //     } else {
                //         appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(6) + "] 发送失败或未连接\n");
                //     }
                //     
                //     if ("02".equals(finalRejectCommand)) {
                //         if (!finalAlarmEnabled) {
                //             appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 已关闭，不发送信号\n");
                //         } else if (finalAlarmSent) {
                //             appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 已发送报警信号: " + finalRejectCommand + "\n");
                //         } else {
                //             appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 发送失败或未连接\n");
                //         }
                //     }
                // });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(1) + "-读码剔除] 处理失败: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 处理箱码采集设备数据（箱码采集）
     * 网口通信，采集箱码，存入数据库
     */
    private void handleSecondDeviceData(String boxCode) {
        try {
            // 从原始数据中提取箱码
            String extractedBoxCode = extractBoxCode(boxCode);
            
            // 获取当前模式（1=有箱码，2=无箱码）
            Integer type = typeComboBox.getSelectionModel().getSelectedIndex() == 0 ? 1 : 2;
            
            // 检查是否为"null"字符串（无码）
            boolean isNullString = "null".equals(extractedBoxCode);
            
            // 无箱码模式：只接受"null"值，其他值忽略
            if (type == 2 && !isNullString) {
                // 不再在操作日志中显示
                return; // 直接返回，不进行后续处理
            }
            
            // 如果是"null"字符串，转换为空字符串，让后端自动生成箱码
            final String finalBoxCode = isNullString ? "" : extractedBoxCode;
            final boolean finalIsNullString = isNullString;
            
            // 不再在操作日志中显示箱码采集信息（已在数据接收区显示）
            
            // 如果是"null"字符串且读码剔除模式开启，触发短报警
            if (finalIsNullString && isRejectModeEnabled) {
                triggerShortAlarm(getCategoryName(2) + "-无码");
            }
            
            // 构建采集请求
            CodeCollectRequest request = new CodeCollectRequest();
            request.setBoxCode(finalBoxCode);
            request.setOrderNo(productionOrderField.getText());
            request.setProductNo(productCodeLabel.getText());
            request.setBatchNo(productionBatchField.getText());
            request.setType(typeComboBox.getSelectionModel().getSelectedIndex() == 0 ? 1 : 2);
            request.setBoxesPerPallet(readBoxesPerPallet());
            
            // 调用采集接口
            String responseJson = HttpUtil.doPost("/api/code/collect", request);
            ApiResult<CodeCollectResult> result = 
                HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<CodeCollectResult>>() {});
            
            if (result.getCode() == 200) {
                CodeCollectResult collectResult = result.getData();
                
                Platform.runLater(() -> {
                    String status = collectResult.getStatus();
                    String message = collectResult.getMessage();
                    
                    // 在数据接收区显示箱码和状态（不显示"状态:"文字和符号，只显示状态文字）
                    String statusText = "";
                    String displayBoxCode = finalBoxCode;
                    
                    if ("SUCCESS".equals(status)) {
                        statusText = "合格"; // 绿色
                    } else if ("NO_CODE".equals(status)) {
                        statusText = "无码"; // 灰色
                        // 显示未读到码
                        displayBoxCode = "未读到码none";
                    } else if ("DUPLICATE".equals(status)) {
                        statusText = "重码"; // 红色
                    } else if ("INVALID".equals(status)) {
                        statusText = "错码"; // 橙色
                    }
                    
                    String displayText = getCurrentTime() + " [" + getCategoryName(2) + "] " + 
                        displayBoxCode + " " + statusText;
                    addColoredTextToDataReceive(displayText, status);
                    
                    // 不在操作日志中显示箱码采集成功消息（已在数据接收区显示）
                    // appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(2) + "] " + message + "\n");
                    
                    // 更新统计显示
                    if ("SUCCESS".equals(status) || "NO_CODE".equals(status)) {
                        // 同步更新前端变量和Label显示
                        currentBoxCount = collectResult.getCurrentCount();
                        currentBoxCountLabel.setText(String.valueOf(currentBoxCount));
                        
                        boxesPerPallet = collectResult.getTotalCount();
                        boxesPerPalletLabel.setText(String.valueOf(boxesPerPallet));
                        
                        // 不再在操作日志中显示满垛信息
                        // if (collectResult.getIsPalletFull()) {
                        //     appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(2) + "] 满垛！TagNo=" + 
                        //         collectResult.getTagNo() + "\n");
                        // }
                        
                        // 记录箱码采集成功日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_COLLECT)
                            .operateType(OperateTypeEnum.COLLECT)
                            .target(displayBoxCode, request.getOrderNo())
                            .content("箱码采集: " + ("NO_CODE".equals(status) ? "无码(" + displayBoxCode + ")" : displayBoxCode) + " - " + statusText)
                            .afterData(collectResult)
                            .saveAsync();
                    } else if ("DUPLICATE".equals(status)) {
                        // 记录重码日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_COLLECT)
                            .operateType(OperateTypeEnum.COLLECT)
                            .target(finalBoxCode, request.getOrderNo())
                            .content("箱码采集失败: 重码 - " + finalBoxCode)
                            .failReason("重复码")
                            .saveAsync();
                    } else if ("INVALID".equals(status)) {
                        // 记录错码日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_COLLECT)
                            .operateType(OperateTypeEnum.COLLECT)
                            .target(finalBoxCode, request.getOrderNo())
                            .content("箱码采集失败: 错码 - " + finalBoxCode)
                            .failReason("错码")
                            .saveAsync();
                    }
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // 不再在操作日志中显示处理失败信息
            // Platform.runLater(() -> {
            //     appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(2) + "] 处理失败: " + e.getMessage() + "\n");
            // });
        }
    }
    
    /**
     * 处理托盘码关联设备数据（托盘码触发）
     * 有箱码模式：保存触发托盘码，等待箱码关联设备的箱码
     * 无箱码模式：直接生成虚拟箱码并完成码关系更新（写入虚拟垛标等）
     */
    private void handleThirdDeviceData(String triggerPalletCode) {
        try {
            // 检查是否为"null"字符串（未读到托盘码）
            boolean isNullString = "null".equals(triggerPalletCode);
            
            Platform.runLater(() -> {
                // 如果是null（字符串），在数据接收区显示"未读到码none"
                if (isNullString) {
                    String displayText = getCurrentTime() + " [" + getCategoryName(3) + "] 未读到码none 无码";
                    addColoredTextToDataReceive(displayText, "NO_CODE");
                }
            });
            
            // 如果是"null"字符串且读码剔除模式开启，触发长报警（只有成功发送报警信号03时才显示报警信息）
            if (isNullString && isRejectModeEnabled) {
                // 只有成功发送报警信号（03）时才显示报警信息
                triggerLongAlarm(getCategoryName(3) + "-未读到托盘码");
                return; // 不保存触发码
            }
            
            // 判断当前是有箱码还是无箱码模式
            Integer type = typeComboBox.getSelectionModel().getSelectedIndex() == 0 ? 1 : 2;
            
            if (type == 1) {
                // 有箱码模式：保存触发托盘码，等待箱码关联设备的箱码
                this.triggerBoxCode = triggerPalletCode;
                
                // 记录托盘码接收日志（不在操作日志区显示）
                OperateLogBuilder.create()
                    .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                    .operateType(OperateTypeEnum.COLLECT)
                    .target(triggerPalletCode, productionOrderField.getText())
                    .content("接收触发托盘码(有箱码模式): " + triggerPalletCode)
                    .saveAsync();
            } else {
                // 无箱码模式：直接生成虚拟箱码并完成码关系更新，无需等待箱码关联设备
                // 记录托盘码接收日志（不在操作日志区显示）
                OperateLogBuilder.create()
                    .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                    .operateType(OperateTypeEnum.COLLECT)
                    .target(triggerPalletCode, productionOrderField.getText())
                    .content("接收触发托盘码(无箱码模式): " + triggerPalletCode)
                    .saveAsync();
                
                // 直接调用无箱码处理逻辑
                handleNoBoxModeAssociation(triggerPalletCode);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // 不在操作日志区显示处理失败信息
        }
    }
    
    /**
     * 处理箱码关联设备数据（箱码）
     * 有箱码模式：使用托盘码关联设备的触发托盘码关联箱码
     * 无箱码模式：已由托盘码关联设备直接完成关联，此处忽略
     */
    private void handleFourthDeviceData(String boxCode) {
        try {
            // 从原始数据中提取箱码
            String extractedBoxCode = extractBoxCode(boxCode);
            final String finalExtractedBoxCode = extractedBoxCode;
            final String finalOriginalBoxCode = boxCode;
            
            // 检查是否为"null"字符串（无码）
            boolean isNullString = "null".equals(finalExtractedBoxCode);
            
            // 判断当前是有箱码还是无箱码模式
            Integer type = typeComboBox.getSelectionModel().getSelectedIndex() == 0 ? 1 : 2;
            
            if (type == 1) {
                // 有箱码模式：需要托盘码关联设备的触发托盘码
                Platform.runLater(() -> {
                    // 如果是null（字符串），在数据接收区显示"未读到码none"
                    if (isNullString) {
                        String displayText = getCurrentTime() + " [" + getCategoryName(4) + "] 未读到码none 无码";
                        addColoredTextToDataReceive(displayText, "NO_CODE");
                    }
                });
                handleFourthDeviceForBoxedMode(finalExtractedBoxCode);
            } else {
                // 无箱码模式：托盘码关联已在托盘码关联设备完成，箱码关联设备数据忽略
                Platform.runLater(() -> {
                    String logMsg = getCurrentTime() + " [" + getCategoryName(4) + "-无箱码] 接收数据: " + finalExtractedBoxCode;
                    // 如果提取的箱码与原始数据不同，记录原始数据
                    if (!finalExtractedBoxCode.equals(finalOriginalBoxCode)) {
                        logMsg += " (原始: " + finalOriginalBoxCode + ")";
                    }
                    appendTextToTop(operationLogArea, logMsg + " (无箱码模式下已由托盘码关联设备完成，此数据忽略)\n");
                    
                    // 如果是null（字符串），在数据接收区显示"未读到码none"
                    if (isNullString) {
                        String displayText = getCurrentTime() + " [" + getCategoryName(4) + "] 未读到码none 无码";
                        addColoredTextToDataReceive(displayText, "NO_CODE");
                    }
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(4) + "] 处理失败: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 有箱码模式：处理箱码关联设备数据（箱码）
     * 使用托盘码关联设备的触发托盘码关联当前箱码
     */
    private void handleFourthDeviceForBoxedMode(String boxCode) {
        try {
            // 检查是否有托盘码关联设备的触发托盘码
            if (triggerBoxCode == null || triggerBoxCode.isEmpty()) {
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(4) + "-有箱码] 错误：未接收到托盘码关联设备的触发托盘码\n");
                });
                return;
            }
            
            // 获取每垛箱数（Qty）
            if (boxesPerPallet <= 0) {
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(4) + "-有箱码] 错误：无法获取每垛箱数\n");
                });
                return;
            }
            
            // 保存托盘码为final变量，避免在异步记录日志时被清除
            final String finalPalletCode = triggerBoxCode;
            
            // 构建托盘码关联请求（传递触发托盘码、箱码和Qty）
            PalletAssociateRequest request = new PalletAssociateRequest();
            request.setPalletCode(triggerBoxCode); // 使用托盘码关联设备的触发托盘码
            request.setOrderNo(productionOrderField.getText());
            request.setProductNo(productCodeLabel.getText()); // 设置产品编号
            request.setTriggerBoxCode(boxCode); // 箱码关联设备的箱码作为触发箱码
            request.setQty(boxesPerPallet); // 设置每垛箱数
            
            // 调用托盘码关联接口
            String responseJson = HttpUtil.doPost("/api/code/associate-pallet", request);
            ApiResult<PalletAssociateResult> result = 
                HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<PalletAssociateResult>>() {});
            
            if (result.getCode() == 200) {
                PalletAssociateResult associateResult = result.getData();
                
                Platform.runLater(() -> {
                    if (associateResult.getSuccess()) {
                        // 在数据接收区显示成功信息
                        String displayText = getCurrentTime() + " [" + getCategoryName(4) + "-有箱码] 箱码=" + boxCode + " 托盘码=" + finalPalletCode + " 成功";
                        addColoredTextToDataReceive(displayText, null);
                        
                        // 在操作日志区显示成功信息（单行格式）
                        String palletCode = associateResult.getPalletCode() != null ? associateResult.getPalletCode() : finalPalletCode;
                        appendTextToTop(operationLogArea,getCurrentTime() + " [箱码关联-有箱码]箱码=" + boxCode + " 托盘码=" + palletCode + " 成功\n");
                        
                        // 清除触发托盘码
                        triggerBoxCode = null; // 清除触发箱码
                        
                        // 前端实时统计：已生产垛数+1，当前箱数重置为0
                        producedPalletCount++;
                        currentBoxCount = 0;
                        updateStatisticsDisplay();
                        
                        // 将托盘码添加到实时上传数据区（箱数为每垛箱数）
                        addPalletToUploadArea(associateResult.getPalletCode(), associateResult.getUpdatedCount());
                        
                        // 记录箱码关联成功日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_ASSOCIATE_BOX)
                            .operateType(OperateTypeEnum.ASSOCIATE)
                            .target(boxCode, request.getOrderNo())
                            .content("箱码关联成功: 箱码=" + boxCode + ", 托盘码=" + finalPalletCode)
                            .afterData(associateResult)
                            .saveAsync();
                    } else {
                        // 在数据接收区显示失败信息
                        String displayText = getCurrentTime() + " [" + getCategoryName(4) + "-有箱码] 箱码=" + boxCode + " 托盘码=" + finalPalletCode + " 失败";
                        addColoredTextToDataReceive(displayText, null);
                        
                        appendTextToTop(operationLogArea,getCurrentTime() + " [托盘码关联] " + 
                            associateResult.getMessage() + "\n");
                        
                        // 校验不通过，触发短报警（只有成功发送报警信号02时才显示报警信息）
                        if (isRejectModeEnabled) {
                            triggerShortAlarm(getCategoryName(4) + "-码关联失败");
                        }
                        
                        // 记录箱码关联失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_ASSOCIATE_BOX)
                            .operateType(OperateTypeEnum.ASSOCIATE)
                            .target(boxCode, request.getOrderNo())
                            .content("箱码关联失败: 箱码=" + boxCode + ", 托盘码=" + finalPalletCode)
                            .failReason(associateResult.getMessage())
                            .saveAsync();
                    }
                });
            } else {
                // API调用失败（如箱数不匹配等业务校验失败）
                Platform.runLater(() -> {
                    String displayText = getCurrentTime() + " [" + getCategoryName(4) + "-有箱码] 箱码=" + boxCode + " 托盘码=" + finalPalletCode + " 失败";
                    addColoredTextToDataReceive(displayText, null);
                    
                    appendTextToTop(operationLogArea,getCurrentTime() + " [托盘码关联] " + result.getMessage() + "\n");
                    
                    // 校验不通过，触发短报警（只有成功发送报警信号02时才显示报警信息）
                    if (isRejectModeEnabled) {
                        triggerShortAlarm(getCategoryName(4) + "-码关联失败");
                    }
                    
                    // 记录箱码关联失败日志
                    OperateLogBuilder.create()
                        .module(ModuleNameEnum.CODE_ASSOCIATE_BOX)
                        .operateType(OperateTypeEnum.ASSOCIATE)
                        .target(boxCode, request.getOrderNo())
                        .content("箱码关联失败: 箱码=" + boxCode + ", 托盘码=" + finalPalletCode)
                        .failReason(result.getMessage())
                        .saveAsync();
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(4) + "-有箱码] 异常: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 无箱码模式：处理托盘码关联
     * 更新之前箱码采集设备采集的那一垛无码数据，写入托盘码和虚拟垛标
     */
    private void handleNoBoxModeAssociation(String palletCode) {
        new Thread(() -> {
            try {
                // 构建无箱码托盘码关联请求
                NoBoxCollectRequest request = new NoBoxCollectRequest();
                request.setPalletCode(palletCode);
                request.setOrderNo(productionOrderField.getText());
                request.setProductNo(productCodeLabel.getText());
                request.setBatchNo(productionBatchField.getText());
                request.setBoxesPerPallet(readBoxesPerPallet());
                
                // 调用无箱码托盘码关联接口
                String responseJson = HttpUtil.doPost("/api/code/collect-no-box", request);
                ApiResult<NoBoxCollectResult> result = 
                    HttpUtil.parseJson(responseJson,
                        new TypeReference<ApiResult<NoBoxCollectResult>>() {});
                
                if (result.getCode() == 200) {
                    NoBoxCollectResult collectResult = result.getData();
                    
                    Platform.runLater(() -> {
                        if (collectResult.getSuccess()) {
                            // 在数据接收区显示成功信息
                            String displayText = getCurrentTime() + " [托盘码关联-无箱码] 托盘码: " + palletCode + " 成功";
                            addColoredTextToDataReceive(displayText, null);
                            
                            // 在操作日志区显示成功信息（单行格式）
                            String resultPalletCode = collectResult.getPalletCode() != null ? collectResult.getPalletCode() : palletCode;
                            appendTextToTop(operationLogArea,getCurrentTime() + " [托盘码关联-无箱码]托盘码:" + resultPalletCode + " 成功\n");
                            
                            // 前端实时统计：已生产垛数+1，当前箱数重置为0
                            producedPalletCount++;
                            currentBoxCount = 0;
                            updateStatisticsDisplay();
                            
                            // 将托盘码添加到实时上传数据区（箱数为生成的记录数）
                            addPalletToUploadArea(collectResult.getPalletCode(), collectResult.getGeneratedCount());
                            
                            // 记录无箱码托盘码关联成功日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                                .operateType(OperateTypeEnum.ASSOCIATE)
                                .target(palletCode, request.getOrderNo())
                                .content("无箱码托盘码关联成功: 托盘码=" + palletCode + ", 更新数量=" + collectResult.getGeneratedCount())
                                .afterData(collectResult)
                                .saveAsync();
                        } else {
                            // 在数据接收区显示失败信息
                            String displayText = getCurrentTime() + " [托盘码关联-无箱码] 托盘码: " + palletCode + " 失败";
                            addColoredTextToDataReceive(displayText, null);
                            
                            appendTextToTop(operationLogArea,getCurrentTime() + " [托盘码关联] " + collectResult.getMessage() + "\n");
                            
                            // 校验不通过，触发短报警（只有成功发送报警信号02时才显示报警信息）
                            if (isRejectModeEnabled) {
                                triggerShortAlarm("托盘码关联-无箱码关联失败");
                            }
                            
                            // 记录托盘码关联失败日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                                .operateType(OperateTypeEnum.ASSOCIATE)
                                .target(palletCode, request.getOrderNo())
                                .content("无箱码托盘码关联失败: 托盘码=" + palletCode)
                                .failReason(collectResult.getMessage())
                                .saveAsync();
                        }
                    });
                } else {
                    // API调用失败（如箱数不匹配等业务校验失败）
                    Platform.runLater(() -> {
                        // 在数据接收区显示失败信息
                        String displayText = getCurrentTime() + " [托盘码关联-无箱码] 托盘码: " + palletCode + " 失败";
                        addColoredTextToDataReceive(displayText, null);
                        
                        appendTextToTop(operationLogArea,getCurrentTime() + " [托盘码关联] " + result.getMessage() + "\n");
                        
                        // 校验不通过，触发短报警（只有成功发送报警信号02时才显示报警信息）
                        if (isRejectModeEnabled) {
                            triggerShortAlarm("托盘码关联-无箱码关联失败");
                        }
                        
                        // 记录托盘码关联失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                            .operateType(OperateTypeEnum.ASSOCIATE)
                            .target(palletCode, request.getOrderNo())
                            .content("无箱码托盘码关联失败: 托盘码=" + palletCode)
                            .failReason(result.getMessage())
                            .saveAsync();
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [托盘码关联-无箱码] 异常: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "无箱码托盘码关联失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 更新时间显示
     */
    private void updateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        currentTimeLabel.setText(LocalDateTime.now().format(formatter));
    }
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * 将文本添加到操作日志区域的顶部（最新数据在最上面）
     */
    private void appendTextToTop(TextArea textArea, String text) {
        if (textArea != null && text != null) {
            // 限制操作日志区最多显示500条，超过则删除最旧的数据（从末尾删除）
            final int MAX_LOG_LINES = 500;
            
            try {
                // 先获取当前文本内容
                String currentText = textArea.getText();
                
                // 在顶部插入新文本
                String newText = text + currentText;
                
                // 计算新文本的行数
                int newLineCount = newText.isEmpty() ? 0 : 1;
                for (int i = 0; i < newText.length(); i++) {
                    if (newText.charAt(i) == '\n') {
                        newLineCount++;
                    }
                }
                
                // 如果超过限制，找到第MAX_LOG_LINES个换行符的位置并截断
                if (newLineCount > MAX_LOG_LINES) {
                    int newlineCount = 0;
                    int cutPosition = newText.length();
                    for (int i = 0; i < newText.length(); i++) {
                        if (newText.charAt(i) == '\n') {
                            newlineCount++;
                            if (newlineCount == MAX_LOG_LINES) {
                                cutPosition = i + 1; // 保留第MAX_LOG_LINES行的换行符
                                break;
                            }
                        }
                    }
                    newText = newText.substring(0, Math.min(cutPosition, newText.length()));
                }
                
                // 一次性设置文本，避免多次操作导致的竞态条件
                textArea.setText(newText);
                
            } catch (IndexOutOfBoundsException e) {
                // 处理索引越界异常（可能是多线程竞态导致）
                System.err.println("[操作日志] 索引越界异常，使用降级方案: " + e.getMessage());
                try {
                    // 降级方案：直接清空并添加新文本
                    textArea.clear();
                    textArea.appendText(text);
                } catch (Exception ex) {
                    System.err.println("[操作日志] 降级方案也失败: " + ex.getMessage());
                }
            } catch (Exception e) {
                // 如果出现其他异常，使用简单的追加方式作为降级方案
                System.err.println("[操作日志] 添加文本异常: " + e.getMessage());
                try {
                    if (textArea.getLength() == 0) {
                        textArea.appendText(text);
                    } else {
                        textArea.insertText(0, text);
                    }
                } catch (Exception ex) {
                    // 如果所有方式都失败，只记录错误
                    System.err.println("[操作日志] 无法添加文本: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * 将文本添加到报警信息区域的顶部（最新数据在最上面）
     */
    private void appendTextToTopAlarm(TextArea textArea, String text) {
        if (textArea != null && text != null) {
            // 如果已有内容，在顶部插入新文本
            if (textArea.getLength() > 0) {
                textArea.insertText(0, text);
            } else {
                // 如果为空，直接追加
                textArea.appendText(text);
            }
        }
    }
    
    /**
     * 从原始数据中提取箱码
     * 按照以下规则提取（优先级从高到低）：
     * 1. 同时出现 / 和 - ，截取最后一个 - 号后面的内容
     * 2. 出现 &c= ，截取最后一个 = 号后面的内容
     * 3. 出现?t=，截取最后一个 = 号后面的内容（使用客户：华山论剑）
     * 4. 出现 ?c= ，截取最后一个 = 号后面的内容（使用客户：宁波同丰）
     * 5. 出现 / 号，截取最后一个 / 号后面的内容
     * 6. 出现 [ 号，截取第一个 [ 号前面的内容
     * 
     * @param rawData 原始数据
     * @return 提取的箱码，如果无法提取则返回原始数据
     */
    private String extractBoxCode(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return rawData;
        }
        
        String data = rawData.trim();
        
        // 规则1：同时出现 / 和 - ，截取最后一个 - 号后面的内容
        if (data.contains("/") && data.contains("-")) {
            int lastDashIndex = data.lastIndexOf("-");
            if (lastDashIndex >= 0 && lastDashIndex < data.length() - 1) {
                return data.substring(lastDashIndex + 1).trim();
            }
        }
        
        // 规则2：出现 &c= ，截取最后一个 = 号后面的内容
        if (data.contains("&c=")) {
            int lastEqualsIndex = data.lastIndexOf("=");
            if (lastEqualsIndex >= 0 && lastEqualsIndex < data.length() - 1) {
                String extracted = data.substring(lastEqualsIndex + 1).trim();
                // 如果提取的内容后面还有参数，需要去除（例如：&c=ABC&other=xxx 应该返回 ABC）
                int andIndex = extracted.indexOf("&");
                if (andIndex > 0) {
                    return extracted.substring(0, andIndex).trim();
                }
                return extracted;
            }
        }
        
        // 规则3：出现?t=，截取最后一个 = 号后面的内容（使用客户：华山论剑）
        if (data.contains("?t=")) {
            int lastEqualsIndex = data.lastIndexOf("=");
            if (lastEqualsIndex >= 0 && lastEqualsIndex < data.length() - 1) {
                String extracted = data.substring(lastEqualsIndex + 1).trim();
                // 如果提取的内容后面还有参数，需要去除（例如：?t=ABC&other=xxx 应该返回 ABC）
                int andIndex = extracted.indexOf("&");
                if (andIndex > 0) {
                    return extracted.substring(0, andIndex).trim();
                }
                return extracted;
            }
        }
        
        // 规则4：出现 ?c= ，截取最后一个 = 号后面的内容（使用客户：宁波同丰）
        if (data.contains("?c=")) {
            int lastEqualsIndex = data.lastIndexOf("=");
            if (lastEqualsIndex >= 0 && lastEqualsIndex < data.length() - 1) {
                String extracted = data.substring(lastEqualsIndex + 1).trim();
                // 如果提取的内容后面还有参数，需要去除
                int andIndex = extracted.indexOf("&");
                if (andIndex > 0) {
                    return extracted.substring(0, andIndex).trim();
                }
                return extracted;
            }
        }
        
        // 规则5：出现 / 号，截取最后一个 / 号后面的内容
        if (data.contains("/")) {
            int lastSlashIndex = data.lastIndexOf("/");
            if (lastSlashIndex >= 0 && lastSlashIndex < data.length() - 1) {
                return data.substring(lastSlashIndex + 1).trim();
            }
        }
        
        // 规则6：出现 [ 号，截取第一个 [ 号前面的内容
        if (data.contains("[")) {
            int firstBracketIndex = data.indexOf("[");
            if (firstBracketIndex > 0) {
                return data.substring(0, firstBracketIndex).trim();
            }
        }
        
        // 如果都不匹配，返回原始数据
        return data;
    }
    
    // ==================== 统计功能 ====================
    
    /**
     * 读取每垛箱数
     */
    private int readBoxesPerPallet() {
        String spec = collectionSpecField.getText().trim();
        if (spec.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(spec);
        } catch (NumberFormatException e) {
            appendTextToTop(operationLogArea,getCurrentTime() + " 采集规格格式错误：" + spec + "\n");
            return 0;
        }
    }
    
    /**
     * 更新统计显示
     */
    private void updateStatisticsDisplay() {
        updateStatisticsDisplay(true);
    }
    
    /**
     * 更新统计显示（前端实时计数，不查数据库）
     * @param updateCurrentBoxCount 是否更新当前箱数（托盘码关联和箱码关联时传false，避免覆盖正在采集的垛的箱数）
     */
    private void updateStatisticsDisplay(boolean updateCurrentBoxCount) {
        if (updateCurrentBoxCount) {
            currentBoxCountLabel.setText(String.valueOf(currentBoxCount));
        }
        boxesPerPalletLabel.setText(String.valueOf(boxesPerPallet));
        producedPalletCountLabel.setText(String.valueOf(producedPalletCount));
        
        // 计算完成度：从后端查询ProductionOrderDetail表的OrderCount/ProductCount
        if (currentTask != null) {
            String orderNo = currentTask.getOrderNo();
            String productNo = currentTask.getProductNo();
            
            if (orderNo != null && !orderNo.isEmpty() && productNo != null && !productNo.isEmpty()) {
                new Thread(() -> {
                    try {
                        // 调用后端接口获取完成度（查询ProductionOrderDetail表的OrderCount/ProductCount）
                        String completionRateUrl = "/api/task/completion-rate?orderNo=" + 
                                java.net.URLEncoder.encode(orderNo, "UTF-8") + 
                                "&productNo=" + java.net.URLEncoder.encode(productNo, "UTF-8");
                        String completionRateJson = HttpUtil.doGet(completionRateUrl);
                        ApiResult<Double> completionRateResult = HttpUtil.parseJson(completionRateJson, 
                            new TypeReference<ApiResult<Double>>() {});
                        
                        if (completionRateResult.getCode() == 200 && completionRateResult.getData() != null) {
                            final double completionRate = completionRateResult.getData();
                            
                            Platform.runLater(() -> {
                                // 使用ProductionOrderDetail表的OrderCount/ProductCount计算完成度
                                completionRateLabel.setText(String.format("%.1f%%", completionRate));
                            });
                        } else {
                            // 查询失败时显示0%
                            Platform.runLater(() -> {
                                completionRateLabel.setText("0%");
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("[完成度统计] 获取完成度失败：" + e.getMessage());
                        e.printStackTrace();
                        // 失败时显示0%
                        Platform.runLater(() -> {
                            completionRateLabel.setText("0%");
                        });
                    }
                }).start();
            } else {
                completionRateLabel.setText("0%");
            }
        } else {
            completionRateLabel.setText("0%");
        }
    }
    
    /**
     * 从数据库加载初始统计数据（仅在选择订单时调用一次）
     * 按OrderNo和ProductNo统计已生产垛数
     */
    private void loadInitialStatisticsFromDatabase() {
        if (currentTask == null) {
            System.out.println("[初始统计] 当前任务为空，跳过查询");
            return;
        }
        
        String orderNo = currentTask.getOrderNo();
        String productNo = currentTask.getProductNo();
        
        if (orderNo == null || orderNo.isEmpty()) {
            System.out.println("[初始统计] 订单号为空，跳过查询");
            return;
        }
        
        if (productNo == null || productNo.isEmpty()) {
            System.out.println("[初始统计] 产品编号为空，跳过查询");
            return;
        }
        
        System.out.println("[初始统计] 开始查询已生产垛数 - 订单号: " + orderNo + ", 产品编号: " + productNo);
        
        new Thread(() -> {
            try {
                // 获取已生产垛数（按OrderNo和ProductNo统计，使用优化接口）
                String palletCountUrl = "/api/code/produced-pallet-count-by-product-optimized?orderNo=" + 
                        java.net.URLEncoder.encode(orderNo, "UTF-8") + 
                        "&productNo=" + java.net.URLEncoder.encode(productNo, "UTF-8");
                String palletCountJson = HttpUtil.doGet(palletCountUrl);
                ApiResult<Integer> palletCountResult = HttpUtil.parseJson(palletCountJson, 
                    new TypeReference<ApiResult<Integer>>() {});
                
                if (palletCountResult.getCode() == 200 && palletCountResult.getData() != null) {
                    final int dbProducedPalletCount = palletCountResult.getData();
                    Platform.runLater(() -> {
                        producedPalletCount = dbProducedPalletCount;
                        System.out.println("[初始统计] 从数据库加载已生产垛数成功 - 订单号: " + orderNo + 
                                ", 产品编号: " + productNo + ", 已生产垛数: " + dbProducedPalletCount);
                        updateStatisticsDisplay();
                    });
                } else {
                    System.err.println("[初始统计] 查询已生产垛数失败 - 订单号: " + orderNo + 
                            ", 产品编号: " + productNo + ", 错误: " + 
                            (palletCountResult != null ? palletCountResult.getMessage() : "未知错误"));
                }
            } catch (Exception e) {
                System.err.println("[初始统计] 获取已生产垛数异常 - 订单号: " + orderNo + 
                        ", 产品编号: " + productNo + ", 异常: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * 重置统计数据
     */
    private void resetStatistics() {
        currentBoxCount = 0;
        producedPalletCount = 0;
        boxesPerPallet = readBoxesPerPallet();
        updateStatisticsDisplay();
        appendTextToTop(operationLogArea,getCurrentTime() + " 统计数据已重置\n");
    }
    
    /**
     * 从数据库加载当前箱数
     */
    private void loadCurrentBoxCountFromDatabase(String orderNo) {
        new Thread(() -> {
            try {
                if (orderNo == null || orderNo.isEmpty()) {
                    Platform.runLater(() -> {
                        currentBoxCount = 0;
                        currentBoxCountLabel.setText("0");
                    });
                    return;
                }
                
                // 调用后端接口获取当前垛信息（使用taskId）
                String responseJson = HttpUtil.doGet("/api/code/current-pallet-info/by-task/" + currentTask.getId());
                
                if (responseJson == null || responseJson.isEmpty()) {
                    Platform.runLater(() -> {
                        currentBoxCount = 0;
                        currentBoxCountLabel.setText("0");
                    });
                    return;
                }
                
                ApiResult<CurrentPalletInfoVO> result = 
                    HttpUtil.parseJson(responseJson,
                        new TypeReference<ApiResult<CurrentPalletInfoVO>>() {});
                
                if (result == null || result.getCode() == null) {
                    Platform.runLater(() -> {
                        currentBoxCount = 0;
                        currentBoxCountLabel.setText("0");
                    });
                    return;
                }
                
                if (result.getCode() == 200 && result.getData() != null) {
                    CurrentPalletInfoVO palletInfo = result.getData();
                    
                    Platform.runLater(() -> {
                        // 更新当前箱数
                        currentBoxCount = palletInfo.getCurrentCount() != null ? 
                            palletInfo.getCurrentCount() : 0;
                        currentBoxCountLabel.setText(String.valueOf(currentBoxCount));
                    });
                } else {
                    Platform.runLater(() -> {
                        currentBoxCount = 0;
                        currentBoxCountLabel.setText("0");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    currentBoxCount = 0;
                    currentBoxCountLabel.setText("0");
                });
            }
        }).start();
    }
    
    /**
     * 触发短报警
     */
    private void triggerShortAlarm(String reason) {
        if (!isRejectModeEnabled) return;
        
        // 检查报警器是否开启
        if (!isAlarmEnabled) {
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 已关闭，不发送短报警 - 原因: " + reason + "\n");
            });
            return;
        }
        
        try {
            // 向报警器设备（类别代码5）发送短报警指令
            System.out.println("[报警器] 准备发送短报警指令: 02");
            boolean alarmSent = DeviceConnectionManager.getInstance().sendToDeviceByCategory(5, "02");
            System.out.println("[报警器] 向报警器发送短报警指令: 02, 发送状态: " + alarmSent);
            
            final boolean finalAlarmSent = alarmSent;
            Platform.runLater(() -> {
                if (finalAlarmSent) {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 触发短报警 - 原因: " + reason + "\n");
                    appendTextToTopAlarm(alarmInfoArea,getCurrentTime() + " 短报警：" + reason + "（短鸣提示）\n");
                    
                    // 更新报警状态为报警中
                    isAlarming = true;
                } else {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 短报警发送失败或未连接 - 原因: " + reason + "\n");
                }
            });
        } catch (Exception e) {
            System.err.println("[报警器] 短报警发送异常: " + e.getMessage());
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 短报警发送异常: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 触发长报警
     */
    private void triggerLongAlarm(String reason) {
        if (!isRejectModeEnabled) return;
        
        // 检查报警器是否开启
        if (!isAlarmEnabled) {
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 已关闭，不发送长报警 - 原因: " + reason + "\n");
            });
            return;
        }
        
        try {
            // 向报警器设备（类别代码5）发送长报警指令
            System.out.println("[报警器] 准备发送长报警指令: 03");
            boolean alarmSent = DeviceConnectionManager.getInstance().sendToDeviceByCategory(5, "03");
            System.out.println("[报警器] 向报警器发送长报警指令: 03, 发送状态: " + alarmSent);
            
            final boolean finalAlarmSent = alarmSent;
            Platform.runLater(() -> {
                if (finalAlarmSent) {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 触发长报警 - 原因: " + reason + "\n");
                    appendTextToTopAlarm(alarmInfoArea,getCurrentTime() + " 长报警：" + reason + "\n");
                    
                    // 更新报警状态为报警中
                    isAlarming = true;
                } else {
                    appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 长报警发送失败或未连接 - 原因: " + reason + "\n");
                }
            });
        } catch (Exception e) {
            System.err.println("[报警器] 长报警发送异常: " + e.getMessage());
            Platform.runLater(() -> {
                appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 长报警发送异常: " + e.getMessage() + "\n");
            });
        }
    }
    
    /**
     * 显示提示对话框
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // 打开对话框方法
    private void openTaskManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaskManagement.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("任务管理");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1200, 700));
            StageIconUtil.setStageIcon(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开任务管理界面");
        }
    }
    
    private void openProductManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductManagement.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("产品管理");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1200, 700));
            StageIconUtil.setStageIcon(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开产品管理界面");
        }
    }
    
    private void openDataUpload() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DataUpload.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("数据上传");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1276, 700));
            StageIconUtil.setStageIcon(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开数据上传界面");
        }
    }
    
    private void openOperationLog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OperateLog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("操作日志");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1276, 700));
            StageIconUtil.setStageIcon(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开操作日志界面");
        }
    }
    
    private void openCodeQuery() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CodeQuery.fxml"));
            Parent root = loader.load();
            CodeQueryController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("查询码");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1200, 700));
            StageIconUtil.setStageIcon(stage);
            
            // 监听窗口关闭事件，确保清除控制器引用
            stage.setOnCloseRequest(event -> {
                CodeQueryController current = CodeQueryController.getCurrentInstance();
                if (current == controller) {
                    // 引用会在CodeQueryController的窗口关闭事件中清除
                    System.out.println("[主界面] 查询码页面关闭");
                }
            });
            
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开查询码界面");
        }
    }
    
    private void openCodeReplace() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CodeReplace.fxml"));
            Parent root = loader.load();
            CodeReplaceController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("码替换");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 600, 500));
            StageIconUtil.setStageIcon(stage);
            
            // 监听窗口关闭事件，确保清除控制器引用
            stage.setOnCloseRequest(event -> {
                CodeReplaceController current = CodeReplaceController.getCurrentInstance();
                if (current == controller) {
                    // 引用会在CodeReplaceController的窗口关闭事件中清除
                    System.out.println("[主界面] 码替换页面关闭");
                }
            });
            
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开码替换界面");
        }
    }
    
    private void openSystemConfig() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SystemConfig.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("系统配置");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1280, 768));
            StageIconUtil.setStageIcon(stage);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开系统配置界面");
        }
    }
    
    // FXML事件处理方法
    /**
     * 退出软件（菜单调用）
     */
    @FXML
    private void onExit() {
        handleExit();
    }
    
    /**
     * 处理退出逻辑（公共方法，可被窗口关闭事件调用）
     */
    public void handleExit() {
        System.out.println("退出软件");
        
        // 1. 检查生产任务是否已停止
        if (currentTask != null && currentTask.getOrderStatus() != null && currentTask.getOrderStatus() == 1) {
            // 任务正在运行，提示用户先停止任务
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("无法退出");
            warningAlert.setHeaderText("生产任务正在运行");
            warningAlert.setContentText("请先停止生产任务才能关闭软件！");
            warningAlert.showAndWait();
            appendTextToTop(operationLogArea,getCurrentTime() + " 退出失败：生产任务正在运行，请先停止任务\n");
            return;
        }
        
        // 2. 弹出确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("退出确认");
        confirmAlert.setHeaderText("确定要退出系统吗？");
        confirmAlert.setContentText("退出后将关闭所有设备连接。");
        
        // 获取确认结果
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // 记录退出日志
                    appendTextToTop(operationLogArea,getCurrentTime() + " 用户退出系统\n");
                    
                    // 关闭所有设备连接
                    DeviceConnectionManager.getInstance().stopAllConnections();
                    appendTextToTop(operationLogArea,getCurrentTime() + " 已关闭所有设备连接\n");
                    
                    // 延迟一小段时间让日志显示出来
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    // 退出JavaFX应用程序
                    Platform.exit();
                    
                    // 彻底退出JVM（确保所有线程都结束）
                    System.exit(0);
                    
                } catch (Exception e) {
                    System.err.println("退出时发生错误：" + e.getMessage());
                    e.printStackTrace();
                    // 即使发生错误也要退出
                    Platform.exit();
                    System.exit(0);
                }
            } else {
                System.out.println("用户取消退出");
            }
        });
    }
    @FXML private void onSystemConfig() { openSystemConfig(); }
    @FXML private void onOperationLog() { openOperationLog(); }
    @FXML private void onHelp() { System.out.println("打开操作帮助"); }
    @FXML private void onAbout() { showAlert(Alert.AlertType.INFORMATION, "关于系统", "产线采集关联软件 v1.0.0"); }
    @FXML private void onProductManagement() { openProductManagement(); }
    @FXML private void onTaskManagement() { openTaskManagement(); }
    @FXML private void onDataUpload() { openDataUpload(); }
    @FXML private void onCodeQuery() { openCodeQuery(); }
    @FXML private void onCodeReplace() { openCodeReplace(); }
    
    @FXML
    private void onClearScreen() {
        System.out.println("清屏");
        dataReceiveList.getItems().clear();
        operationLogArea.clear();
        alarmInfoArea.clear();
        appendTextToTop(operationLogArea,getCurrentTime() + " 屏幕已清空 - 成功\n");
    }
    
    @FXML
    private void onSelectOrder() {
        System.out.println("选择生产订单");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击选择生产订单按钮\n");
        
        // 检查当前任务是否处于启用状态
        if (currentTask != null && currentTask.getOrderStatus() != null && currentTask.getOrderStatus() == 1) {
            appendTextToTop(operationLogArea,getCurrentTime() + " 警告：任务正在启用中，无法切换订单\n");
            showAlert(Alert.AlertType.WARNING, "提示", "当前任务正在启用中，请先停用任务后再切换订单");
            return;
        }
        
        try {
            // 加载选择订单对话框FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/SelectOrderDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // 获取控制器
            SelectOrderDialogController dialogController = loader.getController();
            
            // 创建对话框Stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("选择生产订单");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(root));
            StageIconUtil.setStageIcon(dialogStage);
            dialogStage.showAndWait();
            
            // 检查是否确认选择
            if (dialogController.isConfirmed()) {
                TaskVO selectedTask = dialogController.getSelectedTask();
                if (selectedTask != null) {
                    // 填充主界面的任务信息
                    fillTaskInfo(selectedTask);
                    appendTextToTop(operationLogArea,getCurrentTime() + " 已选择订单：" + selectedTask.getOrderNo() + "\n");
                    
                    // 记录操作日志
                    OperateLogBuilder.create()
                        .module(ModuleNameEnum.ORDER_MANAGEMENT)
                        .operateType(OperateTypeEnum.QUERY)
                        .target(selectedTask.getId().toString(), selectedTask.getOrderNo())
                        .content("选择生产订单: " + selectedTask.getOrderNo() + ", 产品: " + selectedTask.getProductName())
                        .saveAsync();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            appendTextToTop(operationLogArea,getCurrentTime() + " 打开选择订单对话框失败：" + e.getMessage() + "\n");
            showAlert(Alert.AlertType.ERROR, "错误", "打开选择订单对话框失败：" + e.getMessage());
        }
    }
    
    /**
     * 填充任务信息到主界面
     */
    private void fillTaskInfo(TaskVO task) {
        // 保存当前任务
        this.currentTask = task;
        
        Platform.runLater(() -> {
            // 切换订单时清屏（清空数据接收区、操作日志、报警信息）
            dataReceiveList.getItems().clear();
            operationLogArea.clear();
            alarmInfoArea.clear();
            
            productionOrderField.setText(task.getOrderNo());
            productCodeLabel.setText(task.getProductNo() != null ? task.getProductNo() : "--");
            productNameLabel.setText(task.getProductName() != null ? task.getProductName() : "--");
            productSpecLabel.setText(task.getProductFormatName() != null ? task.getProductFormatName() : "--");
            collectionSpecField.setText(task.getRatio() != null ? task.getRatio().toString() : "");
            plannedQuantityLabel.setText(task.getProductCount() != null ? task.getProductCount().toString() : "--");
            
            // 处理生产批次：如果为空，自动填充"当前时间戳(精确到秒)+B"
            String batchNo = task.getSyBatchNo();
            if (batchNo == null || batchNo.trim().isEmpty()) {
                // 生成时间戳（精确到秒）+ B
                long timestamp = System.currentTimeMillis() / 1000; // 转换为秒级时间戳
                batchNo = timestamp + "B";
                appendTextToTop(operationLogArea,getCurrentTime() + " 自动生成生产批次：" + batchNo + "\n");
            }
            productionBatchField.setText(batchNo);
            typeComboBox.getSelectionModel().select(task.getType() == 1 ? 0 : 1);
            
            // 清空实时上传数据区（选择新订单时清空之前的托盘码列表）
            clearUploadDataArea();
            
            // 更新启用/停用按钮文本
            updateEnableButton();
            
            // 更新按钮状态
            updateButtonStates();
            
            // 根据任务状态更新输入框的启用/禁用状态
            updateInputFieldsState(task.getOrderStatus());
            
            // 更新工作状态显示
            updateWorkStatus(task.getOrderStatus());
            
            // 重置统计数据并从数据库加载初始值
            resetStatistics();
            loadCurrentBoxCountFromDatabase(task.getOrderNo());
            loadInitialStatisticsFromDatabase();
        });
    }
    
    @FXML
    private void onEnableTask() {
        System.out.println("启用/停用任务");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击启用/停用任务按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择一个生产订单");
            appendTextToTop(operationLogArea,getCurrentTime() + " 未选择订单，无法启用/停用\n");
            return;
        }
        
        Integer currentStatus = currentTask.getOrderStatus();
        Integer newStatus;
        String actionText;
        
        // 状态0（待生产）和状态3（未启用但有采集数据）都可以启用任务
        if (currentStatus == null || currentStatus == 0 || currentStatus == 3) {
            // 启用任务前检查采集规格是否已填写
            int boxesPerPalletValue = readBoxesPerPallet();
            if (boxesPerPalletValue <= 0) {
                showAlert(Alert.AlertType.WARNING, "提示", "请先输入采集规格（每垛箱数）才能启用任务");
                appendTextToTop(operationLogArea,getCurrentTime() + " 启用失败：采集规格未设置\n");
                return;
            }
            
            newStatus = 1;
            actionText = "启用";
        } else if (currentStatus == 1) {
            newStatus = 0;
            actionText = "停用";
        } else {
            showAlert(Alert.AlertType.WARNING, "提示", "已完成的任务无法启用/停用");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认" + actionText);
        confirmAlert.setHeaderText("确定要" + actionText + "该任务吗？");
        confirmAlert.setContentText("订单编号：" + currentTask.getOrderNo() + "\n产品名称：" + currentTask.getProductName());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateTaskStatus(currentTask.getId(), newStatus, actionText);
            }
        });
    }
    
    private void updateTaskStatus(Integer taskId, Integer newStatus, String actionText) {
        new Thread(() -> {
            try {
                appendTextToTop(operationLogArea,getCurrentTime() + " 正在" + actionText + "任务...\n");
                
                // 获取当前选择的采集类型（1=有箱码，2=无箱码）
                Integer currentType = typeComboBox.getSelectionModel().getSelectedIndex() == 0 ? 1 : 2;
                
                // 启用任务时需要传递type参数和设备连接状态
                String url = "/api/task/updateStatus/" + taskId + "/" + newStatus;
                if (newStatus == 1) {
                    url += "?type=" + currentType;
                    
                    // 获取所有设备的连接状态
                    Map<String, Boolean> deviceConnections = new HashMap<>();
                    try {
                        String deviceResponseJson = HttpUtil.doGet("/api/device/list");
                        ApiResult<List<IoDeviceDTO>> deviceResult = HttpUtil.parseJson(
                            deviceResponseJson, 
                            new TypeReference<ApiResult<List<IoDeviceDTO>>>() {}
                        );
                        
                        if (deviceResult.getCode() == 200 && deviceResult.getData() != null) {
                            for (IoDeviceDTO device : deviceResult.getData()) {
                                boolean isConnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                                deviceConnections.put(device.getId(), isConnected);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[启用任务] 获取设备连接状态失败: " + e.getMessage());
                        e.printStackTrace();
                        // 获取设备连接状态失败时，继续执行（不阻止任务启用）
                    }
                    
                    // 将设备连接状态Map转换为JSON字符串，并作为查询参数传递
                    if (!deviceConnections.isEmpty()) {
                        try {
                            String deviceConnectionsJson = HttpUtil.getObjectMapper().writeValueAsString(deviceConnections);
                            // URL编码JSON字符串
                            String encodedJson = URLEncoder.encode(deviceConnectionsJson, StandardCharsets.UTF_8.toString());
                            url += "&deviceConnectionsJson=" + encodedJson;
                        } catch (Exception e) {
                            System.err.println("[启用任务] 转换设备连接状态为JSON失败: " + e.getMessage());
                            e.printStackTrace();
                            // JSON转换失败时，继续执行（不阻止任务启用）
                        }
                    }
                }
                
                String responseJson = HttpUtil.doPut(url, null);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        currentTask.setOrderStatus(newStatus);
                        updateEnableButton();
                        updateButtonStates();
                        updateInputFieldsState(newStatus);
                        updateWorkStatus(newStatus);
                        
                        // 如果是启用任务（newStatus == 1），立即更新每垛箱数
                        if (newStatus == 1) {
                            boxesPerPallet = readBoxesPerPallet();
                            boxesPerPalletLabel.setText(String.valueOf(boxesPerPallet));
                            appendTextToTop(operationLogArea,getCurrentTime() + " 每垛箱数已更新为: " + boxesPerPallet + "\n");
                        }
                        
                        appendTextToTop(operationLogArea,getCurrentTime() + " " + actionText + "任务成功\n");
                        // 根据任务状态显示相应的成功提示
                        if (newStatus == 1 || newStatus == 3) {
                            showAlert(Alert.AlertType.INFORMATION, "成功", "启用成功");
                        } else if (newStatus == 0) {
                            showAlert(Alert.AlertType.INFORMATION, "成功", "停用成功");
                        } else {
                            showAlert(Alert.AlertType.INFORMATION, "成功", result.getMessage());
                        }
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(newStatus == 1 ? OperateTypeEnum.START : OperateTypeEnum.STOP)
                            .target(taskId.toString(), currentTask.getOrderNo())
                            .content(actionText + "任务: " + currentTask.getOrderNo())
                            .afterData(newStatus)
                            .saveAsync();
                    } else {
                        appendTextToTop(operationLogArea,getCurrentTime() + " " + actionText + "任务失败：" + result.getMessage() + "\n");
                        
                        // 如果是启用任务失败且错误信息包含设备连接相关提示，显示警告弹窗
                        if (newStatus == 1 && result.getMessage() != null && 
                            (result.getMessage().contains("设备未连接") || result.getMessage().contains("启用任务失败"))) {
                            showAlert(Alert.AlertType.WARNING, "启用失败", result.getMessage());
                        } else {
                            showAlert(Alert.AlertType.ERROR, "错误", result.getMessage());
                        }
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(newStatus == 1 ? OperateTypeEnum.START : OperateTypeEnum.STOP)
                            .target(taskId.toString(), currentTask != null ? currentTask.getOrderNo() : "")
                            .content(actionText + "任务失败")
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " " + actionText + "任务异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", actionText + "任务失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void onCompleteOrder() {
        System.out.println("订单结单");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击订单结单按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择一个生产订单");
            appendTextToTop(operationLogArea,getCurrentTime() + " 未选择订单，无法结单\n");
            return;
        }
        
        // 1. 检查任务是否已停止（只有停止了才能结单）
        Integer currentStatus = currentTask.getOrderStatus();
        if (currentStatus != null && currentStatus == 1) {
            // 任务正在运行，提示用户先停止任务
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("无法结单");
            warningAlert.setHeaderText("生产任务正在运行");
            warningAlert.setContentText("请先停止生产任务才能完成订单结单！");
            warningAlert.showAndWait();
            appendTextToTop(operationLogArea,getCurrentTime() + " 结单失败：生产任务正在运行，请先停止任务\n");
            return;
        }
        
        // 2. 检查订单是否已结单
        if (currentStatus != null && currentStatus == 2) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "该订单已经完成结单");
            return;
        }
        
        // 3. 弹出确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认结单");
        confirmAlert.setHeaderText("确定要对该订单进行结单吗？");
        confirmAlert.setContentText("订单编号：" + currentTask.getOrderNo() + 
                                   "\n产品名称：" + currentTask.getProductName() +
                                   "\n\n结单后订单状态将变为【已完成】");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                completeOrderToServer(currentTask.getId());
            }
        });
    }
    
    private void completeOrderToServer(Integer taskId) {
        new Thread(() -> {
            try {
                appendTextToTop(operationLogArea,getCurrentTime() + " 正在执行订单结单...\n");
                String responseJson = HttpUtil.doPut("/api/task/updateStatus/" + taskId + "/2", null);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        currentTask.setOrderStatus(2);
                        updateEnableButton();
                        updateButtonStates();
                        updateWorkStatus(2);
                        appendTextToTop(operationLogArea,getCurrentTime() + " 订单结单成功\n");
                        showAlert(Alert.AlertType.INFORMATION, "成功", "订单结单成功！\n订单编号：" + currentTask.getOrderNo());
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(OperateTypeEnum.COMPLETE)
                            .target(taskId.toString(), currentTask.getOrderNo())
                            .content("订单结单: " + currentTask.getOrderNo())
                            .saveAsync();
                    } else {
                        appendTextToTop(operationLogArea,getCurrentTime() + " 订单结单失败：" + result.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "错误", "订单结单失败：" + result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.TASK_MANAGEMENT)
                            .operateType(OperateTypeEnum.COMPLETE)
                            .target(taskId.toString(), currentTask != null ? currentTask.getOrderNo() : "")
                            .content("订单结单失败")
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " 订单结单异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "订单结单失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void onCodeRejection() {
        isRejectModeEnabled = !isRejectModeEnabled;
        String status = isRejectModeEnabled ? "已开启" : "已关闭";
        System.out.println("读码剔除：" + status);
        appendTextToTop(operationLogArea,getCurrentTime() + " 读码剔除：" + status + "\n");
        
        // 更新按钮文字
        codeRejectionButton.setText(isRejectModeEnabled ? "关闭剔除" : "读码剔除");
        
        // 记录操作日志
        OperateLogBuilder.create()
            .module(ModuleNameEnum.CODE_VALIDATE)
            .operateType(isRejectModeEnabled ? OperateTypeEnum.START : OperateTypeEnum.STOP)
            .content("读码剔除功能" + status)
            .afterData(isRejectModeEnabled)
            .saveAsync();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("读码剔除");
        alert.setHeaderText("读码剔除功能" + status);
        
        if (isRejectModeEnabled) {
            alert.setContentText("读码剔除功能已启用\n\n" +
                    "码校验设备将校验：\n" +
                    "• 无码（剔除）\n" +
                    "• 重码（剔除）\n\n" +
                    "合格码将放行，不合格码将被剔除。");
        } else {
            alert.setContentText("读码剔除功能已关闭\n\n" +
                    "码校验设备将不进行任何校验。");
        }
        alert.showAndWait();
    }
    
    @FXML
    private void onAddCode() {
        System.out.println("生成系统箱码");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击添加码按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择一个生产订单");
            appendTextToTop(operationLogArea,getCurrentTime() + " 未选择订单，无法生成箱码\n");
            return;
        }
        
        String orderNo = productionOrderField.getText();
        if (orderNo == null || orderNo.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "订单编号为空");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("生成系统箱码");
        confirmAlert.setHeaderText("确认生成系统箱码");
        confirmAlert.setContentText("将为当前订单生成一个系统箱码");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                generateSystemCode(orderNo);
            }
        });
    }
    
    private void generateSystemCode(String orderNo) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " 开始生成系统箱码...\n");
                });
                
                // 构建请求
                GenerateSystemCodeRequest request = new GenerateSystemCodeRequest();
                request.setOrderNo(orderNo);
                request.setProductNo(productCodeLabel.getText());
                request.setBatchNo(productionBatchField.getText());
                request.setType(typeComboBox.getSelectionModel().getSelectedIndex() == 0 ? 1 : 2);
                request.setBoxCount(readBoxesPerPallet());
                
                // 调用后端API
                String responseJson = HttpUtil.doPost("/api/code/generate-system-code", request);
                ApiResult<GenerateSystemCodeResult> result = 
                    HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<GenerateSystemCodeResult>>() {});
                
                if (result.getCode() == 200 && result.getData() != null) {
                    GenerateSystemCodeResult codeResult = result.getData();
                    
                    if (codeResult.isSuccess()) {
                        Platform.runLater(() -> {
                            // 使用后端返回的当前箱数（与箱码采集设备保持同步）
                            Integer backendCurrentCount = codeResult.getCurrentCount();
                            if (backendCurrentCount != null) {
                                currentBoxCountLabel.setText(String.valueOf(backendCurrentCount));
                                currentBoxCount = backendCurrentCount;
                                
                                // 在数据接收区显示生成的码（参考箱码采集设备的显示方式）
                                String displayText = getCurrentTime() + " [添加码] " + 
                                    codeResult.getGeneratedCode() + " 系统生成";
                                addColoredTextToDataReceive(displayText, "SYSTEM_GENERATED");
                                
                                appendTextToTop(operationLogArea,getCurrentTime() + " ✓ 生成成功：" + codeResult.getMessage() + "\n");
                                appendTextToTop(operationLogArea,getCurrentTime() + "   系统箱码=" + codeResult.getGeneratedCode() + "\n");
                                appendTextToTop(operationLogArea,getCurrentTime() + "   当前垛箱数=" + backendCurrentCount + "/" + boxesPerPallet + "\n");
                                showAlert(Alert.AlertType.INFORMATION, "成功", 
                                    "系统箱码生成成功！\n\n箱码：" + codeResult.getGeneratedCode() + 
                                    "\n当前垛箱数：" + backendCurrentCount + "/" + boxesPerPallet);
                            } else {
                                // 如果后端未返回当前箱数，使用旧逻辑（兼容）
                                currentBoxCount++;
                                
                                // 检查是否满垛
                                if (currentBoxCount >= boxesPerPallet) {
                                    currentBoxCount = 0;
                                    // 已生产垛数会从数据库查询，不再手动累加
                                    appendTextToTop(operationLogArea,getCurrentTime() + " 🎉 满垛！\n");
                                }
                                
                                // 更新UI显示
                                updateStatisticsDisplay();
                                
                                appendTextToTop(operationLogArea,getCurrentTime() + " ✓ 生成成功：" + codeResult.getMessage() + "\n");
                                appendTextToTop(operationLogArea,getCurrentTime() + "   系统箱码=" + codeResult.getGeneratedCode() + "\n");
                                appendTextToTop(operationLogArea,getCurrentTime() + "   当前垛箱数=" + currentBoxCount + "/" + boxesPerPallet + "\n");
                                showAlert(Alert.AlertType.INFORMATION, "成功", 
                                    "系统箱码生成成功！\n\n箱码：" + codeResult.getGeneratedCode() + 
                                    "\n当前垛箱数：" + currentBoxCount + "/" + boxesPerPallet);
                            }
                            
                            // 记录操作日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_COLLECT)
                                .operateType(OperateTypeEnum.ADD)
                                .target(codeResult.getGeneratedCode(), orderNo)
                                .content("生成系统箱码: " + codeResult.getGeneratedCode())
                                .afterData(codeResult)
                                .saveAsync();
                        });
                    } else {
                        Platform.runLater(() -> {
                            appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 生成失败：" + codeResult.getMessage() + "\n");
                            showAlert(Alert.AlertType.ERROR, "失败", codeResult.getMessage());
                            
                            // 记录失败日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_COLLECT)
                                .operateType(OperateTypeEnum.ADD)
                                .target("", orderNo)
                                .content("生成系统箱码失败")
                                .failReason(codeResult.getMessage())
                                .saveAsync();
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 接口调用失败：" + result.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "错误", "生成系统箱码失败：" + result.getMessage());
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 生成异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "异常", "生成系统箱码异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML 
    private void onClearCode() { 
        System.out.println("清除上一个箱码");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击清除上一个箱码按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择一个生产订单");
            return;
        }
        
        String orderNo = productionOrderField.getText();
        if (orderNo == null || orderNo.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "订单编号为空");
            return;
        }
        
        new Thread(() -> {
            try {
                // 1. 先获取当前垛信息（使用taskId）
                String infoResponseJson = HttpUtil.doGet("/api/code/current-pallet-info/by-task/" + currentTask.getId());
                ApiResult<CurrentPalletInfoVO> infoResult = 
                    HttpUtil.parseJson(infoResponseJson, new TypeReference<ApiResult<CurrentPalletInfoVO>>() {});
                
                if (infoResult.getCode() != 200 || infoResult.getData() == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "错误", "获取当前垛信息失败：" + infoResult.getMessage());
                    });
                    return;
                }
                
                CurrentPalletInfoVO info = infoResult.getData();
                Integer currentCount = info.getCurrentCount();
                
                // 检查是否有当前垛
                if (info.getTagNo() == null || info.getTagNo().isEmpty()) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.WARNING, "提示", "当前订单没有正在采集的垛，无法清除箱码");
                    });
                    return;
                }
                
                if (currentCount == 0) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.WARNING, "提示", "当前垛没有采集任何箱码，无法清除");
                    });
                    return;
                }
                
                // 2. 在UI线程打开确认对话框
                Platform.runLater(() -> {
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/DeleteCodeDialog.fxml"));
                        javafx.scene.Parent root = loader.load();
                        
                        DeleteCodeDialogController dialogController = loader.getController();
                        dialogController.setPalletInfo(orderNo, currentCount);
                        
                        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                        dialogStage.setTitle("清除上一个箱码");
                        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                        dialogStage.setScene(new javafx.scene.Scene(root));
                        dialogStage.setResizable(false);
                        StageIconUtil.setStageIcon(dialogStage);
                        dialogStage.showAndWait();

                        // 3. 如果用户确认，则调用后端接口
                        if (dialogController.isConfirmed()) {
                            deleteLastCodeFromServer(orderNo);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        appendTextToTop(operationLogArea,getCurrentTime() + " 打开对话框失败: " + e.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "错误", "打开对话框失败：" + e.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " 获取垛信息失败: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "获取垛信息失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 调用后端接口删除最后一个箱码
     */
    private void deleteLastCodeFromServer(String orderNo) {
        new Thread(() -> {
            try {
                appendTextToTop(operationLogArea,getCurrentTime() + " 正在清除上一个箱码...\n");
                
                // 获取产品编号
                String productNo = productCodeLabel.getText();
                if (productNo == null || productNo.isEmpty()) {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 产品编号为空，无法删除\n");
                        showAlert(Alert.AlertType.ERROR, "错误", "产品编号为空");
                    });
                    return;
                }
                
                // 使用空字符串作为code参数（后端会自动查找最新一条记录）
                String responseJson = HttpUtil.doDelete("/api/code/delete/LAST/" + orderNo + "/" + productNo);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson, 
                    new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✓ 上一个箱码已清除\n");
                        showAlert(Alert.AlertType.INFORMATION, "成功", "上一个箱码已清除！");
                        
                        // 前端实时统计：当前箱数-1
                        if (currentBoxCount > 0) {
                            currentBoxCount--;
                            updateStatisticsDisplay();
                        }
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_COLLECT)
                            .operateType(OperateTypeEnum.DELETE)
                            .target("", orderNo)
                            .content("清除上一个箱码: 订单=" + orderNo)
                            .saveAsync();
                    } else {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 清除失败：" + result.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "错误", result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_COLLECT)
                            .operateType(OperateTypeEnum.DELETE)
                            .target("", orderNo)
                            .content("清除上一个箱码失败")
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 清除异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "清除箱码失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML 
    private void onSpecifyBoxCount() {
        System.out.println("指定当前箱数");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击指定当前箱数按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择生产订单");
            return;
        }
        
        String orderNo = productionOrderField.getText();
        if (orderNo == null || orderNo.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "订单编号为空");
            return;
        }
        
        new Thread(() -> {
            try {
                // 1. 先获取当前垛信息（使用taskId）
                String infoResponseJson = HttpUtil.doGet("/api/code/current-pallet-info/by-task/" + currentTask.getId());
                ApiResult<CurrentPalletInfoVO> infoResult = 
                    HttpUtil.parseJson(infoResponseJson, new TypeReference<ApiResult<CurrentPalletInfoVO>>() {});
                
                if (infoResult.getCode() != 200 || infoResult.getData() == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "错误", "获取当前垛信息失败：" + infoResult.getMessage());
                    });
                    return;
                }
                
                CurrentPalletInfoVO info = infoResult.getData();
                Integer currentCount = info.getCurrentCount();
                // 从前端采集规格获取每垛箱数，而不是使用后端返回的值
                Integer boxesPerPallet = readBoxesPerPallet();
                
                // 2. 在UI线程打开对话框
                Platform.runLater(() -> {
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AdjustBoxCountDialog.fxml"));
                        javafx.scene.Parent root = loader.load();
                        
                        AdjustBoxCountDialogController dialogController = loader.getController();
                        dialogController.setCurrentPalletInfo(orderNo, currentCount, boxesPerPallet);
                        
                        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                        dialogStage.setTitle("指定当前箱数");
                        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                        dialogStage.setScene(new javafx.scene.Scene(root));
                        dialogStage.setResizable(false);
                        StageIconUtil.setStageIcon(dialogStage);
                        dialogStage.showAndWait();
                        
                        // 3. 如果用户确认，则调用后端接口
                        if (dialogController.isConfirmed()) {
                            Integer targetCount = dialogController.getTargetCount();
                            adjustBoxCount(orderNo, targetCount, boxesPerPallet);
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        appendTextToTop(operationLogArea,getCurrentTime() + " 打开对话框失败: " + e.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "错误", "打开对话框失败：" + e.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " 获取垛信息失败: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "获取垛信息失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 调整箱数
     */
    private void adjustBoxCount(String orderNo, Integer targetCount, Integer boxesPerPallet) {
        new Thread(() -> {
            try {
                appendTextToTop(operationLogArea,getCurrentTime() + " 开始调整箱数：目标=" + targetCount + "\n");
                
                // 构建请求
                AdjustBoxCountRequest request = new AdjustBoxCountRequest(orderNo, targetCount, boxesPerPallet);
                request.setProductNo(productCodeLabel.getText()); // 设置产品编号
                
                // 调用后端接口
                String responseJson = HttpUtil.doPost("/api/code/adjust-box-count", request);
                ApiResult<AdjustBoxCountResult> result = 
                    HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<AdjustBoxCountResult>>() {});
                
                if (result.getCode() == 200 && result.getData() != null) {
                    AdjustBoxCountResult adjustResult = result.getData();
                    
                    Platform.runLater(() -> {
                        if (adjustResult.getSuccess()) {
                            // 成功：更新UI统计
                            String operationType = adjustResult.getOperationType();
                            Integer afterCount = adjustResult.getAfterCount();
                            List<String> generatedCodes = adjustResult.getGeneratedCodes();
                            
                            // 前端实时统计：更新当前箱数
                            currentBoxCount = afterCount;
                            updateStatisticsDisplay();
                            
                            // 根据操作类型显示不同日志
                            if ("DELETE".equals(operationType)) {
                                appendTextToTop(operationLogArea,getCurrentTime() + " ✓ " + adjustResult.getMessage() + "\n");
                            } else if ("ADD".equals(operationType)) {
                                appendTextToTop(operationLogArea,getCurrentTime() + " ✓ " + adjustResult.getMessage() + "\n");
                                
                                // 在数据接收区显示生成的虚拟箱码（如果有）
                                if (generatedCodes != null && !generatedCodes.isEmpty()) {
                                    for (String code : generatedCodes) {
                                        String displayText = getCurrentTime() + " [指定箱数] " + 
                                            code + " 系统生成";
                                        addColoredTextToDataReceive(displayText, "SYSTEM_GENERATED");
                                    }
                                }
                            } else {
                                appendTextToTop(operationLogArea,getCurrentTime() + " ℹ " + adjustResult.getMessage() + "\n");
                            }
                            
                            showAlert(Alert.AlertType.INFORMATION, "成功", adjustResult.getMessage());
                            
                            // 记录操作日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_COLLECT)
                                .operateType(OperateTypeEnum.SPECIFY)
                                .target("", orderNo)
                                .content("指定当前箱数: 目标=" + targetCount + ", 操作=" + operationType)
                                .beforeData(adjustResult.getBeforeCount())
                                .afterData(adjustResult.getAfterCount())
                                .saveAsync();
                        } else {
                            appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 调整失败：" + adjustResult.getMessage() + "\n");
                            showAlert(Alert.AlertType.ERROR, "失败", adjustResult.getMessage());
                            
                            // 记录失败日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_COLLECT)
                                .operateType(OperateTypeEnum.SPECIFY)
                                .target("", orderNo)
                                .content("指定当前箱数失败")
                                .failReason(adjustResult.getMessage())
                                .saveAsync();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 调整失败：" + result.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "失败", result.getMessage());
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 调整异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "调整箱数异常：" + e.getMessage());
                });
            }
        }).start();
    }

    
    @FXML
    private void onForcePallet() {
        System.out.println("强制满垛");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击强制满垛按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择生产订单");
            return;
        }
        
        String orderNo = productionOrderField.getText();
        if (orderNo == null || orderNo.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "订单编号为空");
            return;
        }
        
        new Thread(() -> {
            try {
                // 1. 先获取当前垛信息（使用taskId）
                String infoResponseJson = HttpUtil.doGet("/api/code/current-pallet-info/by-task/" + currentTask.getId());
                ApiResult<CurrentPalletInfoVO> infoResult = 
                    HttpUtil.parseJson(infoResponseJson, new TypeReference<ApiResult<CurrentPalletInfoVO>>() {});
                
                if (infoResult.getCode() != 200 || infoResult.getData() == null) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "错误", "获取当前垛信息失败：" + infoResult.getMessage());
                    });
                    return;
                }
                
                CurrentPalletInfoVO info = infoResult.getData();
                Integer currentCount = info.getCurrentCount();
                // 从前端采集规格字段获取每垛箱数，而不是使用后端返回的数据库值
                Integer boxesPerPallet = readBoxesPerPallet();
                
                // 检查采集规格是否已设置
                if (boxesPerPallet == null || boxesPerPallet <= 0) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.WARNING, "提示", "请先设置采集规格（每垛箱数）");
                    });
                    return;
                }
                
                // 检查是否有当前垛
                if (info.getTagNo() == null || info.getTagNo().isEmpty()) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.WARNING, "提示", "当前订单没有正在采集的垛，无法强制满垛");
                    });
                    return;
                }
                
                if (currentCount == 0) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.WARNING, "提示", "当前垛没有采集任何箱码，无法强制满垛");
                    });
                    return;
                }
                
                // 2. 在UI线程打开确认对话框
                final Integer finalBoxesPerPallet = boxesPerPallet;
                Platform.runLater(() -> {
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ForcePalletDialog.fxml"));
                        javafx.scene.Parent root = loader.load();
                        
                        ForcePalletDialogController dialogController = loader.getController();
                        dialogController.setPalletInfo(orderNo, currentCount, finalBoxesPerPallet);
                        
                        javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                        dialogStage.setTitle("强制满垛确认");
                        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                        dialogStage.setScene(new javafx.scene.Scene(root));
                        dialogStage.setResizable(false);
                        StageIconUtil.setStageIcon(dialogStage);
                        dialogStage.showAndWait();
                        
                        // 3. 如果用户确认，则调用后端接口
                        if (dialogController.isConfirmed()) {
                            forcePalletExecute(orderNo, finalBoxesPerPallet);
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        appendTextToTop(operationLogArea,getCurrentTime() + " 打开对话框失败: " + e.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "错误", "打开对话框失败：" + e.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " 获取垛信息失败: " + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "获取垛信息失败：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 执行强制满垛
     */
    private void forcePalletExecute(String orderNo, Integer boxesPerPallet) {
        new Thread(() -> {
            try {
                // 构建请求，传入前端采集规格中的每垛箱数
                ForcePalletRequest request = new ForcePalletRequest(orderNo, boxesPerPallet);
                request.setProductNo(productCodeLabel.getText()); // 设置产品编号
                
                // 调用后端接口
                String responseJson = HttpUtil.doPost("/api/code/force-pallet", request);
                ApiResult<ForcePalletResult> result = 
                    HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<ForcePalletResult>>() {});
                
                if (result.getCode() == 200 && result.getData() != null) {
                    ForcePalletResult forceResult = result.getData();
                    
                    Platform.runLater(() -> {
                        if (forceResult.getSuccess()) {
                            // 成功：重置当前箱数为0（因为垛已标记为满垛状态，等待关联）
                            currentBoxCountLabel.setText("0");
                            
                            // 在操作日志区只显示一条简洁消息
                            Integer originalCount = forceResult.getOriginalCount() != null ? forceResult.getOriginalCount() : 0;
                            Integer targetBoxesPerPallet = forceResult.getBoxesPerPallet() != null ? forceResult.getBoxesPerPallet() : boxesPerPallet;
                            appendTextToTop(operationLogArea,getCurrentTime() + " 强制满垛成功！当前采集了 " + originalCount + " 箱（应采集 " + targetBoxesPerPallet + " 箱）。\n");
                            
                            showAlert(Alert.AlertType.INFORMATION, "强制满垛成功", forceResult.getMessage());
                            
                            // 向报警器发送信号02（短报警）
                            try {
                                if (isAlarmEnabled) {
                                    boolean alarmSent = DeviceConnectionManager.getInstance().sendToDeviceByCategory(5, "02");
                                    if (alarmSent) {
                                        // 在报警信息区域显示
                                        appendTextToTopAlarm(alarmInfoArea,getCurrentTime() + " 强制了满垛，但对应垛码待采集（短鸣提示）\n");
                                        // 更新报警状态为报警中
                                        isAlarming = true;
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("[报警器] 强制满垛报警发送异常: " + e.getMessage());
                            }
                            
                            // 记录操作日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                                .operateType(OperateTypeEnum.FORCE)
                                .target("", orderNo)
                                .content("强制满垛: 订单=" + orderNo + ", 已标记允许箱数不匹配")
                                .afterData(forceResult)
                                .saveAsync();
                        } else {
                            appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 强制满垛失败：" + forceResult.getMessage() + "\n");
                            showAlert(Alert.AlertType.ERROR, "失败", forceResult.getMessage());
                            
                            // 记录失败日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_ASSOCIATE_PALLET)
                                .operateType(OperateTypeEnum.FORCE)
                                .target("", orderNo)
                                .content("强制满垛失败")
                                .failReason(forceResult.getMessage())
                                .saveAsync();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 强制满垛失败：" + result.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "失败", result.getMessage());
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 强制满垛异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "强制满垛异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void onDeleteEmptyCodes() {
        System.out.println("删除本垛无码");
        appendTextToTop(operationLogArea,getCurrentTime() + " 点击删除本垛无码按钮\n");
        
        if (currentTask == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择生产订单");
            return;
        }
        
        String orderNo = productionOrderField.getText();
        if (orderNo == null || orderNo.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "订单编号为空");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("删除本垛无码");
        confirmAlert.setContentText("确定要删除当前垛的所有无码记录吗？");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteEmptyCodesExecute(orderNo);
            }
        });
    }
    
    /**
     * 执行删除本垛无码
     */
    private void deleteEmptyCodesExecute(String orderNo) {
        new Thread(() -> {
            try {
                appendTextToTop(operationLogArea,getCurrentTime() + " 开始删除本垛无码\n");
                
                // 构建请求
                DeleteEmptyCodesRequest request = new DeleteEmptyCodesRequest(orderNo);
                request.setProductNo(productCodeLabel.getText()); // 设置产品编号
                
                // 调用后端接口
                String responseJson = HttpUtil.doPost("/api/code/delete-empty-codes", request);
                ApiResult<DeleteEmptyCodesResult> result = 
                    HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<DeleteEmptyCodesResult>>() {});
                
                if (result.getCode() == 200 && result.getData() != null) {
                    DeleteEmptyCodesResult deleteResult = result.getData();
                    
                    Platform.runLater(() -> {
                        if (deleteResult.getSuccess()) {
                            // 成功：更新UI统计
                            Integer deletedCount = deleteResult.getDeletedCount();
                            Integer afterCount = deleteResult.getAfterCount();
                            
                            // 更新当前箱数（使用后端返回的 afterCount，该值是从数据库实际查询的）
                            currentBoxCount = afterCount;
                            currentBoxCountLabel.setText(String.valueOf(afterCount));
                            
                            // 删除消息中的"（22位系统箱码）"或"(22位系统箱码)"文字（兼容全角和半角括号）
                            String cleanMessage = deleteResult.getMessage()
                                .replace("（22位系统箱码）", "")
                                .replace("(22位系统箱码)", "")
                                .trim();
                            
                            // 显示日志
                            appendTextToTop(operationLogArea,getCurrentTime() + " ✓ " + cleanMessage + "\n");
                            
                            if (deletedCount > 0) {
                                appendTextToTop(operationLogArea,getCurrentTime() + " ℹ 删除了 " + deletedCount + " 条无码记录\n");
                                showAlert(Alert.AlertType.INFORMATION, "成功", cleanMessage);
                            } else {
                                showAlert(Alert.AlertType.INFORMATION, "提示", cleanMessage);
                            }
                            
                            // 记录操作日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_COLLECT)
                                .operateType(OperateTypeEnum.DELETE)
                                .target("", orderNo)
                                .content("删除本垛无码: 订单=" + orderNo + ", 删除数量=" + deletedCount)
                                .afterData(deleteResult)
                                .saveAsync();
                        } else {
                            appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 删除失败：" + deleteResult.getMessage() + "\n");
                            showAlert(Alert.AlertType.ERROR, "失败", deleteResult.getMessage());
                            
                            // 记录失败日志
                            OperateLogBuilder.create()
                                .module(ModuleNameEnum.CODE_COLLECT)
                                .operateType(OperateTypeEnum.DELETE)
                                .target("", orderNo)
                                .content("删除本垛无码失败")
                                .failReason(deleteResult.getMessage())
                                .saveAsync();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 删除失败：" + result.getMessage() + "\n");
                        showAlert(Alert.AlertType.ERROR, "失败", result.getMessage());
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    appendTextToTop(operationLogArea,getCurrentTime() + " ✗ 删除异常：" + e.getMessage() + "\n");
                    showAlert(Alert.AlertType.ERROR, "错误", "删除本垛无码异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void onCloseAlarm() {
        System.out.println("点击关闭报警按钮");
        
        // 向报警器发送01信号停止报警
        boolean stopSent = DeviceConnectionManager.getInstance().sendToDeviceByCategory(5, "01");
        
        if (stopSent) {
            System.out.println("[报警器] 发送停止报警指令: 01, 发送成功");
            appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 发送停止报警指令: 01\n");
            
            // 更新报警状态为正常
            isAlarming = false;
            
            // 记录操作日志
            OperateLogBuilder.create()
                .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                .operateType(OperateTypeEnum.STOP)
                .content("关闭报警器")
                .saveAsync();
            
            showAlert(Alert.AlertType.INFORMATION, "报警器", "已发送停止报警指令");
        } else {
            System.out.println("[报警器] 发送停止报警指令失败或未连接");
            appendTextToTop(operationLogArea,getCurrentTime() + " [" + getCategoryName(5) + "] 发送停止报警指令失败或未连接\n");
            showAlert(Alert.AlertType.WARNING, "报警器", "发送停止报警指令失败\n报警器可能未连接或通信异常");
        }
    }
    
    /**
     * 旧的报警器开关方法（已废弃，保留以防兼容性问题）
     */
    @Deprecated
    private void toggleAlarmEnabled_OLD() {
        isAlarmEnabled = !isAlarmEnabled;
        String status = isAlarmEnabled ? "已开启" : "已关闭";
        System.out.println("报警器：" + status);
        appendTextToTop(operationLogArea,getCurrentTime() + " 报警器：" + status + "\n");
        
        // 更新按钮文字
        closeAlarmButton.setText(isAlarmEnabled ? "关闭报警" : "开启报警");
        
        // 记录操作日志
        OperateLogBuilder.create()
            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
            .operateType(isAlarmEnabled ? OperateTypeEnum.START : OperateTypeEnum.STOP)
            .content("报警器功能" + status)
            .afterData(isAlarmEnabled)
            .saveAsync();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("报警器");
        alert.setHeaderText("报警器功能" + status);
        
        if (isAlarmEnabled) {
            alert.setContentText("报警器功能已开启\n\n" +
                    "检测到异常情况时将触发报警：\n" +
                    "• 无码、重码、错码（短报警）\n" +
                    "• 托盘码未读到（长报警）\n\n" +
                    "报警器将正常工作。");
        } else {
            alert.setContentText("报警器功能已关闭\n\n" +
                    "所有报警信号将被屏蔽，报警器不会响应任何异常情况。\n\n" +
                    "注意：剔除设备仍会正常工作。");
        }
        alert.showAndWait();
    }
    
    private void updateInputFieldsState(Integer status) {
        boolean isRunning = (status != null && status == 1);
        collectionSpecField.setDisable(isRunning);
        productionBatchField.setDisable(isRunning);
        typeComboBox.setDisable(isRunning);
    }
    
    /**
     * 更新按钮状态
     * 规则：
     * 1. 启用/停用任务按钮：始终可用
     * 2. 读码剔除按钮：始终可用
     * 3. 关闭报警按钮：始终可用
     * 4. 订单结单按钮：只有在任务已停止（status == 0）时才可用
     * 5. 其他按钮（添加码、清除码、指定当前箱数、强制满垛、删除本垛无码）：
     *    只有在任务已启动（status == 1 生产中）时才可用，否则置灰
     */
    private void updateButtonStates() {
        // 检查任务是否已启动（状态为1表示生产中，已启用）
        boolean isTaskRunning = (currentTask != null && currentTask.getOrderStatus() != null && currentTask.getOrderStatus() == 1);
        // 检查任务是否已停止（状态为0或3表示已停止，状态3表示未启用但有采集数据）
        boolean isTaskStopped = (currentTask != null && currentTask.getOrderStatus() != null && 
                                 (currentTask.getOrderStatus() == 0 || currentTask.getOrderStatus() == 3));
        
        // 启用/停用任务按钮：始终可用（不设置disable）
        // enableTaskButton - 不需要设置，始终可用
        
        // 读码剔除按钮：始终可用
        // codeRejectionButton - 不需要设置，始终可用
        
        // 关闭报警按钮：始终可用
        // closeAlarmButton - 不需要设置，始终可用
        
        // 订单结单按钮：只有任务停止时才可用
        completeOrderButton.setDisable(!isTaskStopped);
        
        // 其他按钮：只有任务启动时才可用
        addCodeButton.setDisable(!isTaskRunning);
        clearCodeButton.setDisable(!isTaskRunning);
        specifyBoxCountButton.setDisable(!isTaskRunning);
        forcePalletButton.setDisable(!isTaskRunning);
        deleteEmptyCodesButton.setDisable(!isTaskRunning);
    }
    
    private void updateEnableButton() {
        if (currentTask == null) {
            enableTaskButton.setText("启用任务");
            return;
        }
        Integer status = currentTask.getOrderStatus();
        // 状态0（待生产）和状态3（未启用但有采集数据）都显示"启用任务"
        if (status == null || status == 0 || status == 3) {
            enableTaskButton.setText("启用任务");
        } else if (status == 1) {
            enableTaskButton.setText("停用任务");
        } else {
            enableTaskButton.setText("已完成");
        }
    }
    
    /**
     * 更新工作状态显示
     * @param status 任务状态 (0=待机，1=启用中，2=已完成，3=待机但有采集数据)
     */
    private void updateWorkStatus(Integer status) {
        if (workStatusLabel != null) {
            // 状态0（待生产）和状态3（未启用但有采集数据）都显示为待机
            if (status == null || status == 0 || status == 3) {
                workStatusLabel.setText("工作状态：待机中");
            } else if (status == 1) {
                workStatusLabel.setText("工作状态：启用中");
            } else if (status == 2) {
                workStatusLabel.setText("工作状态：已完成");
            }
        }
    }
    
    /**
     * 清空实时上传数据区
     */
    private void clearUploadDataArea() {
        if (uploadDataList == null) {
            return;
        }
        Platform.runLater(() -> {
            uploadDataList.getItems().clear();
            displayedPallets.clear();
            // 显示空状态提示
            VBox emptyBox = new VBox();
            Label emptyLabel = new Label("暂无上传数据");
            emptyLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px; -fx-padding: 10px;");
            emptyBox.getChildren().add(emptyLabel);
            uploadDataList.getItems().add(emptyBox);
        });
    }
    
    /**
     * 添加托盘码到实时上传数据区（新的托盘码添加到最上面）
     * @param palletCode 托盘码
     * @param boxCount 该托盘下的箱数
     */
    private void addPalletToUploadArea(String palletCode, Integer boxCount) {
        Platform.runLater(() -> {
            if (palletCode == null || palletCode.trim().isEmpty()) {
                return;
            }
            
            int count = (boxCount != null) ? boxCount : 0;
            
            // 如果托盘码已存在，移除旧的（因为要把新的放到最上面）
            displayedPallets.remove(palletCode);
            
            // 创建新的LinkedHashMap，将新托盘码放在最前面
            java.util.LinkedHashMap<String, PalletUploadVO> newMap = new java.util.LinkedHashMap<>();
            newMap.put(palletCode, new PalletUploadVO(palletCode, count, "待上传"));
            newMap.putAll(displayedPallets);
            displayedPallets = newMap;
            
            // 限制最多保留100条数据，超过则删除最旧的数据（列表末尾）
            final int MAX_UPLOAD_DATA_COUNT = 100;
            if (displayedPallets.size() > MAX_UPLOAD_DATA_COUNT) {
                // 获取所有键，删除最旧的（从末尾开始删除）
                java.util.List<String> keysToRemove = new java.util.ArrayList<>();
                int index = 0;
                for (String key : displayedPallets.keySet()) {
                    if (index >= MAX_UPLOAD_DATA_COUNT) {
                        keysToRemove.add(key);
                    }
                    index++;
                }
                // 删除多余的键
                for (String key : keysToRemove) {
                    displayedPallets.remove(key);
                }
            }
            
            // 刷新显示
            refreshUploadAreaDisplay();
        });
    }
    
    /**
     * 刷新实时上传区显示
     */
    private void refreshUploadAreaDisplay() {
        if (uploadDataList == null) {
            return;
        }
        
        uploadDataList.getItems().clear();
        
        if (displayedPallets.isEmpty()) {
            // 显示空状态提示
            VBox emptyBox = new VBox();
            Label emptyLabel = new Label("暂无上传数据");
            emptyLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px; -fx-padding: 10px;");
            emptyBox.getChildren().add(emptyLabel);
            uploadDataList.getItems().add(emptyBox);
        } else {
            // 按添加顺序显示所有托盘码（最新的在最上面）
            for (PalletUploadVO palletVO : displayedPallets.values()) {
                VBox itemBox = createUploadItem(
                    palletVO.getPalletCode(), 
                    String.valueOf(palletVO.getBoxCount()), 
                    palletVO.getUploadStatus()
                );
                uploadDataList.getItems().add(itemBox);
            }
            
            // 滚动到顶部显示最新数据
            uploadDataList.scrollTo(0);
        }
    }
    
    /**
     * 创建上传项UI
     * @param palletCode 托盘码
     * @param boxCount 箱数
     * @param status 上传状态
     */
    private VBox createUploadItem(String palletCode, String boxCount, String status) {
        VBox itemBox = new VBox(3);
        itemBox.setStyle("-fx-padding: 8px; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-background-color: white;");
        
        // 托盘码标签（字体更大）
        Label palletLabel = new Label(palletCode);
        palletLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        
        // 箱数和状态按钮的容器
        javafx.scene.layout.HBox infoBox = new javafx.scene.layout.HBox(10);
        infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label boxLabel = new Label(boxCount + "箱");
        boxLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        Button statusButton = new Button(status);
        statusButton.setStyle(getStatusButtonStyle(status));
        statusButton.setPrefWidth(80);
        statusButton.setPrefHeight(22);
        statusButton.setOnAction(e -> {
            System.out.println("点击上传托盘码：" + palletCode);
            // TODO: 调用上传接口
        });
        
        infoBox.getChildren().addAll(boxLabel, statusButton);
        
        itemBox.getChildren().addAll(palletLabel, infoBox);
        
        return itemBox;
    }
    
    /**
     * 获取状态按钮样式
     */
    private String getStatusButtonStyle(String status) {
        String baseStyle = "-fx-font-size: 10px; -fx-cursor: hand; -fx-background-radius: 12; -fx-border-radius: 12; ";
        
        switch (status) {
            case "上传成功":
                return baseStyle + "-fx-background-color: #e8f5e9; -fx-text-fill: #4caf50; -fx-border-color: #4caf50; -fx-border-width: 1;";
            case "上传失败":
                return baseStyle + "-fx-background-color: #ffebee; -fx-text-fill: #f44336; -fx-border-color: #f44336; -fx-border-width: 1;";
            case "待上传":
                return baseStyle + "-fx-background-color: #fff3e0; -fx-text-fill: #ff9800; -fx-border-color: #ff9800; -fx-border-width: 1;";
            default:
                return baseStyle + "-fx-background-color: #f5f5f5; -fx-text-fill: #999999; -fx-border-color: #cccccc; -fx-border-width: 1;";
        }
    }
    
    /**
     * 处理扫码枪数据
     * 扫码枪的数据不显示在数据接收区和操作日志中，仅进行业务处理
     * 如果查询码页面已打开，则将数据填入输入框并自动查询
     * 如果码替换页面已打开，则将数据填入输入框
     * 
     * @param barcodeData 扫码枪扫描的条码数据
     */
    private void handleBarcodeScannerData(String barcodeData) {
        Platform.runLater(() -> {
            // 不在操作日志中显示扫码枪数据
            System.out.println("[扫码枪] 扫码数据: " + barcodeData);
            
            // 记录扫码日志到数据库（后台记录，不显示在界面上）
            OperateLogBuilder.create()
                .module(ModuleNameEnum.BARCODE_SCAN)
                .operateType(OperateTypeEnum.COLLECT)
                .content("扫码枪扫描: " + barcodeData)
                .saveAsync();
            
            // 如果查询码页面已打开，将扫码数据填入输入框并自动查询
            CodeQueryController queryController = CodeQueryController.getCurrentInstance();
            if (queryController != null) {
                System.out.println("[扫码枪] 查询码页面已打开，自动填入数据并查询: " + barcodeData);
                CodeQueryController.handleBarcodeData(barcodeData);
            }
            
            // 如果码替换页面已打开，将扫码数据填入输入框（优先原码，原码有内容则填新码）
            CodeReplaceController replaceController = CodeReplaceController.getCurrentInstance();
            if (replaceController != null) {
                System.out.println("[扫码枪] 码替换页面已打开，自动填入数据: " + barcodeData);
                CodeReplaceController.handleBarcodeData(barcodeData);
            }
            
            // 如果两个页面都未打开，仅记录日志（后台记录）
            if (queryController == null && replaceController == null) {
                System.out.println("[扫码枪] 查询码页面和码替换页面都未打开，仅后台记录日志");
            }
        });
    }
}
