package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.codepackage.CodePackageViewCodeVO;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 码包查看弹窗控制器
 */
public class ShiwanM2PackageViewCodesDialogController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 当前打开的查看码包弹窗实例，供 {@link ShiwanM2MainController} 扫码路由使用（模态关闭时置空）。 */
    private static volatile ShiwanM2PackageViewCodesDialogController activeInstance;

    public static ShiwanM2PackageViewCodesDialogController getActiveInstance() {
        return activeInstance;
    }

    @FXML private javafx.scene.layout.HBox titleBar;
    @FXML private Label titleLabel;
    @FXML private Label packageTypeLabel;
    @FXML private Label packageNameLabel;
    @FXML private TextField keywordField;
    @FXML private TableView<ViewCodeRow> codeTable;
    @FXML private TableColumn<ViewCodeRow, String> codeValueColumn;
    @FXML private TableColumn<ViewCodeRow, String> associatedStatusColumn;
    @FXML private TableColumn<ViewCodeRow, String> associatedTimeColumn;
    @FXML private Label totalLabel;
    @FXML private TextField pageInputField;
    @FXML private Label totalPagesLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private StackPane loadingOverlay;

    private final ObservableList<ViewCodeRow> rows = FXCollections.observableArrayList();

    private Long importId;
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageSize = 20;

    @FXML
    public void initialize() {
        codeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        codeValueColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().codeValue));
        associatedTimeColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().associatedTime));

        // 关联状态列着色：已关联绿色，未关联灰色
        associatedStatusColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().associatedStatus));
        associatedStatusColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("已关联".equals(item)
                            ? "-fx-text-fill:#16A34A; -fx-font-weight:bold; -fx-alignment:CENTER;"
                            : "-fx-text-fill:#9CA3AF; -fx-alignment:CENTER;");
                }
            }
        });

        codeTable.setItems(rows);
        pageSizeCombo.setValue("20");
        pageSizeCombo.setOnAction(event -> {
            pageSize = resolvePageSize(pageSizeCombo.getValue());
            currentPage = 1;
            loadData();
        });
    }

    @FXML
    private void onPageInput() {
        try {
            int p = Integer.parseInt(pageInputField.getText().trim());
            if (p >= 1 && p <= totalPages) {
                currentPage = p;
                loadData();
            } else {
                pageInputField.setText(String.valueOf(currentPage));
            }
        } catch (NumberFormatException ex) {
            pageInputField.setText(String.valueOf(currentPage));
        }
    }

    public void setContext(Long importId, String packageName,
                           String typeName, Integer codeCount, String statusName, boolean statusNormal) {
        activeInstance = this;
        this.importId = importId;
        packageTypeLabel.setText(typeName == null ? "—" : typeName);
        packageNameLabel.setText(packageName == null ? "—" : packageName);
        currentPage = 1;
        loadData();
    }

    /**
     * 扫码枪：写入搜索关键字并触发查询（与数据查询页行为一致）。
     */
    public void onScanCode(String code) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            Stage st = getStage();
            if (st == null || !st.isShowing()) {
                return;
            }
            if (keywordField == null) {
                return;
            }
            keywordField.setText(c);
            currentPage = 1;
            loadData();
        });
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        loadData();
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadData();
        }
    }

    /** 弹窗任意关闭路径下解除扫码路由，避免指向已销毁窗口。 */
    public void clearActiveScannerRouting() {
        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    @FXML
    private void onClose() {
        clearActiveScannerRouting();
        getStage().close();
    }

    private void loadData() {
        if (importId == null) {
            return;
        }
        setLoading(true);
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder("/api/code-package/")
                        .append(importId)
                        .append("/codes?pageNum=").append(currentPage)
                        .append("&pageSize=").append(pageSize);
                if (!keyword.isEmpty()) {
                    urlBuilder.append("&keyword=")
                            .append(URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString()));
                }
                String responseJson = HttpUtil.doGet(urlBuilder.toString());
                ApiResult<PageOutput<CodePackageViewCodeVO>> result = HttpUtil.parseJson(
                        responseJson, new TypeReference<ApiResult<PageOutput<CodePackageViewCodeVO>>>() {});

                Platform.runLater(() -> {
                    try {
                        if (result == null || result.getCode() != 200 || result.getData() == null) {
                            rows.clear();
                            totalLabel.setText("共 0 条");
                            pageInputField.setText("1");
                            totalPagesLabel.setText("/ 1 页");
                            if (result != null && result.getMessage() != null) {
                                showAlert("查询失败", result.getMessage());
                            }
                            return;
                        }
                        PageOutput<CodePackageViewCodeVO> pageOutput = result.getData();
                        rows.setAll(pageOutput.getRecords() == null ? FXCollections.observableArrayList()
                                : pageOutput.getRecords().stream().map(ViewCodeRow::fromVO).collect(Collectors.toList()));
                        currentPage = pageOutput.getCurrent() == null ? 1 : pageOutput.getCurrent().intValue();
                        totalPages = pageOutput.getPages() == null || pageOutput.getPages() <= 0 ? 1 : pageOutput.getPages().intValue();
                        totalLabel.setText("共 " + (pageOutput.getTotal() == null ? 0 : pageOutput.getTotal()) + " 条");
                        pageInputField.setText(String.valueOf(currentPage));
                        totalPagesLabel.setText("/ " + totalPages + " 页");
                    } finally {
                        setLoading(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    try {
                        showAlert("查询异常", e.getMessage());
                    } finally {
                        setLoading(false);
                    }
                });
            }
        }, "package-view-codes").start();
    }

    private void setLoading(boolean on) {
        if (loadingOverlay == null) {
            return;
        }
        loadingOverlay.setVisible(on);
        loadingOverlay.setManaged(on);
    }

    private int resolvePageSize(String value) {
        try {
            return Integer.parseInt(value == null ? "20" : value.trim());
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    private Stage getStage() {
        return (Stage) titleBar.getScene().getWindow();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    public static class ViewCodeRow {
        private String codeValue;
        private String associatedStatus;
        private String associatedTime;

        public static ViewCodeRow fromVO(CodePackageViewCodeVO vo) {
            ViewCodeRow row = new ViewCodeRow();
            row.codeValue = vo.getCodeValue() == null ? "" : vo.getCodeValue();
            row.associatedStatus = Boolean.TRUE.equals(vo.getAssociated()) ? "已关联" : "未关联";
            row.associatedTime = vo.getAssociatedAt() == null ? "-" : DT_FMT.format(vo.getAssociatedAt());
            return row;
        }
    }
}
