package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * 数据替换 Tab 控制器
 * <p>
 * 支持将原码替换为新码（需密码确认，操作不可逆）。
 * </p>
 */
public class ShiwanM2ReplaceController implements Initializable {

    private static final String OPERATOR_PASSWORD = "123456";
    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== FXML 注入 ====================

    @FXML private TextField origCodeField;
    @FXML private TextField newCodeField;
    @FXML private TextArea  reasonField;

    @FXML private ScrollPane resultScrollPane;
    @FXML private VBox       resultContainer;
    @FXML private Label      emptyResultLabel;

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 扫码枪 Enter 触发
        origCodeField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) newCodeField.requestFocus();
        });
        newCodeField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onConfirmReplace();
        });
    }

    // ==================== 事件处理 ====================

    @FXML
    private void onOrigCodeHelp() {
        showInfo("原码说明", "请输入需要替换的原码，该码必须已在系统中存在关联关系。");
    }

    @FXML
    private void onNewCodeHelp() {
        showInfo("新码说明",
                "新码必须同时满足：\n" +
                "  ✅ 在导入的码包范围内\n" +
                "  ✅ 未被使用过（系统中无关联关系）\n" +
                "  ✅ 格式有效，且不能与原码相同");
    }

    @FXML
    private void onClear() {
        origCodeField.clear();
        newCodeField.clear();
        reasonField.clear();
    }

    @FXML
    private void onConfirmReplace() {
        String orig   = origCodeField.getText().trim();
        String newCode = newCodeField.getText().trim();

        if (orig.isEmpty()) {
            showWarn("请输入原码", "原码不能为空。");
            origCodeField.requestFocus();
            return;
        }
        if (newCode.isEmpty()) {
            showWarn("请输入新码", "新码不能为空。");
            newCodeField.requestFocus();
            return;
        }
        if (orig.equals(newCode)) {
            showWarn("码值相同", "原码与新码不能相同，请重新输入。");
            return;
        }

        // 弹出确认弹窗（含密码）
        openReplaceConfirmDialog(orig, newCode, reasonField.getText().trim());
    }

    // ==================== 替换确认弹窗 ====================

    private void openReplaceConfirmDialog(String orig, String newCode, String reason) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("确认码替换");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color:#DC2626; -fx-text-fill:white; -fx-font-weight:bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label title = new Label("请确认替换信息：");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold;");

        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-background-color:#F3F4F6; -fx-border-radius:6px; -fx-background-radius:6px; -fx-padding:12px;");
        infoBox.getChildren().addAll(
                rowLabel("原码值：", orig),
                rowLabel("新码值：", newCode),
                rowLabel("替换原因：", reason.isEmpty() ? "（未填写）" : reason)
        );

        Label pwdLabel   = new Label("* 密码");
        pwdLabel.setStyle("-fx-font-weight:bold;");
        PasswordField pwd = new PasswordField();
        pwd.setPromptText("请输入系统密码");
        pwd.setStyle("-fx-border-radius:6px; -fx-background-radius:6px; -fx-border-color:#D1D5DB; -fx-border-width:1px; -fx-min-height:40px; -fx-padding:0 12px;");

        Label warn = new Label("⚠ 此操作不可恢复，请仔细核对后确认！");
        warn.setStyle("-fx-text-fill:#DC2626; -fx-font-size:13px; -fx-font-weight:bold;");

        VBox content = new VBox(12, title, infoBox, pwdLabel, pwd, warn);
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (!OPERATOR_PASSWORD.equals(pwd.getText())) {
                showWarn("密码错误", "密码不正确，替换操作已取消。");
                return;
            }
            executeReplace(orig, newCode, reason);
        }
    }

    private Label rowLabel(String key, String val) {
        Label l = new Label(key + val);
        l.setStyle("-fx-font-size:14px;");
        return l;
    }

    // ==================== 替换执行 ====================

    private void executeReplace(String orig, String newCode, String reason) {
        boolean success = simulateReplace(orig, newCode);
        String  time    = LocalDateTime.now().format(DT_FMT);

        emptyResultLabel.setVisible(false);
        emptyResultLabel.setManaged(false);

        VBox card = new VBox(6);
        if (success) {
            card.getStyleClass().add("sw2-replace-result-ok");
            Label titleLbl = new Label("✔ 码替换成功！");
            titleLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#15803D;");
            card.getChildren().addAll(
                    titleLbl,
                    infoLine("原码：", orig),
                    infoLine("新码：", newCode),
                    infoLine("替换原因：", reason.isEmpty() ? "—" : reason),
                    infoLine("操作时间：", time));
        } else {
            card.getStyleClass().add("sw2-replace-result-err");
            Label titleLbl = new Label("✘ 码替换失败！");
            titleLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#DC2626;");
            Label reasonLbl = new Label("失败原因：新码已存在");
            reasonLbl.setStyle("-fx-text-fill:#DC2626; -fx-font-size:13px;");
            card.getChildren().addAll(
                    titleLbl, reasonLbl,
                    infoLine("原码：", orig),
                    infoLine("新码：", newCode),
                    infoLine("操作时间：", time));
        }

        resultContainer.getChildren().add(0, card);
        if (success) {
            origCodeField.clear();
            newCodeField.clear();
            reasonField.clear();
        }
    }

    private Label infoLine(String key, String val) {
        Label l = new Label(key + val);
        l.setStyle("-fx-font-size:14px; -fx-text-fill:#374151;");
        return l;
    }

    private boolean simulateReplace(String orig, String newCode) {
        // 模拟：新码以 "N" 结尾时成功，否则失败
        return newCode.endsWith("N") || newCode.endsWith("n");
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
}
