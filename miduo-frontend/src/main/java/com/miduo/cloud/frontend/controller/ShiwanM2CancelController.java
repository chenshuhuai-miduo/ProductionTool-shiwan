package com.miduo.cloud.frontend.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 取消关联 Tab 控制器
 * <p>
 * 支持单码取消（输入瓶/盒码）和多码取消（批量输入盒/箱/垛码）。
 * 识别后右侧显示识别结果和取消结果。
 * 需要密码 123456 确认。
 * </p>
 */
public class ShiwanM2CancelController implements Initializable {

    private static final String OPERATOR_PASSWORD = "123456";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ==================== FXML 注入 ====================

    @FXML private Button singleModeBtn;
    @FXML private Button batchModeBtn;

    // 单码
    @FXML private VBox      singlePane;
    @FXML private TextField singleCodeField;
    @FXML private Button    singleConfirmBtn;
    @FXML private Label     singleHintLabel;

    // 多码
    @FXML private VBox      batchPane;
    @FXML private TextField batchCodeField;
    @FXML private ListView<String> batchCodeList;

    // 右侧
    @FXML private Label      recognizeHint;
    @FXML private ScrollPane recognizeScrollPane;
    @FXML private VBox       recognizeContainer;
    @FXML private Label      recognizeEmptyLabel;
    @FXML private ListView<String> cancelResultList;

    // ==================== 内部状态 ====================

    private boolean isSingleMode    = true;
    private boolean singleRecognized = false;

    private final ObservableList<String> batchCodes  = FXCollections.observableArrayList();
    private final ObservableList<String> cancelResults = FXCollections.observableArrayList();

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        batchCodeList.setItems(batchCodes);
        cancelResultList.setItems(cancelResults);

        singleCodeField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onSingleRecognize();
        });
    }

    // ==================== 模式切换 ====================

    @FXML
    private void onSingleMode() {
        if (isSingleMode) return;
        isSingleMode = true;

        singleModeBtn.getStyleClass().add("sw2-mode-btn-active");
        batchModeBtn.getStyleClass().remove("sw2-mode-btn-active");

        singlePane.setVisible(true); singlePane.setManaged(true);
        batchPane.setVisible(false); batchPane.setManaged(false);
    }

    @FXML
    private void onBatchMode() {
        if (!isSingleMode) return;
        isSingleMode = false;

        batchModeBtn.getStyleClass().add("sw2-mode-btn-active");
        singleModeBtn.getStyleClass().remove("sw2-mode-btn-active");

        batchPane.setVisible(true); batchPane.setManaged(true);
        singlePane.setVisible(false); singlePane.setManaged(false);
    }

    @FXML
    private void onHelp() {
        showInfo("取消关联说明",
                "单码取消：输入1个下级码（瓶/盒），识别后系统显示其上级关联链路。\n" +
                "  有上级关联时必须先从垛开始逐级解除；无上级后方可取消。\n\n" +
                "多码取消：输入盒/箱/垛码，可添加多个混合层级，识别后批量取消。\n\n" +
                "通用：取消顺序垛→箱→盒→瓶；已上传须先取消云端；操作不可逆，需密码 123456。");
    }

    // ==================== 单码识别 ====================

    @FXML
    private void onSingleRecognize() {
        String code = singleCodeField.getText().trim();
        if (code.isEmpty()) return;

        recognizeEmptyLabel.setVisible(false);
        recognizeEmptyLabel.setManaged(false);
        recognizeContainer.getChildren().clear();

        RecognizeResult result = simulateRecognize(code);
        VBox card = buildRecognizeCard(result);
        recognizeContainer.getChildren().add(card);
        recognizeHint.setText("");

        singleRecognized = true;
        boolean canConfirm = !result.hasParent;
        singleConfirmBtn.setDisable(!canConfirm);

        if (result.hasParent) {
            singleHintLabel.setText("⚠ 有上级关联，请先从垛开始逐级解除");
            singleHintLabel.setVisible(true);
            singleHintLabel.setManaged(true);
        } else {
            singleHintLabel.setVisible(false);
            singleHintLabel.setManaged(false);
        }
    }

    @FXML
    private void onSingleClear() {
        singleCodeField.clear();
        singleConfirmBtn.setDisable(true);
        singleRecognized = false;
        singleHintLabel.setVisible(false);
        singleHintLabel.setManaged(false);
        clearRecognizePane();
    }

    @FXML
    private void onSingleConfirm() {
        String code = singleCodeField.getText().trim();
        if (!singleRecognized || code.isEmpty()) return;
        openConfirmDialog(List.of(code));
    }

    // ==================== 多码操作 ====================

    @FXML
    private void onAddBatchCode() {
        String code = batchCodeField.getText().trim();
        if (code.isEmpty()) return;
        if (!batchCodes.contains(code)) {
            batchCodes.add(code);
        }
        batchCodeField.clear();
    }

    @FXML
    private void onBatchRecognize() {
        if (batchCodes.isEmpty()) {
            showWarn("请先添加码", "请先添加需要取消关联的码。");
            return;
        }
        recognizeEmptyLabel.setVisible(false);
        recognizeEmptyLabel.setManaged(false);
        recognizeContainer.getChildren().clear();

        for (String code : batchCodes) {
            RecognizeResult result = simulateRecognize(code);
            VBox card = buildRecognizeCard(result);
            recognizeContainer.getChildren().add(card);
        }

        // 总影响范围
        int total = batchCodes.size();
        Label summary = new Label("总影响范围：" + total + " 个码单元");
        summary.setStyle("-fx-font-size:13px; -fx-text-fill:#DC2626; -fx-font-weight:bold; -fx-padding:8px 0;");
        recognizeContainer.getChildren().add(summary);
        recognizeHint.setText("");
    }

    @FXML
    private void onBatchClear() {
        batchCodes.clear();
        clearRecognizePane();
    }

    @FXML
    private void onBatchConfirm() {
        if (batchCodes.isEmpty()) {
            showWarn("请先添加码", "请先添加并识别需要取消关联的码。");
            return;
        }
        openConfirmDialog(new ArrayList<>(batchCodes));
    }

    // ==================== 确认弹窗 ====================

    private void openConfirmDialog(List<String> codes) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("取消关联确认");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK)
              .setStyle("-fx-background-color:#DC2626; -fx-text-fill:white; -fx-font-weight:bold;");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL)
              .setStyle("-fx-background-color:#6B7280; -fx-text-fill:white;");

        PasswordField pwd = new PasswordField();
        pwd.setPromptText("请输入密码");
        pwd.setStyle("-fx-border-radius:6px; -fx-background-radius:6px; -fx-border-color:#D1D5DB; " +
                     "-fx-border-width:1px; -fx-min-height:40px; -fx-padding:0 12px;");

        VBox infoBox = new VBox(4);
        infoBox.setStyle("-fx-background-color:#FEF3C7; -fx-border-radius:8px; " +
                         "-fx-background-radius:8px; -fx-padding:12px;");
        infoBox.getChildren().add(new Label("包装信息：" + String.join("、", codes)));
        infoBox.getChildren().add(new Label("将取消该包装内的所有关联关系"));

        Label warn = new Label("⚠ 此操作不可恢复，请仔细核对后确认！");
        warn.setStyle("-fx-text-fill:#DC2626; -fx-font-size:13px; -fx-font-weight:bold;");

        Label pwdLabel = new Label("* 密码");
        pwdLabel.setStyle("-fx-font-weight:bold;");

        VBox content = new VBox(12, pwdLabel, pwd, infoBox, warn);
        content.setPrefWidth(360);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (!OPERATOR_PASSWORD.equals(pwd.getText())) {
                showWarn("密码错误", "密码不正确，取消关联操作已取消。");
                return;
            }
            executeCancelAssociation(codes);
        }
    }

    // ==================== 取消执行 ====================

    private void executeCancelAssociation(List<String> codes) {
        String time = LocalDateTime.now().format(TIME_FMT);
        for (String code : codes) {
            cancelResults.add(0, time + " ✓ " + code + " 解除关联成功");
        }
        if (isSingleMode) {
            singleCodeField.clear();
            singleConfirmBtn.setDisable(true);
            singleRecognized = false;
        } else {
            batchCodes.clear();
        }
        clearRecognizePane();
    }

    // ==================== 右侧结果 ====================

    @FXML
    private void onClearCancelLog() {
        cancelResults.clear();
    }

    private void clearRecognizePane() {
        recognizeContainer.getChildren().clear();
        recognizeEmptyLabel.setVisible(true);
        recognizeEmptyLabel.setManaged(true);
        recognizeContainer.getChildren().add(recognizeEmptyLabel);
        recognizeHint.setText("识别后将在此显示");
    }

    // ==================== 识别结果构建 ====================

    private VBox buildRecognizeCard(RecognizeResult result) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));

        if (result.canCancel) {
            card.setStyle("-fx-background-color:#F0FDF4; -fx-border-color: #86EFAC transparent transparent #16A34A; " +
                          "-fx-border-width: 0 0 0 4px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
            Label title = new Label(result.codeType + " " + result.code + "，可取消关联，解除" + result.associateCount + "个");
            title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#15803D;");
            card.getChildren().add(title);
        } else {
            card.setStyle("-fx-background-color:#FEF2F2; -fx-border-color: #FCA5A5 transparent transparent #DC2626; " +
                          "-fx-border-width: 0 0 0 4px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
            Label title = new Label(result.codeType + " " + result.code + "，不可取消关联");
            title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#DC2626;");
            Label hint  = new Label("⚠ （" + result.hint + ")");
            hint.setStyle("-fx-font-size:13px; -fx-text-fill:#DC2626;");
            Label chain = new Label("链路：" + result.chain);
            chain.setStyle("-fx-font-size:13px; -fx-text-fill:#6B7280;");
            card.getChildren().addAll(title, hint, chain);
        }
        return card;
    }

    // ==================== 模拟识别 ====================

    private RecognizeResult simulateRecognize(String code) {
        RecognizeResult r = new RecognizeResult();
        r.code = code;
        if (code.startsWith("P")) {
            r.codeType = "垛码";
            r.hasParent = false;
            r.canCancel = true;
            r.associateCount = 70;
            r.chain = "—";
        } else if (code.length() == 14) {
            r.codeType = "箱码";
            r.hasParent = true;
            r.canCancel = false;
            r.hint = "箱已关联至垛，请先解除上级";
            r.chain = "垛码xxx→箱码" + code;
        } else if (code.length() == 12) {
            r.codeType = "盒码";
            r.hasParent = false;
            r.canCancel = true;
            r.associateCount = 6;
            r.chain = "盒码" + code;
        } else {
            r.codeType = "瓶码";
            r.hasParent = false;
            r.canCancel = true;
            r.associateCount = 1;
            r.chain = "瓶码" + code;
        }
        return r;
    }

    // ==================== 工具 ====================

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarn(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==================== 数据模型 ====================

    private static class RecognizeResult {
        String  code;
        String  codeType;
        boolean hasParent;
        boolean canCancel;
        int     associateCount;
        String  hint  = "";
        String  chain = "";
    }
}
