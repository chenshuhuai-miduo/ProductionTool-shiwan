package com.miduo.cloud.frontend.util;

import javafx.scene.control.Alert;

/**
 * 石湾2号机弹窗工具类：统一为所有 Alert 弹窗应用正文字体样式，
 * 使其与主界面操作日志字体大小（16px / Microsoft YaHei）保持一致。
 */
public class ShiwanM2AlertUtil {

    private static final String DIALOG_FONT_STYLE =
        "-fx-font-size: 16px; -fx-font-family: 'Microsoft YaHei';";

    public static void applyStyle(Alert alert) {
        if (alert != null && alert.getDialogPane() != null) {
            alert.getDialogPane().setStyle(DIALOG_FONT_STYLE);
        }
    }
}
