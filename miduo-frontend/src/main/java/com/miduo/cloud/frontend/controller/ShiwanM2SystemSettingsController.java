package com.miduo.cloud.frontend.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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

import java.net.URL;
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

    // ==================== 页面配置开关 ====================
    @FXML private ToggleButton pageManualToggle;
    @FXML private ToggleButton pageQueryToggle;
    @FXML private ToggleButton pageReplaceToggle;
    @FXML private ToggleButton pageStatsToggle;
    @FXML private ToggleButton pagePackageToggle;
    @FXML private ToggleButton pageCancelToggle;
    @FXML private ToggleButton pageUploadToggle;

    // ==================== 设备 - IO设备管理 ====================
    @FXML private TableView<IoDeviceRow> ioDeviceTable;
    @FXML private TableColumn<IoDeviceRow, String> deviceNameCol;
    @FXML private TableColumn<IoDeviceRow, String> deviceTypeCol;
    @FXML private TableColumn<IoDeviceRow, String> deviceAddrCol;
    @FXML private TableColumn<IoDeviceRow, String> devicePortCol;
    @FXML private TableColumn<IoDeviceRow, String> deviceStatusCol;
    @FXML private TableColumn<IoDeviceRow, String> deviceActionCol;

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

    // ==================== 数据模型 ====================

    /** IO 设备行数据模型 */
    public static class IoDeviceRow {
        private final String name;
        private final String type;
        private final String address;
        private final String port;
        private final String status;

        public IoDeviceRow(String name, String type, String address, String port, String status) {
            this.name    = name;
            this.type    = type;
            this.address = address;
            this.port    = port;
            this.status  = status;
        }

        public String getName()    { return name; }
        public String getType()    { return type; }
        public String getAddress() { return address; }
        public String getPort()    { return port; }
        public String getStatus()  { return status; }
    }

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupToggleStyles();
        setupIoDeviceTable();
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

    /** 初始化 IO 设备列表示例数据 */
    private void setupIoDeviceTable() {
        deviceNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        deviceTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        deviceAddrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        devicePortCol.setCellValueFactory(new PropertyValueFactory<>("port"));

        // 状态列：带颜色的徽标
        deviceStatusCol.setCellFactory(col -> new TableCell<IoDeviceRow, String>() {
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
        deviceStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 操作列：编辑/测试/删除按钮
        deviceActionCol.setCellFactory(col -> new TableCell<IoDeviceRow, String>() {
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

        // 加载示例数据
        ObservableList<IoDeviceRow> data = FXCollections.observableArrayList(
            new IoDeviceRow("盒码相机",  "网络相机",   "192.168.1.104", "3000", "已连接"),
            new IoDeviceRow("箱码相机",  "网络相机",   "192.168.1.105", "3000", "已连接"),
            new IoDeviceRow("剔除装置",  "串口RS485",  "COM4",          "9600", "已连接"),
            new IoDeviceRow("报警灯",    "网络TCP/IP", "192.168.1.150", "5020", "已连接"),
            new IoDeviceRow("指示灯",    "网络TCP/IP", "192.168.1.151", "5020", "未连接")
        );
        ioDeviceTable.setItems(data);
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
        showSuccess("虚拟垛标规则", "虚拟垛标规则已保存。\n前缀：" + prefix + "  产线号：" + lineNum);
    }

    @FXML
    private void onAutoUploadToggled() {
        syncToggleStyle(autoUploadToggle);
    }

    @FXML
    private void onSaveUploadConfig() {
        showSuccess("上传配置", "上传配置已保存。\n自动上传：" + (autoUploadToggle.isSelected() ? "开启" : "关闭"));
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
        showSuccess("页面配置", "页面配置已保存至配置文件，重启后生效。");
    }

    // ==================== 设备 Tab 事件处理 ====================

    @FXML
    private void onAddIoDevice() {
        showInfo("添加设备", "添加设备功能请联系技术人员配置。\n\n（此功能依赖硬件设备管理模块，在正式部署时对接）");
    }

    @FXML
    private void onTestAllDevices() {
        showInfo("测试所有设备", "正在测试所有设备连接状态...\n\n测试完成后，状态列将自动更新。\n（功能依赖实际设备，当前为演示模式）");
    }

    private void onEditDevice(int index) {
        if (index < 0 || index >= ioDeviceTable.getItems().size()) return;
        IoDeviceRow row = ioDeviceTable.getItems().get(index);
        showInfo("编辑设备", "设备：" + row.getName() + "\n地址：" + row.getAddress() + ":" + row.getPort()
            + "\n\n（完整编辑界面将在后续版本中实现）");
    }

    private void onTestDevice(int index) {
        if (index < 0 || index >= ioDeviceTable.getItems().size()) return;
        IoDeviceRow row = ioDeviceTable.getItems().get(index);
        showInfo("连接测试", "正在测试设备：" + row.getName() + "\n地址：" + row.getAddress() + ":" + row.getPort()
            + "\n\n" + ("已连接".equals(row.getStatus()) ? "✅ 连接测试成功" : "❌ 连接测试失败：设备未响应"));
    }

    private void onDeleteDevice(int index) {
        if (index < 0 || index >= ioDeviceTable.getItems().size()) return;
        IoDeviceRow row = ioDeviceTable.getItems().get(index);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("删除设备");
        confirm.setHeaderText("确认删除设备「" + row.getName() + "」？");
        confirm.setContentText("删除后不可恢复，需重新添加。");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                ioDeviceTable.getItems().remove(index);
                showSuccess("删除设备", "设备「" + row.getName() + "」已删除。");
            }
        });
    }

    @FXML
    private void onSavePrinterConfig() {
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
        showSuccess("报警设置", "报警设置已保存。\n声音报警：" + (soundAlarmToggle.isSelected() ? "开启" : "关闭")
            + "\n延时：" + alarmDelayField.getText() + "ms  间隔：" + alarmIntervalField.getText() + "ms");
    }

    // ==================== 连接 Tab 事件处理 ====================

    @FXML
    private void onTestDbConnection() {
        String host = dbHostField.getText().trim();
        String port = dbPortField.getText().trim();
        String name = dbNameField.getText().trim();
        if (host.isEmpty() || port.isEmpty() || name.isEmpty()) {
            dbTestResultLabel.setText("❌ 请填写完整的数据库连接信息");
            dbTestResultLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
            return;
        }
        dbTestResultLabel.setText("⏳ 正在测试...");
        dbTestResultLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px; -fx-font-family: 'Microsoft YaHei';");
        showInfo("连接测试", "正在测试连接：jdbc:mysql://" + host + ":" + port + "/" + name
            + "\n\n（此功能依赖实际数据库服务，当前为演示模式）");
        dbTestResultLabel.setText("（请在实际部署环境中测试）");
        dbTestResultLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-family: 'Microsoft YaHei';");
    }

    @FXML
    private void onSaveDbConfig() {
        String host = dbHostField.getText().trim();
        String port = dbPortField.getText().trim();
        String name = dbNameField.getText().trim();
        String user = dbUserField.getText().trim();
        if (host.isEmpty() || port.isEmpty() || name.isEmpty() || user.isEmpty()) {
            showError("输入有误", "数据库地址、端口、数据库名、用户名均为必填项。");
            return;
        }
        showSuccess("数据库连接", "数据库连接配置已保存。\n连接地址：" + host + ":" + port + "/" + name);
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
