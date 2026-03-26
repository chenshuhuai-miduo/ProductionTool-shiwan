package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
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

    @FXML private TextField keywordField;
    @FXML private TableView<Map<String, String>> productTable;
    @FXML private TableColumn<Map<String, String>, String> nameColumn;
    @FXML private TableColumn<Map<String, String>, String> pronumberColumn;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private TextField currentPageField;
    @FXML private Label totalPagesLabel;

    private int currentPage = 1;
    private int totalCount = 0;
    private String keyword = "";
    /** 确认时选中的产品，取消为 null */
    private Map<String, String> selectedProduct;

    private int getPageSize() {
        String v = pageSizeCombo.getValue();
        if (v == null) return 20;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void initialize(URL location, ResourceBundle resources) {
        // columnResizePolicy 不在 FXML 中设置（JavaFX 21 反射解析失败），改为代码设置
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        nameColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue() != null ? c.getValue().get("name") : ""));

        pronumberColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue() != null ? c.getValue().get("pronumber") : ""));

        // 列宽自动撑满表格，禁止横向滚动
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 产品名称、产品编号：选中行两列同为蓝字加粗；未选中为灰色（与 table-styles 选中态配合需监听 selectedProperty）
        installProductSelectTextCell(nameColumn);
        installProductSelectTextCell(pronumberColumn);

        productTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        loadPage(1);
    }

    /** 选中行：蓝字加粗；未选中：灰色。名称与编号列共用，避免只有编号列变色。 */
    private void installProductSelectTextCell(TableColumn<Map<String, String>, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            {
                tableRowProperty().addListener((obs, oldRow, newRow) -> {
                    if (newRow != null) {
                        newRow.selectedProperty().addListener((o, wasSelected, isNowSelected) -> applyStyle(isNowSelected));
                    }
                });
            }

            private void applyStyle(boolean selected) {
                if (getItem() != null && !isEmpty()) {
                    setStyle(selected
                            ? "-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-alignment: CENTER;"
                            : "-fx-text-fill: #6B7280; -fx-font-weight: normal; -fx-alignment: CENTER;");
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    boolean selected = getTableRow() != null && getTableRow().isSelected();
                    applyStyle(selected);
                }
            }
        });
    }

    private void loadPage(int page) {
        currentPage = Math.max(page, 1);
        StringBuilder url = new StringBuilder("/api/shiwan-m2/products/search-page?page=")
                .append(currentPage).append("&pageSize=").append(getPageSize());
        if (keyword != null && !keyword.isEmpty()) {
            try {
                url.append("&keyword=").append(java.net.URLEncoder.encode(keyword, "UTF-8"));
            } catch (Exception ignored) {}
        }
        final String reqUrl = url.toString();
        CompletableFuture.runAsync(() -> {
            try {
                String json = HttpUtil.doGet(reqUrl);
                JsonNode root = MAPPER.readTree(json);
                int code = root.has("code") ? root.get("code").asInt() : 500;
                if (code != 200 || !root.has("data")) {
                    Platform.runLater(() -> updateTable(java.util.Collections.emptyList(), 0, 0));
                    return;
                }
                JsonNode data = root.get("data");
                long total = data.has("total") ? data.get("total").asLong() : 0;
                JsonNode listNode = data.has("list") ? data.get("list") : data;
                java.util.List<Map<String, String>> list = new java.util.ArrayList<>();
                if (listNode != null && listNode.isArray()) {
                    for (JsonNode item : listNode) {
                        Map<String, String> row = new HashMap<>();
                        row.put("name",      item.has("productName") ? item.get("productName").asText() : "");
                        row.put("pronumber", item.has("productNo")   ? item.get("productNo").asText()   : "");
                        list.add(row);
                    }
                }
                final long totalFinal = total;
                Platform.runLater(() -> updateTable(list, (int) totalFinal, currentPage));
            } catch (Exception e) {
                Platform.runLater(() -> updateTable(java.util.Collections.emptyList(), 0, 1));
            }
        });
    }

    private void updateTable(java.util.List<Map<String, String>> list, int total, int page) {
        totalCount  = total;
        currentPage = Math.max(page, 1);
        productTable.getItems().clear();
        productTable.getItems().addAll(list);
        totalLabel.setText("共 " + total + " 条");
        int totalPages = total <= 0 ? 1 : (total + getPageSize() - 1) / getPageSize();
        currentPageField.setText(String.valueOf(currentPage));
        totalPagesLabel.setText("/ " + totalPages + " 页");
    }

    @FXML
    private void onSearch() {
        keyword = keywordField.getText() != null ? keywordField.getText().trim() : "";
        loadPage(1);
    }

    @FXML
    private void onPageSizeChanged() {
        loadPage(1);
    }

    @FXML
    private void onPageInput() {
        try {
            int page = Integer.parseInt(currentPageField.getText().trim());
            int totalPages = totalCount <= 0 ? 1 : (totalCount + getPageSize() - 1) / getPageSize();
            loadPage(Math.max(1, Math.min(page, totalPages)));
        } catch (NumberFormatException ignored) {
            currentPageField.setText(String.valueOf(currentPage));
        }
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) loadPage(currentPage - 1);
    }

    @FXML
    private void onNextPage() {
        int totalPages = totalCount <= 0 ? 1 : (totalCount + getPageSize() - 1) / getPageSize();
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
            ShiwanM2AlertUtil.applyStyle(a);
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
