package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.FxDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 剔除记录弹窗：左侧剔除事件主表，右侧概要 + 留痕明细（对齐设计 4J8s2 / P02-07）。
 */
public class ShiwanM2RejectRecordsController implements Initializable {

    /**
     * JDK11 + JavaFX17：可变行高且单元格使用自定义 Graphic 时，仅靠 CSS 的 table-cell 底边线常不显示；
     * 使用内联样式保证行间分隔线，并在 layout/computePrefHeight 中预留 1px 底部，避免 Label 铺满盖住线。
     */
    private static final String REJECT_TABLE_CELL_GRID_STYLE =
            "-fx-border-color: transparent transparent #E5E7EB transparent; -fx-border-width: 0 0 1px 0; -fx-border-style: solid;";
    private static final int REJECT_CELL_BOTTOM_GRID_GAP = 1;

    private static void applyRejectTableCellGridStyle(TableCell<?, ?> cell) {
        cell.setStyle(REJECT_TABLE_CELL_GRID_STYLE);
    }

    @FXML private HBox titleBar;
    @FXML private TextField caseCodeField;
    @FXML private TableView<EventRow> eventTable;
    @FXML private TableColumn<EventRow, String> seqCol;
    @FXML private TableColumn<EventRow, String> caseCol;
    @FXML private TableColumn<EventRow, String> timeCol;
    @FXML private TableColumn<EventRow, String> summaryCol;
    @FXML private Label totalLabel;
    @FXML private TextField pageInputField;
    @FXML private Label totalPagesLabel;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    @FXML private VBox detailEmptyPane;
    @FXML private VBox detailPane;
    @FXML private Label sumCase;
    @FXML private Label sumTime;
    @FXML private Label sumOrder;
    @FXML private Label sumProductNo;
    @FXML private Label sumProductName;
    @FXML private Label sumTraceCount;
    @FXML private TableView<DetailRow> detailTable;
    @FXML private TableColumn<DetailRow, String> dSeqCol;
    @FXML private TableColumn<DetailRow, String> dLinkCol;
    @FXML private TableColumn<DetailRow, String> dProblemCol;
    @FXML private TableColumn<DetailRow, String> dReasonCol;

    private final ObservableList<EventRow> events = FXCollections.observableArrayList();
    private final ObservableList<DetailRow> details = FXCollections.observableArrayList();

    private String startDate = "";
    private String endDate = "";
    private String orderNo = "";
    private int page = 1;
    private int pageSize = 20;
    private int pages = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEventTable();
        initDetailTable();

        eventTable.setItems(events);
        detailTable.setItems(details);
        eventTable.getSelectionModel().selectedItemProperty().addListener((obs, o, row) -> onEventSelected(row));

        pageSizeCombo.setValue("20条");
        pageSizeCombo.setOnAction(e -> {
            pageSize = parsePageSize(pageSizeCombo.getValue());
            page = 1;
            loadData();
        });
        showEmptyRight();
    }

    private void initEventTable() {
        eventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        eventTable.setFixedCellSize(-1);

        seqCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().seq));
        seqCol.setCellFactory(col -> centeredLabelCell(15, "#1F2937"));

        caseCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().displayCaseCode()));
        caseCol.setCellFactory(col -> new CaseCodeTableCell());

        timeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rejectTime));
        timeCol.setCellFactory(col -> wrapEventCell(14, "#374151", timeCol));

        summaryCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reasonSummary));
        summaryCol.setCellFactory(col -> wrapEventCell(14, "#374151", summaryCol));
    }

    private void initDetailTable() {
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setFixedCellSize(-1);

        dSeqCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().seq));
        dSeqCol.setCellFactory(col -> centeredDetailSeqCell(14, "#1F2937"));

        dLinkCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().linkLines()));
        dLinkCol.setCellFactory(col -> wrapDetailCell(13, "#1F2937", dLinkCol));

        dProblemCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().displayProblem()));
        dProblemCol.setCellFactory(col -> new ProblemCodeTableCell(dProblemCol));

        dReasonCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rejectReason));
        dReasonCol.setCellFactory(col -> wrapDetailCell(13, "#1F2937", dReasonCol));
    }

    private static TableCell<EventRow, String> centeredLabelCell(int sizePx, String color) {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.setFont(Font.font("Microsoft YaHei", sizePx));
                label.setStyle(style(sizePx, color));
                label.setAlignment(Pos.CENTER);
                label.setWrapText(true);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                applyRejectTableCellGridStyle(this);
            }
            @Override
            protected double computePrefHeight(double width) {
                return 44 + REJECT_CELL_BOTTOM_GRID_GAP;
            }
            @Override
            protected void layoutChildren() {
                double h = Math.max(0, getHeight() - REJECT_CELL_BOTTOM_GRID_GAP);
                label.resizeRelocate(0, 0, getWidth(), h);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty || item == null ? null : item);
            }
        };
    }

    private static TableCell<DetailRow, String> centeredDetailSeqCell(int sizePx, String color) {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.setFont(Font.font("Microsoft YaHei", sizePx));
                label.setStyle(style(sizePx, color));
                label.setAlignment(Pos.CENTER);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                applyRejectTableCellGridStyle(this);
            }
            @Override
            protected double computePrefHeight(double width) {
                return 44 + REJECT_CELL_BOTTOM_GRID_GAP;
            }
            @Override
            protected void layoutChildren() {
                double h = Math.max(0, getHeight() - REJECT_CELL_BOTTOM_GRID_GAP);
                label.resizeRelocate(0, 0, getWidth(), h);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty || item == null ? null : item);
            }
        };
    }

    private static String style(int sizePx, String color) {
        return "-fx-font-family:'Microsoft YaHei'; -fx-font-size:" + sizePx + "px; -fx-text-fill:" + color + ";";
    }

    private TableCell<EventRow, String> wrapEventCell(int sizePx, String color, TableColumn<EventRow, String> column) {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setFont(Font.font("Microsoft YaHei", sizePx));
                label.setAlignment(Pos.CENTER_LEFT);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                applyRejectTableCellGridStyle(this);
            }
            @Override
            protected double computePrefHeight(double width) {
                String text = label.getText();
                if (text == null || text.isEmpty()) return 44 + REJECT_CELL_BOTTOM_GRID_GAP;
                double w = width > 0 ? width : column.getWidth();
                double lw = Math.max(0, w - 16);
                return lw > 0 ? Math.max(44, label.prefHeight(lw) + 16) + REJECT_CELL_BOTTOM_GRID_GAP : 44 + REJECT_CELL_BOTTOM_GRID_GAP;
            }
            @Override
            protected void layoutChildren() {
                double lw = Math.max(0, getWidth() - 16);
                double maxLabelH = Math.max(0, getHeight() - 8 - REJECT_CELL_BOTTOM_GRID_GAP);
                double lh = Math.min(label.prefHeight(lw), maxLabelH);
                label.resize(lw, lh);
                label.relocate(8, 8);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty || item == null ? null : item);
                label.setStyle(style(sizePx, color));
            }
        };
    }

    private TableCell<DetailRow, String> wrapDetailCell(int sizePx, String color, TableColumn<DetailRow, String> column) {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setFont(Font.font("Microsoft YaHei", sizePx));
                label.setAlignment(Pos.CENTER_LEFT);
                setGraphic(label);
                setText(null);
                setPadding(Insets.EMPTY);
                applyRejectTableCellGridStyle(this);
            }
            @Override
            protected double computePrefHeight(double width) {
                String text = label.getText();
                if (text == null || text.isEmpty()) return 44 + REJECT_CELL_BOTTOM_GRID_GAP;
                double w = width > 0 ? width : column.getWidth();
                double lw = Math.max(0, w - 16);
                return lw > 0 ? Math.max(44, label.prefHeight(lw) + 16) + REJECT_CELL_BOTTOM_GRID_GAP : 44 + REJECT_CELL_BOTTOM_GRID_GAP;
            }
            @Override
            protected void layoutChildren() {
                double lw = Math.max(0, getWidth() - 16);
                double maxLabelH = Math.max(0, getHeight() - 8 - REJECT_CELL_BOTTOM_GRID_GAP);
                double lh = Math.min(label.prefHeight(lw), maxLabelH);
                label.resize(lw, lh);
                label.relocate(8, 8);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty || item == null ? null : item);
                label.setStyle(style(sizePx, color));
            }
        };
    }

    private final class CaseCodeTableCell extends TableCell<EventRow, String> {
        private final Label label = new Label();

        CaseCodeTableCell() {
            label.setWrapText(true);
            label.setFont(Font.font("Microsoft YaHei", 15));
            label.setAlignment(Pos.CENTER_LEFT);
            setGraphic(label);
            setText(null);
            setPadding(Insets.EMPTY);
            applyRejectTableCellGridStyle(this);
        }

        @Override
        protected double computePrefHeight(double width) {
            EventRow row = getTableRow() != null ? getTableRow().getItem() : null;
            String t = row == null ? "" : row.displayCaseCode();
            if (t.isEmpty()) return 44 + REJECT_CELL_BOTTOM_GRID_GAP;
            double w = width > 0 ? width : caseCol.getWidth();
            double lw = Math.max(0, w - 16);
            label.setText(t);
            applyCaseStyle(row);
            return lw > 0 ? Math.max(44, label.prefHeight(lw) + 16) + REJECT_CELL_BOTTOM_GRID_GAP : 44 + REJECT_CELL_BOTTOM_GRID_GAP;
        }

        @Override
        protected void layoutChildren() {
            double lw = Math.max(0, getWidth() - 16);
            double maxLabelH = Math.max(0, getHeight() - 8 - REJECT_CELL_BOTTOM_GRID_GAP);
            double lh = Math.min(label.prefHeight(lw), maxLabelH);
            label.resize(lw, lh);
            label.relocate(8, 8);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            EventRow row = getTableRow() != null ? getTableRow().getItem() : null;
            if (empty || row == null) {
                label.setText(null);
                return;
            }
            label.setText(row.displayCaseCode());
            applyCaseStyle(row);
        }

        private void applyCaseStyle(EventRow row) {
            boolean em = row != null && row.caseCodeIsEmpty();
            label.setStyle(style(15, em ? "#6B7280" : "#1F2937"));
        }
    }

    private static final class ProblemCodeTableCell extends TableCell<DetailRow, String> {
        private final Label label = new Label();
        private final TableColumn<DetailRow, String> column;

        ProblemCodeTableCell(TableColumn<DetailRow, String> column) {
            this.column = column;
            label.setWrapText(true);
            label.setFont(Font.font("Microsoft YaHei", 13));
            label.setAlignment(Pos.CENTER_LEFT);
            setGraphic(label);
            setText(null);
            setPadding(Insets.EMPTY);
            applyRejectTableCellGridStyle(this);
        }

        @Override
        protected double computePrefHeight(double width) {
            String text = label.getText();
            if (text == null || text.isEmpty()) return 44 + REJECT_CELL_BOTTOM_GRID_GAP;
            double w = width > 0 ? width : column.getWidth();
            double lw = Math.max(0, w - 16);
            return lw > 0 ? Math.max(44, label.prefHeight(lw) + 16) + REJECT_CELL_BOTTOM_GRID_GAP : 44 + REJECT_CELL_BOTTOM_GRID_GAP;
        }

        @Override
        protected void layoutChildren() {
            double lw = Math.max(0, getWidth() - 16);
            double maxLabelH = Math.max(0, getHeight() - 8 - REJECT_CELL_BOTTOM_GRID_GAP);
            double lh = Math.min(label.prefHeight(lw), maxLabelH);
            label.resize(lw, lh);
            label.relocate(8, 8);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                label.setText(null);
                label.setStyle("");
                return;
            }
            label.setText(item);
            int sz = "—".equals(item) ? 14 : 13;
            label.setStyle("-fx-font-family:'Microsoft YaHei'; -fx-font-size:" + sz + "px; -fx-text-fill:#F44336;");
        }
    }

    public void setContext(String startDate, String endDate, String orderNo) {
        this.startDate = safe(startDate);
        this.endDate = safe(endDate);
        this.orderNo = safe(orderNo);
        this.page = 1;
        loadData();
    }

    @FXML
    private void onSearch() {
        page = 1;
        loadData();
    }

    @FXML
    private void onReset() {
        caseCodeField.clear();
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
    private void onPageInput() {
        try {
            int p = Integer.parseInt(pageInputField.getText().trim());
            if (p >= 1 && p <= pages) {
                page = p;
                loadData();
            } else {
                pageInputField.setText(String.valueOf(page));
            }
        } catch (NumberFormatException ex) {
            pageInputField.setText(String.valueOf(page));
        }
    }

    @FXML
    private void onClose() {
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    private void loadData() {
        String url = "/api/shiwan-m2/stats/reject-records?startDate=" + enc(startDate)
                + "&endDate=" + enc(endDate)
                + "&orderNo=" + enc(orderNo)
                + "&caseCode=" + enc(caseCodeField.getText())
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
                List<EventRow> mapped = new ArrayList<>();
                for (int i = 0; i < records.size(); i++) {
                    mapped.add(EventRow.from(i + 1 + (page - 1) * pageSize, records.get(i)));
                }
                Platform.runLater(() -> {
                    events.setAll(mapped);
                    eventTable.refresh();
                    long total = num(data.get("total"));
                    pages = Math.max(1, (int) num(data.get("pages")));
                    page = Math.max(1, Math.min(page, pages));
                    totalLabel.setText("共 " + total + " 条");
                    pageInputField.setText(String.valueOf(page));
                    totalPagesLabel.setText("/ " + pages + " 页");
                    prevBtn.setDisable(page <= 1);
                    nextBtn.setDisable(page >= pages);
                    eventTable.getSelectionModel().clearSelection();
                    showEmptyRight();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showWarn("解析失败：" + ex.getMessage()));
            }
        }, ex -> Platform.runLater(() -> showWarn("查询异常：" + ex.getMessage())));
    }

    private void onEventSelected(EventRow row) {
        if (row == null || row.eventId == null) {
            showEmptyRight();
            return;
        }
        detailEmptyPane.setVisible(false);
        detailEmptyPane.setManaged(false);
        detailPane.setVisible(true);
        detailPane.setManaged(true);

        sumCase.setText("箱码：" + row.displayCaseCode());
        sumTime.setText("剔除时间：" + dash(row.rejectTime));
        sumOrder.setText("生产单号：" + dash(row.orderNo));
        sumProductNo.setText("产品编号：" + dash(row.productNo));
        sumProductName.setText("产品名称：" + dash(row.productName));
        sumTraceCount.setText("共 " + row.detailCount + " 条留痕");

        details.clear();
        detailTable.refresh();
        loadDetails(row.eventId);
    }

    private void showEmptyRight() {
        detailEmptyPane.setVisible(true);
        detailEmptyPane.setManaged(true);
        detailPane.setVisible(false);
        detailPane.setManaged(false);
        details.clear();
    }

    private void loadDetails(Long eventId) {
        String url = "/api/shiwan-m2/stats/reject-record-details?eventId=" + eventId;
        HttpUtil.asyncGet(url, json -> {
            try {
                ApiResult<List<Map<String, Object>>> result = HttpUtil.parseJson(
                        json, new TypeReference<ApiResult<List<Map<String, Object>>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    Platform.runLater(() -> showWarn(result == null ? "明细查询失败" : result.getMessage()));
                    return;
                }
                List<Map<String, Object>> list = result.getData();
                List<DetailRow> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    rows.add(DetailRow.from(i + 1, list.get(i)));
                }
                Platform.runLater(() -> {
                    details.setAll(rows);
                    detailTable.refresh();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showWarn("明细解析失败：" + ex.getMessage()));
            }
        }, ex -> Platform.runLater(() -> showWarn("明细查询异常：" + ex.getMessage())));
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
            return Integer.parseInt(value == null ? "20" : value.replace("条", "").trim());
        } catch (Exception ex) {
            return 20;
        }
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String dash(String text) {
        String v = safe(text);
        return v.isEmpty() ? "—" : v;
    }

    private String enc(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private Window dialogOwner() {
        if (titleBar != null && titleBar.getScene() != null) {
            return titleBar.getScene().getWindow();
        }
        if (eventTable != null && eventTable.getScene() != null) {
            return eventTable.getScene().getWindow();
        }
        return null;
    }

    private void showWarn(String msg) {
        FxDialog.warn(dialogOwner(), "提示", msg);
    }

    static final class EventRow {
        Long eventId;
        String seq = "";
        String caseCode = "";
        String rejectTime = "";
        String reasonSummary = "";
        String orderNo = "";
        String productNo = "";
        String productName = "";
        long detailCount;

        boolean caseCodeIsEmpty() {
            return caseCode == null || caseCode.isBlank();
        }

        String displayCaseCode() {
            return caseCodeIsEmpty() ? "—" : caseCode.trim();
        }

        static EventRow from(int seqNo, Map<String, Object> map) {
            EventRow row = new EventRow();
            row.eventId = longOrNull(map.get("Id"), map.get("id"));
            row.seq = String.valueOf(seqNo);
            row.caseCode = str(map.get("CaseCode"), map.get("caseCode"));
            row.rejectTime = formatDateTime(str(map.get("RejectTime"), map.get("rejectTime")));
            row.reasonSummary = str(map.get("RejectReason"), map.get("rejectReason"));
            row.orderNo = str(map.get("OrderNo"), map.get("orderNo"));
            row.productNo = str(map.get("ProductNo"), map.get("productNo"));
            row.productName = str(map.get("ProductName"), map.get("productName"));
            row.detailCount = num(map.get("detailCount"));
            return row;
        }

        private static long num(Object v) {
            if (v instanceof Number) return ((Number) v).longValue();
            try {
                return Long.parseLong(String.valueOf(v));
            } catch (Exception ex) {
                return 0L;
            }
        }

        private static Long longOrNull(Object a, Object b) {
            Object v = a != null ? a : b;
            if (v == null) return null;
            if (v instanceof Number) return ((Number) v).longValue();
            try {
                return Long.parseLong(String.valueOf(v));
            } catch (Exception ex) {
                return null;
            }
        }

        private static String str(Object primary, Object fallback) {
            Object value = primary != null ? primary : fallback;
            return value == null ? "" : String.valueOf(value);
        }

        private static String formatDateTime(String s) {
            return s == null || s.isEmpty() ? s : s.replace("T", " ");
        }
    }

    static final class DetailRow {
        String seq = "";
        String bottle = "";
        String box = "";
        String caseCode = "";
        String problemCode = "";
        String rejectReason = "";

        String linkLines() {
            return "瓶：" + nz(bottle) + "\n盒：" + nz(box) + "\n箱：" + nz(caseCode);
        }

        String displayProblem() {
            String p = safe(problemCode);
            return p.isEmpty() ? "—" : p;
        }

        private static String nz(String s) {
            String v = safe(s);
            return v.isEmpty() ? "—" : v;
        }

        private static String safe(String s) {
            return s == null ? "" : s.trim();
        }

        static DetailRow from(int seqNo, Map<String, Object> map) {
            DetailRow r = new DetailRow();
            r.seq = String.valueOf(seqNo);
            r.bottle = str(map.get("bottleCode"), map.get("BottleCode"));
            r.box = str(map.get("boxCode"), map.get("BoxCode"));
            r.caseCode = str(map.get("caseCode"), map.get("CaseCode"));
            r.problemCode = str(map.get("problemCode"), map.get("ProblemCode"));
            r.rejectReason = str(map.get("rejectReason"), map.get("RejectReason"));
            return r;
        }

        private static String str(Object a, Object b) {
            Object v = a != null ? a : b;
            return v == null ? "" : String.valueOf(v);
        }
    }
}
