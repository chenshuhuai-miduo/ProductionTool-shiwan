package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.text.Font;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 生产统计 Tab 控制器
 */
public class ShiwanM2StatsController implements Initializable {

    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private TextField orderNoField;
    @FXML private Label palletNum;
    @FXML private Label caseNum;
    @FXML private Label boxNum;
    @FXML private Label rejectNum;
    @FXML private VBox palletCard;
    @FXML private VBox rejectCard;

    @FXML private DatePicker uploadStartDate;
    @FXML private DatePicker uploadEndDate;
    @FXML private TextField uploadOrderField;
    @FXML private ComboBox<String> uploadStatusCombo;

    @FXML private TableView<UploadRow> uploadTable;
    @FXML private TableColumn<UploadRow, String> upColPallet;
    @FXML private TableColumn<UploadRow, String> upColCases;
    @FXML private TableColumn<UploadRow, String> upColOrder;
    @FXML private TableColumn<UploadRow, String> upColTime;
    @FXML private TableColumn<UploadRow, String> upColStatus;
    @FXML private TableColumn<UploadRow, String> upColReason;

    @FXML private Label upTotalLabel;
    @FXML private ComboBox<String> upPageSizeCombo;
    @FXML private Button upFirstBtn;
    @FXML private Button upPrevBtn;
    @FXML private Button upNextBtn;
    @FXML private Button upLastBtn;
    @FXML private TextField upPageField;
    @FXML private Label upPageTotalLabel;

    private final ObservableList<UploadRow> pageData = FXCollections.observableArrayList();
    private int currentPage = 1;
    private int pageSize = 20;
    private int totalPages = 1;
    private long total = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startDate.setValue(LocalDate.now().withDayOfMonth(1));
        endDate.setValue(LocalDate.now());
        uploadStartDate.setValue(LocalDate.now().withDayOfMonth(1));
        uploadEndDate.setValue(LocalDate.now());
        // 日期组件只允许通过弹出日历选择，禁止手动键入
        startDate.setEditable(false);
        endDate.setEditable(false);
        uploadStartDate.setEditable(false);
        uploadEndDate.setEditable(false);
        // 生产单号输入框：仅英文和数字，最多16位
        applyOrderNoFilter(orderNoField);
        applyOrderNoFilter(uploadOrderField);
        setupColumns();
    }

    /** 生产单号输入限制：仅英文和数字，最多16位。 */
    private void applyOrderNoFilter(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            if (!newText.matches("[a-zA-Z0-9]{0,16}")) return null;
            return change;
        }));
    }

    /** 首次切换到生产统计 Tab 时由主控制器调用，触发数据加载 */
    public void onFirstShow() {
        onQuery();
        onUploadQuery();
    }

    private void setupColumns() {
        upColPallet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().palletCode));
        upColCases.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cases));
        upColOrder.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().orderNo));
        upColTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().uploadTime));
        upColReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason));
        upColStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        // 禁止列拖动重排
        upColPallet.setReorderable(false);
        upColCases.setReorderable(false);
        upColOrder.setReorderable(false);
        upColTime.setReorderable(false);
        upColStatus.setReorderable(false);
        upColReason.setReorderable(false);
        upColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                setAlignment(Pos.CENTER);
                setStyle("成功".equals(status)
                        ? "-fx-text-fill: #10B981; -fx-font-weight: bold;"
                        : "-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            }
        });
        upColReason.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setFont(Font.font("Microsoft YaHei", 15));
                label.setAlignment(Pos.TOP_LEFT);
                label.setMinWidth(0);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                setAlignment(Pos.TOP_LEFT);
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
        uploadTable.setItems(pageData);
        uploadTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        /* 与码包管理同用 sw2-table；异常原因列多行依赖非固定行高 */
        uploadTable.setFixedCellSize(-1);
    }

    /** 为超长连续串插入零宽断点，避免不换行撑开布局 */
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
    private void onQuery() {
        LocalDate start = startDate.getValue();
        LocalDate end   = endDate.getValue();
        if (start != null && end != null && end.isBefore(start)) {
            showWarn("结束日期不能早于开始日期");
            return;
        }
        String url = "/api/shiwan-m2/stats/production-summary?startDate=" + getDateText(startDate)
                + "&endDate=" + getDateText(endDate)
                + "&orderNo=" + encode(orderNoField.getText());
        HttpUtil.asyncGet(url, response -> {
            try {
                ApiResult<Map<String, Object>> result = HttpUtil.parseJson(
                        response, new TypeReference<ApiResult<Map<String, Object>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    showWarn(result == null ? "查询失败" : result.getMessage());
                    return;
                }
                Map<String, Object> data = result.getData();
                palletNum.setText(String.valueOf(num(data.get("palletCount"))));
                caseNum.setText(String.valueOf(num(data.get("caseCount"))));
                boxNum.setText(String.valueOf(num(data.get("boxCount"))));
                rejectNum.setText(String.valueOf(num(data.get("rejectCount"))));
            } catch (Exception ex) {
                showWarn("统计解析失败");
            }
        }, ex -> showWarn("统计查询异常：" + ex.getMessage()));
    }

    @FXML
    private void onUploadQuery() {
        LocalDate start = uploadStartDate.getValue();
        LocalDate end   = uploadEndDate.getValue();
        if (start != null && end != null && end.isBefore(start)) {
            showWarn("结束日期不能早于开始日期");
            return;
        }
        currentPage = 1;
        loadUploadPage();
    }

    @FXML
    private void onUploadPageSizeChange() {
        try {
            pageSize = Integer.parseInt(upPageSizeCombo.getValue());
        } catch (Exception ignore) {
            pageSize = 20;
        }
        currentPage = 1;
        loadUploadPage();
    }

    @FXML private void onUploadFirstPage() { currentPage = 1; loadUploadPage(); }
    @FXML private void onUploadPrevPage() { if (currentPage > 1) { currentPage--; loadUploadPage(); } }
    @FXML private void onUploadNextPage() { if (currentPage < totalPages) { currentPage++; loadUploadPage(); } }
    @FXML private void onUploadLastPage() { currentPage = totalPages; loadUploadPage(); }

    @FXML
    private void onUploadGoPage() {
        try {
            int page = Integer.parseInt(upPageField.getText().trim());
            currentPage = Math.max(1, Math.min(page, totalPages));
        } catch (Exception ignore) {
            currentPage = 1;
        }
        loadUploadPage();
    }

    private void loadUploadPage() {
        String status = uploadStatusCombo.getValue() == null ? "全部" : uploadStatusCombo.getValue();
        String url = "/api/shiwan-m2/stats/upload-records?startDate=" + getDateText(uploadStartDate)
                + "&endDate=" + getDateText(uploadEndDate)
                + "&orderNo=" + encode(uploadOrderField.getText())
                + "&status=" + encode(status)
                + "&page=" + currentPage
                + "&pageSize=" + pageSize;
        HttpUtil.asyncGet(url, response -> {
            try {
                ApiResult<Map<String, Object>> result = HttpUtil.parseJson(
                        response, new TypeReference<ApiResult<Map<String, Object>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    showWarn(result == null ? "查询失败" : result.getMessage());
                    return;
                }
                Map<String, Object> data = result.getData();
                total = num(data.get("total"));
                totalPages = Math.max(1, (int) num(data.get("pages")));
                currentPage = Math.max(1, Math.min(currentPage, totalPages));

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> records = (List<Map<String, Object>>) data.getOrDefault("records", new ArrayList<>());
                List<UploadRow> rows = new ArrayList<>();
                for (Map<String, Object> item : records) rows.add(UploadRow.from(item));
                pageData.setAll(rows);

                upTotalLabel.setText("共 " + total + " 条");
                upPageField.setText(String.valueOf(currentPage));
                upPageTotalLabel.setText("/ " + totalPages + " 页");
                upFirstBtn.setDisable(currentPage <= 1);
                upPrevBtn.setDisable(currentPage <= 1);
                upNextBtn.setDisable(currentPage >= totalPages);
                upLastBtn.setDisable(currentPage >= totalPages);
            } catch (Exception ex) {
                showWarn("上传统计解析失败");
            }
        }, ex -> showWarn("上传统计查询异常：" + ex.getMessage()));
    }

    @FXML
    private void onPalletCardClick(MouseEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2PalletListDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2PalletListController controller = loader.getController();
            controller.setContext(getDateText(startDate), getDateText(endDate), safe(orderNoField.getText()));
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(palletCard.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(800);
            stage.setMinHeight(650);
            stage.showAndWait();
        } catch (Exception ex) {
            showWarn("打开垛码列表失败：" + ex.getMessage());
        }
    }

    @FXML
    private void onRejectCardClick(MouseEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ShiwanM2RejectRecordsDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2RejectRecordsController controller = loader.getController();
            controller.setContext(getDateText(startDate), getDateText(endDate), safe(orderNoField.getText()));
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(rejectCard.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(1200);
            stage.setMinHeight(700);
            stage.showAndWait();
        } catch (Exception ex) {
            showWarn("打开剔除记录失败：" + ex.getMessage());
        }
    }

    private String getDateText(DatePicker picker) {
        LocalDate value = picker.getValue();
        return value == null ? "" : value.toString();
    }

    private long num(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private String encode(String value) {
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

    public static class UploadRow {
        public String palletCode = "";
        public String cases = "";
        public String orderNo = "";
        public String uploadTime = "";
        public String status = "";
        public String reason = "";

        static UploadRow from(Map<String, Object> map) {
            UploadRow row = new UploadRow();
            row.palletCode = str(map.get("palletCode"));
            row.cases = str(map.get("caseCount"));
            row.orderNo = str(map.get("orderNo"));
            row.uploadTime = formatDateTime(str(map.get("uploadTime")));
            String isUpload = str(map.get("isUpload"));
            row.status = "1".equals(isUpload) ? "成功" : "异常";
            row.reason = "1".equals(isUpload) ? "-" : str(map.get("errorMsg"));
            if (row.reason.isEmpty()) row.reason = "-";
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
