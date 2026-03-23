package com.miduo.cloud.variant.shiwan;

import com.miduo.cloud.frontend.ShiwanM2FrontendApplication;

/**
 * 石湾2号机（盒箱垛关联软件）启动入口。
 * <p>
 * 构建时使用 {@code -Pvariant-shiwan-m2} Profile，
 * 主界面加载 {@code ShiwanM2MainWindow.fxml}（1366×768）。
 * </p>
 */
public class ShiwanM2Launcher {
    // 「关于系统」内容在 ShiwanM2FrontendApplication.main() 中配置
    public static void main(String[] args) {
        ShiwanM2FrontendApplication.main(args);
    }
}
