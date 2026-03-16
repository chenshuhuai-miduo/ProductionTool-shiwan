package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.code.CodeReplaceRequest;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
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
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 石湾M2-数据替换控制器
 */
public class ShiwanM2DataReplaceController {

    private static final String FIXED_PASSWORD = "123456";
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
        replaceResultList.setItems(replaceResults);
        replaceResultList.setPlaceholder(new Label("暂无替换记录"));
        replaceStatusLabel.setText("请填写原码和新码后执行替换");
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

        if (!showReplacePasswordConfirm(oldCode, newCode, reason)) {
            return;
        }

        replaceStatusLabel.setText("替换请求处理中...");
        doReplaceAsync(oldCode, newCode, reason);
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("确认码替换");
        dialog.setHeaderText("请确认替换信息并输入密码");

        ButtonType confirmType = new ButtonType("确认替换", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        Label detail = new Label(buildReplaceConfirmText(oldCode, newCode, reason));
        detail.setWrapText(true);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码（固定密码）");
        VBox content = new VBox(10, detail, new Label("*请输入密码"), passwordField);
        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.setDisable(true);
        passwordField.textProperty().addListener((obs, ov, nv) -> confirmButton.setDisable(!FIXED_PASSWORD.equals(nv)));

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirmType;
    }

    private String buildReplaceConfirmText(String oldCode, String newCode, String reason) {
        StringBuilder builder = new StringBuilder();
        builder.append("原码值：").append(oldCode).append('\n');
        builder.append("新码值：").append(newCode).append('\n');
        if (!reason.isEmpty()) {
            builder.append("替换原因：").append(reason).append('\n');
        }
        builder.append('\n');
        builder.append("已上传的码关联将先替换云端再替换本地。").append('\n');
        builder.append("盖码、箱码等有内码的类型，会同步替换云端内码。").append('\n');
        builder.append('\n');
        builder.append("此操作不可恢复，请仔细核对后确认。");
        return builder.toString();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
