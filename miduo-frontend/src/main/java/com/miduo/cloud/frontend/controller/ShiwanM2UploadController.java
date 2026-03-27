package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.miduo.cloud.application.shiwan.UploadLogBus;
import com.miduo.cloud.frontend.util.FxDialog;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.HttpUtil;
import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

/**
 * 数据上传 Tab 控制器（P02-08）。
 * <p>
 * 左侧：手动上传按钮 + 实时上传日志（日志最新条目在最上方）。<br>
 * 右侧：垛码查询表单 + 查询结果展示区 + 设为未上传/已上传操作。
 * </p>
 */
public class ShiwanM2UploadController implements Initializable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** 查询结果区上传时间展示：与需求 P02-08 一致（yyyy-MM-dd HH:mm:ss） */
    private static final DateTimeFormatter UPLOAD_TIME_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== FXML 注入 ====================

    @FXML private ListView<UploadLogEntry> uploadLogList;

    @FXML private Button helpButton;
    @FXML private Button statusHelpButton;

    @FXML private TextField palletCodeField;
    @FXML private Label     queryResultLabel;
    @FXML private VBox      resultDetailPane;
    @FXML private Label     resultPalletCode;
    @FXML private Label     resultStatus;
    @FXML private Label     resultUploadTime;
    @FXML private Label     resultBoxCount;
    @FXML private HBox      failReasonRow;
    @FXML private Label     resultFailReason;

    // ==================== 内部状态 ====================

    private final ObservableList<UploadLogEntry> logItems = FXCollections.observableArrayList();

    // ==================== 初始化 ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 向 UploadLogBus 注册日志监听，后端服务通过总线推送日志到本控制器
        UploadLogBus.register((msg, color) -> addLog(msg, mapColor(color)));

        uploadLogList.setItems(logItems);
        uploadLogList.setCellFactory(lv -> new UploadLogCell());

        // 失败原因区域固定在右半区内展示，避免超长文本撑开整体布局。
        resultFailReason.setMinWidth(0);
        resultFailReason.setPrefWidth(0);
        resultFailReason.maxWidthProperty().bind(resultDetailPane.widthProperty().subtract(96));
        resultFailReason.setWrapText(true);
        resultFailReason.setMaxHeight(Double.MAX_VALUE);
        resultFailReason.setTextOverrun(OverrunStyle.CLIP);

        // 初始查询区状态
        showQueryState(QueryState.PROMPT, null, null);

        SvgIconLoader.installHelpButtonGraphic(helpButton);
        SvgIconLoader.installHelpButtonGraphic(statusHelpButton);
    }

    /** 将 UploadLogBus.Color 映射到本地 UploadColor */
    private static UploadColor mapColor(UploadLogBus.Color c) {
        if (c == null) return UploadColor.GRAY;
        switch (c) {
            case BLUE:  return UploadColor.BLUE;
            case GREEN: return UploadColor.GREEN;
            case RED:   return UploadColor.RED;
            default:    return UploadColor.GRAY;
        }
    }

    // ==================== 左侧：手动上传 ====================

    @FXML
    private void onManualUpload() {
        boolean ok = FxDialog.confirm(
                uploadDialogOwner(),
                "手动上传",
                "确认批量上传所有未上传的垛数据？\n将按队列顺序依次上传，上传过程中请勿关闭软件。");
        if (!ok) return;

        // 后台发起，服务端负责写日志到实时上传区
        HttpUtil.asyncPost("/api/shiwan-m2/upload/manual-upload", "{}",
                response -> { /* 服务端已通过 addLogExternal 推日志，前端无需额外处理 */ },
                error -> showWarn("手动上传失败", "发起手动上传时发生错误：" + error.getMessage()));
    }

    @FXML
    private void onHelp() {
        FxHelpDialog.show(
                uploadLogList.getScene().getWindow(),
                "数据上传 - 手动上传说明",
                "- **手动上传**：批量上传所有未上传的垛数据"
        );
    }

    // ==================== 右侧：垛码查询与状态管理 ====================

    @FXML
    private void onQueryStatus() {
        String code = palletCodeField.getText().trim();
        if (code.isEmpty()) {
            showQueryState(QueryState.PROMPT, null, null);
            return;
        }
        HttpUtil.asyncGet("/api/shiwan-m2/upload/pallet-status?palletCode=" + code,
                response -> {
                    try {
                        JsonNode root = HttpUtil.getObjectMapper().readTree(response);
                        int httpCode = root.path("code").asInt(0);
                        if (httpCode != 200 || root.path("data").isMissingNode() || root.path("data").isNull()) {
                            showQueryState(QueryState.NOT_FOUND, code, null);
                        } else {
                            showQueryState(QueryState.FOUND, code, root.path("data"));
                        }
                    } catch (Exception e) {
                        showQueryState(QueryState.NOT_FOUND, code, null);
                    }
                },
                error -> showWarn("查询失败", "查询上传状态失败：" + error.getMessage()));
    }

    @FXML
    private void onSetNotUploaded() {
        String code = palletCodeField.getText().trim();
        if (code.isEmpty()) { showWarn("请输入垛码", "请先输入要操作的垛码。"); return; }

        boolean ok = FxDialog.confirm(
                uploadDialogOwner(),
                "设为未上传",
                "确认将垛码「" + code + "」的状态重置为未上传？\n状态重置后，可通过手动上传重新上传此垛。");
        if (!ok) return;

        HttpUtil.asyncPost("/api/shiwan-m2/upload/set-not-uploaded",
                "{\"palletCode\":\"" + code + "\"}",
                response -> {
                    try {
                        JsonNode root = HttpUtil.getObjectMapper().readTree(response);
                        if (root.path("code").asInt(0) == 200) {
                            addLog(now() + " 垛码 " + code + "，已设为未上传（待补传）", UploadColor.GRAY);
                            onQueryStatus();
                        } else {
                            showWarn("操作失败", root.path("msg").asText("未知错误"));
                        }
                    } catch (Exception e) {
                        showWarn("操作失败", e.getMessage());
                    }
                },
                error -> showWarn("操作失败", "设为未上传时发生错误：" + error.getMessage()));
    }

    @FXML
    private void onSetUploaded() {
        String code = palletCodeField.getText().trim();
        if (code.isEmpty()) { showWarn("请输入垛码", "请先输入要操作的垛码。"); return; }

        boolean ok = FxDialog.confirm(
                uploadDialogOwner(),
                "设为已上传",
                "确认将垛码「" + code + "」手动标记为已上传？\n此操作将跳过该垛的自动上传，避免重复上传。");
        if (!ok) return;

        HttpUtil.asyncPost("/api/shiwan-m2/upload/set-uploaded",
                "{\"palletCode\":\"" + code + "\"}",
                response -> {
                    try {
                        JsonNode root = HttpUtil.getObjectMapper().readTree(response);
                        if (root.path("code").asInt(0) == 200) {
                            addLog(now() + " 垛码 " + code + "，已手动标记为已上传", UploadColor.GREEN);
                            onQueryStatus();
                        } else {
                            showWarn("操作失败", root.path("msg").asText("未知错误"));
                        }
                    } catch (Exception e) {
                        showWarn("操作失败", e.getMessage());
                    }
                },
                error -> showWarn("操作失败", "设为已上传时发生错误：" + error.getMessage()));
    }

    @FXML
    private void onStatusHelp() {
        FxHelpDialog.show(
                palletCodeField.getScene().getWindow(),
                "数据上传 - 状态管理说明",
                "- **查询状态**：查询指定垛码的上传状态详情",
                "- **设为未上传**：将垛码状态重置为未上传，用于数据修复后重新补传",
                "- **设为已上传**：手动标记为已上传，避免系统重复上传此垛"
        );
    }

    // ==================== 查询结果展示 ====================

    private enum QueryState { PROMPT, NOT_FOUND, FOUND }

    private void showQueryState(QueryState state, String code, JsonNode data) {
        switch (state) {
            case PROMPT:
                queryResultLabel.setText("请输入垛码并点击查询状态");
                queryResultLabel.setStyle("-fx-text-fill:#6B7280; -fx-font-size:14px;");
                queryResultLabel.setVisible(true);
                queryResultLabel.setManaged(true);
                resultDetailPane.setVisible(false);
                resultDetailPane.setManaged(false);
                break;

            case NOT_FOUND:
                queryResultLabel.setText("未找到该垛码的上传记录：" + code);
                queryResultLabel.setStyle("-fx-text-fill:#DC2626; -fx-font-size:14px;");
                queryResultLabel.setVisible(true);
                queryResultLabel.setManaged(true);
                resultDetailPane.setVisible(false);
                resultDetailPane.setManaged(false);
                break;

            case FOUND:
                queryResultLabel.setVisible(false);
                queryResultLabel.setManaged(false);
                resultDetailPane.setVisible(true);
                resultDetailPane.setManaged(true);

                resultPalletCode.setText(code);

                // 上传状态标签（0=未上传, 1=已上传, 2=上传失败, 3=上传中）
                int isUpload = data != null ? data.path("isUpload").asInt(0) : 0;
                String statusText;
                String statusClass;
                switch (isUpload) {
                    case 1:
                        statusText  = "已上传";
                        statusClass = "sw2-badge-green";
                        break;
                    case 2:
                        statusText  = "上传失败";
                        statusClass = "sw2-badge-red";
                        break;
                    case 3:
                        statusText  = "上传中";
                        statusClass = "sw2-badge-gray";
                        break;
                    default:
                        statusText  = "待上传";
                        statusClass = "sw2-badge-gray";
                        break;
                }
                resultStatus.setText(statusText);
                resultStatus.getStyleClass().removeAll("sw2-badge-green", "sw2-badge-red", "sw2-badge-gray");
                resultStatus.getStyleClass().add(statusClass);

                String uploadTime = data != null ? data.path("uploadTime").asText("") : "";
                resultUploadTime.setText(formatUploadTimeForDisplay(uploadTime));

                int boxCount = data != null ? data.path("boxCount").asInt(0) : 0;
                resultBoxCount.setText(String.valueOf(boxCount));

                boolean showFailReason = (isUpload == 2);
                failReasonRow.setVisible(showFailReason);
                failReasonRow.setManaged(showFailReason);
                if (showFailReason) {
                    String msg = data != null ? data.path("msg").asText("") : "";
                    resultFailReason.setText(isNullOrBlank(msg) ? "-" : softWrapLongToken(msg));
                }
                break;
        }
    }

    /**
     * 为超长连续串（如超长垛标）插入零宽断点，避免撑开布局。
     */
    private static String softWrapLongToken(String text) {
        if (text == null || text.isEmpty()) return text;
        final int chunk = 12;
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            sb.append(ch);
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == ':') {
                run++;
                if (run >= chunk) {
                    sb.append('\u200B');
                    run = 0;
                }
            } else {
                run = 0;
            }
        }
        return sb.toString();
    }

    // ==================== 实时上传日志管理 ====================

    /**
     * 向实时上传日志顶部插入一条记录（最新在最上方）。
     * 可从任意线程调用，内部通过 Platform.runLater 切回 FX 线程。
     */
    public void addLog(String message, UploadColor color) {
        String line = softWrapLongToken(message);
        Platform.runLater(() -> logItems.add(0, new UploadLogEntry(line, color)));
    }

    // ==================== 工具 ====================

    private String now() {
        return LocalDateTime.now().format(TIME_FMT);
    }

    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty() || "null".equalsIgnoreCase(s.trim());
    }

    /**
     * 将接口返回的时间（常见为 ISO-8601 本地时间带 'T'）格式化为 yyyy-MM-dd HH:mm:ss；空则 "-"。
     */
    private static String formatUploadTimeForDisplay(String raw) {
        if (isNullOrBlank(raw)) {
            return "-";
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(UPLOAD_TIME_DISPLAY);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(s, UPLOAD_TIME_DISPLAY).format(UPLOAD_TIME_DISPLAY);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(s).toLocalDateTime().format(UPLOAD_TIME_DISPLAY);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        return s;
    }

    private Window uploadDialogOwner() {
        if (palletCodeField != null && palletCodeField.getScene() != null) {
            return palletCodeField.getScene().getWindow();
        }
        if (uploadLogList != null && uploadLogList.getScene() != null) {
            return uploadLogList.getScene().getWindow();
        }
        return null;
    }

    /** 通用警告提示（可从后台线程调用，内部切回 FX 线程）。 */
    private void showWarn(String title, String content) {
        Runnable r = () -> FxDialog.warn(uploadDialogOwner(), title, content);
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    // ==================== 枚举与数据模型 ====================

    public enum UploadColor { GRAY, BLUE, GREEN, RED }

    public static class UploadLogEntry {
        public final String      text;
        public final UploadColor color;

        public UploadLogEntry(String text, UploadColor color) {
            this.text  = text;
            this.color = color;
        }
    }

    // ==================== ListCell ====================

    static class UploadLogCell extends ListCell<UploadLogEntry> {
        private final Label label = new Label();

        UploadLogCell() {
            label.setWrapText(true);
            label.setMinWidth(0);
            label.setMaxHeight(Double.MAX_VALUE);
            /* 多行换行展示完整文案，不使用末尾「…」省略（行高由 computePrefHeight 撑开） */
            label.setTextOverrun(OverrunStyle.CLIP);
            setPrefWidth(0);
        }

        @Override
        protected double computePrefHeight(double width) {
            if (isEmpty() || getItem() == null) {
                return super.computePrefHeight(width);
            }
            double pad = snapSizeX(getInsets().getLeft() + getInsets().getRight()) + 8;
            double lw = width > 0 ? width - pad : -1;
            if (lw <= 0 && getListView() != null) {
                lw = Math.max(0, getListView().getWidth() - pad);
            }
            if (lw <= 0) {
                lw = 280;
            }
            label.setMaxWidth(lw);
            return snapSizeY(getInsets().getTop() + getInsets().getBottom() + label.prefHeight(lw));
        }

        @Override
        protected void layoutChildren() {
            double lw = Math.max(0, getWidth() - snapSizeX(getInsets().getLeft() + getInsets().getRight()) - 8);
            if (lw > 0) {
                label.setMaxWidth(lw);
            }
            super.layoutChildren();
        }

        @Override
        protected void updateItem(UploadLogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }
            label.setText(item.text);
            label.getStyleClass().removeAll("sw2-ul-gray", "sw2-ul-blue", "sw2-ul-green", "sw2-ul-red");
            String colorClass;
            switch (item.color) {
                case BLUE:  colorClass = "sw2-ul-blue";  break;
                case GREEN: colorClass = "sw2-ul-green"; break;
                case RED:   colorClass = "sw2-ul-red";   break;
                default:    colorClass = "sw2-ul-gray";  break;
            }
            label.getStyleClass().add(colorClass);
            setGraphic(label);
            setText(null);
        }
    }
}
