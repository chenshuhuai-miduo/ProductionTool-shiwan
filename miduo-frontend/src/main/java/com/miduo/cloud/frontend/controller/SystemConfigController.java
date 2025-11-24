package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.service.DeviceConnectionManager;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 系统配置控制器
 * 功能：系统参数配置管理
 * 
 * 注意：API路径已更新为重构后的路径 /api/device/*
 */
public class SystemConfigController {
    
    // 选项卡
    @FXML private TabPane configTabPane;
    
    // IO设备管理
    @FXML private TableView<IoDeviceDTO> ioDeviceTableView;
    @FXML private TableColumn<IoDeviceDTO, String> ioDeviceNameColumn;
    @FXML private TableColumn<IoDeviceDTO, String> ioDeviceCategoryColumn;
    @FXML private TableColumn<IoDeviceDTO, String> ioConnectionTypeColumn;
    @FXML private TableColumn<IoDeviceDTO, String> ioDeviceAddressColumn;
    @FXML private TableColumn<IoDeviceDTO, String> ioDeviceStatusColumn;
    @FXML private TableColumn<IoDeviceDTO, Void> ioDeviceActionColumn;
    
    private ObservableList<IoDeviceDTO> ioDeviceList = FXCollections.observableArrayList();
    
    // 打印机管理
    @FXML private TableView<?> printerTableView;
    @FXML private TableColumn<?, ?> printerNameColumn;
    @FXML private TableColumn<?, ?> printerPathColumn;
    @FXML private TableColumn<?, ?> printerStatusColumn;
    @FXML private TableColumn<?, ?> printerActionColumn;
    
    // 报警设置
    @FXML private CheckBox soundAlarmEnabledCheckBox;
    @FXML private TextField alarmDelayField;
    @FXML private TextField alarmIntervalField;
    
    // 右侧设备状态监控
    @FXML private VBox deviceStatusContainer;
    
    // 底部状态栏
    @FXML private Label configStatusLabel;
    @FXML private Label currentTimeLabel;
    
    private Timer timer;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("系统配置界面初始化...");
        
        // 初始化表格
        initializeTables();
        
        // 禁止列重新排序
        ioDeviceTableView.getColumns().forEach(column -> column.setReorderable(false));
        printerTableView.getColumns().forEach(column -> column.setReorderable(false));
        
        // 禁止列排序
        ioDeviceTableView.getColumns().forEach(column -> column.setSortable(false));
        printerTableView.getColumns().forEach(column -> column.setSortable(false));
        
        // 初始化报警设置默认值
        soundAlarmEnabledCheckBox.setSelected(true);
        alarmDelayField.setText("5");
        alarmIntervalField.setText("60");
        
        // 启动实时时钟
        startRealtimeClock();
        
        configStatusLabel.setText("配置状态: 就绪");
    }
    
    /**
     * 初始化表格
     */
    private void initializeTables() {
        // 初始化IO设备表格列
        ioDeviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        ioDeviceCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("deviceCategory"));
        ioConnectionTypeColumn.setCellValueFactory(new PropertyValueFactory<>("connectionType"));
        ioDeviceAddressColumn.setCellValueFactory(cellData -> {
            IoDeviceDTO device = cellData.getValue();
            String address = device.getAddress() + ":" + device.getPort();
            return new javafx.beans.property.SimpleStringProperty(address);
        });
        ioDeviceStatusColumn.setCellValueFactory(new PropertyValueFactory<>("statusText"));
        
        // 设置操作列
        ioDeviceActionColumn.setCellFactory(param -> new TableCell<IoDeviceDTO, Void>() {
            private final Button editButton = new Button("编辑");
            private final Button deleteButton = new Button("删除");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                
                editButton.setOnAction(event -> {
                    IoDeviceDTO device = getTableView().getItems().get(getIndex());
                    onEditIODevice(device);
                });

                deleteButton.setOnAction(event -> {
                    IoDeviceDTO device = getTableView().getItems().get(getIndex());
                    onDeleteIODevice(device);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
        
        // 设置表格行样式 - 去除选中时文字变白的效果
        ioDeviceTableView.setRowFactory(tv -> {
            TableRow<IoDeviceDTO> row = new TableRow<>();
            
            // 点击行时不改变样式（去除选中效果）
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle(""); // 保持无样式
                }
            });
            
            return row;
        });
        
        ioDeviceTableView.setItems(ioDeviceList);
        ioDeviceTableView.setPlaceholder(new Label("暂无IO设备配置"));
        
        // 去除选中时的焦点样式
        ioDeviceTableView.setFocusTraversable(false);
        
        printerTableView.setPlaceholder(new Label("暂无打印机配置"));
        
        // 加载IO设备数据
        loadIoDevices();
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
    
    /**
     * 加载IO设备列表
     * API路径已更新：/api/iodevice/list → /api/device/list
     */
    private void loadIoDevices() {
        new Thread(() -> {
            try {
                // 在UI线程中更新状态标签
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: 正在加载设备列表...");
                });
                
                // 调用重构后的接口：GET /api/device/list
                String responseJson = HttpUtil.doGet("/api/device/list");
                ApiResult<List<IoDeviceDTO>> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<List<IoDeviceDTO>>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200 && result.getData() != null) {
                        ioDeviceList.clear();
                        // 根据实际连接状态更新设备状态文本
                        for (IoDeviceDTO device : result.getData()) {
                            boolean isConnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                            device.setStatusText(isConnected ? "已连接" : "未连接");
                        }
                        ioDeviceList.addAll(result.getData());
                        configStatusLabel.setText("配置状态: 设备列表加载成功 (共" + result.getData().size() + "个设备)");
                    } else {
                        configStatusLabel.setText("配置状态: 设备列表加载失败");
                        showAlert(Alert.AlertType.ERROR, "错误", "加载设备列表失败：" + result.getMessage());
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: 设备列表加载异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "加载设备列表异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    // ==================== IO设备管理事件 ====================
    
    /**
     * 添加IO设备
     */
    @FXML
    private void onAddIODevice() {
        System.out.println("添加IO设备");
        
        try {
            // 加载IO设备对话框FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/IoDeviceDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // 获取控制器并设置为添加模式
            IoDeviceDialogController dialogController = loader.getController();
            dialogController.setEditMode(false);

            // 创建对话框Stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("添加IO设备");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(root));
            com.miduo.cloud.frontend.util.StageIconUtil.setStageIcon(dialogStage);
            dialogStage.showAndWait();

            // 检查是否确认保存
            if (dialogController.isConfirmed()) {
                // 构建设备DTO
                IoDeviceDTO device = new IoDeviceDTO();
                device.setDeviceName(dialogController.getDeviceName());
                device.setDeviceCategory(dialogController.getDeviceCategory());
                device.setConnectionType(dialogController.getConnectionType());
                device.setProtocolType(dialogController.getProtocolType());
                device.setAddress(dialogController.getIp());
                device.setPort(dialogController.getPort());
                device.setTimeout(Integer.parseInt(dialogController.getTimeout()));
                device.setRetryCount(Integer.parseInt(dialogController.getRetry()));
                device.setEnabled(dialogController.isEnabled());
                device.setDescription(dialogController.getDescription());
                
                // 调用后端接口保存
                saveDeviceToServer(device);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                configStatusLabel.setText("配置状态: IO设备添加失败");
                showAlert(Alert.AlertType.ERROR, "错误", "打开IO设备对话框失败：" + e.getMessage());
            });
        }
    }
    
    /**
     * 保存设备到服务器
     * API路径已更新：/api/iodevice/add → /api/device/add
     */
    private void saveDeviceToServer(IoDeviceDTO device) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: 正在保存设备...");
                });
                
                // 调用重构后的接口：POST /api/device/add
                String responseJson = HttpUtil.doPost("/api/device/add", device);
                ApiResult<String> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<String>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        configStatusLabel.setText("配置状态: IO设备添加成功");
                        showAlert(Alert.AlertType.INFORMATION, "成功", "IO设备添加成功！");
                        // 刷新列表
                        loadIoDevices();
                        
                        // 如果设备已启用，立即启动连接
                        if (device.getEnabled()) {
                            device.setId(result.getData()); // 设置服务器返回的ID
                            tryStartDeviceConnection(device);
                        }
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.ADD)
                            .target(result.getData(), device.getDeviceName())
                            .content("添加IO设备: " + device.getDeviceName() + " (" + device.getDeviceCategory() + ")")
                            .afterData(device)
                            .saveAsync();
                    } else {
                        configStatusLabel.setText("配置状态: IO设备添加失败");
                        showAlert(Alert.AlertType.ERROR, "错误", result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.ADD)
                            .target("", device.getDeviceName())
                            .content("添加IO设备失败: " + device.getDeviceName())
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: IO设备添加异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "添加设备异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 尝试启动设备连接
     */
    private void tryStartDeviceConnection(IoDeviceDTO device) {
        try {
            DeviceConnectionManager.getInstance().startConnection(device);
            Platform.runLater(() -> {
                configStatusLabel.setText("配置状态: 设备连接已启动");
                showAlert(Alert.AlertType.INFORMATION, "成功", 
                         "设备已保存并成功启动连接！\n设备名称：" + device.getDeviceName());
                
                // 更新设备状态为已连接
                updateDeviceStatusInTable(device.getId(), "已连接");
                
                // 记录连接成功日志
                OperateLogBuilder.create()
                    .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                    .operateType(OperateTypeEnum.CONNECT)
                    .target(device.getId(), device.getDeviceName())
                    .content("启动设备连接: " + device.getDeviceName())
                    .saveAsync();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.WARNING, "警告", 
                         "设备已保存，但连接启动失败：" + e.getMessage());
                
                // 更新设备状态为未连接
                updateDeviceStatusInTable(device.getId(), "未连接");
                
                // 记录连接失败日志
                OperateLogBuilder.create()
                    .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                    .operateType(OperateTypeEnum.CONNECT)
                    .target(device.getId(), device.getDeviceName())
                    .content("启动设备连接失败: " + device.getDeviceName())
                    .failReason(e.getMessage())
                    .saveAsync();
            });
        }
    }
    
    /**
     * 编辑IO设备
     */
    private void onEditIODevice(IoDeviceDTO device) {
        System.out.println("编辑IO设备：" + device.getDeviceName());
        
        try {
            // 加载IO设备对话框FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/IoDeviceDialog.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // 获取控制器并设置为编辑模式
            IoDeviceDialogController dialogController = loader.getController();
            dialogController.setEditMode(true);
            
            // 填充现有数据
            dialogController.fillDeviceData(
                device.getDeviceName(),
                device.getDeviceCategory(),
                device.getConnectionType(),
                device.getProtocolType(),
                device.getAddress(),
                device.getPort(),
                String.valueOf(device.getTimeout()),
                String.valueOf(device.getRetryCount()),
                device.getEnabled(),
                device.getDescription()
            );

            // 创建对话框Stage
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("编辑IO设备");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(root));
            com.miduo.cloud.frontend.util.StageIconUtil.setStageIcon(dialogStage);
            dialogStage.showAndWait();

            // 检查是否确认保存
            if (dialogController.isConfirmed()) {
                // 构建设备DTO
                IoDeviceDTO updatedDevice = new IoDeviceDTO();
                updatedDevice.setId(device.getId()); // 保留原ID
                updatedDevice.setDeviceName(dialogController.getDeviceName());
                updatedDevice.setDeviceCategory(dialogController.getDeviceCategory());
                updatedDevice.setConnectionType(dialogController.getConnectionType());
                updatedDevice.setProtocolType(dialogController.getProtocolType());
                updatedDevice.setAddress(dialogController.getIp());
                updatedDevice.setPort(dialogController.getPort());
                updatedDevice.setTimeout(Integer.parseInt(dialogController.getTimeout()));
                updatedDevice.setRetryCount(Integer.parseInt(dialogController.getRetry()));
                updatedDevice.setEnabled(dialogController.isEnabled());
                updatedDevice.setDescription(dialogController.getDescription());
                
                // 调用后端接口更新
                updateDeviceToServer(updatedDevice);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                configStatusLabel.setText("配置状态: IO设备编辑失败");
                showAlert(Alert.AlertType.ERROR, "错误", "打开IO设备对话框失败：" + e.getMessage());
            });
        }
    }
    
    /**
     * 更新设备到服务器
     * API路径已更新：/api/iodevice/update → /api/device/update
     */
    private void updateDeviceToServer(IoDeviceDTO device) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: 正在更新设备...");
                });
                
                // 调用重构后的接口：PUT /api/device/update
                String responseJson = HttpUtil.doPut("/api/device/update", device);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        configStatusLabel.setText("配置状态: IO设备更新成功");
                        showAlert(Alert.AlertType.INFORMATION, "成功", "IO设备更新成功！");
                        
                        // 处理设备状态变化：如果设备被禁用，立刻断开连接
                        handleDeviceStatusChange(device);
                        
                        // 刷新列表
                        loadIoDevices();
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.UPDATE)
                            .target(device.getId(), device.getDeviceName())
                            .content("更新IO设备: " + device.getDeviceName() + " (" + device.getDeviceCategory() + ")")
                            .afterData(device)
                            .saveAsync();
                    } else {
                        configStatusLabel.setText("配置状态: IO设备更新失败");
                        showAlert(Alert.AlertType.ERROR, "错误", result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.UPDATE)
                            .target(device.getId(), device.getDeviceName())
                            .content("更新IO设备失败: " + device.getDeviceName())
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: IO设备更新异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "更新设备异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 处理设备状态变化
     * 当设备被禁用时，立刻断开连接
     * 当设备被启用时，尝试建立连接
     */
    private void handleDeviceStatusChange(IoDeviceDTO device) {
        try {
            if (!device.getEnabled()) {
                // 设备被禁用，断开连接
                boolean wasConnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                if (wasConnected) {
                    DeviceConnectionManager.getInstance().stopConnection(device.getId());
                    System.out.println("[设备管理] 设备已禁用，断开连接: " + device.getDeviceName());
                    
                    Platform.runLater(() -> {
                        configStatusLabel.setText("配置状态: 设备 " + device.getDeviceName() + " 已禁用并断开连接");
                        // 更新设备状态为未连接
                        updateDeviceStatusInTable(device.getId(), "未连接");
                    });
                    
                    // 记录断开日志
                    OperateLogBuilder.create()
                        .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                        .operateType(OperateTypeEnum.DISCONNECT)
                        .target(device.getId(), device.getDeviceName())
                        .content("设备禁用，断开连接: " + device.getDeviceName())
                        .saveAsync();
                } else {
                    System.out.println("[设备管理] 设备已禁用: " + device.getDeviceName() + " (未连接，无需断开)");
                    Platform.runLater(() -> {
                        // 更新设备状态为未连接
                        updateDeviceStatusInTable(device.getId(), "未连接");
                    });
                }
            } else {
                // 设备被启用，尝试连接
                boolean isConnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                if (!isConnected) {
                    System.out.println("[设备管理] 设备已启用，尝试建立连接: " + device.getDeviceName());
                    tryStartDeviceConnection(device);
                } else {
                    System.out.println("[设备管理] 设备已启用: " + device.getDeviceName() + " (已连接)");
                    Platform.runLater(() -> {
                        // 更新设备状态为已连接
                        updateDeviceStatusInTable(device.getId(), "已连接");
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[设备管理] 处理设备状态变化异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取当前时间字符串（用于操作日志）
     */
    private String getCurrentTime() {
        return java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }
    
    /**
     * 删除IO设备
     */
    private void onDeleteIODevice(IoDeviceDTO device) {
        System.out.println("删除IO设备：" + device.getDeviceName());
        
        // 确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("确定要删除该IO设备吗？");
        confirmAlert.setContentText("设备名称：" + device.getDeviceName() + "\n此操作不可恢复！");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteDeviceFromServer(device.getId(), device.getDeviceName());
            }
        });
    }
    
    /**
     * 从服务器删除设备
     * API路径已更新：/api/iodevice/delete/{id} → /api/device/delete/{id}
     */
    private void deleteDeviceFromServer(String deviceId, String deviceName) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: 正在删除设备...");
                });
                
                // 调用重构后的接口：DELETE /api/device/delete/{id}
                String responseJson = HttpUtil.doDelete("/api/device/delete/" + deviceId);
                ApiResult<Boolean> result = HttpUtil.parseJson(responseJson,
                    new TypeReference<ApiResult<Boolean>>() {});
                
                Platform.runLater(() -> {
                    if (result.getCode() == 200) {
                        configStatusLabel.setText("配置状态: IO设备删除成功");
                        showAlert(Alert.AlertType.INFORMATION, "成功", "IO设备删除成功！");
                        // 刷新列表
                        loadIoDevices();
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.DELETE)
                            .target(deviceId, deviceName)
                            .content("删除IO设备: " + deviceName)
                            .saveAsync();
                    } else {
                        configStatusLabel.setText("配置状态: IO设备删除失败");
                        showAlert(Alert.AlertType.ERROR, "错误", result.getMessage());
                        
                        // 记录失败日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.DELETE)
                            .target(deviceId, deviceName)
                            .content("删除IO设备失败: " + deviceName)
                            .failReason(result.getMessage())
                            .saveAsync();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: IO设备删除异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "删除设备异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 保存IO配置
     */
    @FXML
    private void onSaveIOConfig() {
        System.out.println("保存IO配置");
        configStatusLabel.setText("配置状态: IO配置已自动保存到INI文件");
        showAlert(Alert.AlertType.INFORMATION, "提示", "IO设备配置已保存！");
    }
    
    /**
     * 测试所有IO设备连接
     * API路径已更新：/api/iodevice/list → /api/device/list
     */
    @FXML
    private void onTestAllIODevices() {
        System.out.println("测试所有IO设备连接");
        Platform.runLater(() -> {
            configStatusLabel.setText("配置状态: 正在测试所有设备连接...");
        });
        
        new Thread(() -> {
            try {
                // 调用重构后的接口：GET /api/device/list
                String responseJson = HttpUtil.doGet("/api/device/list");
                ApiResult<List<IoDeviceDTO>> result = 
                    HttpUtil.parseJson(responseJson,
                        new TypeReference<ApiResult<List<IoDeviceDTO>>>() {});
                
                if (result.getCode() == 200 && result.getData() != null) {
                    List<IoDeviceDTO> devices = result.getData();
                    int totalCount = devices.size();
                    int testedCount = 0;
                    int connectedCount = 0;
                    int reconnectedCount = 0;
                    
                    Platform.runLater(() -> {
                        configStatusLabel.setText("配置状态: 找到 " + totalCount + " 个设备，开始测试连接...");
                    });
                    
                    for (IoDeviceDTO device : devices) {
                        testedCount++;
                        final int currentIndex = testedCount;
                        
                        Platform.runLater(() -> {
                            configStatusLabel.setText("配置状态: 正在测试 [" + currentIndex + "/" + totalCount + "] " + device.getDeviceName());
                        });
                        
                        // 检查设备是否已连接
                        boolean isConnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                        
                        if (isConnected) {
                            // 已连接
                            connectedCount++;
                            System.out.println("[测试设备] " + device.getDeviceName() + " - 已连接");
                            
                            Platform.runLater(() -> {
                                updateDeviceStatusInTable(device.getId(), "已连接");
                            });
                            
                        } else {
                            // 未连接，尝试重新连接
                            System.out.println("[测试设备] " + device.getDeviceName() + " - 未连接，尝试重新连接...");
                            
                            Platform.runLater(() -> {
                                updateDeviceStatusInTable(device.getId(), "连接中...");
                            });
                            
                            try {
                                // 如果设备是启用状态，尝试连接
                                if (device.getEnabled()) {
                                    // 先停止旧连接
                                    DeviceConnectionManager.getInstance().stopConnection(device.getId());
                                    Thread.sleep(500); // 等待资源释放
                                    
                                    // 启动新连接
                                    DeviceConnectionManager.getInstance().startConnection(device);
                                    Thread.sleep(1000); // 等待连接建立
                                    
                                    // 再次检查连接状态
                                    boolean reconnected = DeviceConnectionManager.getInstance().isConnected(device.getId());
                                    
                                    if (reconnected) {
                                        reconnectedCount++;
                                        connectedCount++;
                                        System.out.println("[测试设备] " + device.getDeviceName() + " - 重新连接成功");
                                        
                                        Platform.runLater(() -> {
                                            updateDeviceStatusInTable(device.getId(), "已连接");
                                        });
                                    } else {
                                        System.err.println("[测试设备] " + device.getDeviceName() + " - 重新连接失败");
                                        
                                        Platform.runLater(() -> {
                                            updateDeviceStatusInTable(device.getId(), "连接失败");
                                        });
                                    }
                                } else {
                                    System.out.println("[测试设备] " + device.getDeviceName() + " - 设备已禁用，跳过连接");
                                    
                                    Platform.runLater(() -> {
                                        updateDeviceStatusInTable(device.getId(), "未连接");
                                    });
                                }
                            } catch (Exception e) {
                                System.err.println("[测试设备] " + device.getDeviceName() + " - 连接异常: " + e.getMessage());
                                e.printStackTrace();
                                
                                Platform.runLater(() -> {
                                    updateDeviceStatusInTable(device.getId(), "连接异常");
                                });
                            }
                        }
                        
                        // 短暂延迟，避免过快测试
                        Thread.sleep(300);
                    }
                    
                    // 显示测试结果
                    final int finalConnected = connectedCount;
                    final int finalReconnected = reconnectedCount;
                    final int failedCount = totalCount - connectedCount;
                    
                    Platform.runLater(() -> {
                        String resultMessage = String.format(
                            "测试完成！\n\n总设备数：%d\n已连接：%d\n重新连接成功：%d\n连接失败：%d",
                            totalCount, finalConnected, finalReconnected, failedCount
                        );
                        
                        configStatusLabel.setText("配置状态: 设备测试完成 - " + finalConnected + "/" + totalCount + " 已连接");
                        
                        Alert alert = new Alert(
                            failedCount > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION,
                            resultMessage,
                            ButtonType.OK
                        );
                        alert.setTitle("设备测试结果");
                        alert.setHeaderText("IO设备连接测试完成");
                        alert.showAndWait();
                        
                        // 记录操作日志
                        OperateLogBuilder.create()
                            .module(ModuleNameEnum.DEVICE_MANAGEMENT)
                            .operateType(OperateTypeEnum.TEST)
                            .content("测试所有IO设备: 总数=" + totalCount + ", 已连接=" + finalConnected + 
                                    ", 重连成功=" + finalReconnected + ", 失败=" + failedCount)
                            .result(failedCount == 0 ? "成功" : "部分失败")
                            .saveAsync();
                    });
                    
                } else {
                    Platform.runLater(() -> {
                        configStatusLabel.setText("配置状态: 获取设备列表失败");
                        showAlert(Alert.AlertType.ERROR, "错误", "获取设备列表失败：" + result.getMessage());
                    });
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    configStatusLabel.setText("配置状态: 测试设备时发生异常");
                    showAlert(Alert.AlertType.ERROR, "错误", "测试设备时发生异常：" + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 更新表格中设备的状态显示
     */
    private void updateDeviceStatusInTable(String deviceId, String status) {
        for (IoDeviceDTO device : ioDeviceList) {
            if (device.getId().equals(deviceId)) {
                device.setStatusText(status);
                ioDeviceTableView.refresh(); // 刷新表格显示
                break;
            }
        }
    }
    
    // ==================== 打印机管理事件 ====================
    
    /**
     * 添加打印机
     */
    @FXML
    private void onAddPrinter() {
        System.out.println("添加打印机");
        showAlert(Alert.AlertType.INFORMATION, "功能提示", "添加打印机功能待实现");
    }
    
    /**
     * 应用打印机配置
     */
    @FXML
    private void onApplyPrinterConfig() {
        System.out.println("应用打印机配置");
        configStatusLabel.setText("配置状态: 正在应用打印机配置...");
        showAlert(Alert.AlertType.INFORMATION, "功能提示", "应用打印机配置功能待实现");
        configStatusLabel.setText("配置状态: 配置已应用");
    }
    
    // ==================== 报警设置事件 ====================
    
    /**
     * 测试报警
     */
    @FXML
    private void onTestAlarm() {
        System.out.println("测试报警");
        if (!soundAlarmEnabledCheckBox.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "提示", "声音报警未启用");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "功能提示", "测试报警功能待实现");
    }
    
    /**
     * 应用报警配置
     */
    @FXML
    private void onApplyAlarmConfig() {
        // 验证输入
        if (!validateAlarmSettings()) {
            return;
        }
        
        System.out.println("应用报警配置");
        configStatusLabel.setText("配置状态: 正在应用报警配置...");
        showAlert(Alert.AlertType.INFORMATION, "功能提示", "应用报警配置功能待实现");
        configStatusLabel.setText("配置状态: 配置已应用");
    }
    
    /**
     * 重置报警配置
     */
    @FXML
    private void onResetAlarmConfig() {
        System.out.println("重置报警配置");
        soundAlarmEnabledCheckBox.setSelected(true);
        alarmDelayField.setText("5");
        alarmIntervalField.setText("60");
        showAlert(Alert.AlertType.INFORMATION, "提示", "报警配置已重置为默认值");
    }
    
    /**
     * 验证报警设置
     */
    private boolean validateAlarmSettings() {
        try {
            int delay = Integer.parseInt(alarmDelayField.getText().trim());
            if (delay < 0 || delay > 300) {
                showAlert(Alert.AlertType.WARNING, "提示", "报警延时必须在0-300秒之间");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入有效的报警延时");
            return false;
        }
        
        try {
            int interval = Integer.parseInt(alarmIntervalField.getText().trim());
            if (interval < 10 || interval > 3600) {
                showAlert(Alert.AlertType.WARNING, "提示", "报警间隔必须在10-3600秒之间");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入有效的报警间隔");
            return false;
        }
        
        return true;
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

