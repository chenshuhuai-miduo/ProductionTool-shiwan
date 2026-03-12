package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.frontend.util.HttpUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * 产品选择弹窗：搜索、分页、表格（产品名称、产品编号），确认后回传主界面。
 */
public class ShiwanM2ProductSelectDialogController implements Initializable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int PAGE_SIZE = 10;

    @FXML private TextField keywordField;
    @FXML private TableView<Map<String, String>> productTable;
    @FXML private TableColumn<Map<String, String>, String> nameColumn;
    @FXML private TableColumn<Map<String, String>, String> pronumberColumn;
    @FXML private Label totalLabel;
    @FXML private Label pageInfoLabel;

    private int currentPage = 1;
    private int totalCount = 0;
    private String keyword = "";
    /** 确认时选中的产品，取消为 null */
    private Map<String, String> selectedProduct;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue() != null ? c.getValue().get("name") : ""));
        pronumberColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue() != null ? c.getValue().get("pronumber") : ""));
        productTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        loadPage(1);
    }

    private void loadPage(int page) {
        // 前端弹窗改用已存在的搜索接口：/api/shiwan-m2/products/search
        // 搜索接口仅支持 keyword 与 size，返回 List，无分页信息
        currentPage = 1;
        String url = "/api/shiwan-m2/products/search?size=" + PAGE_SIZE;
        if (keyword != null && !keyword.isEmpty()) {
            try {
                url += "&keyword=" + java.net.URLEncoder.encode(keyword, "UTF-8");
            } catch (Exception ignored) {}
        }
        final String reqUrl = url;
        CompletableFuture.runAsync(() -> {
            try {
                String json = HttpUtil.doGet(reqUrl);
                JsonNode root = MAPPER.readTree(json);
                int code = root.has("code") ? root.get("code").asInt() : 500;
                if (code != 200 || !root.has("data")) {
                    Platform.runLater(() -> updateTable(java.util.Collections.emptyList(), 0));
                    return;
                }
                JsonNode data = root.get("data");
                java.util.List<Map<String, String>> list = new java.util.ArrayList<>();
                if (data.isArray()) {
                    for (JsonNode item : data) {
                        Map<String, String> row = new HashMap<>();
                        // 兼容后端 ProductInfoPO 字段：productName / productNo
                        row.put("name", item.has("productName") ? item.get("productName").asText() : "");
                        row.put("pronumber", item.has("productNo") ? item.get("productNo").asText() : "");
                        list.add(row);
                    }
                } else if (data.isObject()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("name", data.has("productName") ? data.get("productName").asText() : "");
                    row.put("pronumber", data.has("productNo") ? data.get("productNo").asText() : "");
                    list.add(row);
                }
                Platform.runLater(() -> updateTable(list, list.size()));
            } catch (Exception e) {
                Platform.runLater(() -> updateTable(java.util.Collections.emptyList(), 0));
            }
        });
    }

    private void updateTable(java.util.List<Map<String, String>> list, int total) {
        totalCount = total;
        productTable.getItems().clear();
        productTable.getItems().addAll(list);
        totalLabel.setText("共 " + total + " 条");
        // 搜索接口不分页，这里固定单页展示
        int totalPages = 1;
        pageInfoLabel.setText("第 " + currentPage + " / " + totalPages + " 页");
    }

    @FXML
    private void onSearch() {
        keyword = keywordField.getText() != null ? keywordField.getText().trim() : "";
        loadPage(1);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) loadPage(currentPage - 1);
    }

    @FXML
    private void onNextPage() {
        int totalPages = totalCount <= 0 ? 1 : (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;
        if (currentPage < totalPages) loadPage(currentPage + 1);
    }

    @FXML
    private void onCancel() {
        selectedProduct = null;
        closeStage();
    }

    @FXML
    private void onConfirm() {
        Map<String, String> sel = productTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("提示");
            a.setHeaderText("请先选择一行产品");
            a.showAndWait();
            return;
        }
        selectedProduct = sel;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) (productTable.getScene() != null ? productTable.getScene().getWindow() : null);
        if (stage != null) stage.close();
    }

    /** 获取选中的产品（确认时返回 name、pronumber；取消时返回 null） */
    public Map<String, String> getSelectedProduct() {
        return selectedProduct;
    }
}
