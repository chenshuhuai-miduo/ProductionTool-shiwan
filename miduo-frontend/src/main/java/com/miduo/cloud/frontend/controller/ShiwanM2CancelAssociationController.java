package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import java.util.*;

/**
 * 石湾M2 - 取消关联控制器（P02-06）
 * 布局：左侧「输入→加入列表→识别→确认取消」；右侧「识别结果 + 取消记录」。
 */
public class ShiwanM2CancelAssociationController {
    private static volatile ShiwanM2CancelAssociationController instance;

    public static ShiwanM2CancelAssociationController getInstance() {
        return instance;
    }

    private static final String FIXED_PASSWORD = "123456";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ===== FXML 注入 =====
    @FXML private TextField  codeInputField;
    @FXML private ListView<PendingItem>       pendingList;
    @FXML private Button      scopeOneBtn;
    @FXML private Button      scopeAllBtn;
    @FXML private Button      confirmCancelButton;
    @FXML private Label       identifySummaryLabel;
    @FXML private ListView<IdentifyItem>      identifyResultList;
    @FXML private ListView<CancelRecord>      cancelRecordList;

    // ===== 内部状态：取消范围 =====
    private boolean modeAll = false;

    // ===== 内部状态 =====
    /** 待取消列表（保持插入顺序）key=码值 */
    private final LinkedHashMap<String, PendingItem>  pendingMap     = new LinkedHashMap<>();
    /** 最近一次识别结果 key=码值 */
    private final LinkedHashMap<String, IdentifyItem> lastIdentifyMap = new LinkedHashMap<>();

    private final ObservableList<PendingItem>    pendingItems     = FXCollections.observableArrayList();
    private final ObservableList<IdentifyItem>   identifyItems    = FXCollections.observableArrayList();
    private final ObservableList<CancelRecord>   cancelRecordItems = FXCollections.observableArrayList();

    // ===== 初始化 =====

    @FXML
    public void initialize() {
        instance = this;
        pendingList.setItems(pendingItems);
        identifyResultList.setItems(identifyItems);
        cancelRecordList.setItems(cancelRecordItems);

        pendingList.setPlaceholder(new Label("暂无待取消项"));
        identifyResultList.setPlaceholder(new Label("请在左侧输入码值并点击识别"));
        cancelRecordList.setPlaceholder(new Label("暂无取消关联记录"));

        // 自定义 Cell
        pendingList.setCellFactory(lv -> new PendingCell());
        identifyResultList.setCellFactory(lv -> new IdentifyCell());
        cancelRecordList.setCellFactory(lv -> new CancelRecordCell());

        updateScopeStyle();

        // 回车加入列表
        codeInputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onAddToList();
        });
    }

    /**
     * 扫码枪输入入口：自动填入并加入待取消列表。
     */
    public void onScanCode(String code) {
        String c = normalize(code);
        if (c.isEmpty()) return;
        Platform.runLater(() -> {
            if (codeInputField == null || codeInputField.isDisabled()) return;
            codeInputField.setText(c);
            onAddToList();
        });
    }

    // ===== 取消范围切换 =====

    // 两种状态统一 2px 边框 + 固定尺寸，文字样式由 graphic 内的 Label 控制
    private static final String SCOPE_SEL_STYLE =
            "-fx-background-color:#EFF6FF; -fx-border-color:#2563EB; " +
            "-fx-border-width:2; -fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:0 16; -fx-min-height:40; -fx-pref-height:40; " +
            "-fx-min-width:120; -fx-pref-width:120; " +
            "-fx-cursor:hand; -fx-effect:null;";
    private static final String SCOPE_NRM_STYLE =
            "-fx-background-color:white; -fx-border-color:#D1D5DB; " +
            "-fx-border-width:2; -fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:0 16; -fx-min-height:40; -fx-pref-height:40; " +
            "-fx-min-width:120; -fx-pref-width:120; " +
            "-fx-cursor:hand; -fx-effect:null;";

    @FXML
    private void onScopeOne() {
        modeAll = false;
        updateScopeStyle();
        refreshIdentifyDisplay();
    }

    @FXML
    private void onScopeAll() {
        modeAll = true;
        updateScopeStyle();
        refreshIdentifyDisplay();
    }

    private void updateScopeStyle() {
        if (scopeOneBtn != null) {
            scopeOneBtn.setText("");
            scopeOneBtn.setGraphic(makeScopeGraphic("只解一层", !modeAll));
            scopeOneBtn.setStyle(modeAll ? SCOPE_NRM_STYLE : SCOPE_SEL_STYLE);
        }
        if (scopeAllBtn != null) {
            scopeAllBtn.setText("");
            scopeAllBtn.setGraphic(makeScopeGraphic("全部解除", modeAll));
            scopeAllBtn.setStyle(modeAll ? SCOPE_SEL_STYLE : SCOPE_NRM_STYLE);
        }
    }

    /**
     * 构造选项 graphic：用 Region 元素画圆圈 + Label 显示文字，
     * 确保圆圈大小完全由样式控制，与字体无关。
     */
    private static HBox makeScopeGraphic(String text, boolean selected) {
        // 圆圈：固定 10×10，selected=实心蓝，unselected=空心灰
        Region circle = new Region();
        circle.setMinSize(10, 10);
        circle.setPrefSize(10, 10);
        circle.setMaxSize(10, 10);
        circle.setStyle(selected
                ? "-fx-background-color:#2563EB; -fx-background-radius:50%;"
                : "-fx-background-color:transparent; " +
                  "-fx-border-color:#9CA3AF; -fx-border-width:1.5; " +
                  "-fx-border-radius:50%; -fx-background-radius:50%;");

        // 文字
        Label label = new Label(text);
        label.setStyle(selected
                ? "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1D4ED8;"
                : "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#9CA3AF;");

        HBox box = new HBox(8, circle, label);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

    // ===== 帮助 =====

    @FXML
    private void onShowHelp() {
        FxHelpDialog.show(
                codeInputField.getScene().getWindow(),
                "取消关联 - 操作说明",
                "- **输入码**：支持盒码、箱码、垛码；扫入瓶码时系统自动转换为其所在的盒码。不知道盒/箱/垛码时，可先前往「数据查询」查询码的归属链路。",
                "- **只解一层（默认）**：只取消该码层级的直接关联，下一层保留。垛码→断箱-垛；箱码→断盒-箱（瓶-盒保留）；盒码→断瓶-盒。适用于归错垛/箱（垛码）、换箱体（箱码，保留盒内结构）等场景。操作完成后将实物重新上产线重建关联，瓶-盒层级也可前往「手工采集」页手工重建。",
                "- **全部解除**：取消该码层级及以下所有关联。垛码受层级限制仍只断箱-垛（与只解一层等价）；箱码断盒-箱+瓶-盒，各层码均恢复可用、可重新关联；盒码与只解一层等价。适用于需要彻底清除箱内所有关联的场景（箱码）。召回/返工等需要从垛开始清除的场景，需先垛码断箱-垛，再批量输入箱码执行全部解除。",
                "- **有上级时**：若码已关联至上级（如盒已在箱内），须先取消上级才能取消该码；识别结果会提示上级码值，将上级码加入列表后执行即可。",
                "- **已上传**：云端数据以整垛为单位。若涉及码所在的垛已上传云端，系统先调用云端接口取消整垛数据，成功后再取消本地，保证两端一致。",
                "- **密码**：需要密码验证。"
        );
    }

    // ===== 加入列表 =====

    @FXML
    private void onAddToList() {
        String raw = normalize(codeInputField.getText());
        if (raw.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请输入码值");
            return;
        }
        if (pendingMap.containsKey(raw)) {
            showAlert(Alert.AlertType.WARNING, "提示", "该码已在列表中：" + raw);
            codeInputField.clear();
            return;
        }
        // 异步查询码类型（同时处理瓶码自动转换）
        codeInputField.setDisable(true);
        new Thread(() -> {
            Map<String, Object> checkResult = doCheckCancel(raw);
            Platform.runLater(() -> {
                codeInputField.setDisable(false);
                handleAddResult(raw, checkResult);
            });
        }, "shiwan-m2-check").start();
    }

    private void handleAddResult(String raw, Map<String, Object> check) {
        if (check == null) {
            showAlert(Alert.AlertType.ERROR, "错误", "识别请求失败，请检查网络连接");
            return;
        }
        String codeType = str(check, "codeType");
        String message  = str(check, "message");

        if ("UNKNOWN".equals(codeType)) {
            showAlert(Alert.AlertType.WARNING, "提示", message != null ? message : "该码未在关联表中，无需取消关联");
            return;
        }

        if ("BOTTLE".equals(codeType)) {
            String boxCode = str(check, "boxCode");
            if (boxCode == null || boxCode.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "提示", "瓶码 " + raw + " 未在关联表中，无需取消关联");
                return;
            }
            // 弹出确认转换为盒码
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("瓶码自动转换");
            confirm.setHeaderText(null);
            confirm.setContentText("瓶码 " + raw + " 属于盒码 " + boxCode + "\n已自动转换为盒码 " + boxCode + " 加入列表");
            ShiwanM2AlertUtil.applyStyle(confirm);
            Optional<ButtonType> btn = confirm.showAndWait();
            if (btn.isPresent() && btn.get() == ButtonType.OK) {
                if (pendingMap.containsKey(boxCode)) {
                    showAlert(Alert.AlertType.WARNING, "提示", "盒码 " + boxCode + " 已在列表中");
                } else {
                    addPending(boxCode, "盒码");
                }
            }
            codeInputField.clear();
            return;
        }

        // 正常添加
        String typeName = codeTypeName(codeType);
        addPending(raw, typeName);
        codeInputField.clear();
    }

    private void addPending(String code, String typeName) {
        PendingItem item = new PendingItem(code, typeName);
        pendingMap.put(code, item);
        pendingItems.add(item);
    }

    // ===== 识别 =====

    @FXML
    private void onIdentify() {
        if (pendingMap.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先添加要识别的码");
            return;
        }
        identifySummaryLabel.setText("识别中...");
        identifyItems.clear();
        lastIdentifyMap.clear();
        confirmCancelButton.setDisable(true);

        List<String> codes = new ArrayList<>(pendingMap.keySet());
        new Thread(() -> {
            LinkedHashMap<String, IdentifyItem> resultMap = new LinkedHashMap<>();
            for (String code : codes) {
                Map<String, Object> check = doCheckCancel(code);
                IdentifyItem item = buildIdentifyItem(code, check, isModeAll());
                resultMap.put(code, item);
            }
            Platform.runLater(() -> applyIdentifyResult(resultMap));
        }, "shiwan-m2-identify").start();
    }

    private void applyIdentifyResult(LinkedHashMap<String, IdentifyItem> resultMap) {
        lastIdentifyMap.clear();
        lastIdentifyMap.putAll(resultMap);
        refreshIdentifyDisplay();
    }

    /** 仅刷新识别结果区（切换取消范围时调用）。 */
    private void refreshIdentifyDisplay() {
        if (lastIdentifyMap.isEmpty()) return;
        boolean modeAll = isModeAll();
        int cancelable = 0, nonCancelable = 0;
        identifyItems.clear();
        for (IdentifyItem item : lastIdentifyMap.values()) {
            item.modeAll = modeAll;
            identifyItems.add(item);
            if (item.cancelable) cancelable++;
            else nonCancelable++;
        }
        int total = cancelable + nonCancelable;
        identifySummaryLabel.setText(String.format("共 %d 项，其中 %d 项可解除，%d 项不可解除", total, cancelable, nonCancelable));
        confirmCancelButton.setDisable(cancelable == 0);
    }

    // ===== 清空列表 =====

    @FXML
    private void onClearList() {
        codeInputField.clear();
        pendingMap.clear();
        pendingItems.clear();
        lastIdentifyMap.clear();
        identifyItems.clear();
        identifySummaryLabel.setText("请在左侧输入码值并点击识别");
        confirmCancelButton.setDisable(true);
    }

    // ===== 确认取消关联 =====

    @FXML
    private void onConfirmCancel() {
        List<IdentifyItem> cancelableItems = new ArrayList<>();
        List<IdentifyItem> skippedItems    = new ArrayList<>();
        for (IdentifyItem item : lastIdentifyMap.values()) {
            if (item.cancelable) cancelableItems.add(item);
            else skippedItems.add(item);
        }
        if (cancelableItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "无可取消的码，请先执行识别");
            return;
        }

        // 统计影响条数（根据模式）
        boolean modeAll = isModeAll();
        int totalRelations = cancelableItems.stream()
                .mapToInt(i -> modeAll ? i.affectedAll : i.affectedOneLayer)
                .sum();

        // 检查已上传垛码（弹窗显示云端影响）
        List<IdentifyItem> uploadedPalletItems = new ArrayList<>();
        for (IdentifyItem item : cancelableItems) {
            if ("PALLET".equals(item.codeType) && item.isUploaded) {
                uploadedPalletItems.add(item);
            }
        }

        if (!showPasswordConfirm(modeAll, cancelableItems.size(), totalRelations,
                skippedItems.size(), uploadedPalletItems)) {
            return;
        }

        // 执行取消（支持“先取消上级后重判下级”）
        String mode = modeAll ? "ALL" : "ONE_LAYER";
        confirmCancelButton.setDisable(true);

        new Thread(() -> {
            List<String> successCodes = new ArrayList<>();
            LinkedHashMap<String, IdentifyItem> latestMap = new LinkedHashMap<>(lastIdentifyMap);
            List<String> pendingCodes = new ArrayList<>(latestMap.keySet());

            while (true) {
                // 每轮先按垛→箱→盒排序，确保上级优先
                pendingCodes.sort(Comparator.comparingInt(code -> {
                    IdentifyItem item = latestMap.get(code);
                    return item == null ? 9 : codeTypeOrder(item.codeType);
                }));

                boolean progressed = false;
                for (String code : new ArrayList<>(pendingCodes)) {
                    Map<String, Object> check = doCheckCancel(code);
                    IdentifyItem item = buildIdentifyItem(code, check, modeAll);
                    latestMap.put(code, item);

                    if (!item.cancelable) continue;

                    CancelRecord record = doCancel(code, mode);
                    Platform.runLater(() -> cancelRecordItems.add(0, record));
                    if (record.success) {
                        successCodes.add(code);
                        pendingCodes.remove(code);
                        latestMap.remove(code);
                        progressed = true;
                        logSuccess(code);
                    } else {
                        logFailure(code, record.message);
                    }
                }

                if (!progressed) break;
            }

            Platform.runLater(() -> {
                // 移除成功的码
                for (String code : successCodes) {
                    pendingMap.remove(code);
                    lastIdentifyMap.remove(code);
                    pendingItems.removeIf(p -> p.code.equals(code));
                    identifyItems.removeIf(i -> i.code.equals(code));
                }
                // 用最终重判结果刷新识别区
                if (!latestMap.isEmpty()) {
                    lastIdentifyMap.clear();
                    lastIdentifyMap.putAll(latestMap);
                    refreshIdentifyDisplay();
                } else {
                    identifySummaryLabel.setText("执行完成");
                    confirmCancelButton.setDisable(true);
                }
                codeInputField.clear();
            });
        }, "shiwan-m2-cancel-exec").start();
    }

    // ===== HTTP 调用 =====

    private Map<String, Object> doCheckCancel(String code) {
        try {
            String resp = HttpUtil.doGet("/api/shiwan-m2/code/check-cancel?code=" + enc(code));
            ApiResult<Map<String, Object>> result = HttpUtil.parseJson(resp,
                    new TypeReference<ApiResult<Map<String, Object>>>() {});
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private CancelRecord doCancel(String code, String mode) {
        String time = LocalDateTime.now().format(TIME_FMT);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", code);
            body.put("mode", mode);
            String resp = HttpUtil.doPost("/api/shiwan-m2/code/cancel", body);
            ApiResult<Map<String, Object>> result = HttpUtil.parseJson(resp,
                    new TypeReference<ApiResult<Map<String, Object>>>() {});
            if (result != null && result.getCode() == 200) {
                int count = 0;
                if (result.getData() != null && result.getData().get("cancelledCount") instanceof Number) {
                    count = ((Number) result.getData().get("cancelledCount")).intValue();
                }
                String codeTypeName = result.getData() != null
                        ? codeTypeName(str(result.getData(), "codeType")) : "";
                return new CancelRecord(time, code, codeTypeName, true, count, null);
            }
            String msg = result == null ? "后端无响应" : result.getMessage();
            return new CancelRecord(time, code, "", false, 0, msg);
        } catch (Exception e) {
            return new CancelRecord(time, code, "", false, 0, "请求异常: " + e.getMessage());
        }
    }

    // ===== 密码确认弹窗 =====

    private boolean showPasswordConfirm(boolean modeAll, int execCount, int totalRelations,
                                        int skipCount, List<IdentifyItem> uploadedPallets) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/ShiwanM2CancelConfirmDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            ShiwanM2CancelConfirmDialogController ctrl = loader.getController();

            List<String> uploadedCodes = new java.util.ArrayList<>();
            for (IdentifyItem item : uploadedPallets) {
                uploadedCodes.add(item.code);
            }
            ctrl.setInfo(modeAll, execCount, totalRelations, skipCount, uploadedCodes, FIXED_PASSWORD);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            return ctrl.isConfirmed();
        } catch (Exception ex) {
            // 降级：原生 Dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("取消关联确认");
            ButtonType confirmType = new ButtonType("确认取消", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelType  = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmType, cancelType);
            VBox content = new VBox(12);
            content.setPadding(new Insets(20));
            Label info = new Label("取消范围：" + (modeAll ? "全部解除" : "只解一层")
                    + "，执行 " + execCount + " 项，共 " + totalRelations + " 条关联");
            PasswordField pwdField = new PasswordField();
            pwdField.setPromptText("请输入密码");
            Label pwdErr = new Label();
            pwdErr.setStyle("-fx-text-fill:#EF4444;");
            content.getChildren().addAll(info, pwdField, pwdErr);
            dialog.getDialogPane().setContent(content);
            Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmType);
            confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                if (!FIXED_PASSWORD.equals(pwdField.getText())) {
                    pwdErr.setText("密码错误，请重新输入");
                    pwdField.clear();
                    ev.consume();
                }
            });
            Optional<ButtonType> result = dialog.showAndWait();
            return result.isPresent() && result.get() == confirmType;
        }
    }

    // ===== 日志 =====

    private void logSuccess(String code) {
        OperateLogBuilder.create()
                .module(ModuleNameEnum.CODE_QUERY)
                .operateType(OperateTypeEnum.DELETE)
                .target(code, "取消关联")
                .content("石湾M2取消关联成功: " + code)
                .deviceInfo("石湾M2-取消关联")
                .saveAsync();
    }

    private void logFailure(String code, String reason) {
        OperateLogBuilder.create()
                .module(ModuleNameEnum.CODE_QUERY)
                .operateType(OperateTypeEnum.DELETE)
                .target(code, "取消关联")
                .content("石湾M2取消关联失败: " + code)
                .failReason(reason)
                .deviceInfo("石湾M2-取消关联")
                .saveAsync();
    }

    // ===== 工具方法 =====

    private boolean isModeAll() {
        return modeAll;
    }

    private static String codeTypeName(String codeType) {
        if (codeType == null) return "";
        switch (codeType) {
            case "PALLET": return "垛码";
            case "CASE":   return "箱码";
            case "BOX":    return "盒码";
            case "BOTTLE": return "瓶码";
            default:       return codeType;
        }
    }

    private static int codeTypeOrder(String codeType) {
        if ("PALLET".equals(codeType)) return 0;
        if ("CASE".equals(codeType))   return 1;
        return 2;
    }

    private static String str(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    private static int intVal(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try { return v != null ? Integer.parseInt(v.toString()) : 0; } catch (Exception e) { return 0; }
    }

    private static boolean boolVal(Map<String, Object> map, String key) {
        if (map == null) return false;
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    /** 根据后端 check-cancel 响应构建识别项。若该码已上传则直接标记为不可解除。 */
    private static IdentifyItem buildIdentifyItem(String code, Map<String, Object> check, boolean modeAll) {
        IdentifyItem item = new IdentifyItem();
        item.code    = code;
        item.modeAll = modeAll;
        if (check == null) {
            item.codeType   = "UNKNOWN";
            item.cancelable = false;
            item.message    = "识别失败，请检查网络连接";
            return item;
        }
        item.codeType        = str(check, "codeType");
        item.parentCode      = str(check, "parentCode");
        item.affectedOneLayer = intVal(check, "affectedOneLayer");
        item.affectedAll     = intVal(check, "affectedAll");
        item.isUploaded      = boolVal(check, "isUploaded");
        // 已上传则不允许取消关联
        if (item.isUploaded) {
            item.cancelable = false;
            item.message    = "该码所在垛已上传云端，暂不支持取消关联";
        } else {
            item.cancelable = boolVal(check, "cancelable");
            item.message    = str(check, "message");
        }
        return item;
    }

    // ===== 数据模型 =====

    static class PendingItem {
        final String code;
        final String typeName;
        PendingItem(String code, String typeName) {
            this.code     = code;
            this.typeName = typeName;
        }
        @Override public String toString() {
            return typeName.isEmpty() ? code : typeName + " " + code;
        }
    }

    static class IdentifyItem {
        String  code;
        String  codeType;
        boolean cancelable;
        String  parentCode;
        int     affectedOneLayer;
        int     affectedAll;
        boolean isUploaded;
        String  message;
        boolean modeAll;

        String detailText() {
            if (!cancelable) {
                if (parentCode != null && !parentCode.isEmpty()) {
                    String parentTypeName = parentTypeName(codeType);
                    return "已关联至" + parentTypeName + "码 " + parentCode + "，请先将" + parentTypeName + "码加入列表操作";
                }
                return message != null ? message : "不可解除";
            }
            switch (codeType != null ? codeType : "") {
                case "PALLET":
                    return modeAll
                        ? "解除" + affectedOneLayer + "条箱-垛关联；盒-箱、瓶-盒结构保留（垛码层级限制，与只解一层等价）"
                        : "解除" + affectedOneLayer + "条箱-垛关联；盒-箱、瓶-盒结构保留，各箱可重新上产线归垛（与全部解除等价）";
                case "CASE":
                    if (modeAll) {
                        int bottles = affectedAll - affectedOneLayer;
                        return "解除" + affectedOneLayer + "条盒-箱+" + bottles + "条瓶-盒关联，共" + affectedAll + "条；所有码可重新关联";
                    }
                    return "解除" + affectedOneLayer + "条盒-箱关联；瓶-盒结构保留，各盒可重新上产线归新箱";
                case "BOX":
                    return "解除" + affectedOneLayer + "条瓶-盒关联，可重新关联（与" + (modeAll ? "只解一层" : "全部解除") + "等价）";
                default:
                    return message != null ? message : "";
            }
        }

        static String parentTypeName(String codeType) {
            if ("CASE".equals(codeType)) return "垛";
            if ("BOX".equals(codeType))  return "箱";
            return "上级";
        }
    }

    static class CancelRecord {
        final String  time;
        final String  code;
        final String  typeName;
        final boolean success;
        final int     count;
        final String  message;
        CancelRecord(String time, String code, String typeName, boolean success, int count, String message) {
            this.time     = time;
            this.code     = code;
            this.typeName = typeName;
            this.success  = success;
            this.count    = count;
            this.message  = message;
        }
        @Override public String toString() {
            if (success) {
                return time + " ✓ " + typeName + " " + code + "，解除 " + count + " 条关联";
            }
            return time + " ✗ " + typeName + " " + code + "，失败：" + message;
        }
    }

    // ===== 自定义 ListCell =====

    /** 待取消列表 Cell：「序号. 码类型 码值 [移除]」 */
    private class PendingCell extends ListCell<PendingItem> {
        @Override
        protected void updateItem(PendingItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }

            int idx = getIndex() + 1;
            Label text = new Label(idx + ". " + item);
            text.setStyle("-fx-font-size:16px;");
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMaxWidth(Double.MAX_VALUE);

            Button removeBtn = new Button("移除");
            removeBtn.setStyle("-fx-background-color:#9E9E9E; -fx-text-fill:white; " +
                    "-fx-font-size:12px; -fx-padding:2 8; -fx-cursor:hand;");
            removeBtn.setOnAction(e -> {
                pendingMap.remove(item.code);
                pendingItems.remove(item);
                lastIdentifyMap.remove(item.code);
                identifyItems.removeIf(i -> i.code.equals(item.code));
                refreshIdentifyDisplay();
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(6, text, spacer, removeBtn);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            setGraphic(row);
            setText(null);
        }
    }

    /** 识别结果 Cell：✓/✗ 标题 + 详情描述，带颜色区分 */
    private class IdentifyCell extends ListCell<IdentifyItem> {
        @Override
        protected void updateItem(IdentifyItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }

            VBox card = new VBox(4);
            card.setPadding(new Insets(8, 10, 8, 10));

            String typeName = codeTypeName(item.codeType);
            if (item.cancelable) {
                card.setStyle("-fx-border-color: #16A34A transparent transparent transparent; " +
                        "-fx-border-width: 3 0 0 0;");
                Label title = new Label("✓ " + typeName + " " + item.code + "，可解除");
                title.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#10B981;");
                Label detail = new Label("   " + item.detailText());
                detail.setStyle("-fx-font-size:14px; -fx-text-fill:#6B7280;");
                detail.setWrapText(true);
                card.getChildren().addAll(title, detail);
            } else {
                card.setStyle("-fx-border-color: #DC2626 transparent transparent transparent; " +
                        "-fx-border-width: 3 0 0 0;");
                Label title = new Label("✗ " + typeName + " " + item.code + "，不可解除");
                title.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#DC2626;");
                Label detail = new Label("   " + item.detailText());
                detail.setStyle("-fx-font-size:14px; -fx-text-fill:#6B7280;");
                detail.setWrapText(true);
                card.getChildren().addAll(title, detail);
            }

            setGraphic(card);
            setText(null);
        }
    }

    /** 取消记录 Cell：绿色/红色文字 */
    private static class CancelRecordCell extends ListCell<CancelRecord> {
        @Override
        protected void updateItem(CancelRecord item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }
            Label label = new Label(item.toString());
            label.setStyle("-fx-font-size:16px; -fx-text-fill:" +
                    (item.success ? "#10B981" : "#DC2626") + ";");
            label.setWrapText(true);
            setGraphic(label);
            setText(null);
        }
    }
}
