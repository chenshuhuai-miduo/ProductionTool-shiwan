package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ShiwanM2RejectRecordsController implements Initializable {

    @FXML private HBox titleBar;
    @FXML private TextField caseCodeField;
    @FXML private TableView<RejectRow> rejectTable;
    @FXML private TableColumn<RejectRow, String> seqCol;
    @FXML private TableColumn<RejectRow, String> bottleCol;
    @FXML private TableColumn<RejectRow, String> boxCol;
    @FXML private TableColumn<RejectRow, String> caseCol;
    @FXML private TableColumn<RejectRow, String> reasonCol;
    @FXML private Label totalLabel;
    @FXML private TextField pageInputField;
    @FXML private Label totalPagesLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    @FXML private VBox detailEmptyPane;
    @FXML private VBox detailPane;
    @FXML private Label detProblemCode;
    @FXML private Label detCaseCode;
    @FXML private Label detReason;
    @FXML private Label detTime;
    @FXML private Label detProductNo;
    @FXML private Label detProductName;
    @FXML private Label detOrderNo;

    private final ObservableList<RejectRow> rows = FXCollections.observableArrayList();
    private String startDate = "";
    private String endDate = "";
    private String orderNo = "";
    private int page = 1;
    private int pageSize = 20;
    private int pages = 1;
    private double dragOffsetX, dragOffsetY;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 拖拽支持
        titleBar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        // 列自适应，禁止水平滚动条
        rejectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // 行高随内容自动撑开
        rejectTable.setFixedCellSize(-1);

        // 序号列（居中，不换行，固定最小行高 44px）
        seqCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().seq));
        seqCol.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setFont(Font.font("Microsoft YaHei", 16));
                label.setStyle("-fx-font-family:'Microsoft YaHei'; -fx-font-size:16px; -fx-text-fill:#1F2937;");
                label.setAlignment(Pos.CENTER);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
            }
            @Override
            protected double computePrefHeight(double width) { return 44; }
            @Override
            protected void layoutChildren() {
                label.resizeRelocate(0, 0, getWidth(), getHeight());
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty || item == null ? null : item);
            }
        });

        // 码列（自动换行 + 问题码红色高亮）
        bottleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().bottleCode));
        boxCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().boxCode));
        caseCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().caseCode));
        applyWrapHighlightCellFactory(bottleCol);
        applyWrapHighlightCellFactory(boxCol);
        applyWrapHighlightCellFactory(caseCol);

        // 剔除原因列（自动换行，行高自适应）
        reasonCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rejectReason));
        reasonCol.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setFont(Font.font("Microsoft YaHei", 16));
                label.setStyle("-fx-font-family:'Microsoft YaHei'; -fx-font-size:16px; -fx-text-fill:#1F2937;");
                label.setAlignment(Pos.CENTER);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
            }
            @Override
            protected double computePrefHeight(double width) {
                String text = label.getText();
                if (text == null || text.isEmpty()) return 44;
                // width=-1 表示 JavaFX 还未分配列宽，用列的当前宽度兜底
                double w = width > 0 ? width : col.getWidth();
                double lw = Math.max(0, w - 16);
                return lw > 0 ? Math.max(44, label.prefHeight(lw) + 16) : 44;
            }
            @Override
            protected void layoutChildren() {
                double lw = Math.max(0, getWidth() - 16);
                label.resize(lw, label.prefHeight(lw));
                label.relocate(8, 8);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty || item == null ? null : item);
            }
        });

        rejectTable.setItems(rows);
        rejectTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> showDetail(row));

        pageSizeCombo.setValue("20条");
        pageSizeCombo.setOnAction(e -> {
            pageSize = parsePageSize(pageSizeCombo.getValue());
            page = 1;
            loadData();
        });
        showDetail(null);
    }

    public void setContext(String startDate, String endDate, String orderNo) {
        this.startDate = safe(startDate);
        this.endDate = safe(endDate);
        this.orderNo = safe(orderNo);
        this.page = 1;
        loadData();
    }

    @FXML
    private void onSearch() {
        page = 1;
        loadData();
    }

    @FXML
    private void onReset() {
        caseCodeField.clear();
        page = 1;
        loadData();
    }

    @FXML
    private void onPrevPage() {
        if (page > 1) {
            page--;
            loadData();
        }
    }

    @FXML
    private void onNextPage() {
        if (page < pages) {
            page++;
            loadData();
        }
    }

    @FXML
    private void onPageInput() {
        try {
            int p = Integer.parseInt(pageInputField.getText().trim());
            if (p >= 1 && p <= pages) {
                page = p;
                loadData();
            } else {
                pageInputField.setText(String.valueOf(page));
            }
        } catch (NumberFormatException ex) {
            pageInputField.setText(String.valueOf(page));
        }
    }

    @FXML
    private void onClose() {
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    private void loadData() {
        String url = "/api/shiwan-m2/stats/reject-records?startDate=" + enc(startDate)
                + "&endDate=" + enc(endDate)
                + "&orderNo=" + enc(orderNo)
                + "&caseCode=" + enc(caseCodeField.getText())
                + "&page=" + page
                + "&pageSize=" + pageSize;
        HttpUtil.asyncGet(url, json -> {
            try {
                ApiResult<Map<String, Object>> result = HttpUtil.parseJson(
                        json, new TypeReference<ApiResult<Map<String, Object>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    showWarn(result == null ? "查询失败" : result.getMessage());
                    return;
                }
                Map<String, Object> data = result.getData();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> records = (List<Map<String, Object>>) data.getOrDefault("records", new ArrayList<>());
                List<RejectRow> mapped = new ArrayList<>();
                for (int i = 0; i < records.size(); i++) {
                    mapped.add(RejectRow.from(i + 1 + (page - 1) * pageSize, records.get(i)));
                }
                rows.setAll(mapped);
                // 数据设置后列宽已确定，延迟一帧刷新让 VirtualFlow 用正确列宽重算行高
                Platform.runLater(() -> rejectTable.refresh());
                long total = num(data.get("total"));
                pages = Math.max(1, (int) num(data.get("pages")));
                page = Math.max(1, Math.min(page, pages));
                totalLabel.setText("共 " + total + " 条");
                pageInputField.setText(String.valueOf(page));
                totalPagesLabel.setText("/ " + pages + " 页");
                prevBtn.setDisable(page <= 1);
                nextBtn.setDisable(page >= pages);
                showDetail(null);
            } catch (Exception ex) {
                showWarn("解析失败：" + ex.getMessage());
            }
        }, ex -> showWarn("查询异常：" + ex.getMessage()));
    }

    private void showDetail(RejectRow row) {
        boolean empty = row == null;
        detailEmptyPane.setVisible(empty);
        detailEmptyPane.setManaged(empty);
        detailPane.setVisible(!empty);
        detailPane.setManaged(!empty);
        if (empty) return;

        detProblemCode.setText(safeDash(row.problemCode));
        detCaseCode.setText(safeDash(row.caseCode));
        detReason.setText(safeDash(row.rejectReason));
        detTime.setText(safeDash(row.rejectTime));
        detProductNo.setText(safeDash(row.productNo));
        detProductName.setText(safeDash(row.productName));
        detOrderNo.setText(safeDash(row.orderNo));
    }

    private void applyWrapHighlightCellFactory(TableColumn<RejectRow, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                // 显式设置字体，保证 prefHeight() 计算时字体度量正确
                label.setFont(Font.font("Microsoft YaHei", 16));
                label.setAlignment(Pos.CENTER);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
            }

            /** JavaFX 询问行高时，依据换行后的真实文字高度返回，不再截断 */
            @Override
            protected double computePrefHeight(double width) {
                String text = label.getText();
                if (text == null || text.isEmpty()) return 44;
                // width=-1 表示列宽还未确定，用列的当前宽度兜底
                double w = width > 0 ? width : col.getWidth();
                double lw = Math.max(0, w - 16);
                return lw > 0 ? Math.max(44, label.prefHeight(lw) + 16) : 44;
            }

            @Override
            protected void layoutChildren() {
                double lw = Math.max(0, getWidth() - 16);
                label.resize(lw, label.prefHeight(lw));
                label.relocate(8, 8);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    label.setText(null);
                    label.setStyle("");
                } else {
                    label.setText(item);
                    RejectRow row = getTableRow() == null ? null : getTableRow().getItem();
                    String font = "-fx-font-family:'Microsoft YaHei'; -fx-font-size:16px;";
                    // 仅按数据着色，与是否选中无关；选中行只靠 .sw2-wrap-table 的 CSS 背景高亮
                    if (row != null && !item.isEmpty() && item.equals(row.problemCode)) {
                        label.setStyle(font + " -fx-text-fill:#F44336; -fx-font-weight:bold;");
                    } else {
                        label.setStyle(font + " -fx-text-fill:#1F2937; -fx-font-weight:normal;");
                    }
                }
            }
        });
    }

    private long num(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private int parsePageSize(String value) {
        try {
            return Integer.parseInt(value == null ? "20" : value.replace("条", "").trim());
        } catch (Exception ex) {
            return 20;
        }
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private String safeDash(String text) {
        String v = safe(text);
        return v.isEmpty() ? "-" : v;
    }

    private String enc(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void showWarn(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    public static class RejectRow {
        private String seq = "";
        private String bottleCode = "";
        private String boxCode = "";
        private String caseCode = "";
        private String problemCode = "";
        private String rejectReason = "";
        private String rejectTime = "";
        private String productNo = "";
        private String productName = "";
        private String orderNo = "";

        static RejectRow from(int seqNo, Map<String, Object> map) {
            RejectRow row = new RejectRow();
            row.seq = String.valueOf(seqNo);
            row.bottleCode = str(map.get("BottleCode"), map.get("bottleCode"));
            row.boxCode = str(map.get("BoxCode"), map.get("boxCode"));
            row.caseCode = str(map.get("CaseCode"), map.get("caseCode"));
            row.problemCode = str(map.get("ProblemCode"), map.get("problemCode"));
            row.rejectReason = str(map.get("RejectReason"), map.get("rejectReason"));
            row.rejectTime = formatDateTime(str(map.get("RejectTime"), map.get("rejectTime")));
            row.productNo = str(map.get("ProductNo"), map.get("productNo"));
            row.productName = str(map.get("ProductName"), map.get("productName"));
            row.orderNo = str(map.get("OrderNo"), map.get("orderNo"));
            return row;
        }

        private static String str(Object primary, Object fallback) {
            Object value = primary != null ? primary : fallback;
            return value == null ? "" : String.valueOf(value);
        }

        private static String formatDateTime(String s) {
            return s == null || s.isEmpty() ? s : s.replace("T", " ");
        }
    }
}
