package com.miduo.cloud.frontend.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import java.util.stream.Collectors;

/**
 * 生产统计 Tab 控制器
 * 左侧：生产统计查询（垛/箱/盒/剔除数），右侧：上传统计表格（支持分页）。
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

    // 右侧 - 筛选
    @FXML private DatePicker       uploadStartDate;
    @FXML private DatePicker       uploadEndDate;
    @FXML private TextField        uploadOrderField;
    @FXML private ComboBox<String> uploadStatusCombo;

    // 右侧 - 表格
    @FXML private TableView<UploadRow>           uploadTable;
    @FXML private TableColumn<UploadRow, String> upColPallet;
    @FXML private TableColumn<UploadRow, String> upColCases;
    @FXML private TableColumn<UploadRow, String> upColOrder;
    @FXML private TableColumn<UploadRow, String> upColTime;
    @FXML private TableColumn<UploadRow, String> upColStatus;
    @FXML private TableColumn<UploadRow, String> upColReason;

    // 右侧 - 分页控件
    @FXML private Label              upTotalLabel;
    @FXML private ComboBox<String>   upPageSizeCombo;
    @FXML private Button             upFirstBtn;
    @FXML private Button             upPrevBtn;
    @FXML private Button             upNextBtn;
    @FXML private Button             upLastBtn;
    @FXML private TextField          upPageField;
    @FXML private Label              upPageTotalLabel;

    // ==================== 状态 ====================

    private final ObservableList<UploadRow> pageData = FXCollections.observableArrayList();
    private List<UploadRow> allUploadData     = new ArrayList<>();
    private List<UploadRow> filteredData      = new ArrayList<>();
    private int currentPage = 1;
    private int pageSize    = 20;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startDate.setValue(LocalDate.now().withDayOfMonth(1));
        endDate.setValue(LocalDate.now());
        uploadStartDate.setValue(LocalDate.now().withDayOfMonth(1));
        uploadEndDate.setValue(LocalDate.now());

        setupColumns();
        buildSampleData();
        filteredData = new ArrayList<>(allUploadData);
        refreshTable();
    }

    private void setupColumns() {
        upColPallet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().palletCode));
        upColCases .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cases));
        upColOrder .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().orderNo));
        upColTime  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().uploadTime));
        upColReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason));

        // 状态列 - 带颜色的纯文本，无徽标样式
        upColStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        upColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                setAlignment(Pos.CENTER);
                if ("成功".equals(status)) {
                    setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });

        uploadTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        uploadTable.setItems(pageData);
    }

    // ==================== 分页逻辑 ====================

    private void refreshTable() {
        int total      = filteredData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage    = Math.min(currentPage, totalPages);

        int from = (currentPage - 1) * pageSize;
        int to   = Math.min(from + pageSize, total);

        pageData.setAll(filteredData.subList(from, to));

        upTotalLabel.setText("共 " + total + " 条");
        upPageField.setText(String.valueOf(currentPage));
        upPageTotalLabel.setText("/ " + totalPages + " 页");

        upFirstBtn.setDisable(currentPage <= 1);
        upPrevBtn .setDisable(currentPage <= 1);
        upNextBtn .setDisable(currentPage >= totalPages);
        upLastBtn .setDisable(currentPage >= totalPages);
    }

    @FXML
    private void onUploadPageSizeChange() {
        String v = upPageSizeCombo.getValue();
        if (v != null) {
            try { pageSize = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
        }
        currentPage = 1;
        refreshTable();
    }

    @FXML private void onUploadFirstPage() { currentPage = 1; refreshTable(); }

    @FXML private void onUploadPrevPage() {
        if (currentPage > 1) { currentPage--; refreshTable(); }
    }

    @FXML private void onUploadNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / pageSize));
        if (currentPage < totalPages) { currentPage++; refreshTable(); }
    }

    @FXML private void onUploadLastPage() {
        currentPage = Math.max(1, (int) Math.ceil((double) filteredData.size() / pageSize));
        refreshTable();
    }

    @FXML private void onUploadGoPage() {
        try {
            int page       = Integer.parseInt(upPageField.getText().trim());
            int totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / pageSize));
            currentPage    = Math.max(1, Math.min(page, totalPages));
        } catch (NumberFormatException e) {
            // 输入非法时恢复
        }
        refreshTable();
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onQuery() {
        palletNum.setText("100");
        caseNum.setText("7000");
        boxNum.setText("28000");
        rejectNum.setText("15");
    }

    @FXML
    private void onUploadQuery() {
        String statusFilter = uploadStatusCombo.getValue();
        String orderFilter  = uploadOrderField.getText() == null ? "" : uploadOrderField.getText().trim();

        filteredData = allUploadData.stream()
            .filter(r -> "全部".equals(statusFilter) || statusFilter == null || r.status.equals(statusFilter))
            .filter(r -> orderFilter.isEmpty() || r.orderNo.contains(orderFilter))
            .collect(Collectors.toList());

        currentPage = 1;
        refreshTable();
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

    // ==================== 模拟数据 ====================

    private void buildSampleData() {
        String[] statuses = {"成功", "成功", "异常", "成功", "异常"};
        String[] reasons  = {"-", "-", "盒码B302少2个瓶码", "-", "网络超时"};
        for (int i = 1; i <= 35; i++) {
            int si = (i - 1) % 5;
            allUploadData.add(new UploadRow(
                String.format("P2024120%04d", i),
                "70",
                "PO20241201" + (8710 + (i % 5)),
                String.format("2024-12-01 %02d:%02d:%02d", 11 + i / 60, i % 60, i * 7 % 60),
                statuses[si],
                reasons[si]
            ));
        }
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
