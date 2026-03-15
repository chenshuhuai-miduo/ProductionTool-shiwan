package com.miduo.cloud.frontend.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    @FXML private VBox                             emptyPane;
    @FXML private TableView<String[]>              resultTable;
    @FXML private TableColumn<String[], String>    colSeq;
    @FXML private TableColumn<String[], String>    colL1;
    @FXML private TableColumn<String[], String>    colL2;
    @FXML private TableColumn<String[], String>    colL3;
    @FXML private TableColumn<String[], String>    colL4;

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
    }

    private void setupColumns() {
        // 序号列：居中、灰色
        colSeq.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colSeq.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
            }
        });

        // 四层码列：若等于输入码则红色高亮
        colL1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colL2.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        colL3.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        colL4.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));

        applyHighlightCellFactory(colL1);
        applyHighlightCellFactory(colL2);
        applyHighlightCellFactory(colL3);
        applyHighlightCellFactory(colL4);
    }

    private void applyHighlightCellFactory(TableColumn<String[], String> col) {
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
        doQuery(code);
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
        alert.showAndWait();
    }

    // ==================== 查询逻辑 ====================

    private void doQuery(String code) {
        List<String[]> rows = findRowsByCode(code);
        if (rows.isEmpty()) {
            setStatus("err", "未找到该码信息");
            countLabel.setText("");
            showEmpty();
        } else {
            setStatus("ok", "查询成功");
            countLabel.setText("已查询到 " + rows.size() + " 条码信息");
            ObservableList<String[]> data = FXCollections.observableArrayList(rows);
            resultTable.setItems(data);
            resultTable.refresh();
            showResult();
            hideDetail();
        }
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

    private void showDetail(String[] row) {
        detailEmptyPane.setVisible(false);   detailEmptyPane.setManaged(false);
        detailScrollPane.setVisible(true);   detailScrollPane.setManaged(true);

        // row: [序号, 瓶码, 盒码, 箱码, 垛码]
        detBottle.setText(row[1].isEmpty() ? "—" : row[1]);
        detBox.setText(row[2].isEmpty()    ? "—" : row[2]);
        detCase.setText(row[3].isEmpty()   ? "—" : row[3]);
        detPallet.setText(row[4].isEmpty() ? "—" : row[4]);
        detTime.setText("2024-12-01 10:30:00");
        detLinked.setText("已关联");
        detLinked.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#059669;");
        detProductCode.setText("SP001");
        detProductName.setText("商品001");
        detOrderNo.setText("PO202602248710");
    }

    private void setStatus(String type, String text) {
        // 状态标签已移除，此处保留空实现供后续扩展
    }

    // ==================== 模拟数据 ====================

    /**
     * 根据输入码查找所属垛的所有行数据。
     * 行格式：[序号, 瓶码, 盒码, 箱码, 垛码]
     *
     * 数据结构（模拟）：
     *   垛 P20241201
     *   ├─ 箱 11 → 盒 20241201001002, 20241201002002, 20241201003002
     *   │   每盒 4 瓶：20241201001001/005/009/013, ...
     *   ├─ 箱 12 → 盒 20241201004002, 20241201005002, 20241201006002
     *   └─ 箱 13 → 盒 20241201007002, 20241201008002, 20241201009002
     */
    private List<String[]> findRowsByCode(String code) {
        List<String[]> allRows = buildMockData();
        boolean found = allRows.stream().anyMatch(row ->
                row[1].equals(code) || row[2].equals(code) ||
                row[3].equals(code) || row[4].equals(code));
        return found ? allRows : Collections.emptyList();
    }

    private List<String[]> buildMockData() {
        List<String[]> rows = new ArrayList<>();
        String pallet = "P20241201";
        // 箱码 → 盒码前缀映射（每箱3盒，每盒4瓶）
        String[][] caseBoxes = {
            {"11", "20241201001", "20241201002", "20241201003"},
            {"12", "20241201004", "20241201005", "20241201006"},
            {"13", "20241201007", "20241201008", "20241201009"}
        };
        int seq = 1;
        for (String[] cb : caseBoxes) {
            String caseCode = cb[0];
            for (int bi = 1; bi < cb.length; bi++) {
                String boxPrefix = cb[bi];
                String boxCode   = boxPrefix + "002";
                // 每盒4瓶，瓶码尾号 001, 005, 009, 013
                int[] bottleSuffixes = {1, 5, 9, 13};
                for (int suffix : bottleSuffixes) {
                    String bottleCode = boxPrefix + String.format("%03d", suffix);
                    rows.add(new String[]{
                            String.valueOf(seq++),
                            bottleCode,
                            boxCode,
                            caseCode,
                            pallet
                    });
                }
            }
        }
        return rows;
    }
}
