package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
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
import javafx.stage.Stage;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ShiwanM2PalletListController implements Initializable {

    @FXML private TextField palletCodeField;
    @FXML private TableView<PalletRow> palletTable;
    @FXML private TableColumn<PalletRow, String> palletCodeCol;
    @FXML private TableColumn<PalletRow, String> caseCountCol;
    @FXML private TableColumn<PalletRow, String> orderNoCol;
    @FXML private TableColumn<PalletRow, String> associateTimeCol;
    @FXML private Label totalLabel;
    @FXML private Label pageLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private final ObservableList<PalletRow> rows = FXCollections.observableArrayList();
    private String startDate = "";
    private String endDate = "";
    private String orderNo = "";
    private int page = 1;
    private int pageSize = 20;
    private int pages = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
        ((Stage) palletTable.getScene().getWindow()).close();
    }

    private void loadData() {
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
                pageLabel.setText("第 " + page + " / " + pages + " 页");
                prevBtn.setDisable(page <= 1);
                nextBtn.setDisable(page >= pages);
            } catch (Exception ex) {
                showWarn("解析失败：" + ex.getMessage());
            }
        }, ex -> showWarn("查询异常：" + ex.getMessage()));
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

    private String enc(String value) {
        String v = value == null ? "" : value.trim();
        return URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void showWarn(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
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
            row.associateTime = str(map.get("associateTime"));
            return row;
        }

        private static String str(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }
}
