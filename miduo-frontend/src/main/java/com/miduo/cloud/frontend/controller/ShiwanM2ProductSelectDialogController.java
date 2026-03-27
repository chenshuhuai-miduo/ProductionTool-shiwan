package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 产品选择弹窗：
 * - 打开时检查本地产品表，无数据时自动从服务端拉取（首次/清空后）
 * - 支持刷新（主动从服务端更新本地产品表）
 * - 搜索在本地已落库数据上筛选
 * - 无可选数据时确认按钮禁用
 * - 加载/同步过程中显示遮罩，防止重复触发
 */
public class ShiwanM2ProductSelectDialogController implements Initializable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @FXML private TextField keywordField;
    @FXML private Button helpButton;
    @FXML private Button refreshButton;
    @FXML private TableView<Map<String, String>> productTable;
    @FXML private TableColumn<Map<String, String>, String> nameColumn;
    @FXML private TableColumn<Map<String, String>, String> pronumberColumn;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private TextField currentPageField;
    @FXML private Label totalPagesLabel;
    @FXML private Button confirmButton;
    @FXML private VBox loadingOverlay;

    private int currentPage = 1;
    private int totalCount = 0;
    private String keyword = "";

    /** 确认时选中的产品，取消为 null */
    private Map<String, String> selectedProduct;

    /** 防止 loading 期间重复触发刷新或翻页 */
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    // ──────────────── 初始化 ────────────────

    @Override
    @SuppressWarnings("deprecation")
    public void initialize(URL location, ResourceBundle resources) {
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        nameColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue() != null ? c.getValue().get("name") : ""));
        pronumberColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue() != null ? c.getValue().get("pronumber") : ""));

        installProductSelectTextCell(nameColumn);
        installProductSelectTextCell(pronumberColumn);

        productTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 初始确认按钮禁用，等数据加载完成后按实际数量决定
        confirmButton.setDisable(true);

        SvgIconLoader.installHelpButtonGraphic(helpButton);

        // 检查本地产品表，按规则决定是直接展示还是先从服务端拉取
        checkAndAutoLoad();
    }

    // ──────────────── 核心数据加载逻辑 ────────────────

    /**
     * 打开弹窗时：检查本地产品表是否有数据。
     * 有数据 → 直接从本地展示；
     * 无数据（首次/清空后）→ 自动向服务端拉取，成功后写库再展示。
     */
    private void checkAndAutoLoad() {
        setLoading(true);
        CompletableFuture.runAsync(() -> {
            try {
                String json = HttpUtil.doGet("/api/shiwan-m2/products/search-page?page=1&pageSize=1");
                JsonNode root = MAPPER.readTree(json);
                int code = root.has("code") ? root.get("code").asInt() : 500;
                long total = 0;
                if (code == 200 && root.has("data")) {
                    JsonNode data = root.get("data");
                    total = data.has("total") ? data.get("total").asLong() : 0;
                }
                final long totalFinal = total;
                Platform.runLater(() -> {
                    if (totalFinal > 0) {
                        // 本地有数据，直接读取展示
                        loadPageInternal(1);
                    } else {
                        // 本地无数据，自动触发服务端同步（首次打开 / 数据被清空后）
                        syncFromServer(true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showSyncError("读取本地数据失败：" + e.getMessage(), false);
                });
            }
        });
    }

    /**
     * 从服务端拉取产品数据并写入本地产品表，成功后刷新表格展示。
     * 拉取期间保持 loading 状态，完成后由 loadPageInternal 关闭。
     *
     * @param autoMode true = 系统自动触发（无数据时），false = 用户主动点击刷新
     */
    private void syncFromServer(boolean autoMode) {
        CompletableFuture.runAsync(() -> {
            try {
                // 产品同步可能需要多次外部请求，使用长超时避免中断
                String json = HttpUtil.doPostLong("/api/shiwan-m2/products/sync", "");
                JsonNode root = MAPPER.readTree(json);
                int code = root.has("code") ? root.get("code").asInt() : 500;
                if (code != 200) {
                    String msg = root.has("message") ? root.get("message").asText() : "同步失败，请检查网络或服务配置";
                    Platform.runLater(() -> {
                        setLoading(false);
                        showSyncError(msg, autoMode);
                    });
                    return;
                }
                // 同步成功，重新从本地数据库加载并展示（loadPageInternal 结束时关闭 loading）
                Platform.runLater(() -> loadPageInternal(1));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showSyncError("同步产品失败：" + e.getMessage(), autoMode);
                });
            }
        });
    }

    /**
     * 从本地产品表（已落库数据）分页读取并刷新表格。
     * 调用前需已通过 setLoading(true) 开启加载状态；
     * 本方法完成后自动调用 setLoading(false)。
     */
    private void loadPageInternal(int page) {
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
                    Platform.runLater(() -> {
                        updateTable(Collections.emptyList(), 0, 1);
                        setLoading(false);
                    });
                    return;
                }
                JsonNode data = root.get("data");
                long total = data.has("total") ? data.get("total").asLong() : 0;
                JsonNode listNode = data.has("list") ? data.get("list") : data;
                List<Map<String, String>> list = new ArrayList<>();
                if (listNode != null && listNode.isArray()) {
                    for (JsonNode item : listNode) {
                        Map<String, String> row = new HashMap<>();
                        row.put("name",      item.has("productName") ? item.get("productName").asText() : "");
                        row.put("pronumber", item.has("productNo")   ? item.get("productNo").asText()   : "");
                        list.add(row);
                    }
                }
                final long totalFinal = total;
                Platform.runLater(() -> {
                    updateTable(list, (int) totalFinal, currentPage);
                    setLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateTable(Collections.emptyList(), 0, 1);
                    setLoading(false);
                });
            }
        });
    }

    /** 更新表格数据、分页信息，并同步确认按钮的可用状态 */
    private void updateTable(List<Map<String, String>> list, int total, int page) {
        totalCount  = total;
        currentPage = Math.max(page, 1);
        productTable.getItems().clear();
        productTable.getItems().addAll(list);
        totalLabel.setText("共 " + total + " 条");
        int totalPages = total <= 0 ? 1 : (total + getPageSize() - 1) / getPageSize();
        currentPageField.setText(String.valueOf(currentPage));
        totalPagesLabel.setText("/ " + totalPages + " 页");
        // 有可选数据时才允许确认（loading 结束后由 setLoading(false) 联动更新）
    }

    // ──────────────── Loading 状态管理 ────────────────

    /**
     * 控制加载遮罩、操作区禁用状态和确认按钮。
     * 必须在 FX 线程调用。
     */
    private void setLoading(boolean on) {
        isLoading.set(on);
        loadingOverlay.setVisible(on);
        loadingOverlay.setManaged(on);
        refreshButton.setDisable(on);
        keywordField.setDisable(on);
        // loading 时确认必须禁用；loading 结束后按数据量决定
        confirmButton.setDisable(on || totalCount <= 0);
    }

    // ──────────────── 错误提示（含重试） ────────────────

    private void showSyncError(String message, boolean allowRetry) {
        ButtonType retryBtn  = new ButtonType("重试", ButtonBar.ButtonData.YES);
        ButtonType closeBtn  = new ButtonType("关闭", ButtonBar.ButtonData.NO);
        Alert alert = allowRetry
                ? new Alert(Alert.AlertType.ERROR, message, retryBtn, closeBtn)
                : new Alert(Alert.AlertType.ERROR, message, closeBtn);
        alert.setTitle("获取产品数据失败");
        alert.setHeaderText("产品数据拉取失败");
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == retryBtn) {
                setLoading(true);
                syncFromServer(allowRetry);
            }
        });
    }

    // ──────────────── FXML 事件处理 ────────────────

    @FXML
    private void onSearch() {
        if (isLoading.get()) return;
        keyword = keywordField.getText() != null ? keywordField.getText().trim() : "";
        loadPageInternal(1);
    }

    /** 用户主动刷新：从服务端重新拉取产品列表，更新本地数据库后刷新当前弹窗。 */
    @FXML
    private void onRefresh() {
        if (isLoading.get()) return;
        setLoading(true);
        syncFromServer(false);
    }

    /** 帮助说明（通用帮助弹窗，与数据上传等 Tab 一致） */
    @FXML
    private void onHelp() {
        var scene = keywordField != null ? keywordField.getScene() : productTable.getScene();
        if (scene == null) {
            return;
        }
        FxHelpDialog.show(
                scene.getWindow(),
                "选择产品 - 帮助说明",
                "- **首次打开**：软件会联网拉取产品数据并保存在本机；以后再打开一般直接使用已保存的数据，打开更快。",
                "- **刷新**：需要最新产品数据时请点击「刷新」；更新过程中界面会显示加载中，请稍候。",
                "- **搜索**：在当前已保存的数据中按关键字查找产品；搜索针对本机已落库数据，不含未保存的临时内容。",
                "- **选品限制**：列表无产品或拉取失败时无法完成选品；拉取失败请检查网络或稍后重试。"
        );
    }

    @FXML
    private void onPageSizeChanged() {
        if (!isLoading.get()) loadPageInternal(1);
    }

    @FXML
    private void onPageInput() {
        if (isLoading.get()) return;
        try {
            int page = Integer.parseInt(currentPageField.getText().trim());
            int totalPages = totalCount <= 0 ? 1 : (totalCount + getPageSize() - 1) / getPageSize();
            loadPageInternal(Math.max(1, Math.min(page, totalPages)));
        } catch (NumberFormatException ignored) {
            currentPageField.setText(String.valueOf(currentPage));
        }
    }

    @FXML
    private void onPrevPage() {
        if (!isLoading.get() && currentPage > 1) loadPageInternal(currentPage - 1);
    }

    @FXML
    private void onNextPage() {
        if (isLoading.get()) return;
        int totalPages = totalCount <= 0 ? 1 : (totalCount + getPageSize() - 1) / getPageSize();
        if (currentPage < totalPages) loadPageInternal(currentPage + 1);
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
            a.setHeaderText("请先在列表中选择一行产品");
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

    // ──────────────── 工具方法 ────────────────

    private int getPageSize() {
        String v = pageSizeCombo.getValue();
        if (v == null) return 20;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    /** 选中行：蓝字加粗；未选中：灰色。名称与编号列共用，保证两列同步变色。 */
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
}
