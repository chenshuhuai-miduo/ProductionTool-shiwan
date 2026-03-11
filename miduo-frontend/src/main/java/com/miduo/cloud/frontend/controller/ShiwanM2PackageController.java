package com.miduo.cloud.frontend.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 码包管理 Tab 控制器
 * <p>
 * 支持在线更新/本地导入码包，查询/删除码包记录，分页展示。
 * </p>
 */
public class ShiwanM2PackageController implements Initializable {

    // ==================== FXML 注入 ====================

    @FXML private TextField keywordField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> importTypeCombo;
    @FXML private ComboBox<String> statusCombo;

    @FXML private TableView<PackageRow>             packageTable;
    @FXML private TableColumn<PackageRow, String>   colType;
    @FXML private TableColumn<PackageRow, String>   colName;
    @FXML private TableColumn<PackageRow, String>   colImportTime;
    @FXML private TableColumn<PackageRow, String>   colImportWay;
    @FXML private TableColumn<PackageRow, String>   colCount;
    @FXML private TableColumn<PackageRow, String>   colStatus;
    @FXML private TableColumn<PackageRow, String>   colRemark;
    @FXML private TableColumn<PackageRow, Void>     colAction;

    @FXML private Label totalLabel;
    @FXML private Label pageLabel;
    @FXML private ComboBox<String> pageSizeCombo;

    // ==================== 内部状态 ====================

    private final ObservableList<PackageRow> tableData = FXCollections.observableArrayList();
    private final List<PackageRow> allData = new ArrayList<>();

    private int currentPage  = 1;
    private int pageSize     = 20;
    private int totalPages   = 1;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadSampleData();
        refreshTable();
    }

    private void setupTableColumns() {
        colType      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type));
        colName      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        colImportTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importTime));
        colImportWay .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().importWay));
        colCount     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().count));
        colRemark    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().remark));

        // 状态列 - 带颜色徽标
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                badge.setText(status);
                badge.getStyleClass().removeAll("sw2-badge-green", "sw2-badge-gray", "sw2-badge-red");
                String badgeClass;
                switch (status) {
                    case "正常":
                        badgeClass = "sw2-badge-green";
                        break;
                    case "已删除":
                        badgeClass = "sw2-badge-gray";
                        break;
                    default:
                        badgeClass = "sw2-badge-orange";
                        break;
                }
                badge.getStyleClass().add(badgeClass);
                setGraphic(badge);
                setText(null);
            }
        });

        // 操作列 - 查看/删除按钮
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn   = new Button("查看");
            private final Button deleteBtn = new Button("删除");
            private final HBox   box       = new HBox(6, viewBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                viewBtn.getStyleClass().add("sw2-btn-sm-blue");
                deleteBtn.getStyleClass().add("sw2-btn-sm-red");

                viewBtn.setOnAction(e -> {
                    PackageRow row = getTableView().getItems().get(getIndex());
                    onViewPackage(row);
                });
                deleteBtn.setOnAction(e -> {
                    PackageRow row = getTableView().getItems().get(getIndex());
                    onDeletePackage(row);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                PackageRow row = getTableView().getItems().get(getIndex());
                deleteBtn.setVisible(!"已删除".equals(row.status));
                setGraphic(box);
            }
        });

        packageTable.setItems(tableData);
    }

    private void loadSampleData() {
        allData.add(new PackageRow("盖外码小标", "瓶码包01",        "2024-12-01 10:00:00", "在线", "10,000", "正常",  "石湾批次1"));
        allData.add(new PackageRow("箱外码大标", "箱码包01",        "2024-12-01 09:00:00", "本地", "5,000",  "正常",  "石湾批次1"));
        allData.add(new PackageRow("盒外码中标", "盒码包01",        "2024-11-30 15:30:00", "在线", "8,000",  "正常",  "昨日批次"));
        allData.add(new PackageRow("盖外码小标", "瓶码包20241130", "2024-11-30 08:00:00", "本地", "200",   "已删除", "旧批次"));
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onOnlineUpdate() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("在线更新");
        info.setHeaderText("正在从在线源拉取码包...");
        info.setContentText("将拉取：盖外码小标（瓶码）+ 箱外码大标（箱码）\n盒外码中标不参与在线更新，需本地导入。");
        info.showAndWait();
        refreshTable();
    }

    @FXML
    private void onLocalImport() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("本地导入");
        info.setHeaderText("本地导入码包");
        info.setContentText("请选择码包类型，然后选择 TXT 文件，输入密码 123456 确认导入。\n（功能完整实现时对接文件选择对话框）");
        info.showAndWait();
    }

    @FXML
    private void onSearch() {
        refreshTable();
    }

    @FXML
    private void onReset() {
        keywordField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        importTypeCombo.setValue("全部");
        statusCombo.setValue("全部");
        currentPage = 1;
        refreshTable();
    }

    @FXML private void onFirstPage() { currentPage = 1;          refreshTable(); }
    @FXML private void onPrevPage()  { if (currentPage > 1) currentPage--;        refreshTable(); }
    @FXML private void onNextPage()  { if (currentPage < totalPages) currentPage++; refreshTable(); }
    @FXML private void onLastPage()  { currentPage = totalPages;  refreshTable(); }

    @FXML
    private void onHelp() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("码包导入说明");
        info.setHeaderText("码包管理操作帮助");
        info.setContentText(
                "在线更新：拉取瓶码（盖外码小标）和箱码（箱外码大标），按上次时间增量拉取。\n" +
                "本地导入：选择类型 → 选择TXT文件 → 输入密码123456 → 确认解析入库。\n" +
                "           盒外码中标仅能本地导入。\n" +
                "查看：查看码包内所有码，支持搜索和分页。\n" +
                "删除：仅当码包内无已关联码时允许删除（逻辑删除）。");
        info.showAndWait();
    }

    // ==================== 内部操作 ====================

    private void onViewPackage(PackageRow row) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("查看码包 - " + row.name);
        info.setHeaderText("码包详情：" + row.name);
        info.setContentText(
                "类型：" + row.type + "\n" +
                "导入方式：" + row.importWay + "\n" +
                "码包数量：" + row.count + "\n" +
                "状态：" + row.status + "\n" +
                "备注：" + row.remark + "\n\n" +
                "（完整实现时对接码包查看弹窗，支持分页浏览）");
        info.showAndWait();
    }

    private void onDeletePackage(PackageRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("删除确认");
        confirm.setHeaderText("确认删除码包「" + row.name + "」？");
        confirm.setContentText("若该码包内有已关联码，将无法删除。");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            row.status = "已删除";
            packageTable.refresh();
        }
    }

    private void refreshTable() {
        String keyword    = keywordField.getText().trim().toLowerCase();
        String importType = importTypeCombo.getValue();
        String status     = statusCombo.getValue();

        List<PackageRow> filtered = allData.stream()
                .filter(r -> keyword.isEmpty() || r.name.toLowerCase().contains(keyword))
                .filter(r -> "全部".equals(importType) || r.importWay.equals(importType))
                .filter(r -> "全部".equals(status) || r.status.equals(status))
                .collect(java.util.stream.Collectors.toList());

        int pageSz;
        switch (pageSizeCombo.getValue()) {
            case "50条":
                pageSz = 50;
                break;
            case "100条":
                pageSz = 100;
                break;
            default:
                pageSz = 20;
                break;
        }
        pageSize   = pageSz;
        totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / pageSize));
        currentPage = Math.min(currentPage, totalPages);

        int from = (currentPage - 1) * pageSize;
        int to   = Math.min(from + pageSize, filtered.size());

        tableData.setAll(filtered.subList(from, to));
        totalLabel.setText("共 " + filtered.size() + " 条");
        pageLabel.setText("第 " + currentPage + " / " + totalPages + " 页");
    }

    // ==================== 数据模型 ====================

    public static class PackageRow {
        public final String type;
        public final String name;
        public final String importTime;
        public final String importWay;
        public final String count;
        public       String status;
        public final String remark;

        public PackageRow(String type, String name, String importTime, String importWay,
                          String count, String status, String remark) {
            this.type       = type;
            this.name       = name;
            this.importTime = importTime;
            this.importWay  = importWay;
            this.count      = count;
            this.status     = status;
            this.remark     = remark;
        }
    }
}
