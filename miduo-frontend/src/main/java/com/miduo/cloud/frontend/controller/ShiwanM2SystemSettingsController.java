package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 系统设置弹窗控制器（2号机）
 * <p>
 * 对应 FXML：ShiwanM2SystemSettingsDialog.fxml
 * 需求文档：P03-02-系统设置.md
 * </p>
 */
public class ShiwanM2SystemSettingsController implements Initializable {

    // ==================== 标题栏 ====================
    @FXML private Button closeBtn;

    // ==================== 业务 - 码位数配置 ====================
    @FXML private TextField smallCodeDigitsField;
    @FXML private TextField mediumCodeDigitsField;
    @FXML private TextField largeCodeDigitsField;

    // ==================== 业务 - 虚拟垛标规则 ====================
    @FXML private TextField palletPrefixField;
    @FXML private TextField palletLineNumField;

    // ==================== 业务 - 上传配置 ====================
    @FXML private ToggleButton autoUploadToggle;

    // ==================== 业务 - 入库仓库设置 ====================
    @FXML private TextField warehouseNoField;

    // ==================== 页面配置开关 ====================
    @FXML private ToggleButton pageManualToggle;
    @FXML private ToggleButton pageQueryToggle;
    @FXML private ToggleButton pageReplaceToggle;
    @FXML private ToggleButton pageStatsToggle;
    @FXML private ToggleButton pagePackageToggle;
    @FXML private ToggleButton pageCancelToggle;
    @FXML private ToggleButton pageUploadToggle;

    // ==================== 设备 - IO设备管理 ====================
    @FXML private TableView<IoDeviceDTO> ioDeviceTable;
    @FXML private TableColumn<IoDeviceDTO, String> deviceNameCol;
    @FXML private TableColumn<IoDeviceDTO, String> deviceTypeCol;
    @FXML private TableColumn<IoDeviceDTO, String> deviceAddrCol;
    @FXML private TableColumn<IoDeviceDTO, String> devicePortCol;
    @FXML private TableColumn<IoDeviceDTO, String> deviceStatusCol;
    @FXML private TableColumn<IoDeviceDTO, String> deviceActionCol;

    // ==================== 设备 - 打印机管理 ====================
    @FXML private TextField printerNameField;
    @FXML private TextField printerIpField;
    @FXML private TextField printerPortField;
    @FXML private ComboBox<String> paperSizeCombo;

    // ==================== 设备 - 报警设置 ====================
    @FXML private ToggleButton soundAlarmToggle;
    @FXML private TextField alarmDelayField;
    @FXML private TextField alarmIntervalField;

    // ==================== 连接 - 数据库连接 ====================
    @FXML private TextField dbHostField;
    @FXML private TextField dbPortField;
    @FXML private TextField dbNameField;
    @FXML private TextField dbUserField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Label dbTestResultLabel;

    // ==================== 连接 - 1号机连接 ====================
    @FXML private TextField m1DbHostField;
    @FXML private TextField m1DbPortField;
    @FXML private TextField m1DbNameField;
    @FXML private TextField m1DbTableField;
    @FXML private TextField m1DbUserField;
    @FXML private PasswordField m1DbPasswordField;
    @FXML private Label m1DbTestResultLabel;

    // ==================== 连接 - 后端服务地址 ====================
    @FXML private TextField backendBaseUrlField;
    @FXML private Label backendBaseUrlResultLabel;

    // ==================== 数据模型 ====================
    private final ObservableList<IoDeviceDTO> ioDeviceList = FXCollections.observableArrayList();

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupToggleStyles();
        setupIoDeviceTable();
        loadSettingsIntoUI();
        loadIoDevices();
    }

    /** 从配置加载到界面 */
    private void loadSettingsIntoUI() {
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getCodeDigits() != null) {
            smallCodeDigitsField.setText(String.valueOf(s.getCodeDigits().getSmallCodeDigits()));
            mediumCodeDigitsField.setText(String.valueOf(s.getCodeDigits().getMediumCodeDigits()));
            largeCodeDigitsField.setText(String.valueOf(s.getCodeDigits().getLargeCodeDigits()));
        }
        if (s.getPalletRule() != null) {
            palletPrefixField.setText(s.getPalletRule().getPrefix() != null ? s.getPalletRule().getPrefix() : "V");
            palletLineNumField.setText(s.getPalletRule().getLineCode() != null ? s.getPalletRule().getLineCode() : "A");
        }
        if (s.getUpload() != null) {
            autoUploadToggle.setSelected(s.getUpload().isAutoUpload());
            syncToggleStyle(autoUploadToggle);
        }
        if (warehouseNoField != null) {
            String wno = s.getWarehouseNo();
            warehouseNoField.setText(wno != null && !wno.isEmpty() ? wno : "001");
        }
        if (s.getPageVisible() != null) {
            if (Boolean.TRUE.equals(s.getPageVisible().get("manual"))) pageManualToggle.setSelected(true); else pageManualToggle.setSelected(false);
            if (Boolean.TRUE.equals(s.getPageVisible().get("query"))) pageQueryToggle.setSelected(true); else pageQueryToggle.setSelected(false);
            if (Boolean.TRUE.equals(s.getPageVisible().get("replace"))) pageReplaceToggle.setSelected(true); else pageReplaceToggle.setSelected(false);
            if (Boolean.TRUE.equals(s.getPageVisible().get("stats"))) pageStatsToggle.setSelected(true); else pageStatsToggle.setSelected(false);
            if (Boolean.TRUE.equals(s.getPageVisible().get("package"))) pagePackageToggle.setSelected(true); else pagePackageToggle.setSelected(false);
            if (Boolean.TRUE.equals(s.getPageVisible().get("cancel"))) pageCancelToggle.setSelected(true); else pageCancelToggle.setSelected(false);
            if (Boolean.TRUE.equals(s.getPageVisible().get("upload"))) pageUploadToggle.setSelected(true); else pageUploadToggle.setSelected(false);
            for (ToggleButton tb : new ToggleButton[]{pageManualToggle, pageQueryToggle, pageReplaceToggle, pageStatsToggle, pagePackageToggle, pageCancelToggle, pageUploadToggle}) {
                if (tb != null) syncToggleStyle(tb);
            }
        }
        if (s.getDbConnection() != null) {
            dbHostField.setText(s.getDbConnection().getHost() != null ? s.getDbConnection().getHost() : "127.0.0.1");
            dbPortField.setText(s.getDbConnection().getPort() != null ? s.getDbConnection().getPort() : "3306");
            dbNameField.setText(s.getDbConnection().getDatabase() != null ? s.getDbConnection().getDatabase() : "");
            dbUserField.setText(s.getDbConnection().getUsername() != null ? s.getDbConnection().getUsername() : "");
            dbPasswordField.setText(s.getDbConnection().getPassword() != null ? s.getDbConnection().getPassword() : "");
        }
        if (s.getM1DbConnection() != null && m1DbHostField != null) {
            m1DbHostField.setText(s.getM1DbConnection().getHost() != null ? s.getM1DbConnection().getHost() : "192.168.1.100");
            m1DbPortField.setText(s.getM1DbConnection().getPort() != null ? s.getM1DbConnection().getPort() : "1433");
            m1DbNameField.setText(s.getM1DbConnection().getDatabase() != null ? s.getM1DbConnection().getDatabase() : "");
            m1DbTableField.setText(s.getM1DbConnection().getTableName() != null ? s.getM1DbConnection().getTableName() : "T_Code");
            m1DbUserField.setText(s.getM1DbConnection().getUsername() != null ? s.getM1DbConnection().getUsername() : "");
            m1DbPasswordField.setText(s.getM1DbConnection().getPassword() != null ? s.getM1DbConnection().getPassword() : "");
        }
        if (s.getApi() != null && backendBaseUrlField != null) {
            String url = s.getApi().getBackendBaseUrl();
            backendBaseUrlField.setText(url != null && !url.isEmpty() ? url : "http://localhost:8080");
        }
        if (s.getPrinter() != null) {
            printerNameField.setText(s.getPrinter().getPrinterName() != null ? s.getPrinter().getPrinterName() : "");
            printerIpField.setText(s.getPrinter().getPrinterIp() != null ? s.getPrinter().getPrinterIp() : "");
            printerPortField.setText(s.getPrinter().getPrinterPort() != null ? s.getPrinter().getPrinterPort() : "9100");
            if (paperSizeCombo != null && s.getPrinter().getPaperSize() != null) {
                paperSizeCombo.setValue(s.getPrinter().getPaperSize());
            }
        }
        if (s.getAlarm() != null) {
            soundAlarmToggle.setSelected(s.getAlarm().isSoundAlarmEnabled());
            alarmDelayField.setText(String.valueOf(s.getAlarm().getAlarmDelayMs()));
            alarmIntervalField.setText(String.valueOf(s.getAlarm().getAlarmIntervalMs()));
            syncToggleStyle(soundAlarmToggle);
        }
    }

    /** 初始化 ToggleButton 样式联动 */
    private void setupToggleStyles() {
        for (ToggleButton tb : new ToggleButton[]{
                autoUploadToggle, soundAlarmToggle,
                pageManualToggle, pageQueryToggle, pageReplaceToggle,
                pageStatsToggle, pagePackageToggle, pageCancelToggle, pageUploadToggle}) {
            if (tb != null) {
                syncToggleStyle(tb);
                tb.selectedProperty().addListener((obs, o, n) -> syncToggleStyle(tb));
            }
        }
    }

    /** 根据选中状态同步 ToggleButton 文字与颜色 */
    private void syncToggleStyle(ToggleButton tb) {
        if (tb.isSelected()) {
            tb.setText("开");
            tb.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;"
                + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';"
                + "-fx-background-radius: 6px; -fx-border-width: 0; -fx-cursor: hand;");
        } else {
            tb.setText("关");
            tb.setStyle("-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280;"
                + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';"
                + "-fx-background-radius: 6px; -fx-border-width: 0; -fx-cursor: hand;");
        }
    }

    /** 初始化 IO 设备列表 */
    private void setupIoDeviceTable() {
        deviceNameCol.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        deviceTypeCol.setCellValueFactory(new PropertyValueFactory<>("deviceCategory"));
        deviceAddrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        devicePortCol.setCellValueFactory(new PropertyValueFactory<>("port"));

        // 状态列：带颜色的徽标
        deviceStatusCol.setCellFactory(col -> new TableCell<IoDeviceDTO, String>() {
            private final Label badge = new Label();
            {
                setGraphic(badge);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    badge.setText("");
                } else {
                    badge.setText(item);
                    if ("已连接".equals(item)) {
                        badge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;"
                            + "-fx-background-radius: 20px; -fx-padding: 2px 10px;"
                            + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';");
                    } else {
                        badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;"
                            + "-fx-background-radius: 20px; -fx-padding: 2px 10px;"
                            + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';");
                    }
                }
            }
        });
        deviceStatusCol.setCellValueFactory(new PropertyValueFactory<>("statusText"));

        // 操作列：编辑/测试/删除按钮
        deviceActionCol.setCellFactory(col -> new TableCell<IoDeviceDTO, String>() {
            private final Button editBtn   = new Button("编辑");
            private final Button testBtn   = new Button("测试");
            private final Button deleteBtn = new Button("删除");
            private final HBox box = new HBox(6, editBtn, testBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                String editStyle   = "-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB;"
                    + "-fx-border-width: 0; -fx-background-radius: 4px; -fx-font-size: 12px;"
                    + "-fx-font-weight: bold; -fx-padding: 3px 10px; -fx-min-height: 28px;"
                    + "-fx-cursor: hand; -fx-font-family: 'Microsoft YaHei';";
                String testStyle   = "-fx-background-color: #F0FDF4; -fx-text-fill: #16A34A;"
                    + "-fx-border-width: 0; -fx-background-radius: 4px; -fx-font-size: 12px;"
                    + "-fx-font-weight: bold; -fx-padding: 3px 10px; -fx-min-height: 28px;"
                    + "-fx-cursor: hand; -fx-font-family: 'Microsoft YaHei';";
                String deleteStyle = "-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626;"
                    + "-fx-border-width: 0; -fx-background-radius: 4px; -fx-font-size: 12px;"
                    + "-fx-font-weight: bold; -fx-padding: 3px 10px; -fx-min-height: 28px;"
                    + "-fx-cursor: hand; -fx-font-family: 'Microsoft YaHei';";
                editBtn.setStyle(editStyle);
                testBtn.setStyle(testStyle);
                deleteBtn.setStyle(deleteStyle);

                editBtn.setOnAction(e   -> onEditDevice(getIndex()));
                testBtn.setOnAction(e   -> onTestDevice(getIndex()));
                deleteBtn.setOnAction(e -> onDeleteDevice(getIndex()));
                setGraphic(box);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        ioDeviceTable.setItems(ioDeviceList);
    }

    /**
     * 从后端加载 IO 设备列表
     */
    private void loadIoDevices() {
        new Thread(() -> {
            try {
                String responseJson = HttpUtil.doGet("/api/device/list");
                ApiResult<java.util.List<IoDeviceDTO>> result = HttpUtil.parseJson(
                    responseJson, new TypeReference<ApiResult<java.util.List<IoDeviceDTO>>>() {});
                Platform.runLater(() -> {
                    ioDeviceList.clear();
                    if (result != null && result.getCode() == 200 && result.getData() != null) {
                        for (IoDeviceDTO d : result.getData()) {
                            if (d.getStatusText() == null || d.getStatusText().trim().isEmpty()) {
                                d.setStatusText("未连接");
                            }
                        }
                        ioDeviceList.addAll(result.getData());
                    } else if (result != null) {
                        showError("加载失败", result.getMessage() != null ? result.getMessage() : "获取设备列表失败");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("加载失败", "获取设备列表异常：" + e.getMessage()));
            }
        }).start();
    }

    // ==================== 业务 Tab 事件处理 ====================

    @FXML
    private void onSaveCodeDigits() {
        String small  = smallCodeDigitsField.getText().trim();
        String medium = mediumCodeDigitsField.getText().trim();
        String large  = largeCodeDigitsField.getText().trim();
        if (!isValidDigitConfig(small) || !isValidDigitConfig(medium) || !isValidDigitConfig(large)) {
            showError("输入有误", "码位数请填写正整数或 -1（-1 表示不校验）。");
            return;
        }
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getCodeDigits() == null) s.setCodeDigits(new ShiwanM2Settings.CodeDigitsConfig());
        s.getCodeDigits().setSmallCodeDigits(Integer.parseInt(small));
        s.getCodeDigits().setMediumCodeDigits(Integer.parseInt(medium));
        s.getCodeDigits().setLargeCodeDigits(Integer.parseInt(large));
        saveSettings(s);
        showSuccess("码位数配置", "码位数配置已保存。\n小标：" + small + "位  中标：" + medium + "位  大标：" + large + "位");
    }

    @FXML
    private void onSavePalletRule() {
        String prefix  = palletPrefixField.getText().trim();
        String lineNum = palletLineNumField.getText().trim();
        if (prefix.isEmpty() || lineNum.isEmpty()) {
            showError("输入有误", "前缀和产线号不能为空。");
            return;
        }
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getPalletRule() == null) s.setPalletRule(new ShiwanM2Settings.PalletRuleConfig());
        s.getPalletRule().setPrefix(prefix);
        s.getPalletRule().setLineCode(lineNum);
        saveSettings(s);
        showSuccess("虚拟垛标规则", "虚拟垛标规则已保存。\n前缀：" + prefix + "  产线号：" + lineNum);
    }

    @FXML
    private void onAutoUploadToggled() {
        syncToggleStyle(autoUploadToggle);
    }

    @FXML
    private void onSaveUploadConfig() {
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getUpload() == null) s.setUpload(new ShiwanM2Settings.UploadConfig());
        s.getUpload().setAutoUpload(autoUploadToggle.isSelected());
        saveSettings(s);
        showSuccess("上传配置", "上传配置已保存。\n自动上传：" + (autoUploadToggle.isSelected() ? "开启" : "关闭"));
    }

    @FXML
    private void onSaveWarehouseNo() {
        if (warehouseNoField == null) return;
        String wno = warehouseNoField.getText().trim();
        if (wno.isEmpty()) {
            showError("输入有误", "仓库编号不能为空。");
            return;
        }
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        s.setWarehouseNo(wno);
        saveSettings(s);
        showSuccess("入库仓库设置", "仓库编号已保存：" + wno);
    }

    private void saveSettings(ShiwanM2Settings s) {
        try {
            ShiwanM2SettingsStore.save(s);
        } catch (IOException e) {
            showError("保存失败", "配置文件写入失败：" + e.getMessage());
        }
    }

    // ==================== 页面配置 Tab 事件处理 ====================

    @FXML
    private void onPageToggleChanged() {
        // 触发时同步所有页面开关的样式
        for (ToggleButton tb : new ToggleButton[]{
                pageManualToggle, pageQueryToggle, pageReplaceToggle,
                pageStatsToggle, pagePackageToggle, pageCancelToggle, pageUploadToggle}) {
            if (tb != null) syncToggleStyle(tb);
        }
    }

    @FXML
    private void onSavePageConfig() {
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        Map<String, Boolean> visible = new LinkedHashMap<>();
        visible.put("dataCollection", true);
        visible.put("manual", pageManualToggle.isSelected());
        visible.put("query", pageQueryToggle.isSelected());
        visible.put("replace", pageReplaceToggle.isSelected());
        visible.put("stats", pageStatsToggle.isSelected());
        visible.put("package", pagePackageToggle.isSelected());
        visible.put("cancel", pageCancelToggle.isSelected());
        visible.put("upload", pageUploadToggle.isSelected());
        s.setPageVisible(visible);
        java.util.List<String> order = new java.util.ArrayList<>();
        order.add("dataCollection");
        if (pageManualToggle.isSelected()) order.add("manual");
        if (pageQueryToggle.isSelected()) order.add("query");
        if (pageReplaceToggle.isSelected()) order.add("replace");
        if (pageStatsToggle.isSelected()) order.add("stats");
        if (pagePackageToggle.isSelected()) order.add("package");
        if (pageCancelToggle.isSelected()) order.add("cancel");
        if (pageUploadToggle.isSelected()) order.add("upload");
        s.setPageTabOrder(order);
        saveSettings(s);
        showSuccess("页面配置", "页面配置已保存至配置文件，重启后生效。");
    }

    // ==================== 设备 Tab 事件处理 ====================

    @FXML
    private void onAddIoDevice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/IoDeviceDialog.fxml"));
            Parent root = loader.load();
            IoDeviceDialogController dialogController = loader.getController();
            dialogController.setEditMode(false);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("添加IO设备");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ioDeviceTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            if (dialogController.isConfirmed()) {
                IoDeviceDTO device = new IoDeviceDTO();
                device.setDeviceName(dialogController.getDeviceName());
                device.setId(device.getDeviceName());
                device.setDeviceCategory(dialogController.getDeviceCategory());
                device.setConnectionType(dialogController.getConnectionType());
                device.setProtocolType(dialogController.getProtocolType());
                device.setAddress(dialogController.getIp());
                device.setPort(dialogController.getPort());
                device.setTimeout(Integer.parseInt(dialogController.getTimeout()));
                device.setRetryCount(Integer.parseInt(dialogController.getRetry()));
                device.setEnabled(dialogController.isEnabled());
                device.setDescription(dialogController.getDescription());

                new Thread(() -> {
                    try {
                        String resp = HttpUtil.doPost("/api/device/add", device);
                        ApiResult<String> result = HttpUtil.parseJson(resp, new TypeReference<ApiResult<String>>() {});
                        Platform.runLater(() -> {
                            if (result.getCode() == 200) {
                                showSuccess("添加设备", "IO设备添加成功");
                                loadIoDevices();
                            } else {
                                showError("添加失败", result.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("添加失败", "调用接口异常：" + e.getMessage()));
                    }
                }).start();
            }
        } catch (Exception e) {
            showError("打开失败", "无法打开设备对话框：" + e.getMessage());
        }
    }

    @FXML
    private void onTestAllDevices() {
        showInfo("测试所有设备", "已向所有设备发送测试请求（演示提示）。\n当前列表设备数：" + ioDeviceList.size());
    }

    private void onEditDevice(int index) {
        if (index < 0 || index >= ioDeviceList.size()) return;
        IoDeviceDTO current = ioDeviceList.get(index);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/IoDeviceDialog.fxml"));
            Parent root = loader.load();
            IoDeviceDialogController dialogController = loader.getController();
            dialogController.setEditMode(true);
            dialogController.fillDeviceData(
                current.getDeviceName(),
                current.getDeviceCategory(),
                current.getConnectionType(),
                current.getProtocolType(),
                current.getAddress(),
                current.getPort(),
                String.valueOf(current.getTimeout()),
                String.valueOf(current.getRetryCount()),
                current.getEnabled() != null ? current.getEnabled() : true,
                current.getDescription()
            );

            Stage dialogStage = new Stage();
            dialogStage.setTitle("编辑IO设备");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ioDeviceTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            if (dialogController.isConfirmed()) {
                IoDeviceDTO updated = new IoDeviceDTO();
                updated.setId(current.getId() != null ? current.getId() : current.getDeviceName());
                updated.setDeviceName(dialogController.getDeviceName());
                updated.setDeviceCategory(dialogController.getDeviceCategory());
                updated.setConnectionType(dialogController.getConnectionType());
                updated.setProtocolType(dialogController.getProtocolType());
                updated.setAddress(dialogController.getIp());
                updated.setPort(dialogController.getPort());
                updated.setTimeout(Integer.parseInt(dialogController.getTimeout()));
                updated.setRetryCount(Integer.parseInt(dialogController.getRetry()));
                updated.setEnabled(dialogController.isEnabled());
                updated.setDescription(dialogController.getDescription());

                new Thread(() -> {
                    try {
                        String resp = HttpUtil.doPut("/api/device/update", updated);
                        ApiResult<Boolean> result = HttpUtil.parseJson(resp, new TypeReference<ApiResult<Boolean>>() {});
                        Platform.runLater(() -> {
                            if (result.getCode() == 200) {
                                showSuccess("编辑设备", "IO设备更新成功");
                                loadIoDevices();
                            } else {
                                showError("更新失败", result.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("更新失败", "调用接口异常：" + e.getMessage()));
                    }
                }).start();
            }
        } catch (Exception e) {
            showError("打开失败", "无法打开设备对话框：" + e.getMessage());
        }
    }

    private void onTestDevice(int index) {
        if (index < 0 || index >= ioDeviceList.size()) return;
        IoDeviceDTO device = ioDeviceList.get(index);
        showInfo("连接测试", "正在测试设备：" + device.getDeviceName() + "\n地址：" + device.getAddress() + ":" + device.getPort()
            + "\n\n（演示提示，具体连接测试逻辑依赖设备接入模块）");
    }

    private void onDeleteDevice(int index) {
        if (index < 0 || index >= ioDeviceList.size()) return;
        IoDeviceDTO device = ioDeviceList.get(index);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("删除设备");
        confirm.setHeaderText("确认删除设备「" + device.getDeviceName() + "」？");
        confirm.setContentText("删除后不可恢复，需重新添加。");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                new Thread(() -> {
                    try {
                        String resp = HttpUtil.doDelete("/api/device/delete/" + (device.getId() != null ? device.getId() : device.getDeviceName()));
                        ApiResult<Boolean> result = HttpUtil.parseJson(resp, new TypeReference<ApiResult<Boolean>>() {});
                        Platform.runLater(() -> {
                            if (result.getCode() == 200) {
                                showSuccess("删除设备", "设备已删除");
                                loadIoDevices();
                            } else {
                                showError("删除失败", result.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("删除失败", "调用接口异常：" + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML
    private void onSavePrinterConfig() {
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getPrinter() == null) s.setPrinter(new ShiwanM2Settings.PrinterConfig());
        s.getPrinter().setPrinterName(printerNameField.getText().trim());
        s.getPrinter().setPrinterIp(printerIpField.getText().trim());
        s.getPrinter().setPrinterPort(printerPortField.getText().trim());
        if (paperSizeCombo != null) s.getPrinter().setPaperSize(paperSizeCombo.getValue());
        saveSettings(s);
        showSuccess("打印机配置", "打印机配置已保存。\n打印机：" + printerNameField.getText()
            + "\nIP：" + printerIpField.getText() + ":" + printerPortField.getText());
    }

    @FXML
    private void onSoundAlarmToggled() {
        syncToggleStyle(soundAlarmToggle);
    }

    @FXML
    private void onTestAlarm() {
        showInfo("测试报警", "测试报警已触发。\n\n（正式环境下将触发声光报警器进行测试）");
    }

    @FXML
    private void onSaveAlarmConfig() {
        if (!isValidNumber(alarmDelayField.getText()) || !isValidNumber(alarmIntervalField.getText())) {
            showError("输入有误", "报警延时和报警间隔必须为非负整数（毫秒）。");
            return;
        }
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getAlarm() == null) s.setAlarm(new ShiwanM2Settings.AlarmConfig());
        s.getAlarm().setSoundAlarmEnabled(soundAlarmToggle.isSelected());
        s.getAlarm().setAlarmDelayMs(Integer.parseInt(alarmDelayField.getText().trim()));
        s.getAlarm().setAlarmIntervalMs(Integer.parseInt(alarmIntervalField.getText().trim()));
        saveSettings(s);
        showSuccess("报警设置", "报警设置已保存。\n声音报警：" + (soundAlarmToggle.isSelected() ? "开启" : "关闭")
            + "\n延时：" + alarmDelayField.getText() + "ms  间隔：" + alarmIntervalField.getText() + "ms");
    }

    // ==================== 连接 Tab 事件处理 ====================

    @FXML
    private void onTestDbConnection() {
        String host = dbHostField.getText().trim();
        String port = dbPortField.getText().trim();
        String name = dbNameField.getText().trim();
        String user = dbUserField.getText().trim();
        String pwd  = dbPasswordField.getText();
        if (host.isEmpty() || port.isEmpty() || name.isEmpty() || user.isEmpty()) {
            dbTestResultLabel.setText("❌ 请填写完整的数据库连接信息");
            dbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            return;
        }
        dbTestResultLabel.setText("⏳ 正在测试...");
        dbTestResultLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("host", host);
            body.put("port", port);
            body.put("database", name);
            body.put("username", user);
            body.put("password", pwd != null ? pwd : "");
            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            String response = HttpUtil.doPost("/api/shiwan-m2/settings/test-db-connection", jsonBody);
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
            boolean ok = node.has("code") && node.get("code").asInt(500) == 200;
            if (ok) {
                dbTestResultLabel.setText("✅ 连接成功");
                dbTestResultLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            } else {
                String msg = node.has("message") ? node.get("message").asText() : "连接失败";
                dbTestResultLabel.setText("❌ " + (msg != null && !msg.isEmpty() ? msg : "连接失败"));
                dbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            }
        } catch (Exception e) {
            dbTestResultLabel.setText("❌ 请求失败：" + (e.getMessage() != null ? e.getMessage() : "请确认后端已启动并提供 /api/shiwan-m2/settings/test-db-connection 接口"));
            dbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-family: 'Microsoft YaHei';");
        }
    }

    @FXML
    private void onSaveDbConfig() {
        String host = dbHostField.getText().trim();
        String port = dbPortField.getText().trim();
        String name = dbNameField.getText().trim();
        String user = dbUserField.getText().trim();
        String pwd  = dbPasswordField.getText();
        if (host.isEmpty() || port.isEmpty() || name.isEmpty() || user.isEmpty()) {
            showError("输入有误", "数据库地址、端口、数据库名、用户名均为必填项。");
            return;
        }
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getDbConnection() == null) s.setDbConnection(new ShiwanM2Settings.DbConnectionConfig());
        s.getDbConnection().setHost(host);
        s.getDbConnection().setPort(port);
        s.getDbConnection().setDatabase(name);
        s.getDbConnection().setUsername(user);
        s.getDbConnection().setPassword(pwd != null ? pwd : "");
        saveSettings(s);
        showSuccess("数据库连接", "数据库连接配置已保存。\n连接地址：" + host + ":" + port + "/" + name);
    }

    @FXML
    private void onTestM1DbConnection() {
        if (m1DbHostField == null || m1DbTestResultLabel == null) return;
        String host = m1DbHostField.getText().trim();
        String port = m1DbPortField.getText().trim();
        String name = m1DbNameField.getText().trim();
        String user = m1DbUserField.getText().trim();
        String pwd  = m1DbPasswordField.getText();
        if (host.isEmpty() || port.isEmpty() || name.isEmpty() || user.isEmpty()) {
            m1DbTestResultLabel.setText("❌ 请填写完整的连接信息");
            m1DbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            return;
        }
        m1DbTestResultLabel.setText("⏳ 正在测试...");
        m1DbTestResultLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("host", host);
            body.put("port", port);
            body.put("database", name);
            body.put("username", user);
            body.put("password", pwd != null ? pwd : "");
            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            String response = HttpUtil.doPost("/api/shiwan-m2/settings/test-m1-db-connection", jsonBody);
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
            boolean ok = node.has("code") && node.get("code").asInt(500) == 200;
            if (ok) {
                m1DbTestResultLabel.setText("✅ 连接成功");
                m1DbTestResultLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            } else {
                String msg = node.has("message") ? node.get("message").asText() : "连接失败";
                m1DbTestResultLabel.setText("❌ " + (msg != null && !msg.isEmpty() ? msg : "连接失败"));
                m1DbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            }
        } catch (Exception e) {
            m1DbTestResultLabel.setText("❌ 请求失败：" + (e.getMessage() != null ? e.getMessage() : "请确认后端已启动"));
            m1DbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-family: 'Microsoft YaHei';");
        }
    }

    @FXML
    private void onSaveM1DbConfig() {
        if (m1DbHostField == null) return;
        String host = m1DbHostField.getText().trim();
        String port = m1DbPortField.getText().trim();
        String name = m1DbNameField.getText().trim();
        String tableName = m1DbTableField.getText().trim();
        String user = m1DbUserField.getText().trim();
        String pwd  = m1DbPasswordField.getText();
        if (host.isEmpty() || port.isEmpty() || name.isEmpty() || user.isEmpty()) {
            showError("输入有误", "数据库地址、端口、数据库名、用户名均为必填项。");
            return;
        }
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getM1DbConnection() == null) s.setM1DbConnection(new ShiwanM2Settings.M1DbConnectionConfig());
        s.getM1DbConnection().setHost(host);
        s.getM1DbConnection().setPort(port);
        s.getM1DbConnection().setDatabase(name);
        s.getM1DbConnection().setTableName(tableName != null && !tableName.isEmpty() ? tableName : "T_Code");
        s.getM1DbConnection().setUsername(user);
        s.getM1DbConnection().setPassword(pwd != null ? pwd : "");
        saveSettings(s);
        showSuccess("1号机连接", "1号机数据库连接配置已保存。\n连接地址：" + host + ":" + port + "/" + name + " 表：" + (tableName.isEmpty() ? "T_Code" : tableName));
    }

    @FXML
    private void onSaveBackendBaseUrl() {
        if (backendBaseUrlField == null) return;
        String url = backendBaseUrlField.getText().trim();
        if (url.isEmpty()) {
            if (backendBaseUrlResultLabel != null) {
                backendBaseUrlResultLabel.setText("❌ 请填写后端 API 地址");
                backendBaseUrlResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            }
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        url = url.replaceAll("/+$", "");
        ShiwanM2Settings s = ShiwanM2SettingsStore.get();
        if (s.getApi() == null) s.setApi(new ShiwanM2Settings.ApiConfig());
        s.getApi().setBackendBaseUrl(url);
        saveSettings(s);
        HttpUtil.setBaseUrl(url);
        if (backendBaseUrlResultLabel != null) {
            backendBaseUrlResultLabel.setText("✅ 已保存，当前请求将发往：" + url);
            backendBaseUrlResultLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
        }
    }

    // ==================== 关闭 ====================

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    // ==================== 工具方法 ====================

    private boolean isValidDigitConfig(String text) {
        try {
            int v = Integer.parseInt(text);
            return v == -1 || v > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidNumber(String text) {
        try {
            int v = Integer.parseInt(text.trim());
            return v >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
