package com.miduo.cloud.frontend.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 生产统计 Tab 控制器
 * <p>
 * 左侧：生产统计查询（垛/箱/盒/剔除数），右侧：上传统计表格。
 * </p>
 */
public class ShiwanM2StatsController implements Initializable {

    // ==================== FXML 注入 ====================

    // 左侧
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private TextField  orderNoField;
    @FXML private Label      palletNum;
    @FXML private Label      caseNum;
    @FXML private Label      boxNum;
    @FXML private Label      rejectNum;
    @FXML private VBox       palletCard;
    @FXML private VBox       rejectCard;

    // 右侧
    @FXML private DatePicker   uploadStartDate;
    @FXML private DatePicker   uploadEndDate;
    @FXML private TextField    uploadOrderField;
    @FXML private ComboBox<String> uploadStatusCombo;
    @FXML private TableView<UploadRow>             uploadTable;
    @FXML private TableColumn<UploadRow, String>   upColPallet;
    @FXML private TableColumn<UploadRow, String>   upColCases;
    @FXML private TableColumn<UploadRow, String>   upColOrder;
    @FXML private TableColumn<UploadRow, String>   upColTime;
    @FXML private TableColumn<UploadRow, String>   upColStatus;
    @FXML private TableColumn<UploadRow, String>   upColReason;

    private final ObservableList<UploadRow> uploadData = FXCollections.observableArrayList();

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startDate.setValue(LocalDate.now().withDayOfMonth(1));
        endDate.setValue(LocalDate.now());
        uploadStartDate.setValue(LocalDate.now().withDayOfMonth(1));
        uploadEndDate.setValue(LocalDate.now());

        setupUploadTable();
        loadSampleUploadData();
    }

    private void setupUploadTable() {
        upColPallet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().palletCode));
        upColCases .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cases));
        upColOrder .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().orderNo));
        upColTime  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().uploadTime));
        upColReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason));

        // 状态列 - 带颜色徽标
        upColStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        upColStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.setAlignment(Pos.CENTER); }
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                badge.setText(status);
                badge.getStyleClass().removeAll("sw2-badge-green", "sw2-badge-red");
                badge.getStyleClass().add("成功".equals(status) ? "sw2-badge-green" : "sw2-badge-red");
                setGraphic(badge);
                setText(null);
            }
        });

        uploadTable.setItems(uploadData);
    }

    private void loadSampleUploadData() {
        uploadData.addAll(
            new UploadRow("P20241201001", "70", "PO202412018710", "2024-12-01 11:00:15", "成功", "-"),
            new UploadRow("P20241201002", "70", "PO202412018710", "2024-12-01 11:05:22", "成功", "-"),
            new UploadRow("P20241201003", "70", "PO202412018711", "2024-12-01 11:10:18", "异常", "盒码B302少2个瓶码"),
            new UploadRow("P20241201004", "70", "PO202412018710", "2024-12-01 11:15:33", "成功", "-"),
            new UploadRow("P20241201005", "70", "PO202412018712", "2024-12-01 11:20:47", "异常", "网络超时")
        );
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onQuery() {
        // 模拟查询结果
        palletNum.setText("100");
        caseNum.setText("7000");
        boxNum.setText("28000");
        rejectNum.setText("15");
    }

    @FXML
    private void onUploadQuery() {
        String statusFilter = uploadStatusCombo.getValue();
        if ("全部".equals(statusFilter)) {
            // 不过滤
        } else {
            // 实际实现时过滤
        }
        uploadTable.refresh();
    }

    @FXML
    private void onPalletCardClick(MouseEvent e) {
        showInfo("垛码列表",
                "模拟垛码列表（实际实现时弹出专用弹窗，支持垛码筛选和分页）：\n\n" +
                "P20241201001  70箱  PO202412018710  2024-12-01 10:30:00\n" +
                "P20241201002  70箱  PO202412018710  2024-12-01 10:35:00\n" +
                "...\n（共 100 垛）");
    }

    @FXML
    private void onRejectCardClick(MouseEvent e) {
        showInfo("剔除记录",
                "模拟剔除记录（实际实现时弹出专用弹窗，左侧表格+右侧详情）：\n\n" +
                "1. 箱码X001盒码B101瓶码V010001 - 重码  2024-12-01 09:15:00\n" +
                "2. 箱码X005盒码B201瓶码V020003 - 格式错误  2024-12-01 10:02:30\n" +
                "...\n（共 15 条）");
    }

    // ==================== 工具 ====================

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==================== 数据模型 ====================

    public static class UploadRow {
        public final String palletCode;
        public final String cases;
        public final String orderNo;
        public final String uploadTime;
        public final String status;
        public final String reason;

        public UploadRow(String palletCode, String cases, String orderNo,
                         String uploadTime, String status, String reason) {
            this.palletCode = palletCode;
            this.cases      = cases;
            this.orderNo    = orderNo;
            this.uploadTime = uploadTime;
            this.status     = status;
            this.reason     = reason;
        }
    }
}
