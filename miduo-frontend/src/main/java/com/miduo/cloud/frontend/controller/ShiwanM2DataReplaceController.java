package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.code.CodeReplaceRequest;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.config.ShiwanM2Settings;
import com.miduo.cloud.frontend.config.ShiwanM2SettingsStore;
import com.miduo.cloud.frontend.util.FxDialog;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 石湾M2-数据替换控制器（主窗口内嵌页：布局与确认弹窗与 {@link ShiwanM2ReplaceController} 对齐，业务走真实接口）。
 */
public class ShiwanM2DataReplaceController {
    private static volatile ShiwanM2DataReplaceController instance;

    public static ShiwanM2DataReplaceController getInstance() {
        return instance;
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private TextField origCodeField;
    @FXML private TextField newCodeField;
    @FXML private TextArea  reasonField;

    @FXML private ScrollPane resultScrollPane;
    @FXML private VBox       resultContainer;
    @FXML private Label      emptyResultLabel;

    private TextField lastFocusedCodeField;

    @FXML
    public void initialize() {
        instance = this;
        bindFocusTracking();

        origCodeField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) newCodeField.requestFocus();
        });
        newCodeField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onConfirmReplace();
        });
    }

    /** 扫码枪：先填原码，再填新码；两者都有时覆盖新码。 */
    public void onScanCode(String code) {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) return;
        Platform.runLater(() -> {
            if (origCodeField == null || newCodeField == null) return;
            TextField target = resolveTargetCodeField();
            if (target != null) {
                target.setText(c);
                return;
            }

            String oldVal = origCodeField.getText() == null ? "" : origCodeField.getText().trim();
            String newVal = newCodeField.getText() == null ? "" : newCodeField.getText().trim();
            if (oldVal.isEmpty()) {
                origCodeField.setText(c);
                newCodeField.requestFocus();
            } else if (newVal.isEmpty()) {
                newCodeField.setText(c);
            } else {
                newCodeField.setText(c);
            }
        });
    }

    private void bindFocusTracking() {
        origCodeField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (Boolean.TRUE.equals(focused)) {
                lastFocusedCodeField = origCodeField;
            }
        });
        newCodeField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (Boolean.TRUE.equals(focused)) {
                lastFocusedCodeField = newCodeField;
            }
        });
    }

    private TextField resolveTargetCodeField() {
        if (origCodeField != null
                && origCodeField.getScene() != null
                && origCodeField.getScene().getFocusOwner() instanceof Node) {
            Node focusOwner = origCodeField.getScene().getFocusOwner();
            if (focusOwner == origCodeField || origCodeField.isFocused()) {
                return origCodeField;
            }
            if (focusOwner == newCodeField || newCodeField.isFocused()) {
                return newCodeField;
            }
        }
        if (lastFocusedCodeField == origCodeField || lastFocusedCodeField == newCodeField) {
            return lastFocusedCodeField;
        }
        return null;
    }

    @FXML
    private void onOrigCodeHelp() {
        FxHelpDialog.show(
                replaceDialogOwner(),
                "原码说明",
                "- **原码**：请输入需要替换的原码，该码必须已在系统中存在关联关系"
        );
    }

    @FXML
    private void onNewCodeHelp() {
        FxHelpDialog.show(
                replaceDialogOwner(),
                "新码说明",
                "- **层级一致**：新码必须与原码属于同一层级（瓶→瓶、盒→盒、箱→箱）",
                "- **码包范围**：新码必须在已导入的对应层级码包范围内",
                "- **未被使用**：新码不能已存在关联关系（系统中无该码的任何关联）",
                "- **不能相同**：新码不能与原码相同"
        );
    }

    @FXML
    private void onClearReplaceForm() {
        origCodeField.clear();
        newCodeField.clear();
        reasonField.clear();
    }

    @FXML
    private void onConfirmReplace() {
        String oldCode = origCodeField.getText() == null ? "" : origCodeField.getText().trim();
        String newCode = newCodeField.getText() == null ? "" : newCodeField.getText().trim();
        String reason  = reasonField.getText() == null ? "" : reasonField.getText().trim();

        if (oldCode.isEmpty()) {
            FxDialog.warn(replaceDialogOwner(), "提示", "请输入原码，原码不能为空。");
            origCodeField.requestFocus();
            return;
        }
        if (newCode.isEmpty()) {
            FxDialog.warn(replaceDialogOwner(), "提示", "请输入新码，新码不能为空。");
            newCodeField.requestFocus();
            return;
        }
        if (oldCode.equals(newCode)) {
            FxDialog.warn(replaceDialogOwner(), "提示", "原码与新码不能相同，请重新输入。");
            return;
        }

        if (!openReplaceConfirmDialog(oldCode, newCode, reason)) {
            return;
        }
        doReplaceAsync(oldCode, newCode, reason);
    }

    /** 与 Replace Tab 相同的 FXML 确认弹窗；密码来自系统设置。 */
    private boolean openReplaceConfirmDialog(String orig, String newCode, String reason) {
        ShiwanM2Settings settings = ShiwanM2SettingsStore.load();
        String configPassword = settings.getSystemSettingsPassword();
        if (configPassword == null || configPassword.isEmpty()) {
            configPassword = "123456";
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ShiwanM2ReplaceConfirmDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2ReplaceConfirmDialogController ctrl = loader.getController();
            ctrl.setReplaceInfo(orig, newCode, reason, configPassword);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            Window owner = replaceDialogOwner();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
            return ctrl.isConfirmed();
        } catch (Exception ex) {
            FxDialog.warn(replaceDialogOwner(), "提示", "无法打开确认弹窗：" + ex.getMessage());
            return false;
        }
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
                    appendResultCard(false, oldCode, newCode, "请求异常: " + e.getMessage(), reason);
                    OperateLogBuilder.create()
                            .module(ModuleNameEnum.CODE_REPLACE)
                            .operateType(OperateTypeEnum.REPLACE)
                            .target(oldCode, newCode)
                            .content("石湾M2数据替换失败: " + oldCode + " -> " + newCode)
                            .failReason("请求异常: " + e.getMessage())
                            .deviceInfo("石湾M2-数据替换")
                            .saveAsync();
                });
            }
        }, "shiwan-m2-data-replace").start();
    }

    private void handleReplaceResult(String oldCode, String newCode, String reason, ApiResult<Boolean> result) {
        boolean success = result != null && result.getCode() == 200 && Boolean.TRUE.equals(result.getData());
        if (success) {
            appendResultCard(true, oldCode, newCode, null, reason);
            origCodeField.clear();
            newCodeField.clear();
            reasonField.clear();

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
        appendResultCard(false, oldCode, newCode, message, reason);

        OperateLogBuilder.create()
                .module(ModuleNameEnum.CODE_REPLACE)
                .operateType(OperateTypeEnum.REPLACE)
                .target(oldCode, newCode)
                .content("石湾M2数据替换失败: " + oldCode + " -> " + newCode)
                .failReason(message)
                .deviceInfo("石湾M2-数据替换")
                .saveAsync();
    }

    /** 与 {@link ShiwanM2ReplaceController#executeReplace} 相同的结果卡片样式。 */
    private void appendResultCard(boolean success, String oldCode, String newCode, String failMessage, String reason) {
        emptyResultLabel.setVisible(false);
        emptyResultLabel.setManaged(false);

        VBox card = new VBox(12);
        card.getStyleClass().add(success ? "sw2-replace-result-ok" : "sw2-replace-result-err");

        String iconColor = success ? "#10B981" : "#EF4444";
        String titleText = success ? "码替换成功！" : "码替换失败！";

        Label iconLbl = new Label(success ? "✔" : "✘");
        iconLbl.setMinSize(28, 28);
        iconLbl.setMaxSize(28, 28);
        iconLbl.setAlignment(javafx.geometry.Pos.CENTER);
        iconLbl.setStyle(String.format(
                "-fx-background-color:%s; -fx-background-radius:14;"
                        + "-fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold;", iconColor));

        Label titleLbl = new Label(titleText);
        titleLbl.setStyle(String.format(
                "-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:%s;", iconColor));

        HBox headerRow = new HBox(8, iconLbl, titleLbl);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox detailBox = new VBox(8);
        if (!success && failMessage != null && !failMessage.isEmpty()) {
            Label failReasonLbl = new Label("失败原因：" + failMessage);
            failReasonLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#DC2626;");
            detailBox.getChildren().add(failReasonLbl);
        }
        detailBox.getChildren().addAll(
                infoLine("原码：", oldCode),
                infoLine("新码：", newCode));
        if (success && reason != null && !reason.isEmpty()) {
            detailBox.getChildren().add(infoLine("替换原因：", reason));
        }
        detailBox.getChildren().add(timeLine("操作时间：", LocalDateTime.now().format(DT_FMT)));

        card.getChildren().addAll(headerRow, detailBox);
        resultContainer.getChildren().add(0, card);
    }

    private static Label infoLine(String key, String val) {
        Label l = new Label(key + val);
        l.setStyle("-fx-font-size:16px; -fx-text-fill:#374151;");
        return l;
    }

    private static Label timeLine(String key, String val) {
        Label l = new Label(key + val);
        l.setStyle("-fx-font-size:16px; -fx-text-fill:#6B7280;");
        return l;
    }

    private Window replaceDialogOwner() {
        return origCodeField != null && origCodeField.getScene() != null
                ? origCodeField.getScene().getWindow()
                : null;
    }
}
