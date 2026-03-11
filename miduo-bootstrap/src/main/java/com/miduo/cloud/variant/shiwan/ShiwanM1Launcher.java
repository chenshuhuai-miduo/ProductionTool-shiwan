package com.miduo.cloud.variant.shiwan;

import com.miduo.cloud.frontend.ShiwanM1FrontendApplication;

/**
 * 石湾1号机（瓶盒关联软件）启动入口。
 * <p>
 * 构建时使用 {@code -Pvariant-shiwan-m1} Profile，
 * 主界面加载 {@code ShiwanM1MainWindow.fxml}（1366×768）。
 * </p>
 */
public class ShiwanM1Launcher {
    public static void main(String[] args) {
        ShiwanM1FrontendApplication.main(args);
    }
}
