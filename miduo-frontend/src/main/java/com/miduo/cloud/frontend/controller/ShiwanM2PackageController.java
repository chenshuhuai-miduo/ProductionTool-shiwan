package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.codepackage.CodePackageImportVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageOnlineImportResultVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackagePageQueryDTO;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 石湾 2 号机 - 码包管理控制器
 */
public class ShiwanM2PackageController implements Initializable {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicBoolean ONLINE_UPDATE_TRIGGERED = new AtomicBoolean(false);

    @FXML private TextField keywordField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> importTypeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> packageTypeCombo;

    @FXML private TableView<PackageRow> packageTable;
    @FXML private TableColumn<PackageRow, String> colType;
    @FXML private TableColumn<PackageRow, String> colName;
    @FXML private TableColumn<PackageRow, String> colImportTime;
    @FXML private TableColumn<PackageRow, String> colImportWay;
    @FXML private TableColumn<PackageRow, String> colCount;
    @FXML private TableColumn<PackageRow, String> colStatus;
    @FXML private TableColumn<PackageRow, String> colRemark;
    @FXML private TableColumn<PackageRow, Void> colAction;

    @FXML private Label totalLabel;
    @FXML private Label pageTotalLabel;
    @FXML private TextField pageField;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private Button firstPageBtn;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Button lastPageBtn;

    private final ObservableList<PackageRow> tableData = FXCollections.observableArrayList();
    private int currentPage = 1;
    private int pageSize = 20;
    private int totalPages = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        packageTable.setItems(tableData);
    }

    /** 首次切换到码包管理 Tab 时由主控制器调用，触发数据加载 */
    public void onFirstShow() {
        loadPage(1);
        triggerStartupOnlineUpdate();
    }

    private void setupTableColumns() {
        packageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().typeName));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().packageName));
        colImportTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importTimeText));
        colImportWay.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importSourceName));
        colCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().codeCount)));
        colRemark.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().remark));

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statusName));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(status);
                badge.getStyleClass().removeAll("sw2-badge-green", "sw2-badge-gray", "sw2-badge-orange");
                if ("正常".equals(status)) {
                    badge.getStyleClass().add("sw2-badge-green");
                } else if ("已删除".equals(status)) {
                    badge.getStyleClass().add("sw2-badge-gray");
                } else {
                    badge.getStyleClass().add("sw2-badge-orange");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("查看");
            private final Button deleteBtn = new Button("删除");
            private final HBox box = new HBox(6, viewBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                viewBtn.getStyleClass().add("sw2-btn-sm-blue");
                deleteBtn.getStyleClass().add("sw2-btn-sm-red");
                viewBtn.setOnAction(event -> {
                    PackageRow row = getCurrentRow();
                    if (row != null) {
                        onViewPackage(row);
                    }
                });
                deleteBtn.setOnAction(event -> {
                    PackageRow row = getCurrentRow();
                    if (row != null) {
                        onDeletePackage(row);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                PackageRow row = getCurrentRow();
                if (row != null) {
                    deleteBtn.setVisible(row.status != null && row.status == 1);
                }
                setGraphic(box);
            }
            private PackageRow getCurrentRow() {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) {
                    return null;
                }
                return getTableView().getItems().get(index);
            }
        });
    }

    @FXML
    private void onOnlineUpdate() {
        triggerOnlineUpdate(true);
    }

    @FXML
    private void onLocalImport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2PackageImportDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2PackageImportDialogController controller = loader.getController();
            controller.setOnImportSuccess(() -> loadPage(1));

            Stage stage = new Stage();
            stage.setTitle("码包导入");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(packageTable.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "打开失败", "无法打开导入窗口：" + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        loadPage(currentPage);
    }

    @FXML
    private void onReset() {
        keywordField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        importTypeCombo.setValue("全部");
        statusCombo.setValue("全部");
        packageTypeCombo.setValue("全部");
        pageSizeCombo.setValue("20");
        pageSize = 20;
        currentPage = 1;
        loadPage(currentPage);
    }

    @FXML
    private void onPageSizeChange() {
        pageSize = resolvePageSize(pageSizeCombo.getValue());
        currentPage = 1;
        loadPage(currentPage);
    }

    @FXML
    private void onFirstPage() {
        if (currentPage > 1) {
            currentPage = 1;
            loadPage(currentPage);
        }
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadPage(currentPage);
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage(currentPage);
        }
    }

    @FXML
    private void onLastPage() {
        if (currentPage < totalPages) {
            currentPage = totalPages;
            loadPage(currentPage);
        }
    }

    @FXML
    private void onGoPage() {
        try {
            int target = Integer.parseInt(pageField.getText().trim());
            if (target < 1) target = 1;
            if (target > totalPages) target = totalPages;
            if (target != currentPage) {
                currentPage = target;
                loadPage(currentPage);
            }
        } catch (NumberFormatException ignored) {
            pageField.setText(String.valueOf(currentPage));
        }
    }

    @FXML
    private void onHelp() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("码包导入说明");
        info.setHeaderText("码包管理操作帮助");
        info.setContentText(
                "在线更新：拉取盖外码小标和箱外码大标，按上次成功拉取时间增量更新。\n" +
                "本地导入：选择码包类型 -> 选择 TXT -> 输入密码 -> 后端解析入库并判重。\n" +
                "查看：查看码包内码值，支持搜索和分页。\n" +
                "删除：仅当码包内无已关联码时允许删除（物理删除导入记录和热表记录）。");
        ShiwanM2AlertUtil.applyStyle(info);
        info.showAndWait();
    }

    private void triggerStartupOnlineUpdate() {
        if (!ONLINE_UPDATE_TRIGGERED.compareAndSet(false, true)) {
            return;
        }
        triggerOnlineUpdate(false);
    }

    private void triggerOnlineUpdate(boolean showDialog) {
        new Thread(() -> {
            try {
                String responseJson = HttpUtil.doPost("/api/code-package/import/online", "");
                ApiResult<CodePackageOnlineImportResultVO> result = HttpUtil.parseJson(
                        responseJson,
                        new TypeReference<ApiResult<CodePackageOnlineImportResultVO>>() {});
                Platform.runLater(() -> {
                    if (result != null && result.getCode() == 200) {
                        if (showDialog) {
                            showAlert(Alert.AlertType.INFORMATION, "在线更新完成", buildOnlineResultText(result.getData()));
                        }
                        loadPage(currentPage);
                    } else if (showDialog) {
                        showAlert(Alert.AlertType.WARNING, "在线更新失败", result == null ? "未知错误" : result.getMessage());
                    }
                });
            } catch (Exception e) {
                if (showDialog) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "在线更新异常", e.getMessage()));
                }
            }
        }, "package-online-update").start();
    }

    private String buildOnlineResultText(CodePackageOnlineImportResultVO data) {
        if (data == null) {
            return "在线更新已执行，未返回明细。";
        }
        return "处理数量：" + data.getTotalProcessed() + "\n" +
                "成功：" + data.getSuccessCount() + "，失败：" + data.getFailedCount();
    }

    private void loadPage(int targetPage) {
        String keyword = trimToNull(keywordField.getText());
        Integer importSource = resolveImportSource(importTypeCombo.getValue());
        Integer status = resolveStatus(statusCombo.getValue());
        Integer packageType = resolvePackageType(packageTypeCombo.getValue());
        LocalDateTime startTime = startDatePicker.getValue() == null ? null
                : LocalDateTime.of(startDatePicker.getValue(), LocalTime.MIN);
        LocalDateTime endTime = endDatePicker.getValue() == null ? null
                : LocalDateTime.of(endDatePicker.getValue(), LocalTime.MAX);

        new Thread(() -> {
            try {
                CodePackagePageQueryDTO query = new CodePackagePageQueryDTO();
                query.setCurrent((long) targetPage);
                query.setSize((long) pageSize);
                query.setPageNum((long) targetPage);
                query.setPageSize((long) pageSize);
                query.setKeyword(keyword);
                query.setImportSource(importSource);
                query.setStatus(status);
                query.setPackageType(packageType);
                query.setStartTime(startTime);
                query.setEndTime(endTime);

                String responseJson = HttpUtil.doPost("/api/code-package/page", query);
                ApiResult<PageOutput<CodePackageImportVO>> result = HttpUtil.parseJson(
                        responseJson, new TypeReference<ApiResult<PageOutput<CodePackageImportVO>>>() {});

                Platform.runLater(() -> {
                    if (result == null || result.getCode() != 200 || result.getData() == null) {
                        tableData.clear();
                        totalLabel.setText("共 0 条");
                        pageField.setText("1");
                        pageTotalLabel.setText("/ 1 页");
                        firstPageBtn.setDisable(true);
                        prevPageBtn.setDisable(true);
                        nextPageBtn.setDisable(true);
                        lastPageBtn.setDisable(true);
                        if (result != null && result.getMessage() != null) {
                            showAlert(Alert.AlertType.WARNING, "查询失败", result.getMessage());
                        }
                        return;
                    }
                    PageOutput<CodePackageImportVO> pageOutput = result.getData();
                    List<PackageRow> rows = pageOutput.getRecords() == null ? FXCollections.observableArrayList()
                            : pageOutput.getRecords().stream().map(PackageRow::fromVO).collect(Collectors.toList());
                    tableData.setAll(rows);
                    currentPage = pageOutput.getCurrent() == null ? 1 : pageOutput.getCurrent().intValue();
                    totalPages = pageOutput.getPages() == null || pageOutput.getPages() <= 0 ? 1 : pageOutput.getPages().intValue();
                    totalLabel.setText("共 " + (pageOutput.getTotal() == null ? 0 : pageOutput.getTotal()) + " 条");
                    pageField.setText(String.valueOf(currentPage));
                    pageTotalLabel.setText("/ " + totalPages + " 页");
                    firstPageBtn.setDisable(currentPage <= 1);
                    prevPageBtn.setDisable(currentPage <= 1);
                    nextPageBtn.setDisable(currentPage >= totalPages);
                    lastPageBtn.setDisable(currentPage >= totalPages);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "加载失败", "加载码包列表失败：" + e.getMessage()));
            }
        }, "package-page-load").start();
    }

    private void onViewPackage(PackageRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2PackageViewCodesDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2PackageViewCodesDialogController controller = loader.getController();
            controller.setContext(row.id, row.packageName);

            Stage stage = new Stage();
            stage.setTitle("查看码包");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(packageTable.getScene().getWindow());
            stage.setMinWidth(720);
            stage.setMinHeight(520);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "打开失败", "无法打开查看窗口：" + e.getMessage());
        }
    }

    private void onDeletePackage(PackageRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("删除确认");
        confirm.setHeaderText("确认删除码包「" + row.packageName + "」？");
        confirm.setContentText("若该码包内存在已关联码，将被禁止删除。");
        ShiwanM2AlertUtil.applyStyle(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        new Thread(() -> {
            try {
                String responseJson = HttpUtil.doDelete("/api/code-package/" + row.id);
                ApiResult<String> apiResult = HttpUtil.parseJson(responseJson, new TypeReference<ApiResult<String>>() {});
                Platform.runLater(() -> {
                    if (apiResult != null && apiResult.getCode() == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "删除成功", "码包已删除。");
                        loadPage(currentPage);
                    } else {
                        showAlert(Alert.AlertType.WARNING, "删除失败",
                                apiResult == null ? "未知错误" : apiResult.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "删除异常", e.getMessage()));
            }
        }, "package-delete").start();
    }

    private int resolvePageSize(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    private Integer resolveImportSource(String text) {
        if ("在线".equals(text)) {
            return 1;
        }
        if ("本地".equals(text)) {
            return 2;
        }
        return null;
    }

    private Integer resolveStatus(String text) {
        if ("正常".equals(text)) {
            return 1;
        }
        if ("已删除".equals(text)) {
            return -1;
        }
        return null;
    }

    private Integer resolvePackageType(String text) {
        if ("盖外码小标".equals(text)) {
            return 1;
        }
        if ("盒外码中标".equals(text)) {
            return 2;
        }
        if ("箱外码大标".equals(text)) {
            return 3;
        }
        return null;
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    public static class PackageRow {
        private Long id;
        private Integer status;
        private String typeName;
        private String packageName;
        private String importTimeText;
        private String importSourceName;
        private Integer codeCount;
        private String statusName;
        private String remark;

        public static PackageRow fromVO(CodePackageImportVO vo) {
            PackageRow row = new PackageRow();
            row.id = vo.getId();
            row.status = vo.getStatus();
            row.typeName = safe(vo.getPackageTypeName());
            row.packageName = safe(vo.getPackageName());
            row.importTimeText = vo.getImportTime() == null ? "-" : DT_FMT.format(vo.getImportTime());
            row.importSourceName = safe(vo.getImportSourceName());
            row.codeCount = vo.getCodeCount() == null ? 0 : vo.getCodeCount();
            row.statusName = safe(vo.getStatusName());
            row.remark = safe(vo.getRemark());
            return row;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
