package com.miduo.cloud.frontend.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * 窗口图标工具类
 * 用于统一设置所有窗口的图标
 */
public class StageIconUtil {
    
    private static final String ICON_PATH = "/米多图标.png";
    private static Image appIcon = null;
    
    /**
     * 为Stage设置应用程序图标
     * @param stage 要设置图标的Stage
     */
    public static void setStageIcon(Stage stage) {
        if (stage == null) {
            return;
        }
        
        try {
            // 延迟加载图标（只加载一次）
            if (appIcon == null) {
                appIcon = new Image(StageIconUtil.class.getResourceAsStream(ICON_PATH));
            }
            
            // 设置窗口图标
            if (appIcon != null && !appIcon.isError()) {
                stage.getIcons().clear();
                stage.getIcons().add(appIcon);
            } else {
                System.err.println("[窗口图标] 无法加载图标文件: " + ICON_PATH);
            }
        } catch (Exception e) {
            System.err.println("[窗口图标] 设置窗口图标失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

