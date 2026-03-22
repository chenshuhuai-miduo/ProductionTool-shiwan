package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import com.miduo.cloud.frontend.util.FxHelpDialog;
import com.miduo.cloud.frontend.util.OperateLogBuilder;
import com.miduo.cloud.frontend.util.ShiwanM2AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        FxHelpDialog.show(
                origCodeField.getScene().getWindow(),
                "原码说明",
                "- **原码**：请输入需要替换的原码，该码必须已在系统中存在关联关系"
        );
    }

    @FXML
    private void onNewCodeHelp() {
        FxHelpDialog.show(
                newCodeField.getScene().getWindow(),
                "新码说明",
                "- **层级一致**：新码必须与原码属于同一层级（瓶→瓶、盒→盒、箱→箱）",
                "- **码包范围**：新码必须在已导入的对应层级码包范围内",
                "- **未被使用**：新码不能已存在关联关系（系统中无该码的任何关联）",
                "- **不能相同**：新码不能与原码相同"
        );
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ShiwanM2ReplaceConfirmDialog.fxml"));
            Parent root = loader.load();
            ShiwanM2ReplaceConfirmDialogController ctrl = loader.getController();
            ctrl.setReplaceInfo(orig, newCode, reason, OPERATOR_PASSWORD);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (!ctrl.isConfirmed()) return;
            executeReplace(orig, newCode, reason);
        } catch (Exception ex) {
            showWarn("系统错误", "无法打开确认弹窗：" + ex.getMessage());
        }
    }

    // ==================== 替换执行 ====================

    private void executeReplace(String orig, String newCode, String reason) {
        boolean success = simulateReplace(orig, newCode);
        String  time    = LocalDateTime.now().format(DT_FMT);

        emptyResultLabel.setVisible(false);
        emptyResultLabel.setManaged(false);

        VBox card = new VBox(12);
        card.getStyleClass().add(success ? "sw2-replace-result-ok" : "sw2-replace-result-err");

        // 圆形图标 + 标题行
        String iconColor = success ? "#10B981" : "#EF4444";
        String titleText = success ? "码替换成功！" : "码替换失败！";

        Label iconLbl = new Label(success ? "✔" : "✘");
        iconLbl.setMinSize(28, 28);
        iconLbl.setMaxSize(28, 28);
        iconLbl.setAlignment(javafx.geometry.Pos.CENTER);
        iconLbl.setStyle(String.format(
                "-fx-background-color:%s; -fx-background-radius:14;" +
                "-fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold;", iconColor));

        Label titleLbl = new Label(titleText);
        titleLbl.setStyle(String.format(
                "-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:%s;", iconColor));

        javafx.scene.layout.HBox headerRow = new javafx.scene.layout.HBox(8, iconLbl, titleLbl);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // 详情区
        VBox detailBox = new VBox(8);
        if (!success) {
            Label failReasonLbl = new Label("失败原因：新码已存在");
            failReasonLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#DC2626;");
            detailBox.getChildren().add(failReasonLbl);
        }
        detailBox.getChildren().addAll(
                infoLine("原码：", orig),
                infoLine("新码：", newCode));
        if (success && !reason.isEmpty()) {
            detailBox.getChildren().add(infoLine("替换原因：", reason));
        }
        detailBox.getChildren().add(timeLine("操作时间：", time));

        card.getChildren().addAll(headerRow, detailBox);
        resultContainer.getChildren().add(0, card);

        if (success) {
            origCodeField.clear();
            newCodeField.clear();
            reasonField.clear();

            OperateLogBuilder.create()
                    .module(ModuleNameEnum.CODE_REPLACE)
                    .operateType(OperateTypeEnum.REPLACE)
                    .target(orig, newCode)
                    .content("石湾M2数据替换: " + orig + " -> " + newCode)
                    .beforeData(orig)
                    .afterData(newCode)
                    .remark(reason)
                    .deviceInfo("石湾M2-数据替换")
                    .saveAsync();
        } else {
            OperateLogBuilder.create()
                    .module(ModuleNameEnum.CODE_REPLACE)
                    .operateType(OperateTypeEnum.REPLACE)
                    .target(orig, newCode)
                    .content("石湾M2数据替换失败: " + orig + " -> " + newCode)
                    .failReason("新码已存在")
                    .deviceInfo("石湾M2-数据替换")
                    .saveAsync();
        }
    }

    private Label infoLine(String key, String val) {
        Label l = new Label(key + val);
        l.setStyle("-fx-font-size:16px; -fx-text-fill:#374151;");
        return l;
    }

    private Label timeLine(String key, String val) {
        Label l = new Label(key + val);
        l.setStyle("-fx-font-size:16px; -fx-text-fill:#6B7280;");
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
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }

    private void showWarn(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        ShiwanM2AlertUtil.applyStyle(alert);
        alert.showAndWait();
    }
}
