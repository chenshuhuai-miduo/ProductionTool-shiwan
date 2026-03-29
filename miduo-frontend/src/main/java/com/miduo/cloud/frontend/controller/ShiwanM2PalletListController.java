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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ShiwanM2PalletListController implements Initializable {

    @FXML private javafx.scene.layout.HBox titleBar;
    @FXML private TextField palletCodeField;
    @FXML private TableView<PalletRow> palletTable;
    @FXML private TableColumn<PalletRow, String> palletCodeCol;
    @FXML private TableColumn<PalletRow, String> caseCountCol;
    @FXML private TableColumn<PalletRow, String> orderNoCol;
    @FXML private TableColumn<PalletRow, String> associateTimeCol;
    @FXML private Label totalLabel;
    @FXML private TextField pageInputField;
    @FXML private Label totalPagesLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private StackPane loadingOverlay;

    private double dragOffsetX, dragOffsetY;

    private final ObservableList<PalletRow> rows = FXCollections.observableArrayList();
    private String startDate = "";
    private String endDate = "";
    private String orderNo = "";
    private int page = 1;
    private int pageSize = 20;
    private int pages = 1;

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

        palletTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        palletCodeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().palletCode));
        caseCountCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().caseCount));
        orderNoCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().orderNo));
        associateTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().associateTime));
        palletTable.setItems(rows);
        pageSizeCombo.setOnAction(e -> {
            pageSize = parsePageSize(pageSizeCombo.getValue());
            page = 1;
            loadData();
        });
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

    public void setContext(String startDate, String endDate, String orderNo) {
        this.startDate = startDate == null ? "" : startDate;
        this.endDate = endDate == null ? "" : endDate;
        this.orderNo = orderNo == null ? "" : orderNo;
        page = 1;
        loadData();
    }

    @FXML
    private void onSearch() {
        page = 1;
        loadData();
    }

    @FXML
    private void onReset() {
        palletCodeField.clear();
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
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    private void loadData() {
        setLoading(true);
        String url = "/api/shiwan-m2/stats/pallet-list?startDate=" + enc(startDate)
                + "&endDate=" + enc(endDate)
                + "&orderNo=" + enc(orderNo)
                + "&palletCode=" + enc(palletCodeField.getText())
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
                List<PalletRow> mapped = new ArrayList<>();
                for (Map<String, Object> item : records) mapped.add(PalletRow.from(item));
                rows.setAll(mapped);
                long total = num(data.get("total"));
                pages = Math.max(1, (int) num(data.get("pages")));
                page = Math.max(1, Math.min(page, pages));
                totalLabel.setText("共 " + total + " 条");
                pageInputField.setText(String.valueOf(page));
                totalPagesLabel.setText("/ " + pages + " 页");
                prevBtn.setDisable(page <= 1);
                nextBtn.setDisable(page >= pages);
            } catch (Exception ex) {
                showWarn("解析失败：" + ex.getMessage());
            } finally {
                setLoading(false);
            }
        }, ex -> {
            setLoading(false);
            showWarn("查询异常：" + ex.getMessage());
        });
    }

    private void setLoading(boolean on) {
        if (loadingOverlay == null) {
            return;
        }
        loadingOverlay.setVisible(on);
        loadingOverlay.setManaged(on);
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

    private String enc(String value) {
        String v = value == null ? "" : value.trim();
        return URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void showWarn(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    public static class PalletRow {
        private String palletCode = "";
        private String caseCount = "";
        private String orderNo = "";
        private String associateTime = "";

        static PalletRow from(Map<String, Object> map) {
            PalletRow row = new PalletRow();
            row.palletCode = str(map.get("palletCode"));
            row.caseCount = str(map.get("caseCount"));
            row.orderNo = str(map.get("orderNo"));
            row.associateTime = formatDateTime(str(map.get("associateTime")));
            return row;
        }

        private static String str(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private static String formatDateTime(String s) {
            return s == null || s.isEmpty() ? s : s.replace("T", " ");
        }
    }
}
