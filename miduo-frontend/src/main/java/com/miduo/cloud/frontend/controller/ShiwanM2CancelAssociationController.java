package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 石湾M2-取消关联控制器
 * 说明：当前版本基于已有接口实现（/api/code/query、/api/code/delete-by-box-code）。
 */
public class ShiwanM2CancelAssociationController {

    private static final String FIXED_PASSWORD = "123456";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TextField singleCodeField;
    @FXML
    private Button singleConfirmCancelButton;
    @FXML
    private TextField multiCodeField;
    @FXML
    private ListView<String> multiCodeList;
    @FXML
    private Button multiConfirmCancelButton;
    @FXML
    private Label identifySummaryLabel;
    @FXML
    private ListView<String> identifyResultList;
    @FXML
    private ListView<String> cancelResultList;

    private String singleIdentifiedCode;
    private boolean singleCanCancel;
    private final ObservableList<String> multiCodes = FXCollections.observableArrayList();
    private final ObservableList<String> identifyResults = FXCollections.observableArrayList();
    private final ObservableList<String> cancelResults = FXCollections.observableArrayList();
    private final Map<String, QueryCheckResult> multiIdentifyState = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        multiCodeList.setItems(multiCodes);
        identifyResultList.setItems(identifyResults);
        cancelResultList.setItems(cancelResults);

        multiCodeList.setPlaceholder(new Label("暂无添加的码"));
        identifyResultList.setPlaceholder(new Label("请先执行识别"));
        cancelResultList.setPlaceholder(new Label("暂无取消关联记录"));

        singleConfirmCancelButton.setDisable(true);
        multiConfirmCancelButton.setDisable(true);
        identifySummaryLabel.setText("识别结果：等待输入");

        initMultiCodeCellFactory();
    }

    private void initMultiCodeCellFactory() {
        multiCodeList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label text = new Label(item);
                text.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(text, Priority.ALWAYS);

                Button delete = new Button("删除");
                delete.getStyleClass().add("shiwan-m2-mini-delete-btn");
                delete.setOnAction(evt -> {
                    multiCodes.remove(item);
                    multiIdentifyState.remove(item);
                    updateMultiConfirmButtonState();
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox row = new HBox(8, text, spacer, delete);
                setGraphic(row);
                setText(null);
            }
        });
    }

    @FXML
    private void onShowCancelHelp() {
        showAlert(Alert.AlertType.INFORMATION, "取消关联说明",
                "单码模式：输入1个码并识别后取消。\n" +
                        "多码模式：可添加多个码批量识别和取消。\n" +
                        "本页面当前基于已有接口实现，取消接口使用 /api/code/delete-by-box-code。\n" +
                        "取消操作需输入固定密码 123456。");
    }

    @FXML
    private void onSingleClear() {
        singleCodeField.clear();
        singleIdentifiedCode = null;
        singleCanCancel = false;
        singleConfirmCancelButton.setDisable(true);
        identifySummaryLabel.setText("识别结果：等待输入");
    }

    @FXML
    private void onSingleIdentify() {
        String code = normalize(singleCodeField.getText());
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入待识别的码");
            return;
        }

        identifySummaryLabel.setText("识别中...");
        singleConfirmCancelButton.setDisable(true);
        doIdentifyAsync(List.of(code), true);
    }

    @FXML
    private void onSingleConfirmCancel() {
        if (!singleCanCancel || singleIdentifiedCode == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先识别可取消的码");
            return;
        }

        String detail = "单码取消关联\n目标码：" + singleIdentifiedCode + "\n此操作不可恢复。";
        if (!showPasswordConfirm("取消关联确认", detail)) {
            return;
        }

        doCancelAsync(List.of(singleIdentifiedCode), true);
    }

    @FXML
    private void onMultiAddCode() {
        String code = normalize(multiCodeField.getText());
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入盒/箱/垛码");
            return;
        }
        if (multiCodes.contains(code)) {
            showAlert(Alert.AlertType.WARNING, "提示", "该码已在列表中");
            return;
        }
        multiCodes.add(code);
        multiCodeField.clear();
        multiIdentifyState.remove(code);
        updateMultiConfirmButtonState();
    }

    @FXML
    private void onMultiClear() {
        multiCodeField.clear();
        multiCodes.clear();
        multiIdentifyState.clear();
        multiConfirmCancelButton.setDisable(true);
        identifySummaryLabel.setText("识别结果：等待输入");
    }

    @FXML
    private void onMultiIdentify() {
        if (multiCodes.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先添加要识别的码");
            return;
        }
        identifySummaryLabel.setText("批量识别中...");
        multiConfirmCancelButton.setDisable(true);
        doIdentifyAsync(new ArrayList<>(multiCodes), false);
    }

    @FXML
    private void onMultiConfirmCancel() {
        List<String> canCancelCodes = new ArrayList<>();
        for (Map.Entry<String, QueryCheckResult> entry : multiIdentifyState.entrySet()) {
            if (entry.getValue().cancelable) {
                canCancelCodes.add(entry.getKey());
            }
        }
        if (canCancelCodes.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "无可取消的码，请先识别");
            return;
        }

        String detail = "批量取消关联\n可取消数量：" + canCancelCodes.size() + "\n此操作不可恢复。";
        if (!showPasswordConfirm("取消关联确认", detail)) {
            return;
        }
        doCancelAsync(canCancelCodes, false);
    }

    private void doIdentifyAsync(List<String> codes, boolean singleMode) {
        new Thread(() -> {
            Map<String, QueryCheckResult> resultMap = new LinkedHashMap<>();
            for (String code : codes) {
                resultMap.put(code, queryCancelable(code));
            }
            Platform.runLater(() -> applyIdentifyResult(resultMap, singleMode));
        }, "shiwan-m2-cancel-identify").start();
    }

    private void applyIdentifyResult(Map<String, QueryCheckResult> resultMap, boolean singleMode) {
        int canCancelCount = 0;
        int affectedCount = 0;
        for (Map.Entry<String, QueryCheckResult> entry : resultMap.entrySet()) {
            String code = entry.getKey();
            QueryCheckResult result = entry.getValue();
            if (result.cancelable) {
                canCancelCount++;
                affectedCount += result.affectedCount;
            }
            identifyResults.add(0, buildIdentifyLine(code, result));
        }

        if (singleMode) {
            QueryCheckResult singleResult = resultMap.values().iterator().next();
            String code = resultMap.keySet().iterator().next();
            singleCanCancel = singleResult.cancelable;
            singleIdentifiedCode = singleCanCancel ? code : null;
            singleConfirmCancelButton.setDisable(!singleCanCancel);
            identifySummaryLabel.setText(singleCanCancel
                    ? "识别结果：可取消1个，影响 " + singleResult.affectedCount + " 条关系"
                    : "识别结果：不可取消，原因：" + singleResult.message);
            return;
        }

        multiIdentifyState.clear();
        multiIdentifyState.putAll(resultMap);
        identifySummaryLabel.setText("识别结果：可取消 " + canCancelCount + " 个，影响 " + affectedCount + " 条关系");
        updateMultiConfirmButtonState();
    }

    private void updateMultiConfirmButtonState() {
        for (QueryCheckResult value : multiIdentifyState.values()) {
            if (value.cancelable) {
                multiConfirmCancelButton.setDisable(false);
                return;
            }
        }
        multiConfirmCancelButton.setDisable(true);
    }

    private String buildIdentifyLine(String code, QueryCheckResult result) {
        String now = LocalDateTime.now().format(TIME_FMT);
        if (result.cancelable) {
            return now + "  ✓ " + code + " 可取消关联，预计影响 " + result.affectedCount + " 条关系";
        }
        return now + "  ✗ " + code + " 不可取消，原因：" + result.message;
    }

    private void doCancelAsync(List<String> codes, boolean singleMode) {
        singleConfirmCancelButton.setDisable(true);
        multiConfirmCancelButton.setDisable(true);
        identifySummaryLabel.setText("取消处理中...");

        new Thread(() -> {
            int success = 0;
            List<String> failedCodes = new ArrayList<>();

            for (String code : codes) {
                DeleteResult result = deleteByBoxCode(code);
                if (result.success) {
                    success++;
                    logCancelSuccess(code);
                } else {
                    failedCodes.add(code);
                    logCancelFailure(code, result.message);
                }

                String line = buildCancelLine(code, result);
                Platform.runLater(() -> cancelResults.add(0, line));
            }

            int total = codes.size();
            int successCount = success;
            List<String> failedCodeSnapshot = new ArrayList<>(failedCodes);
            Platform.runLater(() -> {
                String summary = "取消完成：成功 " + successCount + " / " + total;
                if (!failedCodeSnapshot.isEmpty()) {
                    summary += "，失败码：" + String.join("，", failedCodeSnapshot);
                }
                identifySummaryLabel.setText(summary);

                if (singleMode) {
                    if (successCount > 0) {
                        singleCodeField.clear();
                        singleIdentifiedCode = null;
                        singleCanCancel = false;
                    }
                    singleConfirmCancelButton.setDisable(!singleCanCancel);
                    return;
                }

                if (successCount == total) {
                    multiCodes.clear();
                    multiIdentifyState.clear();
                } else {
                    multiCodes.removeIf(code -> !failedCodeSnapshot.contains(code));
                    multiIdentifyState.entrySet().removeIf(entry -> !failedCodeSnapshot.contains(entry.getKey()));
                }
                updateMultiConfirmButtonState();
            });
        }, "shiwan-m2-cancel-exec").start();
    }

    private String buildCancelLine(String code, DeleteResult result) {
        String now = LocalDateTime.now().format(TIME_FMT);
        if (result.success) {
            return now + "  ✓ " + code + " 取消关联成功";
        }
        return now + "  ✗ " + code + " 取消关联失败，原因：" + result.message;
    }

    private QueryCheckResult queryCancelable(String code) {
        try {
            String response = HttpUtil.doGet("/api/code/query/" + encodePath(code));
            ApiResult<List<Object>> result = HttpUtil.parseJson(response, new TypeReference<ApiResult<List<Object>>>() {});
            if (result != null && result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                return new QueryCheckResult(true, result.getData().size(), "可取消");
            }
            String message = result == null ? "后端无响应" : result.getMessage();
            return new QueryCheckResult(false, 0, message);
        } catch (Exception e) {
            return new QueryCheckResult(false, 0, "识别异常: " + e.getMessage());
        }
    }

    private DeleteResult deleteByBoxCode(String code) {
        try {
            String response = HttpUtil.doDelete("/api/code/delete-by-box-code/" + encodePath(code));
            ApiResult<Boolean> result = HttpUtil.parseJson(response, new TypeReference<ApiResult<Boolean>>() {});
            boolean success = result != null && result.getCode() == 200 && Boolean.TRUE.equals(result.getData());
            if (success) {
                return new DeleteResult(true, result.getMessage());
            }
            String message = result == null ? "后端无响应" : result.getMessage();
            return new DeleteResult(false, message);
        } catch (Exception e) {
            return new DeleteResult(false, "请求异常: " + e.getMessage());
        }
    }

    private void logCancelSuccess(String code) {
        OperateLogBuilder.create()
                .module(ModuleNameEnum.CODE_QUERY)
                .operateType(OperateTypeEnum.DELETE)
                .target(code, "取消关联")
                .content("石湾M2取消关联成功: " + code)
                .deviceInfo("石湾M2-取消关联")
                .saveAsync();
    }

    private void logCancelFailure(String code, String reason) {
        OperateLogBuilder.create()
                .module(ModuleNameEnum.CODE_QUERY)
                .operateType(OperateTypeEnum.DELETE)
                .target(code, "取消关联")
                .content("石湾M2取消关联失败: " + code)
                .failReason(reason)
                .deviceInfo("石湾M2-取消关联")
                .saveAsync();
    }

    private boolean showPasswordConfirm(String title, String detailText) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("请输入密码后确认");
        ButtonType confirmType = new ButtonType("确认取消关联", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        Label detail = new Label(detailText);
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

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class QueryCheckResult {
        private final boolean cancelable;
        private final int affectedCount;
        private final String message;

        private QueryCheckResult(boolean cancelable, int affectedCount, String message) {
            this.cancelable = cancelable;
            this.affectedCount = affectedCount;
            this.message = message;
        }
    }

    private static class DeleteResult {
        private final boolean success;
        private final String message;

        private DeleteResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
