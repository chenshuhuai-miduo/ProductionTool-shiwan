package com.miduo.cloud.frontend.util;

/**
 * 与 {@link FxDialog} 一致的「微软雅黑 + font-weight:bold」内联片段，供代码里动态拼 {@code setStyle} 时复用。
 * <p>
 * FXML / 主界面优先用样式表 {@code styleClass}（如 {@code shiwan-m2-styles.css}），避免到处复制内联字符串。
 * </p>
 */
public final class MiduoFxTextStyles {

    private MiduoFxTextStyles() {}

    public static final String FONT_FAMILY_YAHEI = "'Microsoft YaHei'";

    /**
     * 弹窗标题：18px、粗体、深色字（与 FxDialog 标题一致）。
     */
    public static final String DIALOG_TITLE =
            "-fx-font-family: " + FONT_FAMILY_YAHEI + ";"
                    + "-fx-font-size: 18px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-text-fill: #111827;";

    /**
     * 弹窗正文：16px、常规字重（与 FxDialog 内容区一致）。
     */
    public static final String DIALOG_BODY =
            "-fx-font-family: " + FONT_FAMILY_YAHEI + ";"
                    + "-fx-font-size: 16px;"
                    + "-fx-text-fill: #4B5563;"
                    + "-fx-line-spacing: 4;";

    /**
     * 弹窗按钮文字：18px、粗体（背景/边框/尺寸由调用方拼接）。
     */
    public static final String DIALOG_BUTTON_FONT =
            "-fx-font-size: 18px;"
                    + "-fx-font-weight: bold;"
                    + "-fx-font-family: " + FONT_FAMILY_YAHEI + ";";
}
