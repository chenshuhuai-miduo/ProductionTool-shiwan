package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.code.CodeReplaceRequest;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 石湾M2-数据替换控制器
 */
public class ShiwanM2DataReplaceController {
    private static volatile ShiwanM2DataReplaceController instance;

    public static ShiwanM2DataReplaceController getInstance() {
        return instance;
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TextField originalCodeField;
    @FXML
    private TextField newCodeField;
    @FXML
    private TextArea reasonTextArea;
    @FXML
    private ListView<String> replaceResultList;
    @FXML
    private Label replaceStatusLabel;

    private final ObservableList<String> replaceResults = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;
        replaceResultList.setItems(replaceResults);
        replaceResultList.setPlaceholder(new Label("暂无替换记录"));
        replaceStatusLabel.setText("请填写原码和新码后执行替换");
    }

    /**
     * 扫码枪输入入口：先填原码，再填新码；两者都有时覆盖新码。
     */
    public void onScanCode(String code) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) return;
        Platform.runLater(() -> {
            if (originalCodeField == null || newCodeField == null) return;
            String oldVal = originalCodeField.getText() == null ? "" : originalCodeField.getText().trim();
            String newVal = newCodeField.getText() == null ? "" : newCodeField.getText().trim();
            if (oldVal.isEmpty()) {
                originalCodeField.setText(c);
                newCodeField.requestFocus();
            } else if (newVal.isEmpty()) {
                newCodeField.setText(c);
            } else {
                newCodeField.setText(c);
            }
        });
    }

    @FXML
    private void onClearReplaceForm() {
        originalCodeField.clear();
        newCodeField.clear();
        reasonTextArea.clear();
        replaceStatusLabel.setText("表单已清空");
    }

    @FXML
    private void onConfirmReplace() {
        String oldCode = originalCodeField.getText() == null ? "" : originalCodeField.getText().trim();
        String newCode = newCodeField.getText() == null ? "" : newCodeField.getText().trim();
        String reason = reasonTextArea.getText() == null ? "" : reasonTextArea.getText().trim();

        if (oldCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入原码");
            return;
        }
        if (newCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入新码");
            return;
        }
        if (oldCode.equals(newCode)) {
            showAlert(Alert.AlertType.WARNING, "提示", "原码和新码不能相同");
            return;
        }

        // 直接弹出确认对话框，云端接口负责判断是否允许替换
        final String finalOldCode = oldCode;
        final String finalNewCode = newCode;
        final String finalReason  = reason;
        if (!showReplacePasswordConfirm(finalOldCode, finalNewCode, finalReason)) {
            return;
        }
        replaceStatusLabel.setText("替换请求处理中...");
        doReplaceAsync(finalOldCode, finalNewCode, finalReason);
    }

    /**
     * 查询该码是否已上传。复用取消关联的 check-cancel 接口获取 isUploaded 字段。
     * @return true=已上传；false=未上传或查询失败（失败时保守放行，由后端再做校验）
     */
    private boolean checkCodeUploaded(String code) {
        try {
            String enc = java.net.URLEncoder.encode(code, "UTF-8");
            String resp = HttpUtil.doGet("/api/shiwan-m2/code/check-cancel?code=" + enc);
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp);
            if (root != null && root.has("code") && root.get("code").asInt() == 200
                    && root.has("data") && !root.get("data").isNull()) {
                com.fasterxml.jackson.databind.JsonNode data = root.get("data");
                return data.has("isUploaded") && data.get("isUploaded").asBoolean(false);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void doReplaceAsync(String oldCode, String newCode, String reason) {
        new Thread(() -> {
            try {
                CodeReplaceRequest request = new CodeReplaceRequest();
                request.setOldCode(oldCode);
                request.setNewCode(newCode);
                request.setReason(reason);

                String response = HttpUtil.doPost("/api/shiwan-m2/code/replace", request);
                ApiResult<Boolean> result = HttpUtil.parseJson(response, new TypeReference<ApiResult<Boolean>>() {});

                Platform.runLater(() -> handleReplaceResult(oldCode, newCode, reason, result));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appendReplaceResult(false, oldCode, newCode, "请求异常: " + e.getMessage());
                    replaceStatusLabel.setText("替换失败");
                });
            }
        }, "shiwan-m2-data-replace").start();
    }

    private void handleReplaceResult(String oldCode, String newCode, String reason, ApiResult<Boolean> result) {
        boolean success = result != null && result.getCode() == 200 && Boolean.TRUE.equals(result.getData());
        if (success) {
            appendReplaceResult(true, oldCode, newCode, "");
            replaceStatusLabel.setText("替换成功");
            onClearReplaceForm();

            OperateLogBuilder.create()
                    .module(ModuleNameEnum.CODE_REPLACE)
                    .operateType(OperateTypeEnum.REPLACE)
                    .target(oldCode, newCode)
                    .content("石湾M2数据替换: " + oldCode + " -> " + newCode)
                    .beforeData(oldCode)
                    .afterData(newCode)
                    .remark(reason)
                    .deviceInfo("石湾M2-数据替换")
                    .saveAsync();
            return;
        }

        String message = result == null ? "后端无响应" : result.getMessage();
        appendReplaceResult(false, oldCode, newCode, message);
        replaceStatusLabel.setText("替换失败: " + message);

        OperateLogBuilder.create()
                .module(ModuleNameEnum.CODE_REPLACE)
                .operateType(OperateTypeEnum.REPLACE)
                .target(oldCode, newCode)
                .content("石湾M2数据替换失败: " + oldCode + " -> " + newCode)
                .failReason(message)
                .deviceInfo("石湾M2-数据替换")
                .saveAsync();
    }

    private void appendReplaceResult(boolean success, String oldCode, String newCode, String message) {
        String now = LocalDateTime.now().format(TIME_FMT);
        String line;
        if (success) {
            line = now + "  ✓ 替换成功: " + oldCode + " -> " + newCode;
        } else {
            line = now + "  ✗ 替换失败: " + oldCode + " -> " + newCode + "，原因: " + message;
        }
        replaceResults.add(0, line);
    }

    private boolean showReplacePasswordConfirm(String oldCode, String newCode, String reason) {
        ShiwanM2Settings settings = ShiwanM2SettingsStore.load();
        String configPassword = settings.getSystemSettingsPassword();
        if (configPassword == null || configPassword.isEmpty()) {
            configPassword = "123456";
        }
        final String expectedPassword = configPassword;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("确认码替换");
        // 不使用系统 headerText，由内容区自定义标题
        dialog.setHeaderText(null);

        ButtonType confirmType = new ButtonType("确认替换", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType  = new ButtonType("取消",    ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, cancelType);

        // ---- 标题行 ----
        Label titleLabel = new Label("确认码替换");
        titleLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#1F2937;");

        // ---- 确认提示 ----
        Label confirmHint = new Label("请确认替换信息：");
        confirmHint.setStyle("-fx-font-size:14px; -fx-text-fill:#374151;");

        // ---- 信息盒（灰底圆角） ----
        VBox infoBox = new VBox(4);
        infoBox.setStyle("-fx-background-color:#F9FAFB; -fx-border-radius:8; " +
                "-fx-background-radius:8; -fx-padding:14; -fx-line-spacing:4;");
        infoBox.getChildren().add(infoRow("原码值：", oldCode));
        infoBox.getChildren().add(infoRow("新码值：", newCode));
        if (reason != null && !reason.isEmpty()) {
            infoBox.getChildren().add(infoRow("替换原因：", reason));
        }

        // ---- 密码区 ----
        Label pwdTitle = new Label("* 请输入密码");
        pwdTitle.setStyle("-fx-font-size:14px; -fx-font-weight:600; -fx-text-fill:#DC2626;");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("请输入密码");
        pwdField.setStyle("-fx-min-height:44px; -fx-font-size:14px; -fx-border-radius:6; " +
                "-fx-background-radius:6; -fx-border-color:#D1D5DB; -fx-border-width:1px;");
        Label pwdErr = new Label();
        pwdErr.setStyle("-fx-text-fill:#DC2626; -fx-font-size:13px; -fx-min-height:18px;");

        // ---- 不可恢复警告 ----
        Label warnText = new Label("⚠ 此操作不可恢复，请仔细核对后确认！");
        warnText.setStyle("-fx-text-fill:#DC2626; -fx-font-size:14px; -fx-font-weight:600;");

        VBox content = new VBox(14,
                titleLabel,
                confirmHint,
                infoBox,
                new VBox(6, pwdTitle, pwdField, pwdErr),
                warnText);
        content.setPrefWidth(400);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-padding:0;");

        // 按钮样式
        Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmBtn.setStyle("-fx-background-color:#DC2626; -fx-text-fill:white; " +
                "-fx-font-weight:bold; -fx-min-width:100px; -fx-min-height:44px; -fx-font-size:14px;");
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelBtn.setStyle("-fx-background-color:#6B7280; -fx-text-fill:white; " +
                "-fx-min-width:80px; -fx-min-height:44px; -fx-font-size:14px;");

        // 点击确认时验证密码，错误则不关闭
        confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (!expectedPassword.equals(pwdField.getText())) {
                pwdErr.setText("密码错误，请重新输入");
                pwdField.clear();
                ev.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirmType;
    }

    /** 构建信息行：加粗标签 + 普通值，横向排列 */
    private static HBox infoRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:14px; -fx-font-weight:600; -fx-text-fill:#374151;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:14px; -fx-text-fill:#374151;");
        val.setWrapText(true);
        HBox row = new HBox(0, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }
}
