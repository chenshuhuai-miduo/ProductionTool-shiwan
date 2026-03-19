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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

    @FXML private TextField caseCodeField;
    @FXML private TableView<RejectRow> rejectTable;
    @FXML private TableColumn<RejectRow, String> seqCol;
    @FXML private TableColumn<RejectRow, String> bottleCol;
    @FXML private TableColumn<RejectRow, String> boxCol;
    @FXML private TableColumn<RejectRow, String> caseCol;
    @FXML private TableColumn<RejectRow, String> reasonCol;
    @FXML private Label totalLabel;
    @FXML private Label pageLabel;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        seqCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().seq));
        bottleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().bottleCode));
        boxCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().boxCode));
        caseCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().caseCode));
        reasonCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rejectReason));

        applyProblemHighlight(bottleCol);
        applyProblemHighlight(boxCol);
        applyProblemHighlight(caseCol);

        rejectTable.setItems(rows);
        rejectTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> showDetail(row));
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
    private void onClose() {
        ((Stage) rejectTable.getScene().getWindow()).close();
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
                long total = num(data.get("total"));
                pages = Math.max(1, (int) num(data.get("pages")));
                page = Math.max(1, Math.min(page, pages));
                totalLabel.setText("共 " + total + " 条");
                pageLabel.setText("第 " + page + " / " + pages + " 页");
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

    private void applyProblemHighlight(TableColumn<RejectRow, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                RejectRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null && item.equals(row.problemCode)) {
                    setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                } else {
                    setStyle("");
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
            return Integer.parseInt(value);
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
