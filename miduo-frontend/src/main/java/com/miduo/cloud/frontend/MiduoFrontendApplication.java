package com.miduo.cloud.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 米多星球产线采集系统 - JavaFX应用程序类
 * 前端独立启动类（不依赖Spring Boot）
 */
public class MiduoFrontendApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("========================================");
        System.out.println("米多星球产线采集系统正在启动...");
        System.out.println("========================================");
        
        try {
            // 加载主界面FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            
            // 创建场景
            Scene scene = new Scene(root, 1400, 900);
            
            // 设置舞台属性
            primaryStage.setTitle("米多星球产线采集系统 v2.0.0");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true); // 允许调整窗口大小
            primaryStage.setMaximized(false); // 启动时不最大化（可根据需要改为true）
            
            // 窗口关闭事件处理 - 改为最小化
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("========================================");
                System.out.println("用户点击关闭按钮（X）- 最小化窗口");
                System.out.println("========================================");
                
                // 消费事件，防止窗口关闭
                event.consume();
                
                // 最小化窗口
                primaryStage.setIconified(true);
            });
            
            // 显示窗口
            primaryStage.show();
            
            System.out.println("========================================");
            System.out.println("✓ 米多星球产线采集系统启动成功！");
            System.out.println("  主界面已加载");
            System.out.println("  窗口大小: 1400x900");
            System.out.println("  后端地址: http://localhost:8080");
            System.out.println("========================================");
            System.out.println("提示:");
            System.out.println("  1. 请确保后端服务已启动（端口8080）");
            System.out.println("  2. 如需修改后端地址，请编辑 HttpUtil.java");
            System.out.println("  3. 设备连接功能需要相应硬件设备");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("✗ 启动失败！");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("========================================");
            System.err.println("可能的原因:");
            System.err.println("  1. FXML文件路径错误");
            System.err.println("  2. Controller类初始化失败");
            System.err.println("  3. 资源文件缺失");
            System.err.println("========================================");
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public void stop() throws Exception {
        System.out.println("========================================");
        System.out.println("米多星球产线采集系统正在关闭...");
        System.out.println("========================================");
        super.stop();
        System.out.println("✓ 应用程序已安全退出");
        System.out.println("========================================");
    }

    /**
     * JavaFX应用程序启动入口
     * 可以直接运行此方法启动前端
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        launch(args);
    }
}

