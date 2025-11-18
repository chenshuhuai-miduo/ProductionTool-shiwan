package com.miduo.cloud.frontend;

/**
 * 米多星球产线采集系统 - 前端主启动类
 * 使用普通main方法启动JavaFX应用程序
 * 
 * 启动方式：
 * 1. 命令行: java -jar miduo-frontend.jar
 * 2. IDE: 直接运行此类的main方法
 * 3. Maven: mvn javafx:run (如果配置了javafx-maven-plugin)
 */
public class FrontendMain {
    
    /**
     * 程序入口
     * 
     * 使用说明:
     * 1. 确保后端服务已启动（默认: http://localhost:8080）
     * 2. 确保JavaFX运行环境正确配置
     * 3. 确保所有FXML和CSS资源文件已正确打包
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║    米多星球产线采集系统 v2.0.0              ║");
        System.out.println("║    Miduo Production Line Collection System  ║");
        System.out.println("║                                              ║");
        System.out.println("║    前端模块 (JavaFX)                        ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("系统信息:");
        System.out.println("  Java版本: " + System.getProperty("java.version"));
        System.out.println("  JavaFX版本: 17.0.2");
        System.out.println("  操作系统: " + System.getProperty("os.name"));
        System.out.println();
        System.out.println("正在启动JavaFX应用程序...");
        System.out.println();
        
        try {
            // 启动JavaFX应用程序
            MiduoFrontendApplication.main(args);
        } catch (Exception e) {
            System.err.println("\n应用程序启动失败！");
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("\n应用程序已退出。");
        System.out.println("感谢使用米多星球产线采集系统！\n");
    }
}

