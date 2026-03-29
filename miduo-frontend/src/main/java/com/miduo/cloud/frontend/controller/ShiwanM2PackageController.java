package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.codepackage.CodePackageImportVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageOnlineImportResultVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackagePageQueryDTO;
import com.miduo.cloud.frontend.util.FxDialog;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    @FXML private Button helpButton;

    private final ObservableList<PackageRow> tableData = FXCollections.observableArrayList();
    private int currentPage = 1;
    private int pageSize = 20;
    private int totalPages = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        packageTable.setItems(tableData);
        // 日期组件只允许通过弹出日历选择，禁止手动键入
        startDatePicker.setEditable(false);
        endDatePicker.setEditable(false);
        SvgIconLoader.installHelpButtonGraphic(helpButton);
    }

    /** 首次切换到码包管理 Tab 时由主控制器调用，触发数据加载 */
    public void onFirstShow() {
        loadPage(1);
        triggerStartupOnlineUpdate();
    }

    private void setupTableColumns() {
        packageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        packageTable.setFixedCellSize(-1);
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().typeName));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().packageName));
        colImportTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importTimeText));
        colImportWay.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importSourceName));
        colCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().codeCount)));
        colRemark.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().remark));
        // 禁止列拖动重排
        colType.setReorderable(false);
        colName.setReorderable(false);
        colImportTime.setReorderable(false);
        colImportWay.setReorderable(false);
        colCount.setReorderable(false);
        colStatus.setReorderable(false);
        colRemark.setReorderable(false);
        colAction.setReorderable(false);

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statusName));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setFont(Font.font("Microsoft YaHei", 15));
                label.setAlignment(Pos.CENTER);
                label.setMinWidth(0);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected double computePrefHeight(double width) {
                String text = label.getText();
                if (text == null || text.isEmpty()) {
                    return 44;
                }
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
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    label.setText(null);
                    label.setStyle("");
                    return;
                }
                label.setText(softWrapLongToken(status));
                String font = "-fx-font-family: 'Microsoft YaHei'; -fx-font-size: 15px;";
                if ("正常".equals(status)) {
                    label.setStyle(font + "-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                } else if ("已删除".equals(status)) {
                    label.setStyle(font + "-fx-text-fill: #9CA3AF;");
                } else {
                    label.setStyle(font + "-fx-text-fill: #D97706;");
                }
            }
        });

        applyPackageWrapCellFactory(colType, Pos.CENTER);
        applyPackageWrapCellFactory(colName, Pos.CENTER);
        applyPackageWrapCellFactory(colImportTime, Pos.CENTER);
        applyPackageWrapCellFactory(colImportWay, Pos.CENTER);
        applyPackageWrapCellFactory(colCount, Pos.CENTER);
        applyPackageWrapCellFactory(colRemark, Pos.CENTER);

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("查看");
            private final Button deleteBtn = new Button("删除");
            private final HBox box = new HBox(6, viewBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
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
                setAlignment(Pos.CENTER);
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

    /** 码包表专用：多行展示，避免省略号；不影响其它 TableView */
    private static void applyPackageWrapCellFactory(TableColumn<PackageRow, String> col, Pos labelAlignment) {
        col.setCellFactory(c -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setFont(Font.font("Microsoft YaHei", 15));
                label.setAlignment(labelAlignment);
                label.setMinWidth(0);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                setAlignment(labelAlignment);
            }

            @Override
            protected double computePrefHeight(double width) {
                String text = label.getText();
                if (text == null || text.isEmpty()) {
                    return 44;
                }
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
                } else {
                    label.setText(softWrapLongToken(item));
                }
            }
        });
    }

    private static String softWrapLongToken(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        final int chunk = 12;
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            sb.append(ch);
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':') {
                run++;
                if (run >= chunk) {
                    sb.append('\u200B');
                    run = 0;
                }
            } else {
                run = 0;
            }
        }
        return sb.toString();
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
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
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
        FxHelpDialog.show(
                keywordField.getScene().getWindow(),
                "码包管理 - 功能说明",
                "- **码包导入说明**：码包文件均指外码，请勿导入内码，防止码包泄露。",
                "- **在线更新**（仅盖外码小标）：点击「在线更新」后，按上次成功拉取时间增量拉取瓶码（盖外码小标）。盒外码中标、箱外码大标不参与在线拉取，须本地导入。首次拉取全量，后续增量。拉取失败时不覆盖本地数据，可继续使用已有码包；可改本地导入或待网络恢复后重试。",
                "- **启动自动导入**：软件每次启动自动执行一次在线更新，仅拉取盖外码小标，规则与上相同。",
                "- **本地导入**（包材厂提供的码包文件）：盒外码中标、箱外码大标仅能本地导入；盖外码小标在无网络时也可本地导入。点击「本地导入」后，依次选择码包类型（瓶码、盒码或箱码，三种 TXT 格式相同，须手动指定）、本地 TXT 文件，可填备注（选填，最多 50 字），再输入密码并确认解析入库；导入成功后列表自动刷新。",
                "- **码包文件格式**（TXT）：瓶码、盒码、箱码文件均为每行一个对应码值。",
                "- **删除**：仅当该码包内码均未使用过时可删除；存在已使用码则不可删。删除后数据仍保留、可查记录。",
                "- **提醒**：生产前请确认码包已正确导入，否则采集会报错；可通过「查看」核对码包内容。"
        );
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
                String responseJson = HttpUtil.doPostLong("/api/code-package/import/online", "");
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
        Integer status = null; // 状态筛选已移除，默认查全部
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
            controller.setContext(row.id, row.packageName,
                    row.typeName, row.codeCount, row.statusName,
                    row.status != null && row.status == 1);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(packageTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setMinWidth(640);
            stage.setMinHeight(540);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "打开失败", "无法打开查看窗口：" + e.getMessage());
        }
    }

    private void onDeletePackage(PackageRow row) {
        Window owner = packageTable.getScene() != null ? packageTable.getScene().getWindow() : null;
        String body = "确认删除码包「" + row.packageName + "」？\n\n若该码包内存在已关联码，将被禁止删除。";
        if (!FxDialog.danger(owner, "删除确认", body)) {
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
