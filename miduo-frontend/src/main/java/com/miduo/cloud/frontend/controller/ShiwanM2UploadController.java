package com.miduo.cloud.frontend.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 数据上传 Tab 控制器
 * <p>
 * 左侧：实时上传日志 + 手动上传按钮。
 * 右侧：垛码状态查询、设为未上传/已上传管理。
 * </p>
 */
public class ShiwanM2UploadController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ==================== FXML 注入 ====================

    @FXML private ListView<UploadLogEntry> uploadLogList;

    @FXML private TextField palletCodeField;
    @FXML private Label     queryResultLabel;
    @FXML private VBox      resultDetailPane;
    @FXML private Label     resultPalletCode;
    @FXML private Label     resultStatus;
    @FXML private Label     resultUploadTime;
    @FXML private Label     resultBoxCount;
    @FXML private Label     resultFailReason;

    // ==================== 内部状态 ====================

    private final ObservableList<UploadLogEntry> logItems = FXCollections.observableArrayList();

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        uploadLogList.setItems(logItems);
        uploadLogList.setCellFactory(lv -> new UploadLogCell());

        // 加入示例历史日志
        addLog("14:35:20  垛码 P20241201005 70箱 上传成功", UploadColor.GREEN);
        addLog("14:35:18  垛码 P20241201005 70箱 上传中...", UploadColor.BLUE);
        addLog("14:35:15  垛码 P20241201005 70箱 开始上传", UploadColor.GRAY);
        addLog("14:30:22  垛码 P20241201004 70箱 上传失败，网络超时", UploadColor.RED);
        addLog("14:25:10  垛码 P20241201003 70箱 上传成功", UploadColor.GREEN);
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onManualUpload() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("手动上传");
        confirm.setHeaderText("确认批量上传所有未上传的垛数据？");
        confirm.setContentText("将按队列顺序依次上传，上传过程中请勿关闭软件。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String now = now();
        addLog(now + "  手动上传任务已启动，共 3 个待上传垛...", UploadColor.GRAY);

        // 模拟异步上传
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(0.5), e ->
                        addLog(now + "  垛码 P20241201006 70箱 开始上传", UploadColor.GRAY)),
                new javafx.animation.KeyFrame(Duration.seconds(1.5), e ->
                        addLog(now() + "  垛码 P20241201006 70箱 上传中...", UploadColor.BLUE)),
                new javafx.animation.KeyFrame(Duration.seconds(2.5), e ->
                        addLog(now() + "  垛码 P20241201006 70箱 上传成功", UploadColor.GREEN))
        );
        timeline.play();
    }

    @FXML
    private void onQueryStatus() {
        String code = palletCodeField.getText().trim();
        if (code.isEmpty()) {
            queryResultLabel.setText("请输入垛码并点击查询状态");
            queryResultLabel.setVisible(true);
            queryResultLabel.setManaged(true);
            resultDetailPane.setVisible(false);
            resultDetailPane.setManaged(false);
            return;
        }

        // 模拟查询
        boolean found = code.startsWith("P") && code.length() > 5;
        if (!found) {
            queryResultLabel.setText("未找到垛码：" + code);
            queryResultLabel.setStyle("-fx-text-fill:#DC2626; -fx-font-size:14px;");
            queryResultLabel.setVisible(true);
            queryResultLabel.setManaged(true);
            resultDetailPane.setVisible(false);
            resultDetailPane.setManaged(false);
            return;
        }

        queryResultLabel.setVisible(false);
        queryResultLabel.setManaged(false);
        resultDetailPane.setVisible(true);
        resultDetailPane.setManaged(true);

        resultPalletCode.setText(code);
        boolean success = !code.endsWith("4") && !code.endsWith("8");
        resultStatus.setText(success ? "成功" : "异常");
        resultStatus.getStyleClass().removeAll("sw2-badge-green", "sw2-badge-red");
        resultStatus.getStyleClass().add(success ? "sw2-badge-green" : "sw2-badge-red");
        resultUploadTime.setText("2024-12-01 " + (10 + (int)(Math.random() * 5)) + ":30:15");
        resultBoxCount.setText("70");
        resultFailReason.setText(success ? "-" : "网络超时");
    }

    @FXML
    private void onSetNotUploaded() {
        String code = palletCodeField.getText().trim();
        if (code.isEmpty()) { showWarn("请输入垛码", "请先输入要设置的垛码。"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("设为未上传");
        confirm.setHeaderText("确认将垛码「" + code + "」设为未上传？");
        confirm.setContentText("状态重置后，系统将在下次自动上传时重新上传此垛。");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            addLog(now() + "  垛码 " + code + " 已设为未上传（待补传）", UploadColor.GRAY);
            onQueryStatus();
        }
    }

    @FXML
    private void onSetUploaded() {
        String code = palletCodeField.getText().trim();
        if (code.isEmpty()) { showWarn("请输入垛码", "请先输入要设置的垛码。"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("设为已上传");
        confirm.setHeaderText("确认将垛码「" + code + "」手动标记为已上传？");
        confirm.setContentText("此操作将跳过该垛的自动上传，避免重复上传。");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            addLog(now() + "  垛码 " + code + " 已手动标记为已上传", UploadColor.GREEN);
            onQueryStatus();
        }
    }

    @FXML
    private void onHelp() {
        showInfo("手动上传说明",
                "手动上传：批量上传所有未上传的垛数据。\n\n" +
                "查询状态：输入垛码查看上传状态（成功/异常/待上传）及失败原因。\n\n" +
                "设为未上传：用于数据修复后重新补传，系统将在下次自动上传时处理。\n\n" +
                "设为已上传：手动标记已上传，避免系统重复上传此垛。");
    }

    @FXML
    private void onStatusHelp() {
        showInfo("状态管理说明",
                "查询状态：查询指定垛码的上传状态详情。\n" +
                "设为未上传：重置为待上传，用于数据修复后重新补传。\n" +
                "设为已上传：手动标记为已上传，避免系统重复上传此垛。");
    }

    // ==================== 上传日志管理 ====================

    /**
     * 向上传日志添加一条记录（外部服务可调用）
     *
     * @param message 日志内容
     * @param color   颜色类型
     */
    public void addLog(String message, UploadColor color) {
        Platform.runLater(() -> {
            if (logItems.size() >= 500) logItems.remove(logItems.size() - 1);
            logItems.add(0, new UploadLogEntry(message, color));
        });
    }

    // ==================== 工具 ====================

    private String now() {
        return LocalDateTime.now().format(TIME_FMT);
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarn(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==================== 枚举与数据模型 ====================

    public enum UploadColor { GRAY, BLUE, GREEN, RED }

    public static class UploadLogEntry {
        public final String      text;
        public final UploadColor color;

        public UploadLogEntry(String text, UploadColor color) {
            this.text  = text;
            this.color = color;
        }
    }

    // ==================== ListCell ====================

    static class UploadLogCell extends ListCell<UploadLogEntry> {
        private final Label label = new Label();

        UploadLogCell() {
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(UploadLogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }
            label.setText(item.text);
            label.getStyleClass().removeAll("sw2-ul-gray", "sw2-ul-blue", "sw2-ul-green", "sw2-ul-red");
            label.getStyleClass().add(switch (item.color) {
                case BLUE  -> "sw2-ul-blue";
                case GREEN -> "sw2-ul-green";
                case RED   -> "sw2-ul-red";
                default    -> "sw2-ul-gray";
            });
            setGraphic(label);
            setText(null);
        }
    }
}
