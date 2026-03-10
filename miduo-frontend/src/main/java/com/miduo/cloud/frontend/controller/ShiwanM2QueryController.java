package com.miduo.cloud.frontend.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 数据查询 Tab 控制器
 * <p>
 * 支持正向查询（上级码查下级）和反向查询（下级码追溯上级路径及同级）。
 * </p>
 */
public class ShiwanM2QueryController implements Initializable {

    // ==================== FXML 注入 ====================

    @FXML private TextField queryInput;
    @FXML private Label     queryStatusLabel;

    // 空状态
    @FXML private VBox emptyPane;

    // 正向查询结果
    @FXML private VBox   forwardResultPane;
    @FXML private Label  fwdCodeValue;
    @FXML private Label  fwdCodeType;
    @FXML private Label  fwdCodeTime;
    @FXML private Label  fwdResultTitle;
    @FXML private Label  fwdCountLabel;
    @FXML private TableView<String[]>          fwdTable;
    @FXML private TableColumn<String[], String> fwdColSeq;
    @FXML private TableColumn<String[], String> fwdColCode;
    @FXML private TableColumn<String[], String> fwdColTime;

    // 反向查询结果
    @FXML private VBox   reverseResultPane;
    @FXML private Label  revCodeValue;
    @FXML private Label  revCodeType;
    @FXML private Label  revCodeTime;
    @FXML private HBox   chainBox;
    @FXML private Label  siblingTitle;
    @FXML private TableView<String[]>          siblingTable;
    @FXML private TableColumn<String[], String> sibColCode;
    @FXML private TableColumn<String[], String> sibColTime;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
    }

    private void setupColumns() {
        fwdColSeq .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        fwdColCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        fwdColTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));

        sibColCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        sibColTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onInputKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            onForwardQuery();
        }
    }

    @FXML
    private void onForwardQuery() {
        String code = queryInput.getText().trim();
        if (code.isEmpty()) return;
        doForwardQuery(code);
    }

    @FXML
    private void onReverseQuery() {
        String code = queryInput.getText().trim();
        if (code.isEmpty()) return;
        doReverseQuery(code);
    }

    @FXML
    private void onClear() {
        queryInput.clear();
        showEmpty();
        queryStatusLabel.setText("未查询");
        queryStatusLabel.getStyleClass().removeAll("sw2-query-status-ok", "sw2-query-status-err");
        queryStatusLabel.getStyleClass().add("sw2-query-status-idle");
    }

    @FXML
    private void onHelp() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("数据查询帮助");
        alert.setHeaderText(null);
        alert.setContentText(
                "正向查询：输入高层级码（盒/箱/垛），查看其包含的低层级码列表。\n\n" +
                "反向查询：输入低层级码（瓶/盒），追溯其所属的上级路径（盒→箱→垛），\n" +
                "             并展示同级的其他码。\n\n" +
                "可输入：瓶码、盒码、箱码、垛码，支持扫码枪直接扫描后回车查询。");
        alert.showAndWait();
    }

    // ==================== 查询逻辑（模拟数据）====================

    private void doForwardQuery(String code) {
        // 模拟：根据码值判断层级，生成示例下级数据
        String[] codeInfo = resolveCodeInfo(code);
        if (codeInfo == null) {
            setQueryStatus(false, "查询状态：码不存在");
            showEmpty();
            return;
        }

        setQueryStatus(true, "查询状态：正向查询成功");

        fwdCodeValue.setText(code);
        fwdCodeType.setText(codeInfo[0]);
        fwdCodeType.getStyleClass().removeAll(
                "sw2-badge-green", "sw2-badge-blue", "sw2-badge-orange", "sw2-code-type-pallet",
                "sw2-code-type-case", "sw2-code-type-box", "sw2-code-type-bottle");
        fwdCodeType.getStyleClass().add(codeInfo[1]);
        fwdCodeTime.setText(codeInfo[2]);

        fwdResultTitle.setText("正向查询结果（直接下级 - " + codeInfo[3] + "）：");

        ObservableList<String[]> rows = FXCollections.observableArrayList(
                buildSampleForwardData(code, codeInfo[0]));
        fwdTable.setItems(rows);
        fwdCountLabel.setText(codeInfo[3] + "列表（共" + rows.size() + "个）");

        showForwardResult();
    }

    private void doReverseQuery(String code) {
        String[] codeInfo = resolveCodeInfo(code);
        if (codeInfo == null) {
            setQueryStatus(false, "查询状态：码不存在");
            showEmpty();
            return;
        }

        setQueryStatus(true, "查询状态：反向查询成功");

        revCodeValue.setText(code);
        revCodeType.setText(codeInfo[0]);
        revCodeType.getStyleClass().removeAll(
                "sw2-badge-green", "sw2-badge-blue", "sw2-badge-orange", "sw2-code-type-pallet",
                "sw2-code-type-case", "sw2-code-type-box", "sw2-code-type-bottle");
        revCodeType.getStyleClass().add(codeInfo[1]);
        revCodeTime.setText(codeInfo[2]);

        buildChain(code, codeInfo[0]);

        siblingTitle.setText("同级数据（当前" + codeInfo[0] + "内的其他" + codeInfo[4] + "）：");
        ObservableList<String[]> siblings = FXCollections.observableArrayList(
                buildSampleSiblings(code, codeInfo[0]));
        siblingTable.setItems(siblings);

        showReverseResult();
    }

    // ==================== 链路展示 ====================

    private void buildChain(String code, String level) {
        chainBox.getChildren().clear();
        List<String[]> chain = new ArrayList<>();

        switch (level) {
            case "瓶码" -> {
                chain.add(new String[]{"垛码", "sw2-code-type-pallet", "P20241201001"});
                chain.add(new String[]{"箱码", "sw2-code-type-case",   "20241201002001"});
                chain.add(new String[]{"盒码", "sw2-code-type-box",    "20241201001002"});
            }
            case "盒码" -> {
                chain.add(new String[]{"垛码", "sw2-code-type-pallet", "P20241201001"});
                chain.add(new String[]{"箱码", "sw2-code-type-case",   "20241201002001"});
            }
            case "箱码" -> {
                chain.add(new String[]{"垛码", "sw2-code-type-pallet", "P20241201001"});
            }
            default -> {
                Label none = new Label("—（已是最高层级）");
                none.setStyle("-fx-text-fill:#9CA3AF; -fx-font-size:14px;");
                chainBox.getChildren().add(none);
                return;
            }
        }

        for (int i = 0; i < chain.size(); i++) {
            String[] node = chain.get(i);
            Label typeLabel = new Label(node[0]);
            typeLabel.getStyleClass().add(node[1]);
            Label valLabel = new Label(node[2]);
            valLabel.getStyleClass().add("sw2-code-value-label");
            HBox nodeBox = new HBox(4, typeLabel, valLabel);
            nodeBox.setAlignment(Pos.CENTER_LEFT);
            chainBox.getChildren().add(nodeBox);
            if (i < chain.size() - 1) {
                Label arrow = new Label("→");
                arrow.getStyleClass().add("sw2-chain-arrow");
                chainBox.getChildren().add(arrow);
            }
        }
        // 最后添加查询码本身
        if (!chain.isEmpty()) {
            Label arrow = new Label("→");
            arrow.getStyleClass().add("sw2-chain-arrow");
            Label typeLabel = new Label(level);
            typeLabel.getStyleClass().add(resolveCodeInfo(code)[1]);
            Label valLabel = new Label(code);
            valLabel.getStyleClass().add("sw2-code-value-label");
            HBox nodeBox = new HBox(4, typeLabel, valLabel);
            nodeBox.setAlignment(Pos.CENTER_LEFT);
            chainBox.getChildren().addAll(arrow, nodeBox);
        }
    }

    // ==================== 状态显示 ====================

    private void setQueryStatus(boolean ok, String msg) {
        queryStatusLabel.setText(msg);
        queryStatusLabel.getStyleClass().removeAll(
                "sw2-query-status-ok", "sw2-query-status-err", "sw2-query-status-idle");
        queryStatusLabel.getStyleClass().add(ok ? "sw2-query-status-ok" : "sw2-query-status-err");
    }

    private void showEmpty() {
        emptyPane.setVisible(true);       emptyPane.setManaged(true);
        forwardResultPane.setVisible(false); forwardResultPane.setManaged(false);
        reverseResultPane.setVisible(false); reverseResultPane.setManaged(false);
    }

    private void showForwardResult() {
        emptyPane.setVisible(false);      emptyPane.setManaged(false);
        forwardResultPane.setVisible(true);  forwardResultPane.setManaged(true);
        reverseResultPane.setVisible(false); reverseResultPane.setManaged(false);
    }

    private void showReverseResult() {
        emptyPane.setVisible(false);       emptyPane.setManaged(false);
        forwardResultPane.setVisible(false);  forwardResultPane.setManaged(false);
        reverseResultPane.setVisible(true);   reverseResultPane.setManaged(true);
    }

    // ==================== 模拟数据 ====================

    /** 根据码值猜测层级信息 [层级名, CSS类, 采集时间, 下级层级名, 同级层级名] */
    private String[] resolveCodeInfo(String code) {
        if (code.startsWith("P"))
            return new String[]{"垛码", "sw2-code-type-pallet", "2024-12-01 10:00:00", "箱码", "箱码"};
        if (code.length() == 14)
            return new String[]{"箱码", "sw2-code-type-case",   "2024-12-01 09:30:15", "盒码", "盒码"};
        if (code.length() == 12)
            return new String[]{"盒码", "sw2-code-type-box",    "2024-12-01 08:45:10", "瓶码", "瓶码"};
        if (code.length() >= 10)
            return new String[]{"瓶码", "sw2-code-type-bottle", "2024-12-01 08:30:15", "—",  "瓶码"};
        return null;
    }

    private List<String[]> buildSampleForwardData(String code, String level) {
        List<String[]> rows = new ArrayList<>();
        int count = "垛码".equals(level) ? 3 : "箱码".equals(level) ? 4 : 6;
        for (int i = 1; i <= count; i++) {
            rows.add(new String[]{String.valueOf(i), code + "00" + i, "2024-12-01 10:0" + i + ":00"});
        }
        return rows;
    }

    private List<String[]> buildSampleSiblings(String code, String level) {
        List<String[]> rows = new ArrayList<>();
        int count = "盒码".equals(level) ? 3 : "箱码".equals(level) ? 69 : 2;
        for (int i = 1; i <= Math.min(count, 5); i++) {
            rows.add(new String[]{code.substring(0, code.length() - 1) + i,
                    "2024-12-01 08:3" + i + ":00"});
        }
        return rows;
    }
}
