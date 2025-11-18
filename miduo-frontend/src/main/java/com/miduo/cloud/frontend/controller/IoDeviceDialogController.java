package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.util.StringUtils;

/**
 * IO设备添加/编辑对话框控制器
 */
public class IoDeviceDialogController {

    @FXML private Label titleLabel;
    @FXML private TextField deviceNameField;
    @FXML private ComboBox<String> deviceCategoryComboBox;
    @FXML private RadioButton networkRadio;
    @FXML private RadioButton serialRadio;
    @FXML private Label protocolLabel;
    @FXML private HBox protocolBox;
    @FXML private RadioButton tcpRadio;
    @FXML private RadioButton udpRadio;
    @FXML private Label ipLabel;
    @FXML private TextField ipField;
    @FXML private Label portLabel;
    @FXML private TextField portField;
    @FXML private TextField timeoutField;
    @FXML private TextField retryField;
    @FXML private RadioButton enableRadio;
    @FXML private RadioButton disableRadio;
    @FXML private TextArea descriptionArea;

    private boolean confirmed = false;
    private boolean isEditMode = false;
    private ToggleGroup connectionGroup;
    private ToggleGroup protocolGroup;
    private ToggleGroup statusGroup;

    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        System.out.println("IO设备对话框初始化...");

        // 初始化设备类别下拉框
        deviceCategoryComboBox.getItems().addAll(
            "码校验",
            "箱码采集",
            "托盘码关联",
            "箱码关联",
            "报警器",
            "剔除设备",
            "扫码枪"
        );
        deviceCategoryComboBox.getSelectionModel().selectFirst();

        // 初始化连接方式单选组
        connectionGroup = new ToggleGroup();
        networkRadio.setToggleGroup(connectionGroup);
        serialRadio.setToggleGroup(connectionGroup);
        networkRadio.setSelected(true);

        // 初始化协议类型单选组
        protocolGroup = new ToggleGroup();
        tcpRadio.setToggleGroup(protocolGroup);
        udpRadio.setToggleGroup(protocolGroup);
        tcpRadio.setSelected(true);

        // 初始化状态单选组
        statusGroup = new ToggleGroup();
        enableRadio.setToggleGroup(statusGroup);
        disableRadio.setToggleGroup(statusGroup);
        enableRadio.setSelected(true);

        // 监听连接方式切换
        connectionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateConnectionFields();
        });

        // 设置默认值
        timeoutField.setText("5");
        retryField.setText("3");

        updateConnectionFields();
    }

    /**
     * 根据连接方式更新字段显示
     */
    private void updateConnectionFields() {
        boolean isNetwork = networkRadio.isSelected();
        
        // 协议类型仅在网口模式下显示
        protocolLabel.setVisible(isNetwork);
        protocolLabel.setManaged(isNetwork);
        protocolBox.setVisible(isNetwork);
        protocolBox.setManaged(isNetwork);
        
        if (isNetwork) {
            // 网口模式
            ipLabel.setText("IP地址：*");
            ipField.setPromptText("例如：192.168.1.101");
            ipField.setDisable(false);
            
            portLabel.setText("端口号：*");
            portField.setPromptText("例如：502（TCP）或 5000（UDP）");
            portField.setDisable(false);
        } else {
            // 串口模式
            ipLabel.setText("串口号：");
            ipField.setPromptText("例如：COM1");
            ipField.setDisable(false);
            
            portLabel.setText("波特率：");
            portField.setPromptText("例如：9600");
            portField.setDisable(false);
        }
    }

    /**
     * 设置为编辑模式
     */
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (editMode) {
            titleLabel.setText("编辑IO设备");
        } else {
            titleLabel.setText("添加IO设备");
        }
    }

    /**
     * 填充设备数据（编辑模式）
     */
    public void fillDeviceData(String deviceName, String deviceCategory,
                              String connectionType, String protocolType, String ip, String port, 
                              String timeout, String retry, boolean enabled, String description) {
        deviceNameField.setText(deviceName);
        
        // 设置设备类别
        if (StringUtils.hasText(deviceCategory)) {
            // 尝试选择对应的设备类别
            deviceCategoryComboBox.getSelectionModel().select(deviceCategory);
            // 如果选择失败（例如下拉框中没有该选项），尝试选择第一个
            if (deviceCategoryComboBox.getSelectionModel().getSelectedItem() == null) {
                deviceCategoryComboBox.getSelectionModel().selectFirst();
            }
        } else {
            // 如果设备类别为空，默认选择第一个（兼容旧数据）
            deviceCategoryComboBox.getSelectionModel().selectFirst();
        }
        
        // 设置连接方式
        if ("串口".equals(connectionType)) {
            serialRadio.setSelected(true);
        } else {
            networkRadio.setSelected(true);
        }
        
        // 设置协议类型（仅网口有效）
        if ("网口".equals(connectionType)) {
            if ("UDP".equals(protocolType)) {
                udpRadio.setSelected(true);
            } else {
                tcpRadio.setSelected(true);
            }
        }
        
        ipField.setText(ip);
        portField.setText(port);
        timeoutField.setText(timeout);
        retryField.setText(retry);
        
        // 设置状态
        if (enabled) {
            enableRadio.setSelected(true);
        } else {
            disableRadio.setSelected(true);
        }
        
        descriptionArea.setText(description);
    }

    /**
     * 保存按钮事件
     */
    @FXML
    private void onSave() {
        System.out.println("保存IO设备配置");

        // 验证必填字段
        if (!validateInput()) {
            return;
        }

        confirmed = true;
        closeDialog();
    }

    /**
     * 取消按钮事件
     */
    @FXML
    private void onCancel() {
        System.out.println("取消");
        confirmed = false;
        closeDialog();
    }

    /**
     * 验证输入
     */
    private boolean validateInput() {
        // 验证设备名称
        if (!StringUtils.hasText(deviceNameField.getText())) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入设备名称");
            return false;
        }

        // 验证IP/串口号
        if (!StringUtils.hasText(ipField.getText())) {
            String fieldName = networkRadio.isSelected() ? "IP地址" : "串口号";
            showAlert(Alert.AlertType.WARNING, "提示", "请输入" + fieldName);
            return false;
        }

        // 验证端口号/波特率
        if (!StringUtils.hasText(portField.getText())) {
            String fieldName = networkRadio.isSelected() ? "端口号" : "波特率";
            showAlert(Alert.AlertType.WARNING, "提示", "请输入" + fieldName);
            return false;
        }

        // 验证超时时间
        try {
            int timeout = Integer.parseInt(timeoutField.getText());
            if (timeout <= 0) {
                showAlert(Alert.AlertType.WARNING, "提示", "超时时间必须大于0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "超时时间必须是数字");
            return false;
        }

        // 验证重试次数
        try {
            int retry = Integer.parseInt(retryField.getText());
            if (retry < 0) {
                showAlert(Alert.AlertType.WARNING, "提示", "重试次数不能为负数");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "提示", "重试次数必须是数字");
            return false;
        }

        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) deviceNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    // Getter方法，用于获取用户输入的数据
    public String getDeviceName() {
        return deviceNameField.getText();
    }

    public String getDeviceCategory() {
        return deviceCategoryComboBox.getSelectionModel().getSelectedItem();
    }

    public String getConnectionType() {
        return networkRadio.isSelected() ? "网口" : "串口";
    }
    
    public String getProtocolType() {
        if (networkRadio.isSelected()) {
            return udpRadio.isSelected() ? "UDP" : "TCP";
        }
        return null; // 串口模式无协议类型
    }

    public String getIp() {
        return ipField.getText();
    }

    public String getPort() {
        return portField.getText();
    }

    public String getTimeout() {
        return timeoutField.getText();
    }

    public String getRetry() {
        return retryField.getText();
    }

    public boolean isEnabled() {
        return enableRadio.isSelected();
    }

    public String getDescription() {
        return descriptionArea.getText();
    }
}

