package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 数据查询 Tab 控制器
 *
 * 布局：顶部查询栏 + 左右分栏
 *   左侧：码关联信息分层表格（序号 / 第一层瓶码 / 第二层盒码 / 第三层箱码 / 第四层垛码）
 *   右侧：具体信息详情面板（点击表格行后联动显示）
 *
 * 查询逻辑：输入任意层级码，查出该码所属垛的全部关联数据（垛-箱-盒-瓶完整链路）
 * 输入码标识：与输入码对应的单元格以红色字体（#F44336）显示
 */
public class ShiwanM2QueryController implements Initializable {

    // ==================== FXML 注入 ====================

    @FXML private TextField queryInput;
    @FXML private Label     countLabel;

    // 左侧
    @FXML private VBox                                emptyPane;
    @FXML private TableView<QueryRow>                 resultTable;
    @FXML private TableColumn<QueryRow, String>       colSeq;
    @FXML private TableColumn<QueryRow, String>       colL1;
    @FXML private TableColumn<QueryRow, String>       colL2;
    @FXML private TableColumn<QueryRow, String>       colL3;
    @FXML private TableColumn<QueryRow, String>       colL4;

    // 右侧
    @FXML private VBox       detailEmptyPane;
    @FXML private ScrollPane detailScrollPane;
    @FXML private Label      detBottle;
    @FXML private Label      detBox;
    @FXML private Label      detCase;
    @FXML private Label      detPallet;
    @FXML private Label      detTime;
    @FXML private Label      detLinked;
    @FXML private Label      detProductCode;
    @FXML private Label      detProductName;
    @FXML private Label      detOrderNo;

    // ==================== 内部状态 ====================

    /** 当前查询的输入码，用于红色高亮 */
    private String inputCode = "";

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupRowSelection();
        Label placeholder = new Label("未找到该码信息");
        placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 16px;");
        resultTable.setPlaceholder(placeholder);
    }

    private void setupColumns() {
        // 序号列：居中、灰色
        colSeq.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().seq));
        colSeq.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
            }
        });

        // 四层码列：若等于输入码则红色高亮
        colL1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().bottleCode));
        colL2.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().boxCode));
        colL3.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().caseCode));
        colL4.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().palletCode));

        applyHighlightCellFactory(colL1);
        applyHighlightCellFactory(colL2);
        applyHighlightCellFactory(colL3);
        applyHighlightCellFactory(colL4);

        // 禁止列拖动重排
        colSeq.setReorderable(false);
        colL1.setReorderable(false);
        colL2.setReorderable(false);
        colL3.setReorderable(false);
        colL4.setReorderable(false);
    }

    private void applyHighlightCellFactory(TableColumn<QueryRow, String> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setStyle("-fx-font-size: 15px;");
                } else {
                    setText(item);
                    if (!inputCode.isEmpty() && item.equals(inputCode)) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-font-size: 15px;");
                    } else {
                        setStyle("-fx-font-size: 15px; -fx-text-fill: #1F2937;");
                    }
                }
            }
        });
    }

    private void setupRowSelection() {
        resultTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row != null) showDetail(row);
        });
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onInputKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) onQuery();
    }

    @FXML
    private void onQuery() {
        String code = queryInput.getText().trim();
        if (code.isEmpty()) return;
        inputCode = code;
        doQueryAsync(code);
    }

    @FXML
    private void onClear() {
        inputCode = "";
        queryInput.clear();
        countLabel.setText("");
        showEmpty();
        setStatus("idle", "未查询");
    }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("数据查询帮助");
        alert.setHeaderText(null);
        alert.setContentText(
                "一、查询方式\n" +
                "─────────────────────────────────────────\n" +
                "输入码后执行查询，查出该码所属垛的全部数据。\n" +
                "例如输入箱码，显示该垛下所有箱及各自的盒、瓶。\n\n" +
                "左侧表格按层级分列展示：\n" +
                "  第一层（瓶码）、第二层（盒码）、第三层（箱码）、第四层（垛码）\n\n" +
                "与输入的码对应的单元格红色字体显示；\n" +
                "点击某行，右侧展示该行的具体信息。\n\n" +
                "二、包装层级\n" +
                "─────────────────────────────────────────\n" +
                "四级包装结构：瓶 → 盒 → 箱 → 垛\n\n" +
                "三、操作方式\n" +
                "─────────────────────────────────────────\n" +
                "输入：手动输入码值 或 扫码枪扫描\n" +
                "查询：点击「查询」按钮 或 回车键");
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    // ==================== 查询逻辑 ====================

    private void doQueryAsync(String code) {
        setStatus("loading", "正在查询...");
        String url = "/api/shiwan-m2/code/query?code=" + encodeParam(code);
        HttpUtil.asyncGet(url, response -> {
            try {
                ApiResult<Map<String, Object>> result = HttpUtil.parseJson(
                        response, new TypeReference<ApiResult<Map<String, Object>>>() {});
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    setStatus("err", result == null ? "查询服务异常" : result.getMessage());
                    countLabel.setText("未找到该码信息");
                    resultTable.setItems(FXCollections.observableArrayList());
                    showResult();
                    hideDetail();
                    return;
                }
                Object rowsObj = result.getData().get("rows");
                if (!(rowsObj instanceof List) || ((List<?>) rowsObj).isEmpty()) {
                    setStatus("err", "未找到该码信息");
                    countLabel.setText("未找到该码信息");
                    resultTable.setItems(FXCollections.observableArrayList());
                    showResult();
                    hideDetail();
                    return;
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) rowsObj;
                ObservableList<QueryRow> data = FXCollections.observableArrayList();
                for (int i = 0; i < rows.size(); i++) {
                    data.add(QueryRow.from(i + 1, rows.get(i)));
                }
                setStatus("ok", "查询成功");
                countLabel.setText("已查询到 " + data.size() + " 条码信息");
                resultTable.setItems(data);
                resultTable.refresh();
                showResult();
                hideDetail();
            } catch (Exception ex) {
                setStatus("err", "查询服务异常");
                countLabel.setText("");
                showEmpty();
            }
        }, ex -> {
            setStatus("err", "查询服务异常");
            countLabel.setText("");
            showEmpty();
        });
    }

    // ==================== 视图切换 ====================

    private void showEmpty() {
        emptyPane.setVisible(true);     emptyPane.setManaged(true);
        resultTable.setVisible(false);  resultTable.setManaged(false);
        hideDetail();
    }

    private void showResult() {
        emptyPane.setVisible(false);    emptyPane.setManaged(false);
        resultTable.setVisible(true);   resultTable.setManaged(true);
    }

    private void hideDetail() {
        detailEmptyPane.setVisible(true);    detailEmptyPane.setManaged(true);
        detailScrollPane.setVisible(false);  detailScrollPane.setManaged(false);
    }

    private void showDetail(QueryRow row) {
        detailEmptyPane.setVisible(false);   detailEmptyPane.setManaged(false);
        detailScrollPane.setVisible(true);   detailScrollPane.setManaged(true);

        detBottle.setText(row.bottleCode.isEmpty() ? "—" : row.bottleCode);
        detBox.setText(row.boxCode.isEmpty() ? "—" : row.boxCode);
        detCase.setText(row.caseCode.isEmpty() ? "—" : row.caseCode);
        detPallet.setText(row.palletCode.isEmpty() ? "—" : row.palletCode);
        detTime.setText(row.collectTime.isEmpty() ? "-" : row.collectTime);
        detLinked.setText("已关联");
        detLinked.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#059669;");
        detProductCode.setText(row.productNo.isEmpty() ? "-" : row.productNo);
        detProductName.setText(row.productName.isEmpty() ? "-" : row.productName);
        detOrderNo.setText(row.orderNo.isEmpty() ? "-" : row.orderNo);
    }

    private void setStatus(String type, String text) {
        // 状态标签已移除，此处保留空实现供后续扩展
    }

    private String encodeParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static class QueryRow {
        private String seq = "";
        private String bottleCode = "";
        private String boxCode = "";
        private String caseCode = "";
        private String palletCode = "";
        private String collectTime = "";
        private String productNo = "";
        private String productName = "";
        private String orderNo = "";

        static QueryRow from(int seqNo, Map<String, Object> map) {
            QueryRow row = new QueryRow();
            row.seq = String.valueOf(seqNo);
            row.bottleCode = value(map.get("bottleCode"));
            row.boxCode = value(map.get("boxCode"));
            row.caseCode = value(map.get("caseCode"));
            row.palletCode = value(map.get("palletCode"));
            row.collectTime = value(map.get("collectTime"));
            row.productNo = value(map.get("productNo"));
            row.productName = value(map.get("productName"));
            row.orderNo = value(map.get("orderNo"));
            return row;
        }

        private static String value(Object v) {
            return v == null ? "" : String.valueOf(v);
        }
    }
}
