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
 * 通用弹窗工具类，提供确认弹窗（双按钮）、提示弹窗（单按钮）和多选弹窗（任意按钮）三种类型，无需 FXML。
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
 *
 * // 多选弹窗：返回点击的按钮序号（0-based），关闭窗口返回 -1
 * int idx = FxDialog.choice(window, "退出确认", "存在未满垛数据（1箱），是否退出？\n暂存后可通过「提取工单未成垛」继续生产。",
 *     FxDialog.BtnDef.cancel("取消"),
 *     FxDialog.BtnDef.primary("强制满垛"),
 *     FxDialog.BtnDef.warn("暂存退出")
 * );
 * // idx == 0 → 取消，idx == 1 → 强制满垛，idx == 2 → 暂存退出，idx == -1 → 关闭窗口
 * </pre>
 */
public class FxDialog {

    public enum Type { WARN, DANGER, INFO, SUCCESS }

    // ── 按钮定义（用于 choice 多选弹窗）────────────────────────────────────

    public static class BtnDef {
        final String label;
        final String bg;
        final String fg;
        final String border;

        private BtnDef(String label, String bg, String fg, String border) {
            this.label = label; this.bg = bg; this.fg = fg; this.border = border;
        }

        /** 灰色取消按钮（带边框） */
        public static BtnDef cancel(String label) {
            return new BtnDef(label, "#FFFFFF", "#374151", "#D1D5DB");
        }
        /** 蓝色主操作按钮 */
        public static BtnDef primary(String label) {
            return new BtnDef(label, "#2563EB", "#FFFFFF", null);
        }
        /** 红色危险操作按钮 */
        public static BtnDef danger(String label) {
            return new BtnDef(label, "#DC2626", "#FFFFFF", null);
        }
        /** 橙色警告操作按钮 */
        public static BtnDef warn(String label) {
            return new BtnDef(label, "#D97706", "#FFFFFF", null);
        }
        /** 绿色成功操作按钮 */
        public static BtnDef success(String label) {
            return new BtnDef(label, "#16A34A", "#FFFFFF", null);
        }
    }

    // ── 确认弹窗（双按钮，返回是否确认）────────────────────────────────────

    /** 警告类确认弹窗，橙色图标，蓝色确定按钮 */
    public static boolean confirm(Window owner, String title, String content) {
        return showConfirm(owner, title, content, Type.WARN, "取 消", "确 定");
    }

    /** 危险操作确认弹窗，红色图标，红色确定按钮 */
    public static boolean danger(Window owner, String title, String content) {
        return showConfirm(owner, title, content, Type.DANGER, "取 消", "确 定");
    }

    /**
     * 自定义按钮文字的确认弹窗（警告类，蓝色确定按钮）。
     *
     * @param confirmLabel 确定按钮文字，如「继续」「覆盖」「删除」
     */
    public static boolean confirm(Window owner, String title, String content, String confirmLabel) {
        return showConfirm(owner, title, content, Type.WARN, "取 消", confirmLabel);
    }

    // ── 多选弹窗（任意按钮，返回点击序号，关闭窗口返回 -1）─────────────────

    /**
     * 多选弹窗，支持任意数量按钮。
     *
     * @param buttons 按钮定义，使用 {@link BtnDef} 工厂方法创建（cancel / primary / danger / warn / success）
     * @return 点击的按钮序号（0-based）；用户关闭窗口时返回 -1
     */
    public static int choice(Window owner, String title, String content, BtnDef... buttons) {
        int[] result = {-1};
        Stage stage = createStage(owner);
        VBox root = buildDialogRoot(Math.max(400, buttons.length * 110 + 80));

        root.getChildren().add(buildHeader(title, Type.WARN));
        root.getChildren().add(buildDivider());
        root.getChildren().add(buildContent(content));

        HBox footer = buildFooter(Pos.CENTER_RIGHT);
        for (int i = 0; i < buttons.length; i++) {
            BtnDef def = buttons[i];
            final int idx = i;
            Button btn = buildButton(def.label, def.bg, def.fg, def.border, 96, 40);
            btn.setOnAction(e -> { result[0] = idx; stage.close(); });
            footer.getChildren().add(btn);
        }
        root.getChildren().add(footer);

        showStage(stage, root, owner);
        return result[0];
    }

    // ── 提示弹窗（单按钮，阻塞直到用户关闭）────────────────────────────────

    /** 信息提示弹窗（与 {@link #warn} 相同：橙色警告图标，仅语义为「告知」） */
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

    private static boolean showConfirm(Window owner, String title, String content,
                                        Type type, String cancelLabel, String confirmLabel) {
        boolean[] result = {false};
        Stage stage = createStage(owner);
        VBox root = buildDialogRoot();

        root.getChildren().add(buildHeader(title, type));
        root.getChildren().add(buildDivider());
        root.getChildren().add(buildContent(content));

        HBox footer = buildFooter(Pos.CENTER_RIGHT);
        Button cancelBtn = buildButton(cancelLabel, "#FFFFFF", "#374151", "#D1D5DB", 96, 40);
        cancelBtn.setOnAction(e -> { result[0] = false; stage.close(); });

        String confirmBg = (type == Type.DANGER) ? "#DC2626" : "#2563EB";
        Button confirmBtn = buildButton(confirmLabel, confirmBg, "#FFFFFF", null, 96, 40);
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
        return buildDialogRoot(400);
    }

    private static VBox buildDialogRoot(double width) {
        VBox box = new VBox();
        box.setPrefWidth(width);
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

    /** 标题栏左侧圆标尺寸；内嵌 SVG 略小以留白 */
    private static final double DIALOG_ICON_BADGE = 28;
    private static final double DIALOG_ICON_INNER = 18;

    private static HBox buildHeader(String title, Type type) {
        String svgResource;
        Color iconPaint;
        String iconBgCss;
        switch (type) {
            case DANGER:
                svgResource = SvgIconLoader.ICON_ERROR;
                iconPaint = Color.web("#DC2626");
                iconBgCss = "#FEE2E2";
                break;
            case SUCCESS:
                svgResource = SvgIconLoader.ICON_SUCCESS;
                iconPaint = Color.web("#16A34A");
                iconBgCss = "#DCFCE7";
                break;
            case INFO:
            default: // WARN — 信息与普通警告共用橙色感叹号图标
                svgResource = SvgIconLoader.ICON_DIALOG_INVERTED_EXCLAM;
                iconPaint = Color.web("#D97706");
                iconBgCss = "#FEF3C7";
                break;
        }

        StackPane badge = new StackPane();
        badge.setMinSize(DIALOG_ICON_BADGE, DIALOG_ICON_BADGE);
        badge.setPrefSize(DIALOG_ICON_BADGE, DIALOG_ICON_BADGE);
        badge.setMaxSize(DIALOG_ICON_BADGE, DIALOG_ICON_BADGE);
        badge.setStyle(
                "-fx-background-color: " + iconBgCss + ";" +
                "-fx-background-radius: " + (DIALOG_ICON_BADGE / 2) + ";"
        );

        StackPane iconPane = new StackPane();
        SvgIconLoader.loadInto(iconPane, svgResource, DIALOG_ICON_INNER, iconPaint);
        badge.getChildren().add(iconPane);

        Label titleLabel = new Label(title);
        titleLabel.setStyle(MiduoFxTextStyles.DIALOG_TITLE);

        HBox header = new HBox(10, badge, titleLabel);
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
        text.setMaxWidth(Double.MAX_VALUE);
        text.setStyle(MiduoFxTextStyles.DIALOG_BODY);
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
                MiduoFxTextStyles.DIALOG_BUTTON_FONT +
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
