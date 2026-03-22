package com.miduo.cloud.frontend.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * 通用弹窗工具类，提供确认弹窗（双按钮）和提示弹窗（单按钮）两种类型，无需 FXML。
 *
 * <pre>
 * // 确认弹窗：返回 true 表示用户点击「确定」
 * boolean ok = FxDialog.confirm(window, "确认操作", "此操作将永久删除文件，无法撤销。是否继续？");
 * boolean ok = FxDialog.danger(window,  "删除确认", "删除后数据不可恢复，是否继续？");
 *
 * // 提示弹窗：阻塞等待用户点击「确定」
 * FxDialog.alert(window,   "操作提示", "操作已完成，请知悉。");
 * FxDialog.success(window, "保存成功", "数据已保存至云端。");
 * FxDialog.warn(window,    "注意",     "当前存在未完成项。");
 * </pre>
 */
public class FxDialog {

    public enum Type { WARN, DANGER, INFO, SUCCESS }

    // ── 确认弹窗（双按钮，返回是否确认）────────────────────────────────────

    /** 警告类确认弹窗，橙色图标，蓝色确定按钮 */
    public static boolean confirm(Window owner, String title, String content) {
        return showConfirm(owner, title, content, Type.WARN);
    }

    /** 危险操作确认弹窗，红色图标，红色确定按钮 */
    public static boolean danger(Window owner, String title, String content) {
        return showConfirm(owner, title, content, Type.DANGER);
    }

    // ── 提示弹窗（单按钮，阻塞直到用户关闭）────────────────────────────────

    /** 信息提示弹窗，蓝色图标 */
    public static void alert(Window owner, String title, String content) {
        showAlert(owner, title, content, Type.INFO);
    }

    /** 成功提示弹窗，绿色图标 */
    public static void success(Window owner, String title, String content) {
        showAlert(owner, title, content, Type.SUCCESS);
    }

    /** 警告提示弹窗，橙色图标 */
    public static void warn(Window owner, String title, String content) {
        showAlert(owner, title, content, Type.WARN);
    }

    // ── 核心实现 ─────────────────────────────────────────────────────────

    private static boolean showConfirm(Window owner, String title, String content, Type type) {
        boolean[] result = {false};
        Stage stage = createStage(owner);
        VBox root = buildDialogRoot();

        root.getChildren().add(buildHeader(title, type));
        root.getChildren().add(buildDivider());
        root.getChildren().add(buildContent(content));

        HBox footer = buildFooter(Pos.CENTER_RIGHT);
        Button cancelBtn = buildButton("取 消", "#FFFFFF", "#374151", "#D1D5DB", 96, 40);
        cancelBtn.setOnAction(e -> { result[0] = false; stage.close(); });

        String confirmBg = (type == Type.DANGER) ? "#DC2626" : "#2563EB";
        Button confirmBtn = buildButton("确 定", confirmBg, "#FFFFFF", null, 96, 40);
        confirmBtn.setOnAction(e -> { result[0] = true; stage.close(); });

        footer.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().add(footer);

        showStage(stage, root, owner);
        return result[0];
    }

    private static void showAlert(Window owner, String title, String content, Type type) {
        Stage stage = createStage(owner);
        VBox root = buildDialogRoot();

        root.getChildren().add(buildHeader(title, type));
        root.getChildren().add(buildDivider());
        root.getChildren().add(buildContent(content));

        HBox footer = buildFooter(Pos.CENTER);
        Button okBtn = buildButton("确 定", "#2563EB", "#FFFFFF", null, 160, 44);
        okBtn.setOnAction(e -> stage.close());
        footer.getChildren().add(okBtn);
        root.getChildren().add(footer);

        showStage(stage, root, owner);
    }

    // ── 构建各区块 ────────────────────────────────────────────────────────

    private static Stage createStage(Window owner) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);
        return stage;
    }

    private static VBox buildDialogRoot() {
        VBox box = new VBox();
        box.setPrefWidth(400);
        box.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #E5E7EB;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 16;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.10));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        box.setEffect(shadow);
        return box;
    }

    private static HBox buildHeader(String title, Type type) {
        String iconChar, iconColor, iconBg;
        switch (type) {
            case DANGER:
                iconChar = "!"; iconColor = "#DC2626"; iconBg = "#FEE2E2"; break;
            case INFO:
                iconChar = "i"; iconColor = "#2563EB"; iconBg = "#DBEAFE"; break;
            case SUCCESS:
                iconChar = "✓"; iconColor = "#16A34A"; iconBg = "#DCFCE7"; break;
            default: // WARN
                iconChar = "!"; iconColor = "#D97706"; iconBg = "#FEF3C7"; break;
        }

        Label icon = new Label(iconChar);
        icon.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + iconColor + ";" +
                "-fx-min-width: 28; -fx-min-height: 28;" +
                "-fx-max-width: 28; -fx-max-height: 28;" +
                "-fx-alignment: center;" +
                "-fx-background-color: " + iconBg + ";" +
                "-fx-background-radius: 14;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold;" +
                "-fx-text-fill: #111827;" +
                "-fx-font-family: 'Microsoft YaHei';"
        );

        HBox header = new HBox(10, icon, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        return header;
    }

    private static Region buildDivider() {
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setStyle("-fx-background-color: #F3F4F6;");
        return line;
    }

    private static VBox buildContent(String content) {
        Label text = new Label(content);
        text.setWrapText(true);
        text.setMaxWidth(352); // 400 - 24*2
        text.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-text-fill: #4B5563;" +
                "-fx-font-family: 'Microsoft YaHei';" +
                "-fx-line-spacing: 4;"
        );
        VBox area = new VBox(text);
        area.setPadding(new Insets(20, 24, 8, 24));
        return area;
    }

    private static HBox buildFooter(Pos alignment) {
        HBox footer = new HBox(12);
        footer.setAlignment(alignment);
        footer.setPadding(new Insets(28, 24, 28, 24));
        return footer;
    }

    private static Button buildButton(String text, String bgColor, String textColor,
                                      String borderColor, double width, double height) {
        Button btn = new Button(text);
        String border = borderColor != null
                ? "-fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-border-radius: 8;"
                : "-fx-border-width: 0;";
        btn.setStyle(
                "-fx-pref-width: " + width + ";" +
                "-fx-min-width: " + width + ";" +
                "-fx-pref-height: " + height + ";" +
                "-fx-min-height: " + height + ";" +
                "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-font-size: 18px; -fx-font-weight: bold;" +
                "-fx-font-family: 'Microsoft YaHei';" +
                "-fx-background-radius: 8;" +
                "-fx-effect: null; -fx-cursor: hand;" +
                border
        );
        return btn;
    }

    private static void showStage(Stage stage, VBox root, Window owner) {
        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setPadding(new Insets(20)); // 为阴影留出空间

        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        stage.setOnShown(e -> {
            if (owner != null) {
                stage.setX(owner.getX() + (owner.getWidth()  - stage.getWidth())  / 2.0);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2.0);
            }
        });

        stage.showAndWait();
    }
}
