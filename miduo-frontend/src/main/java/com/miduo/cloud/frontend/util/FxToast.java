package com.miduo.cloud.frontend.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * 轻量 Toast 工具：在指定窗口顶部居中显示短暂提示，停留后向上滑出消失，无需用户操作。
 *
 * <pre>
 * FxToast.success(window, "码包导入成功");           // 默认 3 秒
 * FxToast.success(window, "码包导入成功", 5000);     // 自定义 5 秒
 * FxToast.warn(window, "请先选择文件");
 * FxToast.error(window, "导入失败：网络超时");
 * </pre>
 */
public class FxToast {

    public enum Type { SUCCESS, WARN, ERROR }

    /** 默认停留时长（ms） */
    private static final int DEFAULT_STAY_MS = 3000;
    /** 消失动画时长（ms）：淡出 + 上移 */
    private static final int DISMISS_MS      = 500;
    /** 距窗口顶部的偏移（px） */
    private static final int TOP_OFFSET_PX   = 100;
    /** 消失时向上移动的距离（px） */
    private static final int SLIDE_UP_PX     = 24;

    // ── 便捷入口（默认时长） ──────────────────────────────────────────

    public static void success(Window owner, String message) {
        show(owner, message, Type.SUCCESS, DEFAULT_STAY_MS);
    }

    public static void warn(Window owner, String message) {
        show(owner, message, Type.WARN, DEFAULT_STAY_MS);
    }

    public static void error(Window owner, String message) {
        show(owner, message, Type.ERROR, DEFAULT_STAY_MS);
    }

    // ── 便捷入口（自定义时长） ────────────────────────────────────────

    public static void success(Window owner, String message, int stayMs) {
        show(owner, message, Type.SUCCESS, stayMs);
    }

    public static void warn(Window owner, String message, int stayMs) {
        show(owner, message, Type.WARN, stayMs);
    }

    public static void error(Window owner, String message, int stayMs) {
        show(owner, message, Type.ERROR, stayMs);
    }

    // ── 核心实现 ──────────────────────────────────────────────────────

    public static void show(Window owner, String message, Type type, int stayMs) {
        if (owner == null) return;

        String icon, dotColor, textColor, bgColor, borderColor;
        switch (type) {
            case SUCCESS:
                icon        = "✓";
                dotColor    = "#16A34A";
                textColor   = "#14532D";
                bgColor     = "#F0FDF4";
                borderColor = "#86EFAC";
                break;
            case WARN:
                icon        = "!";
                dotColor    = "#D97706";
                textColor   = "#78350F";
                bgColor     = "#FFFBEB";
                borderColor = "#FCD34D";
                break;
            default: // ERROR
                icon        = "✕";
                dotColor    = "#DC2626";
                textColor   = "#7F1D1D";
                bgColor     = "#FEF2F2";
                borderColor = "#FCA5A5";
                break;
        }

        // ── 图标 ──
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + dotColor + ";" +
                "-fx-min-width: 20px; -fx-min-height: 20px;" +
                "-fx-alignment: center;" +
                "-fx-background-color: " + dotColor + "22;" +
                "-fx-background-radius: 10px;"
        );

        // ── 文字（支持多行，避免长文案撑破布局） ──
        Label textLabel = new Label(message);
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(520);
        textLabel.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-family: 'Microsoft YaHei';" +
                "-fx-text-fill: " + textColor + ";"
        );

        // ── 卡片容器 ──
        HBox content = new HBox(8, iconLabel, textLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 10px 20px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.13), 14, 0, 0, 4);"
        );

        StackPane root = new StackPane(content);
        root.setStyle("-fx-background-color: transparent;");

        Popup popup = new Popup();
        popup.getContent().add(root);
        popup.setAutoFix(false); // 关闭自动修正，由 onShown 精确定位

        // ── 定位：顶部居中（等 popup 渲染完成后精确设置） ──
        popup.setOnShown(e -> {
            double cx = owner.getX() + owner.getWidth()  / 2.0 - root.getWidth()  / 2.0;
            double cy = owner.getY() + TOP_OFFSET_PX;
            popup.setX(cx);
            popup.setY(cy);
        });

        popup.show(owner);

        // ── 消失动画：停留 → 淡出 + 向上滑动 → 关闭 ──
        double startY = owner.getY() + TOP_OFFSET_PX;
        double endY   = startY - SLIDE_UP_PX;

        FadeTransition fade = new FadeTransition(Duration.millis(DISMISS_MS), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_IN);

        // popup.yProperty() 是 ReadOnlyDoubleProperty，不能作为 KeyValue 目标；
        // 改用 SimpleDoubleProperty 作中间变量，监听变化后调用 popup.setY()
        javafx.beans.property.DoubleProperty yProp =
                new javafx.beans.property.SimpleDoubleProperty(startY);
        yProp.addListener((obs, ov, nv) -> popup.setY(nv.doubleValue()));

        Timeline slideUp = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(yProp, startY, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(DISMISS_MS),
                        new KeyValue(yProp, endY, Interpolator.EASE_IN))
        );

        // 淡出与上移并行
        SequentialTransition seq = new SequentialTransition(
                new PauseTransition(Duration.millis(Math.max(stayMs, 500))),
                new javafx.animation.ParallelTransition(fade, slideUp)
        );
        seq.setOnFinished(e -> popup.hide());
        seq.play();
    }
}
