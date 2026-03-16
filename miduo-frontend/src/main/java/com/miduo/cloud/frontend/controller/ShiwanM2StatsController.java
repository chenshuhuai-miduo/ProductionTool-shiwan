package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
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
        setupColumns();
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
        uploadTable.setItems(pageData);
        uploadTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void onQuery() {
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
            stage.initOwner(palletCard.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("垛码列表");
            stage.setScene(new Scene(root));
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
            stage.initOwner(rejectCard.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("剔除记录");
            stage.setScene(new Scene(root));
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
            row.uploadTime = str(map.get("uploadTime"));
            String isUpload = str(map.get("isUpload"));
            row.status = "1".equals(isUpload) ? "成功" : "异常";
            row.reason = "1".equals(isUpload) ? "-" : str(map.get("errorMsg"));
            if (row.reason.isEmpty()) row.reason = "-";
            return row;
        }

        private static String str(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }
}
